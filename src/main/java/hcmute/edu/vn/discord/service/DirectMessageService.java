package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.entity.mongo.DirectMessage;

import java.util.List;

public interface DirectMessageService {
    // Gửi tin nhắn giữa 2 user
    DirectMessage sendMessage(Long senderId, Long receiverId, String content);

    // Lấy lịch sử chat giữa 2 user
    List<DirectMessage> getConversation(Long userId1, Long userId2);

    // Lấy danh sách user đã chat với user hiện tại
    List<Long> getChatPartners(Long userId);
}
