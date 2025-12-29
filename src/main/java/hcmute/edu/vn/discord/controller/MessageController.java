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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    // --- HELPER CHECK AUTHENTICATION  ---
    private String validateAndGetUsername(Authentication authentication) {
        if (authentication == null) {
            throw new BadCredentialsException("Lỗi xác thực: Token không hợp lệ hoặc thiếu.");
        }
        if (!authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new BadCredentialsException("Người dùng chưa đăng nhập.");
        }
        return authentication.getName();
    }

    /**
     * 1. Lấy danh sách tin nhắn trong Channel (có phân trang)
     * URL: GET /api/channels/{channelId}/messages?page=0&size=20
     */
    @GetMapping("/channels/{channelId}/messages")
    public ResponseEntity<List<MessageResponse>> getMessagesByChannel(
            @PathVariable Long channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) { // 1. Đảm bảo có Authentication

        // 2. Lấy username (sử dụng hàm helper validateAndGetUsername nếu đã có, hoặc tự check null)
        String username = validateAndGetUsername(authentication);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return ResponseEntity.ok(messageService.getMessagesByChannel(channelId, username, pageable));
    }

    /**
     * 2. Gửi tin nhắn mới vào Channel
     * URL: POST /api/channels/{channelId}/messages
     */
    @PostMapping("/channels/{channelId}/messages")
    public ResponseEntity<MessageResponse> createMessage(
            @PathVariable Long channelId,
            @Valid @RequestBody MessageRequest request,
            Authentication authentication) {

        String username = validateAndGetUsername(authentication);
        return ResponseEntity.ok(messageService.createMessage(channelId, username, request));
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

        String username = validateAndGetUsername(authentication);
        return ResponseEntity.ok(messageService.editMessage(id, username, request));
    }

    /**
     * 4. Xóa tin nhắn (Soft Delete)
     * URL: DELETE /api/messages/{id}
     */
    @DeleteMapping("/messages/{id}")
    public ResponseEntity<?> deleteMessage(
            @PathVariable String id,
            Authentication authentication) {

        String username = validateAndGetUsername(authentication);
        messageService.deleteMessage(id, username);
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

        messageService.addReaction(id, validateAndGetUsername(authentication), emoji);
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

        messageService.removeReaction(id, validateAndGetUsername(authentication), emoji);
        return ResponseEntity.ok().build();
    }
}