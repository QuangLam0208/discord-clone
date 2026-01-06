package hcmute.edu.vn.discord.controller.view;

import hcmute.edu.vn.discord.dto.response.FriendRequestResponse;
import hcmute.edu.vn.discord.dto.response.FriendResponse;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.service.FriendService;
import hcmute.edu.vn.discord.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class AppViewController {

    private final UserService userService;
    private final FriendService friendService;

    @GetMapping({ "/home" })
    public String home(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }
        return "home";
    }

    @GetMapping("/friends")
    public String friendsPage(Authentication auth, Model model) {
        if (auth == null || !auth.isAuthenticated())
            return "redirect:/login";

        User currentUser = userService.findByUsername(auth.getName()).orElse(null);
        if (currentUser == null)
            return "redirect:/login";

        List<FriendResponse> friends = friendService.listFriends(currentUser.getId());
        List<FriendRequestResponse> inboundRequests = friendService.listInboundRequests(currentUser.getId());

        model.addAttribute("friends", friends);
        model.addAttribute("inboundRequests", inboundRequests);
        model.addAttribute("pendingCount", inboundRequests.size());
        model.addAttribute("user", currentUser);
        return "friends";
    }

    @GetMapping("/dm/{friendId}")
    public String dmPage(@PathVariable Long friendId, Authentication auth, Model model) {
        if (auth == null || !auth.isAuthenticated())
            return "redirect:/login";

        User currentUser = userService.findByUsername(auth.getName()).orElse(null);
        if (currentUser == null)
            return "redirect:/login";

        User friend = userService.findById(friendId)
                .orElseThrow(() -> new RuntimeException("Friend not found"));

        model.addAttribute("messages", Collections.emptyList());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentFriend", friend);
        return "dm";
    }
}