package com.feduwacomm.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 数据分发完成事件
 * 当数据分发任务完成（成功或失败）时触发此事件
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataDistributionCompletedEvent {
    
    /**
     * 分发任务ID
     */
    private String distributionId;
    
    /**
     * 关联的联邦学习任务ID
     */
    private String taskId;
    
    /**
     * 分发是否成功
     */
    private boolean success;
    
    /**
     * 分发策略
     */
    private String distributionStrategy;
    
    /**
     * 成功分发的虚拟机数量
     */
    private Integer successfulVmCount;
    
    /**
     * 失败的虚拟机数量
     */
    private Integer failedVmCount;
    
    /**
     * 总执行时间（毫秒）
     */
    private Long executionDuration;
    
    /**
     * 分发的总数据量
     */
    private Long totalDataSize;
    
    /**
     * 错误消息（如果失败）
     */
    private String errorMessage;
    
    /**
     * 分发统计信息
     */
    private Map<String, Object> distributionStatistics;
    
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
     * 简化构造函数
     */
    public DataDistributionCompletedEvent(String distributionId, String taskId, boolean success, String operatorId) {
        this.distributionId = distributionId;
        this.taskId = taskId;
        this.success = success;
        this.operatorId = operatorId;
        this.eventTime = LocalDateTime.now();
    }
    
    /**
     * 检查是否有下一阶段需要执行
     */
    public boolean hasNextStage() {
        return success; // 成功完成数据分发后，通常需要进行模型分发
    }
    
    /**
     * 获取下一阶段名称
     */
    public String getNextStageName() {
        if (success) {
            return "MODEL_DISTRIBUTION";
        }
        return null;
    }
}