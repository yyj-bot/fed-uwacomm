package com.feduwacomm.dto;

import com.feduwacomm.enums.LogCategory;
import com.feduwacomm.enums.LogLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogConfigDTO {

    private LogLevel logLevel;
    
    @Min(value = 1, message = "保留天数必须大于0")
    private Integer retentionDays;
    
    @Min(value = 1024, message = "文件大小必须大于1KB")
    private Long maxFileSize;
    
    private Map<LogCategory, CategoryConfig> categories;
    
    private ExportSettings exportSettings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryConfig {
        private LogLevel level;
        private Boolean enabled;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExportSettings {
        @Min(value = 1, message = "导出记录数必须大于0")
        private Integer maxRecordsPerExport;
        
        @Min(value = 1, message = "保留天数必须大于0")
        private Integer exportRetentionDays;
    }
}