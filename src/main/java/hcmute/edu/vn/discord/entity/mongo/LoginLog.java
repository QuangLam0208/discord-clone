package hcmute.edu.vn.discord.entity.mongo;

import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "login_logs")
@Data
public class LoginLog {
    @Id
    private String id;

    @Indexed
    private Long userId; // Ref tới MySQL User ID

    private String ipAddress;
    private String device;
    private String userAgent; // Bổ sung để biết trình duyệt gì

    // Tự động xóa sau 30 ngày
    @Indexed(expireAfter = "30d")
    private Date loginTime = new Date();
}

