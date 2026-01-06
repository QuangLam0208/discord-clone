package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.dto.response.OtpResponse;
import hcmute.edu.vn.discord.dto.response.OtpVerificationResponse;
import hcmute.edu.vn.discord.entity.jpa.EmailVerification;
import hcmute.edu.vn.discord.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final EmailVerificationRepository verificationRepository;
    private final EmailService emailService;

    @Value("${otp.expiration.minutes:5}")
    private int otpExpirationMinutes;

    @Value("${otp.length:6}")
    private int otpLength;

    @Value("${otp.resend.cooldown.seconds:60}")
    private int resendCooldownSeconds;

    private static final int MAX_ATTEMPTS = 5;

    /**
     * Tạo và gửi OTP đến email (Default context)
     */
    @Transactional
    public OtpResponse generateAndSendOtp(String email) {
        return generateAndSendOtp(email, "DEFAULT");
    }

    /**
     * Tạo và gửi OTP đến email với Context
     */
    @Transactional
    public OtpResponse generateAndSendOtp(String email, String context) {
        try {
            // Kiểm tra rate limiting (chống spam)
            var rateLimitCheck = checkRateLimit(email);
            if (!rateLimitCheck.isSuccess()) {
                return rateLimitCheck;
            }

            // Tạo mã OTP ngẫu nhiên
            String otpCode = generateOtpCode();

            // Lưu vào database
            EmailVerification verification = new EmailVerification();
            verification.setEmail(email);
            verification.setOtpCode(otpCode);
            verification.setCreatedAt(LocalDateTime.now());
            verification.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes));
            verification.setVerified(false);
            verification.setAttemptCount(0);
            // Có thể lưu context vào DB nếu mở rộng bảng EmailVerification, hiện tại chỉ
            // dùng để log/email

            verificationRepository.save(verification);

            // Gửi email với context
            boolean emailSent = emailService.sendOtpEmail(email, otpCode, context);

            if (!emailSent) {
                log.error("Failed to send OTP email to:  {}", email);
                return OtpResponse.error("Không thể gửi email.  Vui lòng thử lại sau.");
            }

            log.info("OTP sent successfully to email: {} (Context: {})", maskEmail(email), context);
            return OtpResponse.success(email, verification.getExpiresAt());

        } catch (Exception e) {
            log.error("Error generating OTP for email: {}", email, e);
            return OtpResponse.error("Đã xảy ra lỗi khi tạo mã OTP.  Vui lòng thử lại.");
        }
    }

    /**
     * Xác thực OTP
     *
     * @param email   Email cần xác thực
     * @param otpCode Mã OTP
     * @return OtpVerificationResponse chứa kết quả xác thực
     */
    @Transactional
    public OtpVerificationResponse verifyOtp(String email, String otpCode) {
        try {
            var verificationOpt = verificationRepository
                    .findByEmailAndOtpCodeAndVerifiedFalseAndExpiresAtAfter(
                            email, otpCode, LocalDateTime.now());

            // Không tìm thấy OTP hoặc đã hết hạn
            if (verificationOpt.isEmpty()) {
                // Kiểm tra xem có phải đã hết hạn không
                var expiredOpt = verificationRepository
                        .findTopByEmailAndOtpCodeOrderByCreatedAtDesc(email, otpCode);

                if (expiredOpt.isPresent()) {
                    log.warn("Expired OTP attempt for email: {}", maskEmail(email));
                    return OtpVerificationResponse.expired();
                }

                log.warn("OTP not found for email: {}", maskEmail(email));
                return OtpVerificationResponse.notFound();
            }

            EmailVerification verification = verificationOpt.get();

            // Kiểm tra số lần thử
            if (verification.getAttemptCount() >= MAX_ATTEMPTS) {
                log.warn("Max attempts reached for email: {}", maskEmail(email));
                return OtpVerificationResponse.maxAttemptsExceeded();
            }

            // Tăng số lần thử
            verification.setAttemptCount(verification.getAttemptCount() + 1);

            // Xác thực thành công
            verification.setVerified(true);
            // setVerifiedAt may not exist; set via reflection to avoid compilation issues
            // if field absent
            try {
                verification.getClass().getMethod("setVerifiedAt", LocalDateTime.class).invoke(verification,
                        LocalDateTime.now());
            } catch (NoSuchMethodException ignored) {
                // method not present; skip
            }

            verificationRepository.save(verification);

            log.info("OTP verified successfully for email: {}", maskEmail(email));
            return OtpVerificationResponse.success();

        } catch (Exception e) {
            log.error("Error verifying OTP for email:  {}", email, e);
            return OtpVerificationResponse.builder()
                    .verified(false)
                    .message("Đã xảy ra lỗi khi xác thực.  Vui lòng thử lại.")
                    .build();
        }
    }

    /**
     * Kiểm tra rate limiting (chống spam gửi OTP)
     */
    private OtpResponse checkRateLimit(String email) {
        var lastVerification = verificationRepository
                .findTopByEmailOrderByCreatedAtDesc(email);

        if (lastVerification.isPresent()) {
            var timeSinceLastSent = Duration.between(
                    lastVerification.get().getCreatedAt(),
                    LocalDateTime.now());

            if (timeSinceLastSent.getSeconds() < resendCooldownSeconds) {
                long remainingSeconds = resendCooldownSeconds - timeSinceLastSent.getSeconds();
                String message = String.format(
                        "Vui lòng đợi %d giây trước khi gửi lại OTP",
                        remainingSeconds);
                log.warn("Rate limit hit for email: {}, remaining:  {}s", maskEmail(email), remainingSeconds);
                return OtpResponse.error(message);
            }
        }

        return OtpResponse.builder().success(true).build();
    }

    /**
     * Tạo mã OTP ngẫu nhiên
     */
    private String generateOtpCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    /**
     * Dọn dẹp OTP hết hạn (chạy định kỳ)
     */
    @Transactional
    public void cleanupExpiredOtps() {
        try {
            verificationRepository.deleteByExpiresAtBefore(LocalDateTime.now());
            log.info("Cleaned up expired OTPs");
        } catch (Exception e) {
            log.error("Error cleaning up expired OTPs", e);
        }
    }

    /**
     * Kiểm tra OTP có tồn tại và hợp lệ không (không đánh dấu verified)
     */
    @Transactional(readOnly = true)
    public boolean isOtpValid(String email, String otpCode) {
        return verificationRepository
                .findByEmailAndOtpCodeAndVerifiedFalseAndExpiresAtAfter(
                        email, otpCode, LocalDateTime.now())
                .isPresent();
    }

    /**
     * Che email cho log (bảo mật)
     */
    private String maskEmail(String email) {
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
