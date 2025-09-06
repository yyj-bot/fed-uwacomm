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
public class LogStatisticsVO {

    private Long totalLogs;
    private Map<LogLevel, Long> levelDistribution;
    private Map<LogCategory, Long> categoryDistribution;
    private List<TimeDistribution> timeDistribution;
    private List<ErrorTrend> errorTrend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeDistribution {
        private String hour;
        private Long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorTrend {
        private String date;
        private Long errorCount;
    }
}