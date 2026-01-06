package hcmute.edu.vn.discord.controller.api.admin;

import hcmute.edu.vn.discord.dto.request.TransferOwnerRequest;
import hcmute.edu.vn.discord.dto.request.UpdateServerStatusRequest;
import hcmute.edu.vn.discord.dto.response.AdminServerDetailResponse;
import hcmute.edu.vn.discord.dto.response.ServerMemberSummaryResponse;
import hcmute.edu.vn.discord.entity.enums.EAuditAction;
import hcmute.edu.vn.discord.entity.enums.ServerStatus;
import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.repository.CategoryRepository;
import hcmute.edu.vn.discord.repository.ChannelRepository;
import hcmute.edu.vn.discord.repository.ServerRepository;
import hcmute.edu.vn.discord.service.AuditLogMongoService;
import hcmute.edu.vn.discord.service.ServerService;
import hcmute.edu.vn.discord.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/servers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ADMIN')")
public class AdminServerController {

    private final ServerRepository serverRepository;
    private final UserService userService;
    private final ChannelRepository channelRepository;
    private final CategoryRepository categoryRepository;
    private final ServerService serverService;
    private final AuditLogMongoService auditLogService;

    // GET /api/admin/servers?page=&size=&q=
    @GetMapping
    public Page<Server> listServers(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String q) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        if (q == null || q.isBlank()) {
            return serverRepository.findAll(pageable);
        }
        return serverRepository.findByNameContainingIgnoreCase(q, pageable);
    }

    // GET /api/admin/servers/{id}
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public AdminServerDetailResponse getServer(@PathVariable Long id) {
        Server s = serverRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Server not found"));

        // Assemble Lists (limit members to 50)
        List<String> channelNames = s.getChannels().stream()
                .map(c -> c.getName() + " (" + c.getType() + ")")
                .sorted()
                .toList();

        List<String> roleNames = s.getRoles().stream()
                .map(r -> r.getName())
                .sorted()
                .toList();

        List<String> memberNames = s.getMembers().stream()
                .limit(50)
                .map(m -> m.getUser().getUsername() + (m.getNickname() != null ? " (" + m.getNickname() + ")" : ""))
                .toList();

        return AdminServerDetailResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .description(s.getDescription())
                .iconUrl(s.getIconUrl())
                .status(s.getStatus())
                .ownerId(s.getOwner() != null ? s.getOwner().getId() : null)
                .ownerUsername(s.getOwner() != null ? s.getOwner().getUsername() : null)
                .ownerDisplayName(s.getOwner() != null ? s.getOwner().getDisplayName() : null)
                .memberCount(s.getMembers().size())
                .channelCount(s.getChannels().size())
                .roleCount(s.getRoles().size())
                .channelNames(channelNames)
                .roleNames(roleNames)
                .memberNames(memberNames)
                .build();
    }

    // DELETE /api/admin/servers/{id}
    // Xử lý quan hệ Category/Channel để tránh lỗi ràng buộc
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteServer(@PathVariable Long id, Authentication authentication) {
        Server s = serverRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Server not found"));
        try {
            // 1) Gỡ liên kết Category khỏi Channels thuộc server
            channelRepository.unsetCategoryByServerId(id);

            // 2) Xóa toàn bộ Category thuộc server
            categoryRepository.deleteByServerId(id);

            // 3) Xóa server
            serverRepository.delete(s);
            serverRepository.flush();

            auditLogService.log(authentication.getName(),
                    EAuditAction.ADMIN_DELETE_SERVER.name(), s.getName(),
                    "Deleted server id: " + id);

            return ResponseEntity.ok(Map.of("message", "Server deleted", "id", id));
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Không thể xóa server do ràng buộc dữ liệu",
                    "error", ex.getMessage()));
        }
    }

    @GetMapping("/{serverId}/members")
    public ResponseEntity<List<ServerMemberSummaryResponse>> getMembers(@PathVariable Long serverId) {
        return ResponseEntity.ok(serverService.getMembersOfServer(serverId));
    }

    @PostMapping("/{serverId}/transfer-owner")
    public ResponseEntity<Void> transferOwner(@PathVariable Long serverId, @RequestBody TransferOwnerRequest req,
            Authentication authentication) {
        serverService.transferOwner(serverId, req);
        auditLogService.log(authentication.getName(),
                EAuditAction.ADMIN_UPDATE_SERVER.name(), "Server ID: " + serverId,
                "Transferred owner to member id: " + req.getNewOwnerMemberId());
        return ResponseEntity.ok().build();
    }

    // PATCH /api/admin/servers/{id}/status
    @PatchMapping("/{id}/status")
    public Server updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateServerStatusRequest req,
            Authentication authentication) {
        Server s = serverRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Server not found"));

        // Chỉ cho phép ACTIVE hoặc FREEZE theo yêu cầu mới
        ServerStatus newStatus = req.getStatus();
        if (newStatus != ServerStatus.ACTIVE && newStatus != ServerStatus.FREEZE
                && newStatus != ServerStatus.SHADOW) {
            throw new IllegalArgumentException("Invalid status. Only ACTIVE, FREEZE or SHADOW are allowed.");
        }

        s.setStatus(newStatus);
        Server saved = serverRepository.save(s);
        auditLogService.log(authentication.getName(),
                EAuditAction.ADMIN_UPDATE_SERVER.name(), s.getName(),
                "Updated status to: " + newStatus);
        return saved;
    }
}