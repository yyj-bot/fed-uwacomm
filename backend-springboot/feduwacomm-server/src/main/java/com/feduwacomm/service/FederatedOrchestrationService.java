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
}