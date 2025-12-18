package hcmute.edu.vn.discord.entity.mongo;

import org.springframework.data.annotation.Id;
import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "system_notifications")
@Data
public class SystemNotification {
    @Id
    private String id;

    @Indexed
    private Long userId; // null = gửi cho tất cả (Broadcast)

    private String title;
    private String content;
    private String type; // INFO, WARNING, ERROR
    private Boolean read = false;

    @Indexed(expireAfter = "60d") // Tự xóa sau 60 ngày
    private Date createdAt = new Date();
}

