package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 训练数据导出请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingDataExportDTO {

    private String exportType = "CSV";

    private ExportFilters filters;

    private List<String> fields;

    private String format = "ZIP";

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExportFilters {
        private String dataType;
        private String status;
        private String startTime;
        private String endTime;
    }
}