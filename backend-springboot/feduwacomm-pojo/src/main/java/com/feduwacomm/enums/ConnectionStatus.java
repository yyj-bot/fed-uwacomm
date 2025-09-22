package com.feduwacomm.enums;

/**
 * 连接状态枚举
 */
public enum ConnectionStatus {
    DISCONNECTED("DISCONNECTED", "未连接"),
    CONNECTED("CONNECTED", "已连接"),
    CONNECTING("CONNECTING", "连接中"),
    RECONNECTING("RECONNECTING", "重连中");

    private final String code;
    private final String description;

    ConnectionStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ConnectionStatus fromCode(String code) {
        if (code == null) {
            return DISCONNECTED;
        }
        for (ConnectionStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid connection status code: " + code);
    }
}