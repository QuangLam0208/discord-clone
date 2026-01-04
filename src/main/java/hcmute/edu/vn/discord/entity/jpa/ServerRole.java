package hcmute.edu.vn.discord.entity.jpa;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "server_roles", uniqueConstraints = @UniqueConstraint(columnNames = { "server_id", "name" }))
@Getter
@Setter
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
        private String color;
        private Integer priority;

        @ManyToMany
        @JoinTable(name = "server_role_permissions", joinColumns = @JoinColumn(name = "role_id"), inverseJoinColumns = @JoinColumn(name = "permission_id"))
        private Set<Permission> permissions = new HashSet<>();

        @ManyToMany(mappedBy = "roles")
        @lombok.EqualsAndHashCode.Exclude
        @lombok.ToString.Exclude
        private Set<ServerMember> members = new HashSet<>();
}
