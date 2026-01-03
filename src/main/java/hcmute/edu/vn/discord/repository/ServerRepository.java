package hcmute.edu.vn.discord.repository;

import com.mongodb.lang.NonNull;
import hcmute.edu.vn.discord.entity.jpa.Server;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ServerRepository extends JpaRepository<Server, Long> {
    @Override
    @NonNull
    @EntityGraph(attributePaths = {"owner"})
    Optional<Server> findById(@NonNull Long id);
}