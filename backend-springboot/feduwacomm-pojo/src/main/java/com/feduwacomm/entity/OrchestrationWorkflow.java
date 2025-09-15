package com.feduwacomm.entity;

import com.feduwacomm.orchestration.WorkflowStage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 工作流编排实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrchestrationWorkflow {

    /**
     * 工作流唯一标识
     */
    private String id;

    /**
     * 关联任务ID
     */
    private String taskId;

    /**
     * 工作流名称
     */
    private String workflowName;

    /**
     * 工作流状态
     */
    private WorkflowStatus status;

    /**
     * 当前阶段
     */
    private WorkflowStage currentStage;

    /**
     * 工作流配置(JSON格式)
     */
    private String config;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 开始时间
     */
    private LocalDateTime startedAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 创建者ID
     */
    private String createdBy;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 工作流状态枚举
     */
    public enum WorkflowStatus {
        CREATED("已创建"),
        IN_PROGRESS("执行中"),
        PAUSED("暂停"),
        COMPLETED("已完成"),
        FAILED("失败"),
        TERMINATED("已终止");

        private final String description;

        WorkflowStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}