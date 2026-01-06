package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.dto.request.ReportRequest;

import hcmute.edu.vn.discord.entity.enums.ReportStatus;
import hcmute.edu.vn.discord.entity.jpa.Report;
import java.util.List;

public interface ReportService {
    void createReport(Long reporterId, ReportRequest request);

    List<Report> getReportsByStatus(ReportStatus status);

    void processReport(Long reportId, boolean accepted, String penalty);

    java.util.List<Report> getAllReports();
}