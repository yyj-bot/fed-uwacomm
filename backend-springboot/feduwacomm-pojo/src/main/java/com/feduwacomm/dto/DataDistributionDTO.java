package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;

/**
 * 数据分发请求DTO
 * 根据重构计划实现多种分发策略支持
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataDistributionDTO {

    /**
     * 任务ID
     */
    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    /**
     * 数据集ID列表
     */
    @NotEmpty(message = "数据集ID列表不能为空") 
    private List<String> datasetIds;

    /**
     * 分发策略: RANDOM, BALANCED, CUSTOM, ROUND_ROBIN
     */
    @NotBlank(message = "分发策略不能为空")
    private String distributionStrategy;

    /**
     * 目标虚拟机ID列表
     */
    @NotEmpty(message = "目标虚拟机列表不能为空")
    private List<String> targetVmIds;

    /**
     * 分发配置参数
     */
    private Map<String, Object> distributionConfig;

    /**
     * 分片数量
     */
    @Min(value = 1, message = "分片数量必须大于0")
    private Integer shardCount;

    /**
     * 重叠比例(0-1)
     */
    private Double overlapRatio;

    /**
     * 是否打乱数据
     */
    @Builder.Default
    private Boolean enableShuffle = false;

    /**
     * 是否启用压缩
     */
    @Builder.Default
    private Boolean enableCompression = false;

    /**
     * 数据类型: TRAINING_DATA, VALIDATION_DATA, TEST_DATA
     */
    private String dataType;

    /**
     * 是否验证数据完整性
     */
    @Builder.Default
    private Boolean verifyIntegrity = true;

    /**
     * 分发超时时间(秒)
     */
    @Builder.Default
    @Min(value = 1, message = "超时时间必须大于0")
    private Integer timeoutSeconds = 600;

    /**
     * 最大重试次数
     */
    @Builder.Default
    @Min(value = 0, message = "重试次数不能小于0")
    private Integer maxRetries = 3;

    /**
     * 分发参数
     */
    private Map<String, Object> distributionParameters;

    /**
     * 元数据
     */
    private Map<String, Object> metadata;

    /**
     * 分发描述
     */
    private String description;
}