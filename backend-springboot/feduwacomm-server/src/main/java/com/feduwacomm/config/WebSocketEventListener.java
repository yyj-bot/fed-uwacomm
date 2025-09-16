package com.feduwacomm.config;

import com.feduwacomm.dto.WebSocketMessage;
import com.feduwacomm.service.VmInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;
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

    public WebSocketEventListener(SimpMessageSendingOperations messagingTemplate,
                                 VmInstanceService vmInstanceService) {
        this.messagingTemplate = messagingTemplate;
        this.vmInstanceService = vmInstanceService;
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
            logger.warn("WebSocket连接事件中session attributes为null - SessionId: {}", headerAccessor.getSessionId());
            return;
        }

        String vmId = (String) sessionAttributes.get("vmId");
        String category = (String) sessionAttributes.get("category");
        String sessionId = headerAccessor.getSessionId();

        logger.info("WebSocket连接建立 - SessionId: {}, Category: {}, VmId: {}", sessionId, category, vmId);

        // 如果是VM连接，更新连接状态
        if ("vm".equals(category) && StringUtils.hasText(vmId)) {
            try {
                vmInstanceService.updateConnectionStatus(vmId, "CONNECTED", sessionId);
                logger.info("VM连接状态已更新为CONNECTED - VmId: {}, SessionId: {}", vmId, sessionId);

                // 发送VM连接成功消息
                WebSocketMessage connectMessage = new WebSocketMessage();
                connectMessage.setType("VM_CONNECT");
                connectMessage.setContent("虚拟机 " + vmId + " 已连接");
                connectMessage.setSender("System");
                connectMessage.setTimestamp(LocalDateTime.now());
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
            logger.warn("WebSocket断开事件中session attributes为null - SessionId: {}", headerAccessor.getSessionId());
            return;
        }

        String vmId = (String) sessionAttributes.get("vmId");
        String category = (String) sessionAttributes.get("category");
        String username = (String) sessionAttributes.get("username");
        String sessionId = headerAccessor.getSessionId();

        logger.info("WebSocket连接断开 - SessionId: {}, Category: {}, VmId: {}, Username: {}",
                   sessionId, category, vmId, username);

        // 如果是VM断开连接，更新连接状态
        if ("vm".equals(category) && StringUtils.hasText(vmId)) {
            try {
                vmInstanceService.disconnectVm(vmId);
                logger.info("VM连接状态已更新为DISCONNECTED - VmId: {}, SessionId: {}", vmId, sessionId);

                // 发送VM断开消息
                WebSocketMessage disconnectMessage = new WebSocketMessage();
                disconnectMessage.setType("VM_DISCONNECT");
                disconnectMessage.setContent("虚拟机 " + vmId + " 已断开连接");
                disconnectMessage.setSender("System");
                disconnectMessage.setTimestamp(LocalDateTime.now());
                messagingTemplate.convertAndSend("/topic/vm-status", disconnectMessage);

            } catch (Exception e) {
                logger.error("更新VM断开状态失败 - VmId: {}, SessionId: {}", vmId, sessionId, e);
            }
        } else if (StringUtils.hasText(username)) {
            // 普通用户断开连接
            logger.info("用户断开连接 - Username: {}, SessionId: {}", username, sessionId);

            // 发送用户离开消息
            WebSocketMessage leaveMessage = new WebSocketMessage();
            leaveMessage.setType("USER_LEAVE");
            leaveMessage.setContent("用户 " + username + " 已离开");
            leaveMessage.setSender(username);
            leaveMessage.setTimestamp(LocalDateTime.now());
            messagingTemplate.convertAndSend("/topic/public", leaveMessage);
        }
    }
}