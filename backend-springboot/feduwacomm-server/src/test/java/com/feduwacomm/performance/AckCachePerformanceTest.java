package com.feduwacomm.performance;

import com.feduwacomm.entity.VmAckTracking;
import com.feduwacomm.service.AckCachePerformanceMonitor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ACK缓存性能测试
 * 测试ACK缓存系统在各种负载下的性能表现
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-29
 */
@Slf4j
@SpringBootTest
class AckCachePerformanceTest {

    private static final int THREAD_COUNT = 10;
    private static final int OPERATIONS_PER_THREAD = 1000;
    private static final int TOTAL_OPERATIONS = THREAD_COUNT * OPERATIONS_PER_THREAD;

    /**
     * 性能监控并发测试
     */
    @Test
    @Disabled("性能测试 - 手动运行")
    void testPerformanceMonitorConcurrency() throws InterruptedException {
        log.info("开始性能监控并发测试...");

        AckCachePerformanceMonitor monitor = new AckCachePerformanceMonitor();
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger operationCounter = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // 启动多个线程进行并发操作
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        int opId = operationCounter.incrementAndGet();

                        // 模拟不同类型的操作
                        switch (opId % 10) {
                            case 0, 1, 2, 3, 4, 5 -> {
                                // 60% 缓存命中
                                monitor.recordCacheHit();
                                monitor.recordOperationTime("cache_get", 1 + (opId % 5));
                            }
                            case 6, 7 -> {
                                // 20% 缓存未命中
                                monitor.recordCacheMiss();
                                monitor.recordDatabaseQuery();
                                monitor.recordOperationTime("database_query", 10 + (opId % 20));
                            }
                            case 8 -> {
                                // 10% 缓存写入
                                monitor.recordCacheWrite();
                                monitor.recordEventPublication();
                                monitor.recordOperationTime("cache_set", 2 + (opId % 3));
                            }
                            case 9 -> {
                                // 10% 错误情况
                                if (opId % 100 == 0) {
                                    monitor.recordCacheError();
                                } else {
                                    monitor.recordCacheHit();
                                }
                            }
                        }

                        // 模拟一些处理延迟
                        if (j % 100 == 0) {
                            Thread.sleep(1);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("线程 {} 被中断", threadId);
                } finally {
                    latch.countDown();
                }
            });
        }

        // 等待所有线程完成
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        executor.shutdown();

        // 输出性能测试结果
        if (completed) {
            log.info("=== 性能测试结果 ===");
            log.info("测试持续时间: {}ms", duration);
            log.info("总操作数: {}", TOTAL_OPERATIONS);
            log.info("操作吞吐量: {:.2f} ops/sec", (double) TOTAL_OPERATIONS / duration * 1000);

            monitor.printPerformanceReport();

            // 验证基本指标
            var summary = monitor.getPerformanceSummary();
            log.info("缓存命中率: {:.2f}%", summary.getCacheHitRate());
            log.info("总错误率: {:.2f}%", summary.getErrorRate());

            // 性能断言
            assert summary.getCacheHitRate() > 50.0 : "缓存命中率应该大于50%";
            assert summary.getErrorRate() < 5.0 : "错误率应该小于5%";
            assert duration < 10000 : "测试应该在10秒内完成";

        } else {
            log.error("性能测试超时！");
            assert false : "性能测试超时";
        }
    }

    /**
     * 内存使用测试
     */
    @Test
    @Disabled("内存测试 - 手动运行")
    void testMemoryUsage() {
        log.info("开始内存使用测试...");

        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        AckCachePerformanceMonitor monitor = new AckCachePerformanceMonitor();

        // 模拟大量操作
        for (int i = 0; i < 100000; i++) {
            monitor.recordCacheHit();
            monitor.recordOperationTime("test_operation", i % 100);

            if (i % 10000 == 0) {
                System.gc(); // 建议垃圾回收
                long currentMemory = runtime.totalMemory() - runtime.freeMemory();
                long memoryUsed = currentMemory - initialMemory;
                log.info("操作 {}: 内存使用 {} MB", i, memoryUsed / 1024 / 1024);
            }
        }

        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long totalMemoryUsed = finalMemory - initialMemory;

        log.info("=== 内存使用测试结果 ===");
        log.info("初始内存: {} MB", initialMemory / 1024 / 1024);
        log.info("最终内存: {} MB", finalMemory / 1024 / 1024);
        log.info("总内存增长: {} MB", totalMemoryUsed / 1024 / 1024);

        // 内存使用断言
        assert totalMemoryUsed < 100 * 1024 * 1024 : "内存使用不应该超过100MB";
    }

    /**
     * 响应时间分布测试
     */
    @Test
    @Disabled("响应时间测试 - 手动运行")
    void testResponseTimeDistribution() throws InterruptedException {
        log.info("开始响应时间分布测试...");

        AckCachePerformanceMonitor monitor = new AckCachePerformanceMonitor();

        // 模拟不同响应时间的操作
        int[] responseTimes = {1, 2, 3, 5, 8, 13, 21, 34, 55, 89}; // 斐波那契数列

        for (int i = 0; i < 1000; i++) {
            int responseTime = responseTimes[i % responseTimes.length];

            // 模拟操作处理时间
            Thread.sleep(responseTime);

            monitor.recordOperationTime("simulated_operation", responseTime);
        }

        // 分析响应时间分布
        var summary = monitor.getPerformanceSummary();
        var operationStats = summary.getOperationStats();

        if (operationStats.containsKey("simulated_operation")) {
            var stats = operationStats.get("simulated_operation");
            log.info("=== 响应时间分布测试结果 ===");
            log.info("操作次数: {}", stats.get("count"));
            log.info("平均响应时间: {:.2f}ms", stats.get("averageTime"));
            log.info("最小响应时间: {}ms", stats.get("minTime"));
            log.info("最大响应时间: {}ms", stats.get("maxTime"));

            // 响应时间断言
            Double avgTime = (Double) stats.get("averageTime");
            assert avgTime > 0 : "平均响应时间应该大于0";
            assert avgTime < 100 : "平均响应时间不应该超过100ms";
        }
    }

    /**
     * 负载压力测试
     */
    @Test
    @Disabled("压力测试 - 手动运行")
    void testLoadStress() throws InterruptedException {
        log.info("开始负载压力测试...");

        AckCachePerformanceMonitor monitor = new AckCachePerformanceMonitor();
        int maxThreads = 50;
        int operationsPerThread = 500;

        ExecutorService executor = Executors.newFixedThreadPool(maxThreads);
        CountDownLatch latch = new CountDownLatch(maxThreads);

        long startTime = System.currentTimeMillis();

        // 逐步增加负载
        for (int threadCount = 1; threadCount <= maxThreads; threadCount++) {
            final int currentThreadCount = threadCount;

            executor.submit(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        // 测量操作执行时间
                        long opStart = System.nanoTime();

                        // 模拟不同类型的ACK操作
                        switch (i % 4) {
                            case 0 -> monitor.recordCacheHit();
                            case 1 -> monitor.recordCacheWrite();
                            case 2 -> monitor.recordEventPublication();
                            case 3 -> monitor.recordDatabaseQuery();
                        }

                        long opDuration = (System.nanoTime() - opStart) / 1_000_000; // 转换为毫秒
                        monitor.recordOperationTime("stress_operation", opDuration);

                        // 在高负载下添加小的延迟
                        if (currentThreadCount > 30 && i % 10 == 0) {
                            Thread.sleep(1);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(60, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        executor.shutdown();

        if (completed) {
            log.info("=== 负载压力测试结果 ===");
            log.info("最大并发线程数: {}", maxThreads);
            log.info("每线程操作数: {}", operationsPerThread);
            log.info("总操作数: {}", maxThreads * operationsPerThread);
            log.info("测试持续时间: {}ms", endTime - startTime);

            monitor.printPerformanceReport();

            // 压力测试断言
            var summary = monitor.getPerformanceSummary();
            assert summary.getTotalOperations() > 0 : "应该记录到操作";
            assert summary.getErrorRate() < 10.0 : "在压力测试下错误率应该小于10%";

        } else {
            log.error("负载压力测试超时！");
            assert false : "压力测试超时";
        }
    }
}