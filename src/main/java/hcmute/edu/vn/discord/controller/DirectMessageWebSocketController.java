package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.request.DirectMessageRequest;
import hcmute.edu.vn.discord.dto.response.DirectMessageResponse;
import hcmute.edu.vn.discord.service.DirectMessageService;
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
public class DirectMessageWebSocketController {

    private final DirectMessageService directMessageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;

    @MessageMapping("/dm.send")
    public void sendDirectMessage(@Payload DirectMessageRequest request, Principal principal) {
        try {
            if (principal == null) {
                log.error("Unauthorized: No principal found for DM");
                return;
            }

            String listSenderUsername = principal.getName();
            // We need sender ID but Principal only gives username (usually).
            // We need to resolve username -> ID.
            var sender = userService.findByUsername(listSenderUsername).orElse(null);
            if (sender == null) {
                log.error("Sender not found: {}", listSenderUsername);
                return;
            }

            Long senderId = sender.getId();

            // 1. Save message to DB
            DirectMessageResponse response = directMessageService.sendMessage(senderId, request);

            // 2. Send to receiver via WebSocket
            userService.findById(request.getReceiverId()).ifPresent(receiver -> {
                messagingTemplate.convertAndSendToUser(receiver.getUsername(), "/queue/dm", response);
            });

            // 3. Send back to sender (sync across devices)
            messagingTemplate.convertAndSendToUser(listSenderUsername, "/queue/dm", response);

            log.info("DM sent from {} to user ID {}", listSenderUsername, request.getReceiverId());

        } catch (Exception e) {
            log.error("Error sending DM via WS: {}", e.getMessage());
        }
    }
}
