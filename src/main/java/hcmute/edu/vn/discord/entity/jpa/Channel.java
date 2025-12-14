package hcmute.edu.vn.discord.entity.jpa;

import hcmute.edu.vn.discord.entity.enums.ChannelType;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "channels")
@Data
public class Channel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    // Nếu channel không thuộc category nào thì sao? Nên thêm quan hệ trực tiếp với Server
    @ManyToOne(optional = true)
    @JoinColumn(name = "server_id", nullable = true)
    private Server server;

    private String name;

    @Enumerated(EnumType.STRING)
    private ChannelType type; // TEXT, VOICE

    private Boolean isPrivate;
}

