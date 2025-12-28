package hcmute.edu.vn.discord.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class MessageRequest {
    // Cho phép để trống content nếu có gửi ảnh (Discord cho phép gửi ảnh không cần lời dẫn)
    @Size(max = 2000, message = "Tin nhắn quá dài (tối đa 2000 ký tự)")
    private String content;

    private String replyToId; // ID của tin nhắn đang được trả lời
    private List<String> attachments; // List URL ảnh/file (Frontend upload lên Cloudinary/S3 rồi gửi link về đây)
}