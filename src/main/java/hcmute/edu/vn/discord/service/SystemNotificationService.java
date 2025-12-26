package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.entity.mongo.SystemNotification;
import java.util.List;

public interface SystemNotificationService {
    // Gửi thông báo cho user cụ thể
    SystemNotification sendNotification(Long userId, String title, String content, String type);

    // Gửi thông báo cho toàn hệ thống (Broadcast)
    SystemNotification sendBroadcastNotification(String title, String content, String type);

    // Lấy thông báo chưa đọc của user
    List<SystemNotification> getUnreadNotifications(Long userId);

    // Đánh dấu đã đọc
    void markAsRead(String notificationId);

    // Đánh dấu tất cả là đã đọc
    void markAllAsRead(Long userId);
}