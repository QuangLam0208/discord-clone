package hcmute.edu.vn.discord.entity.jpa;

import hcmute.edu.vn.discord.entity.enums.ChannelType;
import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "channels")
@Data
public class Channel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Boolean isPrivate;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(optional = true)
    @JoinColumn(name = "server_id", nullable = true)
    private Server server;

    @Enumerated(EnumType.STRING)
    private ChannelType type; // TEXT, VOICE

    // Danh sách Role được phép xem kênh này
    @ManyToMany
    @JoinTable(
            name = "channel_allowed_roles",
            joinColumns = @JoinColumn(name = "channel_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<ServerRole> allowedRoles = new HashSet<>();

    // Danh sách Member cụ thể được phép xem kênh này
    @ManyToMany
    @JoinTable(
            name = "channel_allowed_members",
            joinColumns = @JoinColumn(name = "channel_id"),
            inverseJoinColumns = @JoinColumn(name = "member_id")
    )
    private Set<ServerMember> allowedMembers = new HashSet<>();

    public Set<ServerMember> getAllowedMembers() {
        return new HashSet<>(allowedMembers);
    }

    public void setAllowedMembers(Set<ServerMember> allowedMembers) {
        if (allowedMembers == null) {
            this.allowedMembers = new HashSet<>();
        } else {
            this.allowedMembers = new HashSet<>(allowedMembers);
        }
    }
}