package hcmute.edu.vn.discord.entity.jpa;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Table(name = "invites")
@Data
public class Invite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "server_id")
    private Server server;

    @Column(unique = true)
    private String code;

    @Temporal(TemporalType.TIMESTAMP)
    private Date expiresAt;

    private Integer maxUses;
    private Integer usedCount;
    private Boolean revoked;
}

