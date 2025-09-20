package com.feduwacomm.vo;

import com.feduwacomm.enums.LogCategory;
import com.feduwacomm.enums.LogLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogConfigVO {

    private LogLevel logLevel;
    private Integer retentionDays;
    private Long maxFileSize;
    private Map<String, CategoryConfig> categories;
    private DownloadSettings downloadSettings;
    private LocalDateTime updatedAt;

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
    public static class DownloadSettings {
        private Integer maxRecordsPerDownload;
        private Integer exportRetentionDays;
        private List<String> supportedFormats;
    }
}