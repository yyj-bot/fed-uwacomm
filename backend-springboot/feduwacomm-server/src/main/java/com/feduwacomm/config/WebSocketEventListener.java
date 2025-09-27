package com.feduwacomm.config;

import com.feduwacomm.dto.WebSocketMessage;
import com.feduwacomm.service.VmInstanceService;
import com.feduwacomm.utils.UuidUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket事件监听器
 * 负责处理WebSocket连接和断开事件，实时更新VM连接状态
 *
 * @author FedUWAComm Team
 * @version 1.1.0
 */
@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final SimpMessageSendingOperations messagingTemplate;
    private final VmInstanceService vmInstanceService;
    private final UuidUtil uuidUtil;

    public WebSocketEventListener(SimpMessageSendingOperations messagingTemplate,
                                 VmInstanceService vmInstanceService,
                                 UuidUtil uuidUtil) {
        this.messagingTemplate = messagingTemplate;
        this.vmInstanceService = vmInstanceService;
        this.uuidUtil = uuidUtil;
    }

    /**
     * 监听WebSocket连接事件
     * 处理VM连接状态更新
     *
     * @param event 连接事件
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        // 获取会话属性，添加null检查
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes == null) {
            // 会话属性还没有设置时这是正常情况，使用DEBUG级别日志
            String sessionId = headerAccessor.getSessionId();
            if (sessionId == null) {
                sessionId = uuidUtil.generateUuid();
            }
            logger.debug("WebSocket连接事件中session attributes为null，可能是连接初期 - SessionId: {}", sessionId);
            return;
        }

        String vmId = (String) sessionAttributes.get("vmId");
        String category = (String) sessionAttributes.get("category");

        // 直接使用UuidUtil生成32位的会话ID
        String sessionId = uuidUtil.generateUuid();

        logger.info("WebSocket连接建立 - SessionId: {}, Category: {}, VmId: {}", sessionId, category, vmId);

        // 如果是VM连接，更新连接状态
        if ("vm".equals(category) && StringUtils.hasText(vmId)) {
            try {
                vmInstanceService.updateConnectionStatus(vmId, "CONNECTED", sessionId);
                logger.info("VM连接状态已更新为CONNECTED - VmId: {}, SessionId: {}", vmId, sessionId);

                // 发送VM连接成功消息
                Map<String, Object> data = new HashMap<>();
                data.put("message", "虚拟机 " + vmId + " 已连接");
                data.put("vmId", vmId);
                data.put("status", "CONNECTED");

                WebSocketMessage connectMessage = WebSocketMessage.builder()
                    .type("VM_CONNECT")
                    .id(uuidUtil.generateUuid()) // 临时使用UUID，后续应使用MessageIdGenerator
                    .vmId(vmId)
                    .data(data)
                    .signature("temp-signature") // 临时签名，后续应使用真实签名
                    .build();
                connectMessage.setTimestampFromInstant(Instant.now());
                messagingTemplate.convertAndSend("/topic/vm-status", connectMessage);

            } catch (Exception e) {
                logger.error("更新VM连接状态失败 - VmId: {}, SessionId: {}", vmId, sessionId, e);
            }
        } else {
            // 普通用户连接
            logger.info("用户WebSocket连接建立 - SessionId: {}", sessionId);
        }
    }

    /**
     * 监听WebSocket断开连接事件
     * 处理VM断开状态更新
     *
     * @param event 断开连接事件
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        // 获取会话属性，添加null检查
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes == null) {
            // 断开时session attributes为null也是可能的，使用DEBUG级别
            String sessionId = headerAccessor.getSessionId();
            if (sessionId == null) {
                sessionId = uuidUtil.generateUuid();
            }
            logger.debug("WebSocket断开事件中session attributes为null - SessionId: {}", sessionId);
            return;
        }

        String vmId = (String) sessionAttributes.get("vmId");
        String category = (String) sessionAttributes.get("category");
        String username = (String) sessionAttributes.get("username");

        logger.info("WebSocket连接断开 - Category: {}, VmId: {}, Username: {}",
                   category, vmId, username);

        // 如果是VM断开连接，更新连接状态
        if ("vm".equals(category) && StringUtils.hasText(vmId)) {
            try {
                vmInstanceService.disconnectVm(vmId);
                logger.info("VM连接状态已更新为DISCONNECTED - VmId: {}", vmId);

                // 发送VM断开消息
                Map<String, Object> data = new HashMap<>();
                data.put("message", "虚拟机 " + vmId + " 已断开连接");
                data.put("vmId", vmId);
                data.put("status", "DISCONNECTED");

                WebSocketMessage disconnectMessage = WebSocketMessage.builder()
                    .type("VM_DISCONNECT")
                    .id(uuidUtil.generateUuid()) // 临时使用UUID，后续应使用MessageIdGenerator
                    .vmId(vmId)
                    .data(data)
                    .signature("temp-signature") // 临时签名，后续应使用真实签名
                    .build();
                disconnectMessage.setTimestampFromInstant(Instant.now());
                messagingTemplate.convertAndSend("/topic/vm-status", disconnectMessage);

            } catch (Exception e) {
                logger.error("更新VM断开状态失败 - VmId: {}", vmId, e);
            }
        } else if (StringUtils.hasText(username)) {
            // 普通用户断开连接
            logger.info("用户断开连接 - Username: {}", username);

            // 发送用户离开消息
            Map<String, Object> data = new HashMap<>();
            data.put("message", "用户 " + username + " 已离开");
            data.put("username", username);
            data.put("action", "leave");

            WebSocketMessage leaveMessage = WebSocketMessage.builder()
                .type("USER_LEAVE")
                .id(uuidUtil.generateUuid()) // 临时使用UUID，后续应使用MessageIdGenerator
                .vmId("system") // 系统消息使用固定vmId
                .data(data)
                .signature("temp-signature") // 临时签名，后续应使用真实签名
                .build();
            leaveMessage.setTimestampFromInstant(Instant.now());
            messagingTemplate.convertAndSend("/topic/public", leaveMessage);
        }
    }
}