package hcmute.edu.vn.discord.dto.request;

import hcmute.edu.vn.discord.entity.enums.ReportType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportRequest {
    @NotNull
    private Long targetUserId;
    private ReportType type; // Ví dụ: SPAM
    private String reason;
}