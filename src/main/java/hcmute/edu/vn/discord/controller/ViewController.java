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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

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
    public String root() {
        User user = getCurrentUser();
        return user != null ? "redirect:/home" : "login";
    }

    @GetMapping("/login")
    public String loginPage() {
        return getCurrentUser() != null ? "redirect:/home" : "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return getCurrentUser() != null ? "redirect:/home" : "register";
    }

    @GetMapping("/home")
    public String homePage() {
        return "home";
    }

    // ================= TASK 1: FRIENDS & REQUESTS =================
    @GetMapping("/friends")
    public String friendsPage(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return "redirect:/login";

        // 1. Lấy danh sách bạn bè
        List<FriendResponse> friends = friendService.listFriends(currentUser.getId());

        // 2. Lấy danh sách lời mời kết bạn (Inbound: Người khác gửi mình)
        List<FriendRequestResponse> inboundRequests = friendService.listInboundRequests(currentUser.getId());

        // 3. Lấy danh sách mình gửi đi (Outbound) - Optional hiển thị thêm nếu muốn
        List<FriendRequestResponse> outboundRequests = friendService.listOutboundRequests(currentUser.getId());

        model.addAttribute("friends", friends);
        model.addAttribute("inboundRequests", inboundRequests);
        model.addAttribute("outboundRequests", outboundRequests);
        model.addAttribute("user", currentUser); // Để hiển thị avatar user góc trái

        // Đếm số lượng request để hiện badge
        model.addAttribute("pendingCount", inboundRequests.size());

        return "friends"; // Trả về templates/friends.html
    }

    // ================= TASK 2: DIRECT MESSAGES (Chat 1-1) =================
    @GetMapping("/dm/{friendId}")
    public String dmPage(@PathVariable Long friendId, Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return "redirect:/login";

        User friend = userService.findById(friendId).orElseThrow(() -> new RuntimeException("Friend not found"));

        // TODO: Gọi Service lấy lịch sử tin nhắn (MessageService)
        // List<MessageResponse> messages = messageService.getConversation(currentUser.getId(), friendId);
        // model.addAttribute("messages", messages);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentFriend", friend);

        return "dm"; // Trả về templates/dm.html
    }

    // ================= TASK 3: PROFILE =================
    @GetMapping("/profile")
    public String profilePage(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return "redirect:/login";

        model.addAttribute("user", currentUser);
        return "profile"; // Trả về templates/profile.html
    }

    // ================= TASK 4: ADMIN USERS =================
    @GetMapping("/admin/users")
    public String adminUsers(Model model, @RequestParam(defaultValue = "") String keyword) {
        // Cần check role admin ở đây nếu kỹ hơn

        // Lấy tất cả user (Để đơn giản ta dùng findAll, thực tế nên dùng Pageable)
        List<User> users = userRepository.findAll();

        model.addAttribute("users", users);
        model.addAttribute("keyword", keyword);
        return "admin/users"; // Trả về templates/admin/users.html
    }

    // ================= TASK 5: ADMIN SERVERS =================
    @GetMapping("/admin/servers")
    public String adminServers(Model model) {
        List<Server> servers = serverService.getAllServers();
        model.addAttribute("servers", servers);
        return "admin/servers"; // Trả về templates/admin/servers.html
    }
}