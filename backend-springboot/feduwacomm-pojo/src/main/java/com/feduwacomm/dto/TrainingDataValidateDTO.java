package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * 训练数据验证请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingDataValidateDTO {

    private ValidationRules validationRules;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidationRules {
        private String dataType;
        private List<String> requiredColumns;
        private Map<String, String> dataTypes;
        private Map<String, ColumnConstraint> constraints;
        private List<String> qualityChecks;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ColumnConstraint {
        private Double min;
        private Double max;
        private Boolean notNull;
    }
}