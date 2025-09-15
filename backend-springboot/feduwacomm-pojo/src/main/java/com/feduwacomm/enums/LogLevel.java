package com.feduwacomm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum LogLevel {
    DEBUG("DEBUG", "调试信息，用于开发调试"),
    INFO("INFO", "一般信息，记录系统正常运行状态"),
    WARN("WARN", "警告信息，系统可能出现问题"),
    ERROR("ERROR", "错误信息，系统出现错误");

    private final String code;
    private final String description;

    LogLevel(String code, String description) {
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

    public static LogLevel fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (LogLevel level : values()) {
            if (level.code.equals(code)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Invalid log level code: " + code);
    }
}