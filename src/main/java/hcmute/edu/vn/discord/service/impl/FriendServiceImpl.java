package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.entity.enums.FriendStatus;
import hcmute.edu.vn.discord.entity.jpa.Friend;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.repository.FriendRepository;
import hcmute.edu.vn.discord.repository.UserRepository;
import hcmute.edu.vn.discord.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {
    private final FriendRepository friendRepository;
    private final UserRepository userRepository;

    @Override
    public Friend sendFriendRequest(Long requesterId, Long receiverId){

        if(requesterId.equals(receiverId)){
            throw new RuntimeException("Không thể gửi lời mời kết bạn cho chính mình");
        }

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người yêu cầu"));


        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người nhận"));

        friendRepository.findByRequesterAndReceiverOrRequesterAndReceiver(
                requester, receiver,
                receiver, requester
        ).ifPresent(f -> {
            throw new RuntimeException("Quan hệ đã tồn tại");
        });

        Friend friend = new Friend();
        friend.setRequester(requester);
        friend.setReceiver(receiver);
        friend.setStatus(FriendStatus.PENDING);

        return friendRepository.save(friend);
    }

    @Override
    public Friend acceptFriendRequest(Long requesterId, Long receiverId){

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người gửi"));

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người nhận"));

        Friend friend = friendRepository
                .findByRequesterAndReceiver(requester, receiver)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu kết bạn"));

        if(friend.getStatus() != FriendStatus.PENDING) {
            throw new RuntimeException("Không có yêu cầu kết bạn nào để xử lí");
        }

        friend.setStatus(FriendStatus.ACCEPTED);
        return friendRepository.save(friend);
    }

    @Override
    public void blockUser(Long userId, Long targetUserId) {

        if(userId.equals(targetUserId)) {
            throw new RuntimeException("Không thể block chính mình");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        Friend friend = friendRepository
                .findByRequesterAndReceiverOrRequesterAndReceiver(
                  user, targetUser,
                  targetUser, user
                )
                .orElseGet(() ->{
                   Friend f = new Friend();
                   f.setRequester(user);
                   f.setReceiver(targetUser);
                   return f;
                });

        friend.setStatus(FriendStatus.BLOCKED);
        friendRepository.save(friend);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Friend> getAcceptedFriends(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        FriendStatus status = FriendStatus.ACCEPTED;

        List<Friend> listFriend = friendRepository.findAllAcceptedFriendsOfUser(userId, status);

        return listFriend;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Friend> getSendRequests(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        FriendStatus status = FriendStatus.PENDING;

        List<Friend> listFriend = friendRepository.findByRequesterIdAndStatus(userId, status);

        return listFriend;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Friend> getReceivedRequests(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        FriendStatus status = FriendStatus.PENDING;

        List<Friend> listFriend = friendRepository.findByReceiverIdAndStatus(userId, status);

        return listFriend;
    }
}
