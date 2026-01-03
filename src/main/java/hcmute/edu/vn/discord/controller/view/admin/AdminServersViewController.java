package hcmute.edu.vn.discord.controller.view.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Redirect tới SPA admin với tab Servers.
 * Giữ route /admin/servers để các liên kết cũ vẫn hoạt động.
 */
@Controller
@RequestMapping("/admin/servers")
public class AdminServersViewController {

    @GetMapping({"", "/"})
    public String serversSpa() {
        // Điều hướng đến khung admin SPA và mở tab Servers
        return "redirect:/admin#servers";
    }
}