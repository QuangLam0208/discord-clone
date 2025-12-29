package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.request.ServerRequest;
import hcmute.edu.vn.discord.dto.response.ServerResponse;
import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.service.ServerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
public class ServerController {

    private static final Logger log = LoggerFactory.getLogger(ServerController.class);

    private final ServerService serverService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServerResponse> createServer(@Valid @RequestBody ServerRequest request,
                                                       Principal principal) {

        request.normalize();
        String username = principal.getName();
        log.info("Creating server for user={}", username);

        Server server = new Server();
        server.setName(request.getName());
        server.setDescription(request.getDescription());
        server.setIconUrl(request.getIconUrl());

        Server saved = serverService.createServer(server, username);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();

        return ResponseEntity.created(location).body(ServerResponse.from(saved));
    }

    @GetMapping
    public ResponseEntity<List<ServerResponse>> getAllServers() {
        List<ServerResponse> servers = serverService.getAllServers().stream()
                .map(ServerResponse::from)
                .toList();
        return ResponseEntity.ok(servers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServerResponse> getServerById(@PathVariable Long id) {
        return ResponseEntity.ok(ServerResponse.from(serverService.getServerById(id)));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ServerResponse>> getMyServers(Principal principal) {
        String username = principal.getName();
        List<ServerResponse> servers = serverService.getServersByUsername(username).stream()
                .map(ServerResponse::from)
                .toList();
        return ResponseEntity.ok(servers);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteServer(@PathVariable Long id, Principal principal) {
        String username = principal.getName();
        log.info("Deleting server id={} requestedBy={}", id, username);
        serverService.deleteServer(id, username);
        return ResponseEntity.noContent().build();
    }
}
