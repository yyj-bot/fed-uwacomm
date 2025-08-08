package com.feduwacomm.controller;

import com.feduwacomm.dto.WebSocketMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

/**
 * WebSocket控制器
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Controller
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 处理聊天消息
     * 客户端发送消息到 /app/chat
     * 服务器广播消息到 /topic/public
     * 
     * @param chatMessage 聊天消息
     * @return 处理后的消息
     */
    @MessageMapping("/chat")
    @SendTo("/topic/public")
    public WebSocketMessage handleChatMessage(@Payload WebSocketMessage chatMessage) {
        // 设置时间戳
        chatMessage.setTimestamp(LocalDateTime.now());

        // 记录日志
        System.out.println("收到聊天消息: " + chatMessage);

        return chatMessage;
    }

    /**
     * 处理用户加入聊天室
     * 客户端发送消息到 /app/join
     * 服务器广播消息到 /topic/public
     * 
     * @param joinMessage    加入消息
     * @param headerAccessor 消息头访问器
     * @return 处理后的消息
     */
    @MessageMapping("/join")
    @SendTo("/topic/public")
    public WebSocketMessage handleUserJoin(@Payload WebSocketMessage joinMessage,
            SimpMessageHeaderAccessor headerAccessor) {
        // 将用户名添加到WebSocket会话中
        headerAccessor.getSessionAttributes().put("username", joinMessage.getSender());

        // 设置时间戳
        joinMessage.setTimestamp(LocalDateTime.now());
        joinMessage.setType("JOIN");
        joinMessage.setContent(joinMessage.getSender() + " 加入了聊天室");

        // 记录日志
        System.out.println("用户加入: " + joinMessage.getSender());

        return joinMessage;
    }

    /**
     * 处理用户离开聊天室
     * 客户端发送消息到 /app/leave
     * 服务器广播消息到 /topic/public
     * 
     * @param leaveMessage 离开消息
     * @return 处理后的消息
     */
    @MessageMapping("/leave")
    @SendTo("/topic/public")
    public WebSocketMessage handleUserLeave(@Payload WebSocketMessage leaveMessage) {
        // 设置时间戳
        leaveMessage.setTimestamp(LocalDateTime.now());
        leaveMessage.setType("LEAVE");
        leaveMessage.setContent(leaveMessage.getSender() + " 离开了聊天室");

        // 记录日志
        System.out.println("用户离开: " + leaveMessage.getSender());

        return leaveMessage;
    }

    /**
     * 发送点对点消息
     * 客户端发送消息到 /app/private-message
     * 服务器发送消息到特定用户 /user/{username}/queue/private
     * 
     * @param privateMessage 私信消息
     */
    @MessageMapping("/private-message")
    public void handlePrivateMessage(@Payload WebSocketMessage privateMessage) {
        // 设置时间戳
        privateMessage.setTimestamp(LocalDateTime.now());
        privateMessage.setType("PRIVATE");

        // 发送给特定用户
        messagingTemplate.convertAndSendToUser(
                privateMessage.getReceiver(),
                "/queue/private",
                privateMessage);

        // 记录日志
        System.out.println("私信: " + privateMessage.getSender() + " -> " + privateMessage.getReceiver());
    }

    /**
     * 发送系统通知
     * 客户端发送消息到 /app/notification
     * 服务器广播消息到 /topic/notifications
     * 
     * @param notificationMessage 通知消息
     * @return 处理后的消息
     */
    @MessageMapping("/notification")
    @SendTo("/topic/notifications")
    public WebSocketMessage handleNotification(@Payload WebSocketMessage notificationMessage) {
        // 设置时间戳
        notificationMessage.setTimestamp(LocalDateTime.now());
        notificationMessage.setType("NOTIFICATION");

        // 记录日志
        System.out.println("系统通知: " + notificationMessage.getContent());

        return notificationMessage;
    }

    /**
     * 处理联邦学习相关消息
     * 客户端发送消息到 /app/federated-learning
     * 服务器广播消息到 /topic/federated-learning
     * 
     * @param flMessage 联邦学习消息
     * @return 处理后的消息
     */
    @MessageMapping("/federated-learning")
    @SendTo("/topic/federated-learning")
    public WebSocketMessage handleFederatedLearningMessage(@Payload WebSocketMessage flMessage) {
        // 设置时间戳
        flMessage.setTimestamp(LocalDateTime.now());
        flMessage.setType("FEDERATED_LEARNING");

        // 记录日志
        System.out.println("联邦学习消息: " + flMessage.getContent());

        return flMessage;
    }
}