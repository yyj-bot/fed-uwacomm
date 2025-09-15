package com.feduwacomm.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 工作流阶段开始事件
 * 当工作流某个阶段开始执行时发布此事件
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Getter
public class WorkflowStageStartedEvent extends ApplicationEvent {
    
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
     * 阶段输入数据
     */
    private final Map<String, Object> inputData;
    
    /**
     * 阶段配置
     */
    private final Map<String, Object> stageConfig;
    
    /**
     * 事件发生时间
     */
    private final LocalDateTime eventTime;
    
    /**
     * 操作者
     */
    private final String operator;
    
    public WorkflowStageStartedEvent(Object source, String orchestrationId, String taskId, 
                                   String stageName, int stageOrder, Map<String, Object> inputData,
                                   Map<String, Object> stageConfig, String operator) {
        super(source);
        this.orchestrationId = orchestrationId;
        this.taskId = taskId;
        this.stageName = stageName;
        this.stageOrder = stageOrder;
        this.inputData = inputData;
        this.stageConfig = stageConfig;
        this.operator = operator;
        this.eventTime = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return String.format("WorkflowStageStartedEvent{orchestrationId='%s', taskId='%s', stage='%s', order=%d, eventTime=%s}", 
                orchestrationId, taskId, stageName, stageOrder, eventTime);
    }
}