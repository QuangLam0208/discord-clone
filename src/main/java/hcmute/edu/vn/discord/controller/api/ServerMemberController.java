package hcmute.edu.vn.discord.controller.api;

import hcmute.edu.vn.discord.dto.response.PermissionResponse;
import hcmute.edu.vn.discord.dto.response.ServerBanResponse;
import hcmute.edu.vn.discord.dto.response.ServerMemberResponse;
import hcmute.edu.vn.discord.dto.response.ServerRoleResponse;
import hcmute.edu.vn.discord.entity.enums.EPermission;
import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.entity.jpa.ServerMember;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.service.BanService;
import hcmute.edu.vn.discord.service.ServerMemberService;
import hcmute.edu.vn.discord.service.ServerService;
import hcmute.edu.vn.discord.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/servers/{serverId}/members")
@RequiredArgsConstructor
public class ServerMemberController {

    private static final Logger logger = LoggerFactory.getLogger(ServerMemberController.class);

    private final ServerMemberService serverMemberService;
    private final ServerService serverService;
    private final UserService userService;
    private final BanService banService;

    private User getCurrentUser(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new AccessDeniedException("Not authenticated");
        }
        return userService.findByUsername(auth.getName())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    // 1. Join Server
    @PostMapping("/join")
    public ResponseEntity<ServerMemberResponse> joinServer(@PathVariable Long serverId, Authentication auth) {
        User user = getCurrentUser(auth);

        // Check if banned
        if (banService.isBanned(serverId, user.getId())) {
            throw new AccessDeniedException("Bạn đã bị cấm khỏi server này.");
        }

        // Gọi Service nhận về Entity
        ServerMember member = serverMemberService.addMemberToServer(serverId, user.getId());
        logger.info("User {} joined server {}", user.getUsername(), serverId);
        return ResponseEntity.ok(ServerMemberResponse.from(member));
    }

    // 2. Add Member (Admin add user khác)
    @PostMapping("/add")
    @PreAuthorize("@serverAuth.canManageMembers(#serverId, authentication.name)")
    public ResponseEntity<ServerMemberResponse> addMember(@PathVariable Long serverId, @RequestParam Long userId,
                                                          Authentication auth) {
        User current = getCurrentUser(auth);

        // Check if banned
        if (banService.isBanned(serverId, userId)) {
            throw new IllegalArgumentException("Người dùng này đang bị cấm khỏi server.");
        }

        ServerMember member = serverMemberService.addMemberToServer(serverId, userId);
        logger.info("User {} added user {} to server {}", current.getUsername(), userId, serverId);
        return ResponseEntity.ok(ServerMemberResponse.from(member));
    }

    // 3. List Members
    @GetMapping("")
    @PreAuthorize("@serverAuth.isMember(#serverId, authentication.name)")
    public ResponseEntity<List<ServerMemberResponse>> listMembers(@PathVariable Long serverId, Authentication auth) {
        User current = getCurrentUser(auth);
        if (!serverMemberService.isMember(serverId, current.getId())) {
            throw new AccessDeniedException("Không có quyền xem danh sách thành viên");
        }
        List<ServerMemberResponse> members = serverMemberService.getMembersByServerId(serverId)
                .stream()
                .map(ServerMemberResponse::from)
                .toList();

        return ResponseEntity.ok(members);
    }

    // 4. Check Member Exists
    @GetMapping("/{userId}/exists")
    public ResponseEntity<Boolean> checkMember(@PathVariable Long serverId, @PathVariable Long userId) {
        boolean exists = serverMemberService.isMember(serverId, userId);
        return ResponseEntity.ok(exists);
    }

    // 5. Remove Member (KICK)
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> removeMember(@PathVariable Long serverId, @PathVariable Long userId,
                                          Authentication auth) {
        User current = getCurrentUser(auth);
        // Người có quyền (owner/quyền quản trị) được xóa người khác; người thường chỉ tự rời
        if (!canManageMembers(current, serverId) && !current.getId().equals(userId)) {
            throw new AccessDeniedException("Không có quyền xóa thành viên");
        }

        boolean removed = serverMemberService.removeMember(serverId, userId);
        if (!removed) throw new EntityNotFoundException("Member not found");

        return ResponseEntity.noContent().build();
    }

