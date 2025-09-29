package com.feduwacomm.service;

import com.feduwacomm.orchestration.WorkflowStage;

/**
 * 工作流阶段转换服务接口
 * 负责处理工作流阶段之间的转换逻辑和状态管理
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-28
 */
public interface WorkflowStageTransitionService {

    /**
     * 触发工作流下一阶段
     *
     * @param orchestrationId 工作流编排ID
     * @param currentStage 当前阶段
     * @param context 阶段上下文数据
     * @return 是否触发成功
     */
    boolean triggerNextStage(String orchestrationId, WorkflowStage currentStage,
                           java.util.Map<String, Object> context);

    /**
     * 更新工作流阶段状态
     *
     * @param orchestrationId 工作流编排ID
     * @param stage 目标阶段
     * @param status 阶段状态
     * @param message 状态消息
     * @return 是否更新成功
     */
    boolean updateStageStatus(String orchestrationId, WorkflowStage stage,
                            String status, String message);

    /**
     * 处理工作流异常
     *
     * @param orchestrationId 工作流编排ID
     * @param currentStage 当前阶段
     * @param errorMessage 错误消息
     * @param exception 异常对象
     * @return 是否处理成功
     */
    boolean handleWorkflowException(String orchestrationId, WorkflowStage currentStage,
                                  String errorMessage, Exception exception);

    /**
     * 检查工作流是否可以继续执行
     *
     * @param orchestrationId 工作流编排ID
     * @return 是否可以继续执行
     */
    boolean canContinueWorkflow(String orchestrationId);

    /**
     * 获取工作流当前阶段
     *
     * @param orchestrationId 工作流编排ID
     * @return 当前阶段，如果工作流不存在则返回null
     */
    WorkflowStage getCurrentStage(String orchestrationId);

    /**
     * 暂停工作流执行
     *
     * @param orchestrationId 工作流编排ID
     * @param reason 暂停原因
     * @return 是否暂停成功
     */
    boolean pauseWorkflow(String orchestrationId, String reason);

    /**
     * 恢复工作流执行
     *
     * @param orchestrationId 工作流编排ID
     * @return 是否恢复成功
     */
    boolean resumeWorkflow(String orchestrationId);

    /**
     * 终止工作流执行
     *
     * @param orchestrationId 工作流编排ID
     * @param reason 终止原因
     * @return 是否终止成功
     */
    boolean terminateWorkflow(String orchestrationId, String reason);
}