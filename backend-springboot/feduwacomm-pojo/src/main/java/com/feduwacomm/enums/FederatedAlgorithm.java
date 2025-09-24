package com.feduwacomm.enums;

/**
 * 联邦学习算法枚举
 */
public enum FederatedAlgorithm {
    FEDERATED_AVERAGING("FEDERATED_AVERAGING", "联邦平均算法"),
    FEDERATED_PROXIMAL("FEDERATED_PROXIMAL", "联邦近端算法"),
    FEDERATED_NOVA("FEDERATED_NOVA", "联邦Nova算法"),
    FEDERATED_SCAFFOLD("FEDERATED_SCAFFOLD", "SCAFFOLD算法");

    private final String code;
    private final String description;

    FederatedAlgorithm(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static FederatedAlgorithm fromCode(String code) {
        if (code == null) {
            return FEDERATED_AVERAGING;
        }

        try {
            return valueOf(code.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid federated algorithm code: " + code);
        }
    }
}