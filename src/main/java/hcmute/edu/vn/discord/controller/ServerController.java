package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.request.ServerRequest;
import hcmute.edu.vn.discord.dto.response.ServerResponse;
import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.service.ServerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
public class ServerController {

    private final ServerService serverService;

    @PostMapping
    public ResponseEntity<ServerResponse> createServer(@Valid @RequestBody ServerRequest request,
                                                       Authentication authentication) {

        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Not authenticated");
        }

        Server server = new Server();
        server.setName(request.getName());
        server.setDescription(request.getDescription());
        server.setIconUrl(request.getIconUrl());

        Server saved = serverService.createServer(server, authentication.getName());

        return ResponseEntity.created(URI.create("/api/servers/" + saved.getId()))
                .body(ServerResponse.from(saved));
    }

    @GetMapping
    public ResponseEntity<?> getAllServers() {
        return ResponseEntity.ok(
                serverService.getAllServers().stream()
                        .map(ServerResponse::from)
                        .toList()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServerResponse> getServerById(@PathVariable Long id) {
        return ResponseEntity.ok(ServerResponse.from(serverService.getServerById(id)));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyServers(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Not authenticated");
        }

        return ResponseEntity.ok(
                serverService.getServersByUsername(authentication.getName()).stream()
                        .map(ServerResponse::from)
                        .toList()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteServer(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Not authenticated");
        }
        serverService.deleteServer(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
