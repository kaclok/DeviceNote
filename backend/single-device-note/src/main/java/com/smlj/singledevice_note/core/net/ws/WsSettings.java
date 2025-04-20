package com.smlj.singledevice_note.core.net.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

// Spring Boot 如何集成使用 WebSocket https://mp.weixin.qq.com/s/t7Np7wYYVQTZZlEzcdiyWA
// https://blog.csdn.net/w1014074794/article/details/113845879
@Configuration
@EnableWebSocket // 启用 WebSocket 支持
public class WsSettings implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(new WsHandler(), "/ws")
                .addInterceptors(new WsInterceptor())
                .setAllowedOrigins("*"); // 允许所有来源，生产环境需指定域名
    }
}