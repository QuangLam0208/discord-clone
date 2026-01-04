package hcmute.edu.vn.discord.dto.request;

import lombok.Data;

import java.time.Instant;

@Data
public class AdminDirectMessageSearchRequest {
    private String conversationId;
    private Long userAId;
    private Long userBId;
    private Long userId;
    private Long senderId;
    private String senderUsername;
    private String keyword;
    private String status = "all";
    private Instant from;
    private Instant to;
    private int page = 0;
    private int size = 20;
}