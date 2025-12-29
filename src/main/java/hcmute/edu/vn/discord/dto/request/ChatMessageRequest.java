package hcmute.edu.vn.discord.dto.request;

import lombok.Data;

@Data
public class ChatMessageRequest {
    private String content;
    private Long channelId;

    // Optional: dùng cho testing hoặc khi chưa integrate auth qua WebSocket
    private Long senderId;
}
