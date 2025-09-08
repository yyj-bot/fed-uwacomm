package com.feduwacomm.enums;

public enum TaskStatus {
    PENDING("PENDING", "等待处理"),
    PROCESSING("PROCESSING", "处理中"),
    COMPLETED("COMPLETED", "已完成"),
    FAILED("FAILED", "处理失败"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String description;

    TaskStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

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