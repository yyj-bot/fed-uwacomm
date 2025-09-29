package com.feduwacomm.service;

/**
 * 消息ID生成器服务
 *
 * 统一管理系统中所有消息的ID生成，提供可跟踪、唯一的消息标识符
 * 替换项目中散落的UUID生成，提供更好的消息跟踪和调试能力
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-28
 */
public interface MessageIdGenerator {

    /**
     * 消息类型枚举
     * 用于生成带有语义的消息ID前缀
     */
    enum MessageType {
        WEBSOCKET("WS"),
        VM_CONNECT("VC"),
        VM_DISCONNECT("VD"),
        USER_LEAVE("UL"),
        FEDERATED_TASK("FT"),
        MODEL_BROADCAST("MB"),
        ROUND_START("RS"),
        ROUND_COMPLETE("RC"),
        TRAINING_DATA("TD"),
        SYSTEM_NOTIFICATION("SN");

        private final String prefix;

        MessageType(String prefix) {
            this.prefix = prefix;
        }

        public String getPrefix() {
            return prefix;
        }
    }

    /**
     * 生成标准的消息ID
     * 格式: {PREFIX}-{TIMESTAMP}-{SEQUENCE}-{RANDOM}
     * 例如: WS-20250928142530-001-A7B2
     *
     * @param messageType 消息类型
     * @return 生成的消息ID
     */
    String generateMessageId(MessageType messageType);

    /**
     * 生成WebSocket消息ID
     * 这是一个便捷方法，专门用于WebSocket消息
     *
     * @return WebSocket消息ID
     */
    String generateWebSocketMessageId();

    /**
     * 生成VM连接相关的消息ID
     *
     * @param isConnect true表示连接，false表示断开
     * @return VM连接消息ID
     */
    String generateVmConnectionMessageId(boolean isConnect);

    /**
     * 生成联邦任务相关的消息ID
     *
     * @return 联邦任务消息ID
     */
    String generateFederatedTaskMessageId();

    /**
     * 生成系统通知消息ID
     *
     * @return 系统通知消息ID
     */
    String generateSystemNotificationMessageId();

    /**
     * 从消息ID中提取消息类型
     *
     * @param messageId 消息ID
     * @return 消息类型，如果无法识别则返回null
     */
    MessageType extractMessageType(String messageId);

    /**
     * 从消息ID中提取时间戳
     *
     * @param messageId 消息ID
     * @return Unix时间戳（毫秒），如果无法提取则返回-1
     */
    long extractTimestamp(String messageId);

    /**
     * 验证消息ID的格式是否正确
     *
     * @param messageId 消息ID
     * @return true表示格式正确，false表示格式错误
     */
    boolean isValidMessageId(String messageId);

    /**
     * 生成调试信息
     * 解析消息ID并返回详细信息
     *
     * @param messageId 消息ID
     * @return 消息ID的详细信息字符串
     */
    String getMessageIdInfo(String messageId);

    /**
     * 生成服务器消息ID
     * 用于服务器主动发送的消息
     *
     * @return 服务器消息ID
     */
    String generateServerMessageId();

    /**
     * 生成命令消息ID
     * 用于系统命令相关的消息
     *
     * @return 命令消息ID
     */
    String generateCommandMessageId();
}