package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 训练数据批量操作响应VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingDataBatchVO {

    private String operation;

    private Integer total;

    private Integer success;

    private Integer failed;

    private List<BatchResult> results;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BatchResult {
        private String datasetId;
        private String status;
        private String message;
    }
}