package hcmute.edu.vn.discord.service;
import hcmute.edu.vn.discord.dto.request.ReportRequest;

public interface ReportService {
    void createReport(Long reporterId, ReportRequest request);
}