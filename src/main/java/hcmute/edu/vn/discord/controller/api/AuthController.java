package hcmute.edu.vn.discord.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import hcmute.edu.vn.discord.dto.request.LoginRequest;
import hcmute.edu.vn.discord.dto.request.RegisterRequest;
import hcmute.edu.vn.discord.dto.request.RegisterWithOtpRequest;
import hcmute.edu.vn.discord.dto.response.AuthResponse;
import hcmute.edu.vn.discord.dto.response.OtpResponse;
import hcmute.edu.vn.discord.dto.response.OtpVerificationResponse;
import hcmute.edu.vn.discord.dto.response.UserResponse;
import hcmute.edu.vn.discord.security.jwt.JwtUtils;
import hcmute.edu.vn.discord.service.AuditLogMongoService;
import hcmute.edu.vn.discord.service.OtpService;
import hcmute.edu.vn.discord.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.Map;

@Tag(name = "Authentication", description = "Endpoints for user authentication (Login, Register)")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

        private final AuthenticationManager authenticationManager;
        private final UserService userService;
        private final JwtUtils jwtUtils;
        private final AuditLogMongoService auditLogMongoService;
        private final OtpService otpService;

        @Operation(summary = "Login User", description = "Authenticate a user with username and password, returning a JWT token provided with user details.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Successfully authenticated", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid credentials"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - Account is banned"),
                        @ApiResponse(responseCode = "400", description = "Bad Request - Validation error")
        })
        @PostMapping("/login")
        public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest,
                        HttpServletResponse httpResponse) {
                try {
                        Authentication authentication = authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(loginRequest.getUsername(),
                                                        loginRequest.getPassword()));

                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        // Lấy username an toàn từ principal (UserDetailsImpl hoặc bất kỳ UserDetails)
                        Object principal = authentication.getPrincipal();
                        String username = (principal instanceof UserDetails)
                                        ? ((UserDetails) principal).getUsername()
                                        : authentication.getName();

                        var userEntity = userService.findByUsername(username)
                                        .orElseThrow(() -> new RuntimeException("User not found"));

                        // Chặn đăng nhập nếu tài khoản bị ban (isActive=false)
                        if (Boolean.FALSE.equals(userEntity.getIsActive())) {
                                SecurityContextHolder.clearContext();
                                Cookie expired = new Cookie("jwt", "");
                                expired.setHttpOnly(true);
                                expired.setSecure(false);
                                expired.setPath("/");
                                expired.setMaxAge(0);
                                httpResponse.addCookie(expired);

                                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                                .body(Map.of("message", "Tài khoản đã bị khóa/banned"));
                        }

                        // Chặn đăng nhập nếu bị ban tạm thời
                        if (userEntity.getBannedUntil() != null
                                        && userEntity.getBannedUntil().isAfter(java.time.LocalDateTime.now())) {
                                SecurityContextHolder.clearContext();
                                Cookie expired = new Cookie("jwt", "");
                                expired.setHttpOnly(true);
                                expired.setSecure(false);
                                expired.setPath("/");
                                expired.setMaxAge(0);
                                httpResponse.addCookie(expired);

                                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                                .body(Map.of("message", "Tài khoản bị tạm khóa đến: "
                                                                + userEntity.getBannedUntil()));
                        }

                        String jwt = jwtUtils.generateJwtToken(authentication);

                        // Set JWT vào cookie
                        Cookie cookie = new Cookie("jwt", jwt);
                        cookie.setHttpOnly(true);
                        cookie.setSecure(false); // bật true trên HTTPS production
                        cookie.setPath("/");
                        cookie.setMaxAge(60 * 60 * 24);
                        httpResponse.addCookie(cookie);

                        if (userEntity != null) {
                                try {
                                        auditLogMongoService.log(
                                                        userEntity.getId(),
                                                        "USER_LOGIN",
                                                        "UserId: " + userEntity.getId(),
                                                        "User logged in successfully");
                                } catch (Exception e) {
                                }
                        }

                        return ResponseEntity.ok(new AuthResponse(jwt, "Bearer", UserResponse.from(userEntity)));
                } catch (DisabledException ex) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body(Map.of("message", "Tài khoản đã bị khóa/banned"));
                } catch (BadCredentialsException | InternalAuthenticationServiceException ex) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body(Map.of("message", "Sai tài khoản hoặc mật khẩu"));
                }
        }

        @Operation(summary = "Logout", description = "Clear JWT cookie and end session.")
        @PostMapping("/logout")
        public ResponseEntity<Void> logout(HttpServletResponse httpResponse) {
                Cookie expired = new Cookie("jwt", "");
                expired.setHttpOnly(true);
                expired.setSecure(false); // bật true trên HTTPS production
                expired.setPath("/");
                expired.setMaxAge(0);
                httpResponse.addCookie(expired);
                try {
                        String username = SecurityContextHolder.getContext().getAuthentication().getName();
                        userService.findByUsername(username).ifPresent(u -> {
                                auditLogMongoService.log(u.getId(), "USER_LOGOUT", "UserId: " + u.getId(),
                                                "User logged out");
                        });
                } catch (Exception e) {
                }

                SecurityContextHolder.clearContext();
                return ResponseEntity.noContent().build();
        }

        @Operation(summary = "Register User", description = "Register a new user account and automatically login.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "User registered successfully", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Bad Request - Username/Email already exists")
        })
        @PostMapping("/register")
        public ResponseEntity<AuthResponse> registerUser(@Valid @RequestBody RegisterRequest registerRequest,
                        HttpServletResponse httpResponse) {
                // Tạo user
                userService.registerUser(registerRequest);

                // Đăng nhập ngay sau khi đăng ký
                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(registerRequest.getUsername(),
                                                registerRequest.getPassword()));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                String jwt = jwtUtils.generateJwtToken(authentication);

                Cookie cookie = new Cookie("jwt", jwt);
                cookie.setHttpOnly(true);
                cookie.setSecure(false);
                cookie.setPath("/");
                cookie.setMaxAge(60 * 60 * 24);
                httpResponse.addCookie(cookie);

                Object principal = authentication.getPrincipal();
                String username = (principal instanceof UserDetails)
                                ? ((UserDetails) principal).getUsername()
                                : authentication.getName();

                var userEntity = userService.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                try {
                        auditLogMongoService.log(
                                        userEntity.getId(),
                                        "USER_REGISTER",
                                        "UserId: " + userEntity.getId(),
                                        "New user registered: " + userEntity.getUsername());
                } catch (Exception e) {
                }

                return ResponseEntity.created(URI.create("/api/users/username/" + username))
                                .body(new AuthResponse(jwt, "Bearer", UserResponse.from(userEntity)));
        }

        /**
         * Gửi OTP khi đăng ký
         */
        @PostMapping("/register/send-otp")
        public ResponseEntity<OtpResponse> sendRegistrationOtp(@RequestParam String email) {
                // Kiểm tra email đã tồn tại chưa
                if (userService.existsByEmail(email)) {
                        return ResponseEntity.badRequest()
                                        .body(OtpResponse.error("Email đã được sử dụng"));
                }

                OtpResponse response = otpService.generateAndSendOtp(email, "REGISTER");

                if (!response.isSuccess()) {
                        return ResponseEntity.badRequest().body(response);
                }

                return ResponseEntity.ok(response);
        }

        /**
         * Xác thực OTP và hoàn tất đăng ký
         */
        @PostMapping("/register/verify-otp")
        public ResponseEntity<?> registerWithOtp(
                        @Valid @RequestBody RegisterWithOtpRequest request,
                        HttpServletResponse httpResponse) {

                // Xác thực OTP
                OtpVerificationResponse otpResult = otpService.verifyOtp(
                                request.getEmail(),
                                request.getOtpCode());

                if (!otpResult.isVerified()) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(otpResult);
                }

                // Tạo user mới: map RegisterWithOtpRequest -> RegisterRequest
                RegisterRequest registerRequest = new RegisterRequest();
                registerRequest.setUsername(request.getUsername());
                registerRequest.setEmail(request.getEmail());
                registerRequest.setPassword(request.getPassword());
                registerRequest.setDisplayName(request.getDisplayName());

                userService.registerUser(registerRequest);

                // Tạo JWT và trả về
                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                request.getUsername(),
                                                request.getPassword()));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                String jwt = jwtUtils.generateJwtToken(authentication);

                // Set cookie
                Cookie cookie = new Cookie("jwt", jwt);
                cookie.setHttpOnly(true);
                cookie.setSecure(false);
                cookie.setPath("/");
                cookie.setMaxAge(60 * 60 * 24);
                httpResponse.addCookie(cookie);

                var userEntity = userService.findByUsername(request.getUsername())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                return ResponseEntity.ok(new AuthResponse(jwt, "Bearer", UserResponse.from(userEntity)));
        }

        /**
         * Gửi OTP reset mật khẩu
         */
        @PostMapping("/forgot-password")
        public ResponseEntity<?> forgotPassword(@RequestParam String email) {
                // Không tiết lộ tồn tại email — trả OK chung
                if (!userService.existsByEmail(email)) {
                        return ResponseEntity.ok(Map.of(
                                        "message", "Nếu email tồn tại, mã OTP đã được gửi"));
                }

                otpService.generateAndSendOtp(email);

                return ResponseEntity.ok(Map.of(
                                "message", "Mã OTP đã được gửi đến email của bạn"));
        }

        /**
         * Reset mật khẩu với OTP
         */
        @PostMapping("/reset-password")
        public ResponseEntity<?> resetPassword(
                        @RequestParam String email,
                        @RequestParam String otpCode,
                        @RequestParam String newPassword) {

                // Xác thực OTP
                var otpVerify = otpService.verifyOtp(email, otpCode);
                if (!otpVerify.isVerified()) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body(Map.of("message", "Mã OTP không hợp lệ hoặc đã hết hạn"));
                }

                // Reset password
                var user = userService.findByEmail(email).orElseThrow();
                userService.changePassword(user.getId(), newPassword);

                return ResponseEntity.ok(Map.of("message", "Đặt lại mật khẩu thành công"));
        }
}