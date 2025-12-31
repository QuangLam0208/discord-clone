package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.entity.jpa.Invite;
import hcmute.edu.vn.discord.service.InviteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invites")
@RequiredArgsConstructor
public class InviteController {

    private final InviteService inviteService;

    // 1. Tạo lời mời (Cần quyền CREATE_INVITE trên server)
    @PostMapping("/server/{serverId}")
    @PreAuthorize("@serverAuth.canCreateInvite(#serverId, authentication.name)")
    public ResponseEntity<Invite> createInvite(
            @PathVariable Long serverId,
            @RequestParam(required = false) Integer maxUses,
            @RequestParam(required = false) Long expireSeconds) {
        return ResponseEntity.ok(inviteService.createInvite(serverId, maxUses, expireSeconds));
    }

    // 2. Tham gia server bằng mã code
    @PostMapping("/join/{code}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> joinServer(@PathVariable String code) {
        inviteService.joinServer(code);
        return ResponseEntity.ok().build();
    }
}