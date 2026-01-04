package hcmute.edu.vn.discord.repository;

import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.entity.jpa.ServerBan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServerBanRepository extends JpaRepository<ServerBan, Long> {
    Optional<ServerBan> findByServerIdAndUserId(Long serverId, Long userId);

    List<ServerBan> findByServerId(Long serverId);

    // Fallback methods to ensure JPA works regardless of strictness
    List<ServerBan> findByServer(Server server);

    @Query(value = "SELECT * FROM server_bans WHERE server_id = :serverId", nativeQuery = true)
    List<ServerBan> findByServerIdNative(@Param("serverId") Long serverId);

    boolean existsByServerIdAndUserId(Long serverId, Long userId);
}
