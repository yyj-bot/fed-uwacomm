package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 模型评估响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelEvaluateResponseVO {

    /**
     * 模型ID
     */
    private String modelId;

    /**
     * 评估ID
     */
    private String evaluationId;

    /**
     * 评估指标
     */
    private Map<String, BigDecimal> metrics;

    /**
     * 评估时间（秒）
     */
    private Double evaluationTime;

    /**
     * 测试样本数量
     */
    private Integer testSamples;

    /**
     * 状态
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}