package com.feduwacomm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 联邦学习算法枚举
 */
public enum FederatedAlgorithm {
    FEDAVG("FEDAVG", "联邦平均算法"),
    FEDPROX("FEDPROX", "联邦近端算法"),
    FEDNOVA("FEDNOVA", "联邦Nova算法"),
    SCAFFOLD("SCAFFOLD", "SCAFFOLD算法");

    private final String code;
    private final String description;

    FederatedAlgorithm(String code, String description) {
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

    public static FederatedAlgorithm fromCode(String code) {
        if (code == null) {
            return FEDAVG;
        }
        for (FederatedAlgorithm algorithm : values()) {
            if (algorithm.code.equals(code)) {
                return algorithm;
            }
        }
        throw new IllegalArgumentException("Invalid federated algorithm code: " + code);
    }
}