package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.dto.request.AdminMessageSearchRequest;
import hcmute.edu.vn.discord.dto.response.AdminMessageItemResponse;
import hcmute.edu.vn.discord.dto.response.AdminMessagePageResponse;
import hcmute.edu.vn.discord.dto.response.MessageResponse;
import hcmute.edu.vn.discord.entity.jpa.Channel;
import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.entity.mongo.Message;
import hcmute.edu.vn.discord.repository.ChannelRepository;
import hcmute.edu.vn.discord.repository.MessageRepository;
import hcmute.edu.vn.discord.repository.ServerRepository;
import hcmute.edu.vn.discord.repository.UserRepository;
import hcmute.edu.vn.discord.service.AdminMessageService;
import hcmute.edu.vn.discord.service.AuditLogMongoService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
public class AdminMessageServiceImpl implements AdminMessageService {

    private final MongoTemplate mongoTemplate;
    private final MessageRepository messageRepository;
    private final ChannelRepository channelRepository;
    private final ServerRepository serverRepository;
    private final UserRepository userRepository;
    private final AuditLogMongoService auditLogService;

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional(readOnly = true)
    public AdminMessagePageResponse search(AdminMessageSearchRequest req) {
        List<Criteria> ands = new ArrayList<>();

        // Chỉ áp dụng filter nếu id > 0
        if (req.getChannelId() != null && req.getChannelId() > 0) {
            ands.add(Criteria.where("channelId").is(req.getChannelId()));
        } else if (req.getServerId() != null && req.getServerId() > 0) {
            List<Long> chIds = channelRepository.findByServerId(req.getServerId()).stream()
                    .map(Channel::getId).toList();
            if (chIds.isEmpty()) {
                return emptyPage(req);
            }
            ands.add(Criteria.where("channelId").in(chIds));
        }

        if (req.getSenderId() != null && req.getSenderId() > 0) {
            ands.add(Criteria.where("senderId").is(req.getSenderId()));
        } else if (StringUtils.hasText(req.getSenderUsername())) {
            userRepository.findByUsername(req.getSenderUsername())
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

        // Không có filter: dùng repository
        if (ands.isEmpty()) {
            Page<Message> page = messageRepository.findAll(pr);
            return toPageResponse(pr, page.getContent(), page.getTotalElements());
        }

        // Có filter: dùng MongoTemplate, KHÔNG fallback về findAll nếu rỗng
        Query baseQuery = new Query().addCriteria(new Criteria().andOperator(ands));
        Query pageQuery = baseQuery.with(pr);
        List<Message> messages = mongoTemplate.find(pageQuery, Message.class);
        long total = mongoTemplate.count(baseQuery, Message.class);

        return toPageResponse(pr, messages, total);
    }

    @Override
    @Transactional
    public void softDelete(String messageId, String adminUsername) {
        Message m = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found: " + messageId));
        if (Boolean.TRUE.equals(m.getDeleted())) return;
        m.setDeleted(true);
        messageRepository.save(m);

        Map<String, Object> adminPayload = new HashMap<>();
        adminPayload.put("id", messageId);
        adminPayload.put("status", "DELETED");
        messagingTemplate.convertAndSend("/topic/admin/messages/update", adminPayload);

        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put("type", "DELETE_MESSAGE");
        userPayload.put("messageId", messageId);
        messagingTemplate.convertAndSend("/topic/channel/" + m.getChannelId(), userPayload);

        auditLogService.log(adminUsername, hcmute.edu.vn.discord.entity.enums.EAuditAction.ADMIN_DELETE_MESSAGE.name(),
                "Message ID: " + messageId, "Soft deleted message");
    }

    @Override
    @Transactional
    public void restore(String messageId, String adminUsername) {
        Message m = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found: " + messageId));
        if (!Boolean.TRUE.equals(m.getDeleted())) return;
        m.setDeleted(false);
        messageRepository.save(m);

        Map<String, Object> adminPayload = new HashMap<>();
        adminPayload.put("id", messageId);
        adminPayload.put("status", "ACTIVE");
        messagingTemplate.convertAndSend("/topic/admin/messages/update", adminPayload);

        User sender = userRepository.findById(m.getSenderId()).orElse(null);
        MessageResponse msgResponse = mapToUserMessageResponse(m, sender);

        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put("type", "RESTORE_MESSAGE");

        userPayload.put("id", msgResponse.getId());
        userPayload.put("channelId", msgResponse.getChannelId());
        userPayload.put("content", msgResponse.getContent());
        userPayload.put("senderId", msgResponse.getSenderId());
        userPayload.put("senderName", msgResponse.getSenderName());
        userPayload.put("senderAvatar", msgResponse.getSenderAvatar());
        userPayload.put("createdAt", msgResponse.getCreatedAt());
        userPayload.put("attachments", msgResponse.getAttachments());
        userPayload.put("reactions", msgResponse.getReactions());
        userPayload.put("isEdited", msgResponse.isEdited());
        userPayload.put("deleted", false);

        messagingTemplate.convertAndSend("/topic/channel/" + m.getChannelId(), userPayload);

        auditLogService.log(adminUsername, hcmute.edu.vn.discord.entity.enums.EAuditAction.ADMIN_RESTORE_MESSAGE.name(),
                "Message ID: " + messageId, "Restored message");
    }

    private AdminMessagePageResponse emptyPage(AdminMessageSearchRequest req) {
        return AdminMessagePageResponse.builder()
                .items(List.of())
                .page(req.getPage())
                .size(req.getSize())
                .totalElements(0)
                .totalPages(0)
                .build();
    }

    private AdminMessagePageResponse toPageResponse(PageRequest pr, List<Message> messages, long total) {
        Set<Long> channelIds = messages.stream().map(Message::getChannelId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Channel> chMap = channelRepository.findAllById(channelIds).stream()
                .collect(Collectors.toMap(Channel::getId, c -> c));

        Set<Long> serverIds = chMap.values().stream().map(c -> c.getServer().getId()).collect(Collectors.toSet());
        Map<Long, Server> srvMap = serverRepository.findAllById(serverIds).stream()
                .collect(Collectors.toMap(Server::getId, s -> s));

        Set<Long> senderIds = messages.stream().map(Message::getSenderId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> userMap = userRepository.findAllById(senderIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<AdminMessageItemResponse> items = messages.stream().map(m -> {
            Channel ch = chMap.get(m.getChannelId());
            Server sv = ch != null ? srvMap.get(ch.getServer().getId()) : null;
            User u = userMap.get(m.getSenderId());
            return AdminMessageItemResponse.builder()
                    .id(m.getId())
                    .serverId(sv != null ? sv.getId() : null)
                    .serverName(sv != null ? sv.getName() : null)
                    .channelId(ch != null ? ch.getId() : null)
                    .channelName(ch != null ? ch.getName() : null)
                    .senderId(u != null ? u.getId() : m.getSenderId())
                    .senderUsername(u != null ? u.getUsername() : null)
                    .senderDisplayName(u != null ? u.getDisplayName() : null)
                    .senderAvatarUrl(u != null ? u.getAvatarUrl() : null)
                    .content(m.getContent())
                    .deleted(Boolean.TRUE.equals(m.getDeleted()))
                    .edited(Boolean.TRUE.equals(m.getIsEdited()))
                    .attachmentsCount(m.getAttachments() != null ? m.getAttachments().size() : 0)
                    .reactionsCount(m.getReactions() != null ? m.getReactions().size() : 0)
                    .createdAt(m.getCreatedAt() != null ? m.getCreatedAt().toInstant() : null)
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

    private MessageResponse mapToUserMessageResponse(Message msg, User sender) {
        return MessageResponse.builder()
                .id(msg.getId())
                .channelId(msg.getChannelId())
                .content(msg.getContent()) // Khi restore thì lấy content gốc
                .createdAt(msg.getCreatedAt())
                .senderId(msg.getSenderId())
                .senderName(sender != null ? sender.getDisplayName() : "Unknown User")
                .senderAvatar(sender != null ? sender.getAvatarUrl() : null)
                .attachments(msg.getAttachments())
                .reactions(msg.getReactions())
                .deleted(false)
                .edited(Boolean.TRUE.equals(msg.getIsEdited()))
                .build();
    }
}