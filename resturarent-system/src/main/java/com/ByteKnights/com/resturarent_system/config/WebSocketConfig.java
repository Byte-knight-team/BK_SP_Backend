package com.ByteKnights.com.resturarent_system.config;

import com.ByteKnights.com.resturarent_system.security.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration using STOMP protocol.
 *
 * How it works:
 * - Frontend connects to /ws endpoint (with SockJS fallback for older browsers)
 * - Inbound channel validates JWT on CONNECT and checks topic authorization on SUBSCRIBE
 * - Frontend subscribes to topics like /topic/branch/1/alerts
 * - Backend broadcasts to those topics using SimpMessagingTemplate
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

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
     * - Heartbeat [server→client, client→server] set to 10s so idle connections
     *   stay alive and SockJS does not silently drop them.
     *   Requires a TaskScheduler — provided by webSocketHeartbeatScheduler().
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic")
                .setHeartbeatValue(new long[]{10000, 10000})
                .setTaskScheduler(webSocketHeartbeatScheduler());
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Register the security interceptor on the inbound channel to authenticate
     * incoming STOMP CONNECT frames and authorize SUBSCRIBE destinations before
     * frames reach the message broker.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }

    /**
     * Dedicated scheduler for WebSocket heartbeats.
     * Kept separate from the application's main task scheduler to avoid
     * interference with other scheduled tasks (e.g., OrderScheduler).
     */
    @Bean
    public ThreadPoolTaskScheduler webSocketHeartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }
}
