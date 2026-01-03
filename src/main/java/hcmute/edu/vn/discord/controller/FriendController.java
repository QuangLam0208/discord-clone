package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.request.FriendRequestAction;
import hcmute.edu.vn.discord.dto.response.FriendRequestResponse;
import hcmute.edu.vn.discord.dto.response.FriendResponse;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.service.FriendService;
import hcmute.edu.vn.discord.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {
    private final FriendService friendService;
    private final UserService userService;

    private User getCurrentUser(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new AccessDeniedException("Not authenticated");
        }
        return userService.findByUsername(auth.getName())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @PostMapping("/requests")
    public ResponseEntity<FriendRequestResponse> sendFriendRequest(@Valid @RequestBody FriendRequestAction request,
                                                                   Authentication authentication) {
        User current = getCurrentUser(authentication);
        FriendRequestResponse created = friendService.sendRequest(current.getId(), request.getTargetUserId());
        return ResponseEntity.ok(created);
    }

    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<FriendRequestResponse> acceptRequest(@PathVariable @NotNull Long requestId,
                                                               Authentication authentication) {
        User current = getCurrentUser(authentication);
        FriendRequestResponse updated = friendService.acceptRequest(requestId, current.getId());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/requests/{requestId}/decline")
    public ResponseEntity<FriendRequestResponse> declineRequest(@PathVariable @NotNull Long requestId,
                                                                Authentication authentication) {
        User current = getCurrentUser(authentication);
        FriendRequestResponse updated = friendService.declineRequest(requestId, current.getId());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/requests/{requestId}")
    public ResponseEntity<?> cancelRequest(@PathVariable @NotNull Long requestId,
                                           Authentication authentication) {
        User current = getCurrentUser(authentication);
        friendService.cancelRequest(requestId, current.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("")
    public ResponseEntity<List<FriendResponse>> listFriends(Authentication authentication) {
        User current = getCurrentUser(authentication);
        List<FriendResponse> friends = friendService.listFriends(current.getId());
        return ResponseEntity.ok(friends);
    }

    @GetMapping("/requests")
    public ResponseEntity<List<FriendRequestResponse>> listRequests(Authentication authentication,
                                                                    @RequestParam(name = "type", defaultValue = "inbound") String type) {
        User current = getCurrentUser(authentication);
        boolean inbound = "inbound".equalsIgnoreCase(type);
        List<FriendRequestResponse> requests = inbound
                ? friendService.listInboundRequests(current.getId())
                : friendService.listOutboundRequests(current.getId());
        return ResponseEntity.ok(requests);
    }

    @DeleteMapping("/{friendUserId}")
    public ResponseEntity<?> unfriend(@PathVariable Long friendUserId, Authentication authentication) {
        User current = getCurrentUser(authentication);
        friendService.unfriend(current.getId(), friendUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/block/{targetUserId}")
    public ResponseEntity<?> block(@PathVariable Long targetUserId, Authentication authentication) {
        User current = getCurrentUser(authentication);
        friendService.blockUser(current.getId(), targetUserId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/block/{targetUserId}")
    public ResponseEntity<?> unblock(@PathVariable Long targetUserId, Authentication authentication) {
        User current = getCurrentUser(authentication);
        friendService.unblockUser(current.getId(), targetUserId);
        return ResponseEntity.noContent().build();
    }
}
