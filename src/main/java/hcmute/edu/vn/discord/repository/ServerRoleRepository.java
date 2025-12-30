package hcmute.edu.vn.discord.repository;

import hcmute.edu.vn.discord.entity.jpa.ServerRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServerRoleRepository extends JpaRepository<ServerRole, Long> {

    // Tìm role theo ID server và tên role
    Optional<ServerRole> findByServerIdAndName(Long serverId, String name);

    // Tìm tất cả role thuộc một server
    List<ServerRole> findByServerId(Long serverId);

    // Kiểm tra trùng tên role trong một server (không phân biệt hoa thường)
    boolean existsByServerIdAndNameIgnoreCase(Long serverId, String name);

    // Tìm role theo id và thuộc về server id (đảm bảo ràng buộc)
    Optional<ServerRole> findByIdAndServerId(Long id, Long serverId);
}