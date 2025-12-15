package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.entity.jpa.Server;
import java.util.List;

public interface ServerService {
    Server createServer(Server server, Long ownerId);
    List<Server> getAllServers();
    Server getServerById(Long id);
}