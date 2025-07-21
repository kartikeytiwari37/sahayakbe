package com.sahayak.config;

import com.sahayak.websocket.SahayakWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SahayakWebSocketHandler sahayakWebSocketHandler;

    public WebSocketConfig(SahayakWebSocketHandler sahayakWebSocketHandler) {
        this.sahayakWebSocketHandler = sahayakWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(sahayakWebSocketHandler, "/sahayak-teacher")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setStreamBytesLimit(1024 * 1024) // 1MB
                .setHttpMessageCacheSize(1000)
                .setDisconnectDelay(30 * 1000); // 30 seconds
    }
    
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(1024 * 1024); // 1MB
        container.setMaxBinaryMessageBufferSize(1024 * 1024); // 1MB
        container.setMaxSessionIdleTimeout(30 * 1000L); // 30 seconds
        return container;
    }
}
