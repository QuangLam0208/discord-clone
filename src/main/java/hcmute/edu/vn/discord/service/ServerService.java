package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.dto.request.ServerRequest;
import hcmute.edu.vn.discord.dto.response.ServerResponse;
import hcmute.edu.vn.discord.entity.jpa.Server;
import java.util.List;

public interface ServerService {
    Server createServer(ServerRequest request);
    Server updateServer(Long id, ServerRequest request);
    Server getServerById(Long id);
    List<Server> getAllServers();

    // Lấy servers theo username hiện tại, chỉ ACTIVE/FREEZE, kèm counts an toàn (không chạm LAZY)
    List<ServerResponse> getMyServersResponses();

    List<Server> getServersByCurrentUsername();
    void deleteServer(Long serverId);
}