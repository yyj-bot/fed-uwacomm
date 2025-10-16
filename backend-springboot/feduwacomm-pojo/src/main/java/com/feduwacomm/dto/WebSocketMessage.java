package com.feduwacomm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * WebSocket消息传输对象
 * 符合协议v1.4标准格式
 *
 * @author FedUWAComm Team
 * @version 1.5.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {

    /**
     * 消息类型（必需）
     */
    @JsonProperty("type")
    private String type;

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

    // 便利方法：从String设置时间戳（兼容性支持）
    @Deprecated
    public void setTimestampFromString(String timestampStr) {
        this.timestamp = timestampStr != null ? Instant.parse(timestampStr) : null;
    }

    // 便利方法：获取时间戳为String（兼容性支持）
    @Deprecated
    public String getTimestampAsString() {
        return this.timestamp != null ? this.timestamp.toString() : null;
    }

    // 兼容性支持：从旧格式迁移的便利方法
    @Deprecated
    public void setContent(String content) {
        // 将content作为data中的一个字段
        if (this.data == null) {
            this.data = new java.util.HashMap<>();
        }
        this.data.put("content", content);
    }

    @Deprecated
    public String getContent() {
        if (this.data != null && this.data.containsKey("content")) {
            return (String) this.data.get("content");
        }
        return null;
    }
}