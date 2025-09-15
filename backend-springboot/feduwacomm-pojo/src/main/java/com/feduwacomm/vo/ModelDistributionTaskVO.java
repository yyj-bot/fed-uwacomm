package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 模型分发任务响应VO
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelDistributionTaskVO {
    
    /**
     * 分发任务ID
     */
    private String taskId;
    
    /**
     * 模型ID
     */
    private String modelId;
    
    /**
     * 模型类型
     */
    private String modelType;
    
    /**
     * 目标虚拟机数量
     */
    private Integer targetVmCount;
    
    /**
     * 分发状态统计
     */
    private DistributionStats stats;
    
    /**
     * 分发详情列表
     */
    private List<DistributionDetail> details;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 估计完成时间
     */
    private LocalDateTime estimatedCompletionTime;
    
    /**
     * 分发状态统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistributionStats {
        /**
         * 总数量
         */
        private Integer total;
        
        /**
         * 待分发数量
         */
        private Integer pending;
        
        /**
         * 进行中数量
         */
        private Integer inProgress;
        
        /**
         * 已完成数量
         */
        private Integer completed;
        
        /**
         * 失败数量
         */
        private Integer failed;
        
        /**
         * 进度百分比
         */
        private Double progressPercentage;
    }
    
    /**
     * 分发详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistributionDetail {
        /**
         * 虚拟机ID
         */
        private String vmId;
        
        /**
         * 虚拟机名称
         */
        private String vmName;
        
        /**
         * 虚拟机IP
         */
        private String vmIp;
        
        /**
         * 分发状态
         */
        private String status;
        
        /**
         * 状态描述
         */
        private String statusDescription;
        
        /**
         * 分发时间
         */
        private LocalDateTime distributedAt;
        
        /**
         * 验证时间
         */
        private LocalDateTime verifiedAt;
        
        /**
         * 是否校验和验证通过
         */
        private Boolean checksumVerified;
        
        /**
         * 错误信息
         */
        private String errorMessage;
    }
}