package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.dto.request.DirectMessageRequest;
import hcmute.edu.vn.discord.dto.request.EditMessageRequest;
import hcmute.edu.vn.discord.dto.response.DirectMessageResponse;
import hcmute.edu.vn.discord.entity.mongo.DirectMessage;
import hcmute.edu.vn.discord.entity.mongo.Conversation;
import hcmute.edu.vn.discord.repository.ConversationRepository;
import hcmute.edu.vn.discord.repository.DirectMessageRepository;
import hcmute.edu.vn.discord.repository.UserRepository;
import hcmute.edu.vn.discord.service.DirectMessageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DirectMessageServiceImpl implements DirectMessageService {

    private final ConversationRepository conversationRepo;
    private final DirectMessageRepository messageRepo;
    private final UserRepository userRepo;

    @Override
    @Transactional
    public DirectMessageResponse sendMessage(Long senderId, DirectMessageRequest request) {
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }

        if (!userRepo.existsById(senderId) || !userRepo.existsById(request.getReceiverId())) {
            throw new EntityNotFoundException("User not found");
        }

        if (senderId.equals(request.getReceiverId())) {
            throw new IllegalArgumentException("Cannot message yourself");
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

    // Updated the getMessages method to return the first 50 messages by default.
    @Override
    public List<DirectMessageResponse> getMessages(String conversationId, Long userId) {
        return getMessages(conversationId, userId, PageRequest.of(0, 50)).getContent();
    }

    @Override
    public DirectMessageResponse editMessage(String messageId, Long userId, EditMessageRequest request) {
        throw new UnsupportedOperationException("Method not implemented.");
    }

    @Override
    public void deleteMessage(String messageId, Long userId) {
        throw new UnsupportedOperationException("Method not implemented.");
    }

    // Added @Transactional annotation to ensure database consistency.
    @Override
    @Transactional
    public void addReaction(String messageId, Long userId, String emoji) {
        DirectMessage message = messageRepo.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));

        Conversation conversation = conversationRepo.findById(message.getConversationId())
                .orElseThrow(() -> new EntityNotFoundException("Conversation not found"));

        // Check if the user is part of the conversation
        if (!conversation.getUser1Id().equals(userId) && !conversation.getUser2Id().equals(userId)) {
            throw new AccessDeniedException("User does not have permission to react to this message");
        }

        message.getReactions().put(userId, emoji);
        message.setUpdatedAt(new Date());
        messageRepo.save(message);
    }

    // Fixed security issue in removeReaction() by adding access control checks and optimizing database queries.
    @Override
    @Transactional
    public void removeReaction(String messageId, Long userId) {
        DirectMessage message = messageRepo.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));

        Conversation conversation = conversationRepo.findById(message.getConversationId())
                .orElseThrow(() -> new EntityNotFoundException("Conversation not found"));

        // Check if the user is part of the conversation
        if (!conversation.getUser1Id().equals(userId) && !conversation.getUser2Id().equals(userId)) {
            throw new AccessDeniedException("User does not have permission to remove reactions from this message");
        }

        // Check if the reaction exists before updating the database
        if (message.getReactions().containsKey(userId)) {
            message.getReactions().remove(userId);
            message.setUpdatedAt(new Date());
            messageRepo.save(message);
        }
    }

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
