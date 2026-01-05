package hcmute.edu.vn.discord.dto.request;

import lombok.Data;
import java.time.Instant;

@Data
public class AdminAuditSearchRequest {
    private String email;       // Tìm theo người thực hiện
    private String action;      // Tìm theo hành động (LOGIN, BAN_USER...)
    private String targetType;  // Đối tượng bị tác động (USER, SERVER...)
    private Instant from;       // Từ ngày
    private Instant to;         // Đến ngày
    private int page = 0;
    private int size = 20;
}