package com.feduwacomm.utils;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UUID v7工具类测试
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
    void testGenerateUserId() {
        String userId = uuidUtil.generateUserId();

        assertNotNull(userId);
        assertEquals(32, userId.length());
        assertTrue(uuidUtil.isValidUuid(userId));
        assertTrue(uuidUtil.isUuidV7(userId));
    }

    @Test
    void testGeneratePermissionId() {
        String permissionId = uuidUtil.generatePermissionId();

        assertNotNull(permissionId);
        assertEquals(32, permissionId.length());
        assertTrue(uuidUtil.isValidUuid(permissionId));
        assertTrue(uuidUtil.isUuidV7(permissionId));
    }

    @Test
    void testGenerateVmId() {
        String vmId = uuidUtil.generateVmId();

        assertNotNull(vmId);
        assertEquals(32, vmId.length());
        assertTrue(uuidUtil.isValidUuid(vmId));
        assertTrue(uuidUtil.isUuidV7(vmId));
    }

    @Test
    void testGenerateTaskId() {
        String taskId = uuidUtil.generateTaskId();

        assertNotNull(taskId);
        assertEquals(32, taskId.length());
        assertTrue(uuidUtil.isValidUuid(taskId));
        assertTrue(uuidUtil.isUuidV7(taskId));
    }

    @Test
    void testGenerateDataId() {
        String dataId = uuidUtil.generateDataId();

        assertNotNull(dataId);
        assertEquals(32, dataId.length());
        assertTrue(uuidUtil.isValidUuid(dataId));
        assertTrue(uuidUtil.isUuidV7(dataId));
    }

    @Test
    void testGenerateModelId() {
        String modelId = uuidUtil.generateModelId();

        assertNotNull(modelId);
        assertEquals(32, modelId.length());
        assertTrue(uuidUtil.isValidUuid(modelId));
        assertTrue(uuidUtil.isUuidV7(modelId));
    }

    @Test
    void testGenerateLogId() {
        String logId = uuidUtil.generateLogId();

        assertNotNull(logId);
        assertEquals(32, logId.length());
        assertTrue(uuidUtil.isValidUuid(logId));
        assertTrue(uuidUtil.isUuidV7(logId));
    }

    @Test
    void testGenerateVmLogId() {
        String vmLogId = uuidUtil.generateVmLogId();

        assertNotNull(vmLogId);
        assertEquals(32, vmLogId.length());
        assertTrue(uuidUtil.isValidUuid(vmLogId));
        assertTrue(uuidUtil.isUuidV7(vmLogId));
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

        // 检查变体位
        assertTrue(parts[3].startsWith("8") || parts[3].startsWith("9") ||
                parts[3].startsWith("a") || parts[3].startsWith("b"));
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
    void testExtractTimestampWithHyphens() {
        String uuid = uuidUtil.generateUuidWithHyphens();
        Long timestamp = uuidUtil.extractTimestamp(uuid);

        assertNotNull(timestamp);
        assertTrue(timestamp > 0);
    }

    @Test
    void testExtractVersionWithHyphens() {
        String uuid = uuidUtil.generateUuidWithHyphens();
        Integer version = uuidUtil.extractVersion(uuid);

        assertNotNull(version);
        assertEquals(7, version);
    }
}