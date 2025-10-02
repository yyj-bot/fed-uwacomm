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
    /**
     * v1.5.1.1 数据分配千分比权重
     *
     * <p>取值范围：1-1000的正整数
     * <p>约束条件：同一联邦学习任务的所有参与者的dataRatio之和必须等于1000
     * <p>语义说明：dataRatio表示该VM占用数据集的千分比
     *
     * <p>示例：
     * <ul>
     *   <li>VM1.dataRatio=700, VM2.dataRatio=200, VM3.dataRatio=100 (7:2:1比例)</li>
     *   <li>VM1占用70%数据(700/1000), VM2占用20%(200/1000), VM3占用10%(100/1000)</li>
     *   <li>总和验证: 700+200+100=1000 ✓</li>
     * </ul>
     *
     * <p>如果为null，系统会根据参与者数量自动分配平均权重（如3个VM各334、333、333）
     */
    private Integer dataRatio;
    private String capabilities; // JSON格式存储能力列表
    private Integer maxCpuUsage;
    private Integer maxMemoryUsage;

    // v1.5 新增字段 - assignedDatasetId支持
    private String assignedDatasetId; // 分配给此参与者的数据集ID
    private String datasetStatus; // 数据集状态: PENDING, CREATING, CREATED, FAILED
    private String localPath; // 数据集在VM上的本地路径
    private LocalDateTime datasetCreatedAt; // 数据集创建时间
    private LocalDateTime datasetCompletedAt; // 数据集完成时间

    // 系统字段
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
    
    // 配置和结果JSON存储
    private String parameters; // 参与者参数配置
}