package hcmute.edu.vn.discord.repository;

import hcmute.edu.vn.discord.entity.jpa.Report;
import hcmute.edu.vn.discord.entity.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findAllByStatus(ReportStatus status);

    List<Report> findAllByStatusOrderByCreatedAtDesc(ReportStatus status);

    List<Report> findAllByOrderByCreatedAtDesc();
}