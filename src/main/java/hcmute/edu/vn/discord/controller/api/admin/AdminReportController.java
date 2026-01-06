package hcmute.edu.vn.discord.controller.api.admin;

import hcmute.edu.vn.discord.entity.enums.ReportStatus;
import hcmute.edu.vn.discord.entity.jpa.Report;
import hcmute.edu.vn.discord.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import hcmute.edu.vn.discord.dto.response.ReportResponse;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    @GetMapping
    public ResponseEntity<?> getReports(
            @RequestParam(required = false) ReportStatus status) {
        try {
            List<ReportResponse> responses = reportService.getReportsByStatus(status).stream()
                    .map(ReportResponse::from)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Error fetching reports: " + e.getMessage()));
        }
    }

    @PostMapping("/{reportId}/process")
    public ResponseEntity<?> processReport(
            @PathVariable Long reportId,
            @RequestParam boolean accepted,
            @RequestParam(required = false) String penalty) {
        try {
            reportService.processReport(reportId, accepted, penalty);
            return ResponseEntity.ok(Map.of("message", "Processed report successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", "Error processing report: " + e.getMessage()));
        }
    }
}
