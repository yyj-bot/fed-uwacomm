package com.feduwacomm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 模型类型枚举
 */
public enum ModelType {
    NEURAL_NETWORK("NEURAL_NETWORK", "神经网络"),
    RANDOM_FOREST("RANDOM_FOREST", "随机森林");

    private final String code;
    private final String description;

    ModelType(String code, String description) {
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

    public static ModelType fromCode(String code) {
        if (code == null) {
            return NEURAL_NETWORK;
        }
        for (ModelType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid model type code: " + code);
    }
}