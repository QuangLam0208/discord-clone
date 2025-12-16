package hcmute.edu.vn.discord.repository;

import hcmute.edu.vn.discord.entity.enums.FriendStatus;
import hcmute.edu.vn.discord.entity.jpa.Friend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRepository extends JpaRepository<Friend, Long> {
    // Tìm danh sách bạn bè (đã accept) của 1 user (cần check cả 2 chiều requester/receiver)
    List<Friend> findByRequesterIdAndStatus(Long requesterId, FriendStatus status);
    List<Friend> findByReceiverIdAndStatus(Long receiverId, FriendStatus status);

    // Kiểm tra quan hệ giữa 2 người (để chặn spam request)
    Optional<Friend> findByRequesterIdAndReceiverId(Long requesterId, Long receiverId);
}
