package com.feduwacomm.entity;

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
    private String algorithm;
    private String status;
    
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
    private Double progress;
    private Integer estimatedDuration;
    
    // 配置JSON存储
    private String config;
    
    // 任务结果
    private String finalResults;
    private String modelInfo;
}