package com.feduwacomm.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 虚拟机注册响应VO
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
public class VmRegisterResponseVO {

    private String vmId;
    private String name;
    private String status;
    private String connectionStatus;
    private LocalDateTime createdAt;
    private String sessionId;
    private String accessToken;
    private String secretId;
    private Long tokenExpireSeconds;
    
    /**
     * WebSocket连接信息
     */
    private WebSocketInfo websocket;
    
    /**
     * API端点信息
     */
    private ApiEndpoints apiEndpoints;

    @Data
    @Builder
    public static class WebSocketInfo {
        private String sockjs;
        private String nativeWs;
    }

    @Data
    @Builder
    public static class ApiEndpoints {
        private String status;
        private String control;
        private String tokenRefresh;
    }
}