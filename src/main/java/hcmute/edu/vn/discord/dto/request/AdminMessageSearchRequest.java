package hcmute.edu.vn.discord.dto.request;

import lombok.Data;

import java.time.Instant;

@Data
public class AdminMessageSearchRequest {
    private Long serverId;
    private Long channelId;
    private Long senderId;
    private String senderUsername;
    private String keyword;
    private String status = "all"; // all | active | deleted
    private Instant from;
    private Instant to;
    private int page = 0;
    private int size = 20;
}