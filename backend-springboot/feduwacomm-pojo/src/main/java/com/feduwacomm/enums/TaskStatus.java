package com.feduwacomm.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TaskStatus {
    PENDING("PENDING", "等待处理"),
    PROCESSING("PROCESSING", "处理中"),
    RUNNING("RUNNING", "运行中"),
    COMPLETED("COMPLETED", "已完成"),
    FAILED("FAILED", "处理失败"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String description;

    TaskStatus(String code, String description) {
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

    @JsonCreator
    public static TaskStatus fromCode(String code) {
        if (code == null) {
            return PENDING;
        }
        for (TaskStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid task status code: " + code);
    }
}