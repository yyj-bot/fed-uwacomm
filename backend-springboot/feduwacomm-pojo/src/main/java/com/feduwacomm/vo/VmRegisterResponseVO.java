package com.feduwacomm.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String secretId; // vm_secrets表的ID
    private String rawApiKey; // 明文API Key，仅此一次返回
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
        
        @JsonProperty("native")
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