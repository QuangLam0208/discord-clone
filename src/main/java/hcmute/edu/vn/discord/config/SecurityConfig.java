package hcmute.edu.vn.discord.config;

import hcmute.edu.vn.discord.security.jwt.AuthEntryPointJwt;
import hcmute.edu.vn.discord.security.jwt.AuthTokenFilter;
import hcmute.edu.vn.discord.security.services.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@RequiredArgsConstructor
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
        private final UserDetailsServiceImpl userDetailsService;
        private final AuthEntryPointJwt unauthorizedHandler;
        private final AuthTokenFilter authTokenFilter;

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationManager authenticationManager(
                        AuthenticationConfiguration authConfig) throws Exception {
                return authConfig.getAuthenticationManager();
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(cors -> {
                                }) // bật CORS ở Security layer để dùng WebMvcConfigurer
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(unauthorizedHandler))
                                .sessionManagement(sess -> sess
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                // Prevent Browser Caching for secured pages
                                .headers(headers -> headers
                                                .cacheControl(cache -> cache.disable()))
                                .authorizeHttpRequests(auth -> auth
                                                // Cho phép preflight CORS
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                                // Cho phép render các trang view (SSR khung) và static assets
                                                .requestMatchers(
                                                                "/", "/login", "/register",
                                                                "/error", "/favicon.ico",
                                                                "/css/**", "/js/**", "/images/**", "/fonts/**",
                                                                "/webjars/**",
                                                                "/ws/**", "/ws-test.html",
                                                                "/swagger-ui.html", "/swagger-ui/**",
                                                                "/v3/api-docs/**", "/swagger-resources/**",
                                                                "/websocket-test.html",
                                                                "/files/**")
                                                .permitAll()

                                                // Bảo vệ REST Admin
                                                .requestMatchers("/api/admin/**", "/admin/**").hasAnyAuthority("ADMIN")

                                                // Cho phép các API auth public
                                                .requestMatchers("/api/auth/**").permitAll()

                                                // Các REST API còn lại yêu cầu xác thực
                                                .requestMatchers("/api/**").authenticated()

                                                // Mặc định: yêu cầu xác thực
                                                .anyRequest().authenticated())
                                .userDetailsService(userDetailsService)
                                .addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}