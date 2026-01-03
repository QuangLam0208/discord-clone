package hcmute.edu.vn.discord.repository;

import hcmute.edu.vn.discord.entity.jpa.ServerMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServerMemberRepository extends JpaRepository<ServerMember, Long> {

    @EntityGraph(attributePaths = { "roles", "roles.permissions", "user" })
    List<ServerMember> findByServerId(Long serverId);

    boolean existsByServerIdAndUserId(Long serverId, Long userId);

    Optional<ServerMember> findByServerIdAndUserId(Long serverId, Long userId);

    List<ServerMember> findByUserUsername(String username);

    @org.springframework.data.jpa.repository.Query("SELECT m FROM ServerMember m JOIN m.roles r WHERE r.id = :roleId")
    List<ServerMember> findByRoleId(Long roleId);

}