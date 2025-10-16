package com.feduwacomm.model.dto.federated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 联邦任务创建请求（v1.5）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FederatedTaskCreateRequest {

    @NotBlank(message = "任务名称不能为空")
    @Size(max = 100, message = "任务名称长度不能超过100字符")
    private String taskName;

    @NotBlank(message = "任务类型不能为空")
    private String taskType;

    @Size(max = 500, message = "任务描述长度不能超过500字符")
    private String description;

    @NotBlank(message = "算法类型不能为空")
    private String algorithm;

    @Valid
    private HyperparametersDTO hyperparameters;

    @Valid
    private ScheduleDTO schedule;

    @Valid
    private SecurityConfigDTO securityConfig;

    @Valid
    @NotNull(message = "数据集配置不能为空")
    private DatasetConfigDTO datasetConfig;

    @Valid
    @NotNull(message = "参与者配置不能为空")
    private ParticipantConfigDTO participantConfig;

    @Valid
    @NotNull(message = "初始模型配置不能为空")
    private InitialModelConfigDTO initialModelConfig;

    @Valid
    private AggregationConfigDTO aggregationConfig;

    /**
     * 训练超参数配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HyperparametersDTO {
        @DecimalMin(value = "0.0001", message = "学习率不能小于0.0001")
        @DecimalMax(value = "1.0", message = "学习率不能大于1.0")
        private Double learningRate = 0.01;

        @Min(value = 1, message = "批次大小不能小于1")
        @Max(value = 1024, message = "批次大小不能大于1024")
        private Integer batchSize = 32;

        @Min(value = 1, message = "训练轮次不能小于1")
        @Max(value = 1000, message = "训练轮次不能大于1000")
        private Integer epochs = 100;

        @Min(value = 1, message = "聚合轮数不能小于1")
        @Max(value = 100, message = "聚合轮数不能大于100")
        private Integer rounds = 10;

        @Min(value = 1, message = "最少参与者数量不能小于1")
        private Integer minParticipants = 2;

        private String aggregationMethod = "WEIGHTED_AVERAGE";
    }

    /**
     * 调度配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleDTO {
        private LocalDateTime startTime;
        private LocalDateTime endTime;

        @Min(value = 60, message = "超时时间不能小于60秒")
        @Max(value = 86400, message = "超时时间不能大于86400秒")
        private Integer timeout = 3600;
    }

    /**
     * 安全配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecurityConfigDTO {
        private String encryption = "AES_256";
        private DifferentialPrivacyDTO differentialPrivacy;
        private Boolean secureAggregation = false;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DifferentialPrivacyDTO {
        private Boolean enabled = false;

        @DecimalMin(value = "0.1", message = "epsilon不能小于0.1")
        @DecimalMax(value = "10.0", message = "epsilon不能大于10.0")
        private Double epsilon = 1.0;

        @DecimalMin(value = "0.00001", message = "delta不能小于0.00001")
        @DecimalMax(value = "0.1", message = "delta不能大于0.1")
        private Double delta = 0.0001;
    }

    /**
     * 数据集配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatasetConfigDTO {
        @NotBlank(message = "数据集ID不能为空")
        private String datasetId;

        @NotBlank(message = "分配策略不能为空")
        private String distributionStrategy;

        @Valid
        private Map<String, Double> distributionRatios;

        @DecimalMin(value = "0.0", message = "验证集比例不能小于0.0")
        @DecimalMax(value = "0.5", message = "验证集比例不能大于0.5")
        private Double validationSplit = 0.2;

        @DecimalMin(value = "0.0", message = "测试集比例不能小于0.0")
        @DecimalMax(value = "0.3", message = "测试集比例不能大于0.3")
        private Double testSplit = 0.1;
    }

    /**
     * 参与者配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantConfigDTO {
        @NotBlank(message = "选择模式不能为空")
        private String selectionMode;

        @Valid
        private RequirementsDTO requirements;

        @Valid
        private List<SmartParticipantDTO> participants;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class RequirementsDTO {
            @Min(value = 1, message = "最少参与者数量不能小于1")
            private Integer minParticipants = 2;

            private Integer maxParticipants;
            private Integer minCpuCores;
            private Integer minMemoryMb;
            private List<String> capabilities;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class SmartParticipantDTO {
            @NotBlank(message = "虚拟机ID不能为空")
            private String vmId;

            @NotBlank(message = "角色不能为空")
            private String role;

            private Double dataRatio;

            private List<String> capabilities;

            private ConstraintsDTO constraints;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ConstraintsDTO {
            private Integer maxCpuUsage;
            private Integer maxMemoryUsage;
        }
    }

    /**
     * 聚合配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AggregationConfigDTO {
        private String strategy;
        private Integer maxRounds;
        private Double convergenceThreshold;
    }

}
