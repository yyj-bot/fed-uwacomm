package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 数据分发任务响应VO
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataDistributionTaskVO {
    
    /**
     * 分发任务ID
     */
    private String distributionId;
    
    /**
     * 关联的任务ID
     */
    private String taskId;
    
    /**
     * 分发名称
     */
    private String distributionName;
    
    /**
     * 数据类型
     */
    private String dataType;
    
    /**
     * 分发策略
     */
    private String distributionStrategy;
    
    /**
     * 分发状态: CREATED, PROCESSING, COMPLETED, FAILED, STOPPED
     */
    private String status;
    
    /**
     * 分发进度 (0-100)
     */
    private Double progress;
    
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
     * 创建用户ID
     */
    private String createdBy;
    
    /**
     * 总数据量
     */
    private Long totalDataSize;
    
    /**
     * 总记录数
     */
    private Long totalRecords;
    
    /**
     * 目标虚拟机数量
     */
    private Integer targetVmCount;
    
    /**
     * 成功分发的虚拟机数量
     */
    private Integer successVmCount;
    
    /**
     * 失败的虚拟机数量
     */
    private Integer failedVmCount;
    
    /**
     * 错误消息
     */
    private String errorMessage;
    
    /**
     * 配置参数
     */
    private Map<String, Object> configParams;
    
    /**
     * 虚拟机分发详情
     */
    private List<VmDataInfo> vmDataDetails;
    
    /**
     * 分发统计信息
     */
    private DistributionStatistics statistics;
    
    /**
     * 数据平衡性分析
     */
    private DataBalanceAnalysis balanceAnalysis;
    
    /**
     * 质量评估
     */
    private QualityAssessment qualityAssessment;
    
    /**
     * 虚拟机数据信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VmDataInfo {
        
        /**
         * 虚拟机ID
         */
        private String vmId;
        
        /**
         * 虚拟机名称
         */
        private String vmName;
        
        /**
         * 分发状态
         */
        private String status;
        
        /**
         * 数据大小
         */
        private Long dataSize;
        
        /**
         * 记录数量
         */
        private Long recordCount;
        
        /**
         * 数据分片索引
         */
        private Integer shardIndex;
        
        /**
         * 数据开始位置
         */
        private Long startPosition;
        
        /**
         * 数据结束位置
         */
        private Long endPosition;
        
        /**
         * 数据文件路径
         */
        private String dataFilePath;
        
        /**
         * 校验和
         */
        private String checksum;
        
        /**
         * 分发开始时间
         */
        private LocalDateTime distributionStartTime;
        
        /**
         * 分发完成时间
         */
        private LocalDateTime distributionCompletedTime;
        
        /**
         * 错误信息
         */
        private String errorMessage;
        
        /**
         * 重试次数
         */
        private Integer retryCount;
        
        /**
         * 数据类别分布
         */
        private Map<String, Integer> categoryDistribution;
        
        /**
         * 数据特征统计
         */
        private Map<String, Double> featureStatistics;
    }
    
    /**
     * 分发统计信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistributionStatistics {
        
        /**
         * 总执行时间（毫秒）
         */
        private Long totalExecutionTime;
        
        /**
         * 平均每个VM分发时间
         */
        private Double avgDistributionTime;
        
        /**
         * 数据传输总量
         */
        private Long totalDataTransferred;
        
        /**
         * 平均传输速率（MB/s）
         */
        private Double avgTransferRate;
        
        /**
         * 成功率
         */
        private Double successRate;
        
        /**
         * 重试次数统计
         */
        private Map<String, Integer> retryStatistics;
        
        /**
         * 错误分布统计
         */
        private Map<String, Integer> errorDistribution;
        
        /**
         * 每小时处理量
         */
        private Double throughputPerHour;
    }
    
    /**
     * 数据平衡性分析
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataBalanceAnalysis {
        
        /**
         * 数据分布是否平衡
         */
        private Boolean isBalanced;
        
        /**
         * 平衡度评分 (0-100)
         */
        private Double balanceScore;
        
        /**
         * 最大数据量差异百分比
         */
        private Double maxVariancePercentage;
        
        /**
         * 标准差
         */
        private Double standardDeviation;
        
        /**
         * 基尼系数（数据分布不均匀程度）
         */
        private Double giniCoefficient;
        
        /**
         * 各VM数据量分布
         */
        private Map<String, Long> vmDataSizeDistribution;
        
        /**
         * 各VM记录数分布
         */
        private Map<String, Long> vmRecordCountDistribution;
        
        /**
         * 类别平衡性分析
         */
        private Map<String, CategoryBalance> categoryBalanceMap;
        
        /**
         * 改进建议
         */
        private List<String> improvementSuggestions;
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class CategoryBalance {
            private String categoryName;
            private Integer totalCount;
            private Double balanceScore;
            private Map<String, Integer> vmDistribution;
        }
    }
    
    /**
     * 质量评估
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualityAssessment {
        
        /**
         * 整体质量评分 (0-100)
         */
        private Double overallScore;
        
        /**
         * 数据完整性评分
         */
        private Double integrityScore;
        
        /**
         * 数据一致性评分
         */
        private Double consistencyScore;
        
        /**
         * 分发效率评分
         */
        private Double efficiencyScore;
        
        /**
         * 可用性评分
         */
        private Double availabilityScore;
        
        /**
         * 发现的问题列表
         */
        private List<QualityIssue> issues;
        
        /**
         * 推荐的优化措施
         */
        private List<String> recommendations;
        
        /**
         * 质量检查时间
         */
        private LocalDateTime assessmentTime;
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class QualityIssue {
            private String issueType;
            private String description;
            private String severity; // LOW, MEDIUM, HIGH, CRITICAL
            private String vmId;
            private String affectedData;
            private String suggestedAction;
        }
    }
}