package hcmute.edu.vn.discord.entity.jpa;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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
    @JoinColumn(name = "server_id")
    private Server server;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String nickname;
    private LocalDateTime joinedAt;
    private Boolean isBanned;

    // Một thành viên có thể có nhiều role trong server (Owner, Admin, Member...)
    @ManyToMany
    @JoinTable(
            name = "member_roles",
            joinColumns = @JoinColumn(name = "member_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private List<ServerRole> roles;
}

