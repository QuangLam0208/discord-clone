package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.dto.request.AssignRolesRequest;
import hcmute.edu.vn.discord.dto.request.ServerRoleRequest;
import hcmute.edu.vn.discord.entity.jpa.Permission;
import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.entity.jpa.ServerMember;
import hcmute.edu.vn.discord.entity.jpa.ServerRole;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.repository.PermissionRepository;
import hcmute.edu.vn.discord.repository.ServerMemberRepository;
import hcmute.edu.vn.discord.repository.ServerRoleRepository;
import hcmute.edu.vn.discord.service.ServerRoleService;
import hcmute.edu.vn.discord.service.ServerService;
import hcmute.edu.vn.discord.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ServerRoleServiceImpl implements ServerRoleService {

    private static final String ROLE_ADMIN = "Admin";
    private static final String ROLE_EVERYONE = "@everyone";

    private final ServerService serverService;
    private final UserService userService;
    private final ServerRoleRepository serverRoleRepository;
    private final PermissionRepository permissionRepository;
    private final ServerMemberRepository serverMemberRepository;
    private final hcmute.edu.vn.discord.service.AuditLogService auditLogService;

    private Server requireOwner(Long serverId, String actorUsername) {
        Server server = serverService.getServerById(serverId);
        if (server.getOwner() == null || server.getOwner().getUsername() == null) {
            throw new AccessDeniedException("Server chưa có owner hợp lệ");
        }
        if (!server.getOwner().getUsername().equals(actorUsername)) {
            throw new AccessDeniedException("Chỉ owner server mới được quản lý role");
        }
        return server;
    }

    private Set<Permission> mapPermissionCodes(Set<String> codes) {
        Set<Permission> perms = new HashSet<>();
        if (codes == null || codes.isEmpty())
            return perms;
        for (String code : codes) {
            Permission p = permissionRepository.findByCode(code)
                    .orElseThrow(() -> new EntityNotFoundException("Permission not found: " + code));
            perms.add(p);
        }
        return perms;
    }

    @Transactional
    @Override
    public ServerRole createRole(Long serverId, ServerRoleRequest request, String actorUsername) {
        Server server = requireOwner(serverId, actorUsername);

        if (ROLE_ADMIN.equalsIgnoreCase(request.getName()) || ROLE_EVERYONE.equalsIgnoreCase(request.getName())) {
            throw new IllegalArgumentException("Không thể tạo role hệ thống với tên này");
        }

        ServerRole role = new ServerRole();
        role.setServer(server);
        role.setName(request.getName());
        role.setColor(request.getColor());
        role.setPriority(request.getPriority());
        role.setPermissions(mapPermissionCodes(request.getPermissionCodes()));

        ServerRole saved = serverRoleRepository.save(role);

        // Audit Log
        try {
            User actor = userService.findByUsername(actorUsername).orElse(null);
            auditLogService.logAction(server, actor, hcmute.edu.vn.discord.common.AuditLogAction.ROLE_CREATE,
                    saved.getId().toString(), "ROLE", "Tạo vai trò: " + saved.getName());
        } catch (Exception e) {
        }

        return saved;
    }

    @Transactional
    @Override
    public ServerRole updateRole(Long serverId, Long roleId, ServerRoleRequest request, String actorUsername) {
        requireOwner(serverId, actorUsername);

        ServerRole role = serverRoleRepository.findByIdAndServerId(roleId, serverId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));

        String oldName = role.getName();
        String oldColor = role.getColor();
        int oldPriority = role.getPriority();

        // Không cho đổi tên/xử lý role hệ thống
        if (ROLE_ADMIN.equalsIgnoreCase(role.getName()) || ROLE_EVERYONE.equalsIgnoreCase(role.getName())) {
            role.setPriority(request.getPriority());
            role.setPermissions(mapPermissionCodes(request.getPermissionCodes()));
            ServerRole updated = serverRoleRepository.save(role);

            // Audit Log (System role update)
            try {
                User actor = userService.findByUsername(actorUsername).orElse(null);
                auditLogService.logAction(role.getServer(), actor,
                        hcmute.edu.vn.discord.common.AuditLogAction.ROLE_UPDATE,
                        updated.getId().toString(), "ROLE", "Cập nhật vai trò hệ thống: " + role.getName());
            } catch (Exception e) {
            }

            return updated;
        }

        role.setName(request.getName());
        role.setColor(request.getColor());
        role.setPriority(request.getPriority());
        role.setPermissions(mapPermissionCodes(request.getPermissionCodes()));

        ServerRole updated = serverRoleRepository.save(role);

        // Audit Log
        try {
            User actor = userService.findByUsername(actorUsername).orElse(null);
            StringBuilder changes = new StringBuilder();
            if (!java.util.Objects.equals(oldName, updated.getName())) {
                changes.append("Tên: ").append(oldName).append(" -> ").append(updated.getName()).append("; ");
            }
            if (!java.util.Objects.equals(oldColor, updated.getColor())) {
                changes.append("Màu: ").append(oldColor).append(" -> ").append(updated.getColor()).append("; ");
            }
            if (oldPriority != updated.getPriority()) {
                changes.append("Ưu tiên: ").append(oldPriority).append(" -> ").append(updated.getPriority())
                        .append("; ");
            }

            if (changes.length() > 0) {
                auditLogService.logAction(updated.getServer(), actor,
                        hcmute.edu.vn.discord.common.AuditLogAction.ROLE_UPDATE,
                        updated.getId().toString(), "ROLE", changes.toString());
            } else {
                // Permissions update assumed if no property change recorded but method called
                auditLogService.logAction(updated.getServer(), actor,
                        hcmute.edu.vn.discord.common.AuditLogAction.ROLE_UPDATE,
                        updated.getId().toString(), "ROLE", "Cập nhật quyền hạn");
            }
        } catch (Exception e) {
        }

        return updated;
    }

    @Transactional
    @Override
    public void deleteRole(Long serverId, Long roleId, String actorUsername) {
        requireOwner(serverId, actorUsername);

        ServerRole role = serverRoleRepository.findByIdAndServerId(roleId, serverId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));

        if (ROLE_ADMIN.equalsIgnoreCase(role.getName()) || ROLE_EVERYONE.equalsIgnoreCase(role.getName())) {
            throw new IllegalStateException("Không thể xóa role hệ thống: " + role.getName());
        }

        String roleName = role.getName();
        Server server = role.getServer();
        String roleIdStr = role.getId().toString();

        serverRoleRepository.delete(role);

        // Audit Log
        try {
            User actor = userService.findByUsername(actorUsername).orElse(null);
            auditLogService.logAction(server, actor, hcmute.edu.vn.discord.common.AuditLogAction.ROLE_DELETE,
                    roleIdStr, "ROLE", "Xóa vai trò: " + roleName);
        } catch (Exception e) {
        }
    }

    @Transactional(readOnly = true)
    @Override
    public List<ServerRole> listRoles(Long serverId, String actorUsername) {
        // Yêu cầu: chỉ member của server mới xem được danh sách role
        User user = userService.findByUsername(actorUsername)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        serverMemberRepository.findByServerIdAndUserId(serverId, user.getId())
                .orElseThrow(() -> new AccessDeniedException("Bạn không phải thành viên server này"));

        List<ServerRole> roles = serverRoleRepository.findByServerId(serverId);
        // Initialize members collection to avoid lazy load exception when mapping to
        // DTO
        roles.forEach(role -> role.getMembers().size());
        return roles;
    }

    @Transactional
    @Override
    public ServerMember assignRoles(Long serverId, AssignRolesRequest request, String actorUsername) {
        requireOwner(serverId, actorUsername);

        ServerMember member = serverMemberRepository.findByServerIdAndUserId(serverId, request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("Member not found"));

        Set<ServerRole> newRoles = new HashSet<>();
        for (Long roleId : request.getRoleIds()) {
            ServerRole role = serverRoleRepository.findByIdAndServerId(roleId, serverId)
                    .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleId));
            newRoles.add(role);
        }

        // Luôn thêm @everyone
        ServerRole everyone = serverRoleRepository.findByServerIdAndName(serverId, ROLE_EVERYONE)
                .orElseThrow(() -> new EntityNotFoundException("Role '@everyone' chưa tồn tại"));
        newRoles.add(everyone);

        member.setRoles(newRoles);
        ServerMember saved = serverMemberRepository.save(member); // Fixed: was saving 'member', return value used for
                                                                  // consistency generally, though member is mutable

        // Audit Log
        try {
            User actor = userService.findByUsername(actorUsername).orElse(null);
            auditLogService.logAction(member.getServer(), actor,
                    hcmute.edu.vn.discord.common.AuditLogAction.MEMBER_ROLE_UPDATE,
                    member.getId().toString(), "MEMBER", "Cập nhật vai trò cho thành viên: "
                            + (member.getNickname() != null ? member.getNickname() : member.getUser().getUsername()));
        } catch (Exception e) {
        }

        return saved;
    }

    @Transactional(readOnly = true)
    @Override
    public List<ServerMember> getMembersByRole(Long serverId, Long roleId, String actorUsername) {
        User user = userService.findByUsername(actorUsername)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        // Check member
        serverMemberRepository.findByServerIdAndUserId(serverId, user.getId())
                .orElseThrow(() -> new AccessDeniedException("Bạn không phải thành viên server này"));

        // Verify role belongs to server
        serverRoleRepository.findByIdAndServerId(roleId, serverId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found in this server"));

        return serverMemberRepository.findByRoleId(roleId);
    }
}