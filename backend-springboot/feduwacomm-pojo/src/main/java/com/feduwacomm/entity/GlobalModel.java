package com.feduwacomm.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.feduwacomm.enums.AggregationMethod;
import com.feduwacomm.enums.GlobalModelStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 全局模型实体类
 * 存储联邦学习聚合后的全局模型参数和性能指标
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalModel {

    /**
     * 唯一标识(32位UUID)
     */
    private String id;

    /**
     * 任务ID(32位UUID)
     */
    private String taskId;

    /**
     * 轮次编号
     */
    private Integer roundNumber;

    /**
     * 聚合算法类型 (FEDERATED_AVERAGING, FEDERATED_PROXIMAL, FEDERATED_NOVA等)
     */
    private AggregationMethod aggregationMethod;

    /**
     * 全局模型参数(JSON格式)
     */
    private String globalParameters;

    /**
     * 全局损失值
     */
    private BigDecimal globalLoss;

    /**
     * 全局准确率
     */
    private BigDecimal globalAccuracy;

    /**
     * 参与聚合的客户端数量
     */
    private Integer participantCount;

    /**
     * 聚合耗时(毫秒)
     */
    private Long aggregationDuration;

    /**
     * 聚合状态 (PENDING, AGGREGATING, COMPLETED, FAILED)
     */
    private GlobalModelStatus status;

    /**
     * 聚合开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime startedAt;

    /**
     * 聚合完成时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime completedAt;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime updatedAt;

    /**
     * 聚合元数据(JSON格式)
     */
    private String metadata;
}