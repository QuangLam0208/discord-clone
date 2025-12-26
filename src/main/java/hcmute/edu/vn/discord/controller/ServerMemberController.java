package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.entity.jpa.ServerMember;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.service.ServerMemberService;
import hcmute.edu.vn.discord.service.ServerService;
import hcmute.edu.vn.discord.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servers/{serverId}/members")
@RequiredArgsConstructor
public class ServerMemberController {

    private static final Logger logger = LoggerFactory.getLogger(ServerMemberController.class);

    private final ServerMemberService serverMemberService;
    private final ServerService serverService;
    private final UserService userService;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new AccessDeniedException("Not authenticated");
        }
        return userService.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private boolean isOwner(User user, Long serverId) {
        Server server = serverService.getServerById(serverId);
        return server != null && server.getOwner() != null &&
                server.getOwner().getId().equals(user.getId());
    }

    @PostMapping("/join")
    public ResponseEntity<?> joinServer(@PathVariable Long serverId) {
        try {
            User user = getCurrentUser();
            Server server = serverService.getServerById(serverId);
            if (server == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Server không tồn tại");

            if (serverMemberService.isMember(serverId, user.getId())) {
                return ResponseEntity.badRequest().body("Bạn đã là thành viên của server");
            }

            ServerMember member = serverMemberService.addMemberToServer(serverId, user.getId());
            if (member == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Không thể tham gia server");
            }

            logger.info("User {} joined server {}", user.getUsername(), serverId);
            return ResponseEntity.ok(member);
        } catch (Exception e) {
            logger.error("Lỗi khi join server {}", serverId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi server: " + e.getMessage());
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addMember(@PathVariable Long serverId, @RequestParam Long userId, Authentication auth) {
        try {
            User current = getCurrentUser();
            if (isOwner(current, serverId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Chỉ owner mới có thể thêm thành viên");
            }

            if (serverMemberService.isMember(serverId, userId)) {
                return ResponseEntity.badRequest().body("Người dùng đã là thành viên");
            }

            ServerMember member = serverMemberService.addMemberToServer(serverId, userId);
            if (member == null) return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Không thể thêm thành viên");

            logger.info("Owner {} added user {} to server {}", current.getUsername(), userId, serverId);
            return ResponseEntity.ok(member);
        } catch (Exception e) {
            logger.error("Lỗi khi add member to server {}", serverId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi server: " + e.getMessage());
        }
    }

    @GetMapping("")
    public ResponseEntity<?> listMembers(@PathVariable Long serverId) {
        try {
            User current = getCurrentUser();
            // Only allow if current user is member
            if (!serverMemberService.isMember(serverId, current.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn chưa tham gia server này");
            }

            List<ServerMember> members = serverMemberService.getMembersByServerId(serverId);
            return ResponseEntity.ok(members);
        } catch (Exception e) {
            logger.error("Lỗi khi lấy danh sách thành viên server {}", serverId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi server: " + e.getMessage());
        }
    }

    @GetMapping("/{userId}/exists")
    public ResponseEntity<?> checkMember(@PathVariable Long serverId, @PathVariable Long userId) {
        try {
            boolean exists = serverMemberService.isMember(serverId, userId);
            return ResponseEntity.ok(exists);
        } catch (Exception e) {
            logger.error("Lỗi khi check member {} on server {}", userId, serverId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi server: " + e.getMessage());
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> removeMember(@PathVariable Long serverId, @PathVariable Long userId) {
        try {
            User current = getCurrentUser();
            if (isOwner(current, serverId) && !current.getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không có quyền xoá thành viên này");
            }

            if (!serverMemberService.isMember(serverId, userId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Thành viên không tồn tại");
            }

            serverMemberService.removeMember(serverId, userId);
            logger.info("User {} removed member {} from server {}", current.getUsername(), userId, serverId);
            return ResponseEntity.ok("Đã xóa thành viên");
        } catch (Exception e) {
            logger.error("Lỗi khi xóa member {} trên server {}", userId, serverId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi server: " + e.getMessage());
        }
    }
}
