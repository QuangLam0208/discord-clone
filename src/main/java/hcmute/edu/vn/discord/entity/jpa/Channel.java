package hcmute.edu.vn.discord.entity.jpa;

import hcmute.edu.vn.discord.entity.enums.ChannelType;
import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@Table(name = "channels")
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private ChannelType type; // TEXT, VOICE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "is_private")
    private Boolean isPrivate = false;


    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "channel_allowed_members",
            joinColumns = @JoinColumn(name = "channel_id"),
            inverseJoinColumns = @JoinColumn(name = "server_member_id")
    )
    private Set<ServerMember> allowedMembers = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "channel_allowed_roles",
            joinColumns = @JoinColumn(name = "channel_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<ServerRole> allowedRoles = new HashSet<>();
}