package hcmute.edu.vn.discord.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import hcmute.edu.vn.discord.dto.request.LoginRequest;
import hcmute.edu.vn.discord.dto.request.RegisterRequest;
import hcmute.edu.vn.discord.dto.response.AuthResponse;
import hcmute.edu.vn.discord.dto.response.UserResponse;
import hcmute.edu.vn.discord.security.jwt.JwtUtils;
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

        @Operation(summary = "Login User", description = "Authenticate a user with username and password, returning a JWT token provided with user details.")
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Successfully authenticated",
                        content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid credentials"),
                @ApiResponse(responseCode = "403", description = "Forbidden - Account is banned"),
                @ApiResponse(responseCode = "400", description = "Bad Request - Validation error")
        })
        @PostMapping("/login")
        public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest,
                                                  HttpServletResponse httpResponse) {
                try {
                        Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

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

                        String jwt = jwtUtils.generateJwtToken(authentication);

                        // Set JWT vào cookie
                        Cookie cookie = new Cookie("jwt", jwt);
                        cookie.setHttpOnly(true);
                        cookie.setSecure(false); // bật true trên HTTPS production
                        cookie.setPath("/");
                        cookie.setMaxAge(60 * 60 * 24);
                        httpResponse.addCookie(cookie);

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
                SecurityContextHolder.clearContext();
                return ResponseEntity.noContent().build();
        }

        @Operation(summary = "Register User", description = "Register a new user account and automatically login.")
        @ApiResponses({
                @ApiResponse(responseCode = "201", description = "User registered successfully",
                        content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                @ApiResponse(responseCode = "400", description = "Bad Request - Username/Email already exists")
        })
        @PostMapping("/register")
        public ResponseEntity<AuthResponse> registerUser(@Valid @RequestBody RegisterRequest registerRequest,
                                                         HttpServletResponse httpResponse) {
                // Tạo user
                userService.registerUser(registerRequest);

                // Đăng nhập ngay sau khi đăng ký
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(registerRequest.getUsername(), registerRequest.getPassword()));
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

                return ResponseEntity.created(URI.create("/api/users/username/" + username))
                        .body(new AuthResponse(jwt, "Bearer", UserResponse.from(userEntity)));
        }
}