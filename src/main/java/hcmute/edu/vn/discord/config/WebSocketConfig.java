package hcmute.edu.vn.discord.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint cho client connect: ws://localhost:8081/ws
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Cho phép mọi origin (cần siết chặt khi production)
                .withSockJS(); // Nếu không có hỗ trợ WebSocket, tự động chuyển sang Http Polling
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefix cho các message từ server gửi về client
        registry.enableSimpleBroker("/topic", "/queue");

        // Prefix cho các message từ client gửi lên server
        // Ví dụ: @MessageMapping("/chat.sendMessage") -> client gửi
        // "/app/chat.sendMessage"
        registry.setApplicationDestinationPrefixes("/app");
    }
}
