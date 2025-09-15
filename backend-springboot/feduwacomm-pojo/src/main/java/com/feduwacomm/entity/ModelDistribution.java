package com.feduwacomm.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 模型分发记录实体类
 * 对应数据库表: model_distributions
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelDistribution {
    
    /**
     * 分发记录唯一标识(32位UUID)
     */
    private String id;
    
    /**
     * 模型ID(32位UUID)
     */
    private String modelId;
    
    /**
     * 虚拟机ID(32位UUID)
     */
    private String vmId;
    
    /**
     * 分发状态: PENDING-待分发, IN_PROGRESS-分发中, COMPLETED-已完成, FAILED-失败
     */
    private String distributionStatus;
    
    /**
     * 分发时间
     */
    private LocalDateTime distributedAt;
    
    /**
     * 验证时间
     */
    private LocalDateTime verifiedAt;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 校验和验证状态
     */
    private Boolean checksumVerified;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}