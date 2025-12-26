package hcmute.edu.vn.discord.entity.jpa;

import hcmute.edu.vn.discord.entity.enums.ERole;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "roles")
@Data
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ERole name; // Enum: ADMIN, USER_DEFAULT, USER_PREMIUM
}

