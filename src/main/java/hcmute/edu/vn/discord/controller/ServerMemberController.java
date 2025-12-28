package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.response.ServerMemberResponse;
import hcmute.edu.vn.discord.entity.enums.EPermission;
import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.entity.jpa.ServerMember;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.service.ServerMemberService;
import hcmute.edu.vn.discord.service.ServerService;
import hcmute.edu.vn.discord.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/servers/{serverId}/members")
@RequiredArgsConstructor
public class ServerMemberController {

    private static final Logger logger = LoggerFactory.getLogger(ServerMemberController.class);

    private final ServerMemberService serverMemberService;
    private final ServerService serverService;
    private final UserService userService;

    private User getCurrentUser(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new AccessDeniedException("Not authenticated");
        }
        return userService.findByUsername(auth.getName())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    private boolean isOwner(User user, Long serverId) {
        Server server = serverService.getServerById(serverId);
        return server != null && server.getOwner() != null &&
                server.getOwner().getId().equals(user.getId());
    }

    @PostMapping("/join")
    public ResponseEntity<ServerMemberResponse> joinServer(@PathVariable Long serverId, Authentication auth) {
        User user = getCurrentUser(auth);
        ServerMember member = serverMemberService.addMemberToServer(serverId, user.getId());
        logger.info("User {} joined server {}", user.getUsername(), serverId);
        return ResponseEntity.ok(ServerMemberResponse.from(member));
    }

    @PostMapping("/add")
    public ResponseEntity<ServerMemberResponse> addMember(@PathVariable Long serverId, @RequestParam Long userId,
                                                          Authentication auth) {
        User current = getCurrentUser(auth);
        if (!canManageMembers(current, serverId)) {
            throw new AccessDeniedException("Không có quyền quản lý thành viên");
        }
        ServerMember member = serverMemberService.addMemberToServer(serverId, userId);
        logger.info("User {} added user {} to server {}", current.getUsername(), userId, serverId);
        return ResponseEntity.ok(ServerMemberResponse.from(member));
    }

    @GetMapping("")
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

    @GetMapping("/{userId}/exists")
    public ResponseEntity<Boolean> checkMember(@PathVariable Long serverId, @PathVariable Long userId) {
        boolean exists = serverMemberService.isMember(serverId, userId);
        return ResponseEntity.ok(exists);
    }

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
                        EPermission.KICK_MEMBERS.name(),
                        EPermission.BAN_MEMBERS.name(),
                        EPermission.ADMIN.name()
                )
        );
    }
}
