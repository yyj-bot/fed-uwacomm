package com.feduwacomm.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 性能监控服务接口
 * 提供系统性能监控、指标收集和分析功能
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-28
 */
public interface PerformanceMonitorService {

    /**
     * 性能指标数据
     */
    class PerformanceMetric {
        private final String metricName;
        private final String metricType;
        private final double value;
        private final String unit;
        private final LocalDateTime timestamp;
        private final Map<String, String> tags;

        public PerformanceMetric(String metricName, String metricType, double value,
                               String unit, LocalDateTime timestamp, Map<String, String> tags) {
            this.metricName = metricName;
            this.metricType = metricType;
            this.value = value;
            this.unit = unit;
            this.timestamp = timestamp;
            this.tags = tags;
        }

        public String getMetricName() { return metricName; }
        public String getMetricType() { return metricType; }
        public double getValue() { return value; }
        public String getUnit() { return unit; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public Map<String, String> getTags() { return tags; }

        @Override
        public String toString() {
            return String.format("PerformanceMetric{name='%s', type='%s', value=%.2f %s, time=%s}",
                    metricName, metricType, value, unit, timestamp);
        }
    }

    /**
     * 系统健康状态
     */
    class SystemHealth {
        private final String status;
        private final double cpuUsage;
        private final double memoryUsage;
        private final double diskUsage;
        private final long activeConnections;
        private final long totalRequests;
        private final double avgResponseTime;
        private final LocalDateTime timestamp;

        public SystemHealth(String status, double cpuUsage, double memoryUsage, double diskUsage,
                          long activeConnections, long totalRequests, double avgResponseTime,
                          LocalDateTime timestamp) {
            this.status = status;
            this.cpuUsage = cpuUsage;
            this.memoryUsage = memoryUsage;
            this.diskUsage = diskUsage;
            this.activeConnections = activeConnections;
            this.totalRequests = totalRequests;
            this.avgResponseTime = avgResponseTime;
            this.timestamp = timestamp;
        }

