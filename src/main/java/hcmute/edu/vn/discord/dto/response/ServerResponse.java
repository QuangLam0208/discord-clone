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
public class ServerResponse {

    private Long id;
    private String name;
    private String description;
    private String iconUrl;

    private Long ownerId;
    private String ownerUsername;

    private ServerStatus status;

    private int channelCount;
    private int memberCount;

    public static ServerResponse from(Server server) {
        if (server == null) return null;
        return ServerResponse.builder()
                .id(server.getId())
                .name(server.getName())
                .description(server.getDescription())
                .iconUrl(server.getIconUrl())
                .status(server.getStatus())
                .ownerId(server.getOwner() != null ? server.getOwner().getId() : null)
                .ownerUsername(server.getOwner() != null ? server.getOwner().getUsername() : null)
                .channelCount(server.getChannels() != null ? server.getChannels().size() : 0)
                .memberCount(server.getMembers() != null ? server.getMembers().size() : 0)
                .build();
    }

    // Dùng hàm này khi map danh sách (tránh đụng LAZY, truyền sẵn counts)
    public static ServerResponse from(Server server, int channelCount, int memberCount) {
        if (server == null) return null;
        return ServerResponse.builder()
                .id(server.getId())
                .name(server.getName())
                .description(server.getDescription())
                .iconUrl(server.getIconUrl())
                .status(server.getStatus())
                .ownerId(server.getOwner() != null ? server.getOwner().getId() : null)
                .ownerUsername(server.getOwner() != null ? server.getOwner().getUsername() : null)
                .channelCount(channelCount)
                .memberCount(memberCount)
                .build();
    }
}

