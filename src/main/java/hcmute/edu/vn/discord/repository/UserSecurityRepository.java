package hcmute.edu.vn.discord.repository;

import hcmute.edu.vn.discord.entity.jpa.UserSecurity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSecurityRepository extends JpaRepository<UserSecurity, Long> {
    // Dùng khi login để check xem user có bật 2FA không
    Optional<UserSecurity> findByUserId(Long userId);
}
