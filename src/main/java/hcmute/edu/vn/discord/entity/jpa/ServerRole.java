package hcmute.edu.vn.discord.entity.jpa;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(
        name = "server_roles",
        uniqueConstraints = @UniqueConstraint(columnNames = {"server_id", "name"})
)
@Data
public class ServerRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "server_id")
    private Server server;

    private String name;
    private Integer priority;

    @ManyToMany
    @JoinTable(
            name = "server_role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private List<Permission> permissions;
}

