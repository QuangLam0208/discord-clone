package hcmute.edu.vn.discord.repository;

import com.mongodb.lang.NonNull;
import hcmute.edu.vn.discord.entity.enums.ServerStatus;
import hcmute.edu.vn.discord.entity.jpa.Server;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ServerRepository extends JpaRepository<Server, Long> {
    @Override
    @NonNull
    @EntityGraph(attributePaths = {"owner"})
    Optional<Server> findById(@NonNull Long id);

    @EntityGraph(attributePaths = {"owner"})
    Optional<Server> findWithOwnerById(Long id);

    Page<Server> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // Danh sách server mà user (theo id) là member; nạp sẵn owner
    @EntityGraph(attributePaths = {"owner"})
    List<Server> findDistinctByMembers_User_Id(Long userId);

    @EntityGraph(attributePaths = {"owner"}) // nạp owner để hiện username
    List<Server> findDistinctByMembers_User_UsernameAndStatusIn(String username, Set<ServerStatus> active);
}