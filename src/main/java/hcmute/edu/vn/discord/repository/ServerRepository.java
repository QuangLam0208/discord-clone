package hcmute.edu.vn.discord.repository;

import hcmute.edu.vn.discord.entity.jpa.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServerRepository extends JpaRepository<Server, Long> {
    List<Server> findByOwner_Id(Long ownerId);
    List<Server> findByOwnerUsername(String userName);
    @Query("SELECT s FROM Server s JOIN FETCH s.owner JOIN s.members m WHERE m.user.username = :username")
    List<Server> findByMemberUsername(@Param("username") String username);

}
