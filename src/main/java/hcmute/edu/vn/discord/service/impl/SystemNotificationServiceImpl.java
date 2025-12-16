package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.entity.mongo.SystemNotification;
import hcmute.edu.vn.discord.repository.SystemNotificationRepository;
import hcmute.edu.vn.discord.service.SystemNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class SystemNotificationServiceImpl implements SystemNotificationService {

    @Autowired
    private SystemNotificationRepository notificationRepository;

    @Override
    public SystemNotification sendNotification(Long userId, String title, String content, String type) {
        SystemNotification notification = new SystemNotification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type); // INFO, WARNING, ERROR
        notification.setRead(false);
        notification.setCreatedAt(new Date());

        return notificationRepository.save(notification);
    }

    @Override
    public SystemNotification sendBroadcastNotification(String title, String content, String type) {
        // userId = null nghĩa là gửi cho tất cả
        return sendNotification(null, title, content, type);
    }

    @Override
    public List<SystemNotification> getUnreadNotifications(Long userId) {
        // Cần lấy cả thông báo riêng VÀ thông báo chung (userId = null)
        // Repository của bạn hiện tại chỉ tìm findByUserIdAndReadFalse
        // Bạn có thể cần custom query hoặc gọi 2 lần rồi merge list
        // Tạm thời mình gọi hàm có sẵn:
        return notificationRepository.findByUserIdAndReadFalse(userId);
    }

    @Override
    public void markAsRead(String notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }

    @Override
    public void markAllAsRead(Long userId) {
        List<SystemNotification> list = notificationRepository.findByUserIdAndReadFalse(userId);
        for (SystemNotification noti : list) {
            noti.setRead(true);
        }
        notificationRepository.saveAll(list);
    }
}