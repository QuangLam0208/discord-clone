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

    // --- THÔNG TIN NGƯỜI GỬI (QUAN TRỌNG CHO UI) ---
    private Long senderId;
    private String senderUsername;
    private String senderName;    // Tên hiển thị (DisplayName)
    private String senderAvatar;  // URL Avatar

    // --- THÔNG TIN NGƯỜI NHẬN ---
    private Long recipientId;
    private String recipientUsername;
    private String recipientName;
    private String recipientAvatar;

    private String status; // PENDING, ACCEPTED, DECLINED, BLOCKED
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

    // Loại request: INBOUND (người khác gửi mình) hay OUTBOUND (mình gửi đi)
    // Trường này sẽ được set thủ công trong Service/Controller tùy ngữ cảnh
    private String type;

    public static FriendRequestResponse from(Friend f) {
        if (f == null) return null;

        // Lấy thông tin người gửi (Requester)
        String sName = (f.getRequester() != null) ? f.getRequester().getDisplayName() : null;
        // Nếu chưa có DisplayName thì dùng Username làm tên hiển thị tạm
        if (sName == null && f.getRequester() != null) sName = f.getRequester().getUsername();

        // Lấy thông tin người nhận (Receiver)
        String rName = (f.getReceiver() != null) ? f.getReceiver().getDisplayName() : null;
        if (rName == null && f.getReceiver() != null) rName = f.getReceiver().getUsername();

        return FriendRequestResponse.builder()
                .id(f.getId())
                // Map Sender
                .senderId(f.getRequester() != null ? f.getRequester().getId() : null)
                .senderUsername(f.getRequester() != null ? f.getRequester().getUsername() : null)
                .senderName(sName)
                .senderAvatar(f.getRequester() != null ? f.getRequester().getAvatarUrl() : null) // Lấy Avatar

                // Map Recipient
                .recipientId(f.getReceiver() != null ? f.getReceiver().getId() : null)
                .recipientUsername(f.getReceiver() != null ? f.getReceiver().getUsername() : null)
                .recipientName(rName)
                .recipientAvatar(f.getReceiver() != null ? f.getReceiver().getAvatarUrl() : null)

                .status(f.getStatus() != null ? f.getStatus().name() : null)
                .createdAt(f.getCreatedAt())
                .respondedAt(f.getRespondedAt())
                .build();
    }
}