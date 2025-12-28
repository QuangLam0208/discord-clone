package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.dto.request.DirectMessageRequest;
import hcmute.edu.vn.discord.dto.response.DirectMessageResponse;
import hcmute.edu.vn.discord.entity.mongo.DirectMessage;
import hcmute.edu.vn.discord.entity.mongo.Conversation;
import hcmute.edu.vn.discord.repository.ConversationRepository;
import hcmute.edu.vn.discord.repository.DirectMessageRepository;
import hcmute.edu.vn.discord.service.DirectMessageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DirectMessageServiceImpl implements DirectMessageService {

    private final ConversationRepository conversationRepo;
    private final DirectMessageRepository messageRepo;

    @Override
    public DirectMessageResponse sendMessage(Long senderId, DirectMessageRequest request) {
        if (senderId == null || request.getReceiverId() == null) {
            throw new IllegalArgumentException("Sender or receiver cannot be null");
        }

        if (senderId.equals(request.getReceiverId())) {
            throw new IllegalArgumentException("Không thể nhắn tin cho chính mình");
        }

        Conversation conversation = getOrCreateConversation(senderId, request.getReceiverId());

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
                        .build()
        );

        return toResponse(message);
    }

    @Override
    public List<DirectMessageResponse> getMessages(String conversationId, Long userId) {
        Conversation conversation = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation not found"));

        if (!conversation.getUser1Id().equals(userId) && !conversation.getUser2Id().equals(userId)) {
            throw new AccessDeniedException("User does not have permission to access this conversation");
        }

        return messageRepo.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public DirectMessageResponse editMessage(String messageId, Long userId, DirectMessageRequest request) {
        DirectMessage message = messageRepo.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));

        if (!message.getSenderId().equals(userId)) {
            throw new IllegalArgumentException("User is not the sender of the message");
        }

        String newContent = request.getContent();
        if (newContent == null || newContent.trim().isEmpty()) {
            throw new IllegalArgumentException("[editMessage] Message content cannot be empty or blank");
        }

        message.setContent(newContent);
        message.setEdited(true);
        message.setUpdatedAt(new Date());

        DirectMessage updatedMessage = messageRepo.save(message);
        return toResponse(updatedMessage);
    }

    @Override
    public void deleteMessage(String messageId, Long userId) {
        DirectMessage message = messageRepo.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));

        if (!message.getSenderId().equals(userId)) {
            throw new AccessDeniedException("You are not allowed to delete this message");
        }

        if (message.isDeleted()) {
            return; // Message already deleted
        }

        message.setDeleted(true);
        message.setUpdatedAt(new Date());
        messageRepo.save(message);
    }

    @Override
    public void addReaction(String messageId, Long userId, String emoji) {
        DirectMessage message = messageRepo.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));

        message.getReactions().put(userId, emoji);
        message.setUpdatedAt(new Date());
        messageRepo.save(message);
    }

    @Override
    public void removeReaction(String messageId, Long userId) {
        DirectMessage message = messageRepo.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));

        if (message.getReactions().remove(userId) != null) {
            message.setUpdatedAt(new Date());
            messageRepo.save(message);
        }
    }

    // ===== INTERNAL =====
    private Conversation getOrCreateConversation(Long u1, Long u2) {

        Long user1 = Math.min(u1, u2);
        Long user2 = Math.max(u1, u2);

        return conversationRepo
                .findByUser1IdAndUser2Id(user1, user2)
                .orElseGet(() ->
                        conversationRepo.save(
                                Conversation.builder()
                                        .user1Id(user1)
                                        .user2Id(user2)
                                        .createdAt(new Date())
                                        .updatedAt(new Date())
                                        .build()
                        )
                );
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
}
