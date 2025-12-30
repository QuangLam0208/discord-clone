package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.request.ChannelRequest;
import hcmute.edu.vn.discord.dto.response.ChannelResponse;
import hcmute.edu.vn.discord.service.ChannelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    @PostMapping
    // Check quyền: Phải có quyền MANAGE_CHANNELS trên server gửi lên
    @PreAuthorize("@serverAuth.canManageChannels(#request.serverId, authentication.name)")
    public ResponseEntity<ChannelResponse> createChannel(@Valid @RequestBody ChannelRequest request) {
        ChannelResponse created = channelService.createChannel(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    // Check quyền: Lấy serverId từ ChannelId để check
    @PreAuthorize("@serverAuth.canManageChannels(@serverAuth.serverIdOfChannel(#id), authentication.name)")
    public ResponseEntity<ChannelResponse> updateChannel(@PathVariable Long id,
                                                         @Valid @RequestBody ChannelRequest request) {
        return ResponseEntity.ok(channelService.updateChannel(id, request));
    }

    @DeleteMapping("/{id}")
    // Check quyền: Tương tự update, cần quyền MANAGE_CHANNELS
    @PreAuthorize("@serverAuth.canManageChannels(@serverAuth.serverIdOfChannel(#id), authentication.name)")
    public ResponseEntity<Void> deleteChannel(@PathVariable Long id) {
        channelService.deleteChannel(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    // Check quyền: VIEW_CHANNELS (quan trọng: Channel Private cần check kỹ hơn Category)
    @PreAuthorize("@serverAuth.canViewChannel(#id, authentication.name)")
    public ResponseEntity<ChannelResponse> getChannelById(@PathVariable Long id) {
        return ResponseEntity.ok(channelService.getChannelById(id));
    }

    @GetMapping("/server/{serverId}")
    // Chỉ thành viên server mới được xem danh sách channel
    @PreAuthorize("@serverAuth.isMember(#serverId, authentication.name)")
    public ResponseEntity<List<ChannelResponse>> getChannelsByServer(@PathVariable Long serverId) {
        return ResponseEntity.ok(channelService.getChannelsByServer(serverId));
    }
}