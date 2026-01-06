package hcmute.edu.vn.discord.repository;

import hcmute.edu.vn.discord.entity.mongo.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {

    // 1. Tìm tin nhắn theo Channel có hỗ trợ Phân trang (Pageable)
    // Spring Data Mongo sẽ tự động xử lý page & size cho bạn
    Page<Message> findByChannelId(Long channelId, Pageable pageable);

    List<Message> findByChannelIdAndCreatedAtAfter(Long channelId, Date date, Pageable pageable);

    // 2. (Optional) Tìm tin nhắn cũ không phân trang (nếu cần dùng nội bộ)
    List<Message> findByChannelId(Long channelId);

    // 3. Tìm tin nhắn của user cụ thể trong channel (nếu cần tính năng search)
    List<Message> findByChannelIdAndSenderId(Long channelId, Long senderId);

    // Lấy message mới nhất theo senderId (phù hợp với schema hiện tại)
    Optional<Message> findFirstBySenderIdOrderByCreatedAtDesc(Long senderId);

    // nếu cần theo channel + sender
    Optional<Message> findFirstByChannelIdAndSenderIdOrderByCreatedAtDesc(Long channelId, Long senderId);
}