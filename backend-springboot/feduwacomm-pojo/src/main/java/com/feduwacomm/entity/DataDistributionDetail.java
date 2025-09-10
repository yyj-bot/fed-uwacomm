package com.feduwacomm.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 数据分发详情实体类
 * 对应数据库表: data_distribution_details
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataDistributionDetail {
    
    /**
     * 分发详情唯一标识(32位UUID)
     */
    private String id;
    
    /**
     * 分发任务ID(32位UUID)
     */
    private String distributionId;
    
    /**
     * 数据集ID(32位UUID)
     */
    private String datasetId;
    
    /**
     * 虚拟机ID(32位UUID)
     */
    private String vmId;
    
    /**
     * 分发状态: PENDING-待分发, IN_PROGRESS-分发中, COMPLETED-已完成, FAILED-失败
     */
    private String status;
    
    /**
     * 数据大小(字节)
     */
    private Long dataSize;
    
    /**
     * 已传输大小(字节)
     */
    private Long transferredSize;
    
    /**
     * 数据校验和
     */
    private String checksum;
    
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
     * 创建时间
     */
    private LocalDateTime createdAt;
}