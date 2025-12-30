package hcmute.edu.vn.discord.repository;

import hcmute.edu.vn.discord.entity.jpa.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, Long> {
    List<Channel> findByServerId(Long serverId);
    List<Channel> findByCategoryId(Long categoryId);
}
