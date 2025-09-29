package com.feduwacomm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * WebSocket协议消息对象
 * 符合协议v1.4标准格式
 *
 * @author FedUWAComm Team
 * @version 1.4.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolMessage {

    /**
     * 消息类型（必需）
     */
    @JsonProperty("type")
    private ProtocolType type;

    /**
     * 消息唯一标识（必需）
     * 格式: {prefix}-{timestamp}-{random}
     */
    @JsonProperty("id")
    private String id;

    /**
     * 消息时间戳（必需）
     * Jackson自动序列化为ISO字符串格式 (如: "2024-01-01T00:00:00.000Z")
     */
    @JsonProperty("timestamp")
    private Instant timestamp;

    /**
     * 虚拟机ID（必需）
     * 32位UUID格式
     */
    @JsonProperty("vmId")
    private String vmId;

    /**
     * 消息数据（必需）
     */
    @JsonProperty("data")
    private Map<String, Object> data;

    /**
     * 数字签名（必需，用于安全验证）
     */
    @JsonProperty("signature")
    private String signature;

} 