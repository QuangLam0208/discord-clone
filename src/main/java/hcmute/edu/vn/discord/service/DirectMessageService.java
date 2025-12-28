package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.dto.request.DirectMessageRequest;
import hcmute.edu.vn.discord.dto.response.DirectMessageResponse;

import java.util.List;

public interface DirectMessageService {

    DirectMessageResponse sendMessage(Long senderId, DirectMessageRequest request);

    List<DirectMessageResponse> getMessages(String conversationId, Long userId);

    DirectMessageResponse editMessage(String messageId, Long userId, DirectMessageRequest request);

    void deleteMessage(String messageId, Long userId);

    void addReaction(String messageId, Long userId, String emoji);

    void removeReaction(String messageId, Long userId);
}
