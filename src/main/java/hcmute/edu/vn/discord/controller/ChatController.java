package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.request.ChatMessageRequest;
import hcmute.edu.vn.discord.dto.response.ChatMessageResponse;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.entity.mongo.Message;
import hcmute.edu.vn.discord.service.MessageService;
import hcmute.edu.vn.discord.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

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
            hcmute.edu.vn.discord.dto.request.MessageRequest serviceRequest = new hcmute.edu.vn.discord.dto.request.MessageRequest();
            serviceRequest.setContent(chatMessage.getContent());
            // chatMessage chưa hỗ trợ reply/attachments, có thể mở rộng sau

            var savedResponse = messageService.createMessage(chatMessage.getChannelId(), username, serviceRequest);

            // Convert sang ChatMessageResponse (nếu cần thiết, hoặc dùng chung
            // MessageResponse)
            // Ở đây ta map từ savedResponse sang ChatMessageResponse để giữ tương thích với
            // Client hiện tại
            ChatMessageResponse response = ChatMessageResponse.builder()
                    .id(savedResponse.getId())
                    .content(savedResponse.getContent())
                    .senderId(savedResponse.getSenderId())
                    .senderName(savedResponse.getSenderName())
                    .channelId(savedResponse.getChannelId())
                    .createdAt(savedResponse.getCreatedAt().toInstant())
                    .build();

            // Gửi tin nhắn đến topic của channel đó
            String destination = "/topic/channel/" + chatMessage.getChannelId();
            messagingTemplate.convertAndSend(destination, response);

            log.info("Message sent to {} by {}", destination, username);

        } catch (Exception e) {
            // Log lỗi và có thể gửi message lỗi về lại cho user qua user-specific queue
            // (nếu cần)
            log.error("Error sending message via WS: {}", e.getMessage());
        }
    }
}
