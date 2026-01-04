package hcmute.edu.vn.discord.dto.response;

import hcmute.edu.vn.discord.common.AuditLogAction;
import hcmute.edu.vn.discord.entity.jpa.AuditLog;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogResponse {
    private Long id;
    private UserResponse actor;
    private AuditLogAction actionType;
    private String targetId;
    private String targetType;
    private String changes;
    private LocalDateTime createdAt;

    public static AuditLogResponse from(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .actor(log.getActor() != null ? UserResponse.from(log.getActor()) : null)
                .actionType(log.getActionType())
                .targetId(log.getTargetId())
                .targetType(log.getTargetType())
                .changes(log.getChanges())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
