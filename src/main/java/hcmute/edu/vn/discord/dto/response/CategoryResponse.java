package hcmute.edu.vn.discord.dto.response;

import hcmute.edu.vn.discord.entity.jpa.Category;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CategoryResponse {
    Long id;
    Long serverId;
    String name;
    int channelCount;

    public static CategoryResponse from(Category c) {
        if (c == null) return null;
        return CategoryResponse.builder()
                .id(c.getId())
                .serverId(c.getServer() != null ? c.getServer().getId() : null)
                .name(c.getName())
                .channelCount(c.getChannels() != null ? c.getChannels().size() : 0)
                .build();
    }
}