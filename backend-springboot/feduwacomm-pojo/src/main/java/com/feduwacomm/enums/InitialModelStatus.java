package com.feduwacomm.enums;

/**
 * 初始模型状态枚举
 */
public enum InitialModelStatus {
    GENERATING("GENERATING", "生成中"),
    READY("READY", "就绪"),
    DISTRIBUTED("DISTRIBUTED", "已分发"),
    FAILED("FAILED", "失败");

    private final String code;
    private final String description;

    InitialModelStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static InitialModelStatus fromCode(String code) {
        if (code == null) {
            return GENERATING;
        }
        for (InitialModelStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid initial model status code: " + code);
    }
}