package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.request.ServerRequest;
import hcmute.edu.vn.discord.dto.response.ServerResponse;
import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.service.ChannelService;
import hcmute.edu.vn.discord.service.ServerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

        Server server = new Server();
        server.setName(request.getName());
        server.setDescription(request.getDescription());
        server.setIconUrl(request.getIconUrl());

        Server saved = serverService.createServer(server, authentication.getName());

        return ResponseEntity.created(URI.create("/api/servers/" + saved.getId()))
                .body(ServerResponse.from(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteServer(@PathVariable Long id, Authentication authentication) {
        try {
            serverService.deleteServer(id, authentication.getName());
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
