package hcmute.edu.vn.discord.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AdminMessageItemResponse {
    private String id;
    private Long serverId;
    private String serverName;
    private Long channelId;
    private String channelName;
    private Long senderId;
    private String senderUsername;
    private String senderDisplayName;
    private String senderAvatarUrl;
    private String content;
    private boolean deleted;
    private boolean edited;
    private int attachmentsCount;
    private int reactionsCount;
    private Instant createdAt;
}