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
        var endpointRegistration = registry.addEndpoint("/ws");

        if (allowedOriginPatterns != null
                && allowedOriginPatterns.length > 0
                && !(allowedOriginPatterns.length == 1 && allowedOriginPatterns[0].isEmpty())) {
            endpointRegistration.setAllowedOriginPatterns(allowedOriginPatterns);
        }

        endpointRegistration.withSockJS();
    }

    @Autowired
    private JwtChannelInterceptor jwtChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue", "/user");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Interceptor đọc JWT từ header 'Authorization' trong STOMP CONNECT/SUBSCRIBE
        registration.interceptors(jwtChannelInterceptor);
    }
}