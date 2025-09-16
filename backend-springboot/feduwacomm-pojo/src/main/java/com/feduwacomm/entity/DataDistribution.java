package com.feduwacomm.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 数据分发任务实体类
 * 对应数据库表: data_distributions
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataDistribution {
    
    /**
     * 数据分发任务唯一标识(32位UUID)
     */
    private String id;
    
    /**
     * 关联任务ID(32位UUID)
     */
    private String taskId;
    
    /**
     * 分发任务名称
     */
    private String distributionName;
    
    /**
     * 分发策略: RANDOM-随机, BALANCED-均衡, CUSTOM-自定义, ROUND_ROBIN-轮询
     */
    private String strategy;
    
    /**
     * 分发状态: CREATED-已创建, IN_PROGRESS-进行中, PAUSED-暂停, COMPLETED-完成, FAILED-失败, CANCELLED-取消
     */
    private String status;
    
    /**
     * 分发配置参数(JSON格式)
     */
    private String config;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 开始时间
     */
    private LocalDateTime startedAt;
    
    /**
     * 完成时间
     */
    private LocalDateTime completedAt;
    
    /**
     * 创建者ID(32位UUID)
     */
    private String createdBy;
}