package hcmute.edu.vn.discord.entity.jpa;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "permissions")
@Data
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private String code; // Ví dụ: SERVER_MANAGE, MESSAGE_DELETE

    private String description;

    // Mối quan hệ N-N với ServerRole đã được khai báo ở ServerRole.java
}

