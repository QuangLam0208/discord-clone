package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.dto.request.ReportRequest;
import hcmute.edu.vn.discord.dto.response.ReportResponse;
import hcmute.edu.vn.discord.entity.enums.ReportStatus;
import hcmute.edu.vn.discord.entity.jpa.Report;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.repository.ReportRepository;
import hcmute.edu.vn.discord.repository.UserRepository;
import hcmute.edu.vn.discord.service.ReportService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public void createReport(Long reporterId, ReportRequest request) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new EntityNotFoundException("Reporter not found"));
        User target = userRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new EntityNotFoundException("Target user not found"));

        Report report = new Report();
        report.setReporter(reporter);
        report.setTargetUser(target);
        report.setType(request.getType());
        report.setStatus(ReportStatus.PENDING);
        report.setCreatedAt(new Date());
        reportRepository.save(report);
    }

    @Override
    public java.util.List<Report> getReportsByStatus(ReportStatus status) {
        if (status == null) {
            return reportRepository.findAllByOrderByCreatedAtDesc();
        }
        return reportRepository.findAllByStatusOrderByCreatedAtDesc(status);
    }

    @Override
    public java.util.List<Report> getAllReports() {
        return reportRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    @Transactional
    public void processReport(Long reportId, boolean accepted, String penalty) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("Report not found"));

        if (accepted) {
            report.setStatus(ReportStatus.ACCEPTED);

            // Apply penalty based on ViolationAction values
            User targetUser = report.getTargetUser();
            if (penalty != null) {
                switch (penalty) {
                    case "WARN":
                        // Send warning (Implementation pending message service)
                        // For now we just log it or add to violation record if needed
                        break;
                    case "MUTE":
                        targetUser.setIsMuted(true);
                        // Default mute duration could be set here, or handled by frontend param?
                        // Assuming indefinite mute for now based on requirement "mute là không cho user
                        // thực hiện chức năng gửi tin nhắn"
                        break;
                    case "BAN": // 7 days ban
                        targetUser.setBannedUntil(java.time.LocalDateTime.now().plusDays(7));
                        targetUser.setBanReason("Reported: " + report.getType() + " (7 Days Ban)");
                        break;
                    case "GLOBAL_BAN":
                        targetUser.setIsActive(false);
                        targetUser.setBanReason("Reported: " + report.getType() + " (Permanent Ban)");
                        break;
                }
                userRepository.save(targetUser);
            }
        } else {
            report.setStatus(ReportStatus.REJECTED);
        }

        reportRepository.save(report);

        // Notify admin via WebSocket
        ReportResponse response = ReportResponse.from(report);
        messagingTemplate.convertAndSend("/topic/admin/reports", response);
    }
}