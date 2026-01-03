package hcmute.edu.vn.discord.dto.response;

import hcmute.edu.vn.discord.entity.enums.ServerStatus;
import hcmute.edu.vn.discord.entity.jpa.Server;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerSummaryResponse {
    private Long id;
    private String name;
    private String description;
    private String iconUrl;

    private ServerStatus status;
    private String ownerUsername;

    private int channelCount;
    private int memberCount;

    public static ServerSummaryResponse from(Server server, int channelCount, int memberCount) {
        if (server == null) return null;
        return ServerSummaryResponse.builder()
                .id(server.getId())
                .name(server.getName())
                .description(server.getDescription())
                .iconUrl(server.getIconUrl())
                .status(server.getStatus())
                .ownerUsername(server.getOwner() != null ? server.getOwner().getUsername() : null)
                .channelCount(channelCount)
                .memberCount(memberCount)
                .build();
    }
}