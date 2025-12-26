package hcmute.edu.vn.discord.repository;

import hcmute.edu.vn.discord.entity.jpa.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    // Tìm quyền theo mã code (VD: MESSAGE_DELETE)
    Optional<Permission> findByCode(String code);
}
