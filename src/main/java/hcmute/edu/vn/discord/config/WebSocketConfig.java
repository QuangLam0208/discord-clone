package hcmute.edu.vn.discord.config;

import hcmute.edu.vn.discord.security.jwt.JwtChannelInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.websocket.allowed-origins:}")
    private String[] allowedOriginPatterns;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint cho client connect: ws://<host>/ws
        var endpointRegistration = registry.addEndpoint("/ws");

        // Cấu hình origin cho phép thông qua property app.websocket.allowed-origins
        // Ví dụ: app.websocket.allowed-origins=http://localhost:3000,https://example.com
        if (allowedOriginPatterns != null
                && allowedOriginPatterns.length > 0
                && !(allowedOriginPatterns.length == 1 && allowedOriginPatterns[0].isEmpty())) {
            endpointRegistration.setAllowedOriginPatterns(allowedOriginPatterns);
        }

        endpointRegistration.withSockJS(); // fallback nếu WS bị chặn
    }

    @Autowired
    private JwtChannelInterceptor jwtChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Broker đơn giản: /topic, /queue; bật user destinations với prefix /user
        registry.enableSimpleBroker("/topic", "/queue", "/user");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user"); // user-specific destinations: /user/queue/...
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Interceptor đọc JWT từ header 'Authorization' trong STOMP CONNECT/SUBSCRIBE
        registration.interceptors(jwtChannelInterceptor);
    }
}