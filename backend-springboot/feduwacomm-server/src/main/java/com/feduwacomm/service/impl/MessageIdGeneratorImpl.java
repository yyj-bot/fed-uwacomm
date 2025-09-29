package com.feduwacomm.service.impl;

import com.feduwacomm.service.MessageIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;
import java.security.SecureRandom;

/**
 * 消息ID生成器实现类
 *
 * 生成格式: {PREFIX}-{TIMESTAMP}-{SEQUENCE}-{RANDOM}
 * - PREFIX: 消息类型前缀（2-3字符）
 * - TIMESTAMP: 时间戳（yyyyMMddHHmmss格式）
 * - SEQUENCE: 序列号（3位，单实例内唯一）
 * - RANDOM: 随机字符串（4位十六进制）
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-28
 */
@Slf4j
@Service
public class MessageIdGeneratorImpl implements MessageIdGenerator {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String DELIMITER = "-";
    private static final AtomicLong sequenceCounter = new AtomicLong(1);
    private static final SecureRandom random = new SecureRandom();

    // 每1000个序列号重置一次，避免序列号过大
    private static final long MAX_SEQUENCE = 999;

    @Override
    public String generateMessageId(MessageType messageType) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String sequence = String.format("%03d", getNextSequence());
        String randomSuffix = generateRandomSuffix();

        String messageId = String.join(DELIMITER,
                messageType.getPrefix(),
                timestamp,
                sequence,
                randomSuffix);

        log.debug("生成消息ID: type={}, id={}", messageType, messageId);
        return messageId;
    }

    @Override
    public String generateWebSocketMessageId() {
        return generateMessageId(MessageType.WEBSOCKET);
    }

    @Override
    public String generateVmConnectionMessageId(boolean isConnect) {
        return generateMessageId(isConnect ? MessageType.VM_CONNECT : MessageType.VM_DISCONNECT);
    }

    @Override
    public String generateFederatedTaskMessageId() {
        return generateMessageId(MessageType.FEDERATED_TASK);
    }

    @Override
    public String generateSystemNotificationMessageId() {
        return generateMessageId(MessageType.SYSTEM_NOTIFICATION);
    }

    @Override
    public MessageType extractMessageType(String messageId) {
        if (messageId == null || messageId.isEmpty()) {
            return null;
        }

        String[] parts = messageId.split(DELIMITER);
        if (parts.length < 4) {
            return null;
        }

        String prefix = parts[0];
        for (MessageType type : MessageType.values()) {
            if (type.getPrefix().equals(prefix)) {
                return type;
            }
        }

        return null;
    }

    @Override
    public long extractTimestamp(String messageId) {
        if (messageId == null || messageId.isEmpty()) {
            return -1;
        }

        String[] parts = messageId.split(DELIMITER);
        if (parts.length < 4) {
            return -1;
        }

        try {
            String timestampStr = parts[1];
            LocalDateTime dateTime = LocalDateTime.parse(timestampStr, TIMESTAMP_FORMAT);
            return java.time.ZoneId.systemDefault().getRules()
                    .getOffset(dateTime)
                    .getTotalSeconds() * 1000;
        } catch (Exception e) {
            log.debug("无法从消息ID中提取时间戳: messageId={}", messageId);
            return -1;
        }
    }

    @Override
    public boolean isValidMessageId(String messageId) {
        if (messageId == null || messageId.isEmpty()) {
            return false;
        }

        String[] parts = messageId.split(DELIMITER);
        if (parts.length != 4) {
            return false;
        }

        // 验证前缀
        String prefix = parts[0];
        boolean validPrefix = false;
        for (MessageType type : MessageType.values()) {
            if (type.getPrefix().equals(prefix)) {
                validPrefix = true;
                break;
            }
        }
        if (!validPrefix) {
            return false;
        }

        // 验证时间戳格式
        try {
            LocalDateTime.parse(parts[1], TIMESTAMP_FORMAT);
        } catch (Exception e) {
            return false;
        }

        // 验证序列号格式（3位数字）
        String sequence = parts[2];
        if (sequence.length() != 3 || !sequence.matches("\\d{3}")) {
            return false;
        }

        // 验证随机后缀格式（4位十六进制）
        String randomSuffix = parts[3];
        if (randomSuffix.length() != 4 || !randomSuffix.matches("[0-9A-F]{4}")) {
            return false;
        }

        return true;
    }

    @Override
    public String getMessageIdInfo(String messageId) {
        if (!isValidMessageId(messageId)) {
            return "无效的消息ID格式: " + messageId;
        }

        String[] parts = messageId.split(DELIMITER);
        MessageType type = extractMessageType(messageId);
        long timestamp = extractTimestamp(messageId);

        StringBuilder info = new StringBuilder();
        info.append("消息ID详情:\n");
        info.append("  完整ID: ").append(messageId).append("\n");
        info.append("  消息类型: ").append(type != null ? type.name() : "未知").append("\n");
        info.append("  时间戳: ").append(parts[1]).append("\n");
        info.append("  序列号: ").append(parts[2]).append("\n");
        info.append("  随机后缀: ").append(parts[3]).append("\n");

        if (timestamp > 0) {
            info.append("  生成时间: ").append(java.time.Instant.ofEpochMilli(timestamp)).append("\n");
        }

        return info.toString();
    }

    /**
     * 获取下一个序列号
     */
    private long getNextSequence() {
        long current = sequenceCounter.getAndIncrement();
        if (current > MAX_SEQUENCE) {
            // 重置序列号
            sequenceCounter.set(1);
            return 1;
        }
        return current;
    }

    @Override
    public String generateServerMessageId() {
        return generateMessageId(MessageType.SYSTEM_NOTIFICATION);
    }

    @Override
    public String generateCommandMessageId() {
        return "CMD-" + generateTimestamp() + "-" + generateSequence() + "-" + generateRandomSuffix();
    }

    /**
     * 生成4位随机十六进制字符串
     */
    private String generateRandomSuffix() {
        int randomInt = random.nextInt(65536); // 0 到 FFFF
        return String.format("%04X", randomInt);
    }

    /**
     * 生成时间戳字符串
     */
    private String generateTimestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FORMAT);
    }

    /**
     * 生成序列号字符串
     */
    private String generateSequence() {
        return String.format("%03d", getNextSequence());
    }
}