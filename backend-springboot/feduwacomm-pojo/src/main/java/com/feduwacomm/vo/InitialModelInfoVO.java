package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 初始模型信息响应VO
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitialModelInfoVO {
    
    /**
     * 模型ID
     */
    private String id;
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 模型类型
     */
    private String modelType;
    
    /**
     * 生成方式
     */
    private String generationMethod;
    
    /**
     * 模型大小(字节)
     */
    private Long modelSize;
    
    /**
     * 模型大小(格式化显示)
     */
    private String modelSizeFormatted;
    
    /**
     * 架构参数
     */
    private Map<String, Object> architectureParams;
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 状态描述
     */
    private String statusDescription;
    
    /**
     * 文件路径
     */
    private String filePath;
    
    /**
     * 校验和
     */
    private String checksum;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 创建者
     */
    private String createdBy;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 分发状态统计
     */
    private DistributionStats distributionStats;
    
    /**
     * 分发状态统计内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistributionStats {
        /**
         * 总分发数量
         */
        private Integer total;
        
        /**
         * 已完成数量
         */
        private Integer completed;
        
        /**
         * 进行中数量
         */
        private Integer inProgress;
        
        /**
         * 失败数量
         */
        private Integer failed;
        
        /**
         * 分发进度百分比
         */
        private Double progressPercentage;
    }
}