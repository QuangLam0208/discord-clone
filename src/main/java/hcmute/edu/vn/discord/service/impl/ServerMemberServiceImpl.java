package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.entity.enums.EPermission;
import hcmute.edu.vn.discord.entity.enums.ServerStatus;
import hcmute.edu.vn.discord.entity.jpa.*;
import hcmute.edu.vn.discord.repository.PermissionRepository;
import hcmute.edu.vn.discord.repository.ServerMemberRepository;
import hcmute.edu.vn.discord.repository.ServerRepository;
import hcmute.edu.vn.discord.repository.ServerRoleRepository;
import hcmute.edu.vn.discord.repository.UserRepository;
import hcmute.edu.vn.discord.service.ServerMemberService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ServerMemberServiceImpl implements ServerMemberService {

    private final ServerMemberRepository serverMemberRepository;
    private final ServerRepository serverRepository;
    private final UserRepository userRepository;
    private final ServerRoleRepository serverRoleRepository;
    private final PermissionRepository permissionRepository;
    private final hcmute.edu.vn.discord.service.AuditLogService auditLogService;

    @Override
    @Transactional
    public ServerMember addMemberToServer(Long serverId, Long userId) {
        // Kiểm tra đã là thành viên chưa
        if (isMember(serverId, userId)) {
            // Logic cũ của bạn trả về data integrity exception, giữ nguyên
            throw new DataIntegrityViolationException("User is already a member");
        }

        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new EntityNotFoundException("Server not found"));

        if (server.getStatus() != ServerStatus.ACTIVE) {
            throw new IllegalStateException("Server không hoạt động");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Kiểm tra xem user có bị ban khỏi server không (soft ban flag)
        if (serverMemberRepository.findByServerIdAndUserId(serverId, userId)
                .map(ServerMember::getIsBanned).orElse(false)) {
            throw new IllegalStateException("User bị ban khỏi server");
        }

        // MẶC ĐỊNH: Gán role @everyone
        ServerRole everyoneRole = serverRoleRepository.findByServerIdAndName(serverId, "@everyone")
                .orElseGet(() -> {
                    // Self-healing: Nếu role @everyone bị thiếu, tự động tạo lại
                    ServerRole role = new ServerRole();
                    role.setName("@everyone");
                    role.setPriority(0);
                    role.setServer(server);

                    // Cấp quyền cơ bản mặc định
                    Set<Permission> defaults = new HashSet<>();
                    List<String> defaultCodes = List.of(
                            EPermission.VIEW_CHANNELS.name(),
                            EPermission.SEND_MESSAGES.name(),
                            EPermission.READ_MESSAGE_HISTORY.name(),
                            EPermission.CREATE_INVITE.name(),
                            EPermission.CHANGE_NICKNAME.name(),
                            EPermission.ADD_REACTIONS.name());

                    for (String code : defaultCodes) {
                        permissionRepository.findByCode(code).ifPresent(defaults::add);
                    }

                    role.setPermissions(defaults);
                    return serverRoleRepository.save(role);
                });

        ServerMember newMember = new ServerMember();
        newMember.setServer(server);
        newMember.setUser(user);
        newMember.setJoinedAt(LocalDateTime.now());
        newMember.setIsBanned(false);
        // Nếu user chưa có display name thì lấy username
        newMember.setNickname(user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());

        Set<ServerRole> roles = new HashSet<>();
        roles.add(everyoneRole);
        newMember.setRoles(roles);

        try {
            return serverMemberRepository.save(newMember);
        } catch (DataIntegrityViolationException e) {
            throw new DataIntegrityViolationException("User is already a member", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServerMember> getMembersByServerId(Long serverId) {
        return serverMemberRepository.findByServerId(serverId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMember(Long serverId, Long userId) {
        return serverMemberRepository.existsByServerIdAndUserId(serverId, userId);
    }

    @Override
    @Transactional
    public boolean removeMember(Long serverId, Long userId) {
        return serverMemberRepository.findByServerIdAndUserId(serverId, userId)
                .map(member -> {
                    String nickname = member.getNickname();
                    String username = member.getUser().getUsername();
                    Server server = member.getServer();

                    serverMemberRepository.delete(member);

                    // Audit Log
                    try {
                        String actorName = org.springframework.security.core.context.SecurityContextHolder.getContext()
                                .getAuthentication().getName();
                        User actor = userRepository.findByUsername(actorName).orElse(null);

                        // Determine if kick or leave
                        hcmute.edu.vn.discord.common.AuditLogAction action = hcmute.edu.vn.discord.common.AuditLogAction.MEMBER_KICK;
                        String desc = "Đã đuổi thành viên: " + (nickname != null ? nickname : username);

                        if (actor != null && actor.getId().equals(userId)) {
                            // User left
                            // We don't have MEMBER_LEAVE yet, so skip or add it.
                            // For now, let's just log it as a Kick by self? No, that's weird.
                            // Let's assume removeMember called by another is a KICK.
                            // If called by self, it is LEAVE.
                            // I'll skip logging leave for now to avoid confusion with Kick filter, unless
                            // required.
                            // Requirement said "MEMBER_KICK" is in action list.
                            return true;
                        }

                        auditLogService.logAction(server, actor, action,
                                userId.toString(), "MEMBER", desc);
                    } catch (Exception e) {
                    }

                    return true;
                })
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean userHasAnyPermission(Long serverId, Long userId, Set<String> requiredCodes) {
        return serverMemberRepository.findByServerIdAndUserId(serverId, userId)
                .map(member -> member.getRoles().stream()
                        .flatMap(r -> r.getPermissions().stream())
                        .map(Permission::getCode)
                        .anyMatch(requiredCodes::contains))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ServerRole> getRolesOfMember(Long serverId, Long userId) {
        ServerMember member = serverMemberRepository.findByServerIdAndUserId(serverId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Member not found"));

        boolean hasEveryone = member.getRoles().stream().anyMatch(r -> "@everyone".equalsIgnoreCase(r.getName()));
        if (!hasEveryone) {
            serverRoleRepository.findByServerIdAndName(serverId, "@everyone")
                    .ifPresent(everyone -> member.getRoles().add(everyone));
        }
        return member.getRoles().stream().toList();
    }

    @Transactional(readOnly = true)
    @Override
    public Set<Permission> getEffectivePermissions(Long serverId, Long userId) {
        ServerMember member = serverMemberRepository.findByServerIdAndUserId(serverId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Member not found"));
        Set<Permission> result = new HashSet<>();
        member.getRoles().forEach(role -> {
            if (role.getPermissions() != null) {
                result.addAll(role.getPermissions());
            }
        });
        return result;
    }
}