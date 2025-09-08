package com.feduwacomm.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模型版本实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelVersion {

    /**
     * 模型版本唯一标识（32位UUID）
     */
    private String id;
    
    /**
     * 关联任务ID（32位UUID）
     */
    private String taskId;
    
    /**
     * 聚合轮次
     */
    private Integer roundNumber;
    
    /**
     * 聚合方式（如FEDAVG、FEDPROX等）
     */
    private String aggregationMethod;
    
    /**
     * 参与客户端数量
     */
    private Integer clientCount;
    
    /**
     * 聚合后模型参数（JSON格式）
     */
    private String modelJson;
    
    /**
     * 准确率
     */
    private BigDecimal accuracy;
    
    /**
     * 损失值
     */
    private BigDecimal loss;
    
    /**
     * 聚合后评估指标（JSON格式）
     */
    private String metrics;
    
    /**
     * 模型状态
     * UPLOADING - 上传中
     * UPLOADED - 已上传
     * VALIDATING - 验证中
     * VALIDATED - 已验证
     * DEPLOYED - 已部署
     * DEPRECATED - 已废弃
     * FAILED - 上传失败
     */
    private String status;
    
    /**
     * 模型描述
     */
    private String description;
    
    /**
     * 模型文件路径
     */
    private String filePath;
    
    /**
     * 模型文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 模型文件格式
     */
    private String fileFormat;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 聚合完成时间
     */
    private LocalDateTime aggregatedAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 创建者
     */
    private String createdBy;
    
    /**
     * 更新者
     */
    private String updatedBy;
    
    /**
     * 扩展参数（JSON格式，存储其他模型相关信息）
     */
    private String parameters;
}