package hcmute.edu.vn.discord.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AdminDirectMessageItemResponse {
    private String id;
    private String conversationId;

    private Long senderId;
    private String senderUsername;
    private String senderDisplayName;
    private String senderAvatarUrl;

    private Long receiverId;
    private String receiverUsername;   // NEW

    private String content;
    private boolean deleted;
    private boolean edited;
    private int reactionsCount;
    private int attachmentsCount;

    private Instant createdAt;

    // Participants (IDs)
    private Long userAId;
    private Long userBId;

    private String userAUsername;
    private String userBUsername;
}