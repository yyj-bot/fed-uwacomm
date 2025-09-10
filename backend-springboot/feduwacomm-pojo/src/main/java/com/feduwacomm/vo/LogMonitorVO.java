package com.feduwacomm.vo;

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
public class LogMonitorVO {

    private SystemInfo systemInfo;
    private ResourceUsage resourceUsage;
    private ApplicationMetrics applicationMetrics;
    private DatabaseMetrics databaseMetrics;
    private LogMetrics logMetrics;
    private List<TimeSeriesData> levelTrend;
    private List<TimeSeriesData> categoryTrend;
    private List<RecentError> recentErrors;
    private ApiMetrics apiMetrics;
    private List<EndpointMetric> endpointMetrics;
    private List<ResponseTimeData> responseTimeTrend;
    private List<Alert> alerts;
    private List<AlertHistory> alertHistory;
    private Map<String, Object> alertStatistics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemInfo {
        private String version;
        private Long uptime;
        private LocalDateTime startTime;
        private String javaVersion;
        private String osInfo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceUsage {
        private Double cpuUsage;
        private Double memoryUsage;
        private Double diskUsage;
        private NetworkIO networkIO;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NetworkIO {
        private Long bytesIn;
        private Long bytesOut;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicationMetrics {
        private Integer activeConnections;
        private Double requestPerSecond;
        private Double averageResponseTime;
        private Double errorRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatabaseMetrics {
        private Integer activeConnections;
        private Double queryPerSecond;
        private Double averageQueryTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogMetrics {
        private Long totalLogs;
        private Long errorCount;
        private Long warningCount;
        private Double errorRate;
        private Double warningRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSeriesData {
        private LocalDateTime timestamp;
        private Map<String, Long> data;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentError {
        private String logId;
        private String level;
        private String category;
        private String message;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiMetrics {
        private Long totalRequests;
        private Long successfulRequests;
        private Long failedRequests;
        private Double successRate;
        private Double averageResponseTime;
        private Double p95ResponseTime;
        private Double p99ResponseTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EndpointMetric {
        private String endpoint;
        private Long requestCount;
        private Double successRate;
        private Double averageResponseTime;
        private Long errorCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseTimeData {
        private LocalDateTime timestamp;
        private Double average;
        private Double p95;
        private Double p99;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Alert {
        private String alertId;
        private String name;
        private String type;
        private String condition;
        private String status;
        private String severity;
        private Boolean enabled;
        private Double threshold;
        private Double currentValue;
        private LocalDateTime lastTriggered;
        private Integer triggerCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertHistory {
        private String alertId;
        private String alertName;
        private LocalDateTime triggeredAt;
        private LocalDateTime resolvedAt;
        private String message;
        private String severity;
        private Double value;
        private Boolean resolved;
    }
}