package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.dto.request.ServerRequest;
import hcmute.edu.vn.discord.entity.enums.ChannelType;
import hcmute.edu.vn.discord.entity.enums.EPermission;
import hcmute.edu.vn.discord.entity.enums.ServerStatus;
import hcmute.edu.vn.discord.entity.jpa.*;
import hcmute.edu.vn.discord.repository.*;
import hcmute.edu.vn.discord.service.ServerService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ServerServiceImpl implements ServerService {

    private final ServerRepository serverRepository;
    private final UserRepository userRepository;
    private final ServerMemberRepository serverMemberRepository;
    private final ServerRoleRepository serverRoleRepository;
    private final ChannelRepository channelRepository;
    private final PermissionRepository permissionRepository;


    @Override
    @Transactional
    public Server createServer(ServerRequest request) {
        // 1. Lấy User hiện tại (Owner)
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // 2. Tạo và lưu Server
        Server server = new Server();
        server.setName(request.getName());
        server.setDescription(request.getDescription());
        server.setIconUrl(request.getIconUrl());
        server.setOwner(owner);
        server.setStatus(ServerStatus.ACTIVE);

        // Lưu server trước để có ID gán cho Role/Channel
        Server savedServer = serverRepository.save(server);

        // 3. Chuẩn bị Map Permission để gán quyền nhanh
        Map<EPermission, Permission> permMap = bootstrapPermissions();

        // 4. Tạo Role @everyone
        ServerRole everyoneRole = new ServerRole();
        everyoneRole.setName("@everyone");
        everyoneRole.setServer(savedServer);
        everyoneRole.setPriority(0);
        everyoneRole.setPermissions(new HashSet<>(Set.of(
                permMap.get(EPermission.VIEW_CHANNELS),
                permMap.get(EPermission.SEND_MESSAGES),
                permMap.get(EPermission.READ_MESSAGE_HISTORY)
        )));
        serverRoleRepository.save(everyoneRole);

        // 5. Tạo Role Admin (Full quyền)
        ServerRole adminRole = new ServerRole();
        adminRole.setName("Admin");
        adminRole.setServer(savedServer);
        adminRole.setPriority(999); // Cao nhất
        adminRole.setPermissions(new HashSet<>(permMap.values()));
        serverRoleRepository.save(adminRole);

        // 6. Tạo Channel mặc định "General"
        Channel generalChannel = new Channel();
        generalChannel.setName("General");
        generalChannel.setType(ChannelType.TEXT);
        generalChannel.setIsPrivate(false);
        generalChannel.setServer(savedServer);
        channelRepository.save(generalChannel);

        // 7. Add Owner vào ServerMember với Role Admin
        ServerMember ownerMember = new ServerMember();
        ownerMember.setServer(savedServer);
        ownerMember.setUser(owner);
        ownerMember.setNickname(owner.getDisplayName());
        ownerMember.setJoinedAt(LocalDateTime.now());
        ownerMember.setIsBanned(false);

        ownerMember.setRoles(new HashSet<>(Set.of(everyoneRole, adminRole)));

        serverMemberRepository.save(ownerMember);

        return savedServer;
    }

    // Helper method để lấy Permission từ DB (Giữ lại từ code cũ)
    private Map<EPermission, Permission> bootstrapPermissions() {
        Map<EPermission, Permission> map = new EnumMap<>(EPermission.class);
        for (EPermission ep : EPermission.values()) {
            Permission perm = permissionRepository.findByCode(ep.name())
                    .orElseGet(() -> permissionRepository.save(
                            new Permission(null, ep.name(), ep.getDescription())
                    ));
            map.put(ep, perm);
        }
        return map;
    }

    @Override
    @Transactional
    public Server updateServer(Long serverId, ServerRequest request) {
        Server server = getServerById(serverId);
        server.setName(request.getName());
        server.setDescription(request.getDescription());
        if (request.getIconUrl() != null) {
            server.setIconUrl(request.getIconUrl());
        }
        return serverRepository.save(server);
    }

    @Override
    @Transactional
    public void deleteServer(Long serverId) {
        if (!serverRepository.existsById(serverId)) {
            throw new EntityNotFoundException("Server not found");
        }
        serverRepository.deleteById(serverId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Server> getAllServers() {
        return serverRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Server getServerById(Long serverId) {
        return serverRepository.findById(serverId)
                .orElseThrow(() -> new EntityNotFoundException("Server not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Server> getServersByCurrentUsername() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return serverMemberRepository.findByUserUsername(username).stream()
                .map(ServerMember::getServer)
                .toList();
    }
}