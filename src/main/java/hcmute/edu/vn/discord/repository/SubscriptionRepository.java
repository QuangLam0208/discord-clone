package hcmute.edu.vn.discord.repository;

import hcmute.edu.vn.discord.entity.jpa.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    // Kiểm tra user có đang đăng ký gói nào còn hạn không
    Optional<Subscription> findByUserIdAndEndDateAfter(Long userId, Date now);
}
