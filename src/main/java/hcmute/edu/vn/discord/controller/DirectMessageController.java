package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.request.DirectMessageRequest;
import hcmute.edu.vn.discord.dto.response.DirectMessageResponse;
import hcmute.edu.vn.discord.service.DirectMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/direct-messages")
@RequiredArgsConstructor
public class DirectMessageController {

    private final DirectMessageService directMessageService;

    @PostMapping
    public ResponseEntity<DirectMessageResponse> sendMessage(
            Principal principal,
            @Valid @RequestBody DirectMessageRequest request) {
        if (principal == null || principal.getName() == null) {
            throw new IllegalArgumentException("Principal is null or unauthenticated");
        }
        Long senderId = Long.valueOf(principal.getName());
        return ResponseEntity.ok(
                directMessageService.sendMessage(senderId, request)
        );
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<List<DirectMessageResponse>> getMessages(
            @PathVariable String conversationId) {

        return ResponseEntity.ok(
                directMessageService.getMessages(conversationId)
        );
    }
}
