package hcmute.edu.vn.discord.repository;

import hcmute.edu.vn.discord.entity.mongo.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {
    // Tìm tất cả tin nhắn trong 1 channel
    List<Message> findByChannelId(Long channelId);

    // Tìm tất cả tin nhắn của 1 người gửi trong channel cụ thể
    List<Message> findByChannelIdAndSenderId(Long channelId, Long senderId);
}
