package com.feduwacomm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 生成方法枚举
 */
public enum GenerationMethod {
    RANDOM("RANDOM", "随机生成"),
    CUSTOM_UPLOAD("CUSTOM_UPLOAD", "自定义上传");

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
            return RANDOM;
        }
        for (GenerationMethod method : values()) {
            if (method.code.equals(code)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Invalid generation method code: " + code);
    }
}