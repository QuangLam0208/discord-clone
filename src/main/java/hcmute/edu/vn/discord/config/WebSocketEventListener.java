package hcmute.edu.vn.discord.config;

import hcmute.edu.vn.discord.entity.enums.FriendStatus;
import hcmute.edu.vn.discord.entity.jpa.Friend;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.repository.FriendRepository;
import hcmute.edu.vn.discord.repository.UserRepository;
import hcmute.edu.vn.discord.service.UserPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final UserPresenceService userPresenceService;
    private final FriendRepository friendRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        Principal principal = event.getUser();
        if (principal != null) {
            String username = principal.getName();
            userRepository.findByUsername(username).ifPresent(user -> {
                log.info("User connected: {}", username);
                userPresenceService.userConnected(user.getId());
                broadcastStatus(user, true);
            });
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        if (principal != null) {
            String username = principal.getName();
            userRepository.findByUsername(username).ifPresent(user -> {
                log.info("User disconnected: {}", username);
                userPresenceService.userDisconnected(user.getId());
                broadcastStatus(user, false);
            });
        }
    }

    private void broadcastStatus(User user, boolean online) {
        // Find all accepted friends
        List<Friend> friends = friendRepository.findAllAcceptedFriendsOfUser(user.getId(), FriendStatus.ACCEPTED);

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", online ? "USER_ONLINE" : "USER_OFFLINE");
        payload.put("userId", user.getId());

        for (Friend f : friends) {
            User friendUser = f.getRequester().getId().equals(user.getId()) ? f.getReceiver() : f.getRequester();
            messagingTemplate.convertAndSendToUser(friendUser.getUsername(), "/queue/friends", payload);
        }
    }
}
