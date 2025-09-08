package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 训练数据详情响应VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingDataVO {

    private String datasetId;

    private String datasetDescription;

    private String datasetType;

    private String vmId;

    private String status;

    private LocalDateTime uploadTime;

    private String uploadedBy;

    private List<String> tags;

    private Map<String, Object> metadata;

    private ValidationInfo validation;

    private PreprocessingInfo preprocessing;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidationInfo {
        private Boolean isValid;
        private LocalDateTime validationTime;
        private List<String> errors;
        private List<String> warnings;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PreprocessingInfo {
        private Boolean isProcessed;
        private LocalDateTime processTime;
        private List<String> methods;
        private Map<String, Object> parameters;
    }
}