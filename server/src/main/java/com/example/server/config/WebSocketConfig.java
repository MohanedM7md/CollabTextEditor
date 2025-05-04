package com.example.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        System.out.println("[WebSocketConfig] Registering STOMP endpoint at /ws");
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
        System.out.println("[WebSocketConfig] STOMP endpoint registered with CORS: *");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic"); // for broadcasting
        config.setApplicationDestinationPrefixes("/app"); // for client-to-server messages
    }
}
