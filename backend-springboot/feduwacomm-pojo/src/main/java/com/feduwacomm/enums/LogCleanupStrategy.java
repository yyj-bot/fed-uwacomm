package com.feduwacomm.enums;

public enum LogCleanupStrategy {
    TIME_BASED("TIME_BASED", "基于时间的清理策略"),
    LEVEL_BASED("LEVEL_BASED", "基于日志级别的清理策略"),
    CATEGORY_BASED("CATEGORY_BASED", "基于分类的清理策略");

    private final String code;
    private final String description;

    LogCleanupStrategy(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static LogCleanupStrategy fromCode(String code) {
        if (code == null) {
            return TIME_BASED; // 默认时间策略
        }
        for (LogCleanupStrategy strategy : values()) {
            if (strategy.code.equals(code)) {
                return strategy;
            }
        }
        return TIME_BASED; // 无效代码时返回默认策略
    }
}