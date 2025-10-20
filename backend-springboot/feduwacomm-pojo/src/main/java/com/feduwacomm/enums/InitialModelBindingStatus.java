package com.feduwacomm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 初始模型绑定状态枚举
 */
public enum InitialModelBindingStatus {
    UNBOUND("UNBOUND", "未绑定"),
    BOUND("BOUND", "已绑定");

    private final String code;
    private final String description;

    InitialModelBindingStatus(String code, String description) {
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

    public static InitialModelBindingStatus fromCode(String code) {
        for (InitialModelBindingStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid initial model binding status: " + code);
    }
}
