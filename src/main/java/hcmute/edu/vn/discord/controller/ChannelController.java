package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.request.ChannelRequest;
import hcmute.edu.vn.discord.dto.response.ChannelResponse;
import hcmute.edu.vn.discord.entity.enums.ChannelType;
import hcmute.edu.vn.discord.entity.enums.EPermission;
import hcmute.edu.vn.discord.entity.jpa.*;
import hcmute.edu.vn.discord.repository.CategoryRepository;
import hcmute.edu.vn.discord.repository.ServerMemberRepository;
import hcmute.edu.vn.discord.service.ChannelService;
import hcmute.edu.vn.discord.service.ServerService;
import hcmute.edu.vn.discord.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelController {

    private static final Logger logger = LoggerFactory.getLogger(ChannelController.class);

    private final ChannelService channelService;
    private final ServerService serverService;
    private final UserService userService;
    private final ServerMemberRepository serverMemberRepository;
    private final CategoryRepository categoryRepository;

    private User getCurrentUser(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new AccessDeniedException("Not authenticated");
        }
        return userService.findByUsername(auth.getName())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    private boolean hasManagePermission(User user, Long serverId) {
        Server server = serverService.getServerById(serverId);

        User owner = server.getOwner();
        if (owner != null && owner.getId() != null && owner.getId().equals(user.getId())) {
            return true;
        }

        Optional<ServerMember> memberOpt = serverMemberRepository.findByServerIdAndUserId(serverId, user.getId());
        if (memberOpt.isEmpty()) return false;
        ServerMember member = memberOpt.get();

        for (ServerRole role : member.getRoles()) {
            for (Permission perm : role.getPermissions()) {
                String code = perm.getCode();
                if (EPermission.MANAGE_CHANNELS.name().equals(code) ||
                        EPermission.ADMIN.name().equals(code)) {
                    return true;
                }
            }
        }
        return false;
    }

    @PostMapping
    public ResponseEntity<ChannelResponse> createChannel(@Valid @RequestBody ChannelRequest request,
                                                         Authentication authentication) {
        User user = getCurrentUser(authentication);

        if (request.getServerId() == null || request.getName() == null) {
            throw new IllegalArgumentException("Thiếu ServerId hoặc tên kênh");
        }

        if (!hasManagePermission(user, request.getServerId())) {
            logger.warn("User {} cố gắng tạo kênh trái phép tại Server {}", user.getUsername(), request.getServerId());
            throw new AccessDeniedException("Bạn không có quyền quản lý kênh!");
        }

        Server server = serverService.getServerById(request.getServerId());

        Channel channel = new Channel();
        channel.setName(request.getName());
        channel.setType(request.getType() != null ? request.getType() : ChannelType.TEXT);
        channel.setIsPrivate(request.getIsPrivate() != null ? request.getIsPrivate() : false);
        channel.setServer(server);

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found"));
            channel.setCategory(category);
        }

        Channel newChannel = channelService.createChannel(channel);
        logger.info("User {} đã tạo kênh mới: {} (ID: {})", user.getUsername(), newChannel.getName(), newChannel.getId());

        return ResponseEntity.created(URI.create("/api/channels/" + newChannel.getId()))
                .body(ChannelResponse.from(newChannel));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChannelResponse> updateChannel(@PathVariable Long id,
                                                         @Valid @RequestBody ChannelRequest request,
                                                         Authentication authentication) {
        User user = getCurrentUser(authentication);

        Channel existing = channelService.getChannelById(id)
                .orElseThrow(() -> new EntityNotFoundException("Channel not found"));

        if (!hasManagePermission(user, existing.getServer().getId())) {
            logger.warn("User {} cố gắng sửa kênh {} trái phép", user.getUsername(), id);
            throw new AccessDeniedException("Không có quyền sửa kênh này");
        }

        if (request.getType() != null && !request.getType().equals(existing.getType())) {
            logger.warn("User {} attempted to change channel type from {} to {} for channel ID {}",
                    user.getUsername(), existing.getType(), request.getType(), id);
            throw new IllegalArgumentException("Không thể thay đổi loại kênh (TEXT/VOICE) sau khi đã tạo");
        }

        if (request.getName() != null) existing.setName(request.getName());
        if (request.getIsPrivate() != null) existing.setIsPrivate(request.getIsPrivate());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found"));
            existing.setCategory(category);
        }

        Channel updated = channelService.updateChannel(id, existing);
        logger.info("User {} cập nhật kênh ID {}", user.getUsername(), id);

        return ResponseEntity.ok(ChannelResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteChannel(@PathVariable Long id, Authentication authentication) {
        User user = getCurrentUser(authentication);

        Channel existing = channelService.getChannelById(id)
                .orElseThrow(() -> new EntityNotFoundException("Channel not found"));

        if (!hasManagePermission(user, existing.getServer().getId())) {
            logger.warn("User {} cố gắng xóa kênh {} trái phép", user.getUsername(), id);
            throw new AccessDeniedException("Không có quyền xóa kênh");
        }

        channelService.deleteChannel(id);
        logger.info("User {} đã xóa kênh ID {}", user.getUsername(), id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/server/{serverId}")
    public ResponseEntity<List<ChannelResponse>> getChannelsByServer(@PathVariable Long serverId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        boolean isMember = serverMemberRepository.existsByServerIdAndUserId(serverId, user.getId());
        if (!isMember) {
            throw new AccessDeniedException("Bạn chưa tham gia server này");
        }
        List<ChannelResponse> channels = channelService.getChannelsByServer(serverId).stream()
                .map(ChannelResponse::from)
                .toList();
        return ResponseEntity.ok(channels);
    }
}