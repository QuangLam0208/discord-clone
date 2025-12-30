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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Channel Messages", description = "Operations for messages within a specific server channel")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    /**
     * 1. Lấy danh sách tin nhắn trong Channel (có phân trang)
     * URL: GET /api/channels/{channelId}/messages?page=0&size=20
     */
    @Operation(summary = "Get Messages by Channel", description = "Retrieve a paginated list of messages for a given channel.")
    @GetMapping("/channels/{channelId}/messages")
    @PreAuthorize("@serverAuth.canViewChannel(#channelId, authentication.name)")
    public ResponseEntity<List<MessageResponse>> getMessagesByChannel(
            @PathVariable Long channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(messageService.getMessagesByChannel(channelId, authentication.getName(), pageable));
    }

    /**
     * 2. Gửi tin nhắn mới vào Channel
     * URL: POST /api/channels/{channelId}/messages
     */
    @Operation(summary = "Send Message", description = "Send a new message to a specific channel.")
    @PostMapping("/channels/{channelId}/messages")
    @PreAuthorize("@serverAuth.canSendMessage(#channelId, authentication.name)")
    public ResponseEntity<MessageResponse> createMessage(
            @PathVariable Long channelId,
            @Valid @RequestBody MessageRequest request,
            Authentication authentication) {

        MessageResponse response = messageService.createMessage(channelId, authentication.getName(), request);

        // Broadcast to WebSocket subscribers
        messagingTemplate.convertAndSend("/topic/channel/" + channelId, response);

        return ResponseEntity.ok(response);
    }

    /**
     * 3. Chỉnh sửa tin nhắn
     * URL: PUT /api/messages/{id}
     * Ghi chú: quyền chỉnh sửa được kiểm tra ở service (chính chủ, chưa xóa, v.v.)
     */
    @Operation(summary = "Update Message", description = "Edit the content of an existing message.")
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
     * Ghi chú: quyền xóa (chính chủ/owner/ADMIN/MANAGE_MESSAGES) được kiểm tra ở
     * service.
     */
    @Operation(summary = "Delete Message", description = "Soft delete a message by ID.")
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
    @Operation(summary = "Add Reaction", description = "Add an emoji reaction to a message.")
    @PostMapping("/messages/{id}/reactions")
    @PreAuthorize("@serverAuth.canReactToMessage(#id, authentication.name)")
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
     * Yêu cầu quyền ADD_REACTIONS hoặc ADMIN, đồng thời phải xem được channel.
     */
    @Operation(summary = "Remove Reaction", description = "Remove an emoji reaction from a message.")
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