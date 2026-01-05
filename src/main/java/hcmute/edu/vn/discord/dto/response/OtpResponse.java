package hcmute.edu.vn.discord.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpResponse {
    private boolean success;
    private String message;
    private String email;
    private LocalDateTime expiresAt;
    private Integer remainingAttempts;

    // Factory methods để tạo response nhanh
    public static OtpResponse success(String email, LocalDateTime expiresAt) {
        return OtpResponse.builder()
                .success(true)
                .message("Mã OTP đã được gửi thành công")
                .email(maskEmail(email))
                .expiresAt(expiresAt)
                .build();
    }

    public static OtpResponse error(String message) {
        return OtpResponse.builder()
                .success(false)
                .message(message)
                .build();
    }

    // Che email:  test@example.com -> t***@example.com
    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String username = parts[0];
        String domain = parts[1];

        if (username.length() <= 2) {
            return username.charAt(0) + "***@" + domain;
        }
        return username.charAt(0) + "***" + username.charAt(username.length() - 1) + "@" + domain;
    }
}

