package hcmute.edu.vn.discord.repository;

import hcmute.edu.vn.discord.entity.mongo.SystemNotification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemNotificationRepository extends MongoRepository<SystemNotification, String> {
    // Lấy thông báo của user (chưa đọc hoặc tất cả)
    List<SystemNotification> findByUserIdAndReadFalse(Long userId);
    List<SystemNotification> findByUserId(Long userId);
}
