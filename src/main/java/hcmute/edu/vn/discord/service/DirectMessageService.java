package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.dto.request.DirectMessageRequest;
import hcmute.edu.vn.discord.dto.request.EditMessageRequest;
import hcmute.edu.vn.discord.dto.response.DirectMessageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DirectMessageService {

    DirectMessageResponse sendMessage(Long senderId, DirectMessageRequest request);

    List<DirectMessageResponse> getMessages(String conversationId, Long userId);

    Page<DirectMessageResponse> getMessages(String conversationId, Long userId, Pageable pageable);

    DirectMessageResponse editMessage(String messageId, Long userId, EditMessageRequest request);

    void deleteMessage(String messageId, Long userId);

    void addReaction(String messageId, Long userId, String emoji);

    void removeReaction(String messageId, Long userId);

    hcmute.edu.vn.discord.dto.response.ConversationResponse getOrCreateConversation(Long senderId, Long receiverId);
}
