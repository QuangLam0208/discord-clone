package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.entity.jpa.Friend;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


public interface FriendService {
    // Gửi lời mời kết bạn
    Friend sendFriendRequest(Long requesterId, Long receiverId);

    // Chấp nhận lời mời kết bạn
    Friend acceptFriendRequest(Long requesterId, Long receiverId);

    // Chặn người dùng
    void blockUser(Long userId, Long targetUserId);

    // Lấy danh sách bạn bè
    List<Friend> getAcceptedFriends(Long userId);

    // Lấy lời mời đã gửi
    List<Friend> getSendRequests(Long userId);

    // Lấy lời mời đã nhận
    List<Friend> getReceivedRequests(Long userId);
}
