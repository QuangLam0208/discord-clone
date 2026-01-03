package hcmute.edu.vn.discord.controller.api.admin;

import hcmute.edu.vn.discord.dto.request.TransferOwnerRequest;
import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.repository.ServerRepository;
import hcmute.edu.vn.discord.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/servers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ADMIN','MODERATOR')")
public class AdminServerController {

    private final ServerRepository serverRepository;
    private final UserService userService;

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
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServer(@PathVariable Long id) {
        if (!serverRepository.existsById(id)) {
            throw new EntityNotFoundException("Server not found");
        }
        serverRepository.deleteById(id);
        return ResponseEntity.noContent().build();
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
}