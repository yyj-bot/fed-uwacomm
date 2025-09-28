package com.feduwacomm.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 初始模型分发完成事件
 * 当初始模型分发到虚拟机完成时发布此事件
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Getter
public class InitialModelDistributionCompletedEvent extends ApplicationEvent {
    
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
     * 分发是否成功
     */
    private final boolean success;
    
    /**
     * 总的分发任务数
     */
    private final int totalDistributions;
    
    /**
     * 成功分发的数量
     */
    private final int successfulDistributions;
    
    /**
     * 失败分发的数量
     */
    private final int failedDistributions;
    
    /**
     * 成功分发的虚拟机ID列表
     */
    private final List<String> successfulVmIds;
    
    /**
     * 失败分发的虚拟机ID列表
     */
    private final List<String> failedVmIds;
    
    /**
     * 分发统计信息
     */
    private final Map<String, Object> distributionStats;
    
    /**
     * 分发耗时（毫秒）
     */
    private final long distributionDuration;
    
    /**
     * 错误信息（如果有）
     */
    private final String errorMessage;
    
    /**
     * 事件发生时间
     */
    private final LocalDateTime eventTime;
    
    public InitialModelDistributionCompletedEvent(Object source, String modelId, String taskId, 
                                                String orchestrationId, boolean success, 
                                                int totalDistributions, int successfulDistributions, 
                                                int failedDistributions, List<String> successfulVmIds, 
                                                List<String> failedVmIds, Map<String, Object> distributionStats,
                                                long distributionDuration, String errorMessage) {
        super(source);
        this.modelId = modelId;
        this.taskId = taskId;
        this.orchestrationId = orchestrationId;
        this.success = success;
        this.totalDistributions = totalDistributions;
        this.successfulDistributions = successfulDistributions;
        this.failedDistributions = failedDistributions;
        this.successfulVmIds = successfulVmIds;
        this.failedVmIds = failedVmIds;
        this.distributionStats = distributionStats;
        this.distributionDuration = distributionDuration;
        this.errorMessage = errorMessage;
        this.eventTime = LocalDateTime.now();
    }
    
    /**
     * 创建成功事件
     */
    public static InitialModelDistributionCompletedEvent success(Object source, String modelId, String taskId,
                                                               String orchestrationId, int totalDistributions,
                                                               int successfulDistributions, int failedDistributions,
                                                               List<String> successfulVmIds, List<String> failedVmIds,
                                                               Map<String, Object> distributionStats,
                                                               long distributionDuration) {
        return new InitialModelDistributionCompletedEvent(source, modelId, taskId, orchestrationId, true,
                totalDistributions, successfulDistributions, failedDistributions, successfulVmIds, failedVmIds,
                distributionStats, distributionDuration, null);
    }
    
    /**
     * 创建失败事件
     */
    public static InitialModelDistributionCompletedEvent failure(Object source, String modelId, String taskId,
                                                               String orchestrationId, int totalDistributions,
                                                               String errorMessage, long distributionDuration) {
        return new InitialModelDistributionCompletedEvent(source, modelId, taskId, orchestrationId, false,
                totalDistributions, 0, totalDistributions, null, null, null, distributionDuration, errorMessage);
    }
    
    /**
     * 分发是否部分成功
     */
    public boolean isPartialSuccess() {
        return success && failedDistributions > 0 && successfulDistributions > 0;
    }
    
    /**
     * 分发是否完全成功
     */
    public boolean isCompleteSuccess() {
        return success && failedDistributions == 0 && successfulDistributions == totalDistributions;
    }
    
    /**
     * 获取成功率
     */
    public double getSuccessRate() {
        return totalDistributions > 0 ? (double) successfulDistributions / totalDistributions : 0.0;
    }

    /**
     * 获取成功的虚拟机数量
     */
    public int getSuccessVmCount() {
        return successfulDistributions;
    }

    /**
     * 获取失败的虚拟机数量
     */
    public int getFailedVmCount() {
        return failedDistributions;
    }

    /**
     * 获取分发持续时间（毫秒）
     */
    public long getDuration() {
        return distributionDuration;
    }
    
    @Override
    public String toString() {
        return String.format("InitialModelDistributionCompletedEvent{modelId='%s', taskId='%s', success=%s, " +
                        "total=%d, successful=%d, failed=%d, successRate=%.2f%%, duration=%dms, eventTime=%s}", 
                modelId, taskId, success, totalDistributions, successfulDistributions, failedDistributions, 
                getSuccessRate() * 100, distributionDuration, eventTime);
    }
}