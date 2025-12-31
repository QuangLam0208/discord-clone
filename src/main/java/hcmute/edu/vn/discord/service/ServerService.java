package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.dto.request.ServerRequest;
import hcmute.edu.vn.discord.entity.jpa.Server;
import java.util.List;

public interface ServerService {
    Server createServer(ServerRequest request);
    Server updateServer(Long id, ServerRequest request);
    Server getServerById(Long id);
    List<Server> getAllServers();
    List<Server> getServersByCurrentUsername();
    void deleteServer(Long serverId);
}