package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.ChannelRequest;
import hcmute.edu.vn.discord.entity.enums.ChannelType;
import hcmute.edu.vn.discord.entity.enums.EPermission;
import hcmute.edu.vn.discord.entity.jpa.*;
import hcmute.edu.vn.discord.service.ChannelService;
import hcmute.edu.vn.discord.service.ServerService;
import hcmute.edu.vn.discord.service.UserService;
import hcmute.edu.vn.discord.repository.ServerMemberRepository;
import hcmute.edu.vn.discord.repository.CategoryRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/channels")
public class ChannelController {

    // Khởi tạo Logger để ghi log hệ thống
    private static final Logger logger = LoggerFactory.getLogger(ChannelController.class);

    @Autowired
    private ChannelService channelService;
    @Autowired
    private ServerService serverService;
    @Autowired
    private UserService userService;
    @Autowired
    private ServerMemberRepository serverMemberRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    // --- HELPER: Lấy User đang đăng nhập ---
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userService.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // --- HELPER: Check quyền ---
    private boolean hasManagePermission(User user, Long serverId) {
        Server server = serverService.getServerById(serverId);
        if (server == null) return false;

        // 1. Nếu là Owner -> Auto có quyền
        if (server.getOwner().getId().equals(user.getId())) {
            return true;
        }

        // 2. Tìm Member trong Server
        Optional<ServerMember> memberOpt = serverMemberRepository.findByServerIdAndUserId(serverId, user.getId());
        if (memberOpt.isEmpty()) return false;
        ServerMember member = memberOpt.get();

        // 3. Duyệt Role & Permission
        for (ServerRole role : member.getRoles()) {
            for (Permission perm : role.getPermissions()) {
                String code = perm.getCode();
                // Check xem code trong DB có trùng với Enum MANAGE_CHANNELS hoặc ADMIN không
                if (EPermission.MANAGE_CHANNELS.name().equals(code) ||
                        EPermission.ADMIN.name().equals(code)) {
                    return true;
                }
            }
        }
        return false;
    }

    // --- API 1: Tạo kênh ---
    @PostMapping("/create")
    public ResponseEntity<?> createChannel(@RequestBody ChannelRequest request) {
        try {
            User user = getCurrentUser();

            // Validate dữ liệu
            if (request.getServerId() == null || request.getName() == null) {
                return ResponseEntity.badRequest().body("Thiếu ServerId hoặc Tên kênh");
            }

            // Check quyền
            if (!hasManagePermission(user, request.getServerId())) {
                logger.warn("User {} cố gắng tạo kênh trái phép tại Server {}", user.getUsername(), request.getServerId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không có quyền quản lý kênh!");
            }

            // Map DTO -> Entity
            Channel channel = new Channel();
            channel.setName(request.getName());
            channel.setType(request.getType() != null ? request.getType() : ChannelType.TEXT);
            channel.setIsPrivate(request.getIsPrivate() != null ? request.getIsPrivate() : false);

            // Set Server
            Server server = serverService.getServerById(request.getServerId());
            if (server == null) {
                return ResponseEntity.badRequest().body("Server không tồn tại");
            }
            channel.setServer(server);

            // Xử lý Category
            if (request.getCategoryId() != null) {
                Optional<Category> categoryOpt = categoryRepository.findById(request.getCategoryId());
                if (categoryOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body("Category ID không tồn tại!");
                }
                channel.setCategory(categoryOpt.get());
            }

            Channel newChannel = channelService.createChannel(channel);
            logger.info("User {} đã tạo kênh mới: {} (ID: {})", user.getUsername(), newChannel.getName(), newChannel.getId());

            return ResponseEntity.ok(newChannel);

        } catch (Exception e) {
            logger.error("Lỗi khi tạo kênh mới: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi Server: " + e.getMessage());
        }
    }

    // --- API 2: Cập nhật kênh ---
    @PutMapping("/{id}")
    public ResponseEntity<?> updateChannel(@PathVariable Long id, @RequestBody ChannelRequest request) {
        try {
            User user = getCurrentUser();
            Optional<Channel> channelOpt = channelService.getChannelById(id);

            if (channelOpt.isEmpty()) return ResponseEntity.notFound().build();
            Channel channel = channelOpt.get();

            // Check quyền (dựa trên server chứa kênh đó)
            if (!hasManagePermission(user, channel.getServer().getId())) {
                logger.warn("User {} cố gắng sửa kênh {} trái phép", user.getUsername(), id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Không có quyền sửa kênh này");
            }

            // Update fields
            if (request.getName() != null) channel.setName(request.getName());
            if (request.getIsPrivate() != null) channel.setIsPrivate(request.getIsPrivate());

            // Xử lý Category khi update
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
            logger.error("Lỗi khi cập nhật kênh ID " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi Server: " + e.getMessage());
        }
    }

    // --- API 3: Xóa kênh ---
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteChannel(@PathVariable Long id) {
        try {
            User user = getCurrentUser();
            Optional<Channel> channelOpt = channelService.getChannelById(id);

            if (channelOpt.isEmpty()) return ResponseEntity.notFound().build();

            // Check quyền
            if (!hasManagePermission(user, channelOpt.get().getServer().getId())) {
                logger.warn("User {} cố gắng xóa kênh {} trái phép", user.getUsername(), id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Không có quyền xóa kênh");
            }

            channelService.deleteChannel(id);
            logger.info("User {} đã xóa kênh ID {}", user.getUsername(), id);

            return ResponseEntity.ok("Đã xóa kênh");

        } catch (Exception e) {
            logger.error("Lỗi khi xóa kênh ID " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi Server: " + e.getMessage());
        }
    }

    // --- API 4: Lấy danh sách kênh ---
    @GetMapping("/server/{serverId}")
    public ResponseEntity<?> getChannelsByServer(@PathVariable Long serverId) {
        try {
            User user = getCurrentUser();
            // Check thành viên
            boolean isMember = serverMemberRepository.existsByServerIdAndUserId(serverId, user.getId());
            if (!isMember) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn chưa tham gia server này");
            }
            return ResponseEntity.ok(channelService.getChannelsByServer(serverId));
        } catch (Exception e) {
            logger.error("Lỗi khi lấy danh sách kênh server ID " + serverId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi: " + e.getMessage());
        }
    }
}