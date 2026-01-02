package hcmute.edu.vn.discord.controller.view.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
public class AdminDashboardViewController {

    @GetMapping({"", "/", "/index"})
    public String index() {
        return "admin/index";
    }

    @GetMapping("/messages")
    public String messages() {
        return "admin/messages";
    }

    @GetMapping("/audit")
    public String audit() {
        return "admin/audit";
    }
}