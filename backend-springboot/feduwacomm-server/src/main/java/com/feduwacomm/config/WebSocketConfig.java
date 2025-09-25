package com.feduwacomm.config;

import com.feduwacomm.config.properties.WebSocketProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * WebSocket配置类
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    private final WebSocketHandshakeAuthInterceptor handshakeAuthInterceptor;
    private final WebSocketProperties webSocketProperties;

    public WebSocketConfig(StompAuthChannelInterceptor stompAuthChannelInterceptor,
            WebSocketHandshakeAuthInterceptor handshakeAuthInterceptor,
            WebSocketProperties webSocketProperties) {
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
        this.handshakeAuthInterceptor = handshakeAuthInterceptor;
        this.webSocketProperties = webSocketProperties;
    }

    /**
     * 配置消息代理
     *
     * @param registry 消息代理注册表
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 启用简单的内存消息代理，用于将消息从一个客户端广播到其他客户端
        // 客户端订阅以"/topic"开头的目标
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{webSocketProperties.getHeartbeat().getValue(),
                                            webSocketProperties.getHeartbeat().getValue()}) // 从配置读取心跳间隔
                .setTaskScheduler(heartBeatScheduler()); // 设置心跳任务调度器

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
        registry.addEndpoint(webSocketProperties.getEndpoint())
                .addInterceptors(handshakeAuthInterceptor)
                .setAllowedOriginPatterns("*") // 允许跨域访问
                .withSockJS(); // 启用SockJS支持

        // 也可以不使用SockJS，直接使用原生WebSocket
        registry.addEndpoint(webSocketProperties.getNativeEndpoint())
                .addInterceptors(handshakeAuthInterceptor)
                .setAllowedOriginPatterns("*");
    }

    /**
     * 配置入站通道拦截器（认证）
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }

    /**
     * 配置WebSocket传输
     * 支持大消息和联邦学习模型参数传输
     * 所有配置从配置文件读取，支持不同环境的差异化配置
     */
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        WebSocketProperties.Transport transport = webSocketProperties.getTransport();

        // 从配置文件读取消息大小限制 - 支持超大型神经网络参数传输
        registration.setMessageSizeLimit(transport.getMessageSizeLimit());
        registration.setSendBufferSizeLimit(transport.getSendBufferSizeLimit());
        registration.setSendTimeLimit(transport.getSendTimeLimit());

        // 设置传输超时 - 放宽超时限制以支持联邦学习长时间训练
        registration.setTimeToFirstMessage(transport.getTimeToFirstMessage());
    }

    /**
     * 配置TaskScheduler用于WebSocket心跳
     * 从配置文件读取线程池大小，支持不同环境的优化配置
     */
    @Bean
    public TaskScheduler heartBeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(webSocketProperties.getHeartbeat().getSchedulerPoolSize()); // 从配置读取线程池大小
        scheduler.setThreadNamePrefix("websocket-heartbeat-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        return scheduler;
    }
}