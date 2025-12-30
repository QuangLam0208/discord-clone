package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.request.ServerRequest;
import hcmute.edu.vn.discord.dto.response.ServerDetailResponse;
import hcmute.edu.vn.discord.dto.response.ServerResponse;
import hcmute.edu.vn.discord.service.ServerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
public class ServerController {

    private static final Logger log = LoggerFactory.getLogger(ServerController.class);
    private final ServerService serverService;

    // 1. TẠO SERVER (Tự động tạo Owner, Role Admin và Channel mặc định)
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServerResponse> createServer(@Valid @RequestBody ServerRequest request) {
        ServerResponse saved = serverService.createServer(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();

        return ResponseEntity.created(location).body(saved);
    }

    // 2. CẬP NHẬT THÔNG TIN SERVER (Tên, Mô tả, Icon)
    @PutMapping("/{id}")
    @PreAuthorize("@serverAuth.isOwner(#id, authentication.name)")
    public ResponseEntity<ServerResponse> updateServer(@PathVariable Long id,
                                                       @Valid @RequestBody ServerRequest request) {
        return ResponseEntity.ok(serverService.updateServer(id, request));
    }

    // 3. LẤY CHI TIẾT SERVER TỔNG HỢP (Gồm Info + Categories + Channels + Members)
    // Endpoint này giúp FE load 1 lần là đủ dữ liệu vào giao diện chính
    @GetMapping("/{id}/detail")
    @PreAuthorize("@serverAuth.isMember(#id, authentication.name)")
    public ResponseEntity<ServerDetailResponse> getServerDetail(@PathVariable Long id) {
        return ResponseEntity.ok(serverService.getServerDetail(id));
    }

    // 4. LẤY TẤT CẢ SERVER (Dùng cho trang khám phá hoặc Admin)
    @GetMapping
    public ResponseEntity<List<ServerResponse>> getAllServers() {
        List<ServerResponse> servers = serverService.getAllServers().stream()
                .map(ServerResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(servers);
    }

    // 5. LẤY THÔNG TIN 1 SERVER CƠ BẢN
    @GetMapping("/{id}")
    public ResponseEntity<ServerResponse> getServerById(@PathVariable Long id) {
        return ResponseEntity.ok(ServerResponse.from(serverService.getServerById(id)));
    }

    // 6. LẤY DANH SÁCH SERVER MÀ USER HIỆN TẠI ĐANG THAM GIA
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ServerResponse>> getMyServers() {
        List<ServerResponse> servers = serverService.getServersByCurrentUsername().stream()
                .map(ServerResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(servers);
    }

    // 7. XÓA SERVER (Chỉ Owner mới có quyền)
    @DeleteMapping("/{id}")
    @PreAuthorize("@serverAuth.isOwner(#id, authentication.name)")
    public ResponseEntity<Void> deleteServer(@PathVariable Long id) {
        serverService.deleteServer(id);
        return ResponseEntity.noContent().build();
    }
}