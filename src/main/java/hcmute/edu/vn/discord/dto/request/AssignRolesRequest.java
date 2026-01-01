package hcmute.edu.vn.discord.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class AssignRolesRequest {
    @NotNull
    private Long userId;

    @NotNull
    private Set<Long> roleIds; // Role IDs sẽ được gán cho user (luôn bao gồm @everyone trên server)
}