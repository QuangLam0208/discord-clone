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
    public ResponseEntity<hcmute.edu.vn.discord.dto.response.InviteResponse> createInvite(
            @PathVariable Long serverId,
            @RequestParam(required = false) Integer maxUses,
            @RequestParam(required = false) Long expireSeconds) {
        Invite invite = inviteService.createInvite(serverId, maxUses, expireSeconds);
        return ResponseEntity.ok(hcmute.edu.vn.discord.dto.response.InviteResponse.builder()
                .code(invite.getCode())
                .serverId(invite.getServer().getId())
                .serverName(invite.getServer().getName())
                .serverIconUrl(invite.getServer().getIconUrl())
                .maxUses(invite.getMaxUses())
                .usedCount(invite.getUsedCount())
                .expiresAt(invite.getExpiresAt())
                .build());
    }

    // 2. Tham gia server bằng mã code
    @PostMapping("/join/{code}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.Map<String, Object>> joinServer(@PathVariable String code) {
        Long serverId = inviteService.joinServer(code);
        return ResponseEntity.ok(java.util.Map.of("message", "Joined successfully", "serverId", serverId));
    }
}