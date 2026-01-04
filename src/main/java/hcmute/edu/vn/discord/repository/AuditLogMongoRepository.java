package hcmute.edu.vn.discord.repository;

import hcmute.edu.vn.discord.entity.mongo.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditLogMongoRepository extends MongoRepository<AuditLog, String> {

    Page<AuditLog> findByActionContainingIgnoreCaseAndDetailContainingIgnoreCase(
            String action, String keyword, Pageable pageable
    );

    Page<AuditLog> findByAdminIdAndActionContainingIgnoreCaseAndDetailContainingIgnoreCase(
            Long adminId, String action, String keyword, Pageable pageable
    );
}