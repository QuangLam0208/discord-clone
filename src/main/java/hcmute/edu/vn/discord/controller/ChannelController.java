package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.request.ChannelPermissionRequest;
import hcmute.edu.vn.discord.dto.request.ChannelRequest;
import hcmute.edu.vn.discord.dto.response.ChannelResponse;
import hcmute.edu.vn.discord.service.ChannelService;
import hcmute.edu.vn.discord.security.servers.ServerAuth;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;
    private final ServerAuth serverAuth;

    @PostMapping("/servers/{serverId}/channels")
    @PreAuthorize("@serverAuth.canManageChannels(#serverId, authentication.name)")
    public ResponseEntity<ChannelResponse> createChannel(
            @PathVariable Long serverId,
            @RequestBody @Valid ChannelRequest request,
            Authentication authentication) {

        request.normalize();

        ChannelResponse created = ChannelResponse.from(channelService.createChannel(
                serverId,
                request.getName(),
                request.getType(),
                request.getCategoryId(),
                request.getIsPrivate(),
                authentication.getName()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/servers/{serverId}/channels")
    @PreAuthorize("@serverAuth.isMember(#serverId, authentication.name)")
    public ResponseEntity<List<ChannelResponse>> getChannels(
            @PathVariable Long serverId,
            Authentication authentication) {
        String username = authentication.getName();
        List<ChannelResponse> responses = channelService.getChannelsByServer(serverId)
                .stream()
                .filter(channel -> serverAuth.canViewChannel(channel.getId(), username))
                .map(ChannelResponse::from)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/channels/{channelId}")
    @PreAuthorize("@serverAuth.canViewChannel(#channelId, authentication.name)")
    public ResponseEntity<ChannelResponse> getChannelDetail(@PathVariable Long channelId) {
        return ResponseEntity.ok(ChannelResponse.from(channelService.getChannelById(channelId)));
    }

    @PatchMapping("/channels/{channelId}")
    @PreAuthorize("@serverAuth.canEditChannel(#channelId, authentication.name)")
    public ResponseEntity<ChannelResponse> updateChannel(
            @PathVariable Long channelId,
            @RequestBody @Valid ChannelRequest request) {

        request.normalize();
        ChannelResponse updated = ChannelResponse.from(channelService.updateChannel(
                channelId,
                request.getName(),
                request.getDescription(),
                request.getCategoryId(),
                request.getIsPrivate()));
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/channels/{channelId}")
    @PreAuthorize("@serverAuth.canEditChannel(#channelId, authentication.name)")
    public ResponseEntity<Void> deleteChannel(@PathVariable Long channelId) {
        channelService.deleteChannel(channelId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/channels/{channelId}/permissions")
    @PreAuthorize("@serverAuth.canEditChannel(#channelId, authentication.name)")
    public ResponseEntity<ChannelResponse> updateChannelPermissions(
            @PathVariable Long channelId,
            @RequestBody ChannelPermissionRequest request) {
        return ResponseEntity.ok().build();
    }
}