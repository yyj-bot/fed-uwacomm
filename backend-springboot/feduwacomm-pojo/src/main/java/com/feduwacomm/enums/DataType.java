package com.feduwacomm.enums;

/**
 * 数据类型枚举
 */
public enum DataType {
    ACOUSTIC("ACOUSTIC", "水声数据"),
    ENVIRONMENT("ENVIRONMENT", "环境数据"),
    MODEL("MODEL", "模型数据"),
    OTHER("OTHER", "其他数据");

    private final String code;
    private final String description;

    DataType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static DataType fromCode(String code) {
        if (code == null) {
            return OTHER;
        }
        for (DataType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid data type code: " + code);
    }
}