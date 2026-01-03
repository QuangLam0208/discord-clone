package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.request.AssignRolesRequest;
import hcmute.edu.vn.discord.dto.request.ServerRoleRequest;
import hcmute.edu.vn.discord.dto.response.ServerRoleResponse;
import hcmute.edu.vn.discord.entity.jpa.ServerRole;
import hcmute.edu.vn.discord.service.ServerRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servers/{serverId}/roles")
@RequiredArgsConstructor
public class ServerRoleController {

    private final ServerRoleService serverRoleService;

    // 1. Tạo Role mới
    @PostMapping
    @PreAuthorize("@serverAuth.canManageRole(#serverId, authentication.name)")
    public ResponseEntity<ServerRoleResponse> createRole(@PathVariable Long serverId,
            @RequestBody @Valid ServerRoleRequest request,
            Authentication authentication) {
        ServerRole role = serverRoleService.createRole(serverId, request, authentication.getName());
        return ResponseEntity.ok(ServerRoleResponse.from(role));
    }

    // 2. Cập nhật Role
    @PutMapping("/{roleId}")
    @PreAuthorize("@serverAuth.canManageRole(#serverId, authentication.name)")
    public ResponseEntity<ServerRoleResponse> updateRole(@PathVariable Long serverId,
            @PathVariable Long roleId,
            @RequestBody @Valid ServerRoleRequest request,
            Authentication authentication) {
        ServerRole role = serverRoleService.updateRole(serverId, roleId, request, authentication.getName());
        return ResponseEntity.ok(ServerRoleResponse.from(role));
    }

    // 3. Xóa Role
    @DeleteMapping("/{roleId}")
    @PreAuthorize("@serverAuth.canManageRole(#serverId, authentication.name)")
    public ResponseEntity<Void> deleteRole(@PathVariable Long serverId,
            @PathVariable Long roleId,
            Authentication authentication) {
        serverRoleService.deleteRole(serverId, roleId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    // 4. Lấy danh sách Role (Full quyền quản lý)
    @GetMapping
    @PreAuthorize("@serverAuth.canManageRole(#serverId, authentication.name)")
    public ResponseEntity<List<ServerRoleResponse>> getAllRoles(@PathVariable Long serverId,
            Authentication authentication) {
        return ResponseEntity.ok(serverRoleService.listRoles(serverId, authentication.getName()).stream()
                .map(ServerRoleResponse::from)
                .toList());
    }

    // 5. Lấy danh sách Role Public
    @GetMapping("/public")
    @PreAuthorize("@serverAuth.isMember(#serverId, authentication.name)")
    public ResponseEntity<List<ServerRoleResponse>> getPublicRoles(@PathVariable Long serverId,
            Authentication authentication) {
        return ResponseEntity.ok(serverRoleService.listRoles(serverId, authentication.getName()).stream()
                .map(ServerRoleResponse::from)
                .toList());
    }

    // 6. Gán Role cho thành viên
    @PostMapping("/assign")
    @PreAuthorize("@serverAuth.canManageRole(#serverId, authentication.name)")
    public ResponseEntity<Void> assignRolesToMember(@PathVariable Long serverId,
            @RequestBody @Valid AssignRolesRequest request,
            Authentication authentication) {
        serverRoleService.assignRoles(serverId, request, authentication.getName());
        return ResponseEntity.ok().build();
    }
}