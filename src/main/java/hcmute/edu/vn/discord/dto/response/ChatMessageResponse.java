package hcmute.edu.vn.discord.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ChatMessageResponse {
    private String id;
    private String content;
    private Long senderId;
    private String senderName; // Thêm tên người gửi để hiển thị
    private Long channelId;
    private Instant createdAt;
}
