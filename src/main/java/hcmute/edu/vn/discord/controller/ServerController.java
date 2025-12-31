package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.request.ServerRequest;
import hcmute.edu.vn.discord.dto.response.CategoryResponse;
import hcmute.edu.vn.discord.dto.response.ChannelResponse;
import hcmute.edu.vn.discord.dto.response.ServerDetailResponse;
import hcmute.edu.vn.discord.dto.response.ServerMemberResponse;
import hcmute.edu.vn.discord.dto.response.ServerResponse;
import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.repository.CategoryRepository;
import hcmute.edu.vn.discord.repository.ServerMemberRepository;
import hcmute.edu.vn.discord.security.servers.ServerAuth;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
public class ServerController {
    private final ServerService serverService;
    private final CategoryRepository categoryRepository;
    private final ChannelService channelService;
    private final ServerMemberRepository serverMemberRepository;
    private final ServerAuth serverAuth;

    // 1. TẠO SERVER
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServerResponse> createServer(@Valid @RequestBody ServerRequest request) {
        Server saved = serverService.createServer(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();

        return ResponseEntity.created(location).body(ServerResponse.from(saved));
    }

    // 2. CẬP NHẬT
    @PutMapping("/{id}")
    @PreAuthorize("@serverAuth.isOwner(#id, authentication.name)")
    public ResponseEntity<ServerResponse> updateServer(@PathVariable Long id,
                                                       @Valid @RequestBody ServerRequest request) {
        Server updated = serverService.updateServer(id, request);
        return ResponseEntity.ok(ServerResponse.from(updated));
    }

    // 3. LẤY CHI TIẾT SERVER
    @GetMapping("/{id}/detail")
    @PreAuthorize("@serverAuth.isMember(#id, authentication.name)")
    public ResponseEntity<ServerDetailResponse> getServerDetail(
            @PathVariable Long id,
            Authentication authentication) {

        String username = authentication.getName();
        Server server = serverService.getServerById(id);

        List<CategoryResponse> categories = categoryRepository.findByServerId(id).stream()
                .map(CategoryResponse::from)
                .toList();

        List<ChannelResponse> channels = channelService.getChannelsByServer(id).stream()
                .filter(c -> serverAuth.canViewChannel(c.getId(), username)) // Check quyền
                .map(ChannelResponse::from) // Map Entity -> DTO
                .toList();

        List<ServerMemberResponse> members = serverMemberRepository.findByServerId(id).stream()
                .map(ServerMemberResponse::from)
                .toList();

        return ResponseEntity.ok(ServerDetailResponse.builder()
                .serverInfo(ServerResponse.from(server))
                .categories(categories)
                .channels(channels)
                .members(members)
                .build());
    }

    // 4. LẤY ALL
    @GetMapping
    public ResponseEntity<List<ServerResponse>> getAllServers() {
        List<ServerResponse> servers = serverService.getAllServers().stream()
                .map(ServerResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(servers);
    }

    // 5. GET ONE
    @GetMapping("/{id}")
    public ResponseEntity<ServerResponse> getServerById(@PathVariable Long id) {
        Server server = serverService.getServerById(id);
        return ResponseEntity.ok(ServerResponse.from(server));
    }

    // 6. GET MY SERVERS
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ServerResponse>> getMyServers() {
        List<ServerResponse> servers = serverService.getServersByCurrentUsername().stream()
                .map(ServerResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(servers);
    }

    // 7. DELETE
    @DeleteMapping("/{id}")
    @PreAuthorize("@serverAuth.isOwner(#id, authentication.name)")
    public ResponseEntity<Void> deleteServer(@PathVariable Long id) {
        serverService.deleteServer(id);
        return ResponseEntity.noContent().build();
    }
}