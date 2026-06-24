package com.ByteKnights.com.resturarent_system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration using STOMP protocol.
 *
 * How it works:
 * - Frontend connects to /ws endpoint (with SockJS fallback for older browsers)
 * - Frontend subscribes to topics like /topic/branch/1/alerts
 * - Backend broadcasts to those topics using SimpMessagingTemplate
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Register the /ws endpoint that the React frontend will connect to.
     * SockJS provides a fallback if WebSocket is not supported.
     * Allow any origin so the React dev server (localhost:5173) can connect.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /**
     * Configure the message broker:
     * - /topic prefix is for server-to-client broadcasts (subscriptions)
     * - /app prefix is for client-to-server messages (if needed in the future)
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
