package hcmute.edu.vn.discord.dto.response;

import hcmute.edu.vn.discord.entity.enums.ChannelType;
import hcmute.edu.vn.discord.entity.jpa.Category;
import hcmute.edu.vn.discord.entity.jpa.Channel;
import hcmute.edu.vn.discord.entity.jpa.Server;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelResponse {
    private Long id;
    private String name;
    private ChannelType type;
    private Boolean isPrivate;

    private Long serverId;
    private String serverName;

    private Long categoryId;
    private String categoryName;

    public static ChannelResponse from(Channel c) {
        if (c == null) return null;
        Server s = c.getServer();
        Category cat = c.getCategory();
        return ChannelResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .type(c.getType())
                .isPrivate(c.getIsPrivate())
                .serverId(s != null ? s.getId() : null)
                .serverName(s != null ? s.getName() : null)
                .categoryId(cat != null ? cat.getId() : null)
                .categoryName(cat != null ? cat.getName() : null)
                .build();
    }
}
