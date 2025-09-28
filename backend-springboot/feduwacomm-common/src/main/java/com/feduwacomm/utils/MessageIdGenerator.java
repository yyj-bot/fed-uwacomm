package com.feduwacomm.utils;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.regex.Pattern;

/**
 * WebSocket协议消息ID生成器
 * 符合协议v1.4标准: {prefix}-{timestamp}-{random}
 *
 * @author FedUWAComm Team
 * @version 1.4.0
 */
@Component
public class MessageIdGenerator {

    private static final SecureRandom random = new SecureRandom();
    private static final Pattern MESSAGE_ID_PATTERN = Pattern.compile("^(client|server|cmd)-\\d{13}-\\d{6}$");

    /**
     * 生成服务端消息ID
     * 格式: server-{timestamp}-{random}
     *
     * @return 标准格式的服务端消息ID
     */
    public String generateServerMessageId() {
        long timestamp = System.currentTimeMillis();
        int randomNum = random.nextInt(1000000); // 0-999999
        return String.format("server-%d-%06d", timestamp, randomNum);
    }

    /**
     * 生成命令消息ID
     * 格式: cmd-{timestamp}-{random}
     *
     * @return 标准格式的命令消息ID
     */
    public String generateCommandMessageId() {
        long timestamp = System.currentTimeMillis();
        int randomNum = random.nextInt(1000000); // 0-999999
        return String.format("cmd-%d-%06d", timestamp, randomNum);
    }

    /**
     * 验证消息ID格式是否符合协议标准
     *
     * @param messageId 待验证的消息ID
     * @return true如果格式正确，false否则
     */
    public boolean validateMessageId(String messageId) {
        if (messageId == null || messageId.trim().isEmpty()) {
            return false;
        }
        return MESSAGE_ID_PATTERN.matcher(messageId.trim()).matches();
    }

    /**
     * 从消息ID中提取时间戳
     *
     * @param messageId 消息ID
     * @return 时间戳，如果格式不正确返回-1
     */
    public long extractTimestamp(String messageId) {
        if (!validateMessageId(messageId)) {
            return -1;
        }

        try {
            String[] parts = messageId.split("-");
            if (parts.length == 3) {
                return Long.parseLong(parts[1]);
            }
        } catch (NumberFormatException e) {
            // 静默处理解析错误
        }

        return -1;
    }

    /**
     * 从消息ID中提取前缀
     *
     * @param messageId 消息ID
     * @return 前缀(client/server/cmd)，如果格式不正确返回null
     */
    public String extractPrefix(String messageId) {
        if (!validateMessageId(messageId)) {
            return null;
        }

        String[] parts = messageId.split("-");
        if (parts.length == 3) {
            return parts[0];
        }

        return null;
    }

    /**
     * 检查消息ID是否为客户端生成
     *
     * @param messageId 消息ID
     * @return true如果是客户端消息ID
     */
    public boolean isClientMessage(String messageId) {
        return "client".equals(extractPrefix(messageId));
    }

    /**
     * 检查消息ID是否为服务端生成
     *
     * @param messageId 消息ID
     * @return true如果是服务端消息ID
     */
    public boolean isServerMessage(String messageId) {
        return "server".equals(extractPrefix(messageId));
    }

    /**
     * 检查消息ID是否为命令消息
     *
     * @param messageId 消息ID
     * @return true如果是命令消息ID
     */
    public boolean isCommandMessage(String messageId) {
        return "cmd".equals(extractPrefix(messageId));
    }
}