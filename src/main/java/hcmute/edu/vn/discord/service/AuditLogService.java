package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.common.AuditLogAction;
import hcmute.edu.vn.discord.entity.jpa.AuditLog;
import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.entity.jpa.User;

import java.util.List;

public interface AuditLogService {
    void logAction(Server server, User actor, AuditLogAction action, String targetId, String targetType,
                   String changes);

    List<AuditLog> getAuditLogs(Long serverId);
}