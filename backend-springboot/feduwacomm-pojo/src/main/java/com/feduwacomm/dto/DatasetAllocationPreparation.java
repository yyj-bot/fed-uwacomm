package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * v1.5数据集分配准备结果
 * 用于为联邦学习任务准备数据集分配
 *
 * @author FedUWAComm Team
 * @version 1.5.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DatasetAllocationPreparation {

    /**
     * 原始数据集信息
     */
    private TrainingDataset originalDataset;

    /**
     * 数据集分配计划列表
     */
    private List<DatasetAllocationPlan> allocationPlans;

    /**
     * 准备状态
     */
    private boolean success;

    /**
     * 错误信息（如果失败）
     */
    private String errorMessage;

    /**
     * 准备时间
     */
    private LocalDateTime preparedAt;

    public DatasetAllocationPreparation(TrainingDataset originalDataset, List<DatasetAllocationPlan> allocationPlans) {
        this.originalDataset = originalDataset;
        this.allocationPlans = allocationPlans;
        this.success = true;
        this.preparedAt = LocalDateTime.now();
    }

    /**
     * 数据集分配计划
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatasetAllocationPlan {
        /**
         * 虚拟机ID
         */
        private String vmId;

        /**
         * 分配的数据集ID
         */
        private String assignedDatasetId;

        /**
         * 原始数据集ID
         */
        private String originalDatasetId;

        /**
         * 分配策略
         */
        private String allocationStrategy;

        /**
         * 预期数据量大小
         */
        private Integer expectedDataSize;

        /**
         * 分配比例
         */
        private Double allocationRatio;

        /**
         * 计划创建时间
         */
        private LocalDateTime plannedAt;

        public static DatasetAllocationPlan create(String vmId, String assignedDatasetId,
                                                   String originalDatasetId, String strategy) {
            return DatasetAllocationPlan.builder()
                .vmId(vmId)
                .assignedDatasetId(assignedDatasetId)
                .originalDatasetId(originalDatasetId)
                .allocationStrategy(strategy)
                .plannedAt(LocalDateTime.now())
                .build();
        }
    }

    /**
     * 训练数据集简化信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrainingDataset {
        private String datasetId;
        private String name;
        private String description;
        private String filePath;
        private Long fileSize;
        private Integer rowCount;
        private String dataType;
        private String status;
        private LocalDateTime uploadTime;
        private String uploadedBy;
    }

    /**
     * 创建成功结果
     */
    public static DatasetAllocationPreparation success(TrainingDataset dataset, List<DatasetAllocationPlan> plans) {
        return new DatasetAllocationPreparation(dataset, plans);
    }

    /**
     * 创建失败结果
     */
    public static DatasetAllocationPreparation failure(String errorMessage) {
        DatasetAllocationPreparation result = new DatasetAllocationPreparation();
        result.success = false;
        result.errorMessage = errorMessage;
        result.preparedAt = LocalDateTime.now();
        return result;
    }
}