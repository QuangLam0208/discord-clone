package hcmute.edu.vn.discord.controller.api;

import hcmute.edu.vn.discord.dto.request.DirectMessageRequest;
import hcmute.edu.vn.discord.dto.request.EditMessageRequest;
import hcmute.edu.vn.discord.dto.response.ConversationResponse;
import hcmute.edu.vn.discord.dto.response.DirectMessageResponse;
import hcmute.edu.vn.discord.service.DirectMessageService;
import hcmute.edu.vn.discord.service.UserService;
import hcmute.edu.vn.discord.security.services.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/direct-messages")
@Tag(name = "Direct Messages", description = "Operations for private 1-on-1 messaging")
@RequiredArgsConstructor
@Slf4j
public class DirectMessageController {

    private final DirectMessageService directMessageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;

    // --- API MỚI: Lấy danh sách hội thoại cho sidebar ---
    @GetMapping("/conversations")
    public ResponseEntity<List<Map<String, Object>>> getConversations(@AuthenticationPrincipal UserDetailsImpl user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User is null or unauthenticated");
        }
        return ResponseEntity.ok(directMessageService.getConversationList(user.getId()));
    }

    @Operation(summary = "Send Direct Message")
    @PostMapping
    public ResponseEntity<DirectMessageResponse> sendMessage(
            @AuthenticationPrincipal UserDetailsImpl user,
            @Valid @RequestBody DirectMessageRequest request) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User is null or unauthenticated");
        }
        Long senderId = user.getId();
        DirectMessageResponse response = directMessageService.sendMessage(senderId, request);
        userService.findById(request.getReceiverId()).ifPresentOrElse(receiver -> {
            if (receiver.getUsername() != null) {
                messagingTemplate.convertAndSendToUser(
                        receiver.getUsername(),
                        "/queue/dm",
                        response
                );
            }
        }, () -> log.error("REST: Không tìm thấy người nhận ID: {}", request.getReceiverId()));
        if (user.getUsername() != null) {
            messagingTemplate.convertAndSendToUser(
                    user.getUsername(),
                    "/queue/dm",
                    response
            );
        }
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Conversation Messages")
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<Page<DirectMessageResponse>> getMessages(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserDetailsImpl user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User is null or unauthenticated");
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(
                directMessageService.getMessages(conversationId, user.getId(), pageable));
    }

    @Operation(summary = "Edit Direct Message")
    @PatchMapping("/messages/{id}")
    public ResponseEntity<DirectMessageResponse> editMessage(
            @PathVariable String id,
            @Valid @RequestBody EditMessageRequest request,
            @AuthenticationPrincipal UserDetailsImpl user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User is null or unauthenticated");
        }
        return ResponseEntity.ok(
                directMessageService.editMessage(id, user.getId(), request));
    }

    @Operation(summary = "Init Conversation")
    @PostMapping("/conversation/init")
    public ResponseEntity<ConversationResponse> initConversation(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam Long receiverId) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User is null or unauthenticated");
        }
        return ResponseEntity.ok(
                directMessageService.getOrCreateConversation(user.getId(), receiverId));
    }

    @Operation(summary = "Get or create DM conversation by friend userId")
    @GetMapping("/conversation/by-user/{friendId}")
    public ResponseEntity<ConversationResponse> getOrCreateConversationByUser(
            @PathVariable Long friendId,
            @AuthenticationPrincipal UserDetailsImpl user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User is null or unauthenticated");
        }
        ConversationResponse conv = directMessageService.getOrCreateConversation(user.getId(), friendId);
        return ResponseEntity.ok(conv);
    }
}