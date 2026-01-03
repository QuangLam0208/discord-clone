package hcmute.edu.vn.discord.controller.api.admin;

import hcmute.edu.vn.discord.dto.request.TransferOwnerRequest;
import hcmute.edu.vn.discord.dto.request.UpdateServerStatusRequest;
import hcmute.edu.vn.discord.entity.enums.ServerStatus;
import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.repository.CategoryRepository;
import hcmute.edu.vn.discord.repository.ChannelRepository;
import hcmute.edu.vn.discord.repository.ServerRepository;
import hcmute.edu.vn.discord.repository.UserRepository;
import hcmute.edu.vn.discord.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/servers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ADMIN')") // theo hệ thống của bạn
public class AdminServerController {

    private final ServerRepository serverRepository;
    private final UserService userService;
    private final ChannelRepository channelRepository;
    private final CategoryRepository categoryRepository;

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
    public Server getServer(@PathVariable Long id) {
        return serverRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Server not found"));
    }

    // DELETE /api/admin/servers/{id}
    // Xử lý quan hệ Category/Channel để tránh lỗi ràng buộc
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteServer(@PathVariable Long id) {
        Server s = serverRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Server not found"));
        try {
            // 1) Gỡ liên kết Category khỏi Channels thuộc server
            channelRepository.unsetCategoryByServerId(id);

            // 2) Xóa toàn bộ Category thuộc server
            categoryRepository.deleteByServerId(id);

            // 3) Xóa server
            serverRepository.delete(s);
            serverRepository.flush();

            return ResponseEntity.ok(Map.of("message", "Server deleted", "id", id));
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Không thể xóa server do ràng buộc dữ liệu",
                    "error", ex.getMessage()
            ));
        }
    }

    // POST /api/admin/servers/{id}/transfer-owner
    @PostMapping("/{id}/transfer-owner")
    public Server transferOwner(@PathVariable Long id, @Valid @RequestBody TransferOwnerRequest req) {
        Server s = serverRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Server not found"));
        User newOwner = (req.getNewOwnerUsername() != null)
                ? userService.findByUsername(req.getNewOwnerUsername()).orElse(null)
                : (req.getNewOwnerId() != null
                ? userService.findById(req.getNewOwnerId()).orElse(null)
                : null);
        if (newOwner == null) {
            throw new EntityNotFoundException("New owner not found");
        }
        s.setOwner(newOwner);
        return serverRepository.save(s);
    }

    // PATCH /api/admin/servers/{id}/status
    @PatchMapping("/{id}/status")
    public Server updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateServerStatusRequest req) {
        Server s = serverRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Server not found"));

        // Chỉ cho phép ACTIVE hoặc FREEZE theo yêu cầu mới
        ServerStatus newStatus = req.getStatus();
        if (newStatus != ServerStatus.ACTIVE && newStatus != ServerStatus.FREEZE) {
            throw new IllegalArgumentException("Invalid status. Only ACTIVE or FREEZE are allowed.");
        }

        s.setStatus(newStatus);
        return serverRepository.save(s);
    }
}