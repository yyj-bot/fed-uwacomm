package com.feduwacomm.performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.aggregation.UniversalAggregationEngine;
import com.feduwacomm.entity.VmRoundModel;
import com.feduwacomm.enums.FederatedAlgorithm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

/**
 * 联邦学习聚合性能测试
 *
 * 测试聚合引擎在各种负载条件下的性能：
 * - 大规模模型聚合性能
 * - 并发聚合安全性
 * - 内存使用效率
 * - 不同算法的性能对比
 * - 极限条件测试
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("联邦学习聚合性能测试")
class FederatedAggregationPerformanceTest {

    @Autowired
    private UniversalAggregationEngine aggregationEngine;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("测试大规模RandomForest聚合性能")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testLargeScaleRandomForestAggregation() {
        System.out.println("🚀 开始大规模RandomForest聚合性能测试");

        // 测试不同规模的性能表现
        int[] modelCounts = {10, 50, 100, 200};
        Map<Integer, Long> performanceResults = new HashMap<>();

        for (int modelCount : modelCounts) {
            // Given - 创建指定数量的RandomForest模型
            List<VmRoundModel> models = createRandomForestModels(modelCount);
            Map<String, Object> taskConfig = createPerformanceTaskConfig();

            // When - 执行聚合并测量时间
            long startTime = System.currentTimeMillis();
            UniversalAggregationEngine.AggregationResult result =
                aggregationEngine.aggregate(models, FederatedAlgorithm.FEDERATED_AVERAGING, taskConfig);
            long duration = System.currentTimeMillis() - startTime;

            performanceResults.put(modelCount, duration);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getParticipantCount()).isEqualTo(modelCount);

            System.out.println(String.format("   %d个模型聚合耗时: %dms (平均%.2fms/模型)",
                modelCount, duration, duration / (double) modelCount));

            // 验证性能合理性（随模型数量线性增长）
            assertThat(duration).isLessThan(modelCount * 100L); // 每模型不超过100ms
        }

        // 验证性能呈线性增长趋势
        assertThat(performanceResults.get(200)).isGreaterThan(performanceResults.get(10));
        System.out.println("✅ 大规模聚合性能测试完成，性能表现符合预期");
    }

    @Test
    @DisplayName("测试并发聚合安全性")
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void testConcurrentAggregationSafety() throws InterruptedException {
        System.out.println("🚀 开始并发聚合安全性测试");

        // Given
        int threadCount = 20;
        int modelsPerThread = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        List<Future<TestResult>> futures = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // When - 并发执行聚合
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            Future<TestResult> future = executor.submit(() -> {
                try {
                    startLatch.await(); // 等待所有线程准备就绪

                    List<VmRoundModel> models = createRandomForestModels(modelsPerThread);
                    Map<String, Object> taskConfig = createPerformanceTaskConfig();
                    taskConfig.put("threadId", threadIndex);

                    long startTime = System.currentTimeMillis();
                    UniversalAggregationEngine.AggregationResult result =
                        aggregationEngine.aggregate(models, FederatedAlgorithm.FEDERATED_AVERAGING, taskConfig);
                    long duration = System.currentTimeMillis() - startTime;

                    return new TestResult(threadIndex, result.isSuccess(),
                        result.getParticipantCount(), duration, null);

                } catch (Exception e) {
                    return new TestResult(threadIndex, false, 0, 0, e.getMessage());
                } finally {
                    finishLatch.countDown();
                }
            });
            futures.add(future);
        }

        // 开始测试
        long testStartTime = System.currentTimeMillis();
        startLatch.countDown();
        finishLatch.await(60, TimeUnit.SECONDS);
        long totalTestTime = System.currentTimeMillis() - testStartTime;

        executor.shutdown();

        // Then - 验证所有聚合都成功
        List<TestResult> results = new ArrayList<>();
        for (Future<TestResult> future : futures) {
            try {
                TestResult result = future.get();
                results.add(result);

                assertThat(result.success).as("线程 %d 应该成功执行", result.threadId).isTrue();
                assertThat(result.participantCount).isEqualTo(modelsPerThread);
            } catch (Exception e) {
                fail("获取并发测试结果失败: " + e.getMessage());
            }
        }

        // 统计性能信息
        long totalDuration = results.stream().mapToLong(r -> r.duration).sum();
        long avgDuration = totalDuration / results.size();
        long maxDuration = results.stream().mapToLong(r -> r.duration).max().orElse(0);
        long minDuration = results.stream().mapToLong(r -> r.duration).min().orElse(0);

        System.out.println("✅ 并发聚合安全性测试完成:");
        System.out.println(String.format("   线程数: %d, 总用时: %dms", threadCount, totalTestTime));
        System.out.println(String.format("   平均聚合时间: %dms, 最大: %dms, 最小: %dms",
            avgDuration, maxDuration, minDuration));
        System.out.println(String.format("   总聚合次数: %d, 成功率: 100%%", results.size()));
    }

    @Test
    @DisplayName("测试内存使用效率")
    void testMemoryEfficiency() {
        System.out.println("🚀 开始内存使用效率测试");

        Runtime runtime = Runtime.getRuntime();

        // 强制垃圾回收，获取基准内存
        System.gc();
        Thread.yield();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        // 执行多次大规模聚合
        int iterationCount = 20;
        int modelsPerIteration = 100;

        for (int i = 0; i < iterationCount; i++) {
            List<VmRoundModel> models = createRandomForestModels(modelsPerIteration);
            Map<String, Object> taskConfig = createPerformanceTaskConfig();
            taskConfig.put("iteration", i);

            UniversalAggregationEngine.AggregationResult result =
                aggregationEngine.aggregate(models, FederatedAlgorithm.FEDERATED_AVERAGING, taskConfig);

            assertThat(result.isSuccess()).isTrue();

            // 每5次迭代强制垃圾回收
            if (i % 5 == 0) {
                System.gc();
                Thread.yield();
            }
        }

        // 最终垃圾回收
        System.gc();
        Thread.yield();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;

        // 验证内存使用合理
        long maxMemoryMB = 200; // 最大允许200MB内存增长
        assertThat(memoryUsed).isLessThan(maxMemoryMB * 1024 * 1024);

        System.out.println("✅ 内存使用效率测试完成:");
        System.out.println(String.format("   执行 %d 次聚合，每次 %d 个模型",
            iterationCount, modelsPerIteration));
        System.out.println(String.format("   内存使用: %.2f MB (限制: %d MB)",
            memoryUsed / 1024.0 / 1024.0, maxMemoryMB));
    }

    @Test
    @DisplayName("测试不同算法性能对比")
    void testAlgorithmPerformanceComparison() {
        System.out.println("🚀 开始不同算法性能对比测试");

        List<VmRoundModel> models = createRandomForestModels(50);
        Map<String, Object> taskConfig = createPerformanceTaskConfig();

        FederatedAlgorithm[] algorithms = {
            FederatedAlgorithm.FEDERATED_AVERAGING,
            FederatedAlgorithm.FEDERATED_PROXIMAL,
            FederatedAlgorithm.FEDERATED_NOVA,
            FederatedAlgorithm.SCAFFOLD
        };

        Map<String, AlgorithmPerformance> performanceResults = new HashMap<>();

        for (FederatedAlgorithm algorithm : algorithms) {
            // 预热
            aggregationEngine.aggregate(models.subList(0, 5), algorithm, taskConfig);

            // 正式测试 - 多次测试取平均值
            List<Long> durations = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                long startTime = System.currentTimeMillis();
                UniversalAggregationEngine.AggregationResult result =
                    aggregationEngine.aggregate(models, algorithm, taskConfig);
                long duration = System.currentTimeMillis() - startTime;

                assertThat(result.isSuccess()).isTrue();
                durations.add(duration);
            }

            long avgDuration = (long) durations.stream().mapToLong(Long::longValue).average().orElse(0);
            long maxDuration = durations.stream().mapToLong(Long::longValue).max().orElse(0);
            long minDuration = durations.stream().mapToLong(Long::longValue).min().orElse(0);

            performanceResults.put(algorithm.name(),
                new AlgorithmPerformance(avgDuration, maxDuration, minDuration));
        }

        System.out.println("✅ 算法性能对比测试完成:");
        performanceResults.forEach((algorithm, perf) ->
            System.out.println(String.format("   %s: 平均%dms, 最大%dms, 最小%dms",
                algorithm, perf.avgDuration, perf.maxDuration, perf.minDuration)));
    }

    @Test
    @DisplayName("测试极限条件处理")
    void testExtremeConditions() {
        System.out.println("🚀 开始极限条件处理测试");

        Map<String, Object> taskConfig = createPerformanceTaskConfig();

        // 测试1: 单个模型聚合
        List<VmRoundModel> singleModel = createRandomForestModels(1);
        UniversalAggregationEngine.AggregationResult singleResult =
            aggregationEngine.aggregate(singleModel, FederatedAlgorithm.FEDERATED_AVERAGING, taskConfig);
        assertThat(singleResult.isSuccess()).isTrue();
        assertThat(singleResult.getParticipantCount()).isEqualTo(1);

        // 测试2: 大量特征的模型
        List<VmRoundModel> highDimensionModels = createHighDimensionModels(10, 1000);
        UniversalAggregationEngine.AggregationResult highDimResult =
            aggregationEngine.aggregate(highDimensionModels, FederatedAlgorithm.FEDERATED_AVERAGING, taskConfig);
        assertThat(highDimResult.isSuccess()).isTrue();

        // 测试3: 极小样本数据
        List<VmRoundModel> smallSampleModels = createSmallSampleModels(5);
        UniversalAggregationEngine.AggregationResult smallSampleResult =
            aggregationEngine.aggregate(smallSampleModels, FederatedAlgorithm.FEDERATED_AVERAGING, taskConfig);
        assertThat(smallSampleResult.isSuccess()).isTrue();

        System.out.println("✅ 极限条件处理测试完成:");
        System.out.println("   - 单模型聚合: ✅");
        System.out.println("   - 高维特征聚合: ✅");
        System.out.println("   - 小样本聚合: ✅");
    }

    // ================= 测试数据构建方法 =================

    private List<VmRoundModel> createRandomForestModels(int count) {
        List<VmRoundModel> models = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            VmRoundModel model = new VmRoundModel();
            model.setId("perf-rf-" + i);
            model.setTaskId("performance-test");
            model.setVmId("vm-perf-" + i);
            model.setRoundNumber(1);
            model.setAccuracy(BigDecimal.valueOf(0.75 + Math.random() * 0.20));
            model.setLoss(BigDecimal.valueOf(0.05 + Math.random() * 0.15));

            List<Double> importances = Arrays.asList(
                0.2 + Math.random() * 0.1,
                0.25 + Math.random() * 0.1,
                0.3 + Math.random() * 0.1,
                0.25 + Math.random() * 0.1
            );

            Map<String, Object> params = Map.of(
                "model_parameters", Map.of(
                    "feature_importances_", importances,
                    "n_estimators", 100 + (int)(Math.random() * 50)
                ),
                "training_metadata", Map.of(
                    "algorithm", "RandomForest",
                    "framework", "sklearn",
                    "samples_count", 500 + (int)(Math.random() * 1500)
                )
            );
            model.setParameters(toJson(params));
            models.add(model);
        }

        return models;
    }

    private List<VmRoundModel> createHighDimensionModels(int modelCount, int featureCount) {
        List<VmRoundModel> models = new ArrayList<>();

        for (int i = 0; i < modelCount; i++) {
            VmRoundModel model = new VmRoundModel();
            model.setId("high-dim-" + i);
            model.setTaskId("high-dimension-test");
            model.setVmId("vm-hd-" + i);
            model.setRoundNumber(1);
            model.setAccuracy(BigDecimal.valueOf(0.80 + Math.random() * 0.15));

            // 创建高维特征重要性
            List<Double> importances = new ArrayList<>();
            for (int j = 0; j < featureCount; j++) {
                importances.add(Math.random());
            }

            Map<String, Object> params = Map.of(
                "model_parameters", Map.of(
                    "feature_importances_", importances,
                    "n_estimators", 100
                ),
                "training_metadata", Map.of(
                    "algorithm", "RandomForest",
                    "framework", "sklearn",
                    "samples_count", 1000 + i * 100,
                    "feature_count", featureCount
                )
            );
            model.setParameters(toJson(params));
            models.add(model);
        }

        return models;
    }

    private List<VmRoundModel> createSmallSampleModels(int modelCount) {
        List<VmRoundModel> models = new ArrayList<>();

        for (int i = 0; i < modelCount; i++) {
            VmRoundModel model = new VmRoundModel();
            model.setId("small-sample-" + i);
            model.setTaskId("small-sample-test");
            model.setVmId("vm-ss-" + i);
            model.setRoundNumber(1);
            model.setAccuracy(BigDecimal.valueOf(0.60 + Math.random() * 0.25)); // 小样本可能准确率较低

            Map<String, Object> params = Map.of(
                "model_parameters", Map.of(
                    "feature_importances_", Arrays.asList(0.25, 0.25, 0.25, 0.25),
                    "n_estimators", 50
                ),
                "training_metadata", Map.of(
                    "algorithm", "RandomForest",
                    "framework", "sklearn",
                    "samples_count", 10 + i * 5 // 极小样本量
                )
            );
            model.setParameters(toJson(params));
            models.add(model);
        }

        return models;
    }

    private Map<String, Object> createPerformanceTaskConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("taskId", "performance-test");
        config.put("roundNumber", 1);
        config.put("algorithm", "FEDERATED_AVERAGING");
        config.put("minParticipants", 1);
        config.put("performanceTest", true);
        return config;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    // ================= 内部类 =================

    private static class TestResult {
        final int threadId;
        final boolean success;
        final int participantCount;
        final long duration;
        final String errorMessage;

        TestResult(int threadId, boolean success, int participantCount, long duration, String errorMessage) {
            this.threadId = threadId;
            this.success = success;
            this.participantCount = participantCount;
            this.duration = duration;
            this.errorMessage = errorMessage;
        }
    }

    private static class AlgorithmPerformance {
        final long avgDuration;
        final long maxDuration;
        final long minDuration;

        AlgorithmPerformance(long avgDuration, long maxDuration, long minDuration) {
            this.avgDuration = avgDuration;
            this.maxDuration = maxDuration;
            this.minDuration = minDuration;
        }
    }
}