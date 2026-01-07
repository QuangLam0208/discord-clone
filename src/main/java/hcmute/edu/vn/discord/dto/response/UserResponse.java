package hcmute.edu.vn.discord.dto.response;

import hcmute.edu.vn.discord.entity.jpa.User;
import lombok.AllArgsConstructor;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String email;

    private String displayName;
    private String avatarUrl;
    private String bio;
    private String bannerColor;
    private String workplace;
    private String address;
    private String education;
    private Boolean allowProfileView;

    private LocalDate birthDate;
    private String country;

    private Boolean isActive;
    private Boolean isEmailVerified;

    private LocalDateTime createdAt;
    private LocalDateTime lastActive;

    private Set<String> roles; // ADMIN, USER_DEFAULT, USER_PREMIUM

    public static UserResponse from(User user) {
        if (user == null)
            return null;

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .bannerColor(user.getBannerColor())
                .workplace(user.getWorkplace())
                .address(user.getAddress())
                .education(user.getEducation())
                .allowProfileView(user.getAllowProfileView())
                .birthDate(user.getBirthDate())
                .country(user.getCountry())
                .isActive(user.getIsActive())
                .isEmailVerified(user.getIsEmailVerified())
                .createdAt(user.getCreatedAt())
                .lastActive(user.getLastActive())
                .roles(
                        user.getRoles() == null ? Set.of()
                                : user.getRoles().stream()
                                        .map(r -> r.getName().name())
                                        .collect(Collectors.toSet()))
                .build();
    }
}
