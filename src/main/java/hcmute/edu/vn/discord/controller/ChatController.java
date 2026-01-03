package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.request.ChatMessageRequest;
import hcmute.edu.vn.discord.dto.request.MessageRequest;
import hcmute.edu.vn.discord.dto.response.ErrorResponse;
import hcmute.edu.vn.discord.dto.response.MessageResponse;
import hcmute.edu.vn.discord.security.servers.ServerAuth;
import hcmute.edu.vn.discord.service.MessageService;
import hcmute.edu.vn.discord.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final UserService userService;
    private final ServerAuth serverAuth;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageRequest chatMessage, Principal principal) {
        try {
            // Lấy username từ Principal (WS phải có auth)
            String username = principal != null ? principal.getName() : null;

            // Fallback dev/test: nếu không có principal, cho phép truyền senderId (không khuyến nghị production)
            if (username == null && chatMessage.getSenderId() != null) {
                var user = userService.findById(chatMessage.getSenderId()).orElse(null);
                if (user == null) {
                    log.error("Sender not found for ID: {}", chatMessage.getSenderId());
                    return;
                }
                username = user.getUsername();
            }

            if (username == null) {
                log.warn("WS sendMessage rejected: no principal");
                // Gửi lỗi về queue cá nhân nếu có principal
                return;
            }

            // Gate quyền gửi: FREEZE → chỉ OWNER/ADMIN; ACTIVE → OWNER hoặc có SEND_MESSAGES
            boolean allowed = serverAuth.canSendMessage(chatMessage.getChannelId(), username);
            if (!allowed) {
                log.warn("WS sendMessage rejected: user={} channel={} not allowed", username, chatMessage.getChannelId());
                // Thông báo lỗi cho client qua user-specific queue
                ErrorResponse err = new ErrorResponse(
                        LocalDateTime.now(),
                        HttpStatus.FORBIDDEN.value(),
                        "Forbidden",
                        "Bạn không được phép gửi tin nhắn trong kênh này",
                        "/ws/chat.sendMessage?channelId=" + chatMessage.getChannelId()
                );
                messagingTemplate.convertAndSendToUser(username, "/queue/errors", err);
                return;
            }

            // Dùng request object của MessageService để tái sử dụng logic
            MessageRequest serviceRequest = new MessageRequest();
            serviceRequest.setContent(chatMessage.getContent());
            serviceRequest.setAttachments(chatMessage.getAttachments());

            // Lưu và broadcast
            MessageResponse savedResponse = messageService.createMessage(chatMessage.getChannelId(), username, serviceRequest);
            String destination = "/topic/channel/" + chatMessage.getChannelId();
            messagingTemplate.convertAndSend(destination, savedResponse);

            log.info("WS message sent to {} by {}", destination, username);
        } catch (Exception e) {
            log.error("Error sending message via WS", e);
            // Tùy chọn: gửi lỗi về queue cá nhân
            if (principal != null) {
                ErrorResponse err = new ErrorResponse(
                        LocalDateTime.now(),
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Internal Server Error",
                        e.getMessage(),
                        "/ws/chat.sendMessage"
                );
                messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", err);
            }
        }
    }

    @GetMapping("/api/chat/history/{channelId}")
    @ResponseBody
    @PreAuthorize("@serverAuth.canViewChannel(#channelId, authentication.name)")
    public ResponseEntity<List<MessageResponse>> getChatHistory(
            @PathVariable Long channelId,
            @RequestParam(required = false) Long senderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {

        String username = principal != null ? principal.getName() : null;

        // Fallback dev/test nếu không có auth (không khuyến nghị production)
        if (username == null && senderId != null) {
            var user = userService.findById(senderId).orElse(null);
            if (user != null) {
                username = user.getUsername();
            }
        }

        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        return ResponseEntity.ok(messageService.getMessagesByChannel(channelId, username, pageable));
    }
}