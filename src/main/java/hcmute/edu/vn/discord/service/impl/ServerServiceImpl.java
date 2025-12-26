package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.entity.enums.ChannelType;
import hcmute.edu.vn.discord.entity.enums.ServerStatus;
import hcmute.edu.vn.discord.entity.jpa.*;
import hcmute.edu.vn.discord.repository.*;
import hcmute.edu.vn.discord.service.ServerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ServerServiceImpl implements ServerService {

    private final ServerRepository serverRepository;
    private final ServerRoleRepository serverRoleRepository;
    private final ChannelRepository channelRepository;
    private final ServerMemberRepository serverMemberRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Server createServer(Server server, String userName) {
        User owner = userRepository.findByUsername(userName)
                .orElseThrow(() -> new RuntimeException("User not found"));

        server.setOwner(owner);
        server.setStatus(ServerStatus.ACTIVE);

        Server savedServer = serverRepository.save(server);

        ServerRole memberRole = new ServerRole();
        memberRole.setName("Member");
        memberRole.setPriority(1);
        memberRole.setServer(savedServer);
        memberRole.setPermissions(new ArrayList<>());
        serverRoleRepository.save(memberRole);

        ServerRole adminRole = new ServerRole();
        adminRole.setName("Admin");
        adminRole.setPriority(10);
        adminRole.setServer(savedServer);
        // TODO: Bạn có thể set full permission cho adminRole ở đây
        serverRoleRepository.save(adminRole);

        Channel generalChat = new Channel();
        generalChat.setName("general");
        generalChat.setType(ChannelType.TEXT);
        generalChat.setServer(savedServer);
        generalChat.setIsPrivate(false);
        channelRepository.save(generalChat);

        ServerMember ownerMember = new ServerMember();
        ownerMember.setServer(savedServer);
        ownerMember.setUser(owner);
        ownerMember.setNickname(owner.getDisplayName());
        ownerMember.setJoinedAt(LocalDateTime.now());
        ownerMember.setIsBanned(false);

        ownerMember.setRoles(new ArrayList<>());
        ownerMember.getRoles().add(adminRole);

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
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Server không tồn tại"));

        User user = userRepository.findByUsername(userName)
                .orElseThrow(() ->
                        new RuntimeException("User không tồn tại"));

        if (!server.getOwner().getId().equals(user.getId())) {
            throw new IllegalStateException("Bạn không có quyền xóa server này");
        }

        serverRepository.delete(server);
    }
}