package com.feduwacomm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 联邦学习任务状态枚举
 */
public enum FederatedTaskStatus {
    CREATED("CREATED", "已创建"),
    CREATING("CREATING", "创建中"),
    CONFIGURED("CONFIGURED", "已配置"),
    READY("READY", "准备就绪"),
    PENDING("PENDING", "等待中"),
    RUNNING("RUNNING", "运行中"),
    PAUSED("PAUSED", "已暂停"),
    COMPLETED("COMPLETED", "已完成"),
    FAILED("FAILED", "失败"),
    STOPPED("STOPPED", "已停止"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String description;

    FederatedTaskStatus(String code, String description) {
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

    public static FederatedTaskStatus fromCode(String code) {
        if (code == null) {
            return PENDING;
        }
        for (FederatedTaskStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid federated task status code: " + code);
    }
}
