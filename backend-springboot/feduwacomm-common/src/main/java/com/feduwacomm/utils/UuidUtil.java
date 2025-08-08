package com.feduwacomm.utils;

import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.UUID;

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
     * 构造函数
     */
    UuidUtil() {
        // 包可见构造函数
    }

    /**
     * 生成UUID v7格式的字符串（32位，无连字符）
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
     * 生成UUID v7
     * 基于RFC 9562标准实现
     * 
     * @return UUID v7字符串
     */
    private String generateUuidV7() {
        byte[] value = randomBytes();
        ByteBuffer buf = ByteBuffer.wrap(value);
        long high = buf.getLong();
        long low = buf.getLong();
        // "01988a77-0006-7278-9a17-ce17cb2d8e70"
        
        return  new UUID(high, low).toString();
    }

    /**
     * 生成符合RFC 9562标准的随机字节数组
     * 
     * @return 16字节的UUID数据
     */
    private byte[] randomBytes() {
        // 生成随机字节
        byte[] value = new byte[16];
        random.nextBytes(value);

        // 获取当前时间戳（毫秒）
        ByteBuffer timestamp = ByteBuffer.allocate(Long.BYTES);
        timestamp.putLong(System.currentTimeMillis());

        // 前6字节：Unix时间戳（48位）
        // RFC 9562: unix_ts_ms (48 bits) - Unix timestamp in milliseconds
        System.arraycopy(timestamp.array(), 2, value, 0, 6);

        // 第7字节：版本位(4位) + 随机数A的高4位
        // RFC 9562: ver (4 bits) - Version 7 + rand_a (12 bits) - Random data
        int randA = random.nextInt(4096); // 12位随机数
        value[6] = (byte) (0x70 | ((randA >> 8) & 0x0F));

        // 第8字节：随机数A的低4位 + 变体位(2位) + 随机数B的高2位
        // RFC 9562: rand_a (4 bits) + var (2 bits) - Variant bits (10) + rand_b (62
        // bits)
        int randB = random.nextInt(64); // 6位随机数
        // 变体位设置为10 (二进制)，即0x08 (00001000)
        value[7] = (byte) (((randA & 0x0F) << 4) | 0x08 | (randB & 0x03));

        // 第9-15字节：随机数B的剩余部分
        // 生成剩余的随机字节
        byte[] remainingRandom = new byte[7];
        random.nextBytes(remainingRandom);
        System.arraycopy(remainingRandom, 0, value, 8, 7);

        return value;
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

            // 提取前6字节作为Unix时间戳（毫秒）
            // RFC 9562: unix_ts_ms (48 bits) - Unix timestamp in milliseconds
            String timestampHex = cleanUuid.substring(0, 12);
            return Long.parseLong(timestampHex, 16);
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
            // 版本号在第7字节的高4位
            String versionHex = cleanUuid.substring(12, 14);
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

    /**
     * 检查UUID的变体位是否正确
     * 
     * @param uuid UUID字符串
     * @return 变体位是否正确
     */
    public boolean isValidVariant(String uuid) {
        if (!isValidUuid(uuid)) {
            return false;
        }

        try {
            String cleanUuid = uuid.replace("-", "");
            // 变体位在第7字节的中间2位
            String variantHex = cleanUuid.substring(14, 16);
            int variantByte = Integer.parseInt(variantHex, 16);
            int variant = (variantByte >> 2) & 0x03;
            // RFC 9562: 变体位应该是10 (二进制)
            return variant == 2;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 生成符合RFC 9562标准的测试向量
     * 用于验证实现的正确性
     * 
     * @return 测试用的UUID v7
     */
    public String generateTestVector() {
        // 使用RFC 9562文档中的测试时间戳：2022-02-22 14:22:22.000 GMT-05:00
        // 对应Unix时间戳：1645557742000
        long testTimestamp = 1645557742000L;

        byte[] value = new byte[16];
        random.nextBytes(value);

        // 前6字节：Unix时间戳（48位）
        ByteBuffer timestamp = ByteBuffer.allocate(Long.BYTES);
        timestamp.putLong(testTimestamp);
        System.arraycopy(timestamp.array(), 2, value, 0, 6);

        // 第7字节：版本位(4位) + 随机数A的高4位
        int randA = 0x0CC3; // 使用RFC文档中的测试值
        value[6] = (byte) (0x70 | ((randA >> 8) & 0x0F));

        // 第8字节：随机数A的低4位 + 变体位(2位) + 随机数B的高2位
        int randB = 0x3F; // 使用RFC文档中的测试值
        value[7] = (byte) (((randA & 0x0F) << 4) | 0x08 | (randB & 0x03));

        // 第9-15字节：随机数B的剩余部分
        byte[] remainingRandom = { (byte) 0x98, (byte) 0xC4, (byte) 0xDC, 0x0C, 0x0C, 0x07, (byte) 0x39 };
        System.arraycopy(remainingRandom, 0, value, 8, 7);

        ByteBuffer buf = ByteBuffer.wrap(value);
        long high = buf.getLong();
        long low = buf.getLong();
        return new UUID(high, low).toString();
    }
}