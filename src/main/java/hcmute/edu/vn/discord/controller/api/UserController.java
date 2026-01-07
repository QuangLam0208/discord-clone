package hcmute.edu.vn.discord.controller.api;

import hcmute.edu.vn.discord.dto.response.UserResponse;
import hcmute.edu.vn.discord.service.UserService;
import hcmute.edu.vn.discord.service.OtpService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final OtpService otpService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(Principal principal) {
        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(401).build();
        }
        var user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PostMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @RequestParam("displayName") String displayName,
            @RequestParam(value = "bio", required = false) String bio,
            @RequestParam(value = "bannerColor", required = false) String bannerColor,
            @RequestParam(value = "file", required = false) MultipartFile file,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        System.out.println("Update Profile Request - User: " + principal.getName() + ", DisplayName: " + displayName
                + ", BannerColor: " + bannerColor);
        var updatedUser = userService.updateProfile(principal.getName(), displayName, bio, bannerColor, file);
        return ResponseEntity.ok(UserResponse.from(updatedUser));
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable @NotBlank String username) {
        var user = userService.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        var user = userService.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Boolean>> checkEmail(@RequestParam @Email String email) {
        boolean exists = userService.existsByEmail(email);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /**
     * Gửi OTP để xác nhận đổi mật khẩu
     */
    @PostMapping("/change-password/send-otp")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> sendChangePasswordOtp(Authentication auth) {
        var user = userService.findByUsername(auth.getName()).orElseThrow();

        otpService.generateAndSendOtp(user.getEmail());

        return ResponseEntity.ok(Map.of(
                "message", "Mã OTP đã được gửi đến email của bạn"));
    }

    /**
     * Đổi mật khẩu sau khi xác thực OTP
     */
    @PostMapping("/change-password/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePasswordWithOtp(
            @RequestParam String otpCode,
            @RequestParam String newPassword,
            Authentication auth) {

        var user = userService.findByUsername(auth.getName()).orElseThrow();

        // Xác thực OTP
        var verifyResult = otpService.verifyOtp(user.getEmail(), otpCode);
        if (!verifyResult.isVerified()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", verifyResult.getMessage()));
        }

        // Đổi mật khẩu
        userService.changePassword(user.getId(), newPassword);

        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
    }

    // Temporary storage (production nên dùng Redis với TTL)
    private final Map<Long, String> tempEmailChangeStorage = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Yêu cầu đổi email - Gửi OTP đến email hiện tại
     */
    @PostMapping("/change-email/request")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> requestChangeEmail(
            @RequestParam @Email String newEmail,
            Authentication auth) {

        var user = userService.findByUsername(auth.getName())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Kiểm tra email mới đã tồn tại chưa
        if (userService.existsByEmail(newEmail)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Email đã được sử dụng"));
        }

        // Kiểm tra email mới khác email cũ
        if (user.getEmail().equals(newEmail)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Email mới phải khác email hiện tại"));
        }

        // Gửi OTP đến email hiện tại
        hcmute.edu.vn.discord.dto.response.OtpResponse otpResponse = otpService.generateAndSendOtp(user.getEmail(),
                "CHANGE_EMAIL");

        if (!otpResponse.isSuccess()) {
            return ResponseEntity.badRequest().body(otpResponse);
        }

        // Lưu newEmail tạm thời vào cache/session (dùng Spring Cache hoặc Redis)
        // Ở đây đơn giản hóa: lưu vào Map tạm (production nên dùng Redis)
        tempEmailChangeStorage.put(user.getId(), newEmail);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Mã OTP đã được gửi đến email hiện tại của bạn",
                "expiresAt", otpResponse.getExpiresAt()));
    }

    /**
     * Xác thực OTP email cũ và gửi OTP đến email mới
     */
    @PostMapping("/change-email/verify-old")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> verifyOldEmailOtp(
            @RequestParam String otpCode,
            Authentication auth) {

        var user = userService.findByUsername(auth.getName())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Xác thực OTP email cũ
        hcmute.edu.vn.discord.dto.response.OtpVerificationResponse verification = otpService.verifyOtp(user.getEmail(),
                otpCode);

        if (!verification.isVerified()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(verification);
        }

        // Lấy email mới từ storage
        String newEmail = tempEmailChangeStorage.get(user.getId());
        if (newEmail == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Phiên đổi email đã hết hạn"));
        }

        // Gửi OTP đến email mới
        hcmute.edu.vn.discord.dto.response.OtpResponse otpResponse = otpService.generateAndSendOtp(newEmail,
                "CHANGE_EMAIL_NEW");

        if (!otpResponse.isSuccess()) {
            return ResponseEntity.badRequest().body(otpResponse);
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Mã OTP đã được gửi đến email mới. Vui lòng kiểm tra.",
                "expiresAt", otpResponse.getExpiresAt()));
    }

    /**
     * Xác nhận OTP email mới và hoàn tất đổi email
     */
    @PostMapping("/change-email/verify-new")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> verifyNewEmailOtp(
            @RequestParam String otpCode,
            Authentication auth) {

        var user = userService.findByUsername(auth.getName())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Lấy email mới từ storage
        String newEmail = tempEmailChangeStorage.get(user.getId());
        if (newEmail == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Phiên đổi email đã hết hạn"));
        }

        // Xác thực OTP email mới
        hcmute.edu.vn.discord.dto.response.OtpVerificationResponse verification = otpService.verifyOtp(newEmail,
                otpCode);

        if (!verification.isVerified()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(verification);
        }

        // Cập nhật email
        userService.updateEmail(user.getId(), newEmail);

        // Xóa khỏi storage
        tempEmailChangeStorage.remove(user.getId());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đổi email thành công",
                "newEmail", newEmail));
    }
}