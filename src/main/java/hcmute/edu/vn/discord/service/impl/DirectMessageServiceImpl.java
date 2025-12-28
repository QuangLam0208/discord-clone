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
import org.springframework.data.domain.Pageable;
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
    public List<DirectMessageResponse> getMessages(String conversationId) {
        return messageRepo
                .findByConversationIdOrderByCreatedAtAsc(conversationId)
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

        message.setContent(request.getContent());
        message.setEdited(true);
        message.setUpdatedAt(new Date());

        DirectMessage updatedMessage = messageRepo.save(message);
        return toResponse(updatedMessage);
    }

    @Override
    public void deleteMessage(String messageId, Long userId) {
        // Logic for deleting a message
    }

    @Override
    public void addReaction(String messageId, Long userId, String emoji) {
        // Logic for adding a reaction
    }

    @Override
    public void removeReaction(String messageId, Long userId) {
        throw new UnsupportedOperationException("Removing reactions is not yet supported.");
    }

    @Override
    public List<DirectMessageResponse> getMessages(Long userId, String conversationId, Pageable pageable) {
        // Delegate to the existing method and apply in-memory pagination
        List<DirectMessageResponse> allMessages = getMessages(conversationId);

        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        int fromIndex = pageNumber * pageSize;

        if (fromIndex >= allMessages.size()) {
            return List.of();
        }

        int toIndex = Math.min(fromIndex + pageSize, allMessages.size());
        return allMessages.subList(fromIndex, toIndex);
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
                .build();
    }
}
