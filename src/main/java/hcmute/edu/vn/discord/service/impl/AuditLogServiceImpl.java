package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.entity.enums.EAuditAction;
import hcmute.edu.vn.discord.entity.jpa.AuditLog;
import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.repository.AuditLogRepository;
import hcmute.edu.vn.discord.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    public void logAction(Server server, User actor, EAuditAction action, String targetId, String targetType,
            String changes) {
        AuditLog log = AuditLog.builder()
                .server(server)
                .actor(actor)
                .actionType(action)
                .targetId(targetId)
                .targetType(targetType)
                .changes(changes)
                .build();
        auditLogRepository.save(log);
    }

    @Override
    public List<AuditLog> getAuditLogs(Long serverId) {
        return auditLogRepository.findByServerIdOrderByCreatedAtDesc(serverId);
    }

    @Override
    public void deleteByServer(Long serverId) {
        auditLogRepository.deleteByServerId(serverId);
    }
}