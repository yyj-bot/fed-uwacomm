package com.feduwacomm.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * WebSocket配置属性类
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "feduwacomm.network.websocket")
public class WebSocketProperties {

    /**
     * WebSocket协议
     */
    private String protocol = "ws://";

    /**
     * WebSocket安全协议
     */
    private String secureProtocol = "wss://";

    /**
     * 是否启用WSS
     */
    private boolean enableWss = false;

    /**
     * WebSocket端点 (统一使用原生WebSocket协议)
     */
    private String endpoint = "/ws";

    /**
     * 心跳间隔(秒)
     */
    private int heartbeatInterval = 30;

    /**
     * 连接超时(秒)
     */
    private int connectionTimeout = 60;

    /**
     * WebSocket传输配置
     */
    private Transport transport = new Transport();

    /**
     * WebSocket心跳配置
     */
    private Heartbeat heartbeat = new Heartbeat();

    /**
     * WebSocket传输配置类
     */
    @Data
    public static class Transport {

        /**
         * 消息大小限制(字节)
         * 默认1GB
         */
        private int messageSizeLimit = 1024 * 1024 * 1024;

        /**
         * 发送缓冲区大小限制(字节)
         * 默认1GB
         */
        private int sendBufferSizeLimit = 1024 * 1024 * 1024;

        /**
         * 发送超时时间(毫秒)
         * 默认10分钟
         */
        private int sendTimeLimit = 10 * 60 * 1000;

        /**
         * 首条消息超时时间(毫秒)
         * 默认2分钟
         */
        private int timeToFirstMessage = 2 * 60 * 1000;
    }

    /**
     * WebSocket心跳配置类
     */
    @Data
    public static class Heartbeat {

        /**
         * 心跳间隔(毫秒)
         * 默认60秒
         */
        private long value = 60 * 1000;

        /**
         * 心跳调度器线程池大小
         * 默认10个线程
         */
        private int schedulerPoolSize = 10;
    }
}