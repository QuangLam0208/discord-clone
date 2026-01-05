package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.dto.request.AdminDirectMessageSearchRequest;
import hcmute.edu.vn.discord.dto.response.AdminDirectMessageItemResponse;
import hcmute.edu.vn.discord.dto.response.AdminMessagePageResponse;
import hcmute.edu.vn.discord.dto.response.DirectMessageResponse;
import hcmute.edu.vn.discord.entity.enums.EAuditAction;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.entity.mongo.Conversation;
import hcmute.edu.vn.discord.entity.mongo.DirectMessage;
import hcmute.edu.vn.discord.repository.ConversationRepository;
import hcmute.edu.vn.discord.repository.DirectMessageRepository;
import hcmute.edu.vn.discord.repository.UserRepository;
import hcmute.edu.vn.discord.service.AdminDirectMessageService;
import hcmute.edu.vn.discord.service.AuditLogMongoService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDirectMessageServiceImpl implements AdminDirectMessageService {

    private final MongoTemplate mongoTemplate;
    private final DirectMessageRepository dmRepo;
    private final ConversationRepository convRepo;
    private final UserRepository userRepo;
    private final AuditLogMongoService auditLogMongoService;

    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    @Override
    public AdminMessagePageResponse search(AdminDirectMessageSearchRequest req) {
        List<Criteria> ands = new ArrayList<>();

        Set<String> convIds = new LinkedHashSet<>();
        if (StringUtils.hasText(req.getConversationId())) {
            convIds.add(req.getConversationId());
        } else if (req.getUserAId() != null && req.getUserBId() != null) {
            convRepo.findByUser1IdAndUser2Id(req.getUserAId(), req.getUserBId()).ifPresent(c -> convIds.add(c.getId()));
            convRepo.findByUser1IdAndUser2Id(req.getUserBId(), req.getUserAId()).ifPresent(c -> convIds.add(c.getId()));
        } else if (req.getUserId() != null) {
            convRepo.findByUser1IdOrUser2Id(req.getUserId(), req.getUserId())
                    .forEach(c -> convIds.add(c.getId()));
        }
        if (!convIds.isEmpty()) {
            ands.add(Criteria.where("conversationId").in(convIds));
        }

        if (req.getSenderId() != null && req.getSenderId() > 0) {
            ands.add(Criteria.where("senderId").is(req.getSenderId()));
        } else if (StringUtils.hasText(req.getSenderUsername())) {
            userRepo.findByUsername(req.getSenderUsername())
                    .ifPresent(u -> ands.add(Criteria.where("senderId").is(u.getId())));
        }

        if (StringUtils.hasText(req.getKeyword())) {
            Pattern regex = Pattern.compile(Pattern.quote(req.getKeyword()), Pattern.CASE_INSENSITIVE);
            ands.add(Criteria.where("content").regex(regex));
        }

        if ("active".equalsIgnoreCase(req.getStatus())) {
            ands.add(Criteria.where("deleted").ne(true));
        } else if ("deleted".equalsIgnoreCase(req.getStatus())) {
            ands.add(Criteria.where("deleted").is(true));
        }

        Instant from = req.getFrom();
        Instant to = req.getTo();
        if (from != null || to != null) {
            Criteria c = Criteria.where("createdAt");
            if (from != null && to != null) {
                ands.add(c.gte(Date.from(from)).lte(Date.from(to)));
            } else if (from != null) {
                ands.add(c.gte(Date.from(from)));
            } else {
                ands.add(c.lte(Date.from(to)));
            }
        }

        PageRequest pr = PageRequest.of(Math.max(0, req.getPage()), Math.max(1, req.getSize()),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Query q = new Query();
        if (!ands.isEmpty())
            q.addCriteria(new Criteria().andOperator(ands));

        long total = mongoTemplate.count(q, DirectMessage.class);

        Query pageQ = q.with(pr);

        List<DirectMessage> msgs = mongoTemplate.find(pageQ, DirectMessage.class);

        Set<Long> senderIds = msgs.stream().map(DirectMessage::getSenderId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> receiverIds = msgs.stream().map(DirectMessage::getReceiverId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> userIds = new HashSet<>();
        userIds.addAll(senderIds);
        userIds.addAll(receiverIds);
        Map<Long, User> userMap = userRepo.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        Set<String> cids = msgs.stream().map(DirectMessage::getConversationId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, Conversation> convMap = convRepo.findAllById(cids).stream()
                .collect(Collectors.toMap(Conversation::getId, c -> c));

        List<AdminDirectMessageItemResponse> items = msgs.stream().map(m -> {
            User su = userMap.get(m.getSenderId());
            User ru = userMap.get(m.getReceiverId());
            Conversation cv = convMap.get(m.getConversationId());
            Long aId = null, bId = null;
            String aUsername = null, bUsername = null;
            if (cv != null) {
                aId = cv.getUser1Id();
                bId = cv.getUser2Id();
                User aU = aId != null ? userMap.get(aId) : null;
                User bU = bId != null ? userMap.get(bId) : null;
                aUsername = aU != null ? aU.getUsername() : null;
                bUsername = bU != null ? bU.getUsername() : null;
            }

            return AdminDirectMessageItemResponse.builder()
                    .id(m.getId())
                    .conversationId(m.getConversationId())
                    .senderId(su != null ? su.getId() : m.getSenderId())
                    .senderUsername(su != null ? su.getUsername() : null)
                    .senderDisplayName(su != null ? su.getDisplayName() : null)
                    .senderAvatarUrl(su != null ? su.getAvatarUrl() : null)
                    .receiverId(ru != null ? ru.getId() : m.getReceiverId())
                    .receiverUsername(ru != null ? ru.getUsername() : null)
                    .content(m.getContent())
                    .deleted(Boolean.TRUE.equals(m.isDeleted()))
                    .edited(Boolean.TRUE.equals(m.isEdited()))
                    .reactionsCount(m.getReactions() != null ? m.getReactions().size() : 0)
                    .attachmentsCount(0)
                    .createdAt(m.getCreatedAt() != null ? m.getCreatedAt().toInstant() : null)
                    .userAId(aId)
                    .userBId(bId)
                    .userAUsername(aUsername)
                    .userBUsername(bUsername)
                    .build();
        }).toList();

        int totalPages = (int) Math.ceil(total / (double) pr.getPageSize());
        return AdminMessagePageResponse.builder()
                .items(items)
                .page(pr.getPageNumber())
                .size(pr.getPageSize())
                .totalElements(total)
                .totalPages(totalPages)
                .build();
    }

    @Transactional
    @Override
    public void softDelete(String messageId, String adminUsername) {
        DirectMessage m = dmRepo.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("DM not found: " + messageId));
        if (Boolean.TRUE.equals(m.isDeleted()))
            return;
        m.setDeleted(true);
        dmRepo.save(m);

        // Admin UI
        messagingTemplate.convertAndSend("/topic/admin/dms/update",
                Map.of("id", messageId, "status", "DELETED"));

        // Build payload cho User UI
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "DELETE_MESSAGE");
        payload.put("messageId", messageId);
        payload.put("conversationId", m.getConversationId());

        // User UI (topic theo hội thoại)
        messagingTemplate.convertAndSend("/topic/dm/" + m.getConversationId(), payload);

        // User UI (queue cá nhân của 2 participants)
        Conversation conv = convRepo.findById(m.getConversationId()).orElse(null);
        sendToConversationParticipants(conv, payload);

        // Audit Log
        if (adminUsername != null) {
            auditLogMongoService.log(
                    adminUsername,
                    EAuditAction.ADMIN_DELETE_MESSAGE.name(),
                    "DirectMessage:" + messageId,
                    "Admin soft deleted DM in Conv: " + m.getConversationId());
        }
    }

    @Transactional
    @Override
    public void restore(String messageId, String adminUsername) {
        DirectMessage m = dmRepo.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("DM not found: " + messageId));
        if (!Boolean.TRUE.equals(m.isDeleted()))
            return;
        m.setDeleted(false);
        dmRepo.save(m);

        // Admin UI
        messagingTemplate.convertAndSend("/topic/admin/dms/update",
                Map.of("id", messageId, "status", "ACTIVE"));

        // User UI (full message & type)
        DirectMessageResponse response = mapToUserDirectMessageResponse(m);
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "RESTORE_MESSAGE");
        payload.putAll(convertToMap(response));

        // Topic theo hội thoại
        messagingTemplate.convertAndSend("/topic/dm/" + m.getConversationId(), payload);

        // Queue cá nhân
        Conversation conv = convRepo.findById(m.getConversationId()).orElse(null);
        sendToConversationParticipants(conv, payload);

        // Audit Log
        if (adminUsername != null) {
            auditLogMongoService.log(
                    adminUsername,
                    EAuditAction.ADMIN_RESTORE_MESSAGE.name(),
                    "DirectMessage:" + messageId,
                    "Admin restored DM in Conv: " + m.getConversationId());
        }
    }

    private Map<String, Object> convertToMap(DirectMessageResponse res) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", res.getId());
        map.put("conversationId", res.getConversationId());
        map.put("content", res.getContent());
        map.put("senderId", res.getSenderId());
        map.put("createdAt", res.getCreatedAt());
        map.put("edited", res.isEdited());
        map.put("deleted", false);
        return map;
    }

    private DirectMessageResponse mapToUserDirectMessageResponse(DirectMessage m) {
        return DirectMessageResponse.builder()
                .id(m.getId())
                .conversationId(m.getConversationId())
                .senderId(m.getSenderId())
                .receiverId(m.getReceiverId())
                .content(m.getContent())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .edited(m.isEdited())
                .deleted(m.isDeleted())
                .reactions(m.getReactions())
                .build();
    }

    private void sendToConversationParticipants(Conversation conv, Object payload) {
        if (conv == null)
            return;
        userRepo.findById(conv.getUser1Id())
                .ifPresent(u -> messagingTemplate.convertAndSendToUser(u.getUsername(), "/queue/dm", payload));
        userRepo.findById(conv.getUser2Id())
                .ifPresent(u -> messagingTemplate.convertAndSendToUser(u.getUsername(), "/queue/dm", payload));
    }
}