package com.feduwacomm.service;

import java.util.Map;

/**
 * WebSocket服务接口
 * 提供WebSocket消息发送的基础能力
 *
 * @author FedUWAComm Team
 * @version 1.5.0
 * @since 2025-09-28
 */
public interface WebSocketService {

    /**
     * 向指定用户发送消息
     *
     * @param userId 用户ID
     * @param message 消息内容
     * @return 是否发送成功
     */
    boolean sendMessageToUser(String userId, Object message);

    /**
     * 广播消息给所有在线用户
     *
     * @param message 消息内容
     * @return 成功发送的用户数量
     */
    int sendBroadcastMessage(Object message);

    /**
     * 向指定主题发送消息
     *
     * @param topic 主题
     * @param message 消息内容
     * @return 是否发送成功
     */
    boolean sendMessageToTopic(String topic, Object message);

    /**
     * 检查用户是否在线
     *
     * @param userId 用户ID
     * @return 是否在线
     */
    boolean isUserOnline(String userId);

    /**
     * 获取在线用户列表
     *
     * @return 在线用户ID列表
     */
    java.util.List<String> getOnlineUsers();

    /**
     * 获取在线用户数量
     *
     * @return 在线用户数量
     */
    int getOnlineUserCount();

    /**
     * 向管理员用户发送消息
     *
     * @param message 消息内容
     * @return 成功发送的管理员数量
     */
    int sendToAdmins(Map<String, Object> message);

    /**
     * 向任务订阅者发送消息
     *
     * @param taskId 任务ID
     * @param message 消息内容
     * @return 成功发送的订阅者数量
     */
    int sendToTaskSubscribers(String taskId, Map<String, Object> message);

    /**
     * 向所有在线用户发送消息
     *
     * @param message 消息内容
     * @return 成功发送的用户数量
     */
    int sendToAllUsers(Map<String, Object> message);
}