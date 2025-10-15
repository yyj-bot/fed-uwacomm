package com.feduwacomm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 生成方法枚举
 */
public enum GenerationMethod {
    AUTO("AUTO", "自动生成"),
    CUSTOM("CUSTOM", "自定义上传");

    private final String code;
    private final String description;

    GenerationMethod(String code, String description) {
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

    public static GenerationMethod fromCode(String code) {
        if (code == null) {
            return AUTO;
        }
        for (GenerationMethod method : values()) {
            if (method.code.equals(code)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Invalid generation method code: " + code);
    }
}
