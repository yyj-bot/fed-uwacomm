package com.feduwacomm.entity;

import com.feduwacomm.enums.ParticipantRole;
import com.feduwacomm.enums.ParticipantStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 联邦学习任务参与者实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskParticipant {

    private String id;
    private String taskId;
    private String vmId;
    private ParticipantRole role; // PARTICIPANT
    private ParticipantStatus status; // CONNECTED, DISCONNECTED, TRAINING, COMPLETED, FAILED
    private String dataSource;
    
    // 训练状态
    private Integer currentEpoch;
    private Double loss;
    private Double accuracy;
    private LocalDateTime lastHeartbeat;
    
    // 训练结果
    private Double finalAccuracy;
    private Double finalLoss;
    private Long trainingTime; // 训练时间(秒)
    private Integer dataSize; // 数据量大小

    // v1.3 新增字段
    private String participantId;
    private Double dataRatio;
    private String capabilities; // JSON格式存储能力列表
    private Integer maxCpuUsage;
    private Integer maxMemoryUsage;

    // v1.5 新增字段 - assignedDatasetId支持
    private String assignedDatasetId; // 分配给此参与者的数据集ID
    private String datasetStatus; // 数据集状态: PENDING, CREATING, CREATED, FAILED
    private String localPath; // 数据集在VM上的本地路径
    private LocalDateTime datasetCreatedAt; // 数据集创建时间

    // 系统字段
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
    
    // 配置和结果JSON存储
    private String parameters; // 参与者参数配置
}