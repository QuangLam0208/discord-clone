package hcmute.edu.vn.discord.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerificationResponse {
    private boolean verified;
    private String message;
    private Integer remainingAttempts;
    private boolean maxAttemptsReached;
    private boolean expired;

    // Factory methods
    public static OtpVerificationResponse success() {
        return OtpVerificationResponse.builder()
                .verified(true)
                .message("Xác thực OTP thành công")
                .build();
    }

    public static OtpVerificationResponse invalidOtp(int remainingAttempts) {
        return OtpVerificationResponse.builder()
                .verified(false)
                .message("Mã OTP không hợp lệ")
                .remainingAttempts(remainingAttempts)
                .maxAttemptsReached(remainingAttempts <= 0)
                .build();
    }

    public static OtpVerificationResponse expired() {
        return OtpVerificationResponse.builder()
                .verified(false)
                .message("Mã OTP đã hết hạn")
                .expired(true)
                .build();
    }

    public static OtpVerificationResponse maxAttemptsExceeded() {
        return OtpVerificationResponse.builder()
                .verified(false)
                .message("Vượt quá số lần thử.  Vui lòng yêu cầu mã mới.")
                .maxAttemptsReached(true)
                .remainingAttempts(0)
                .build();
    }

    public static OtpVerificationResponse notFound() {
        return OtpVerificationResponse.builder()
                .verified(false)
                .message("Không tìm thấy mã OTP hoặc email không đúng")
                .build();
    }
}

