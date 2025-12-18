package hcmute.edu.vn.discord.entity.jpa;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "user_security")
@Data
public class UserSecurity {
    @Id
    private Long userId; // Dùng chung ID với User (Shared PK)

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private Boolean twoFaEnabled;
    private String twoFaSecret;
}

