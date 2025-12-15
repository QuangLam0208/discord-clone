package hcmute.edu.vn.discord.repository;

import hcmute.edu.vn.discord.entity.jpa.ServerRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ServerRoleRepository extends JpaRepository<ServerRole, Long> {
    // Tìm role dựa trên ID server và tên role
    Optional<ServerRole> findByServerIdAndName(Long serverId, String name);
}