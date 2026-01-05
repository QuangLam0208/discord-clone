package hcmute.edu.vn.discord.dto.response;

import hcmute.edu.vn.discord.entity.jpa.Permission;
import hcmute.edu.vn.discord.entity.jpa.ServerRole;
import lombok.Builder;
import lombok.Value;

import java.util.Set;
import java.util.stream.Collectors;

@Value
@Builder
public class ServerRoleResponse {
    Long id;
    String name;
    String color;
    Integer priority;
    Set<String> permissionCodes;
    Integer memberCount;

    public static ServerRoleResponse from(ServerRole role) {
        return ServerRoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .color(role.getColor())
                .priority(role.getPriority())
                .permissionCodes(role.getPermissions() != null
                        ? role.getPermissions().stream().map(Permission::getCode).collect(Collectors.toSet())
                        : Set.of())
                .memberCount(role.getMembers() != null ? role.getMembers().size() : 0)
                .build();
    }
}