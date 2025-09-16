package com.feduwacomm.vo;

import com.feduwacomm.entity.WorkflowStageExecution;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作流状态VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStatusVO {

    /**
     * 工作流ID
     */
    private String orchestrationId;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 工作流名称
     */
    private String workflowName;

    /**
     * 工作流状态
     */
    private String status;

    /**
     * 当前阶段
     */
    private String currentStage;

    /**
     * 当前阶段描述
     */
    private String currentStageDescription;

    /**
     * 进度百分比
     */
    private Double progressPercent;

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
     * 所有阶段执行记录
     */
    private List<WorkflowStageExecution> stages;

    /**
     * 最新阶段执行记录
     */
    private WorkflowStageExecution latestStageExecution;

    /**
     * 总阶段数
     */
    private Integer totalStages;

    /**
     * 已完成阶段数
     */
    private Integer completedStages;

    /**
     * 失败阶段数
     */
    private Integer failedStages;

    /**
     * 执行中阶段数
     */
    private Integer inProgressStages;

    /**
     * 工作流是否完成
     */
    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    /**
     * 工作流是否失败
     */
    public boolean isFailed() {
        return "FAILED".equals(status);
    }

    /**
     * 工作流是否正在执行
     */
    public boolean isInProgress() {
        return "IN_PROGRESS".equals(status);
    }

    /**
     * 工作流是否暂停
     */
    public boolean isPaused() {
        return "PAUSED".equals(status);
    }

    /**
     * 工作流是否已终止
     */
    public boolean isTerminated() {
        return "TERMINATED".equals(status);
    }
}