package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.dto.request.MessageRequest;
import hcmute.edu.vn.discord.dto.response.MessageResponse;
import hcmute.edu.vn.discord.entity.mongo.Message;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface MessageService {
    // Lưu và phát tán message được dựng sẵn
    Message sendMessage(Message message);
    // 1. Gửi tin nhắn (DTO MessageRequest đã chứa content, replyToId, attachments)
    MessageResponse createMessage(Long channelId, String username, MessageRequest request);

    // 2. Lấy danh sách tin nhắn (Đã bao gồm logic map ReplyTo và Attachments trong response)
    List<MessageResponse> getMessagesByChannel(Long channelId, String username, Pageable pageable);
    // 3. Sửa tin nhắn (Đã có logic chặn sửa tin đã xóa)
    MessageResponse editMessage(String messageId, String username, MessageRequest request);

    // 4. Xóa tin nhắn (Đã có logic cho phép Admin xóa)
    void deleteMessage(String messageId, String username);

    // 5. Thả cảm xúc (Reaction)
    void addReaction(String messageId, String username, String emoji);

    // 6. Gỡ cảm xúc
    void removeReaction(String messageId, String username, String emoji);
}