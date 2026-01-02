package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.request.DirectMessageRequest;
import hcmute.edu.vn.discord.dto.request.EditMessageRequest;
import hcmute.edu.vn.discord.dto.response.ConversationResponse;
import hcmute.edu.vn.discord.dto.response.DirectMessageResponse;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.service.DirectMessageService;
import hcmute.edu.vn.discord.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import hcmute.edu.vn.discord.security.services.UserDetailsImpl;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/direct-messages")
@Tag(name = "Direct Messages", description = "Operations for private 1-on-1 messaging")
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class DirectMessageController {

    @Autowired
    private SimpUserRegistry simpUserRegistry;
    private final DirectMessageService directMessageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;

    @Operation(summary = "Send Direct Message", description = "Send a private message to another user.")
    @PostMapping
    public ResponseEntity<DirectMessageResponse> sendMessage(
            @AuthenticationPrincipal UserDetailsImpl user,
            @Valid @RequestBody DirectMessageRequest request) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User is null or unauthenticated");
        }
        Long senderId = user.getId();
        DirectMessageResponse response = directMessageService.sendMessage(senderId, request);

        // 1. Receiver
        userService.findById(request.getReceiverId()).ifPresentOrElse(receiver -> {
            log.info("REST: DM from {} to Receiver Username: {}", user.getUsername(), receiver.getUsername());
            // gửi đến tất cả principal name có thể (username/email) nhưng chỉ nếu đang online
            sendToOnlinePrincipalNames(receiver, response);
        }, () -> log.error("REST: Receiver not found with ID: {}", request.getReceiverId()));

        // 2. Sender (multi-device)
        // sender cũng có thể dùng email làm principal; gửi theo registry
        var senderPrincipals = List.of(user.getUsername());
        for (String name : senderPrincipals) {
            if (simpUserRegistry.getUser(name) != null) {
                messagingTemplate.convertAndSendToUser(name, "/queue/dm", response);
            }
        }

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Conversation Messages", description = "Retrieve history of a direct message conversation.")
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

    @Operation(summary = "Edit Direct Message", description = "Modify an existing direct message.")
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

    @Operation(summary = "Init Conversation", description = "Get or create a conversation with a user by their ID.")
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
    private void sendToOnlinePrincipalNames(User target, Object payload) {
        List<String> names = new ArrayList<>();
        if (target.getUsername() != null) names.add(target.getUsername());
        if (target.getEmail() != null) names.add(target.getEmail());

        for (String name : names) {
            if (name != null && simpUserRegistry.getUser(name) != null) {
                messagingTemplate.convertAndSendToUser(name, "/queue/dm", payload);
                log.info("Sent DM to principal: {}", name);
            }
        }
    }
}
