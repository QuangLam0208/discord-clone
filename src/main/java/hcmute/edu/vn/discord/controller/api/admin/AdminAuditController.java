package hcmute.edu.vn.discord.controller.api.admin;

import hcmute.edu.vn.discord.entity.mongo.AuditLog;
import hcmute.edu.vn.discord.repository.AuditLogRepository;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/auditlogs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ADMIN')")
public class AdminAuditController {

    private final AuditLogRepository repo;

    // GET /api/admin/auditlogs?page=&size=&keyword=&action=&adminId=
    @GetMapping
    public Page<AuditLog> list(@RequestParam(defaultValue = "0") @Min(0) int page,
                               @RequestParam(defaultValue = "10") @Min(1) int size,
                               @RequestParam(defaultValue = "") String keyword,
                               @RequestParam(defaultValue = "") String action,
                               @RequestParam(required = false) Long adminId) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        String kw = keyword == null ? "" : keyword;
        String act = action == null ? "" : action;

        return (adminId != null)
                ? repo.findByAdminIdAndActionContainingIgnoreCaseAndDetailContainingIgnoreCase(adminId, act, kw, pageable)
                : repo.findByActionContainingIgnoreCaseAndDetailContainingIgnoreCase(act, kw, pageable);
    }
}
