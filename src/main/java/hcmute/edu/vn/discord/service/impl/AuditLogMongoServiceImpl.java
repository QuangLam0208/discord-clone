package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.entity.mongo.AuditLog;
import hcmute.edu.vn.discord.repository.AuditLogMongoRepository;
import hcmute.edu.vn.discord.service.AuditLogMongoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogMongoServiceImpl implements AuditLogMongoService {
    private final AuditLogMongoRepository repo;
    private final hcmute.edu.vn.discord.repository.UserRepository userRepository;

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
            // Assuming premium is another role or status, for now Default/Admin
            // Check for other roles if they exist in system logic
            String role = isAdmin ? "ADMIN" : "DEFAULT";
            // Better logic: Join roles names
            if (u.getRoles() != null && !u.getRoles().isEmpty()) {
                if (u.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName())))
                    role = "ADMIN";
                else if (u.getRoles().stream().anyMatch(r -> "ROLE_PREMIUM".equals(r.getName())))
                    role = "PREMIUM";
            }
            log.setRole(role);
        });

        repo.save(log);
    }

    @Override
    public void log(String adminUsername, String action, String target, String detail) {
        userRepository.findByUsername(adminUsername).ifPresent(u -> {
            log(u.getId(), action, target, detail);
        });
    }
}