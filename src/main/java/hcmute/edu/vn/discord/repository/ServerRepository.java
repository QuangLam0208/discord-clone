package hcmute.edu.vn.discord.repository;

import hcmute.edu.vn.discord.entity.jpa.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServerRepository extends JpaRepository<Server, Long> {

    List<Server> findByOwner_Id(Long ownerId);
}
