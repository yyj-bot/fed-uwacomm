package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * v1.5数据集分配验证结果DTO
 * 用于验证整个任务的数据集分配完整性
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatasetAllocationValidation {

    /**
     * 分配信息映射
     * Key: vmId, Value: 数据集分片信息
     */
    private Map<String, DatasetSliceInfo> allocationInfo;

    /**
     * 总样本数
     */
    private int totalSamples;

    /**
     * 验证时间
     */
    private LocalDateTime validatedAt;

    /**
     * 是否通过验证
     */
    private boolean isValid;

    /**
     * 验证错误信息
     */
    private String errorMessage;

    /**
     * 便利构造函数
     */
    public DatasetAllocationValidation(Map<String, DatasetSliceInfo> allocationInfo, int totalSamples) {
        this.allocationInfo = allocationInfo;
        this.totalSamples = totalSamples;
        this.validatedAt = LocalDateTime.now();
        this.isValid = validateAllocation();
    }

    /**
     * 验证分配是否正确
     */
    private boolean validateAllocation() {
        if (allocationInfo == null || allocationInfo.isEmpty()) {
            this.errorMessage = "分配信息为空";
            return false;
        }

        int calculatedTotal = allocationInfo.values().stream()
            .mapToInt(DatasetSliceInfo::getSampleCount)
            .sum();

        if (calculatedTotal != totalSamples) {
            this.errorMessage = "分配的总样本数不匹配: 期望=" + totalSamples + ", 实际=" + calculatedTotal;
            return false;
        }

        return true;
    }

    /**
     * 数据集分片信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatasetSliceInfo {
        private String assignedDatasetId;
        private int sampleCount;
        private int startIndex;
        private int endIndex;
        private String localPath;
        private String status;
    }
}