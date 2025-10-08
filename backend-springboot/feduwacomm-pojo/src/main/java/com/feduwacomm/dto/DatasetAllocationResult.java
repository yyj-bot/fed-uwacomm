package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * v1.5数据集分配结果DTO
 * 用于返回数据集分配操作的结果信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatasetAllocationResult {

    /**
     * 分配是否成功
     */
    private boolean success;

    /**
     * 数据集分配列表
     */
    private List<DatasetAllocation> allocations;

    /**
     * 错误信息（失败时）
     */
    private String errorMessage;

    /**
     * 分配时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 创建成功结果
     */
    public static DatasetAllocationResult success(List<DatasetAllocation> allocations) {
        return DatasetAllocationResult.builder()
            .success(true)
            .allocations(allocations)
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * 创建失败结果
     */
    public static DatasetAllocationResult failure(String errorMessage) {
        return DatasetAllocationResult.builder()
            .success(false)
            .errorMessage(errorMessage)
            .timestamp(LocalDateTime.now())
            .build();
    }
}