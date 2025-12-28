package hcmute.edu.vn.discord.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class MessageRequest {
    // Allow empty content if an image is sent (Discord allows sending images without a caption)
    @Size(max = 2000, message = "Tin nhắn quá dài (tối đa 2000 ký tự)")
    private String content;

    private String replyToId; // ID of the message being replied to
    private List<String> attachments; // List of image/file URLs (Frontend uploads to Cloudinary/S3 then sends the link here)
}