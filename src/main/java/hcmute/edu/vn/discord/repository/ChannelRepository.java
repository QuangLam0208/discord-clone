package hcmute.edu.vn.discord.repository;

import hcmute.edu.vn.discord.entity.jpa.Channel;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, Long> {
    @EntityGraph(attributePaths = {"category", "allowedMembers", "allowedRoles"})
    List<Channel> findByServerIdOrderByIdAsc(Long serverId);
    List<Channel> findByCategoryId(Long categoryId);
    @Query("SELECT c.server.id FROM Channel c WHERE c.id = :channelId")
    Optional<Long> findServerIdByChannelId(Long channelId);
}