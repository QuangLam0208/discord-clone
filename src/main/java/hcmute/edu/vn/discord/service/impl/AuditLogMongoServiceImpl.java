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

    @Override
    public void log(Long adminId, String action, String target, String detail) {
        AuditLog log = new AuditLog();
        log.setAdminId(adminId);
        log.setAction(action);
        log.setTarget(target);
        log.setDetail(detail);
        repo.save(log);
    }
}