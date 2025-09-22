package com.feduwacomm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 聚合方法枚举
 */
public enum AggregationMethod {
    FEDAVG("FEDAVG", "联邦平均"),
    FEDPROX("FEDPROX", "联邦近端"),
    FEDNOVA("FEDNOVA", "联邦Nova"),
    SCAFFOLD("SCAFFOLD", "SCAFFOLD");

    private final String code;
    private final String description;

    AggregationMethod(String code, String description) {
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

    public static AggregationMethod fromCode(String code) {
        if (code == null) {
            return FEDAVG;
        }
        for (AggregationMethod method : values()) {
            if (method.code.equals(code)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Invalid aggregation method code: " + code);
    }
}