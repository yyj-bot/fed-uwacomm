package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 可用数据集查询响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableDatasetsResponseVO {

    private Integer total;
    private List<AvailableDatasetVO> availableDatasets;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvailableDatasetVO {
        private String datasetId;
        private String title;
        private String dataType;
        private String status;
        private Long size;
        private Integer rowCount;
        private LocalDateTime uploadedAt;
        private Map<String, Object> metadata;
    }
}