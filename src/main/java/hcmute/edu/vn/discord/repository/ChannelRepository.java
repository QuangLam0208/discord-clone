package hcmute.edu.vn.discord.repository;

import hcmute.edu.vn.discord.entity.enums.ChannelType;
import hcmute.edu.vn.discord.entity.jpa.Channel;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;

import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, Long> {
    @EntityGraph(attributePaths = {"category", "allowedMembers", "allowedRoles"})
    List<Channel> findByServerIdOrderByIdAsc(Long serverId);

    List<Channel> findByCategoryId(Long categoryId);

    @Query("SELECT c.server.id FROM Channel c WHERE c.id = :channelId")
    Optional<Long> findServerIdByChannelId(Long channelId);

    List<Channel> findByServerIdAndType(Long serverId, ChannelType type);

    // Đếm channel theo server để map DTO không đụng LAZY
    int countByServerId(Long serverId);

    // Các hàm phục vụ xóa server có category
    @Modifying
    @Transactional
    @Query("update Channel c set c.category = null where c.server.id = :serverId")
    int unsetCategoryByServerId(Long serverId);

    @Modifying
    @Transactional
    @Query("delete from Channel c where c.server.id = :serverId")
    int deleteByServerId(Long serverId);

    @EntityGraph(attributePaths = {"server", "allowedMembers", "allowedRoles"})
    Optional<Channel> findWithAccessListsById(Long id);
}