package com.feduwacomm.orchestration;

import lombok.Getter;

/**
 * 联邦学习工作流阶段枚举
 */
@Getter
public enum WorkflowStage {
    INITIALIZATION("初始化", 0),
    INITIAL_MODEL_GENERATION("初始模型生成", 1),
    DATA_DISTRIBUTION("数据分发", 2),
    MODEL_DISTRIBUTION("模型分发", 3),
    FEDERATED_TRAINING("联邦训练", 4),
    FINAL_AGGREGATION("最终聚合", 5),
    COMPLETED("完成", 6);

    private final String description;
    private final int order;

    WorkflowStage(String description, int order) {
        this.description = description;
        this.order = order;
    }

    /**
     * 获取下一个阶段
     */
    public WorkflowStage getNext() {
        WorkflowStage[] stages = values();
        for (int i = 0; i < stages.length - 1; i++) {
            if (stages[i] == this) {
                return stages[i + 1];
            }
        }
        return this; // 如果是最后一个阶段，返回自身
    }

    /**
     * 是否可以转换到目标阶段
     */
    public boolean canTransitionTo(WorkflowStage target) {
        if (target == null) {
            return false;
        }
        // 只能向前转换，且只能转换到相邻的下一阶段
        return target.getOrder() == this.getOrder() + 1;
    }

    /**
     * 是否为最终阶段
     */
    public boolean isFinal() {
        return this == COMPLETED;
    }
}