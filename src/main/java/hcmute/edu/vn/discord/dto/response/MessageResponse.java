package hcmute.edu.vn.discord.dto.response;

import hcmute.edu.vn.discord.entity.mongo.Message;
import lombok.Builder;
import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
@Builder
public class MessageResponse {
    private String id;
    private Long channelId;
    private String content;
    private Date createdAt;
    private boolean deleted;
    private boolean isEdited; // Thêm cờ để hiện chữ (đã chỉnh sửa)

    // Thông tin người gửi
    private Long senderId;
    private String senderName;
    private String senderAvatar;

    // Tính năng nâng cao
    private List<String> attachments; // Ảnh đính kèm
    private MessageResponse replyTo; // Object tin nhắn gốc (để hiển thị preview)
    private List<Message.Reaction> reactions;
}