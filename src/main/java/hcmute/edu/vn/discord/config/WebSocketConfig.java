package hcmute.edu.vn.discord.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.websocket.allowed-origins:}")
    private String[] allowedOriginPatterns;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint cho client connect: ws://localhost:8081/ws
        var endpointRegistration = registry.addEndpoint("/ws");

        // Cấu hình origin cho phép thông qua property app.websocket.allowed-origins
        // Ví dụ:
        // app.websocket.allowed-origins=http://localhost:3000,https://example.com
        if (allowedOriginPatterns != null
                && allowedOriginPatterns.length > 0
                && !(allowedOriginPatterns.length == 1 && allowedOriginPatterns[0].isEmpty())) {
            endpointRegistration.setAllowedOriginPatterns(allowedOriginPatterns);
        }

        endpointRegistration.withSockJS(); // Nếu không có hỗ trợ WebSocket, tự động chuyển sang Http Polling
    }

    @org.springframework.beans.factory.annotation.Autowired
    private hcmute.edu.vn.discord.security.jwt.JwtChannelInterceptor jwtChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefix cho các message từ server gửi về client
        registry.enableSimpleBroker("/topic", "/queue", "/user"); // Added /user for user-specific destinations

        // Prefix cho các message từ client gửi lên server
        // Ví dụ: @MessageMapping("/chat.sendMessage") -> client gửi
        // "/app/chat.sendMessage"
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user"); // Enable user destinations
    }

    @Override
    public void configureClientInboundChannel(
            org.springframework.messaging.simp.config.ChannelRegistration registration) {
        registration.interceptors(jwtChannelInterceptor);
    }
}
