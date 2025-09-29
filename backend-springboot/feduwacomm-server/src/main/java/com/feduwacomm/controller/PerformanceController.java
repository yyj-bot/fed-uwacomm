package com.feduwacomm.controller;

import com.feduwacomm.common.Result;
import com.feduwacomm.service.PerformanceMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 性能监控控制器
 * 提供性能监控和指标查询的REST API
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-28
 */
@Slf4j
@RestController
@RequestMapping("/api/performance")
@Tag(name = "性能监控", description = "系统性能监控和指标管理API")
public class PerformanceController {

    @Autowired
    private PerformanceMonitorService performanceMonitorService;

    @GetMapping("/health")
    @Operation(summary = "获取系统健康状态", description = "返回当前系统的健康状态和关键性能指标")
    public Result<PerformanceMonitorService.SystemHealth> getSystemHealth() {
        try {
            PerformanceMonitorService.SystemHealth health = performanceMonitorService.getSystemHealth();
            return Result.success(health);
        } catch (Exception e) {
            log.error("获取系统健康状态失败: {}", e.getMessage(), e);
            return Result.error("获取系统健康状态失败");
        }
    }

    @GetMapping("/realtime")
    @Operation(summary = "获取实时性能数据", description = "返回当前的实时性能监控数据")
    public Result<Map<String, Object>> getRealTimeMetrics() {
        try {
            Map<String, Object> metrics = performanceMonitorService.getRealTimeMetrics();
            return Result.success(metrics);
        } catch (Exception e) {
            log.error("获取实时性能数据失败: {}", e.getMessage(), e);
            return Result.error("获取实时性能数据失败");
        }
    }

    @GetMapping("/summary")
    @Operation(summary = "获取性能统计摘要", description = "获取指定时间段内的性能统计摘要")
    @Parameter(name = "hours", description = "统计时间范围(小时)", example = "24")
    public Result<Map<String, Object>> getPerformanceSummary(
            @RequestParam(defaultValue = "24") int hours) {
        try {
            if (hours <= 0 || hours > 168) { // 最多1周
                return Result.error("时间范围必须在1-168小时之间");
            }

            Map<String, Object> summary = performanceMonitorService.getPerformanceSummary(hours);
            return Result.success(summary);
        } catch (Exception e) {
            log.error("获取性能摘要失败: hours={}, error={}", hours, e.getMessage(), e);
            return Result.error("获取性能摘要失败");
        }
    }

