package com.feduwacomm.service.impl;

import com.feduwacomm.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket服务实现类
 * 提供基础的WebSocket消息发送功能
 *
 * @author FedUWAComm Team
 * @version 1.5.0
 * @since 2025-09-28
 */
@Slf4j
@Service
public class WebSocketServiceImpl implements WebSocketService {

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    // 在线用户管理
    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();

    @Override
    public boolean sendMessageToUser(String userId, Object message) {
        try {
            if (userId == null || message == null) {
                log.warn("发送消息失败：用户ID或消息为空");
                return false;
            }

            if (!isUserOnline(userId)) {
                log.warn("用户不在线，无法发送消息: userId={}", userId);
                return false;
            }

            // 发送到用户专用通道
            String destination = "/topic/user/" + userId;
            messagingTemplate.convertAndSend(destination, message);

            log.debug("向用户发送消息成功: userId={}, destination={}", userId, destination);
            return true;
        } catch (Exception e) {
            log.error("向用户发送消息失败: userId={}, error={}", userId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public int sendBroadcastMessage(Object message) {
        try {
            if (message == null) {
                log.warn("广播消息失败：消息为空");
                return 0;
            }

            // 发送到广播通道
            messagingTemplate.convertAndSend("/topic/broadcast", message);

            int onlineCount = onlineUsers.size();
            log.debug("广播消息发送成功: 在线用户数={}", onlineCount);
            return onlineCount;
        } catch (Exception e) {
            log.error("广播消息失败: error={}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public boolean sendMessageToTopic(String topic, Object message) {
        try {
            if (topic == null || message == null) {
                log.warn("发送主题消息失败：主题或消息为空");
                return false;
            }

            String destination = "/topic/" + topic;
            messagingTemplate.convertAndSend(destination, message);

            log.debug("向主题发送消息成功: topic={}, destination={}", topic, destination);
            return true;
        } catch (Exception e) {
            log.error("向主题发送消息失败: topic={}, error={}", topic, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean isUserOnline(String userId) {
        return userId != null && onlineUsers.contains(userId);
    }

    @Override
    public List<String> getOnlineUsers() {
        return new ArrayList<>(onlineUsers);
    }

    @Override
    public int getOnlineUserCount() {
        return onlineUsers.size();
    }

    /**
     * 添加在线用户
     * 当用户建立WebSocket连接时调用
     */
    public void addOnlineUser(String userId) {
        if (userId != null) {
            onlineUsers.add(userId);
            log.debug("用户上线: userId={}, 当前在线数={}", userId, onlineUsers.size());
        }
    }

    /**
     * 移除在线用户
     * 当用户断开WebSocket连接时调用
     */
    public void removeOnlineUser(String userId) {
        if (userId != null) {
            onlineUsers.remove(userId);
            log.debug("用户下线: userId={}, 当前在线数={}", userId, onlineUsers.size());
        }
    }

    /**
     * 清空所有在线用户
     * 用于系统重启时的清理
     */
    public void clearAllOnlineUsers() {
        int previousCount = onlineUsers.size();
        onlineUsers.clear();
        log.info("清空所有在线用户: 之前在线数={}", previousCount);
    }

    @Override
    public int sendToAdmins(Map<String, Object> message) {
        try {
            if (message == null) {
                log.warn("向管理员发送消息失败：消息为空");
                return 0;
            }

            // 发送到管理员专用通道
            messagingTemplate.convertAndSend("/topic/admin", message);

            // 简化实现：假设所有在线用户都可能是管理员
            // 实际项目中应该维护管理员列表
            int adminCount = Math.min(onlineUsers.size(), 5); // 假设最多5个管理员在线
            log.debug("向管理员发送消息成功: 预估管理员数={}", adminCount);
            return adminCount;
        } catch (Exception e) {
            log.error("向管理员发送消息失败: error={}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public int sendToTaskSubscribers(String taskId, Map<String, Object> message) {
        try {
            if (taskId == null || message == null) {
                log.warn("向任务订阅者发送消息失败：任务ID或消息为空");
                return 0;
            }

            // 发送到任务订阅者专用通道
            String destination = "/topic/task/" + taskId;
            messagingTemplate.convertAndSend(destination, message);

            // 简化实现：假设每个任务有少量订阅者
            int subscriberCount = Math.min(onlineUsers.size(), 3); // 假设每个任务最多3个订阅者
            log.debug("向任务订阅者发送消息成功: taskId={}, 预估订阅者数={}", taskId, subscriberCount);
            return subscriberCount;
        } catch (Exception e) {
            log.error("向任务订阅者发送消息失败: taskId={}, error={}", taskId, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public int sendToAllUsers(Map<String, Object> message) {
        try {
            if (message == null) {
                log.warn("向所有用户发送消息失败：消息为空");
                return 0;
            }

            // 发送到所有用户通道
            messagingTemplate.convertAndSend("/topic/all", message);

            int userCount = onlineUsers.size();
            log.debug("向所有用户发送消息成功: 在线用户数={}", userCount);
            return userCount;
        } catch (Exception e) {
            log.error("向所有用户发送消息失败: error={}", e.getMessage(), e);
            return 0;
        }
    }
}