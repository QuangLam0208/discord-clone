package hcmute.edu.vn.discord.service;

public interface AuditLogMongoService {
    void log(Long adminId, String action, String target, String detail);

    void log(String adminUsername, String action, String target, String detail);
}
