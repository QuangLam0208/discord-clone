package hcmute.edu.vn.discord.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Gửi OTP qua email
     * @return true nếu gửi thành công, false nếu thất bại
     */
    public boolean sendOtpEmail(String toEmail, String otpCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Mã xác thực OTP - Discord Clone");
            message.setText(buildOtpEmailContent(otpCode));

            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", maskEmail(toEmail));
            return true;

        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", toEmail, e);
            return false;
        }
    }

    private String buildOtpEmailContent(String otpCode) {
        return String.format(
                "Xin chào,\n\n" +
                "Mã OTP của bạn là: %s\n\n" +
                "Mã này sẽ hết hạn sau 5 phút.\n\n" +
                "Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email.\n\n" +
                "Trân trọng,\n" +
                "Discord Clone Team",
                otpCode
        );
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        return parts[0].charAt(0) + "***@" + parts[1];
    }
}
