package com.feduwacomm.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.Map;

/**
 * ACK缓存性能监控服务
 * 收集和统计ACK缓存操作的性能指标
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-29
 */
@Slf4j
@Service
public class AckCachePerformanceMonitor {

    // 操作计数器
    private final LongAdder cacheHits = new LongAdder();
    private final LongAdder cacheMisses = new LongAdder();
    private final LongAdder cacheWrites = new LongAdder();
    private final LongAdder cacheEvictions = new LongAdder();
    private final LongAdder eventPublications = new LongAdder();
    private final LongAdder databaseQueries = new LongAdder();

    // 响应时间统计
    private final Map<String, ResponseTimeStats> operationStats = new ConcurrentHashMap<>();

    // 错误计数器
    private final LongAdder cacheErrors = new LongAdder();
    private final LongAdder eventErrors = new LongAdder();
    private final LongAdder databaseErrors = new LongAdder();

    // 启动时间
    private final LocalDateTime startTime = LocalDateTime.now();

    /**
     * 响应时间统计数据
     */
    @Data
    public static class ResponseTimeStats {
        private final AtomicLong totalTime = new AtomicLong(0);
        private final AtomicLong count = new AtomicLong(0);
        private final AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxTime = new AtomicLong(0);

        public void addSample(long timeMs) {
            totalTime.addAndGet(timeMs);
            count.incrementAndGet();
            minTime.updateAndGet(current -> Math.min(current, timeMs));
            maxTime.updateAndGet(current -> Math.max(current, timeMs));
        }

        public double getAverageTime() {
            long totalCount = count.get();
            return totalCount > 0 ? (double) totalTime.get() / totalCount : 0.0;
        }

        public long getMinTime() {
            long min = minTime.get();
            return min == Long.MAX_VALUE ? 0 : min;
        }
    }

    /**
     * 性能监控摘要
     */
    @Data
    public static class PerformanceSummary {
        private LocalDateTime startTime;
        private LocalDateTime reportTime;
        private long totalOperations;

        // 缓存统计
        private long cacheHits;
        private long cacheMisses;
        private long cacheWrites;
        private long cacheEvictions;
        private double cacheHitRate;

        // 事件统计
        private long eventPublications;
        private long eventErrors;

        // 数据库统计
        private long databaseQueries;
        private long databaseErrors;

        // 错误统计
        private long totalErrors;
        private double errorRate;

        // 响应时间统计
        private Map<String, Map<String, Object>> operationStats;
    }

    // ======================== 性能指标记录方法 ========================

    /**
     * 记录缓存命中
     */
    public void recordCacheHit() {
        cacheHits.increment();
    }

    /**
     * 记录缓存未命中
     */
    public void recordCacheMiss() {
        cacheMisses.increment();
    }

    /**
     * 记录缓存写入
     */
    public void recordCacheWrite() {
        cacheWrites.increment();
    }

    /**
     * 记录缓存清理
     */
    public void recordCacheEviction() {
        cacheEvictions.increment();
    }

    /**
     * 记录事件发布
     */
    public void recordEventPublication() {
        eventPublications.increment();
    }

    /**
     * 记录数据库查询
     */
    public void recordDatabaseQuery() {
        databaseQueries.increment();
    }

    /**
     * 记录缓存错误
     */
    public void recordCacheError() {
        cacheErrors.increment();
    }

    /**
     * 记录事件错误
     */
    public void recordEventError() {
        eventErrors.increment();
    }

    /**
     * 记录数据库错误
     */
    public void recordDatabaseError() {
        databaseErrors.increment();
    }

    /**
     * 记录操作响应时间
     */
    public void recordOperationTime(String operation, long timeMs) {
        operationStats.computeIfAbsent(operation, k -> new ResponseTimeStats())
                .addSample(timeMs);
    }

    /**
     * 创建性能监控包装器
     */
    public <T> T measureOperation(String operationName, PerformanceMonitoredOperation<T> operation) throws Exception {
        long startTime = System.currentTimeMillis();
        try {
            T result = operation.execute();
            long duration = System.currentTimeMillis() - startTime;
            recordOperationTime(operationName, duration);
            return result;
        } catch (Exception e) {
            recordCacheError();
            throw e;
        }
    }

    /**
     * 创建异步性能监控包装器
     */
    public void measureAsyncOperation(String operationName, Runnable operation) {
        long startTime = System.currentTimeMillis();
        try {
            operation.run();
            long duration = System.currentTimeMillis() - startTime;
            recordOperationTime(operationName, duration);
        } catch (Exception e) {
            recordCacheError();
            log.warn("异步操作执行失败: {}", operationName, e);
        }
    }

    // ======================== 统计信息获取方法 ========================

