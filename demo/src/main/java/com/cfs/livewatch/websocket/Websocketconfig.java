package com.cfs.livewatch.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class Websocketconfig implements WebSocketConfigurer {

    private final com.cfs.livewatch.websocket.WatchPartyHandler watchPartyHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(watchPartyHandler, "/ws")
                .setAllowedOrigins("http://localhost:5173");
    }
}