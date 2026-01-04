package hcmute.edu.vn.discord.controller.api.admin;

import hcmute.edu.vn.discord.dto.request.AdminDirectMessageSearchRequest;
import hcmute.edu.vn.discord.dto.response.AdminMessagePageResponse;
import hcmute.edu.vn.discord.repository.DirectMessageRepository;
import hcmute.edu.vn.discord.service.AdminDirectMessageService;
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
@RequestMapping("/api/admin/direct-messages")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminDirectMessageController {

    private final AdminDirectMessageService service;
    private final DirectMessageRepository dmRepo;

    @GetMapping
    public ResponseEntity<AdminMessagePageResponse> search(
            @RequestParam(required = false) String conversationId,
            @RequestParam(required = false) Long userAId,
            @RequestParam(required = false) Long userBId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long senderId,
            @RequestParam(required = false) String senderUsername,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AdminDirectMessageSearchRequest req = new AdminDirectMessageSearchRequest();
        req.setConversationId(conversationId);
        req.setUserAId(userAId);
        req.setUserBId(userBId);
        req.setUserId(userId);
        req.setSenderId(senderId);
        req.setSenderUsername(senderUsername);
        req.setKeyword(keyword);
        req.setStatus(status);
        req.setFrom(parseInstantFlexible(from));
        req.setTo(parseInstantFlexible(to));
        req.setPage(page);
        req.setSize(size);
        return ResponseEntity.ok(service.search(req));
    }

    @PatchMapping("/{id}/delete")
    public ResponseEntity<Void> softDelete(@PathVariable String id, Authentication auth) {
        service.softDelete(id, auth.getName());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable String id, Authentication auth) {
        service.restore(id, auth.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/_stats")
    public ResponseEntity<Map<String, Object>> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalDirectMessages", dmRepo.count());
        return ResponseEntity.ok(m);
    }

    private Instant parseInstantFlexible(String val) {
        if (val == null || val.isBlank()) return null;
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