package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.response.FriendRequestResponse;
import hcmute.edu.vn.discord.dto.response.FriendResponse;
import hcmute.edu.vn.discord.entity.enums.ERole;
import hcmute.edu.vn.discord.entity.jpa.Role;
import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.repository.RoleRepository;
import hcmute.edu.vn.discord.repository.ServerRepository;
import hcmute.edu.vn.discord.repository.UserRepository;
import hcmute.edu.vn.discord.service.FriendService;
import hcmute.edu.vn.discord.service.ServerService;
import hcmute.edu.vn.discord.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ViewController
 * -------------------
 * Controller điều hướng chính cho các trang giao diện (Thymeleaf).
 * Bao gồm các chức năng:
 * 1. Login/Register/Home
 * 2. Friends & DM
 * 3. Profile (Upload Avatar)
 * 4. Admin Users (CRUD)
 * 5. Admin Servers (CRUD)
 *
 * Đồng bộ với cơ chế của nhánh ui-js để tránh vòng lặp:
 * - /login và /register luôn trả về view tương ứng, KHÔNG tự redirect sang /home.
 * - / (root) nếu đã xác thực thì redirect /home, chưa xác thực thì trả về login.
 */
@Controller
@RequiredArgsConstructor
public class ViewController {

    private final UserService userService;
    private final FriendService friendService;
    private final ServerService serverService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ServerRepository serverRepository;

    // =========================================================
    // HELPER METHODS
    // =========================================================

    /**
     * Lấy thông tin User đang đăng nhập từ Security Context.
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userService.findByUsername(auth.getName()).orElse(null);
    }

    // =========================================================
    // 1. PUBLIC PAGES (Login, Register, Home)
    // =========================================================

    @GetMapping("/")
    public String root() {
        User user = getCurrentUser();
        return user != null ? "redirect:/home" : "login";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/home")
    public String homePage() {
        return "home";
    }

    // =========================================================
    // 2. FRIENDS & DIRECT MESSAGES
    // =========================================================

    @GetMapping("/friends")
    public String friendsPage(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return "redirect:/login";

        List<FriendResponse> friends = friendService.listFriends(currentUser.getId());
        List<FriendRequestResponse> inboundRequests = friendService.listInboundRequests(currentUser.getId());

        model.addAttribute("friends", friends);
        model.addAttribute("inboundRequests", inboundRequests);
        model.addAttribute("pendingCount", inboundRequests.size());
        model.addAttribute("user", currentUser);

        return "friends";
    }

    @GetMapping("/dm/{friendId}")
    public String dmPage(@PathVariable Long friendId, Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return "redirect:/login";

        User friend = userService.findById(friendId)
                .orElseThrow(() -> new RuntimeException("Friend not found"));

        // Mock tin nhắn rỗng (Sau này tích hợp MessageService thật)
        model.addAttribute("messages", Collections.emptyList());

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentFriend", friend);

        return "dm";
    }

    // =========================================================
    // 3. USER PROFILE & UPLOAD
    // =========================================================

    @GetMapping("/profile")
    public String profilePage(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return "redirect:/login";

        model.addAttribute("user", currentUser);
        return "profile";
    }

    @PostMapping("/profile/avatar")
    public String uploadAvatar(@RequestParam("avatarFile") MultipartFile file) {
        User user = getCurrentUser();
        if (user == null) return "redirect:/login";

        if (file.isEmpty()) return "redirect:/profile?error=empty";

        try {
            // Lưu file vào thư mục static/uploads
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            // Lưu vào target/classes để test ngay không cần rebuild resource
            Path uploadDir = Paths.get("target/classes/static/uploads/");

            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            Files.copy(file.getInputStream(), uploadDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

            // Cập nhật đường dẫn vào DB
            user.setAvatarUrl("/uploads/" + fileName);
            userRepository.save(user);

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/profile?error=server";
        }
        return "redirect:/profile?success";
    }

    // =========================================================
    // 4. ADMIN USERS MANAGEMENT
    // =========================================================

    @GetMapping("/admin/users")
    public String adminUsers(Model model,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "") String keyword) {
        int pageSize = 10;
        // Sắp xếp TĂNG DẦN theo ID (Ascending) để Admin (ID=1) lên đầu
        PageRequest pageable = PageRequest.of(page, pageSize, Sort.by("id").ascending());

        Page<User> userPage;
        if (keyword.isBlank()) {
            userPage = userRepository.findAll(pageable);
        } else {
            userPage = userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword, pageable);
        }

        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("keyword", keyword);

        // Gửi danh sách tất cả các Role sang View để hiển thị trong Modal Sửa
        model.addAttribute("allRoles", roleRepository.findAll());

        return "admin/users";
    }

    // --- Cập nhật User (Từ Modal Sửa) ---
    @PostMapping("/admin/users/update")
    public String updateUser(@RequestParam Long id,
                             @RequestParam String displayName,
                             @RequestParam(required = false) List<String> roleNames) {
        User user = userRepository.findById(id).orElseThrow();
        user.setDisplayName(displayName);

        // Cập nhật Roles
        if (roleNames != null) {
            Set<Role> newRoles = new HashSet<>();
            for (String rName : roleNames) {
                try {
                    ERole eRole = ERole.valueOf(rName);
                    roleRepository.findByName(eRole).ifPresent(newRoles::add);
                } catch (IllegalArgumentException e) {
                    // Bỏ qua nếu tên role không hợp lệ
                }
            }
            user.setRoles(newRoles);
        }

        userRepository.save(user);
        return "redirect:/admin/users";
    }

    // --- Ban User (Từ Modal Ban) ---
    @PostMapping("/admin/users/ban")
    public String banUser(@RequestParam Long id) {
        User user = userRepository.findById(id).orElseThrow();
        user.setIsActive(false); // Logic khóa tài khoản
        userRepository.save(user);
        return "redirect:/admin/users";
    }

    // =========================================================
    // 5. ADMIN SERVERS MANAGEMENT
    // =========================================================

    @GetMapping("/admin/servers")
    public String adminServers(Model model,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "") String keyword) {
        int pageSize = 10;
        PageRequest pageable = PageRequest.of(page, pageSize, Sort.by("id").ascending());

        Page<Server> serverPage;
        if (keyword.isBlank()) {
            serverPage = serverRepository.findAll(pageable);
        } else {
            serverPage = serverRepository.findByNameContainingIgnoreCase(keyword, pageable);
        }

        model.addAttribute("servers", serverPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", serverPage.getTotalPages());
        model.addAttribute("keyword", keyword);

        return "admin/servers";
    }

    // --- Xóa Server (Từ Modal Delete) ---
    @PostMapping("/admin/servers/delete")
    public String deleteServer(@RequestParam Long id) {
        // Thực tế nên có logic kiểm tra quyền Owner hoặc Admin hệ thống ở đây
        serverRepository.deleteById(id);
        return "redirect:/admin/servers";
    }

    // --- Chuyển quyền Owner (Từ Modal Transfer) ---
    @PostMapping("/admin/servers/transfer")
    public String transferServer(@RequestParam Long serverId, @RequestParam String newOwnerUsername) {
        Server server = serverRepository.findById(serverId).orElse(null);
        User newOwner = userService.findByUsername(newOwnerUsername).orElse(null);

        if (server != null && newOwner != null) {
            server.setOwner(newOwner);
            serverRepository.save(server);
            System.out.println("Đã chuyển Server ID " + serverId + " cho User: " + newOwnerUsername);
        } else {
            System.out.println("Không tìm thấy Server hoặc User mới!");
        }

        return "redirect:/admin/servers";
    }
}