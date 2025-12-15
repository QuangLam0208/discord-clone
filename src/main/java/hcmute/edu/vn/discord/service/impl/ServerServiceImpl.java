package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.repository.ServerRepository;
import hcmute.edu.vn.discord.service.ServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ServerServiceImpl implements ServerService {

    @Autowired
    private ServerRepository serverRepository;

    @Override
    public Server createServer(Server server, Long ownerId) {
        // Logic: Set chủ sở hữu cho server rồi lưu
        // server.setOwnerId(ownerId);
        return serverRepository.save(server);
    }

    @Override
    public List<Server> getAllServers() {
        return serverRepository.findAll();
    }

    @Override
    public Server getServerById(Long id) {
        return serverRepository.findById(id).orElse(null);
    }
}