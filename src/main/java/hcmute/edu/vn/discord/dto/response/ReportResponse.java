package hcmute.edu.vn.discord.dto.response;

import hcmute.edu.vn.discord.entity.enums.ReportStatus;
import hcmute.edu.vn.discord.entity.enums.ReportType;
import hcmute.edu.vn.discord.entity.jpa.Report;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class ReportResponse {
    private Long id;
    private UserResponse reporter;
    private UserResponse targetUser;
    private String serverName;
    private ReportType type;
    private ReportStatus status;
    private Date createdAt;

    public static ReportResponse from(Report report) {
        return ReportResponse.builder()
                .id(report.getId())
                .reporter(UserResponse.from(report.getReporter()))
                .targetUser(UserResponse.from(report.getTargetUser()))
                // Handle case where server might be null or lazy loaded proxy failure (though
                // we are in controller, session might be closed if lazy)
                // Default ManyToOne is EAGER, but if it is LAZY, this could fail outside
                // transaction.
                // Best practice: Service should return DTO. But here we are mapping in
                // Controller.
                // Let's assume EAGER or handle exception.
                .serverName(report.getServer() != null ? report.getServer().getName() : "Direct Message")
                .type(report.getType())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
