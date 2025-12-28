package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.entity.enums.ChannelType;
import hcmute.edu.vn.discord.entity.enums.EPermission;
import hcmute.edu.vn.discord.entity.enums.ServerStatus;
import hcmute.edu.vn.discord.entity.jpa.*;
import hcmute.edu.vn.discord.repository.*;
import hcmute.edu.vn.discord.service.ServerService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ServerServiceImpl implements ServerService {

    private final ServerRepository serverRepository;
    private final ServerRoleRepository serverRoleRepository;
    private final ChannelRepository channelRepository;
    private final ServerMemberRepository serverMemberRepository;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Server createServer(Server server, String userName) {
        User owner = userRepository.findByUsername(userName)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        server.setOwner(owner);
        server.setStatus(ServerStatus.ACTIVE);
        Server savedServer = serverRepository.save(server);

        Map<EPermission, Permission> permMap = bootstrapPermissions();

        ServerRole memberRole = new ServerRole();
        memberRole.setName("Member");
        memberRole.setPriority(1);
        memberRole.setServer(savedServer);
        memberRole.setPermissions(Set.of(
                permMap.get(EPermission.VIEW_CHANNELS),
                permMap.get(EPermission.SEND_MESSAGES)
        ));
        serverRoleRepository.save(memberRole);

        ServerRole adminRole = new ServerRole();
        adminRole.setName("Admin");
        adminRole.setPriority(10);
        adminRole.setServer(savedServer);
        adminRole.setPermissions(new HashSet<>(permMap.values())); // full access
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
        ownerMember.setRoles(new HashSet<>(Set.of(adminRole)));

        serverMemberRepository.save(ownerMember);

        return savedServer;
    }

    @Override
    public List<Server> getAllServers() {
        return serverRepository.findAll();
    }

    @Override
    public Server getServerById(Long id) {
        return serverRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Server không tồn tại"));
    }

    @Override
    public List<Server> getServersByUsername(String userName){
        return serverRepository.findByMemberUsername(userName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteServer(Long serverId, String userName){
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new EntityNotFoundException("Server không tồn tại"));

        User user = userRepository.findByUsername(userName)
                .orElseThrow(() -> new EntityNotFoundException("User không tồn tại"));

        if (!server.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("Bạn không có quyền xóa server này");
        }

        serverRepository.delete(server);
    }

    private Map<EPermission, Permission> bootstrapPermissions() {
        Map<EPermission, Permission> map = new EnumMap<>(EPermission.class);
        for (EPermission ep : EPermission.values()) {
            Permission perm = permissionRepository.findByCode(ep.name())
                    .orElseGet(() -> permissionRepository.save(new Permission(null, ep.name(), ep.getDescription())));
            map.put(ep, perm);
        }
        return map;
    }
}