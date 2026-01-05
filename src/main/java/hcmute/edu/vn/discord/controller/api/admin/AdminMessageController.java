package hcmute.edu.vn.discord.controller.api.admin;

import hcmute.edu.vn.discord.dto.request.AdminMessageSearchRequest;
import hcmute.edu.vn.discord.dto.response.AdminMessagePageResponse;
import hcmute.edu.vn.discord.repository.MessageRepository;
import hcmute.edu.vn.discord.service.AdminMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/messages")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminMessageController {

    private final AdminMessageService adminMessageService;
    private final MessageRepository messageRepository;

    @GetMapping
    public ResponseEntity<AdminMessagePageResponse> search(
            @RequestParam(required = false) Long serverId,
            @RequestParam(required = false) Long channelId,
            @RequestParam(required = false) Long senderId,
            @RequestParam(required = false) String senderUsername,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AdminMessageSearchRequest req = new AdminMessageSearchRequest();
        req.setServerId(serverId);
        req.setChannelId(channelId);
        req.setSenderId(senderId);
        req.setSenderUsername(senderUsername);
        req.setKeyword(keyword);
        req.setStatus(status);
        req.setFrom(parseInstantFlexible(from));
        req.setTo(parseInstantFlexible(to));
        req.setPage(page);
        req.setSize(size);
        return ResponseEntity.ok(adminMessageService.search(req));
    }

    @PatchMapping("/{id}/delete")
    public ResponseEntity<Void> softDelete(@PathVariable String id, Authentication authentication) {
        adminMessageService.softDelete(id, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable String id, Authentication authentication) {
        adminMessageService.restore(id, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/_stats")
    public ResponseEntity<Map<String, Object>> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalMessages", messageRepository.count());
        return ResponseEntity.ok(m);
    }

    private Instant parseInstantFlexible(String val) {
        if (val == null || val.isBlank()) return null;
        try {
            if (val.matches("^\\d{10,}$")) {
                long epoch = Long.parseLong(val);
                return epoch < 1_000_000_000_000L
                        ? Instant.ofEpochSecond(epoch)
                        : Instant.ofEpochMilli(epoch);
            }
        } catch (Exception ignored) {}
        try { return Instant.parse(val); } catch (Exception ignored) {}
        try { return OffsetDateTime.parse(val).toInstant(); } catch (Exception ignored) {}
        try {
            LocalDateTime ldt = LocalDateTime.parse(val, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return ldt.atZone(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException ignored) {}
        try {
            LocalDate ld = LocalDate.parse(val, DateTimeFormatter.ISO_LOCAL_DATE);
            return ld.atStartOfDay(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException ignored) {}
        return null;
    }
}