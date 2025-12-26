package hcmute.edu.vn.discord.dto.response;

import hcmute.edu.vn.discord.entity.jpa.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServerMemberSummary {
    private Long id;
    private String username;
    private String displayName;
    private String avatarUrl;

    public static ServerMemberSummary from(User u) {
        if (u == null) return null;
        return ServerMemberSummary.builder()
                .id(u.getId())
                .username(u.getUsername())
                .displayName(u.getDisplayName())
                .avatarUrl(u.getAvatarUrl())
                .build();
    }
}