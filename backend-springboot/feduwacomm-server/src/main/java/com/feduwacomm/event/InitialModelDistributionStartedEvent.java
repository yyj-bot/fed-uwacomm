package com.feduwacomm.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 初始模型分发开始事件
 * 当初始模型开始分发到虚拟机时发布此事件
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Getter
public class InitialModelDistributionStartedEvent extends ApplicationEvent {
    
    /**
     * 模型ID
     */
    private final String modelId;
    
    /**
     * 任务ID
     */
    private final String taskId;
    
    /**
     * 编排ID（如果有）
     */
    private final String orchestrationId;
    
    /**
     * 模型类型
     */
    private final String modelType;
    
    /**
     * 目标虚拟机ID列表
     */
    private final List<String> targetVmIds;
    
    /**
     * 分发配置
     */
    private final DistributionConfig config;
    
    /**
     * 事件发生时间
     */
    private final LocalDateTime eventTime;
    
    public InitialModelDistributionStartedEvent(Object source, String modelId, String taskId, 
                                              String orchestrationId, String modelType, 
                                              List<String> targetVmIds, DistributionConfig config) {
        super(source);
        this.modelId = modelId;
        this.taskId = taskId;
        this.orchestrationId = orchestrationId;
        this.modelType = modelType;
        this.targetVmIds = targetVmIds;
        this.config = config;
        this.eventTime = LocalDateTime.now();
    }
    
    /**
     * 分发配置
     */
    @Getter
    public static class DistributionConfig {
        private final boolean verifyIntegrity;
        private final int timeoutSeconds;
        private final int maxRetries;
        
        public DistributionConfig(boolean verifyIntegrity, int timeoutSeconds, int maxRetries) {
            this.verifyIntegrity = verifyIntegrity;
            this.timeoutSeconds = timeoutSeconds;
            this.maxRetries = maxRetries;
        }
    }
    
    @Override
    public String toString() {
        return String.format("InitialModelDistributionStartedEvent{modelId='%s', taskId='%s', targetVmCount=%d, eventTime=%s}", 
                modelId, taskId, targetVmIds != null ? targetVmIds.size() : 0, eventTime);
    }
}