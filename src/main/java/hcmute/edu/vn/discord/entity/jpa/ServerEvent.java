package hcmute.edu.vn.discord.entity.jpa;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Table(name = "events")
@Data
public class ServerEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "server_id")
    private Server server;

    @ManyToOne
    @JoinColumn(name = "creator_id")
    private User creator;

    private String title;

    @Temporal(TemporalType.TIMESTAMP)
    private Date startTime;
}

