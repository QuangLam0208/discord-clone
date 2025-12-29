package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.request.ChannelRequest;
import hcmute.edu.vn.discord.dto.response.ChannelResponse;
import hcmute.edu.vn.discord.entity.jpa.Category;
import hcmute.edu.vn.discord.entity.jpa.Channel;
import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.service.CategoryService;
import hcmute.edu.vn.discord.service.ChannelService;
import hcmute.edu.vn.discord.service.ServerService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;
    private final ServerService serverService;
    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("@serverAuth.canManageChannels(#request.serverId, principal.name)")
    public ResponseEntity<ChannelResponse> createChannel(
            @Valid @RequestBody ChannelRequest request,
            Principal principal) {

        String username = principal.getName();

        Channel channel = new Channel();
        channel.setName(request.getName());
        channel.setIsPrivate(request.getIsPrivate());
        channel.setType(request.getType());

        if (request.getServerId() == null) {
            throw new IllegalArgumentException("serverId là bắt buộc");
        }
        Server server = serverService.getServerById(request.getServerId());
        channel.setServer(server);

        if (request.getCategoryId() != null) {
            Category category = categoryService.getCategoryById(request.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found"));
            if (!category.getServer().getId().equals(server.getId())) {
                throw new IllegalArgumentException("Category không thuộc về server này");
            }
            channel.setCategory(category);
        }

        Channel saved = channelService.createChannel(channel, username);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();

        return ResponseEntity.created(location).body(ChannelResponse.from(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@serverAuth.canManageChannels(#request.serverId != null ? #request.serverId : @serverAuth.serverIdOfChannel(#id), principal.name)")
    public ResponseEntity<ChannelResponse> updateChannel(
            @PathVariable Long id,
            @Valid @RequestBody ChannelRequest request,
            Principal principal) {

        String username = principal.getName();

        Channel channel = new Channel();
        channel.setName(request.getName());
        channel.setIsPrivate(request.getIsPrivate());
        channel.setType(request.getType());

        Server server = null;
        if (request.getServerId() != null) {
            server = serverService.getServerById(request.getServerId());
            channel.setServer(server);
        }

        if (request.getCategoryId() != null) {
            Category category = categoryService.getCategoryById(request.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found"));
            if (server != null && !category.getServer().getId().equals(server.getId())) {
                throw new IllegalArgumentException("Category không thuộc về server này");
            }
            channel.setCategory(category);
        }

        Channel updated = channelService.updateChannel(id, channel, username);
        return ResponseEntity.ok(ChannelResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@serverAuth.canManageChannels(@serverAuth.serverIdOfChannel(#id), principal.name)")
    public ResponseEntity<Void> deleteChannel(@PathVariable Long id, Principal principal) {
        channelService.deleteChannel(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@serverAuth.canViewChannel(#id, principal.name)")
    public ResponseEntity<ChannelResponse> getChannelById(@PathVariable Long id, Principal principal) {
        return channelService.getChannelById(id)
                .map(ch -> ResponseEntity.ok(ChannelResponse.from(ch)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<ChannelResponse>> getAllChannels() {
        List<ChannelResponse> data = channelService.getAllChannels().stream()
                .map(ChannelResponse::from)
                .toList();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/server/{serverId}")
    @PreAuthorize("@serverAuth.isMember(#serverId, principal.name)")
    public ResponseEntity<List<ChannelResponse>> getChannelsByServer(@PathVariable Long serverId,
                                                                     Principal principal) {
        List<ChannelResponse> data = channelService.getChannelsByServerVisibleToUser(serverId, principal.getName()).stream()
                .map(ChannelResponse::from)
                .toList();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/category/{categoryId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChannelResponse>> getChannelsByCategory(@PathVariable Long categoryId,
                                                                       Principal principal){
        List<ChannelResponse> data = channelService.getChannelsByCategoryVisibleToUser(categoryId, principal.getName()).stream()
                .map(ChannelResponse::from)
                .toList();
        return ResponseEntity.ok(data);
    }
}