        public String getStatus() { return status; }
        public double getCpuUsage() { return cpuUsage; }
        public double getMemoryUsage() { return memoryUsage; }
        public double getDiskUsage() { return diskUsage; }
        public long getActiveConnections() { return activeConnections; }
        public long getTotalRequests() { return totalRequests; }
        public double getAvgResponseTime() { return avgResponseTime; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    /**
     * 记录性能指标
     *
     * @param metricName 指标名称
     * @param metricType 指标类型 (COUNTER, GAUGE, TIMER, HISTOGRAM)
     * @param value 指标值
     * @param unit 单位
     * @param tags 标签
     */
    void recordMetric(String metricName, String metricType, double value, String unit, Map<String, String> tags);

    /**
     * 记录方法执行时间
     *
     * @param methodName 方法名
     * @param executionTimeMs 执行时间(毫秒)
     * @param success 是否成功
     */
    void recordMethodExecution(String methodName, long executionTimeMs, boolean success);

    /**
     * 记录HTTP请求
     *
     * @param endpoint 端点
     * @param method HTTP方法
     * @param statusCode 状态码
     * @param responseTimeMs 响应时间(毫秒)
     */
    void recordHttpRequest(String endpoint, String method, int statusCode, long responseTimeMs);

    /**
     * 记录数据库操作
     *
     * @param operation 操作类型
     * @param table 表名
     * @param executionTimeMs 执行时间(毫秒)
     * @param success 是否成功
     */
    void recordDatabaseOperation(String operation, String table, long executionTimeMs, boolean success);

    /**
     * 记录缓存操作
     *
     * @param operation 操作类型(GET, PUT, REMOVE)
     * @param cacheKey 缓存键
     * @param hit 是否命中
     * @param executionTimeMs 执行时间(毫秒)
     */
    void recordCacheOperation(String operation, String cacheKey, boolean hit, long executionTimeMs);

    /**
     * 获取指定时间范围的性能指标
     *
     * @param metricName 指标名称
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 性能指标列表
     */
    List<PerformanceMetric> getMetrics(String metricName, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取系统健康状态
     *
     * @return 系统健康状态
     */
    SystemHealth getSystemHealth();

    /**
     * 获取性能统计摘要
     *
     * @param hours 统计小时数
     * @return 性能统计数据
     */
    Map<String, Object> getPerformanceSummary(int hours);

    /**
     * 清理过期的性能数据
     *
     * @param retentionHours 保留小时数
     * @return 清理的记录数
     */
    long cleanupOldMetrics(int retentionHours);

    /**
     * 检查性能告警
     *
     * @return 需要告警的性能问题列表
     */
    List<String> checkPerformanceAlerts();

    /**
     * 开始性能分析会话
     *
     * @param sessionId 会话ID
     * @param description 描述
     */
    void startProfilingSession(String sessionId, String description);

    /**
     * 结束性能分析会话
     *
     * @param sessionId 会话ID
     * @return 分析报告
     */
    Map<String, Object> endProfilingSession(String sessionId);

    /**
     * 获取实时性能数据
     *
     * @return 实时性能指标
     */
    Map<String, Object> getRealTimeMetrics();

    // === 数据分发相关监控方法 ===

    /**
     * 增加分发任务计数器
     *
     * @param taskId 任务ID
     * @param status 状态
     */
    void incrementDistributionTaskCounter(String taskId, String status);

    /**
     * 增加分发任务计数器（带VM ID）
     *
     * @param taskId 任务ID
     * @param vmId VM ID
     * @param status 状态
     */
    void incrementDistributionTaskCounter(String taskId, String vmId, String status);

    /**
     * 记录分发VM数量
     *
     * @param taskId 任务ID
     * @param vmCount VM数量
     */
    void recordDistributionVmCount(String taskId, Integer vmCount);

    /**
     * 记录分发数据大小
     *
     * @param taskId 任务ID
     * @param dataSize 数据大小
     */
    void recordDistributionDataSize(String taskId, Long dataSize);

    /**
     * 记录分发开始时间
     *
     * @param taskId 任务ID
     * @param startTime 开始时间
     */
    void recordDistributionStartTime(String taskId, long startTime);

    /**
     * 记录分发执行时间
     *
     * @param taskId 任务ID
     * @param executionTime 执行时间
     */
    void recordDistributionExecutionTime(String taskId, Long executionTime);

    /**
     * 记录分发结果
     *
     * @param taskId 任务ID
     * @param success 是否成功
     */
    void recordDistributionResult(String taskId, boolean success);

    /**
     * 记录数据传输速率
     *
     * @param taskId 任务ID
     * @param transferRate 传输速率
     */
    void recordDataTransferRate(String taskId, double transferRate);

    /**
     * 记录VM分发成功率
     *
     * @param taskId 任务ID
     * @param successRate 成功率
     */
    void recordVmDistributionSuccessRate(String taskId, double successRate);

    /**
     * 记录分发VM结果
     *
     * @param taskId 任务ID
     * @param successCount 成功数量
     * @param failedCount 失败数量
     */
    void recordDistributionVmResults(String taskId, Integer successCount, Integer failedCount);

    /**
     * 记录分发统计信息
     *
     * @param taskId 任务ID
     * @param statistics 统计信息
     */
    void recordDistributionStatistics(String taskId, Map<String, Object> statistics);

    /**
     * 记录分发数据集数量
     *
     * @param distributionStrategy 分发策略
     * @param datasetCount 数据集数量
     */
    void recordDistributionDatasetCount(String distributionStrategy, int datasetCount);

    /**
     * 记录手动干预请求
     *
     * @param orchestrationId 工作流ID
     * @param reason 原因
     * @param operator 操作者
     */
    void recordManualInterventionRequest(String orchestrationId, String reason, String operator);

    /**
     * 记录工作流终止
     *
     * @param orchestrationId 工作流ID
     * @param reason 终止原因
     * @param duration 持续时间
     */
    void recordWorkflowTermination(String orchestrationId, String reason, double duration);

    // === 工作流阶段相关监控方法 ===

    /**
     * 记录阶段执行时间
     *
     * @param stageName 阶段名称
     * @param executionTime 执行时间（毫秒）
     */
    void recordStageExecutionTime(String stageName, Long executionTime);

    /**
     * 记录阶段执行结果
     *
     * @param stageName 阶段名称
     * @param success 是否成功
     */
    void recordStageResult(String stageName, boolean success);

    /**
     * 记录工作流阶段指标
     *
     * @param orchestrationId 工作流ID
     * @param stageName 阶段名称
     * @param stageOrder 阶段顺序
     * @param executionTime 执行时间
     * @param success 是否成功
     */
    void recordWorkflowStageMetrics(String orchestrationId, String stageName,
                                  Integer stageOrder, Long executionTime, boolean success);

    /**
     * 记录数据处理指标
     *
     * @param stageName 阶段名称
     * @param dataType 数据类型（input/output）
     * @param dataSize 数据大小
     */
    void recordDataProcessingMetrics(String stageName, String dataType, int dataSize);

    /**
     * 记录资源使用情况
     *
     * @param stageName 阶段名称
     * @param resourceUsage 资源使用情况
     */
    void recordResourceUsage(String stageName, Map<String, Object> resourceUsage);

    /**
     * 更新吞吐量指标
     *
     * @param stageName 阶段名称
     * @param currentTime 当前时间
     */
    void updateThroughputMetrics(String stageName, long currentTime);

    /**
     * 记录错误指标
     *
     * @param stageName 阶段名称
     * @param errorCategory 错误类别
     */
    void recordErrorMetrics(String stageName, String errorCategory);

    /**
     * 记录并发性能指标
     *
     * @param orchestrationId 工作流ID
     * @param stageName 阶段名称
     * @param concurrentStageCount 并发阶段数量
     */
    void recordConcurrencyMetrics(String orchestrationId, String stageName, int concurrentStageCount);
}