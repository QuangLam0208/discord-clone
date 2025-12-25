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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/channels")
public class ChannelController {

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

    // --- HELPER: Check quyền  ---
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

        // 3. Duyệt Role & Permission (Dùng Enum EPermission để so sánh)
        for (ServerRole role : member.getRoles()) {
            for (Permission perm : role.getPermissions()) {
                // Check xem code trong DB có trùng với Enum MANAGE_CHANNELS hoặc ADMIN không
                String code = perm.getCode();
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

            // Validate
            if (request.getServerId() == null || request.getName() == null) {
                return ResponseEntity.badRequest().body("Thiếu ServerId hoặc Tên kênh");
            }

            // Check quyền
            if (!hasManagePermission(user, request.getServerId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không có quyền quản lý kênh!");
            }

            // Map DTO -> Entity
            Channel channel = new Channel();
            channel.setName(request.getName());
            channel.setType(request.getType() != null ? request.getType() : ChannelType.TEXT);
            channel.setIsPrivate(request.getIsPrivate() != null ? request.getIsPrivate() : false);

            // Set Server
            Server server = serverService.getServerById(request.getServerId());
            channel.setServer(server);

            // Set Category (nếu có)
            if (request.getCategoryId() != null) {
                categoryRepository.findById(request.getCategoryId()).ifPresent(channel::setCategory);
            }

            return ResponseEntity.ok(channelService.createChannel(channel));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi: " + e.getMessage());
        }
    }

    // --- API 2: Cập nhật kênh (Sửa tên, chuyển category...) ---
    @PutMapping("/{id}")
    public ResponseEntity<?> updateChannel(@PathVariable Long id, @RequestBody ChannelRequest request) {
        User user = getCurrentUser();
        Optional<Channel> channelOpt = channelService.getChannelById(id);

        if (channelOpt.isEmpty()) return ResponseEntity.notFound().build();
        Channel channel = channelOpt.get();

        // Check quyền (dựa trên server chứa kênh đó)
        if (!hasManagePermission(user, channel.getServer().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Không có quyền sửa kênh này");
        }

        // Update fields
        if (request.getName() != null) channel.setName(request.getName());
        if (request.getIsPrivate() != null) channel.setIsPrivate(request.getIsPrivate());
        if (request.getCategoryId() != null) {
            categoryRepository.findById(request.getCategoryId()).ifPresent(channel::setCategory);
        }

        return ResponseEntity.ok(channelService.updateChannel(id, channel));
    }

    // --- API 3: Xóa kênh ---
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteChannel(@PathVariable Long id) {
        User user = getCurrentUser();
        Optional<Channel> channelOpt = channelService.getChannelById(id);

        if (channelOpt.isEmpty()) return ResponseEntity.notFound().build();

        // Check quyền
        if (!hasManagePermission(user, channelOpt.get().getServer().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Không có quyền xóa kênh");
        }

        channelService.deleteChannel(id);
        return ResponseEntity.ok("Đã xóa kênh");
    }
}