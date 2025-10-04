package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * v1.5数据集分配信息DTO
 * 包含单个VM的数据集分配详情
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatasetAllocation {

    /**
     * 虚拟机ID
     */
    private String vmId;

    /**
     * 分配的数据集ID
     */
    private String assignedDatasetId;

    /**
     * 分配时间
     */
    private LocalDateTime allocatedAt;

    /**
     * 便利构造函数
     */
    public DatasetAllocation(String vmId, String assignedDatasetId) {
        this.vmId = vmId;
        this.assignedDatasetId = assignedDatasetId;
        this.allocatedAt = LocalDateTime.now();
    }
}