    // 6. Get Bans
    @GetMapping("/bans")
    @PreAuthorize("@serverAuth.canBanMembers(#serverId, authentication.name)")
    public ResponseEntity<List<ServerBanResponse>> getBans(@PathVariable Long serverId) {
        try {
            System.out.println("DEBUG: Controller getBans called for server: " + serverId);
            List<hcmute.edu.vn.discord.entity.jpa.ServerBan> rawBans = banService.getBansByServerId(serverId);
            System.out.println("DEBUG: banService returned " + rawBans.size() + " raw bans");

            var bans = rawBans.stream()
                    .map(b -> {
                        try {
                            return ServerBanResponse.from(b);
                        } catch (Exception e) {
                            System.err.println("ERROR mapping ban ID " + b.getId() + ": " + e.getMessage());
                            e.printStackTrace();
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .toList();

            System.out.println("DEBUG: Controller returning " + bans.size() + " DTOs");
            return ResponseEntity.ok(bans);
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR in getBans: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // 7. Ban Member
    @PostMapping("/bans")
    @PreAuthorize("@serverAuth.canBanMembers(#serverId, authentication.name)")
    public ResponseEntity<ServerBanResponse> banMember(@PathVariable Long serverId,
                                                       @RequestBody Map<String, Object> body,
                                                       Authentication auth) {
        User current = getCurrentUser(auth);
        Long userId = Long.valueOf(body.get("userId").toString());
        String reason = body.getOrDefault("reason", "").toString();

        // Prevent banning self
        if (current.getId().equals(userId)) {
            throw new IllegalArgumentException("Không thể tự ban chính mình");
        }

        // Check hierarchy if needed (optional for now)

        var ban = banService.banMember(serverId, userId, reason, current.getId());
        return ResponseEntity.ok(ServerBanResponse.from(ban));
    }

    // 8. Unban Member
    @DeleteMapping("/bans/{userId}")
    @PreAuthorize("@serverAuth.canBanMembers(#serverId, authentication.name)")
    public ResponseEntity<?> unbanMember(@PathVariable Long serverId, @PathVariable Long userId) {
        banService.unbanMember(serverId, userId);
        return ResponseEntity.noContent().build();
    }

    // 9. Get My Roles
    @GetMapping("/me/roles")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ServerRoleResponse>> myRoles(@PathVariable Long serverId, Authentication auth) {
        User current = getCurrentUser(auth);
        if (!serverMemberService.isMember(serverId, current.getId())) {
            throw new AccessDeniedException("Bạn không phải thành viên server này");
        }
        var roles = serverMemberService.getRolesOfMember(serverId, current.getId()).stream()
                .map(ServerRoleResponse::from)
                .toList();
        return ResponseEntity.ok(roles);
    }

    // 10. Get My Permissions
    @GetMapping("/me/permissions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PermissionResponse>> myPermissions(@PathVariable Long serverId, Authentication auth) {
        User current = getCurrentUser(auth);
        if (!serverMemberService.isMember(serverId, current.getId())) {
            throw new AccessDeniedException("Bạn không phải thành viên server này");
        }
        var perms = serverMemberService.getEffectivePermissions(serverId, current.getId()).stream()
                .map(PermissionResponse::from)
                .toList();
        return ResponseEntity.ok(perms);
    }

    private boolean canManageMembers(User current, Long serverId) {
        Server server = serverService.getServerById(serverId);
        boolean isOwner = server != null
                && server.getOwner() != null
                && server.getOwner().getId() != null
                && server.getOwner().getId().equals(current.getId());

        if (isOwner) {
            return true;
        }

        return serverMemberService.userHasAnyPermission(
                serverId,
                current.getId(),
                Set.of(
                        EPermission.MANAGE_SERVER.name(),
                        EPermission.KICK_APPROVE_REJECT_MEMBERS.name(),
                        EPermission.BAN_MEMBERS.name(),
                        EPermission.ADMIN.name()
                )
        );
    }
}