package com.feduwacomm.utils;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UUID v7工具类测试
 * 基于RFC 9562标准验证UUID v7的实现
 * 
 * @author FedUWAComm Team
 * @version 2.0.0
 */
@SpringBootTest
class UuidUtilTest {

    @Autowired
    private UuidUtil uuidUtil;
    
    @Test
    void testGenerateUuid() {
        String uuid = uuidUtil.generateUuid();

        assertNotNull(uuid);
        assertEquals(32, uuid.length());
        assertTrue(uuidUtil.isValidUuid(uuid));
        assertTrue(uuidUtil.isUuidV7(uuid));
    }

    @Test
    void testGenerateUuidWithHyphens() {
        String uuid = uuidUtil.generateUuidWithHyphens();

        assertNotNull(uuid);
        assertEquals(36, uuid.length());
        assertTrue(uuid.contains("-"));
        assertTrue(uuidUtil.isValidUuid(uuid));
        assertTrue(uuidUtil.isUuidV7(uuid));
    }

    @Test
    void testUuidUniqueness() {
        Set<String> uuids = new HashSet<>();
        int count = 1000;

        for (int i = 0; i < count; i++) {
            String uuid = uuidUtil.generateUuid();
            assertTrue(uuids.add(uuid), "UUID should be unique: " + uuid);
        }

        assertEquals(count, uuids.size());
    }

    @Test
    void testIsValidUuid() {
        // 有效UUID
        String validUuid = uuidUtil.generateUuid();
        assertTrue(uuidUtil.isValidUuid(validUuid));

        // 带连字符的有效UUID
        String validUuidWithHyphens = uuidUtil.generateUuidWithHyphens();
        assertTrue(uuidUtil.isValidUuid(validUuidWithHyphens));

        // 无效UUID
        assertFalse(uuidUtil.isValidUuid(null));
        assertFalse(uuidUtil.isValidUuid(""));
        assertFalse(uuidUtil.isValidUuid("invalid"));
        assertFalse(uuidUtil.isValidUuid("1234567890123456789012345678901")); // 31位
        assertFalse(uuidUtil.isValidUuid("123456789012345678901234567890123")); // 33位
        assertFalse(uuidUtil.isValidUuid("1234567890123456789012345678901g")); // 包含非十六进制字符
    }

    @Test
    void testExtractTimestamp() {
        String uuid = uuidUtil.generateUuid();
        Long timestamp = uuidUtil.extractTimestamp(uuid);

        assertNotNull(timestamp);
        assertTrue(timestamp > 0);

        // 验证时间戳接近当前时间
        long currentTime = System.currentTimeMillis();
        assertTrue(Math.abs(currentTime - timestamp) < 1000); // 允许1秒误差
    }

    @Test
    void testExtractVersion() {
        String uuid = uuidUtil.generateUuid();
        Integer version = uuidUtil.extractVersion(uuid);

        assertNotNull(version);
        assertEquals(7, version);
    }

    @Test
    void testIsUuidV7() {
        String uuid = uuidUtil.generateUuid();
        assertTrue(uuidUtil.isUuidV7(uuid));

        // 测试非v7 UUID
        assertFalse(uuidUtil.isUuidV7("12345678901234567890123456789012"));
        assertFalse(uuidUtil.isUuidV7(null));
    }

    @Test
    void testUuidV7Format() {
        String uuid = uuidUtil.generateUuidWithHyphens();

        // UUID v7格式: xxxxxxxx-xxxx-7xxx-axxx-xxxxxxxxxxxx
        String[] parts = uuid.split("-");
        assertEquals(5, parts.length);
        assertEquals(8, parts[0].length());
        assertEquals(4, parts[1].length());
        assertEquals(4, parts[2].length());
        assertEquals(4, parts[3].length());
        assertEquals(12, parts[4].length());

        // 检查版本位
        assertTrue(parts[2].startsWith("7"));

        // 检查变体位 - 更准确的检查
        String cleanUuid = uuid.replace("-", "");
        String byte7Hex = cleanUuid.substring(14, 16);
        int byte7Value = Integer.parseInt(byte7Hex, 16);
        int variant = (byte7Value >> 2) & 0x03;
        assertEquals(2, variant, "Variant should be 2 (binary: 10)");
    }

