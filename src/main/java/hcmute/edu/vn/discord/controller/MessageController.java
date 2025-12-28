package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.request.MessageRequest;
import hcmute.edu.vn.discord.dto.response.MessageResponse;
import hcmute.edu.vn.discord.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /**
     * 1. Lấy danh sách tin nhắn trong Channel (có phân trang)
     * URL: GET /api/channels/{channelId}/messages?page=0&size=20
     */
    @GetMapping("/channels/{channelId}/messages")
    public ResponseEntity<List<MessageResponse>> getMessagesByChannel(
            @PathVariable Long channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Sắp xếp tin nhắn mới nhất lên đầu (createdAt DESC)
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(messageService.getMessagesByChannel(channelId, pageable));
    }

    /**
     * 2. Gửi tin nhắn mới vào Channel
     * URL: POST /api/channels/{channelId}/messages
     * Body: { "content": "hello", "replyToId": "...", "attachments": ["url1", "url2"] }
     */
    @PostMapping("/channels/{channelId}/messages")
    public ResponseEntity<MessageResponse> createMessage(
            @PathVariable Long channelId,
            @Valid @RequestBody MessageRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(messageService.createMessage(channelId, authentication.getName(), request));
    }

    /**
     * 3. Chỉnh sửa tin nhắn
     * URL: PUT /api/messages/{id}
     */
    @PutMapping("/messages/{id}")
    public ResponseEntity<MessageResponse> updateMessage(
            @PathVariable String id,
            @Valid @RequestBody MessageRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(messageService.editMessage(id, authentication.getName(), request));
    }

    /**
     * 4. Xóa tin nhắn (Soft Delete)
     * URL: DELETE /api/messages/{id}
     */
    @DeleteMapping("/messages/{id}")
    public ResponseEntity<?> deleteMessage(
            @PathVariable String id,
            Authentication authentication) {

        messageService.deleteMessage(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    /**
     * 5. Thả Reaction (Emoji)
     * URL: POST /api/messages/{id}/reactions?emoji=👍
     */
    @PostMapping("/messages/{id}/reactions")
    public ResponseEntity<?> addReaction(
            @PathVariable String id,
            @RequestParam String emoji,
            Authentication authentication) {

        messageService.addReaction(id, authentication.getName(), emoji);
        return ResponseEntity.ok().build();
    }

    /**
     * 6. Gỡ Reaction
     * URL: DELETE /api/messages/{id}/reactions?emoji=👍
     */
    @DeleteMapping("/messages/{id}/reactions")
    public ResponseEntity<?> removeReaction(
            @PathVariable String id,
            @RequestParam String emoji,
            Authentication authentication) {

        messageService.removeReaction(id, authentication.getName(), emoji);
        return ResponseEntity.ok().build();
    }
}