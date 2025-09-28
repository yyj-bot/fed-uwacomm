package com.feduwacomm.service;

import com.feduwacomm.dto.WebSocketMessage;
import com.feduwacomm.exception.UserException;
import com.feduwacomm.utils.UuidUtil;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket服务类
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Service
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final UuidUtil uuidUtil;

    // 在线用户统计
    private final AtomicInteger onlineUserCount = new AtomicInteger(0);

    // 在线用户列表（用户名 -> 会话ID）
    private final ConcurrentHashMap<String, String> onlineUsers = new ConcurrentHashMap<>();

    public WebSocketService(SimpMessagingTemplate messagingTemplate, UuidUtil uuidUtil) {
        this.messagingTemplate = messagingTemplate;
        this.uuidUtil = uuidUtil;
    }

    /**
     * 发送广播消息
     * 
     * @param message 消息内容
     */
    public void sendBroadcastMessage(String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("message", message);
        data.put("sender", "System");

        WebSocketMessage wsMessage = WebSocketMessage.builder()
            .type("BROADCAST")
            .id(uuidUtil.generateUuid())
            .vmId("system")
            .data(data)
            .signature("temp-signature")
            .build();
        wsMessage.setTimestampFromInstant(Instant.now());

        messagingTemplate.convertAndSend("/topic/public", wsMessage);
    }

    /**
     * 发送点对点消息
     * 
     * @param username 目标用户名
     * @param message  消息内容
     */
    public void sendPrivateMessage(String username, String message) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(message)) {
            throw UserException.paramValidationError("参数", "用户名和消息不能为空");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("message", message);
        data.put("sender", "System");
        data.put("receiver", username);

        WebSocketMessage wsMessage = WebSocketMessage.builder()
            .type("PRIVATE")
            .id(uuidUtil.generateUuid())
            .vmId("system")
            .data(data)
            .signature("temp-signature")
            .build();
        wsMessage.setTimestampFromInstant(Instant.now());

        messagingTemplate.convertAndSendToUser(username, "/queue/private", wsMessage);
    }

    /**
     * 发送系统通知
     * 
     * @param notification 通知内容
     */
    public void sendNotification(String notification) {
        Map<String, Object> data = new HashMap<>();
        data.put("notification", notification);
        data.put("sender", "System");

        WebSocketMessage wsMessage = WebSocketMessage.builder()
            .type("NOTIFICATION")
            .id(uuidUtil.generateUuid())
            .vmId("system")
            .data(data)
            .signature("temp-signature")
            .build();
        wsMessage.setTimestampFromInstant(Instant.now());

        messagingTemplate.convertAndSend("/topic/notifications", wsMessage);
    }

    /**
     * 发送联邦学习相关消息
     * 
     * @param message 联邦学习消息
     */
    public void sendFederatedLearningMessage(String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("message", message);
        data.put("sender", "System");

        WebSocketMessage wsMessage = WebSocketMessage.builder()
            .type("FEDERATED_LEARNING")
            .id(uuidUtil.generateUuid())
            .vmId("system")
            .data(data)
            .signature("temp-signature")
            .build();
        wsMessage.setTimestampFromInstant(Instant.now());

        messagingTemplate.convertAndSend("/topic/federated-learning", wsMessage);
    }

    /**
     * 用户上线
     * 
     * @param username  用户名
     * @param sessionId 会话ID
     */
    public void userOnline(String username, String sessionId) {
        onlineUsers.put(username, sessionId);
        onlineUserCount.incrementAndGet();

        // 发送用户上线通知
        sendBroadcastMessage(username + " 上线了");

        // 发送在线用户统计
        sendOnlineUserCount();
    }

    /**
     * 用户下线
     * 
     * @param username 用户名
     */
    public void userOffline(String username) {
        onlineUsers.remove(username);
        onlineUserCount.decrementAndGet();

        // 发送用户下线通知
        sendBroadcastMessage(username + " 下线了");

        // 发送在线用户统计
        sendOnlineUserCount();
    }

    /**
     * 获取在线用户数量
     * 
     * @return 在线用户数量
     */
    public int getOnlineUserCount() {
        return onlineUserCount.get();
    }

    /**
     * 获取在线用户列表
     * 
     * @return 在线用户列表
     */
    public ConcurrentHashMap<String, String> getOnlineUsers() {
        return new ConcurrentHashMap<>(onlineUsers);
    }

    /**
     * 检查用户是否在线
     * 
     * @param username 用户名
     * @return 是否在线
     */
    public boolean isUserOnline(String username) {
        return onlineUsers.containsKey(username);
    }

    /**
     * 发送在线用户统计
     */
    private void sendOnlineUserCount() {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "当前在线用户数: " + onlineUserCount.get());
        data.put("count", onlineUserCount.get());
        data.put("sender", "System");

        WebSocketMessage wsMessage = WebSocketMessage.builder()
            .type("USER_COUNT")
            .id(uuidUtil.generateUuid())
            .vmId("system")
            .data(data)
            .signature("temp-signature")
            .build();
        wsMessage.setTimestampFromInstant(Instant.now());

        messagingTemplate.convertAndSend("/topic/user-count", wsMessage);
    }

    /**
     * 发送联邦学习进度更新
     * 
     * @param progress 进度百分比
     * @param message  进度消息
     */
    public void sendFederatedLearningProgress(Integer progress, String message) {
        if (progress == null || !StringUtils.hasText(message)) {
            throw UserException.paramValidationError("参数", "进度和消息不能为空");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("message", message);
        data.put("progress", progress);
        data.put("sender", "System");

        WebSocketMessage wsMessage = WebSocketMessage.builder()
            .type("FL_PROGRESS")
            .id(uuidUtil.generateUuid())
            .vmId("system")
            .data(data)
            .signature("temp-signature")
            .build();
        wsMessage.setTimestampFromInstant(Instant.now());

        messagingTemplate.convertAndSend("/topic/federated-learning", wsMessage);
    }

    /**
     * 发送联邦学习结果
     * 
     * @param result 结果数据
     */
    public void sendFederatedLearningResult(Object result) {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "联邦学习完成");
        data.put("result", result);
        data.put("sender", "System");

        WebSocketMessage wsMessage = WebSocketMessage.builder()
            .type("FL_RESULT")
            .id(uuidUtil.generateUuid())
            .vmId("system")
            .data(data)
            .signature("temp-signature")
            .build();
        wsMessage.setTimestampFromInstant(Instant.now());

        messagingTemplate.convertAndSend("/topic/federated-learning", wsMessage);
    }
}