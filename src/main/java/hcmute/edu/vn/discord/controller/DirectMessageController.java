package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.request.DirectMessageRequest;
import hcmute.edu.vn.discord.dto.request.EditMessageRequest;
import hcmute.edu.vn.discord.dto.response.DirectMessageResponse;
import hcmute.edu.vn.discord.service.DirectMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.List;

import hcmute.edu.vn.discord.security.services.UserDetailsImpl;

@RestController
@RequestMapping("/api/direct-messages")
@Tag(name = "Direct Messages", description = "Operations for private 1-on-1 messaging")
@RequiredArgsConstructor
public class DirectMessageController {

    private final DirectMessageService directMessageService;

    @Operation(summary = "Send Direct Message", description = "Send a private message to another user.")
    @PostMapping
    public ResponseEntity<DirectMessageResponse> sendMessage(
            @AuthenticationPrincipal UserDetailsImpl user,
            @Valid @RequestBody DirectMessageRequest request) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User is null or unauthenticated");
        }
        Long senderId = user.getId();
        return ResponseEntity.ok(
                directMessageService.sendMessage(senderId, request));
    }

    @Operation(summary = "Get Conversation Messages", description = "Retrieve history of a direct message conversation.")
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<List<DirectMessageResponse>> getMessages(
            @PathVariable String conversationId,
            @AuthenticationPrincipal UserDetailsImpl user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User is null or unauthenticated");
        }
        return ResponseEntity.ok(
                directMessageService.getMessages(conversationId, user.getId()));
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
}
