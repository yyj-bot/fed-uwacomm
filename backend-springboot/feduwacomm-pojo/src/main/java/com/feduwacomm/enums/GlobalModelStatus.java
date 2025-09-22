package com.feduwacomm.enums;

/**
 * 全局模型状态枚举
 */
public enum GlobalModelStatus {
    PENDING("PENDING", "等待中"),
    AGGREGATING("AGGREGATING", "聚合中"),
    COMPLETED("COMPLETED", "已完成"),
    FAILED("FAILED", "失败");

    private final String code;
    private final String description;

    GlobalModelStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static GlobalModelStatus fromCode(String code) {
        if (code == null) {
            return PENDING;
        }
        for (GlobalModelStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid global model status code: " + code);
    }
}