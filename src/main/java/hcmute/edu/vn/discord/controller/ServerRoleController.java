package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.request.AssignRolesRequest;
import hcmute.edu.vn.discord.dto.request.ServerRoleRequest;
import hcmute.edu.vn.discord.dto.response.ServerMemberResponse;
import hcmute.edu.vn.discord.dto.response.ServerRoleResponse;
import hcmute.edu.vn.discord.entity.jpa.ServerMember;
import hcmute.edu.vn.discord.entity.jpa.ServerRole;
import hcmute.edu.vn.discord.service.ServerRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/servers/{serverId}/roles")
@RequiredArgsConstructor
public class ServerRoleController {

    private final ServerRoleService serverRoleService;

    @PostMapping
    @PreAuthorize("@serverAuth.canManageRole(#serverId, principal.name)")
    public ResponseEntity<ServerRoleResponse> createRole(@PathVariable Long serverId,
                                                         @Valid @RequestBody ServerRoleRequest request,
                                                         Principal principal) {
        ServerRole created = serverRoleService.createRole(serverId, request, principal.getName());
        return ResponseEntity.ok(ServerRoleResponse.from(created));
    }

    @PutMapping("/{roleId}")
    @PreAuthorize("@serverAuth.canManageRole(#serverId, principal.name)")
    public ResponseEntity<ServerRoleResponse> updateRole(@PathVariable Long serverId,
                                                         @PathVariable Long roleId,
                                                         @Valid @RequestBody ServerRoleRequest request,
                                                         Principal principal) {
        ServerRole updated = serverRoleService.updateRole(serverId, roleId, request, principal.getName());
        return ResponseEntity.ok(ServerRoleResponse.from(updated));
    }

    @DeleteMapping("/{roleId}")
    @PreAuthorize("@serverAuth.canManageRole(#serverId, principal.name)")
    public ResponseEntity<Void> deleteRole(@PathVariable Long serverId,
                                           @PathVariable Long roleId,
                                           Principal principal) {
        serverRoleService.deleteRole(serverId, roleId, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("@serverAuth.canManageRole(#serverId, principal.name)")
    public ResponseEntity<List<ServerRoleResponse>> listRoles(@PathVariable Long serverId,
                                                              Principal principal) {
        List<ServerRoleResponse> data = serverRoleService.listRoles(serverId, principal.getName())
                .stream().map(ServerRoleResponse::from).toList();
        return ResponseEntity.ok(data);
    }

    // endpoint lấy danh sách Role cho user thường
    @GetMapping("/public")
    @PreAuthorize("@serverAuth.isMember(#serverId, principal.name)")
    public ResponseEntity<List<ServerRoleResponse>> getPublicRoles(@PathVariable Long serverId) {
        // Tận dụng hàm listRoles của service nhưng quyền truy cập rộng hơn
        List<ServerRoleResponse> data = serverRoleService.listRoles(serverId, null) // Hoặc gọi hàm riêng nếu cần
                .stream().map(ServerRoleResponse::from).toList();
        return ResponseEntity.ok(data);
    }

    @PostMapping("/assign")
    @PreAuthorize("@serverAuth.canManageRole(#serverId, principal.name)")
    public ResponseEntity<ServerMemberResponse> assignRoles(@PathVariable Long serverId,
                                                            @Valid @RequestBody AssignRolesRequest request,
                                                            Principal principal) {
        ServerMember member = serverRoleService.assignRoles(serverId, request, principal.getName());
        return ResponseEntity.ok(ServerMemberResponse.from(member));
    }
}