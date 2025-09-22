package com.feduwacomm.enums;

/**
 * 参与者角色枚举
 */
public enum ParticipantRole {
    PARTICIPANT("PARTICIPANT", "参与者"),
    COORDINATOR("COORDINATOR", "协调者");

    private final String code;
    private final String description;

    ParticipantRole(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ParticipantRole fromCode(String code) {
        if (code == null) {
            return PARTICIPANT;
        }
        for (ParticipantRole role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Invalid participant role code: " + code);
    }
}