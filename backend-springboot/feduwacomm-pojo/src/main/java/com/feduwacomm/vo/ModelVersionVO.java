package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 模型版本信息VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelVersionVO {

    /**
     * 模型版本ID
     */
    private String modelId;

    /**
     * 关联任务ID
     */
    private String taskId;

    /**
     * 聚合轮次
     */
    private Integer roundNumber;

    /**
     * 聚合方式
     */
    private String aggregationMethod;

    /**
     * 参与客户端数量
     */
    private Integer clientCount;

    /**
     * 准确率
     */
    private BigDecimal accuracy;

    /**
     * 损失值
     */
    private BigDecimal loss;

    /**
     * 模型状态
     */
    private String status;

    /**
     * 模型描述
     */
    private String description;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件格式
     */
    private String fileFormat;

    /**
     * 评估指标
     */
    private Map<String, Object> metrics;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 聚合完成时间
     */
    private LocalDateTime aggregatedAt;

    /**
     * 扩展参数
     */
    private Map<String, Object> parameters;
}