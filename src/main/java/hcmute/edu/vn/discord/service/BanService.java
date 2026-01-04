package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.entity.jpa.ServerBan;
import java.util.List;

public interface BanService {
    ServerBan banMember(Long serverId, Long userId, String reason, Long executorId);

    void unbanMember(Long serverId, Long userId);

    List<ServerBan> getBansByServerId(Long serverId);

    boolean isBanned(Long serverId, Long userId);
}
