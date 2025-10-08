package com.feduwacomm.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 数据切片策略枚举
 * 对应数据库 data_distributions 表的 strategy 字段
 */
public enum SlicingStrategy {
    /**
     * 随机分配策略
     */
    RANDOM("RANDOM", "随机分配"),

    /**
     * 平衡分配策略
     */
    BALANCED("BALANCED", "平衡分配"),

    /**
     * 自定义分配策略
     */
    CUSTOM("CUSTOM", "自定义分配"),

    /**
     * 轮询分配策略
     */
    ROUND_ROBIN("ROUND_ROBIN", "轮询分配");

    private final String value;
    private final String description;

    SlicingStrategy(String value, String description) {
        this.value = value;
        this.description = description;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 从字符串值创建枚举实例
     * @param value 字符串值
     * @return 对应的枚举实例
     */
    @JsonCreator
    public static SlicingStrategy fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (SlicingStrategy strategy : SlicingStrategy.values()) {
            if (strategy.value.equalsIgnoreCase(value)) {
                return strategy;
            }
        }
        throw new IllegalArgumentException("未知的切片策略: " + value);
    }

    @Override
    public String toString() {
        return this.value;
    }
}