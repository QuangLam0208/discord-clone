package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.dto.request.ReportRequest;
import hcmute.edu.vn.discord.entity.enums.ReportStatus;
import hcmute.edu.vn.discord.entity.jpa.Report;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.repository.ReportRepository;
import hcmute.edu.vn.discord.repository.UserRepository;
import hcmute.edu.vn.discord.service.ReportService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

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
}