package hcmute.edu.vn.discord.controller.view.admin;

import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.entity.mongo.AuditLog;
import hcmute.edu.vn.discord.repository.AuditLogMongoRepository;
import hcmute.edu.vn.discord.repository.ServerRepository;
import hcmute.edu.vn.discord.repository.RoleRepository;
import hcmute.edu.vn.discord.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminContentViewController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ServerRepository serverRepository;
    private final AuditLogMongoRepository auditLogMongoRepository;

    @GetMapping("/_dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalServers", serverRepository.count());
        model.addAttribute("messages24h", "--");
        return "admin/partials/dashboard";
    }

    @GetMapping("/_users")
    public String users(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String keyword) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<User> userPage = keyword.isBlank()
                ? userRepository.findAll(pageable)
                : userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword,
                        pageable);

        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("allRoles", roleRepository.findAll());
        return "admin/partials/users";
    }

    @GetMapping("/_servers")
    public String servers(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String keyword) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Server> serverPage = keyword.isBlank()
                ? serverRepository.findAll(pageable)
                : serverRepository.findByNameContainingIgnoreCase(keyword, pageable);

        model.addAttribute("servers", serverPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", serverPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        return "admin/partials/servers";
    }

    @GetMapping("/_messages")
    public String messages(Model model) {
        return "admin/partials/messages";
    }

    @GetMapping("/_audit")
    public String audit(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String action,
            @RequestParam(required = false) Long adminId) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Chuẩn hóa chuỗi rỗng để query chứa '' trả về all
        String kw = keyword == null ? "" : keyword;
        String act = action == null ? "" : action;

        Page<AuditLog> result = (adminId != null)
                ? auditLogMongoRepository.findByAdminIdAndActionContainingIgnoreCaseAndDetailContainingIgnoreCase(
                        adminId, act, kw, pageable)
                : auditLogMongoRepository.findByActionContainingIgnoreCaseAndDetailContainingIgnoreCase(act, kw,
                        pageable);

        model.addAttribute("logs", result.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("keyword", kw);
        model.addAttribute("action", act);
        model.addAttribute("adminId", adminId);

        return "admin/partials/audit";
    }

    @GetMapping("/_reports")
    public String getReports() {
        return "admin/partials/reports";
    }
}