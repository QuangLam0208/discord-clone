package hcmute.edu.vn.discord.repository;

import hcmute.edu.vn.discord.entity. jpa.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java. util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findByEmailAndOtpCodeAndVerifiedFalseAndExpiresAtAfter(
            String email,
            String otpCode,
            LocalDateTime now
    );

    Optional<EmailVerification> findTopByEmailOrderByCreatedAtDesc(String email);

    Optional<EmailVerification> findTopByEmailAndOtpCodeOrderByCreatedAtDesc(String email, String otpCode);

    void deleteByExpiresAtBefore(LocalDateTime now);
}