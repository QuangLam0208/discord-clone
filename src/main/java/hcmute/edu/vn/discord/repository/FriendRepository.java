package hcmute.edu.vn.discord.repository;

import hcmute.edu.vn.discord.entity.enums.FriendStatus;
import hcmute.edu.vn.discord.entity.jpa.Friend;
import hcmute.edu.vn.discord.entity.jpa.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRepository extends JpaRepository<Friend, Long> {
    // Lấy lời mời kết bạn mà User đã gửi (PENDING)
    List<Friend> findByRequesterIdAndStatus(
            Long requesterId,
            FriendStatus status
    );

    // Lấy Lời mời kết bạn mà User đã nhận (PENDING)
    List<Friend> findByReceiverIdAndStatus(
            Long receiverId,
            FriendStatus status
    );

    // Tìm mối quan hệ giữa 2 user theo 2 chiều
    Optional<Friend> findByRequesterAndReceiverOrRequesterAndReceiver(
            User requester1, User receiver1,
            User requester2, User receiver2
    );

    // Lấy danh sách bạn bè đã ACCEPT
    @Query("""
            SELECT f FROM Friend f
            WHERE f.status = :status
                  AND (f.requester.id = :userId OR f.receiver.id = :userId)
            """)
    List<Friend> findAllAcceptedFriendsOfUser(
            @Param("userId") Long userId,
            @Param("status") FriendStatus status
    );

    Optional<Friend> findByRequesterAndReceiver(User requester, User receiver);
}
