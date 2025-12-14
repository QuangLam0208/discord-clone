package hcmute.edu.vn.discord.entity.jpa;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    private String displayName;
    private String avatarUrl;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private LocalDate birthDate;
    private String country;
    private Boolean isActive;
    private Boolean isEmailVerified;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime lastActive;

    // Quan hệ 1-1 với UserSecurity
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private UserSecurity userSecurity;

    // Quan hệ với Server (Server mình tham gia)
    @OneToMany(mappedBy = "user")
    private List<ServerMember> memberships;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles;
}
