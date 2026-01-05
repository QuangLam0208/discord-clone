package hcmute.edu.vn.discord.entity.jpa;

import hcmute.edu.vn.discord.entity.enums.EAuditAction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id") // Can be null if system action or user deleted
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EAuditAction actionType;

    @Column(name = "target_id")
    private String targetId; // ID of the target (Role ID, Channel ID, Member ID)

    @Column(name = "target_type")
    private String targetType; // "ROLE", "CHANNEL", "MEMBER", "SERVER"

    @Column(columnDefinition = "TEXT")
    private String changes; // JSON or text description of changes

    @CreationTimestamp
    private LocalDateTime createdAt;
}