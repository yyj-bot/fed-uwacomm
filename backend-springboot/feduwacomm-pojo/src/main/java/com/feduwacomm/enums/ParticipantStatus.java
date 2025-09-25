package com.feduwacomm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 参与者状态枚举
 */
public enum ParticipantStatus {
    CREATED("CREATED", "已创建"),
    PENDING("PENDING", "等待中"),
    CONNECTED("CONNECTED", "已连接"),
    DISCONNECTED("DISCONNECTED", "未连接"),
    TRAINING("TRAINING", "训练中"),
    COMPLETED("COMPLETED", "已完成"),
    FAILED("FAILED", "失败");

    private final String code;
    private final String description;

    ParticipantStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ParticipantStatus fromCode(String code) {
        if (code == null) {
            return CONNECTED;
        }
        for (ParticipantStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid participant status code: " + code);
    }
}