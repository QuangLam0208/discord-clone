package hcmute.edu.vn.discord.repository;

import hcmute.edu.vn.discord.entity.jpa.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByServerIdOrderByCreatedAtDesc(Long serverId);

    void deleteByServerId(Long serverId);
}