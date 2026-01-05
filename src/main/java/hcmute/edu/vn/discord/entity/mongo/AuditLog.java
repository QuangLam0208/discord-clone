package hcmute.edu.vn.discord.entity.mongo;

import org.springframework.data.annotation.Id;
import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "audit_logs")
@Data
public class AuditLog {

    @Id
    private String id;

    @Indexed
    private Long adminId;

    @Indexed
    private String action; // BAN_USER, DELETE_MESSAGE
    private String target; // userId / messageId / serverId
    private String detail;

    private String username;
    private String role; // ADMIN, PREMIUM, DEFAULT

    private Date createdAt = new Date();
}