    @Test
    void testTimestampOrdering() {
        // 生成多个UUID并验证时间顺序
        String[] uuids = new String[10];
        for (int i = 0; i < 10; i++) {
            uuids[i] = uuidUtil.generateUuid();
            try {
                Thread.sleep(1); // 确保时间戳不同
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 验证UUID按时间戳排序
        for (int i = 1; i < uuids.length; i++) {
            Long prevTimestamp = uuidUtil.extractTimestamp(uuids[i - 1]);
            Long currTimestamp = uuidUtil.extractTimestamp(uuids[i]);
            assertTrue(prevTimestamp <= currTimestamp,
                    "UUIDs should be ordered by timestamp");
        }
    }

    @Test
    void testRfc9562Compliance() {
        // 验证UUID v7完全符合RFC 9562标准
        String uuid = uuidUtil.generateUuidWithHyphens();
        System.out.println("Generated UUID: " + uuid);

        String cleanUuid = uuid.replace("-", "");

        // 验证长度
        assertEquals(32, cleanUuid.length(), "UUID should be 32 characters");

        // 验证十六进制格式
        assertTrue(cleanUuid.matches("[0-9a-f]{32}"), "UUID should be hexadecimal");

        // 验证版本位（第7字节的高4位）
        String byte6Hex = cleanUuid.substring(12, 14);
        int byte6Value = Integer.parseInt(byte6Hex, 16);
        int version = (byte6Value >> 4) & 0x0F;
        assertEquals(7, version, "Version should be 7");

        // 验证变体位（第7字节的中间2位）
        String byte7Hex = cleanUuid.substring(14, 16);
        int byte7Value = Integer.parseInt(byte7Hex, 16);
        int variant = (byte7Value >> 2) & 0x03;
        assertEquals(2, variant, "Variant should be 2 (binary: 10)");

        // 验证时间戳部分
        String timestampHex = cleanUuid.substring(0, 12);
        long timestamp = Long.parseLong(timestampHex, 16);
        long currentTime = System.currentTimeMillis();
        assertTrue(Math.abs(currentTime - timestamp) < 1000, "Timestamp should be close to current time");
    }

    @Test
    void testDetailedBitAnalysis() {
        // 详细分析UUID v7的位结构
        String uuid = uuidUtil.generateUuidWithHyphens();
        System.out.println("Generated UUID: " + uuid);

        String cleanUuid = uuid.replace("-", "");

        // 分析每个字节
        System.out.println("Byte analysis:");
        for (int i = 0; i < 16; i++) {
            String byteHex = cleanUuid.substring(i * 2, i * 2 + 2);
            int byteValue = Integer.parseInt(byteHex, 16);
            System.out.printf("Byte %d: %s (0x%02X, binary: %s)%n",
                    i, byteHex, byteValue, String.format("%8s", Integer.toBinaryString(byteValue)).replace(' ', '0'));
        }

        // 验证RFC 9562位布局
        // 字节0-5: unix_ts_ms (48 bits)
        String timestampHex = cleanUuid.substring(0, 12);
        long timestamp = Long.parseLong(timestampHex, 16);
        System.out.println("Timestamp (ms): " + timestamp);

        // 字节6: ver (4 bits) + rand_a (4 bits)
        String byte6Hex = cleanUuid.substring(12, 14);
        int byte6Value = Integer.parseInt(byte6Hex, 16);
        int version = (byte6Value >> 4) & 0x0F;
        int randAHigh = byte6Value & 0x0F;
        System.out.println("Version: " + version + ", rand_a high: " + randAHigh);

        // 字节7: rand_a (4 bits) + var (2 bits) + rand_b (2 bits)
        String byte7Hex = cleanUuid.substring(14, 16);
        int byte7Value = Integer.parseInt(byte7Hex, 16);
        int randALow = (byte7Value >> 4) & 0x0F;
        int variant = (byte7Value >> 2) & 0x03;
        int randBHigh = byte7Value & 0x03;
        System.out.println("rand_a low: " + randALow + ", variant: " + variant + ", rand_b high: " + randBHigh);

        // 字节8-15: rand_b (56 bits)
        String randBHex = cleanUuid.substring(16, 32);
        System.out.println("rand_b: " + randBHex);

        // 验证关键字段
        assertEquals(7, version, "Version should be 7");
        assertEquals(2, variant, "Variant should be 2");
        assertTrue(timestamp > 0, "Timestamp should be positive");
    }

    @Test
    void testUuidV7GenerationAndParsing() {
        // 测试UUID v7的生成和解析
        String uuid = uuidUtil.generateUuidWithHyphens();
        System.out.println("Generated UUID v7: " + uuid);

        // 验证UUID格式
        assertTrue(uuidUtil.isValidUuid(uuid), "UUID should be valid");
        assertTrue(uuidUtil.isUuidV7(uuid), "UUID should be v7");
        assertTrue(uuidUtil.isValidVariant(uuid), "UUID should have valid variant");

        // 验证时间戳提取
        Long timestamp = uuidUtil.extractTimestamp(uuid);
        assertNotNull(timestamp, "Timestamp should not be null");
        assertTrue(timestamp > 0, "Timestamp should be positive");

        // 验证版本提取
        Integer version = uuidUtil.extractVersion(uuid);
        assertNotNull(version, "Version should not be null");
        assertEquals(7, version, "Version should be 7");

        System.out.println("Extracted timestamp: " + timestamp);
        System.out.println("Extracted version: " + version);
    }

    @Test
    void testUuidV7VariantBit() {
        // 专门测试变体位
        String uuid = uuidUtil.generateUuidWithHyphens();
        System.out.println("Test UUID: " + uuid);

        // 验证变体位
        assertTrue(uuidUtil.isValidVariant(uuid), "UUID should have valid variant");

        // 手动验证变体位
        String cleanUuid = uuid.replace("-", "");
        String byte7Hex = cleanUuid.substring(14, 16);
        int byte7Value = Integer.parseInt(byte7Hex, 16);
        int variant = (byte7Value >> 2) & 0x03;
        assertEquals(2, variant, "Variant should be 2");

        System.out.println("Byte 7: " + byte7Hex + " (0x" + Integer.toHexString(byte7Value) + ")");
        System.out.println("Variant: " + variant + " (binary: " + Integer.toBinaryString(variant) + ")");
    }
}