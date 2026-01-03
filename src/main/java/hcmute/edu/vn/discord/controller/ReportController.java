package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.request.ReportRequest;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.service.ReportService;
import hcmute.edu.vn.discord.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<?> createReport(@RequestBody ReportRequest request, Authentication auth) {
        if (auth == null) throw new AccessDeniedException("Not authenticated");
        User current = userService.findByUsername(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("User not found"));
        reportService.createReport(current.getId(), request);
        return ResponseEntity.ok().build();
    }
}