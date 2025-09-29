package com.feduwacomm.service.impl;

import com.feduwacomm.service.PerformanceMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能监控服务实现类
 */
@Slf4j
@Service
public class PerformanceMonitorServiceImpl implements PerformanceMonitorService {

    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private final Map<String, Double> gauges = new ConcurrentHashMap<>();
    private final List<PerformanceMetric> metrics = new ArrayList<>();

    @Override
    public void recordMetric(String metricName, String metricType, double value, String unit, Map<String, String> tags) {
        try {
            PerformanceMetric metric = new PerformanceMetric(metricName, metricType, value, unit, LocalDateTime.now(), tags);
            metrics.add(metric);

            if ("GAUGE".equals(metricType)) {
                gauges.put(metricName, value);
            } else if ("COUNTER".equals(metricType)) {
                counters.computeIfAbsent(metricName, k -> new AtomicLong(0)).set((long) value);
            }

            log.debug("记录性能指标: {}", metric);
        } catch (Exception e) {
            log.error("记录性能指标失败: metricName={}, value={}", metricName, value, e);
        }
    }

    @Override
    public void recordMethodExecution(String methodName, long executionTimeMs, boolean success) {
        Map<String, String> tags = new HashMap<>();
        tags.put("method", methodName);
        tags.put("success", String.valueOf(success));
        recordMetric(methodName + "_execution_time", "HISTOGRAM", executionTimeMs, "milliseconds", tags);
    }

    @Override
    public void recordHttpRequest(String endpoint, String method, int statusCode, long responseTimeMs) {
        Map<String, String> tags = new HashMap<>();
        tags.put("endpoint", endpoint);
        tags.put("method", method);
        tags.put("status_code", String.valueOf(statusCode));
        recordMetric("http_request_duration", "HISTOGRAM", responseTimeMs, "milliseconds", tags);
    }

    @Override
    public void recordDatabaseOperation(String operation, String table, long executionTimeMs, boolean success) {
        Map<String, String> tags = new HashMap<>();
        tags.put("operation", operation);
        tags.put("table", table);
        tags.put("success", String.valueOf(success));
        recordMetric("db_operation_duration", "HISTOGRAM", executionTimeMs, "milliseconds", tags);
    }

    @Override
    public void recordCacheOperation(String operation, String cacheKey, boolean hit, long executionTimeMs) {
        Map<String, String> tags = new HashMap<>();
        tags.put("operation", operation);
        tags.put("cache_key", cacheKey);
        tags.put("hit", String.valueOf(hit));
        recordMetric("cache_operation_duration", "HISTOGRAM", executionTimeMs, "milliseconds", tags);
    }

    @Override
    public List<PerformanceMetric> getMetrics(String metricName, LocalDateTime startTime, LocalDateTime endTime) {
        return metrics.stream()
                .filter(m -> m.getMetricName().equals(metricName))
                .filter(m -> m.getTimestamp().isAfter(startTime) && m.getTimestamp().isBefore(endTime))
                .toList();
    }

    @Override
    public SystemHealth getSystemHealth() {
        return new SystemHealth("healthy", 50.0, 60.0, 30.0, 10, 1000, 100.0, LocalDateTime.now());
    }

