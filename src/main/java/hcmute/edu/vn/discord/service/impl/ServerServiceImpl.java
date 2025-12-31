package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.dto.request.ServerRequest;
import hcmute.edu.vn.discord.entity.enums.ChannelType;
import hcmute.edu.vn.discord.entity.enums.ServerStatus;
import hcmute.edu.vn.discord.entity.jpa.*;
import hcmute.edu.vn.discord.repository.*;
import hcmute.edu.vn.discord.service.ServerService;
import hcmute.edu.vn.discord.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ServerServiceImpl implements ServerService {

    private final ServerRepository serverRepository;
    private final UserRepository userRepository;
    private final ServerMemberRepository serverMemberRepository;
    private final ServerRoleRepository serverRoleRepository;
    private final ChannelRepository channelRepository;

    @Override
    @Transactional
    public Server createServer(ServerRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Server server = new Server();
        server.setName(request.getName());
        server.setDescription(request.getDescription());
        server.setIconUrl(request.getIconUrl());
        server.setOwner(owner);
        server.setStatus(ServerStatus.ACTIVE);

        server = serverRepository.save(server);

        // Tạo Role @everyone
        ServerRole everyoneRole = new ServerRole();
        everyoneRole.setName("@everyone");
        everyoneRole.setServer(server);
        everyoneRole.setPriority(0);

        everyoneRole.setPermissions(new HashSet<>());
        serverRoleRepository.save(everyoneRole);

        // Add Owner
        ServerMember ownerMember = new ServerMember();
        ownerMember.setServer(server);
        ownerMember.setUser(owner);
        ownerMember.setNickname(owner.getDisplayName());
        ownerMember.setJoinedAt(LocalDateTime.now());
        ownerMember.setIsBanned(false);
        ownerMember.setRoles(new HashSet<>(Set.of(everyoneRole)));
        serverMemberRepository.save(ownerMember);

        // Tạo Channel General
        Channel generalChannel = new Channel();
        generalChannel.setName("General");
        generalChannel.setType(ChannelType.TEXT);
        generalChannel.setIsPrivate(false);
        generalChannel.setServer(server);
        channelRepository.save(generalChannel);

        return server;
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