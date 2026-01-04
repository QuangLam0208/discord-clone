package hcmute.edu.vn.discord.dto.response;

import hcmute.edu.vn.discord.entity.jpa.ServerBan;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ServerBanResponse {
    private Long id;
    private Long serverId;
    private UserResponse user;
    private UserResponse bannedBy;
    private String reason;
    private LocalDateTime bannedAt;

    public static ServerBanResponse from(ServerBan ban) {
        UserResponse userResp = null;
        try {
            if (ban.getUser() != null) {
                userResp = UserResponse.from(ban.getUser());
            }
        } catch (Exception e) {
            // Broken reference or deleted user
            userResp = UserResponse.builder()
                    .id(0L)
                    .username("Unknown User")
                    .displayName("Deleted User")
                    .build();
        }

        UserResponse bannedByResp = null;
        try {
            if (ban.getBannedBy() != null) {
                bannedByResp = UserResponse.from(ban.getBannedBy());
            }
        } catch (Exception ignored) {
        }

        return ServerBanResponse.builder()
                .id(ban.getId())
                .serverId(ban.getServer().getId())
                .user(userResp)
                .bannedBy(bannedByResp)
                .reason(ban.getReason())
                .bannedAt(ban.getBannedAt())
                .build();
    }
}
