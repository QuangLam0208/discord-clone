package hcmute.edu.vn.discord.controller.view.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Redirect tới SPA admin với tab Users.
 * Giữ route /admin/users để các liên kết cũ vẫn hoạt động nhưng không render trang riêng biệt,
 * đảm bảo sidebar không bị mất.
 */
@Controller
@RequestMapping("/admin/users")
public class AdminUsersViewController {

    @GetMapping({"", "/"})
    public String usersSpa() {
        // Điều hướng đến khung admin SPA và mở tab Users
        return "redirect:/admin#users";
    }
}