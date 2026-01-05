package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.entity.mongo.AuditLog;
import hcmute.edu.vn.discord.repository.AuditLogMongoRepository;
import hcmute.edu.vn.discord.repository.UserRepository;
import hcmute.edu.vn.discord.service.AuditLogMongoService;
import lombok.RequiredArgsConstructor;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogMongoServiceImpl implements AuditLogMongoService {
    private final AuditLogMongoRepository repo;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void log(Long adminId, String action, String target, String detail) {
        AuditLog log = new AuditLog();
        log.setAdminId(adminId);
        log.setAction(action);
        log.setTarget(target);
        log.setDetail(detail);

        // Fetch User info
        userRepository.findById(adminId).ifPresent(u -> {
            log.setUsername(u.getUsername());
            // Determine Role
            boolean isAdmin = u.getRoles() != null
                    && u.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()));
            String role = isAdmin ? "ADMIN" : "DEFAULT";
            if (u.getRoles() != null && !u.getRoles().isEmpty()) {
                if (u.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName())))
                    role = "ADMIN";
                else if (u.getRoles().stream().anyMatch(r -> "ROLE_PREMIUM".equals(r.getName())))
                    role = "PREMIUM";
            }
            log.setRole(role);
        });

        AuditLog saved = repo.save(log);
        try {
            messagingTemplate.convertAndSend("/topic/admin/audit", saved);
        } catch (Exception e) {
            System.err.println("Audit WS error: " + e.getMessage());
        }
    }

    @Override
    public void log(String adminUsername, String action, String target, String detail) {
        userRepository.findByUsername(adminUsername).ifPresent(u -> {
            log(u.getId(), action, target, detail);
        });
    }
}