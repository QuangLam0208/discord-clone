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
                "message", "Mã OTP đã được gửi đến email của bạn"
        ));
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
}