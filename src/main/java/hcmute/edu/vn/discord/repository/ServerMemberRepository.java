package hcmute.edu.vn.discord.repository;

import hcmute.edu.vn.discord.entity.jpa.ServerMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServerMemberRepository extends JpaRepository<ServerMember, Long> {

    // Tìm tất cả thành viên của 1 server
    List<ServerMember> findByServerId(Long serverId);

    // Kiểm tra sự tồn tại
    boolean existsByServerIdAndUserId(Long serverId, Long userId);

    // Tìm cụ thể 1 dòng để xóa
    Optional<ServerMember> findByServerIdAndUserId(Long serverId, Long userId);
}