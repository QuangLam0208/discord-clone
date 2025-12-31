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
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(unauthorizedHandler))
                                .sessionManagement(sess -> sess
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                // TODO: /ws/**, /ws-test.html is temporarily public for testing. MUST
                                                // secure before production.
                                                .requestMatchers(
                                                                "/api/auth/**",
                                                                "/api/chat/history/**",
                                                                "/api/test/**",
                                                                "/ws/**",
                                                                "/ws-test.html",
                                                                "/swagger-ui.html",
                                                                "/swagger-ui/**",
                                                                "/v3/api-docs/**",
                                                                "/swagger-resources/**",
                                                                "/webjars/**",
                                                                "/websocket-test.html",
                                                                "/files/**")  // Cho phép xem file public
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .userDetailsService(userDetailsService)
                                .addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
