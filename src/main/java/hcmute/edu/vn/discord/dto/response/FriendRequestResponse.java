package hcmute.edu.vn.discord.dto.response;

import hcmute.edu.vn.discord.entity.jpa.Friend;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestResponse {
    private Long id;

    private Long senderId;
    private String senderUsername;

    private Long recipientId;
    private String recipientUsername;

    private String status; // PENDING, ACCEPTED, DECLINED, BLOCKED
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

    public static FriendRequestResponse from(Friend f) {
        if (f == null) return null;
        return FriendRequestResponse.builder()
                .id(f.getId())
                .senderId(f.getRequester() != null ? f.getRequester().getId() : null)
                .senderUsername(f.getRequester() != null ? f.getRequester().getUsername() : null)
                .recipientId(f.getReceiver() != null ? f.getReceiver().getId() : null)
                .recipientUsername(f.getReceiver() != null ? f.getReceiver().getUsername() : null)
                .status(f.getStatus() != null ? f.getStatus().name() : null)
                .createdAt(f.getCreatedAt())
                .respondedAt(f.getRespondedAt())
                .build();
    }
}
