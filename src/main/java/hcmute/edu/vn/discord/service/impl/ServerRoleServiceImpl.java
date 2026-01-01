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
import org.springframework.dao.DataIntegrityViolationException;
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

    private void ensureRoleNameUnique(Long serverId, String name) {
        if (serverRoleRepository.existsByServerIdAndNameIgnoreCase(serverId, name)) {
            throw new DataIntegrityViolationException("Role name đã tồn tại trong server");
        }
    }

    private Set<Permission> mapPermissionCodes(Set<String> codes) {
        Set<Permission> perms = new HashSet<>();
        if (codes == null || codes.isEmpty()) return perms;
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
        // Không cho tạo trùng tên trong server
        ensureRoleNameUnique(serverId, request.getName());

        // Không cho tạo role hệ thống với cách copy tên
        if (ROLE_ADMIN.equalsIgnoreCase(request.getName()) || ROLE_EVERYONE.equalsIgnoreCase(request.getName())) {
            throw new IllegalArgumentException("Không thể tạo role hệ thống với tên này");
        }

        ServerRole role = new ServerRole();
        role.setServer(server);
        role.setName(request.getName());
        role.setPriority(request.getPriority());
        role.setPermissions(mapPermissionCodes(request.getPermissionCodes()));

        return serverRoleRepository.save(role);
    }

    @Transactional
    @Override
    public ServerRole updateRole(Long serverId, Long roleId, ServerRoleRequest request, String actorUsername) {
        requireOwner(serverId, actorUsername);

        ServerRole role = serverRoleRepository.findByIdAndServerId(roleId, serverId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));

        // Không cho đổi tên/xử lý role hệ thống
        if (ROLE_ADMIN.equalsIgnoreCase(role.getName()) || ROLE_EVERYONE.equalsIgnoreCase(role.getName())) {
            // Ở đây chỉ cho đổi priority/permissions, cấm đổi tên
            role.setPriority(request.getPriority());
            role.setPermissions(mapPermissionCodes(request.getPermissionCodes()));
            return serverRoleRepository.save(role);
        }

        // Nếu đổi tên, kiểm tra trùng
        if (!role.getName().equalsIgnoreCase(request.getName())) {
            ensureRoleNameUnique(serverId, request.getName());
        }

        role.setName(request.getName());
        role.setPriority(request.getPriority());
        role.setPermissions(mapPermissionCodes(request.getPermissionCodes()));

        return serverRoleRepository.save(role);
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

        serverRoleRepository.delete(role);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ServerRole> listRoles(Long serverId, String actorUsername) {
        // Yêu cầu: chỉ member của server mới xem được danh sách role
        User user = userService.findByUsername(actorUsername)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        serverMemberRepository.findByServerIdAndUserId(serverId, user.getId())
                .orElseThrow(() -> new AccessDeniedException("Bạn không phải thành viên server này"));

        return serverRoleRepository.findByServerId(serverId);
    }

    @Transactional
    @Override
    public ServerMember assignRoles(Long serverId, AssignRolesRequest request, String actorUsername) {
        Server server = requireOwner(serverId, actorUsername);

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
        return serverMemberRepository.save(member);
    }
}