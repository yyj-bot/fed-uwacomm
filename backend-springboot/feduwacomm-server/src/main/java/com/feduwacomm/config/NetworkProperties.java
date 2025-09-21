package com.feduwacomm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static com.feduwacomm.constants.SystemConstants.*;

/**
 * 网络配置属性类
 *
 * 管理系统中所有网络相关的配置项
 *
 * @author FedUWAComm Team
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "feduwacomm.network")
public class NetworkProperties {

    /**
     * 服务器配置
     */
    private Server server = new Server();

    /**
     * WebSocket配置
     */
    private WebSocket websocket = new WebSocket();

    /**
     * API配置
     */
    private Api api = new Api();

    @Data
    public static class Server {
        /**
         * 服务器主机名
         * 默认: localhost
         */
        private String host = DEFAULT_HOST;

        /**
         * 服务器端口
         * 默认: 8080
         */
        private String port = DEFAULT_SERVER_PORT;

        /**
         * HTTP协议
         * 默认: http://
         */
        private String protocol = HTTP_PROTOCOL;

        /**
         * HTTPS协议
         * 默认: https://
         */
        private String secureProtocol = HTTPS_PROTOCOL;

        /**
         * 是否启用HTTPS
         * 默认: false
         */
        private boolean enableHttps = false;

        /**
         * 获取完整的服务器URL
         * @return 服务器URL (例如: http://localhost:8080)
         */
        public String getBaseUrl() {
            String selectedProtocol = enableHttps ? secureProtocol : protocol;
            return selectedProtocol + host + ":" + port;
        }
    }

    @Data
    public static class WebSocket {
        /**
         * WebSocket协议
         * 默认: ws://
         */
        private String protocol = WS_PROTOCOL;

        /**
         * 安全WebSocket协议
         * 默认: wss://
         */
        private String secureProtocol = WSS_PROTOCOL;

        /**
         * 是否启用安全WebSocket
         * 默认: false
         */
        private boolean enableWss = false;

        /**
         * WebSocket端点路径
         * 默认: /ws
         */
        private String endpoint = WEBSOCKET_ENDPOINT;

        /**
         * 原生WebSocket端点路径
         * 默认: /ws-native
         */
        private String nativeEndpoint = WEBSOCKET_NATIVE_ENDPOINT;

        /**
         * 心跳间隔（秒）
         * 默认: 30
         */
        private int heartbeatInterval = 30;

        /**
         * 连接超时时间（秒）
         * 默认: 60
         */
        private int connectionTimeout = 60;

        /**
         * 获取WebSocket协议
         * @return WebSocket协议
         */
        public String getProtocol() {
            return enableWss ? secureProtocol : protocol;
        }

        /**
         * 获取完整的WebSocket URL
         * @param server 服务器配置
         * @param nativeWs 是否使用原生WebSocket
         * @return WebSocket URL
         */
        public String getWebSocketUrl(Server server, boolean nativeWs) {
            String wsProtocol = getProtocol();
            String path = nativeWs ? nativeEndpoint : endpoint;
            return wsProtocol + server.getHost() + ":" + server.getPort() + path;
        }
    }

    @Data
    public static class Api {
        /**
         * API基础路径
         * 默认: /api
         */
        private String basePath = API_BASE_PATH;

        /**
         * API v1路径
         * 默认: /api/v1
         */
        private String v1Path = API_V1_PATH;

        /**
         * 虚拟机API路径前缀
         * 默认: /api/v1/vm
         */
        private String vmPrefix = VM_API_PREFIX;

        /**
         * Token刷新路径
         * 默认: /api/v1/vm/token/refresh
         */
        private String tokenRefreshPath = TOKEN_REFRESH_PATH;

        /**
         * API请求超时时间（秒）
         * 默认: 30
         */
        private int requestTimeout = 30;

        /**
         * 最大重试次数
         * 默认: 3
         */
        private int maxRetries = 3;

        /**
         * 构建虚拟机状态查询路径
         * @param vmId 虚拟机ID
         * @return 状态查询路径
         */
        public String buildVmStatusPath(String vmId) {
            return vmPrefix + "/" + vmId + "/status";
        }

        /**
         * 构建虚拟机控制路径
         * @param vmId 虚拟机ID
         * @return 控制路径
         */
        public String buildVmControlPath(String vmId) {
            return vmPrefix + "/" + vmId + "/control";
        }
    }

    /**
     * 获取完整的API基础URL
     * @return API基础URL
     */
    public String getApiBaseUrl() {
        return server.getBaseUrl() + api.getBasePath();
    }

    /**
     * 获取WebSocket连接信息
     * @return WebSocket连接信息
     */
    public WebSocketInfo getWebSocketInfo() {
        WebSocketInfo info = new WebSocketInfo();
        info.setSockjs(server.getBaseUrl() + websocket.getEndpoint());
        info.setNativeWs(websocket.getWebSocketUrl(server, true));
        return info;
    }

    @Data
    public static class WebSocketInfo {
        private String sockjs;
        private String nativeWs;
    }
}