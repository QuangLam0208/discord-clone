package hcmute.edu.vn.discord.controller.api;

import hcmute.edu.vn.discord.dto.request.ServerRequest;
import hcmute.edu.vn.discord.dto.response.*;
import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.repository.CategoryRepository;
import hcmute.edu.vn.discord.repository.ServerMemberRepository;
import hcmute.edu.vn.discord.security.servers.ServerAuth;
import hcmute.edu.vn.discord.service.AuditLogService;
import hcmute.edu.vn.discord.service.ChannelService;
import hcmute.edu.vn.discord.service.ServerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
public class ServerController {

    private final ServerService serverService;
    private final CategoryRepository categoryRepository;
    private final ChannelService channelService;
    private final ServerMemberRepository serverMemberRepository;
    private final ServerAuth serverAuth;
    private final AuditLogService auditLogService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServerResponse> createServer(@Valid @RequestBody ServerRequest request) {
        request.normalize();
        Server saved = serverService.createServer(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location).body(ServerResponse.from(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@serverAuth.isOwner(#id, authentication.name) or @serverAuth.hasPermission(#id, authentication.name, 'MANAGE_SERVER')")
    public ResponseEntity<ServerResponse> updateServer(@PathVariable Long id,
            @Valid @RequestBody ServerRequest request) {
        request.normalize();
        Server updated = serverService.updateServer(id, request);
        return ResponseEntity.ok(ServerResponse.from(updated));
    }

    @GetMapping("/{id}/detail")
    @PreAuthorize("@serverAuth.isMember(#id, authentication.name)")
    public ResponseEntity<ServerDetailResponse> getServerDetail(@PathVariable Long id, Authentication authentication) {
        String username = authentication.getName();
        Server server = serverService.getServerById(id);

        List<CategoryResponse> categories = categoryRepository.findByServerId(id).stream()
                .map(CategoryResponse::from).toList();

        List<ChannelResponse> channels = channelService.getChannelsByServer(id).stream()
                .filter(c -> serverAuth.canViewChannel(c.getId(), username))
                .map(ChannelResponse::from).toList();

        List<ServerMemberResponse> members = serverMemberRepository.findByServerId(id).stream()
                .map(ServerMemberResponse::from).toList();

        return ResponseEntity.ok(ServerDetailResponse.builder()
                .serverInfo(ServerResponse.from(server))
                .categories(categories)
                .channels(channels)
                .members(members)
                .build());
    }

    @GetMapping
    public ResponseEntity<List<ServerResponse>> getAllServers() {
        return ResponseEntity.ok(serverService.getAllServers().stream()
                .map(ServerResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServerResponse> getServerById(@PathVariable Long id) {
        ServerResponse response = ServerResponse.from(serverService.getServerById(id));
        if (response != null) {
            response.setOnlineCount(serverService.countOnlineMembers(id));
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ServerResponse>> getMyServers() {
        // Dùng method trả DTO đã có counts, tránh đụng LAZY
        return ResponseEntity.ok(serverService.getMyServersResponses());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@serverAuth.isOwner(#id, authentication.name)")
    public ResponseEntity<Void> deleteServer(@PathVariable Long id) {
        serverService.deleteServer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/audit-logs")
    @PreAuthorize("@serverAuth.isOwner(#id, authentication.name) or @serverAuth.hasPermission(#id, authentication.name, 'VIEW_AUDIT_LOG')")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogs(@PathVariable Long id) {
        return ResponseEntity.ok(auditLogService.getAuditLogs(id).stream()
                .map(AuditLogResponse::from).toList());
    }

    @PostMapping("/{id}/transfer-owner")
    @PreAuthorize("@serverAuth.isOwner(#id, authentication.name)")
    public ResponseEntity<Void> transferOwner(@PathVariable Long id,
            @RequestBody hcmute.edu.vn.discord.dto.request.TransferOwnerRequest req) {
        serverService.transferOwner(id, req);
        return ResponseEntity.ok().build();
    }
}