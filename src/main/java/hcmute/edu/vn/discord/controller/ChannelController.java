package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.request.ChannelRequest;
import hcmute.edu.vn.discord.entity.enums.ChannelType;
import hcmute.edu.vn.discord.entity.enums.EPermission;
import hcmute.edu.vn.discord.entity.jpa.*;
import hcmute.edu.vn.discord.service.ChannelService;
import hcmute.edu.vn.discord.service.ServerService;
import hcmute.edu.vn.discord.service.UserService;
import hcmute.edu.vn.discord.repository.ServerMemberRepository;
import hcmute.edu.vn.discord.repository.CategoryRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
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
        return userService.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private boolean hasManagePermission(User user, Long serverId) {
        Server server = serverService.getServerById(serverId);
        if (server == null) return false;

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
    public ResponseEntity<?> createChannel(@Valid @RequestBody ChannelRequest request, Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);

            if (request.getServerId() == null || request.getName() == null) {
                return ResponseEntity.badRequest().body("Thiếu ServerId hoặc Tên kênh");
            }

            if (hasManagePermission(user, request.getServerId())) {
                logger.warn("User {} cố gắng tạo kênh trái phép tại Server {}", user.getUsername(), request.getServerId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không có quyền quản lý kênh!");
            }

            Channel channel = new Channel();
            channel.setName(request.getName());
            channel.setType(request.getType() != null ? request.getType() : ChannelType.TEXT);
            channel.setIsPrivate(request.getIsPrivate() != null ? request.getIsPrivate() : false);

            Server server = serverService.getServerById(request.getServerId());
            if (server == null) {
                return ResponseEntity.badRequest().body("Server không tồn tại");
            }
            channel.setServer(server);

            if (request.getCategoryId() != null) {
                Optional<Category> categoryOpt = categoryRepository.findById(request.getCategoryId());
                if (categoryOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body("Category ID không tồn tại!");
                }
                channel.setCategory(categoryOpt.get());
            }

            Channel newChannel = channelService.createChannel(channel);
            logger.info("User {} đã tạo kênh mới: {} (ID: {})", user.getUsername(), newChannel.getName(), newChannel.getId());

            return ResponseEntity.created(URI.create("/api/channels/" + newChannel.getId()))
                    .body(newChannel);
        } catch (Exception e) {
            logger.error("Lỗi khi tạo kênh mới: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi Server: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateChannel(@PathVariable Long id, @Valid @RequestBody ChannelRequest request, Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Optional<Channel> channelOpt = channelService.getChannelById(id);

            if (channelOpt.isEmpty()) return ResponseEntity.notFound().build();
            Channel channel = channelOpt.get();

            if (hasManagePermission(user, channel.getServer().getId())) {
                logger.warn("User {} cố gắng sửa kênh {} trái phép", user.getUsername(), id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Không có quyền sửa kênh này");
            }

            if (request.getName() != null) channel.setName(request.getName());
            if (request.getIsPrivate() != null) channel.setIsPrivate(request.getIsPrivate());

            if (request.getType() != null && !request.getType().equals(channel.getType())) {
                logger.warn("User {} attempted to change channel type from {} to {} for channel ID {}",
                        user.getUsername(), channel.getType(), request.getType(), id);
                return ResponseEntity.badRequest().body("Không thể thay đổi loại kênh (TEXT/VOICE) sau khi đã tạo");
            }

            if (request.getCategoryId() != null) {
                Optional<Category> categoryOpt = categoryRepository.findById(request.getCategoryId());
                if (categoryOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body("Category ID mới không tồn tại!");
                }
                channel.setCategory(categoryOpt.get());
            }

            Channel updated = channelService.updateChannel(id, channel);
            logger.info("User {} cập nhật kênh ID {}", user.getUsername(), id);

            return ResponseEntity.ok(updated);

        } catch (Exception e) {
            logger.error("Lỗi khi cập nhật kênh ID {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi Server: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteChannel(@PathVariable Long id, Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Optional<Channel> channelOpt = channelService.getChannelById(id);

            if (channelOpt.isEmpty()) return ResponseEntity.notFound().build();

            if (hasManagePermission(user, channelOpt.get().getServer().getId())) {
                logger.warn("User {} cố gắng xóa kênh {} trái phép", user.getUsername(), id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Không có quyền xóa kênh");
            }

            channelService.deleteChannel(id);
            logger.info("User {} đã xóa kênh ID {}", user.getUsername(), id);

            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            logger.error("Lỗi khi xóa kênh ID {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi Server: " + e.getMessage());
        }
    }

    @GetMapping("/server/{serverId}")
    public ResponseEntity<?> getChannelsByServer(@PathVariable Long serverId, Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            boolean isMember = serverMemberRepository.existsByServerIdAndUserId(serverId, user.getId());
            if (!isMember) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn chưa tham gia server này");
            }
            return ResponseEntity.ok(channelService.getChannelsByServer(serverId));
        } catch (Exception e) {
            logger.error("Lỗi khi lấy danh sách kênh server ID {}", serverId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi: " + e.getMessage());
        }
    }
}