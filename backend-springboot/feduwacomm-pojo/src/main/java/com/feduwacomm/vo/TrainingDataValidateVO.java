package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 训练数据验证响应VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingDataValidateVO {

    private String datasetId;

    private Boolean isValid;

    private LocalDateTime validationTime;

    private ValidationResults results;

    private List<ValidationError> errors;

    private List<ValidationWarning> warnings;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidationResults {
        private Integer totalRows;
        private Integer validRows;
        private Integer invalidRows;
        private Integer missingValues;
        private Integer duplicates;
        private Integer outliers;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidationError {
        private Integer row;
        private String column;
        private String error;
        private Object value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidationWarning {
        private String type;
        private Integer count;
        private List<String> columns;
    }
}