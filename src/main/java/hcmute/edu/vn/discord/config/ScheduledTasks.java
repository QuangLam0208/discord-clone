package hcmute.edu.vn.discord.config;

import hcmute.edu.vn.discord.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ScheduledTasks {

    private final OtpService otpService;

    /**
     * Chạy mỗi 1 giờ để dọn OTP hết hạn
     */
    @Scheduled(cron = "0 0 * * * *") // Chạy vào đầu mỗi giờ
    public void cleanupExpiredOtps() {
        log.info("Starting cleanup of expired OTPs");
        otpService.cleanupExpiredOtps();
    }
}

