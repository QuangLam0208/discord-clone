package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.entity.jpa.ServerBan;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.repository.ServerBanRepository;
import hcmute.edu.vn.discord.repository.ServerRepository;
import hcmute.edu.vn.discord.repository.UserRepository;
import hcmute.edu.vn.discord.service.BanService;
import hcmute.edu.vn.discord.service.ServerMemberService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BanServiceImpl implements BanService {

    private final ServerBanRepository serverBanRepository;
    private final ServerMemberService serverMemberService;
    private final ServerRepository serverRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ServerBan banMember(Long serverId, Long userId, String reason, Long executorId) {
        // 1. Check if already banned
        if (serverBanRepository.existsByServerIdAndUserId(serverId, userId)) {
            throw new IllegalArgumentException("User is already banned from this server");
        }

        // 2. Remove member if exists
        try {
            serverMemberService.removeMember(serverId, userId);
        } catch (Exception ignored) {
        }

        // 3. Create Ban Record
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new EntityNotFoundException("Server not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        User executor = userRepository.findById(executorId)
                .orElseThrow(() -> new EntityNotFoundException("Executor not found"));

        ServerBan ban = ServerBan.builder()
                .server(server)
                .user(user)
                .bannedBy(executor)
                .reason(reason)
                .bannedAt(LocalDateTime.now())
                .build();

        return serverBanRepository.save(ban);
    }

    @Override
    @Transactional
    public void unbanMember(Long serverId, Long userId) {
        ServerBan ban = serverBanRepository.findByServerIdAndUserId(serverId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Ban record not found"));
        serverBanRepository.delete(ban);
    }

    @Override
    public List<ServerBan> getBansByServerId(Long serverId) {
        // Debugging all retrieval methods
        List<ServerBan> standard = serverBanRepository.findByServerId(serverId);
        List<ServerBan> nativeQuery = serverBanRepository.findByServerIdNative(serverId);

        System.out.println("DEBUG: Standard findByServerId found: " + standard.size());
        System.out.println("DEBUG: Native Query found: " + nativeQuery.size());

        if (!nativeQuery.isEmpty())
            return nativeQuery;
        return standard;
    }

    @Override
    public boolean isBanned(Long serverId, Long userId) {
        return serverBanRepository.existsByServerIdAndUserId(serverId, userId);
    }
}