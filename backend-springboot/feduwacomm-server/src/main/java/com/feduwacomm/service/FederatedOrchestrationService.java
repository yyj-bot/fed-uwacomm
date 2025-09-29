package com.feduwacomm.service;

import com.feduwacomm.entity.OrchestrationWorkflow;

/**
 * 联邦学习工作流编排服务接口
 */
public interface FederatedOrchestrationService {

    /**
     * 创建工作流实例
     *
     * @param taskId 任务ID
     * @param createdBy 创建者
     * @return 工作流实例
     */
    OrchestrationWorkflow createWorkflow(String taskId, String createdBy);

    /**
     * 启动工作流执行
     *
     * @param workflow 工作流实例
     */
    void startWorkflowExecution(OrchestrationWorkflow workflow);

    /**
     * 根据任务ID获取工作流
     *
     * @param taskId 任务ID
     * @return 工作流实例
     */
    OrchestrationWorkflow getWorkflowByTaskId(String taskId);

    /**
     * 暂停工作流
     *
     * @param orchestrationId 工作流ID
     */
    void pauseWorkflow(String orchestrationId);

    /**
     * 恢复工作流
     *
     * @param orchestrationId 工作流ID
     */
    void resumeWorkflow(String orchestrationId);

    /**
     * 终止工作流
     *
     * @param orchestrationId 工作流ID
     */
    void terminateWorkflow(String orchestrationId);

    /**
     * 触发下一个阶段
     *
     * @param orchestrationId 工作流ID
     * @param triggeredBy 触发者
     * @param context 上下文信息
     */
    void triggerNextStage(String orchestrationId, String triggeredBy,
                         java.util.Map<String, Object> context);

    /**
     * 终止编排
     *
     * @param orchestrationId 工作流ID
     * @param reason 终止原因
     */
    void terminateOrchestration(String orchestrationId, String reason);

    /**
     * 回滚到指定阶段
     *
     * @param taskId 任务ID
     * @param targetStage 目标阶段
     * @param reason 回滚原因
     */
    void rollbackToStage(String taskId, String targetStage, String reason);

    /**
     * 更新阶段状态
     *
     * @param orchestrationId 工作流ID
     * @param stageName 阶段名称
     * @param status 状态
     * @return 更新是否成功
     */
    boolean updateStageStatus(String orchestrationId, String stageName, String status);

    /**
     * 更新工作流编排状态
     *
     * @param orchestrationId 工作流ID
     * @param status 状态
     * @return 更新是否成功
     */
    boolean updateOrchestrationStatus(String orchestrationId, String status);

    /**
     * 触发指定阶段
     *
     * @param orchestrationId 工作流ID
     * @param stageName 阶段名称
     * @param input 输入数据
     * @return 触发是否成功
     */
    boolean triggerStage(String orchestrationId, String stageName, java.util.Map<String, Object> input);
}