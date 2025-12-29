package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.request.LoginRequest;
import hcmute.edu.vn.discord.dto.request.RegisterRequest;
import hcmute.edu.vn.discord.dto.response.JwtResponse;
import hcmute.edu.vn.discord.security.jwt.JwtUtils;
import hcmute.edu.vn.discord.security.services.UserDetailsImpl;
import hcmute.edu.vn.discord.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor

public class AuthController {

        private final AuthenticationManager authenticationManager;
        private final UserService userService;
        private final JwtUtils jwtUtils;

        @org.springframework.beans.factory.annotation.Autowired
        private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

        @PostMapping("/login")
        public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

                try {
                        Authentication authentication = authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(
                                                        loginRequest.getUsername(), loginRequest.getPassword().trim()));
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        String jwt = jwtUtils.generateJwtToken(authentication);

                        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                        List<String> roles = userDetails.getAuthorities().stream()
                                        .map(GrantedAuthority::getAuthority)
                                        .toList();

                        return ResponseEntity.ok(new JwtResponse(
                                        jwt,
                                        "Bearer",
                                        userDetails.getId(),
                                        userDetails.getUsername(),
                                        userDetails.getEmail(),
                                        roles));
                } catch (Exception e) {
                        e.printStackTrace(); // Log the full stack trace
                        return ResponseEntity.status(401).build();
                }
        }

        @PostMapping("/register")
        public ResponseEntity<Map<String, String>> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {

                userService.registerUser(registerRequest);

                return ResponseEntity.created(
                                URI.create("/api/users/username/" + registerRequest.getUsername()))
                                .body(Map.of("message", "User registered successfully"));
        }
}
