package hcmute.edu.vn.discord.repository;

import hcmute.edu.vn.discord.entity.jpa.ServerMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServerMemberRepository extends JpaRepository<ServerMember, Long> {

    // 1. Lấy danh sách thành viên của 1 server
    List<ServerMember> findByServerId(Long serverId);

    // 2. Kiểm tra xem user có phải là thành viên của server không
    boolean existsByServerIdAndUserId(Long serverId, Long userId);

    // 3. Tìm chính xác dòng record để xóa (rời nhóm / kick)
    Optional<ServerMember> findByServerIdAndUserId(Long serverId, Long userId);

}