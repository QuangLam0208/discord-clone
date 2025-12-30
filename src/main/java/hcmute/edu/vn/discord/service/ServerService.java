package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.dto.request.ServerRequest;
import hcmute.edu.vn.discord.dto.response.ServerResponse;
import hcmute.edu.vn.discord.dto.response.ServerDetailResponse;
import hcmute.edu.vn.discord.entity.jpa.Server;
import java.util.List;

public interface ServerService {
    ServerResponse createServer(ServerRequest request);
    ServerResponse updateServer(Long id, ServerRequest request);
    ServerDetailResponse getServerDetail(Long id);
    List<Server> getAllServers();
    Server getServerById(Long id);
    List<Server> getServersByCurrentUsername();
    void deleteServer(Long serverId);
}