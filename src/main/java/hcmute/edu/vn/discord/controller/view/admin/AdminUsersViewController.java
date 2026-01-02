package hcmute.edu.vn.discord.controller.view.admin;

import hcmute.edu.vn.discord.entity.enums.ERole;
import hcmute.edu.vn.discord.entity.jpa.Role;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.repository.RoleRepository;
import hcmute.edu.vn.discord.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasAnyRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUsersViewController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @GetMapping({"", "/"})
    public String list(Model model,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "") String keyword) {
        int size = 10;
        PageRequest pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<User> userPage = keyword.isBlank()
                ? userRepository.findAll(pageable)
                : userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword, pageable);

        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("allRoles", roleRepository.findAll());
        return "admin/users";
    }

    @PostMapping("/update")
    public String updateUser(@RequestParam Long id,
                             @RequestParam String displayName,
                             @RequestParam(required = false) List<String> roleNames) {
        User user = userRepository.findById(id).orElseThrow();
        user.setDisplayName(displayName);

        if (roleNames != null) {
            Set<Role> newRoles = new HashSet<>();
            for (String rName : roleNames) {
                try {
                    ERole eRole = ERole.valueOf(rName);
                    roleRepository.findByName(eRole).ifPresent(newRoles::add);
                } catch (IllegalArgumentException ignored) {}
            }
            user.setRoles(newRoles);
        }

        userRepository.save(user);
        return "redirect:/admin/users";
    }

    @PostMapping("/ban")
    public String banUser(@RequestParam Long id) {
        User user = userRepository.findById(id).orElseThrow();
        user.setIsActive(false);
        userRepository.save(user);
        return "redirect:/admin/users";
    }
}