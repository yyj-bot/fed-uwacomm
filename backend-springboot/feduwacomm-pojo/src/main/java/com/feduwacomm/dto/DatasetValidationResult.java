package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * v1.5数据集验证结果DTO
 * 用于返回参与者数据集验证的结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatasetValidationResult {

    /**
     * 所有数据集是否都就绪
     */
    private boolean allDatasetReady;

    /**
     * 每个VM的验证结果
     * Key: vmId, Value: 验证状态 (READY/NOT_READY)
     */
    private Map<String, String> validationResults;

    /**
     * 验证时间
     */
    private LocalDateTime validatedAt;

    /**
     * 便利构造函数
     */
    public DatasetValidationResult(boolean allDatasetReady, Map<String, String> validationResults) {
        this.allDatasetReady = allDatasetReady;
        this.validationResults = validationResults;
        this.validatedAt = LocalDateTime.now();
    }
}