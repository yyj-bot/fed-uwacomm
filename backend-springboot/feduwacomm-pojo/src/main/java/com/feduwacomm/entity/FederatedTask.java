package com.feduwacomm.entity;

import com.feduwacomm.enums.FederatedAlgorithm;
import com.feduwacomm.enums.FederatedTaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 联邦学习任务实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FederatedTask {

    private String id;
    private String taskName;
    private String taskType;
    private String description;
    private FederatedAlgorithm algorithm;
    private FederatedTaskStatus status;
    
    // 超参数
    private Double learningRate;
    private Integer batchSize;
    private Integer epochs;
    private Integer totalRounds;
    private Integer currentRound;
    private Integer minParticipants;
    
    // 模型配置
    private String modelType;
    private String featureColumns;
    private String targetColumn;
    private Double testSize;
    private Integer randomState;
    
    // 调度配置
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer timeout;
    
    // 安全配置
    private String encryption;
    private Boolean differentialPrivacy;
    private Double epsilon;
    private Double delta;
    private Boolean secureAggregation;

    // v1.3 新增字段
    private String datasetId;
    private String distributionStrategy;

    // 系统字段
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime startedAt;
    private LocalDateTime pausedAt;
    private LocalDateTime resumedAt;
    private LocalDateTime stoppedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    
    private String createdBy;
    private String updatedBy;
    
    // 任务统计
    private Integer participantCount;
    private Integer estimatedDuration;
    
    // 配置JSON存储
    private String config;
    
    // 任务结果
    private String finalResults;
    private String modelInfo;

    // v1.4 新增字段 - 任务恢复信息
    private String resumeInfo;

    // 乐观锁版本号
    private Long version;

    /**
     * 计算任务进度百分比
     * @return 进度百分比 (0-100)
     */
    public double getProgress() {
        if (totalRounds == null || totalRounds <= 0) {
            return 0.0;
        }
        if (currentRound == null) {
            return 0.0;
        }
        return Math.round((currentRound * 100.0) / totalRounds * 100.0) / 100.0;
    }

    /**
     * 获取任务恢复信息
     * @return 恢复信息（JSON格式）
     */
    public String getResumeInfo() {
        return resumeInfo;
    }

    /**
     * 设置任务恢复信息
     * @param resumeInfo 恢复信息（JSON格式）
     */
    public void setResumeInfo(String resumeInfo) {
        this.resumeInfo = resumeInfo;
    }

    // v1.4新增方法 - 协议版本和生命周期管理
    private String protocolVersion = "1.4";
    private String lifecycleStatus = "CREATED";

    /**
     * 获取协议版本
     * @return 协议版本
     */
    public String getProtocolVersion() {
        return protocolVersion;
    }

    /**
     * 设置协议版本
     * @param protocolVersion 协议版本
     */
    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    /**
     * 获取生命周期状态
     * @return 生命周期状态
     */
    public String getLifecycleStatus() {
        return lifecycleStatus;
    }

    /**
     * 设置生命周期状态
     * @param lifecycleStatus 生命周期状态
     */
    public void setLifecycleStatus(String lifecycleStatus) {
        this.lifecycleStatus = lifecycleStatus;
    }

    /**
     * 设置任务ID（兼容性方法）
     * @param taskId 任务ID
     */
    public void setTaskId(String taskId) {
        this.id = taskId;
    }

    /**
     * 设置联邦算法（兼容性方法）
     * @param algorithm 联邦算法
     */
    public void setFederatedAlgorithm(String algorithm) {
        this.algorithm = FederatedAlgorithm.valueOf(algorithm);
    }
}