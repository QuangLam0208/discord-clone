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
        User sender = null;

        if (principal != null) {
            // Lấy thông tin user từ Principal
            String username = principal.getName();
            sender = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
        } else if (chatMessage.getSenderId() != null) {
            // Fallback cho testing
            sender = userService.findById(chatMessage.getSenderId()).orElse(null);
        }

        if (sender == null) {
            log.error("Tin nhắn bị từ chối: Không xác định được người gửi");
            return;
        }

        // Lưu tin nhắn vào MongoDB
        Message message = new Message();
        message.setChannelId(chatMessage.getChannelId());
        message.setSenderId(sender.getId());
        message.setContent(chatMessage.getContent());

        Message savedMessage = messageService.sendMessage(message);

        // Tạo response
        ChatMessageResponse response = ChatMessageResponse.builder()
                .id(savedMessage.getId())
                .content(savedMessage.getContent())
                .senderId(sender.getId())
                .senderName(sender.getDisplayName() != null ? sender.getDisplayName() : sender.getUsername())
                .channelId(savedMessage.getChannelId())
                .createdAt(savedMessage.getCreatedAt().toInstant())
                .build();

        // Gửi tin nhắn đến topic của channel đó
        // Client sẽ subscribe: /topic/channel/{channelId}
        String destination = "/topic/channel/" + chatMessage.getChannelId();
        messagingTemplate.convertAndSend(destination, response);

        log.info("Message sent to {}", destination);
    }
}