    /**
     * 获取缓存命中率
     */
    public double getCacheHitRate() {
        long hits = cacheHits.sum();
        long misses = cacheMisses.sum();
        long total = hits + misses;
        return total > 0 ? (double) hits / total * 100 : 0.0;
    }

    /**
     * 获取错误率
     */
    public double getErrorRate() {
        long totalOps = getTotalOperations();
        long totalErrors = getTotalErrors();
        return totalOps > 0 ? (double) totalErrors / totalOps * 100 : 0.0;
    }

    /**
     * 获取总操作数
     */
    public long getTotalOperations() {
        return cacheHits.sum() + cacheMisses.sum() + cacheWrites.sum() +
               eventPublications.sum() + databaseQueries.sum();
    }

    /**
     * 获取总错误数
     */
    public long getTotalErrors() {
        return cacheErrors.sum() + eventErrors.sum() + databaseErrors.sum();
    }

    /**
     * 获取操作统计信息
     */
    public Map<String, Map<String, Object>> getOperationStats() {
        Map<String, Map<String, Object>> stats = new ConcurrentHashMap<>();

        operationStats.forEach((operation, responseStats) -> {
            Map<String, Object> operationData = new ConcurrentHashMap<>();
            operationData.put("count", responseStats.getCount().get());
            operationData.put("totalTime", responseStats.getTotalTime().get());
            operationData.put("averageTime", responseStats.getAverageTime());
            operationData.put("minTime", responseStats.getMinTime());
            operationData.put("maxTime", responseStats.getMaxTime().get());
            stats.put(operation, operationData);
        });

        return stats;
    }

    /**
     * 获取性能摘要
     */
    public PerformanceSummary getPerformanceSummary() {
        PerformanceSummary summary = new PerformanceSummary();

        summary.setStartTime(startTime);
        summary.setReportTime(LocalDateTime.now());
        summary.setTotalOperations(getTotalOperations());

        // 缓存统计
        summary.setCacheHits(cacheHits.sum());
        summary.setCacheMisses(cacheMisses.sum());
        summary.setCacheWrites(cacheWrites.sum());
        summary.setCacheEvictions(cacheEvictions.sum());
        summary.setCacheHitRate(getCacheHitRate());

        // 事件统计
        summary.setEventPublications(eventPublications.sum());
        summary.setEventErrors(eventErrors.sum());

        // 数据库统计
        summary.setDatabaseQueries(databaseQueries.sum());
        summary.setDatabaseErrors(databaseErrors.sum());

        // 错误统计
        summary.setTotalErrors(getTotalErrors());
        summary.setErrorRate(getErrorRate());

        // 响应时间统计
        summary.setOperationStats(getOperationStats());

        return summary;
    }

    /**
     * 重置所有统计信息
     */
    public void reset() {
        cacheHits.reset();
        cacheMisses.reset();
        cacheWrites.reset();
        cacheEvictions.reset();
        eventPublications.reset();
        databaseQueries.reset();
        cacheErrors.reset();
        eventErrors.reset();
        databaseErrors.reset();
        operationStats.clear();

        log.info("ACK缓存性能监控统计信息已重置");
    }

    /**
     * 打印性能报告
     */
    public void printPerformanceReport() {
        PerformanceSummary summary = getPerformanceSummary();

        log.info("=== ACK缓存性能报告 ===");
        log.info("监控开始时间: {}", summary.getStartTime());
        log.info("报告生成时间: {}", summary.getReportTime());
        log.info("总操作数: {}", summary.getTotalOperations());
        log.info("缓存命中率: {:.2f}%", summary.getCacheHitRate());
        log.info("总错误率: {:.2f}%", summary.getErrorRate());

        log.info("--- 缓存统计 ---");
        log.info("缓存命中: {}", summary.getCacheHits());
        log.info("缓存未命中: {}", summary.getCacheMisses());
        log.info("缓存写入: {}", summary.getCacheWrites());
        log.info("缓存清理: {}", summary.getCacheEvictions());

        log.info("--- 事件统计 ---");
        log.info("事件发布: {}", summary.getEventPublications());
        log.info("事件错误: {}", summary.getEventErrors());

        log.info("--- 数据库统计 ---");
        log.info("数据库查询: {}", summary.getDatabaseQueries());
        log.info("数据库错误: {}", summary.getDatabaseErrors());

        log.info("--- 操作响应时间 ---");
        summary.getOperationStats().forEach((operation, stats) -> {
            log.info("{}: 次数={}, 平均={}ms, 最小={}ms, 最大={}ms",
                    operation, stats.get("count"),
                    String.format("%.2f", stats.get("averageTime")),
                    stats.get("minTime"), stats.get("maxTime"));
        });

        log.info("=== 报告结束 ===");
    }

    /**
     * 性能监控操作接口
     */
    @FunctionalInterface
    public interface PerformanceMonitoredOperation<T> {
        T execute() throws Exception;
    }
}