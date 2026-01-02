package hcmute.edu.vn.discord.controller.api.admin;

import hcmute.edu.vn.discord.dto.request.UpdateUserRolesRequest;
import hcmute.edu.vn.discord.entity.enums.ERole;
import hcmute.edu.vn.discord.entity.jpa.Role;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.repository.RoleRepository;
import hcmute.edu.vn.discord.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
public class AdminUserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    // GET /api/admin/users?page=&size=&q=
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

    // GET /api/admin/users/{id}
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    // POST /api/admin/users/{id}/ban
    @PostMapping("/{id}/ban")
    public User banUser(@PathVariable Long id) {
        User u = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        u.setIsActive(false);
        return userRepository.save(u);
    }

    // POST /api/admin/users/{id}/unban
    @PostMapping("/{id}/unban")
    public User unbanUser(@PathVariable Long id) {
        User u = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        u.setIsActive(true);
        return userRepository.save(u);
    }

    // POST /api/admin/users/{id}/roles
    @PostMapping("/{id}/roles")
    public User updateRoles(@PathVariable Long id, @Valid @RequestBody UpdateUserRolesRequest req) {
        User u = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        Set<Role> newRoles = new HashSet<>();
        List<String> roleNames = req.getRoles();
        if (roleNames != null) {
            for (String rName : roleNames) {
                try {
                    ERole e = ERole.valueOf(rName);
                    roleRepository.findByName(e).ifPresent(newRoles::add);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        u.setRoles(newRoles);
        return userRepository.save(u);
    }
}
