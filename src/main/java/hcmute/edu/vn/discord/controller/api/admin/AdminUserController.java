package hcmute.edu.vn.discord.controller.api.admin;

import hcmute.edu.vn.discord.dto.request.UpdateUserAdminRequest;
import hcmute.edu.vn.discord.dto.response.UserDetailResponse;
import hcmute.edu.vn.discord.entity.enums.EAuditAction;
import hcmute.edu.vn.discord.entity.enums.ERole;
import hcmute.edu.vn.discord.entity.jpa.Role;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.repository.RoleRepository;
import hcmute.edu.vn.discord.repository.UserRepository;
import hcmute.edu.vn.discord.service.AuditLogMongoService;
import hcmute.edu.vn.discord.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ADMIN')")
public class AdminUserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;
    private final AuditLogMongoService auditLogService;

    @GetMapping
    public Page<User> listUsers(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String q) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        if (q == null || q.isBlank()) {
            return userRepository.findAll(pageable);
        }
        return userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(q, q, pageable);
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @PostMapping("/{id}/ban")
    public User banUser(@PathVariable Long id, Authentication authentication) {
        User u = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        u.setIsActive(false);
        User saved = userRepository.save(u);

        // Gửi sự kiện force-logout đến user đó (user destination)
        // JwtChannelInterceptor nên set Principal.getName() = username từ JWT
        Map<String, String> payload = Map.of("type", "FORCE_LOGOUT", "reason", "BANNED");
        messagingTemplate.convertAndSendToUser(saved.getUsername(), "/queue/force-logout", payload);

        auditLogService.log(authentication.getName(),
                hcmute.edu.vn.discord.entity.enums.EAuditAction.ADMIN_BAN_USER.name(), saved.getUsername(),
                "Banned user id: " + id);

        return saved;
    }

    @PostMapping("/{id}/unban")
    public User unbanUser(@PathVariable Long id, Authentication authentication) {
        User u = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        u.setIsActive(true);
        u.setBannedUntil(null); // Clear temporary ban
        User saved = userRepository.save(u);
        auditLogService.log(authentication.getName(),
                hcmute.edu.vn.discord.entity.enums.EAuditAction.ADMIN_UNBAN_USER.name(), saved.getUsername(),
                "Unbanned user id: " + id);
        return saved;
    }

    // (Giữ nếu bạn còn dùng ở nơi khác)
    @PostMapping("/{id}/roles")
    public User updateRoles(@PathVariable Long id, @Valid @RequestBody UpdateUserAdminRequest req) {
        User u = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        applyRoles(u, req.getRoles());
        return userRepository.save(u);
    }

    @PatchMapping("/{id}")
    public User updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserAdminRequest req,
            Authentication authentication) {
        User u = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (req.getDisplayName() != null)
            u.setDisplayName(req.getDisplayName());
        if (req.getRoles() != null)
            applyRoles(u, req.getRoles());
        User saved = userRepository.save(u);
        auditLogService.log(authentication.getName(),
                EAuditAction.ADMIN_UPDATE_USER.name(), saved.getUsername(),
                "Updated user id: " + id);
        return saved;
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<UserDetailResponse> getUserDetail(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserDetail(id));
    }

    private void applyRoles(User u, List<String> roleNames) {
        Set<Role> newRoles = new HashSet<>();
        if (roleNames != null) {
            for (String rName : roleNames) {
                try {
                    ERole e = ERole.valueOf(rName);
                    roleRepository.findByName(e).ifPresent(newRoles::add);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        u.setRoles(newRoles);
    }
}