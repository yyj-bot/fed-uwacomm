package com.feduwacomm.model.dto.federated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import com.feduwacomm.model.dto.federated.InitialModelConfigDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 联邦学习任务配置更新请求（v1.5）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FederatedTaskConfigDTO {

    private String algorithm;

    @Valid
    private HyperparametersDTO hyperparameters;

    @Valid
    private InitialModelConfigDTO initialModelConfig;

    @Valid
    private DataConfigDTO dataConfig;

    @Valid
    private SecurityConfigDTO securityConfig;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HyperparametersDTO {
        @DecimalMin(value = "0.0001", message = "学习率不能小于0.0001")
        @DecimalMax(value = "1.0", message = "学习率不能大于1.0")
        private Double learningRate;

        @Min(value = 1, message = "批次大小不能小于1")
        @Max(value = 1024, message = "批次大小不能大于1024")
        private Integer batchSize;

        @Min(value = 1, message = "训练轮次不能小于1")
        @Max(value = 1000, message = "训练轮次不能大于1000")
        private Integer epochs;

        @Min(value = 1, message = "聚合轮数不能小于1")
        @Max(value = 100, message = "聚合轮数不能大于100")
        private Integer rounds;

        @Min(value = 1, message = "最少参与者数量不能小于1")
        private Integer minParticipants;

        private String aggregationMethod;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataConfigDTO {
        @Valid
        private PreprocessingDTO preprocessing;

        @Valid
        private ValidationDTO validation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreprocessingDTO {
        private String normalization;
        private String featureSelection;
        private Boolean outlierRemoval = false;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationDTO {
        private String crossValidation;

        @Min(value = 2, message = "K折数不能小于2")
        @Max(value = 10, message = "K折数不能大于10")
        private Integer kFolds = 5;

        private Boolean stratified = true;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecurityConfigDTO {
        private String encryption;

        @Valid
        private DifferentialPrivacyDTO differentialPrivacy;

        private Boolean secureAggregation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DifferentialPrivacyDTO {
        private Boolean enabled;

        @DecimalMin(value = "0.1", message = "epsilon不能小于0.1")
        @DecimalMax(value = "10.0", message = "epsilon不能大于10.0")
            private Double epsilon;

        @DecimalMin(value = "0.00001", message = "delta不能小于0.00001")
        @DecimalMax(value = "0.1", message = "delta不能大于0.1")
        private Double delta;
    }

}
