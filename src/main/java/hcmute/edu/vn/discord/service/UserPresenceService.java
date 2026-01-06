package hcmute.edu.vn.discord.service;

import java.util.Set;

public interface UserPresenceService {
    void userConnected(Long userId);

    void userDisconnected(Long userId);

    boolean isUserOnline(Long userId);

    Set<Long> getOnlineUsers();
}
