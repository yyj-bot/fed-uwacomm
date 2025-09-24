package com.feduwacomm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 聚合方法枚举
 */
public enum AggregationMethod {
    FEDERATED_AVERAGING("FEDERATED_AVERAGING", "联邦平均"),
    FEDERATED_PROXIMAL("FEDERATED_PROXIMAL", "联邦近端"),
    FEDERATED_NOVA("FEDERATED_NOVA", "联邦Nova"),
    FEDERATED_SCAFFOLD("FEDERATED_SCAFFOLD", "SCAFFOLD");

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
            return FEDERATED_AVERAGING;
        }
        for (AggregationMethod method : values()) {
            if (method.code.equals(code)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Invalid aggregation method code: " + code);
    }
}