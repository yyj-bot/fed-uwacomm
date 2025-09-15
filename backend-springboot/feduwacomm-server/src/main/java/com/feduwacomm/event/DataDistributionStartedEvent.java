package com.feduwacomm.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 数据分发开始事件
 * 当数据分发任务开始执行时触发此事件
 * 
 * @author FedUWAComm Team  
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataDistributionStartedEvent {
    
    /**
     * 分发任务ID
     */
    private String distributionId;
    
    /**
     * 关联的联邦学习任务ID
     */
    private String taskId;
    
    /**
     * 分发策略
     */
    private String distributionStrategy;
    
    /**
     * 目标虚拟机ID列表
     */
    private String[] targetVmIds;
    
    /**
     * 数据集ID列表
     */
    private String[] datasetIds;
    
    /**
     * 分发配置参数
     */
    private Map<String, Object> configParams;
    
    /**
     * 创建用户ID
     */
    private String operatorId;
    
    /**
     * 事件时间
     */
    @Builder.Default
    private LocalDateTime eventTime = LocalDateTime.now();
    
    /**
     * 预期总数据量
     */
    private Long totalDataSize;
    
    /**
     * 预期分发虚拟机数量
     */
    private Integer expectedVmCount;
    
    public DataDistributionStartedEvent(String distributionId, String taskId, String distributionStrategy, String operatorId) {
        this.distributionId = distributionId;
        this.taskId = taskId;
        this.distributionStrategy = distributionStrategy;
        this.operatorId = operatorId;
        this.eventTime = LocalDateTime.now();
    }
}