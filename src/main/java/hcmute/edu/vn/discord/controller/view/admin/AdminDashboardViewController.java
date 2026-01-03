package hcmute.edu.vn.discord.controller.view.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Render khung SPA Admin (sidebar + khu vực nội dung).
 * Nội dung bên phải được nạp động qua các fragment /admin/_dashboard, /admin/_users, /admin/_servers, /admin/_messages.
 */
@Controller
@RequestMapping("/admin")
public class AdminDashboardViewController {

    @GetMapping({"", "/", "/index"})
    public String index() {
        return "admin/index";
    }
}