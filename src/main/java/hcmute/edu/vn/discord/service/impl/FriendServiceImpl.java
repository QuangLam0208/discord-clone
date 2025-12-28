package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.dto.response.FriendRequestResponse;
import hcmute.edu.vn.discord.dto.response.FriendResponse;
import hcmute.edu.vn.discord.entity.enums.FriendStatus;
import hcmute.edu.vn.discord.entity.jpa.Friend;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.repository.FriendRepository;
import hcmute.edu.vn.discord.repository.UserRepository;
import hcmute.edu.vn.discord.service.FriendService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {
    private final FriendRepository friendRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public FriendRequestResponse sendRequest(Long senderId, Long recipientId){
        if (senderId.equals(recipientId)) {
            throw new IllegalArgumentException("Không thể gửi lời mời kết bạn cho chính mình");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người gửi"));
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người nhận"));

        var existingOpt = friendRepository.findByRequesterAndReceiverOrRequesterAndReceiver(
                sender, recipient,recipient, sender);

        if (existingOpt.isPresent()) {
            Friend existing = existingOpt.get();
            switch (existing.getStatus()) {
                case PENDING -> {
                    throw new DataIntegrityViolationException("Yêu cầu kết bạn đang chờ xử lý");
                }
                case ACCEPTED -> {
                    throw new DataIntegrityViolationException("Hai bạn đã là bạn bè");
                }
                case BLOCKED -> {
                    boolean blockedBySender = existing.getRequester().getId().equals(senderId)
                            && existing.getReceiver().getId().equals(recipientId);
                    String msg = blockedBySender
                            ? ("Bạn đã chặn " + recipient.getUsername())
                            : (recipient.getUsername() + " đã chặn bạn");
                    throw new AccessDeniedException(msg);
                }
                case DECLINED -> {
                    // Cho phép gửi lại (revive)
                    existing.setRequester(sender);
                    existing.setReceiver(recipient);
                    existing.setStatus(FriendStatus.PENDING);
                    existing.setCreatedAt(LocalDateTime.now());
                    existing.setRespondedAt(null);
                    Friend revived = friendRepository.save(existing);
                    return toRequestResponse(revived);
                }
                default -> throw new IllegalStateException("Trạng thái không hợp lệ");
            }
        }

        Friend friend = new Friend();
        friend.setRequester(sender);
        friend.setReceiver(recipient);
        friend.setStatus(FriendStatus.PENDING);
        friend.setCreatedAt(LocalDateTime.now());

        Friend saved = friendRepository.save(friend);
        return toRequestResponse(saved);
    }

    @Transactional
    @Override
    public FriendRequestResponse acceptRequest(Long requestId, Long currentUserId){
        Friend friend = friendRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy yêu cầu kết bạn"));

        if (!friend.getReceiver().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Chỉ người nhận mới có thể chấp nhận yêu cầu");
        }
        if (friend.getStatus() != FriendStatus.PENDING) {
            throw new IllegalStateException("Không có yêu cầu kết bạn nào để xử lý");
        }

        friend.setStatus(FriendStatus.ACCEPTED);
        friend.setRespondedAt(LocalDateTime.now());
        Friend saved = friendRepository.save(friend);
        return toRequestResponse(saved);
    }

    @Override
    @Transactional
    public FriendRequestResponse declineRequest(Long requestId, Long currentUserId){
        Friend friend = friendRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy yêu cầu kết bạn"));

        if (!friend.getReceiver().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Chỉ người nhận mới có thể từ chối yêu cầu");
        }
        if (friend.getStatus() != FriendStatus.PENDING) {
            throw new IllegalStateException("Không có yêu cầu kết bạn nào để xử lý");
        }

        friend.setStatus(FriendStatus.DECLINED);
        friend.setRespondedAt(LocalDateTime.now());
        Friend saved = friendRepository.save(friend);
        return toRequestResponse(saved);
    }

    @Override
    @Transactional
    public void cancelRequest(Long requestId, Long currentUserId) {
        Friend friend = friendRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy yêu cầu kết bạn"));
        if (!friend.getRequester().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Chỉ người gửi mới có thể huỷ yêu cầu");
        }
        friendRepository.delete(friend);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FriendResponse> listFriends(Long currentUserId) {
        List<Friend> list = friendRepository.findAllAcceptedFriendsOfUser(currentUserId, FriendStatus.ACCEPTED);
        return list.stream().map(f -> {
            User other = f.getRequester().getId().equals(currentUserId) ? f.getReceiver() : f.getRequester();
            return FriendResponse.builder()
                    .friendUserId(other.getId())
                    .friendUsername(other.getUsername())
                    .displayName(other.getDisplayName())
                    .avatarUrl(other.getAvatarUrl())
                    .since(f.getRespondedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FriendRequestResponse> listInboundRequests(Long currentUserId) {
        List<Friend> list = friendRepository.findByReceiverIdAndStatus(currentUserId, FriendStatus.PENDING);
        return list.stream().map(this::toRequestResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FriendRequestResponse> listOutboundRequests(Long currentUserId) {
        List<Friend> list = friendRepository.findByRequesterIdAndStatus(currentUserId, FriendStatus.PENDING);
        return list.stream().map(this::toRequestResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void unfriend(Long currentUserId, Long friendUserId) {
        User current = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));
        User other = userRepository.findById(friendUserId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));

        Friend friend = friendRepository.findByRequesterAndReceiverOrRequesterAndReceiver(current, other, other, current)
                .orElseThrow(() -> new EntityNotFoundException("Quan hệ bạn bè không tồn tại"));

        if (friend.getStatus() != FriendStatus.ACCEPTED) {
            throw new IllegalStateException("Không thể huỷ kết bạn khi chưa chấp nhận");
        }
        friendRepository.delete(friend);
    }

    @Override
    @Transactional
    public void blockUser(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new IllegalArgumentException("Không thể block chính mình");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));

        Friend friend = friendRepository
                .findByRequesterAndReceiverOrRequesterAndReceiver(user, targetUser, targetUser, user)
                .orElseGet(() -> {
                    Friend f = new Friend();
                    f.setRequester(user);
                    f.setReceiver(targetUser);
                    f.setCreatedAt(LocalDateTime.now());
                    return f;
                });

        // Chỉ coi là "user block target" nếu chiều requester=user, receiver=target
        friend.setRequester(user);
        friend.setReceiver(targetUser);
        friend.setStatus(FriendStatus.BLOCKED);
        friend.setRespondedAt(LocalDateTime.now());
        friendRepository.save(friend);
    }

    @Transactional
    @Override
    public void unblockUser(Long userId, Long targetUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));

        // Chỉ cho phép gỡ block nếu chính bạn đã block (requester=user, receiver=target, status=BLOCKED)
        Friend friend = friendRepository.findByRequesterAndReceiver(user, target)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy trạng thái block"));

        if (friend.getStatus() != FriendStatus.BLOCKED) {
            throw new IllegalStateException("Không ở trạng thái block");
        }

        friendRepository.delete(friend);
    }

    private FriendRequestResponse toRequestResponse(Friend f) {
        return FriendRequestResponse.builder()
                .id(f.getId())
                .senderId(f.getRequester().getId())
                .senderUsername(f.getRequester().getUsername())
                .recipientId(f.getReceiver().getId())
                .recipientUsername(f.getReceiver().getUsername())
                .status(f.getStatus().name())
                .createdAt(f.getCreatedAt())
                .respondedAt(f.getRespondedAt())
                .build();
    }
}
