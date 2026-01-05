package hcmute.edu.vn.discord.controller.view;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthViewController {

    @GetMapping("/")
    public String root(Authentication auth) {
        // Nếu server có Authentication (cookie/session) → chuyển theo role
        if (auth != null && auth.isAuthenticated()) {
            boolean isAdmin = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(a -> "ROLE_ADMIN".equals(a) || "ROLE_MODERATOR".equals(a));
            return isAdmin ? "redirect:/admin" : "redirect:/home";
        }
        return "login";
    }

    @GetMapping("/login")
    public String loginPage() { return "login"; }

    @GetMapping("/register")
    public String registerPage() { return "register"; }
}