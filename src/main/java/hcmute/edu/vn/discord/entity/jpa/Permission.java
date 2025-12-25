package hcmute.edu.vn.discord.entity.jpa;

import jakarta.persistence.*;
import lombok.*;
import hcmute.edu.vn.discord.entity.enums.EPermission;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // Tên hiển thị

    // Quan trọng: Lưu mã code từ Enum
    @Column(unique = true, nullable = false)
    private String code;

    // Constructor tiện lợi dùng cho DataSeeder
    public Permission(String name, EPermission permissionEnum) {
        this.name = name;
        this.code = permissionEnum.name(); // Lấy tên Enum lưu vào String (VD: "MANAGE_CHANNELS")
    }
}