package hcmute.edu.vn.discord.dto.response;

import hcmute.edu.vn.discord.entity.jpa.Permission;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PermissionResponse {
    String code;
    String description;

    public static PermissionResponse from(Permission p) {
        if (p == null) return null;
        return PermissionResponse.builder()
                .code(p.getCode())
                .description(p.getDescription())
                .build();
    }
}