package hcmute.edu.vn.discord.entity.jpa;

import hcmute.edu.vn.discord.entity.enums.ServerStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "servers")
@Data
public class Server {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String iconUrl;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    @Enumerated(EnumType.STRING)
    private ServerStatus status; // ACTIVE, FREEZE...

    @OneToMany(mappedBy = "server", cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<Channel> channels;

    @OneToMany(mappedBy = "server")
    private List<ServerMember> members;
}

