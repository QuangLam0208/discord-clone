package hcmute.edu.vn.discord.repository;

import hcmute.edu.vn.discord.entity.jpa.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    // Lấy tất cả Category của 1 Server (để render sidebar)
    List<Category> findByServerId(Long serverId);

    // Xóa tất cả category thuộc server (dùng khi xóa server)
    @Modifying
    @Transactional
    @Query("delete from Category c where c.server.id = :serverId")
    void deleteByServerId(Long serverId);
}