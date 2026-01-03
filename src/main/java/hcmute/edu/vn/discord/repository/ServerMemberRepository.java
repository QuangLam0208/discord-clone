package hcmute.edu.vn.discord.repository;

import hcmute.edu.vn.discord.entity.jpa.ServerMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ServerMemberRepository extends JpaRepository<ServerMember, Long> {

    @EntityGraph(attributePaths = {"roles", "roles.permissions", "user"})
    List<ServerMember> findByServerId(Long serverId);

    boolean existsByServerIdAndUserId(Long serverId, Long userId);

    Optional<ServerMember> findByServerIdAndUserId(Long serverId, Long userId);

    List<ServerMember> findByUserUsername(String username);

    int countByServerId(Long serverId);

    // Nạp sẵn roles và permissions của member để tránh LAZY trong WS
    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<ServerMember> findWithRolesByServerIdAndUserId(Long serverId, Long userId);

    // Lấy trực tiếp các permission code của member bằng JOIN (tránh đụng collection LAZY)
    @Query("""
        select distinct p.code
        from ServerMember sm
        join sm.roles r
        join r.permissions p
        where sm.server.id = :serverId and sm.user.username = :username
    """)
    Set<String> findPermissionCodesByServerIdAndUsername(Long serverId, String username);

}