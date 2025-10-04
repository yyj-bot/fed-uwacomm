package com.feduwacomm.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProtocolType枚举序列化测试
 * 验证Jackson序列化和反序列化功能
 */
class ProtocolTypeSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("测试CONNECT协议类型的序列化")
    void testConnectSerialization() throws Exception {
        // 测试序列化
        String json = objectMapper.writeValueAsString(ProtocolType.CONNECT);
        assertEquals("\"CONNECT\"", json);

        // 测试反序列化
        ProtocolType result = objectMapper.readValue("\"CONNECT\"", ProtocolType.class);
        assertEquals(ProtocolType.CONNECT, result);
    }

    @Test
    @DisplayName("测试所有协议类型的序列化反序列化")
    void testAllProtocolTypesSerialization() throws Exception {
        for (ProtocolType type : ProtocolType.values()) {
            // 序列化
            String json = objectMapper.writeValueAsString(type);

            // 反序列化
            ProtocolType result = objectMapper.readValue(json, ProtocolType.class);

            assertEquals(type, result, "协议类型 " + type + " 序列化反序列化失败");
        }
    }

    @Test
    @DisplayName("测试ProtocolMessage包含CONNECT类型的序列化")
    void testProtocolMessageWithConnectSerialization() throws Exception {
        ProtocolMessage message = ProtocolMessage.builder()
                .type(ProtocolType.CONNECT)
                .id("test-123")
                .vmId("vm-123")
                .build();

        // 序列化
        String json = objectMapper.writeValueAsString(message);
        assertTrue(json.contains("\"type\":\"CONNECT\""));

        // 反序列化
        ProtocolMessage result = objectMapper.readValue(json, ProtocolMessage.class);
        assertEquals(ProtocolType.CONNECT, result.getType());
    }

    @Test
    @DisplayName("测试无效协议类型的反序列化")
    void testInvalidProtocolTypeDeserialization() {
        assertThrows(IllegalArgumentException.class, () -> {
            ProtocolType.fromString("INVALID_TYPE");
        });
    }

    @Test
    @DisplayName("测试null值处理")
    void testNullValueHandling() {
        assertThrows(IllegalArgumentException.class, () -> {
            ProtocolType.fromString(null);
        });
    }

    @Test
    @DisplayName("测试大小写不敏感")
    void testCaseInsensitive() {
        assertEquals(ProtocolType.CONNECT, ProtocolType.fromString("connect"));
        assertEquals(ProtocolType.CONNECT, ProtocolType.fromString("Connect"));
        assertEquals(ProtocolType.CONNECT, ProtocolType.fromString("CONNECT"));
    }
}