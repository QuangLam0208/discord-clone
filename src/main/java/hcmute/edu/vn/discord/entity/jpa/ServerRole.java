package hcmute.edu.vn.discord.entity.jpa;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(
        name = "server_roles",
        uniqueConstraints = @UniqueConstraint(columnNames = {"server_id", "name"})
)
@Data
@AllArgsConstructor
@NoArgsConstructor
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
    private Set<Permission> permissions = new HashSet<>();
}

