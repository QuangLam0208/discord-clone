package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.entity.jpa.Server;
import java.util.List;

public interface ServerService {
    Server createServer(Server server, String userName);
    List<Server> getAllServers();
    Server getServerById(Long id);
    List<Server> getServersByUsername(String userName);
    void deleteServer(Long serverId, String userName);
}