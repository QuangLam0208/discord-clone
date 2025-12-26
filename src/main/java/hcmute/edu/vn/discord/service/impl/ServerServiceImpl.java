package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.entity.enums.ChannelType;
import hcmute.edu.vn.discord.entity.jpa.*;
import hcmute.edu.vn.discord.repository.*;
import hcmute.edu.vn.discord.service.ServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ServerServiceImpl implements ServerService {

    @Autowired
    private ServerRepository serverRepository;
    @Autowired
    private ServerRoleRepository serverRoleRepository;
    @Autowired
    private ChannelRepository channelRepository;
    @Autowired
    private ServerMemberRepository serverMemberRepository;
    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public Server createServer(Server server, String userName) {
        User owner = userRepository.findByUsername(userName)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. Set Owner và Lưu Server
        server.setOwner(owner);
        Server savedServer = serverRepository.save(server);

        // 2. Tạo Role mặc định cho Server (Admin & Member)
        // Role Member (Để addMemberToServer có cái mà dùng)
        ServerRole memberRole = new ServerRole();
        memberRole.setName("Member");
        memberRole.setPriority(1);
        memberRole.setServer(savedServer);
        memberRole.setPermissions(new ArrayList<>()); // List rỗng permission
        serverRoleRepository.save(memberRole);

        // Role Admin (Cho chủ server)
        ServerRole adminRole = new ServerRole();
        adminRole.setName("Admin");
        adminRole.setPriority(10); // Độ ưu tiên cao nhất
        adminRole.setServer(savedServer);
        // TODO: Bạn có thể set full permission cho adminRole ở đây
        serverRoleRepository.save(adminRole);

        // 3. Tạo Channel mặc định (General - Text & Voice)
        Channel generalChat = new Channel();
        generalChat.setName("general");
        generalChat.setType(ChannelType.TEXT);
        generalChat.setServer(savedServer);
        generalChat.setIsPrivate(false);
        channelRepository.save(generalChat);

        // 4. Add Owner vào làm thành viên đầu tiên (Role Admin)
        ServerMember ownerMember = new ServerMember();
        ownerMember.setServer(savedServer);
        ownerMember.setUser(owner);
        ownerMember.setNickname(owner.getDisplayName()); // Hoặc username
        ownerMember.setJoinedAt(LocalDateTime.now());
        ownerMember.setIsBanned(false);

        ownerMember.setRoles(new ArrayList<>());
        ownerMember.getRoles().add(adminRole); // Gán quyền to nhất

        serverMemberRepository.save(ownerMember);

        return savedServer;
    }

    @Override
    public List<Server> getAllServers() {
        return serverRepository.findAll();
    }

    @Override
    public Server getServerById(Long id) {
        return serverRepository.findById(id).orElse(null);
    }

    @Override
    public List<Server> getServersByUsername(String userName){
        return serverRepository.findByMemberUsername(userName);
    }

    @Override
    @Transactional
    public void deleteServer(Long serverId, String userName){
        // Lấy server
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Server không tồn tại"));

        // Lấy user đang đăng nhập
        User user = userRepository.findByUsername(userName)
                .orElseThrow(() ->
                        new RuntimeException("User không tồn tại"));

        if (!server.getOwner().getId().equals(user.getId())) {
            throw new IllegalStateException("Bạn không có quyền xóa server này");
        }

        // Xóa server
        serverRepository.delete(server);
    }
}