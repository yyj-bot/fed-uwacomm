package com.feduwacomm.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 工作流阶段执行记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStageExecution {

    /**
     * 阶段执行记录唯一标识
     */
    private String id;

    /**
     * 工作流ID
     */
    private String orchestrationId;

    /**
     * 阶段名称
     */
    private String stageName;

    /**
     * 执行状态
     */
    private StageExecutionStatus status;

    /**
     * 开始时间
     */
    private LocalDateTime startedAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 输入数据(JSON格式)
     */
    private String inputData;

    /**
     * 输出数据(JSON格式)
     */
    private String outputData;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 阶段执行状态枚举
     */
    public enum StageExecutionStatus {
        PENDING("待执行"),
        IN_PROGRESS("执行中"),
        COMPLETED("已完成"),
        FAILED("失败"),
        SKIPPED("已跳过");

        private final String description;

        StageExecutionStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}