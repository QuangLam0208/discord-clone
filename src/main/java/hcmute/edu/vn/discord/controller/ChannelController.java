package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.request.ChannelRequest;
import hcmute.edu.vn.discord.dto.response.ChannelResponse;
import hcmute.edu.vn.discord.entity.jpa.Channel;
import hcmute.edu.vn.discord.service.ChannelService;
import hcmute.edu.vn.discord.security.servers.ServerAuth;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;
    private final ServerAuth serverAuth;

    // 1. Tạo Channel (Có isPrivate)
    @PostMapping("/servers/{serverId}/channels")
    @PreAuthorize("@serverAuth.canManageChannels(#serverId, authentication.name)")
    public ResponseEntity<ChannelResponse> createChannel(
            @PathVariable Long serverId,
            @RequestBody ChannelRequest request) {

        Channel channel = channelService.createChannel(
                serverId,
                request.getName(),
                request.getType(),
                request.getCategoryId(),
                request.getIsPrivate()
        );
        return ResponseEntity.ok(ChannelResponse.from(channel));
    }

    // 2. Lấy danh sách Channel
    @GetMapping("/servers/{serverId}/channels")
    @PreAuthorize("@serverAuth.isMember(#serverId, authentication.name)")
    public ResponseEntity<List<ChannelResponse>> getChannels(
            @PathVariable Long serverId,
            Authentication authentication) {
        String username = authentication.getName();
        List<Channel> allChannels = channelService.getChannelsByServer(serverId);
        // Logic: Chỉ giữ lại các kênh mà user có quyền "canViewChannel"
        List<ChannelResponse> responses = allChannels.stream()
                .filter(channel -> serverAuth.canViewChannel(channel.getId(), username))
                .map(ChannelResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    // 3. Lấy chi tiết 1 channel (Check quyền xem luôn cho chắc)
    @GetMapping("/channels/{channelId}")
    @PreAuthorize("@serverAuth.canViewChannel(#channelId, authentication.name)")
    public ResponseEntity<ChannelResponse> getChannelDetail(@PathVariable Long channelId) {
        Channel channel = channelService.getChannelById(channelId);
        return ResponseEntity.ok(ChannelResponse.from(channel));
    }

    // 4. Update Channel
    @PatchMapping("/channels/{channelId}")
    @PreAuthorize("@serverAuth.canEditChannel(#channelId, authentication.name)")
    public ResponseEntity<ChannelResponse> updateChannel(
            @PathVariable Long channelId,
            @RequestBody ChannelRequest request) {

        Channel updated = channelService.updateChannel(
                channelId,
                request.getName(),
                request.getCategoryId()
        );
        return ResponseEntity.ok(ChannelResponse.from(updated));
    }

    // 5. Delete Channel
    @DeleteMapping("/channels/{channelId}")
    @PreAuthorize("@serverAuth.canEditChannel(#channelId, authentication.name)")
    public ResponseEntity<?> deleteChannel(@PathVariable Long channelId) {
        channelService.deleteChannel(channelId);
        return ResponseEntity.noContent().build();
    }
}