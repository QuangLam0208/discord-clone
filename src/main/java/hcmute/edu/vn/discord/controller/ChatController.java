package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.request.ChatMessageRequest;
import hcmute.edu.vn.discord.dto.request.MessageRequest;
import hcmute.edu.vn.discord.service.MessageService;
import hcmute.edu.vn.discord.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import hcmute.edu.vn.discord.dto.response.MessageResponse;
import java.util.List;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final UserService userService;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageRequest chatMessage, Principal principal) {
        try {
            String username;
            if (principal != null) {
                username = principal.getName();
            } else if (chatMessage.getSenderId() != null) {
                // Fallback chỉ dùng cho dev/test environment không có auth
                var user = userService.findById(chatMessage.getSenderId()).orElse(null);
                if (user == null) {
                    log.error("Sender not found for ID: {}", chatMessage.getSenderId());
                    return;
                }
                username = user.getUsername();
            } else {
                log.error("Unauthorized: No principal and no senderId provided");
                return;
            }

            // Dùng request object của MessageService để tái sử dụng logic kiểm tra quyền
            MessageRequest serviceRequest = new MessageRequest();
            serviceRequest.setContent(chatMessage.getContent());
            // chatMessage chưa hỗ trợ reply/attachments, có thể mở rộng sau

            var savedResponse = messageService.createMessage(chatMessage.getChannelId(), username, serviceRequest);

            // Gửi tin nhắn đến topic của channel đó
            String destination = "/topic/channel/" + chatMessage.getChannelId();
            messagingTemplate.convertAndSend(destination, savedResponse);

            log.info("Message sent to {} by {}", destination, username);

        } catch (Exception e) {
            // Log lỗi và có thể gửi message lỗi về lại cho user qua user-specific queue
            // (nếu cần)
            log.error("Error sending message via WS: {}", e.getMessage());
        }
    }

    @GetMapping("/api/chat/history/{channelId}")
    @ResponseBody
    public ResponseEntity<List<MessageResponse>> getChatHistory(
            @PathVariable Long channelId,
            @RequestParam(required = false) Long senderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {

        String username = null;
        if (principal != null) {
            username = principal.getName();
        } else if (senderId != null) {
            var user = userService.findById(senderId).orElse(null);
            if (user != null) {
                username = user.getUsername();
            }
        }

        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt")); // thời gian
        return ResponseEntity.ok(messageService.getMessagesByChannel(channelId, username, pageable));
    }
}
