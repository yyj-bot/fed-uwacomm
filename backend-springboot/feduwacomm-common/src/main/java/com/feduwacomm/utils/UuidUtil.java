package com.feduwacomm.utils;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * UUID v7生成工具类
 * 基于RFC 9562标准实现，提供数据库友好的UUID生成
 * 
 * @author FedUWAComm Team
 * @version 2.0.0
 */
@Component
public class UuidUtil {

    /**
     * 随机数生成器
     */
    private final SecureRandom random = new SecureRandom();

    /**
     * 序列号生成器，用于确保同一毫秒内的唯一性
     */
    private final ThreadLocal<Long> sequence = ThreadLocal.withInitial(() -> 0L);

    /**
     * 上次生成UUID的时间戳
     */
    private final ThreadLocal<Long> lastTimestamp = ThreadLocal.withInitial(() -> 0L);

    /**
     * 私有构造函数
     */
    private UuidUtil() {
        // 私有构造函数
    }

    /**
     * 生成UUID v7格式的字符串（32位，无连字符）
     * 格式: 时间戳(48位) + 版本(4位) + 随机数(74位)
     * 
     * @return 32位UUID字符串
     */
    public String generateUuid() {
        return generateUuidV7().replace("-", "");
    }

    /**
     * 生成UUID v7格式的字符串（带连字符）
     * 
     * @return 标准格式的UUID字符串
     */
    public String generateUuidWithHyphens() {
        return generateUuidV7();
    }

    /**
     * 生成用户ID（UUID v7格式）
     * 
     * @return 用户ID
     */
    public String generateUserId() {
        return generateUuid();
    }

    /**
     * 生成权限ID（UUID v7格式）
     * 
     * @return 权限ID
     */
    public String generatePermissionId() {
        return generateUuid();
    }

    /**
     * 生成虚拟机ID（UUID v7格式）
     * 
     * @return 虚拟机ID
     */
    public String generateVmId() {
        return generateUuid();
    }

    /**
     * 生成任务ID（UUID v7格式）
     * 
     * @return 任务ID
     */
    public String generateTaskId() {
        return generateUuid();
    }

    /**
     * 生成数据ID（UUID v7格式）
     * 
     * @return 数据ID
     */
    public String generateDataId() {
        return generateUuid();
    }

    /**
     * 生成模型ID（UUID v7格式）
     * 
     * @return 模型ID
     */
    public String generateModelId() {
        return generateUuid();
    }

    /**
     * 生成日志ID（UUID v7格式）
     * 
     * @return 日志ID
     */
    public String generateLogId() {
        return generateUuid();
    }

    /**
     * 生成虚拟机运行日志ID（UUID v7格式）
     * 
     * @return 虚拟机运行日志ID
     */
    public String generateVmLogId() {
        return generateUuid();
    }

    /**
     * 生成UUID v7
     * 基于RFC 9562标准实现
     * 
     * @return UUID v7字符串
     */
    private String generateUuidV7() {
        long currentTimestamp = System.currentTimeMillis();
        long currentSequence = sequence.get();

        // 确保同一毫秒内的序列号递增
        if (currentTimestamp == lastTimestamp.get()) {
            currentSequence++;
            sequence.set(currentSequence);
        } else {
            currentSequence = 0;
            sequence.set(currentSequence);
            lastTimestamp.set(currentTimestamp);
        }

        // 转换为Unix时间戳（秒）
        long unixTimestamp = currentTimestamp / 1000;

        // 获取毫秒部分
        long millis = currentTimestamp % 1000;

        // 构建UUID v7的字节数组
        byte[] uuidBytes = new byte[16];

        // 前6字节：Unix时间戳（48位）
        uuidBytes[0] = (byte) (unixTimestamp >> 40);
        uuidBytes[1] = (byte) (unixTimestamp >> 32);
        uuidBytes[2] = (byte) (unixTimestamp >> 24);
        uuidBytes[3] = (byte) (unixTimestamp >> 16);
        uuidBytes[4] = (byte) (unixTimestamp >> 8);
        uuidBytes[5] = (byte) unixTimestamp;

        // 第7字节：毫秒高4位 + 版本号(7) + 随机数低2位
        uuidBytes[6] = (byte) ((millis >> 4) | 0x70 | (random.nextInt(4) << 2));

        // 第8字节：毫秒低4位 + 随机数高4位
        uuidBytes[7] = (byte) ((millis << 4) | (random.nextInt(16) & 0x0F));

        // 第9字节：变体位(10) + 随机数
        uuidBytes[8] = (byte) (0x80 | (random.nextInt(64) & 0x3F));

        // 剩余7字节：随机数
        byte[] randomBytes = new byte[7];
        random.nextBytes(randomBytes);
        System.arraycopy(randomBytes, 0, uuidBytes, 9, 7);

        // 转换为UUID字符串
        return bytesToUuidString(uuidBytes);
    }

    /**
     * 将字节数组转换为UUID字符串
     * 
     * @param bytes 16字节的UUID数据
     * @return UUID字符串
     */
    private String bytesToUuidString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(36);

        for (int i = 0; i < 16; i++) {
            if (i == 4 || i == 6 || i == 8 || i == 10) {
                sb.append('-');
            }
            sb.append(String.format("%02x", bytes[i] & 0xFF));
        }

        return sb.toString();
    }

    /**
     * 验证UUID格式是否正确
     * 
     * @param uuid 待验证的UUID
     * @return 是否为有效格式
     */
    public boolean isValidUuid(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            return false;
        }

        // 移除连字符
        String cleanUuid = uuid.replace("-", "");

        // 检查长度
        if (cleanUuid.length() != 32) {
            return false;
        }

        // 检查是否为十六进制字符
        return cleanUuid.matches("[0-9a-fA-F]{32}");
    }

    /**
     * 从UUID中提取时间戳
     * 
     * @param uuid UUID字符串
     * @return 时间戳（毫秒）
     */
    public Long extractTimestamp(String uuid) {
        if (!isValidUuid(uuid)) {
            return null;
        }

        try {
            String cleanUuid = uuid.replace("-", "");

            // 提取前6字节作为时间戳
            String timestampHex = cleanUuid.substring(0, 12);
            long unixTimestamp = Long.parseLong(timestampHex, 16);

            // 提取毫秒部分
            String millisHex = cleanUuid.substring(12, 16);
            long millis = Long.parseLong(millisHex, 16) >> 4;

            // 转换为毫秒时间戳
            return unixTimestamp * 1000 + millis;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 从UUID中提取版本号
     * 
     * @param uuid UUID字符串
     * @return 版本号
     */
    public Integer extractVersion(String uuid) {
        if (!isValidUuid(uuid)) {
            return null;
        }

        try {
            String cleanUuid = uuid.replace("-", "");
            String versionHex = cleanUuid.substring(12, 16);
            int versionByte = Integer.parseInt(versionHex, 16);
            return (versionByte >> 4) & 0x0F;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 检查UUID是否为v7版本
     * 
     * @param uuid UUID字符串
     * @return 是否为v7版本
     */
    public boolean isUuidV7(String uuid) {
        Integer version = extractVersion(uuid);
        return version != null && version == 7;
    }
}