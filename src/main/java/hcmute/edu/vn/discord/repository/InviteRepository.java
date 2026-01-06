package hcmute.edu.vn.discord.repository;

import hcmute.edu.vn.discord.entity.jpa.Invite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InviteRepository extends JpaRepository<Invite, Long> {
    // Tìm invite theo mã code (để user join server)
    Optional<Invite> findByCode(String code);

    // Lấy danh sách invite của server (cho admin quản lý)
    List<Invite> findByServerId(Long serverId);

    // Xóa tất cả invite của server
    void deleteAllByServerId(Long serverId);
}
