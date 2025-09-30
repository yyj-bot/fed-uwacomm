package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 联邦学习任务创建请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreateDTO {

    @NotBlank(message = "任务名称不能为空")
    @Size(max = 100, message = "任务名称长度不能超过100字符")
    private String taskName;

    @NotBlank(message = "任务类型不能为空")
    private String taskType; // CLASSIFICATION, REGRESSION, CLUSTERING, ANOMALY_DETECTION

    private String description;

    @NotBlank(message = "算法类型不能为空")
    private String algorithm; // FEDERATED_AVERAGING, FEDERATED_PROXIMAL, FEDERATED_NOVA, FEDERATED_SCAFFOLD

    // v1.0 废弃：使用新的participantConfig替代
    @Deprecated
    private List<ParticipantDTO> participants;

    @Valid
    private HyperparametersDTO hyperparameters;

    @Valid
    private ModelConfigDTO modelConfig;

    @Valid
    private ScheduleDTO schedule;

    @Valid
    private SecurityConfigDTO securityConfig;

    // v1.3 必需：智能数据集配置
    @Valid
    @NotNull(message = "数据集配置不能为空")
    private DatasetConfigDTO datasetConfig;

    // v1.3 必需：智能参与者配置
    @Valid
    @NotNull(message = "参与者配置不能为空")
    private ParticipantConfigDTO participantConfig;

    // v2.0 新增：联邦学习聚合配置
    @Valid
    private AggregationConfigDTO aggregationConfig;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantDTO {
        @NotBlank(message = "虚拟机ID不能为空")
        private String vmId;
        
        @NotBlank(message = "参与者角色不能为空")
        private String role; // PARTICIPANT
        
        private String dataSource;
    }

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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelConfigDTO {
        @NotBlank(message = "模型类型不能为空")
        private String modelType; // RANDOM_FOREST, SVM, NEURAL_NETWORK

        private List<String> featureColumns;
        private String targetColumn;
        
        @DecimalMin(value = "0.1", message = "测试集比例不能小于0.1")
        @DecimalMax(value = "0.5", message = "测试集比例不能大于0.5")
        private Double testSize = 0.2;
        
        private Integer randomState = 42;

        // 模型特定参数
        private Integer nEstimators = 100;
        private Integer maxDepth = 10;
        private Integer minSamplesSplit = 2;
        private Integer minSamplesLeaf = 1;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleDTO {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        
        @Min(value = 60, message = "超时时间不能小于60秒")
        @Max(value = 86400, message = "超时时间不能大于86400秒")
        private Integer timeout = 3600; // 默认1小时
    }

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

    // v1.3 新增：数据集配置DTO
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatasetConfigDTO {
        @NotBlank(message = "数据集ID不能为空")
        private String datasetId;

        @NotBlank(message = "分配策略不能为空")
        private String distributionStrategy; // BALANCED, WEIGHTED, CUSTOM

        @Valid
        private java.util.Map<String, Double> distributionRatios; // vmId -> ratio

        @DecimalMin(value = "0.0", message = "验证集比例不能小于0.0")
        @DecimalMax(value = "0.5", message = "验证集比例不能大于0.5")
        private Double validationSplit = 0.2;

        @DecimalMin(value = "0.0", message = "测试集比例不能小于0.0")
        @DecimalMax(value = "0.3", message = "测试集比例不能大于0.3")
        private Double testSplit = 0.1;

    }

    // v1.3 新增：参与者配置DTO
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantConfigDTO {
        @NotBlank(message = "选择模式不能为空")
        private String selectionMode; // MANUAL, AUTO, HYBRID

        @Valid
        private RequirementsDTO requirements;

        @Valid
        @NotEmpty(message = "参与者列表不能为空")
        private List<SmartParticipantDTO> participants;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class RequirementsDTO {
            @Min(value = 1, message = "最少参与者数量不能小于1")
            private Integer minParticipants = 2;

            @Max(value = 50, message = "最多参与者数量不能大于50")
            private Integer maxParticipants = 10;

            @Min(value = 1, message = "最小CPU核心数不能小于1")
            private Integer minCpuCores = 2;

            @Min(value = 1024, message = "最小内存不能小于1024MB")
            private Integer minMemoryMb = 4096;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class SmartParticipantDTO {
            @NotBlank(message = "虚拟机ID不能为空")
            private String vmId;

            @NotBlank(message = "参与者角色不能为空")
            private String role; // PARTICIPANT

            /**
             * v1.5.1.1 数据分配千分比权重
             *
             * <p>取值范围：1-1000的正整数
             * <p>约束条件：同一联邦学习任务的所有参与者的dataRatio之和必须等于1000
             * <p>语义说明：dataRatio表示该VM占用数据集的千分比
             *
             * <p>示例：
             * <ul>
             *   <li>VM1.dataRatio=700, VM2.dataRatio=200, VM3.dataRatio=100 (7:2:1比例)</li>
             *   <li>VM1占用70%数据(700/1000), VM2占用20%(200/1000), VM3占用10%(100/1000)</li>
             *   <li>总和验证: 700+200+100=1000 ✓</li>
             * </ul>
             *
             * <p>如果为null，系统会根据参与者数量自动分配平均权重（如3个VM各334、333、333）
             */
            @Min(value = 1, message = "数据分配权重不能小于1")
            @Max(value = 1000, message = "数据分配权重不能大于1000")
            private Integer dataRatio;

            private List<String> capabilities; // GPU, HIGH_MEMORY, FAST_NETWORK

            @Valid
            private ConstraintsDTO constraints;

            @Data
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            public static class ConstraintsDTO {
                @Min(value = 1, message = "最大CPU使用率不能小于1%")
                @Max(value = 100, message = "最大CPU使用率不能大于100%")
                private Integer maxCpuUsage = 80;

                @Min(value = 1, message = "最大内存使用率不能小于1%")
                @Max(value = 100, message = "最大内存使用率不能大于100%")
                private Integer maxMemoryUsage = 75;
            }
        }
    }

    // v2.0 新增：联邦学习聚合配置DTO
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AggregationConfigDTO {
        @NotBlank(message = "聚合策略不能为空")
        private String strategy; // FEDERATED_AVERAGING, FEDERATED_PROXIMAL, FEDERATED_NOVA, FEDERATED_SCAFFOLD

        private String engineType; // UNIVERSAL, SPECIALIZED
        private List<String> supportedModelTypes; // RANDOM_FOREST, NEURAL_NETWORK, SVM
        private List<String> supportedAlgorithms; // FEDERATED_AVERAGING, FEDERATED_PROXIMAL, etc.
        private Boolean performanceOptimization = true;
        private Boolean memoryEfficient = true;

        @DecimalMin(value = "0.001", message = "收敛阈值不能小于0.001")
        @DecimalMax(value = "1.0", message = "收敛阈值不能大于1.0")
        private Double convergenceThreshold = 0.01;

        @Min(value = 1, message = "最少聚合模型数量不能小于1")
        private Integer minModelsForAggregation = 2;

        @Min(value = 1, message = "最大等待时间不能小于1秒")
        @Max(value = 3600, message = "最大等待时间不能大于3600秒")
        private Integer maxWaitTimeSeconds = 300;

        private Boolean supportMixedModels = false;
        private String aggregationMethod = "UNIVERSAL";

        // FedProx专用参数
        @DecimalMin(value = "0.0", message = "近端项系数不能小于0.0")
        @DecimalMax(value = "1.0", message = "近端项系数不能大于1.0")
        private Double proximalTerm = 0.1;

        // FedNova专用参数
        @DecimalMin(value = "0.1", message = "动量因子不能小于0.1")
        @DecimalMax(value = "0.9", message = "动量因子不能大于0.9")
        private Double momentumFactor = 0.9;

        // Scaffold专用参数
        private Boolean enableControlVariates = true;
    }
}