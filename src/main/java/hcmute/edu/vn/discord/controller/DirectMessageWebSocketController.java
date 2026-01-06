package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.request.DirectMessageRequest;
import hcmute.edu.vn.discord.dto.response.DirectMessageResponse;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.service.DirectMessageService;
import hcmute.edu.vn.discord.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Controller;
import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DirectMessageWebSocketController {

    private final DirectMessageService directMessageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;
    @Autowired
    private SimpUserRegistry simpUserRegistry;

    private void sendToOnlinePrincipalNames(User target, Object payload) {
        java.util.List<String> names = new java.util.ArrayList<>();
        if (target.getUsername() != null) names.add(target.getUsername());
        if (target.getEmail() != null) names.add(target.getEmail());
        for (String name : names) {
            if (name != null && simpUserRegistry.getUser(name) != null) {
                messagingTemplate.convertAndSendToUser(name, "/queue/dm", payload);
            }
        }
    }

    @MessageMapping("/dm.send")
    public void sendDirectMessage(@Payload DirectMessageRequest request, Principal principal) {
        if (principal == null) {
            log.error("Unauthorized: No principal found for DM");
            return;
        }
        var sender = userService.findByUsername(principal.getName()).orElse(null);
        if (sender == null) {
            log.error("Sender not found: {}", principal.getName());
            return;
        }

        // Check isMuted
        if (Boolean.TRUE.equals(sender.getIsMuted())) {
            // Optional: send error message back to user via separate queue or error channel
            // For now, just silently drop or log
            log.warn("User {} is muted and cannot send messages", sender.getUsername());
            return;
        }

        Long senderId = sender.getId();
        DirectMessageResponse response = directMessageService.sendMessage(senderId, request);

        userService.findById(request.getReceiverId()).ifPresent(receiver -> {
            sendToOnlinePrincipalNames(receiver, response);
        });

        sendToOnlinePrincipalNames(sender, response);
    }
}
