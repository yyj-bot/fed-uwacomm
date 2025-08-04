package com.feduwacomm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket配置类
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 配置消息代理
     * 
     * @param registry 消息代理注册表
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 启用简单的内存消息代理，用于将消息从一个客户端广播到其他客户端
        // 客户端订阅以"/topic"开头的目标
        registry.enableSimpleBroker("/topic", "/queue");

        // 设置客户端发送消息的前缀
        // 客户端发送消息到"/app"开头的目标
        registry.setApplicationDestinationPrefixes("/app");

        // 设置用户目标前缀，用于点对点消息
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * 注册STOMP端点
     * 
     * @param registry STOMP端点注册表
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册STOMP端点，客户端通过这个端点进行WebSocket连接
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // 允许跨域访问
                .withSockJS(); // 启用SockJS支持

        // 也可以不使用SockJS，直接使用原生WebSocket
        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns("*");
    }
}