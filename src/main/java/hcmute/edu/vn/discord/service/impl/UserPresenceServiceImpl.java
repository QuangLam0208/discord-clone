package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.service.UserPresenceService;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserPresenceServiceImpl implements UserPresenceService {

    private final Set<Long> onlineUsers = ConcurrentHashMap.newKeySet();

    @Override
    public void userConnected(Long userId) {
        if (userId != null) {
            onlineUsers.add(userId);
        }
    }

    @Override
    public void userDisconnected(Long userId) {
        if (userId != null) {
            onlineUsers.remove(userId);
        }
    }

    @Override
    public boolean isUserOnline(Long userId) {
        return userId != null && onlineUsers.contains(userId);
    }

    @Override
    public Set<Long> getOnlineUsers() {
        return onlineUsers;
    }
}
