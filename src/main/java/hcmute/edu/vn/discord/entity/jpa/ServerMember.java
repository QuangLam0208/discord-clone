package hcmute.edu.vn.discord.entity.jpa;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "server_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"server_id", "user_id"})
)
@Data
public class ServerMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String nickname;

    @CreationTimestamp
    private LocalDateTime joinedAt;

    @Column(nullable = false)
    private Boolean isBanned = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "member_roles",
            joinColumns = @JoinColumn(name = "member_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<ServerRole> roles = new HashSet<>();
}