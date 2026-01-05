package hcmute.edu.vn.discord.service;

public interface AuditLogMongoService {
    void log(Long adminId, String action, String target, String detail);
}
