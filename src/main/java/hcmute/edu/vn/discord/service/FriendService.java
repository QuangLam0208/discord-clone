package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.dto.response.FriendRequestResponse;
import hcmute.edu.vn.discord.dto.response.FriendResponse;
import hcmute.edu.vn.discord.entity.jpa.Friend;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


public interface FriendService {
    @Transactional
    FriendRequestResponse sendRequest(Long senderId, Long recipientId);

    @Transactional
    FriendRequestResponse acceptRequest(Long requestId, Long currentUserId);

    @Transactional
    FriendRequestResponse declineRequest(Long requestId, Long currentUserId);

    @Transactional
    void cancelRequest(Long requestId, Long currentUserId);

    @Transactional(readOnly = true)
    List<FriendResponse> listFriends(Long currentUserId);

    @Transactional(readOnly = true)
    List<FriendRequestResponse> listInboundRequests(Long currentUserId);

    @Transactional(readOnly = true)
    List<FriendRequestResponse> listOutboundRequests(Long currentUserId);

    @Transactional
    void unfriend(Long currentUserId, Long friendUserId);

    @Transactional
    void blockUser(Long userId, Long targetUserId);

    @Transactional
    void unblockUser(Long userId, Long targetUserId);
}
