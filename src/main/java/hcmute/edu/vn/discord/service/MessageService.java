package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.entity.mongo.Message;

import java.util.List;
import java.util.Optional;

public interface MessageService {
    Message sendMessage(Message message);
    Optional<Message> getMessageById(String id);
    List<Message> getMessagesByChannel(Long channelId);
    Message editMessage(String id, String newContent);
    void deleteMessage(String id);

    Message addReaction(String messageId, Long userId, String emoji);
    Message removeReaction(String messageId, Long userId, String emoji);
}
