package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.request.ChannelRequest;
import hcmute.edu.vn.discord.entity.jpa.Category;
import hcmute.edu.vn.discord.entity.jpa.Channel;
import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.service.CategoryService;
import hcmute.edu.vn.discord.service.ChannelService;
import hcmute.edu.vn.discord.service.ServerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;
    private final ServerService serverService;
    private final CategoryService categoryService;

    // Helper Check Auth
    private String validateAndGetUsername(Authentication authentication) {
        if (authentication == null) {
            throw new BadCredentialsException("Lỗi xác thực: Token không hợp lệ hoặc thiếu.");
        }
        if (!authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new BadCredentialsException("Người dùng chưa đăng nhập.");
        }
        return authentication.getName();
    }

    @PostMapping
    public ResponseEntity<Channel> createChannel(
            @Valid @RequestBody ChannelRequest request,
            Authentication authentication) {

        String username = validateAndGetUsername(authentication);

        Channel channel = new Channel();
        channel.setName(request.getName());
        channel.setIsPrivate(request.getIsPrivate());
        channel.setType(request.getType());

        if (request.getServerId() != null) {
            Server server = serverService.getServerById(request.getServerId());
            channel.setServer(server);
        }

        if (request.getCategoryId() != null) {
            Category category = categoryService.getCategoryById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            channel.setCategory(category);
        }

        return ResponseEntity.ok(channelService.createChannel(channel, username));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Channel> updateChannel(
            @PathVariable Long id,
            @Valid @RequestBody ChannelRequest request,
            Authentication authentication) { // Thêm Auth

        String username = validateAndGetUsername(authentication);

        Channel channel = new Channel();
        channel.setName(request.getName());
        channel.setIsPrivate(request.getIsPrivate());
        channel.setType(request.getType());

        if (request.getServerId() != null) {
            Server server = serverService.getServerById(request.getServerId());
            channel.setServer(server);
        }

        if (request.getCategoryId() != null) {
            Category category = categoryService.getCategoryById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            channel.setCategory(category);
        }
        // Truyền username xuống Service để check quyền
        return ResponseEntity.ok(channelService.updateChannel(id, channel, username));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChannel(
            @PathVariable Long id,
            Authentication authentication) { // Thêm Auth

        String username = validateAndGetUsername(authentication);
        // Truyền username xuống Service để check quyền
        channelService.deleteChannel(id, username);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Channel> getChannelById(@PathVariable Long id) {
        return channelService.getChannelById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Channel>> getAllChannels() {
        return ResponseEntity.ok(channelService.getAllChannels());
    }

    @GetMapping("/server/{serverId}")
    public ResponseEntity<List<Channel>> getChannelsByServer(@PathVariable Long serverId,
                                                              Authentication authentication) {
        // Ensure the user is authenticated before accessing server channels
        String username = validateAndGetUsername(authentication);
        return ResponseEntity.ok(channelService.getChannelsByServer(serverId));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<Channel>> getChannelsByCategory(@PathVariable Long categoryId){
        return ResponseEntity.ok(channelService.getChannelsByCategory(categoryId));
    }
}