package com.feduwacomm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 虚拟机状态枚举
 */
public enum VmStatus {
    OFFLINE("OFFLINE", "离线"),
    RUNNING("RUNNING", "运行中"),
    STOPPED("STOPPED", "已停止"),
    STARTING("STARTING", "启动中"),
    STOPPING("STOPPING", "停止中"),
    ERROR("ERROR", "错误");

    private final String code;
    private final String description;

    VmStatus(String code, String description) {
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

    public static VmStatus fromCode(String code) {
        if (code == null) {
            return OFFLINE;
        }
        for (VmStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid VM status code: " + code);
    }
}