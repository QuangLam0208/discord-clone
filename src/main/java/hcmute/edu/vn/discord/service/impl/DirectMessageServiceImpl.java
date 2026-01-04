package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.dto.request.DirectMessageRequest;
import hcmute.edu.vn.discord.dto.request.EditMessageRequest;
import hcmute.edu.vn.discord.dto.response.AdminDirectMessageItemResponse;
import hcmute.edu.vn.discord.dto.response.ConversationResponse;
import hcmute.edu.vn.discord.dto.response.DirectMessageResponse;
import hcmute.edu.vn.discord.entity.enums.FriendStatus;
import hcmute.edu.vn.discord.entity.jpa.Friend;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.entity.mongo.DirectMessage;
import hcmute.edu.vn.discord.entity.mongo.Conversation;
import hcmute.edu.vn.discord.repository.ConversationRepository;
import hcmute.edu.vn.discord.repository.DirectMessageRepository;
import hcmute.edu.vn.discord.repository.FriendRepository;
import hcmute.edu.vn.discord.repository.UserRepository;
import hcmute.edu.vn.discord.service.DirectMessageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DirectMessageServiceImpl implements DirectMessageService {

    private final ConversationRepository conversationRepo;
    private final DirectMessageRepository messageRepo;
    private final UserRepository userRepo;
    private final FriendRepository friendRepository;

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public DirectMessageResponse sendMessage(Long senderId, DirectMessageRequest request) {
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung tin nhắn không được để trống");
        }

        if (!userRepo.existsById(senderId) || !userRepo.existsById(request.getReceiverId())) {
            throw new EntityNotFoundException("Không tìm thấy người dùng");
        }

        if (senderId.equals(request.getReceiverId())) {
            throw new IllegalArgumentException("Không thể tự nhắn tin cho chính mình");
        }

        User sender = userRepo.findById(senderId).get();
        User receiver = userRepo.findById(request.getReceiverId()).get();

        var friendRel = friendRepository.findByRequesterAndReceiverOrRequesterAndReceiver(sender, receiver, receiver, sender);

        if (friendRel.isPresent()) {
            Friend f = friendRel.get();
            if (f.getStatus() == FriendStatus.BLOCKED) {
                boolean blockedBySender = f.getRequester().getId().equals(senderId);
                if (blockedBySender) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bạn đang chặn người dùng này. Bỏ chặn để gửi tin nhắn.");
                } else {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể gửi tin nhắn. Bạn đã bị người dùng này chặn.");
                }
            }
        }
        Conversation conversation = findOrCreateConversation(senderId, request.getReceiverId());
        DirectMessage message = messageRepo.save(
                DirectMessage.builder()
                        .conversationId(conversation.getId())
                        .senderId(senderId)
                        .receiverId(request.getReceiverId())
                        .content(request.getContent())
                        .deleted(false)
                        .edited(false)
                        .reactions(new HashMap<>())
                        .createdAt(new Date())
                        .updatedAt(new Date())
                        .build());

        try {
            AdminDirectMessageItemResponse adminDto = convertToAdminDto(message, sender, receiver, conversation);
            messagingTemplate.convertAndSend("/topic/admin/dms/create", adminDto);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return toResponse(message);
    }

    @Override
    public Page<DirectMessageResponse> getMessages(String conversationId, Long userId, Pageable pageable) {
        Conversation conversation = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation not found"));
        if (!conversation.getUser1Id().equals(userId) && !conversation.getUser2Id().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }
        Page<DirectMessage> messages = messageRepo
                .findByConversationIdAndDeletedFalseOrderByCreatedAtDesc(conversationId, pageable);
        return messages.map(this::toResponse);
    }

    @Override
    public List<DirectMessageResponse> getMessages(String conversationId, Long userId) {
        return getMessages(conversationId, userId, PageRequest.of(0, 50)).getContent();
    }

    @Override
    @Transactional
    public DirectMessageResponse editMessage(String messageId, Long userId, EditMessageRequest request) {
        DirectMessage message = messageRepo.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));
        if (!message.getSenderId().equals(userId)) {
            throw new AccessDeniedException("Not your message");
        }
        if (message.isDeleted()) {
            throw new IllegalStateException("Cannot edit deleted message");
        }
        long hoursSinceCreation = (new Date().getTime() - message.getCreatedAt().getTime()) / (1000 * 60 * 60);
        if (hoursSinceCreation > 48) {
            throw new IllegalStateException("Edit time limit exceeded (48 hours)");
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        message.setContent(request.getContent());
        message.setEdited(true);
        message.setUpdatedAt(new Date());
        return toResponse(messageRepo.save(message));
    }

    @Override
    @Transactional
    public void deleteMessage(String messageId, Long userId) {
        DirectMessage message = messageRepo.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));
        if (!message.getSenderId().equals(userId)) {
            throw new AccessDeniedException("Not your message");
        }
        if (message.isDeleted()) return;
        message.setDeleted(true);
        message.setContent("[Message deleted]");
        message.setReactions(new HashMap<>());
        message.setUpdatedAt(new Date());
        messageRepo.save(message);
    }

    @Override
    @Transactional
    public void addReaction(String messageId, Long userId, String emoji) {
        DirectMessage message = messageRepo.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));
        Conversation conversation = conversationRepo.findById(message.getConversationId())
                .orElseThrow(() -> new EntityNotFoundException("Conversation not found"));
        if (!conversation.getUser1Id().equals(userId) && !conversation.getUser2Id().equals(userId)) {
            throw new AccessDeniedException("User does not have permission to react to this message");
        }
        message.getReactions().put(userId, emoji);
        message.setUpdatedAt(new Date());
        messageRepo.save(message);
    }

    @Override
    @Transactional
    public void removeReaction(String messageId, Long userId) {
        DirectMessage message = messageRepo.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));
        Conversation conversation = conversationRepo.findById(message.getConversationId())
                .orElseThrow(() -> new EntityNotFoundException("Conversation not found"));
        if (!conversation.getUser1Id().equals(userId) && !conversation.getUser2Id().equals(userId)) {
            throw new AccessDeniedException("User does not have permission to remove reactions from this message");
        }
        if (message.getReactions().containsKey(userId)) {
            message.getReactions().remove(userId);
            message.setUpdatedAt(new Date());
            messageRepo.save(message);
        }
    }

    // --- Lấy danh sách hội thoại ---
    @Override
    public List<Map<String, Object>> getConversationList(Long userId) {
        List<Conversation> conversations = conversationRepo.findByUser1IdOrUser2Id(userId, userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Conversation c : conversations) {
            Long otherUserId = c.getUser1Id().equals(userId) ? c.getUser2Id() : c.getUser1Id();
            Optional<User> otherUserOpt = userRepo.findById(otherUserId);
            if (otherUserOpt.isPresent()) {
                User u = otherUserOpt.get();
                Map<String, Object> map = new HashMap<>();
                map.put("conversationId", c.getId());
                map.put("friendUserId", u.getId());
                String actualDisplayName = u.getDisplayName();
                if (actualDisplayName == null || actualDisplayName.trim().isEmpty()) {
                    actualDisplayName = u.getUsername();
                }
                map.put("displayName", actualDisplayName);
                map.put("friendUsername", u.getUsername());
                map.put("avatarUrl", u.getAvatarUrl());
                map.put("updatedAt", c.getUpdatedAt());
                result.add(map);
            }
        }
        result.sort((a, b) -> ((Date)b.get("updatedAt")).compareTo((Date)a.get("updatedAt")));
        return result;
    }

    private DirectMessageResponse toResponse(DirectMessage m) {
        return DirectMessageResponse.builder()
                .id(m.getId())
                .conversationId(m.getConversationId())
                .senderId(m.getSenderId())
                .receiverId(m.getReceiverId())
                .content(m.getContent())
                .createdAt(m.getCreatedAt())
                .edited(m.isEdited())
                .deleted(m.isDeleted())
                .reactions(m.getReactions())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

    @Override
    public ConversationResponse getOrCreateConversation(Long senderId, Long receiverId) {
        Conversation conversation = findOrCreateConversation(senderId, receiverId);
        return new ConversationResponse(
                conversation.getId(),
                conversation.getUser1Id(),
                conversation.getUser2Id(),
                conversation.getUpdatedAt());
    }

    private Conversation findOrCreateConversation(Long u1, Long u2) {
        Long user1 = Math.min(u1, u2);
        Long user2 = Math.max(u1, u2);
        return conversationRepo
                .findByUser1IdAndUser2Id(user1, user2)
                .orElseGet(() -> conversationRepo.save(
                        Conversation.builder()
                                .user1Id(user1)
                                .user2Id(user2)
                                .createdAt(new Date())
                                .updatedAt(new Date())
                                .build()));
    }

    private AdminDirectMessageItemResponse convertToAdminDto(DirectMessage m, User sender, User receiver, Conversation c) {
        // Tìm User A/B trong conversation để hiển thị cột Participants
        Long userAId = c.getUser1Id();
        Long userBId = c.getUser2Id();
        String userAUsername = (userAId.equals(sender.getId())) ? sender.getUsername() : (userAId.equals(receiver.getId()) ? receiver.getUsername() : null);
        String userBUsername = (userBId.equals(sender.getId())) ? sender.getUsername() : (userBId.equals(receiver.getId()) ? receiver.getUsername() : null);

        // Nếu A/B chưa có username (trường hợp user thứ 3 lạ lẫm nào đó - ít xảy ra), query thêm nếu cần.
        // Ở đây context sendMessage chắc chắn có 2 user này.

        return AdminDirectMessageItemResponse.builder()
                .id(m.getId())
                .conversationId(m.getConversationId())
                .senderId(sender.getId())
                .senderUsername(sender.getUsername())
                .senderDisplayName(sender.getDisplayName())
                .senderAvatarUrl(sender.getAvatarUrl())
                .receiverId(receiver.getId())
                .receiverUsername(receiver.getUsername())
                .content(m.getContent())
                .deleted(false)
                .edited(false)
                .reactionsCount(0)
                .attachmentsCount(0)
                .createdAt(m.getCreatedAt().toInstant())
                .userAId(userAId)
                .userBId(userBId)
                .userAUsername(userAUsername)
                .userBUsername(userBUsername)
                .build();
    }
}