package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.entity.enums.ServerStatus;
import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.entity.jpa.ServerMember;
import hcmute.edu.vn.discord.entity.jpa.ServerRole;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.repository.ServerMemberRepository;
import hcmute.edu.vn.discord.repository.ServerRepository;
import hcmute.edu.vn.discord.repository.ServerRoleRepository;
import hcmute.edu.vn.discord.repository.UserRepository;
import hcmute.edu.vn.discord.service.ServerMemberService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ServerMemberServiceImpl implements ServerMemberService {

    private final ServerMemberRepository serverMemberRepository;
    private final ServerRepository serverRepository;
    private final UserRepository userRepository;
    private final ServerRoleRepository serverRoleRepository;

    @Override
    public ServerMember addMemberToServer(Long serverId, Long userId) {
        if (isMember(serverId, userId)) {
            return serverMemberRepository.findByServerIdAndUserId(serverId, userId)
                    .orElseThrow(() -> new IllegalStateException("Membership state inconsistent"));
        }

        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new IllegalArgumentException("Server not found"));

        if (server.getStatus() != ServerStatus.ACTIVE) {
            throw new IllegalStateException("Server không hoạt động");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        ServerRole memberRole = serverRoleRepository.findByServerIdAndName(serverId, "Member")
                .orElseThrow(() -> new IllegalStateException("Default role 'Member' not found"));

        ServerMember newMember = new ServerMember();
        newMember.setServer(server);
        newMember.setUser(user);
        newMember.setJoinedAt(LocalDateTime.now());
        newMember.setIsBanned(false);
        newMember.setNickname(user.getUsername());
        newMember.setRoles(new HashSet<>());
        newMember.getRoles().add(memberRole);

        return serverMemberRepository.save(newMember);
    }

    @Override
    public List<ServerMember> getMembersByServerId(Long serverId) {
        return serverMemberRepository.findByServerId(serverId);
    }

    @Override
    public boolean isMember(Long serverId, Long userId) {
        return serverMemberRepository.existsByServerIdAndUserId(serverId, userId);
    }

    @Override
    public void removeMember(Long serverId, Long userId) {
        ServerMember member = serverMemberRepository.findByServerIdAndUserId(serverId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));
        serverMemberRepository.delete(member);
    }
}