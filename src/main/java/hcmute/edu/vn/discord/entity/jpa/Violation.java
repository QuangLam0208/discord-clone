package hcmute.edu.vn.discord.entity.jpa;

import hcmute.edu.vn.discord.entity.enums.ViolationAction;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Table(name = "violations")
@Data
public class Violation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // Người bị phạt

    @ManyToOne
    @JoinColumn(name = "admin_id")
    private User admin; // Người ra quyết định phạt

    @Enumerated(EnumType.STRING)
    private ViolationAction action; // WARN, MUTE, BAN, GLOBAL_BAN

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
}

