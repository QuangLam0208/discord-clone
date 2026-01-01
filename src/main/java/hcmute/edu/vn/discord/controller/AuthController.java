package hcmute.edu.vn.discord.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import hcmute.edu.vn.discord.dto.request.LoginRequest;
import hcmute.edu.vn.discord.dto.request.RegisterRequest;
import hcmute.edu.vn.discord.dto.response.AuthResponse;
import hcmute.edu.vn.discord.dto.response.UserResponse;
import hcmute.edu.vn.discord.security.jwt.JwtUtils;
import hcmute.edu.vn.discord.security.services.UserDetailsImpl;
import hcmute.edu.vn.discord.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.List;

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
                @ApiResponse(responseCode = "400", description = "Bad Request - Validation error")
        })
        @PostMapping("/login")
        public ResponseEntity<AuthResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest,
                                                             HttpServletResponse httpResponse) {
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                String jwt = jwtUtils.generateJwtToken(authentication);

                // Set JWT vào cookie để trình duyệt giữ phiên đăng nhập
                Cookie cookie = new Cookie("jwt", jwt);
                cookie.setHttpOnly(true);
                cookie.setSecure(false); // bật true trên HTTPS production
                cookie.setPath("/");
                cookie.setMaxAge(60 * 60 * 24); // 1 ngày (hoặc đồng bộ với jwtExpirationMs/1000)
                httpResponse.addCookie(cookie);

                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                var userEntity = userService.findByUsername(userDetails.getUsername())
                        .orElseThrow(() -> new RuntimeException("User not found"));

                return ResponseEntity.ok(new AuthResponse(jwt, "Bearer", UserResponse.from(userEntity)));
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

                // Set cookie JWT
                Cookie cookie = new Cookie("jwt", jwt);
                cookie.setHttpOnly(true);
                cookie.setSecure(false);
                cookie.setPath("/");
                cookie.setMaxAge(60 * 60 * 24);
                httpResponse.addCookie(cookie);

                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                var userEntity = userService.findByUsername(userDetails.getUsername())
                        .orElseThrow(() -> new RuntimeException("User not found"));

                return ResponseEntity.created(URI.create("/api/users/username/" + userDetails.getUsername()))
                        .body(new AuthResponse(jwt, "Bearer", UserResponse.from(userEntity)));
        }
}