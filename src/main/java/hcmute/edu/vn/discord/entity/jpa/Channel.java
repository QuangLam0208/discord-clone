package hcmute.edu.vn.discord.entity.jpa;

import hcmute.edu.vn.discord.entity.enums.ChannelType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@Table(name = "channels")
public class Channel {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @EqualsAndHashCode.Include
        private Long id;

        @ToString.Include
        private String name;

        @Enumerated(EnumType.STRING)
        private ChannelType type;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "server_id", nullable = false)
        @ToString.Exclude
        @EqualsAndHashCode.Exclude
        private Server server;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "category_id")
        @ToString.Exclude
        @EqualsAndHashCode.Exclude
        private Category category;

        @Column(name = "is_private")
        private Boolean isPrivate = false;

        @Column(columnDefinition = "TEXT")
        private String description;

        @ManyToMany(fetch = FetchType.LAZY)
        @JoinTable(
                name = "channel_allowed_members",
                joinColumns = @JoinColumn(name = "channel_id"),
                inverseJoinColumns = @JoinColumn(name = "member_id")
        )
        @ToString.Exclude
        @EqualsAndHashCode.Exclude
        private Set<ServerMember> allowedMembers = new HashSet<>();

        @ManyToMany(fetch = FetchType.LAZY)
        @JoinTable(
                name = "channel_allowed_roles",
                joinColumns = @JoinColumn(name = "channel_id"),
                inverseJoinColumns = @JoinColumn(name = "role_id")
        )
        @ToString.Exclude
        @EqualsAndHashCode.Exclude
        private Set<ServerRole> allowedRoles = new HashSet<>();
}