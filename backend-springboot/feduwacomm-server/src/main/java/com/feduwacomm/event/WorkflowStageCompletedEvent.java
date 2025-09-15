package com.feduwacomm.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 工作流阶段完成事件
 * 当工作流某个阶段完成执行时发布此事件
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Getter
public class WorkflowStageCompletedEvent extends ApplicationEvent {
    
    /**
     * 编排ID
     */
    private final String orchestrationId;
    
    /**
     * 任务ID
     */
    private final String taskId;
    
    /**
     * 阶段名称
     */
    private final String stageName;
    
    /**
     * 阶段顺序
     */
    private final int stageOrder;
    
    /**
     * 是否成功
     */
    private final boolean success;
    
    /**
     * 阶段输出数据
     */
    private final Map<String, Object> outputData;
    
    /**
     * 执行耗时（毫秒）
     */
    private final long executionDuration;
    
    /**
     * 错误信息（如果失败）
     */
    private final String errorMessage;
    
    /**
     * 事件发生时间
     */
    private final LocalDateTime eventTime;
    
    /**
     * 下一个阶段名称（如果有）
     */
    private final String nextStageName;
    
    public WorkflowStageCompletedEvent(Object source, String orchestrationId, String taskId, 
                                     String stageName, int stageOrder, boolean success,
                                     Map<String, Object> outputData, long executionDuration,
                                     String errorMessage, String nextStageName) {
        super(source);
        this.orchestrationId = orchestrationId;
        this.taskId = taskId;
        this.stageName = stageName;
        this.stageOrder = stageOrder;
        this.success = success;
        this.outputData = outputData;
        this.executionDuration = executionDuration;
        this.errorMessage = errorMessage;
        this.nextStageName = nextStageName;
        this.eventTime = LocalDateTime.now();
    }
    
    /**
     * 创建成功完成事件
     */
    public static WorkflowStageCompletedEvent success(Object source, String orchestrationId, String taskId,
                                                    String stageName, int stageOrder, Map<String, Object> outputData,
                                                    long executionDuration, String nextStageName) {
        return new WorkflowStageCompletedEvent(source, orchestrationId, taskId, stageName, stageOrder, true,
                outputData, executionDuration, null, nextStageName);
    }
    
    /**
     * 创建失败完成事件
     */
    public static WorkflowStageCompletedEvent failure(Object source, String orchestrationId, String taskId,
                                                    String stageName, int stageOrder, String errorMessage,
                                                    long executionDuration) {
        return new WorkflowStageCompletedEvent(source, orchestrationId, taskId, stageName, stageOrder, false,
                null, executionDuration, errorMessage, null);
    }
    
    /**
     * 是否有下一个阶段
     */
    public boolean hasNextStage() {
        return nextStageName != null && !nextStageName.trim().isEmpty();
    }
    
    @Override
    public String toString() {
        return String.format("WorkflowStageCompletedEvent{orchestrationId='%s', taskId='%s', stage='%s', " +
                        "order=%d, success=%s, duration=%dms, hasNext=%s, eventTime=%s}", 
                orchestrationId, taskId, stageName, stageOrder, success, executionDuration, 
                hasNextStage(), eventTime);
    }
}