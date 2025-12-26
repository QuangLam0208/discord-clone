package hcmute.edu.vn.discord.dto.request;

import hcmute.edu.vn.discord.entity.enums.ChannelType;
import lombok.Data;

@Data
public class ChannelRequest {
    private String name;
    private ChannelType type;
    private Boolean isPrivate;
    private Long serverId;    // ID server muốn tạo kênh
    private Long categoryId;  // ID category (có thể null)
}