package com.feduwacomm.config;

import com.feduwacomm.dto.WebSocketMessage;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;

/**
 * WebSocket事件监听器
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Component
public class WebSocketEventListener {

    private final SimpMessageSendingOperations messagingTemplate;

    public WebSocketEventListener(SimpMessageSendingOperations messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 监听WebSocket连接事件
     * 
     * @param event 连接事件
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        System.out.println("收到新的WebSocket连接");

        // 可以在这里发送欢迎消息
        WebSocketMessage welcomeMessage = new WebSocketMessage();
        welcomeMessage.setType("SYSTEM");
        welcomeMessage.setContent("欢迎连接到FedUWAComm WebSocket服务器！");
        welcomeMessage.setSender("System");
        welcomeMessage.setTimestamp(LocalDateTime.now());

        // 广播欢迎消息
        messagingTemplate.convertAndSend("/topic/public", welcomeMessage);
    }

    /**
     * 监听WebSocket断开连接事件
     * 
     * @param event 断开连接事件
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        // 获取用户名
        String username = (String) headerAccessor.getSessionAttributes().get("username");

        if (username != null) {
            System.out.println("用户断开连接: " + username);

            // 创建离开消息
            WebSocketMessage leaveMessage = new WebSocketMessage();
            leaveMessage.setType("LEAVE");
            leaveMessage.setContent(username + " 离开了聊天室");
            leaveMessage.setSender(username);
            leaveMessage.setTimestamp(LocalDateTime.now());

            // 广播离开消息
            messagingTemplate.convertAndSend("/topic/public", leaveMessage);
        }
    }
}