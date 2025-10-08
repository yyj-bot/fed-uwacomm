package com.feduwacomm.enums;

import lombok.Getter;

/**
 * 验证决策枚举 (v1.5.1)
 * 定义数据完整性验证失败时的处理策略
 *
 * @author FedUWAComm Team
 * @version 1.5.1
 * @since 2025-09-30
 */
@Getter
public enum VerificationDecision {
    /**
     * 接受：数据完整，继续训练
     */
    ACCEPT("ACCEPT", "接受数据，继续训练"),

    /**
     * 重传缺失数据：缺失率较低，重新传输缺失部分
     */
    RETRY_MISSING("RETRY_MISSING", "重传缺失数据"),

    /**
     * 排除该VM：缺失率较高，从训练中排除该VM
     */
    EXCLUDE_VM("EXCLUDE_VM", "排除该虚拟机"),

    /**
     * 中止任务：严重错误，中止整个训练任务
     */
    ABORT_TASK("ABORT_TASK", "中止训练任务");

    /**
     * 决策代码
     */
    private final String code;

    /**
     * 决策描述
     */
    private final String description;

    VerificationDecision(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据代码获取决策
     *
     * @param code 决策代码
     * @return 决策枚举，如果不存在返回null
     */
    public static VerificationDecision fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }

        for (VerificationDecision decision : values()) {
            if (decision.code.equals(code)) {
                return decision;
            }
        }

        return null;
    }

    /**
     * 根据缺失率自动决策
     *
     * @param missingRate 缺失率（0.0-1.0）
     * @return 推荐的决策
     */
    public static VerificationDecision fromMissingRate(double missingRate) {
        if (missingRate == 0.0) {
            return ACCEPT;
        } else if (missingRate < 0.05) {  // 缺失率 < 5%
            return RETRY_MISSING;
        } else if (missingRate < 0.20) {  // 缺失率 < 20%
            return EXCLUDE_VM;
        } else {
            return ABORT_TASK;
        }
    }
}