    @GetMapping("/metrics")
    @Operation(summary = "查询性能指标", description = "根据指标名称和时间范围查询性能指标")
    public Result<List<PerformanceMonitorService.PerformanceMetric>> getMetrics(
            @Parameter(description = "指标名称") @RequestParam(required = false) String metricName,
            @Parameter(description = "开始时间") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            // 默认查询最近1小时的数据
            if (startTime == null) {
                startTime = LocalDateTime.now().minusHours(1);
            }
            if (endTime == null) {
                endTime = LocalDateTime.now();
            }

            if (startTime.isAfter(endTime)) {
                return Result.error("开始时间不能晚于结束时间");
            }

            List<PerformanceMonitorService.PerformanceMetric> metrics =
                    performanceMonitorService.getMetrics(metricName, startTime, endTime);

            return Result.success(metrics);
        } catch (Exception e) {
            log.error("查询性能指标失败: metric={}, error={}", metricName, e.getMessage(), e);
            return Result.error("查询性能指标失败");
        }
    }

    @GetMapping("/alerts")
    @Operation(summary = "检查性能告警", description = "检查当前系统是否存在性能告警")
    public Result<Map<String, Object>> checkPerformanceAlerts() {
        try {
            List<String> alerts = performanceMonitorService.checkPerformanceAlerts();

            Map<String, Object> result = new HashMap<>();
            result.put("alert_count", alerts.size());
            result.put("alerts", alerts);
            result.put("has_alerts", !alerts.isEmpty());
            result.put("check_time", LocalDateTime.now());

            return Result.success(result);
        } catch (Exception e) {
            log.error("检查性能告警失败: {}", e.getMessage(), e);
            return Result.error("检查性能告警失败");
        }
    }

    @PostMapping("/profiling/start")
    @Operation(summary = "开始性能分析", description = "开始一个新的性能分析会话")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> startProfiling(
            @Parameter(description = "会话ID") @RequestParam String sessionId,
            @Parameter(description = "分析描述") @RequestParam(required = false) String description) {
        try {
            if (sessionId == null || sessionId.trim().isEmpty()) {
                return Result.error("会话ID不能为空");
            }

            if (description == null) {
                description = "性能分析会话";
            }

            performanceMonitorService.startProfilingSession(sessionId, description);

            Map<String, Object> result = new HashMap<>();
            result.put("session_id", sessionId);
            result.put("description", description);
            result.put("start_time", LocalDateTime.now());
            result.put("status", "started");

            log.info("开始性能分析会话: sessionId={}", sessionId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("开始性能分析失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            return Result.error("开始性能分析失败");
        }
    }

    @PostMapping("/profiling/end")
    @Operation(summary = "结束性能分析", description = "结束指定的性能分析会话并获取分析报告")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> endProfiling(
            @Parameter(description = "会话ID") @RequestParam String sessionId) {
        try {
            if (sessionId == null || sessionId.trim().isEmpty()) {
                return Result.error("会话ID不能为空");
            }

            Map<String, Object> report = performanceMonitorService.endProfilingSession(sessionId);

            if (report.containsKey("error")) {
                return Result.error(report.get("error").toString());
            }

            log.info("结束性能分析会话: sessionId={}", sessionId);
            return Result.success(report);
        } catch (Exception e) {
            log.error("结束性能分析失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            return Result.error("结束性能分析失败");
        }
    }

    @PostMapping("/metrics")
    @Operation(summary = "记录性能指标", description = "手动记录一个性能指标")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> recordMetric(
            @Parameter(description = "指标名称") @RequestParam String metricName,
            @Parameter(description = "指标类型") @RequestParam String metricType,
            @Parameter(description = "指标值") @RequestParam double value,
            @Parameter(description = "单位") @RequestParam(defaultValue = "") String unit,
            @Parameter(description = "标签") @RequestParam(required = false) Map<String, String> tags) {
        try {
            if (metricName == null || metricName.trim().isEmpty()) {
                return Result.error("指标名称不能为空");
            }

            if (tags == null) {
                tags = new HashMap<>();
            }

            performanceMonitorService.recordMetric(metricName, metricType, value, unit, tags);

            log.debug("手动记录性能指标: {}={} {}", metricName, value, unit);
            return Result.success("指标记录成功");
        } catch (Exception e) {
            log.error("记录性能指标失败: metric={}, error={}", metricName, e.getMessage(), e);
            return Result.error("记录性能指标失败");
        }
    }

    @DeleteMapping("/metrics/cleanup")
    @Operation(summary = "清理过期指标", description = "清理指定时间之前的过期性能指标数据")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> cleanupOldMetrics(
            @Parameter(description = "保留时间(小时)") @RequestParam(defaultValue = "24") int retentionHours) {
        try {
            if (retentionHours <= 0 || retentionHours > 8760) { // 最多1年
                return Result.error("保留时间必须在1-8760小时之间");
            }

            long cleanedCount = performanceMonitorService.cleanupOldMetrics(retentionHours);

            Map<String, Object> result = new HashMap<>();
            result.put("cleaned_count", cleanedCount);
            result.put("retention_hours", retentionHours);
            result.put("cleanup_time", LocalDateTime.now());

            log.info("清理过期性能数据: {} 条记录", cleanedCount);
            return Result.success(result);
        } catch (Exception e) {
            log.error("清理性能数据失败: error={}", e.getMessage(), e);
            return Result.error("清理性能数据失败");
        }
    }
}