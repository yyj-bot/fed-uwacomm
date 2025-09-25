package com.feduwacomm.utils;

import com.feduwacomm.dto.ProtocolType;
import lombok.experimental.UtilityClass;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket消息构建工具类
 * 提供统一的消息构建接口，确保类型安全和一致性
 * 包含符合协议v1.4.1标准的消息ID生成功能
 */
@UtilityClass
public class MessageBuilder {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 创建基础消息
     * @param type 消息类型
     * @return 消息Map
     */
    public static Map<String, Object> createMessage(ProtocolType type) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", type.name());
        message.put("id", generateServerId());
        message.put("timestamp", java.time.Instant.now().toString());
        return message;
    }

    /**
     * 创建包含数据的消息
     * @param type 消息类型
     * @param data 数据内容
     * @return 消息Map
     */
    public static Map<String, Object> createMessage(ProtocolType type, Object data) {
        Map<String, Object> message = createMessage(type);
        message.put("data", data);
        return message;
    }

    /**
     * 创建响应消息
     * @param type 消息类型
     * @param success 是否成功
     * @param message 响应消息
     * @return 消息Map
     */
    public static Map<String, Object> createResponse(ProtocolType type, boolean success, String message) {
        Map<String, Object> response = createMessage(type);
        response.put("success", success);
        response.put("message", message);
        return response;
    }

    /**
     * 创建响应消息（包含数据）
     * @param type 消息类型
     * @param success 是否成功
     * @param message 响应消息
     * @param data 数据内容
     * @return 消息Map
     */
    public static Map<String, Object> createResponse(ProtocolType type, boolean success, String message, Object data) {
        Map<String, Object> response = createResponse(type, success, message);
        response.put("data", data);
        return response;
    }

    /**
     * 创建错误消息
     * @param type 消息类型
     * @param errorMessage 错误信息
     * @return 消息Map
     */
    public static Map<String, Object> createError(ProtocolType type, String errorMessage) {
        return createResponse(type, false, errorMessage);
    }

    /**
     * 创建成功消息
     * @param type 消息类型
     * @param successMessage 成功信息
     * @return 消息Map
     */
    public static Map<String, Object> createSuccess(ProtocolType type, String successMessage) {
        return createResponse(type, true, successMessage);
    }

    /**
     * 创建成功消息（包含数据）
     * @param type 消息类型
     * @param successMessage 成功信息
     * @param data 数据内容
     * @return 消息Map
     */
    public static Map<String, Object> createSuccess(ProtocolType type, String successMessage, Object data) {
        return createResponse(type, true, successMessage, data);
    }

    /**
     * 创建通知消息
     * @param notificationType 通知类型
     * @param title 通知标题
     * @param content 通知内容
     * @return 消息Map
     */
    public static Map<String, Object> createNotification(ProtocolType notificationType, String title, String content) {
        Map<String, Object> notification = createMessage(notificationType);
        notification.put("title", title);
        notification.put("content", content);
        notification.put("timestamp", System.currentTimeMillis());
        return notification;
    }

    /**
     * 创建通知消息（包含数据）
     * @param notificationType 通知类型
     * @param title 通知标题
     * @param content 通知内容
     * @param data 附加数据
     * @return 消息Map
     */
    public static Map<String, Object> createNotification(ProtocolType notificationType, String title, String content, Object data) {
        Map<String, Object> notification = createNotification(notificationType, title, content);
        notification.put("data", data);
        return notification;
    }

    // ========== 消息ID生成方法 (v1.4.1协议标准) ==========

    /**
     * 生成客户端消息ID
     * @return 格式: client-{timestamp}-{random}
     */
    public static String generateClientId() {
        return generateMessageId("client");
    }

    /**
     * 生成服务器端消息ID
     * @return 格式: server-{timestamp}-{random}
     */
    public static String generateServerId() {
        return generateMessageId("server");
    }

    /**
     * 生成命令消息ID
     * @return 格式: cmd-{timestamp}-{random}
     */
    public static String generateCommandId() {
        return generateMessageId("cmd");
    }

    /**
     * 生成标准化消息ID
     * 符合协议v1.4.1标准：{prefix}-{timestamp}-{random}
     *
     * @param prefix 前缀 (client/server/cmd)
     * @return 标准化消息ID
     */
    private static String generateMessageId(String prefix) {
        long timestamp = System.currentTimeMillis(); // 13位Unix毫秒时间戳
        int random = RANDOM.nextInt(1000000); // 0-999999的随机数
        return String.format("%s-%d-%06d", prefix, timestamp, random);
    }

    /**
     * 验证消息ID格式是否符合协议标准
     * @param messageId 待验证的消息ID
     * @return 是否符合标准格式
     */
    public static boolean isValidMessageId(String messageId) {
        if (messageId == null) {
            return false;
        }
        return messageId.matches("^(client|server)-\\d{13}-\\d{6}$|^cmd-\\d{13}-\\d{6}$");
    }

    /**
     * 创建带有客户端ID的消息
     * @param type 消息类型
     * @return 消息Map
     */
    public static Map<String, Object> createClientMessage(ProtocolType type) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", type.name());
        message.put("id", generateClientId());
        message.put("timestamp", java.time.Instant.now().toString());
        return message;
    }

    /**
     * 创建带有命令ID的消息
     * @param type 消息类型
     * @return 消息Map
     */
    public static Map<String, Object> createCommandMessage(ProtocolType type) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", type.name());
        message.put("id", generateCommandId());
        message.put("timestamp", java.time.Instant.now().toString());
        return message;
    }
}