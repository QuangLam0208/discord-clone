package hcmute.edu.vn.discord.entity.jpa;

import hcmute.edu.vn.discord.entity.enums.ReportStatus;
import hcmute.edu.vn.discord.entity.enums.ReportType;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Table(name = "reports")
@Data
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "reporter_id")
    private User reporter;

    @ManyToOne
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    @ManyToOne
    @JoinColumn(name = "server_id")
    private Server server;

    @Enumerated(EnumType.STRING)
    private ReportType type; // SPAM, HARASSMENT, SCAM

    @Enumerated(EnumType.STRING)
    private ReportStatus status; // PENDING, ACCEPTED, REJECTED

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
}

