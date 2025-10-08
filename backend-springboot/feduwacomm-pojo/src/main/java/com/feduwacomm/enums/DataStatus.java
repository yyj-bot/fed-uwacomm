package com.feduwacomm.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 数据状态枚举
 */
public enum DataStatus {
    UPLOADING("UPLOADING", "上传中"),
    PROCESSING("PROCESSING", "处理中"),
    READY("READY", "就绪"),
    ERROR("ERROR", "错误");

    private final String code;
    private final String description;

    DataStatus(String code, String description) {
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
    public static DataStatus fromCode(String code) {
        if (code == null) {
            return UPLOADING;
        }
        for (DataStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid data status code: " + code);
    }
}