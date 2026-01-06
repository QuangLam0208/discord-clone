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
     * Gửi OTP qua email với context
     */
    public boolean sendOtpEmail(String toEmail, String otpCode, String context) {
        try {
            jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(
                    message, true, "UTF-8");

            helper.setTo(toEmail);
            String subject = switch (context) {
                case "REGISTER" -> "Xác thực đăng ký tài khoản - Discord Clone";
                case "CHANGE_EMAIL" -> "Xác thực yêu cầu đổi Email - Discord Clone";
                case "CHANGE_EMAIL_NEW" -> "Xác thực Email mới - Discord Clone";
                default -> "Mã xác thực OTP - Discord Clone";
            };
            helper.setSubject(subject);

            String htmlContent = buildOtpEmailContent(otpCode, context);
            helper.setText(htmlContent, true); // true = isHtml

            mailSender.send(message);
            log.info("OTP email sent successfully to: {} (Context: {})", maskEmail(toEmail), context);
            return true;

        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", toEmail, e);
            return false;
        }
    }

    // Overload cho legacy calls (mặc định context OTP thường)
    public boolean sendOtpEmail(String toEmail, String otpCode) {
        return sendOtpEmail(toEmail, otpCode, "DEFAULT");
    }

    private String buildOtpEmailContent(String otpCode, String context) {
        String title = switch (context) {
            case "REGISTER" -> "Đăng ký tải khoản";
            case "CHANGE_EMAIL", "CHANGE_EMAIL_NEW" -> "Đổi địa chỉ Email";
            default -> "Xác thực OTP";
        };

        return String.format(
                """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <style>
                                body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f6f6f6; margin: 0; padding: 0; }
                                .container { max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
                                .header { background-color: #5865F2; padding: 30px; text-align: center; }
                                .header h1 { color: white; margin: 0; font-size: 24px; }
                                .content { padding: 40px; color: #23272A; }
                                .otp-box { background-color: #F2F3F5; border-radius: 4px; padding: 15px; text-align: center; font-size: 32px; letter-spacing: 5px; font-weight: bold; color: #5865F2; margin: 20px 0; border: 1px dashed #99AAB5; }
                                .footer { background-color: #F2F3F5; padding: 20px; text-align: center; font-size: 12px; color: #72767D; }
                                .note { font-size: 14px; color: #72767D; margin-top: 20px; }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="header">
                                    <h1>%s</h1>
                                </div>
                                <div class="content">
                                    <p>Xin chào,</p>
                                    <p>Bạn đã yêu cầu mã xác thực cho tài khoản Discord Clone của mình. Sử dụng mã bên dưới để hoàn tất:</p>

                                    <div class="otp-box">%s</div>

                                    <p class="note">Mã này sẽ hết hạn sau 5 phút. Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email và bảo vệ tài khoản của bạn.</p>

                                    <p>Trân trọng,<br>Đội ngũ Discord Clone</p>
                                </div>
                                <div class="footer">
                                    &copy; 2024 Discord Clone. All rights reserved.
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                title, otpCode);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        return parts[0].charAt(0) + "***@" + parts[1];
    }
}
