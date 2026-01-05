package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.entity.enums.EAuditAction;
import hcmute.edu.vn.discord.entity.jpa.AuditLog;
import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.entity.jpa.User;

import java.util.List;

public interface AuditLogService {
    void logAction(Server server, User actor, EAuditAction action, String targetId, String targetType,
                   String changes);

    List<AuditLog> getAuditLogs(Long serverId);
}