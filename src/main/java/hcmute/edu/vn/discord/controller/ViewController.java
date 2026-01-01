package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.response.FriendRequestResponse;
import hcmute.edu.vn.discord.dto.response.FriendResponse;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.service.FriendService;
import hcmute.edu.vn.discord.service.ServerService;
import hcmute.edu.vn.discord.service.UserService;
import hcmute.edu.vn.discord.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ViewController {

    private final UserService userService;
    private final FriendService friendService;
    private final ServerService serverService;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userService.findByUsername(auth.getName()).orElse(null);
    }

    @GetMapping("/")
    public String root() { return getCurrentUser() != null ? "redirect:/home" : "login"; }

    @GetMapping("/login")
    public String loginPage() { return getCurrentUser() != null ? "redirect:/home" : "login"; }

    @GetMapping("/register")
    public String registerPage() { return getCurrentUser() != null ? "redirect:/home" : "register"; }

    @GetMapping("/home")
    public String homePage() { return "home"; }

    // ================= TASK 1: FRIENDS =================
    @GetMapping("/friends")
    public String friendsPage(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return "redirect:/login";

        List<FriendResponse> friends = friendService.listFriends(currentUser.getId());
        List<FriendRequestResponse> inbound = friendService.listInboundRequests(currentUser.getId());

        model.addAttribute("friends", friends);
        model.addAttribute("inboundRequests", inbound);
        model.addAttribute("pendingCount", inbound.size());
        model.addAttribute("user", currentUser);

        return "friends";
    }

    // ================= TASK 2: DM / CHAT =================
    @GetMapping("/dm/{friendId}")
    public String dmPage(@PathVariable Long friendId, Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return "redirect:/login";

        User friend = userService.findById(friendId).orElseThrow(() -> new RuntimeException("User not found"));

        // Mock list tin nhắn rỗng để tránh lỗi null (Sau này gọi MessageService thật ở đây)
        model.addAttribute("messages", Collections.emptyList());

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentFriend", friend);
        return "dm";
    }

    // ================= TASK 3: PROFILE + UPLOAD =================
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

        if (file.isEmpty()) return "redirect:/profile?error";

        try {
            // Lưu ảnh vào thư mục static/uploads
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path uploadDir = Paths.get("target/classes/static/uploads/");
            if (!Files.exists(uploadDir)) Files.createDirectories(uploadDir);

            Files.copy(file.getInputStream(), uploadDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

            // Cập nhật DB
            user.setAvatarUrl("/uploads/" + fileName);
            userRepository.save(user);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/profile";
    }

    // ================= TASK 4: ADMIN USERS (Paging + Filter) =================
    @GetMapping("/admin/users")
    public String adminUsers(Model model,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "") String keyword) {
        int pageSize = 10;
        Page<User> userPage;

        if (keyword.isBlank()) {
            userPage = userRepository.findAll(PageRequest.of(page, pageSize, Sort.by("id").descending()));
        } else {
            userPage = userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword, PageRequest.of(page, pageSize));
        }

        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("keyword", keyword);

        return "admin/users";
    }

    // ================= TASK 5: ADMIN SERVERS =================
    @GetMapping("/admin/servers")
    public String adminServers(Model model) {
        List<Server> servers = serverService.getAllServers();
        model.addAttribute("servers", servers);
        return "admin/servers";
    }
}