    @Override
    public Map<String, Object> getPerformanceSummary(int hours) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("total_metrics", metrics.size());
        summary.put("counters", counters.size());
        summary.put("gauges", gauges.size());
        return summary;
    }

    @Override
    public long cleanupOldMetrics(int retentionHours) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(retentionHours);
        int initialSize = metrics.size();
        metrics.removeIf(m -> m.getTimestamp().isBefore(cutoff));
        return initialSize - metrics.size();
    }

    @Override
    public List<String> checkPerformanceAlerts() {
        return new ArrayList<>();
    }

    @Override
    public void startProfilingSession(String sessionId, String description) {
        log.info("开始性能分析会话: sessionId={}, description={}", sessionId, description);
    }

    @Override
    public Map<String, Object> endProfilingSession(String sessionId) {
        log.info("结束性能分析会话: sessionId={}", sessionId);
        return new HashMap<>();
    }

    @Override
    public Map<String, Object> getRealTimeMetrics() {
        Map<String, Object> realTime = new HashMap<>();
        realTime.put("timestamp", System.currentTimeMillis());
        realTime.put("active_counters", counters.size());
        realTime.put("active_gauges", gauges.size());
        return realTime;
    }

    // === 数据分发相关监控方法 ===

    @Override
    public void incrementDistributionTaskCounter(String taskId, String status) {
        String metricName = "distribution_task_" + status.toLowerCase();
        Map<String, String> tags = new HashMap<>();
        tags.put("task_id", taskId);
        tags.put("status", status);

        AtomicLong counter = counters.computeIfAbsent(metricName, k -> new AtomicLong(0));
        counter.incrementAndGet();
        recordMetric(metricName, "COUNTER", counter.get(), "count", tags);
    }

    @Override
    public void incrementDistributionTaskCounter(String taskId, String vmId, String status) {
        String metricName = "distribution_task_vm_" + status.toLowerCase();
        Map<String, String> tags = new HashMap<>();
        tags.put("task_id", taskId);
        tags.put("vm_id", vmId);
        tags.put("status", status);

        AtomicLong counter = counters.computeIfAbsent(metricName, k -> new AtomicLong(0));
        counter.incrementAndGet();
        recordMetric(metricName, "COUNTER", counter.get(), "count", tags);
    }

    @Override
    public void recordDistributionVmCount(String taskId, Integer vmCount) {
        Map<String, String> tags = new HashMap<>();
        tags.put("task_id", taskId);
        recordMetric("distribution_vm_count", "GAUGE", vmCount, "count", tags);
    }

    @Override
    public void recordDistributionDataSize(String taskId, Long dataSize) {
        Map<String, String> tags = new HashMap<>();
        tags.put("task_id", taskId);
        recordMetric("distribution_data_size", "GAUGE", dataSize, "bytes", tags);
    }

    @Override
    public void recordDistributionStartTime(String taskId, long startTime) {
        Map<String, String> tags = new HashMap<>();
        tags.put("task_id", taskId);
        recordMetric("distribution_start_time", "GAUGE", startTime, "milliseconds", tags);
    }

    @Override
    public void recordDistributionExecutionTime(String taskId, Long executionTime) {
        Map<String, String> tags = new HashMap<>();
        tags.put("task_id", taskId);
        recordMetric("distribution_execution_time", "HISTOGRAM", executionTime, "milliseconds", tags);
    }

    @Override
    public void recordDistributionResult(String taskId, boolean success) {
        Map<String, String> tags = new HashMap<>();
        tags.put("task_id", taskId);
        tags.put("success", String.valueOf(success));

        String metricName = "distribution_result_" + (success ? "success" : "failure");
        AtomicLong counter = counters.computeIfAbsent(metricName, k -> new AtomicLong(0));
        counter.incrementAndGet();
        recordMetric(metricName, "COUNTER", counter.get(), "count", tags);
    }

    @Override
    public void recordDataTransferRate(String taskId, double transferRate) {
        Map<String, String> tags = new HashMap<>();
        tags.put("task_id", taskId);
        recordMetric("data_transfer_rate", "GAUGE", transferRate, "bytes_per_second", tags);
    }

    @Override
    public void recordVmDistributionSuccessRate(String taskId, double successRate) {
        Map<String, String> tags = new HashMap<>();
        tags.put("task_id", taskId);
        recordMetric("vm_distribution_success_rate", "GAUGE", successRate, "percentage", tags);
    }

    @Override
    public void recordDistributionVmResults(String taskId, Integer successCount, Integer failedCount) {
        Map<String, String> tags = new HashMap<>();
        tags.put("task_id", taskId);
        recordMetric("distribution_vm_success", "GAUGE", successCount, "count", tags);
        recordMetric("distribution_vm_failed", "GAUGE", failedCount, "count", tags);
    }

    @Override
    public void recordDistributionStatistics(String taskId, Map<String, Object> statistics) {
        Map<String, String> tags = new HashMap<>();
        tags.put("task_id", taskId);

        for (Map.Entry<String, Object> entry : statistics.entrySet()) {
            if (entry.getValue() instanceof Number) {
                double value = ((Number) entry.getValue()).doubleValue();
                recordMetric("distribution_stat_" + entry.getKey(), "GAUGE", value, "units", tags);
            }
        }
    }

    @Override
    public void recordDistributionDatasetCount(String distributionStrategy, int datasetCount) {
        Map<String, String> tags = new HashMap<>();
        tags.put("strategy", distributionStrategy);
        recordMetric("distribution_dataset_count", "GAUGE", datasetCount, "count", tags);
    }

    @Override
    public void recordManualInterventionRequest(String orchestrationId, String reason, String operator) {
        Map<String, String> tags = new HashMap<>();
        tags.put("orchestration_id", orchestrationId);
        tags.put("reason", reason);
        tags.put("operator", operator);

        AtomicLong counter = counters.computeIfAbsent("manual_intervention_requests", k -> new AtomicLong(0));
        counter.incrementAndGet();
        recordMetric("manual_intervention_requests", "COUNTER", counter.get(), "count", tags);
    }

    @Override
    public void recordWorkflowTermination(String orchestrationId, String reason, double duration) {
        Map<String, String> tags = new HashMap<>();
        tags.put("orchestration_id", orchestrationId);
        tags.put("reason", reason);
        recordMetric("workflow_termination_duration", "HISTOGRAM", duration, "milliseconds", tags);
    }

    // === 工作流阶段相关监控方法 ===

    @Override
    public void recordStageExecutionTime(String stageName, Long executionTime) {
        Map<String, String> tags = new HashMap<>();
        tags.put("stage", stageName);
        recordMetric("stage_execution_time", "HISTOGRAM", executionTime, "milliseconds", tags);
    }

    @Override
    public void recordStageResult(String stageName, boolean success) {
        Map<String, String> tags = new HashMap<>();
        tags.put("stage", stageName);
        tags.put("success", String.valueOf(success));

        String metricName = "stage_result_" + (success ? "success" : "failure");
        AtomicLong counter = counters.computeIfAbsent(metricName, k -> new AtomicLong(0));
        counter.incrementAndGet();
        recordMetric(metricName, "COUNTER", counter.get(), "count", tags);
    }

    @Override
    public void recordWorkflowStageMetrics(String orchestrationId, String stageName,
                                         Integer stageOrder, Long executionTime, boolean success) {
        Map<String, String> tags = new HashMap<>();
        tags.put("orchestration_id", orchestrationId);
        tags.put("stage_name", stageName);
        tags.put("stage_order", String.valueOf(stageOrder));
        tags.put("success", String.valueOf(success));

        if (executionTime != null) {
            recordMetric("workflow_stage_execution_time", "HISTOGRAM", executionTime.doubleValue(), "milliseconds", tags);
        }

        String resultMetric = "workflow_stage_" + (success ? "success" : "failure");
        AtomicLong resultCount = counters.computeIfAbsent(resultMetric, k -> new AtomicLong(0));
        resultCount.incrementAndGet();
        recordMetric(resultMetric, "COUNTER", resultCount.get(), "count", tags);
    }

    @Override
    public void recordDataProcessingMetrics(String stageName, String dataType, int dataSize) {
        Map<String, String> tags = new HashMap<>();
        tags.put("stage", stageName);
        tags.put("data_type", dataType);
        recordMetric("data_processing_size", "GAUGE", dataSize, "bytes", tags);

        String countMetric = "data_processing_count_" + dataType;
        AtomicLong processCount = counters.computeIfAbsent(countMetric, k -> new AtomicLong(0));
        processCount.incrementAndGet();
        recordMetric(countMetric, "COUNTER", processCount.get(), "count", tags);
    }

    @Override
    public void recordResourceUsage(String stageName, Map<String, Object> resourceUsage) {
        if (resourceUsage == null || resourceUsage.isEmpty()) {
            return;
        }

        Map<String, String> tags = new HashMap<>();
        tags.put("stage", stageName);

        for (Map.Entry<String, Object> entry : resourceUsage.entrySet()) {
            String resourceName = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Number) {
                double numericValue = ((Number) value).doubleValue();
                String unit = "units";
                if (resourceName.toLowerCase().contains("memory")) {
                    unit = "bytes";
                } else if (resourceName.toLowerCase().contains("cpu")) {
                    unit = "percentage";
                } else if (resourceName.toLowerCase().contains("time")) {
                    unit = "milliseconds";
                }

                recordMetric("resource_usage_" + resourceName, "GAUGE", numericValue, unit, tags);
            }
        }
    }

    @Override
    public void updateThroughputMetrics(String stageName, long currentTime) {
        Map<String, String> tags = new HashMap<>();
        tags.put("stage", stageName);
        recordMetric("throughput_timestamp", "GAUGE", currentTime, "milliseconds", tags);
        recordMetric("throughput_rate", "GAUGE", 0.0, "events_per_second", tags);
    }

    @Override
    public void recordErrorMetrics(String stageName, String errorCategory) {
        Map<String, String> tags = new HashMap<>();
        tags.put("stage", stageName);
        tags.put("error_category", errorCategory);

        String errorCountMetric = "error_count_" + errorCategory;
        AtomicLong errorCount = counters.computeIfAbsent(errorCountMetric, k -> new AtomicLong(0));
        errorCount.incrementAndGet();
        recordMetric(errorCountMetric, "COUNTER", errorCount.get(), "errors", tags);
    }

    @Override
    public void recordConcurrencyMetrics(String orchestrationId, String stageName, int concurrentStageCount) {
        Map<String, String> tags = new HashMap<>();
        tags.put("orchestration_id", orchestrationId);
        tags.put("stage", stageName);
        recordMetric("concurrent_stage_count", "GAUGE", concurrentStageCount, "count", tags);
    }
}