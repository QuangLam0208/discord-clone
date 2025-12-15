package hcmute.edu.vn.discord.repository;

import hcmute.edu.vn.discord.entity.jpa.ServerMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServerMemberRepository extends JpaRepository<ServerMember, Long> {

    Optional<ServerMember> findByServer_IdAndUser_Id(Long serverId, Long userId);

    List<ServerMember> findByUser_Id(Long userId);

    List<ServerMember> findByServer_Id(Long serverId);

    boolean existsByServer_IdAndUser_Id(Long serverId, Long userId);
}
