package hcmute.edu.vn.discord.repository;

import hcmute.edu.vn.discord.entity.jpa.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
}