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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
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
    @PreAuthorize("@serverAuth.canViewChannel(#channelId, principal.name)")
    public ResponseEntity<List<MessageResponse>> getMessagesByChannel(
            @PathVariable Long channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(messageService.getMessagesByChannel(channelId, principal.getName(), pageable));
    }

    /**
     * 2. Gửi tin nhắn mới vào Channel
     * URL: POST /api/channels/{channelId}/messages
     */
    @PostMapping("/channels/{channelId}/messages")
    @PreAuthorize("@serverAuth.canSendMessage(#channelId, principal.name)")
    public ResponseEntity<MessageResponse> createMessage(
            @PathVariable Long channelId,
            @Valid @RequestBody MessageRequest request,
            Principal principal) {

        return ResponseEntity.ok(messageService.createMessage(channelId, principal.getName(), request));
    }

    /**
     * 3. Chỉnh sửa tin nhắn
     * URL: PUT /api/messages/{id}
     * Ghi chú: quyền chỉnh sửa được kiểm tra ở service (chính chủ, chưa xóa, v.v.)
     */
    @PutMapping("/messages/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> updateMessage(
            @PathVariable String id,
            @Valid @RequestBody MessageRequest request,
            Principal principal) {

        return ResponseEntity.ok(messageService.editMessage(id, principal.getName(), request));
    }

    /**
     * 4. Xóa tin nhắn (Soft Delete)
     * URL: DELETE /api/messages/{id}
     * Ghi chú: quyền xóa (chính chủ/owner/ADMIN/MANAGE_MESSAGES) được kiểm tra ở service.
     */
    @DeleteMapping("/messages/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteMessage(
            @PathVariable String id,
            Principal principal) {

        messageService.deleteMessage(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    /**
     * 5. Thả Reaction (Emoji)
     * URL: POST /api/messages/{id}/reactions?emoji=👍
     * Yêu cầu quyền ADD_REACTIONS hoặc ADMIN, đồng thời phải xem được channel.
     */
    @PostMapping("/messages/{id}/reactions")
    @PreAuthorize("@serverAuth.canReactToMessage(#id, principal.name)")
    public ResponseEntity<?> addReaction(
            @PathVariable String id,
            @RequestParam String emoji,
            Principal principal) {

        messageService.addReaction(id, principal.getName(), emoji);
        return ResponseEntity.ok().build();
    }

    /**
     * 6. Gỡ Reaction
     * URL: DELETE /api/messages/{id}/reactions?emoji=👍
     * Yêu cầu quyền ADD_REACTIONS hoặc ADMIN, đồng thời phải xem được channel.
     */
    @DeleteMapping("/messages/{id}/reactions")
    @PreAuthorize("@serverAuth.canReactToMessage(#id, principal.name)")
    public ResponseEntity<?> removeReaction(
            @PathVariable String id,
            @RequestParam String emoji,
            Principal principal) {

        messageService.removeReaction(id, principal.getName(), emoji);
        return ResponseEntity.ok().build();
    }
}