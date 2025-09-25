package com.feduwacomm.aggregation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.entity.VmRoundModel;
import com.feduwacomm.enums.FederatedAlgorithm;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用聚合引擎 - 支持多种模型类型的联邦学习聚合
 *
 * 功能特点：
 * 1. 支持RandomForest和神经网络等多种模型类型
 * 2. 自动检测模型类型并选择合适的聚合策略
 * 3. 基于现有数据库枚举实现多种联邦学习算法
 * 4. 提供详细的聚合结果和性能指标
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UniversalAggregationEngine {

    private final AggregationStrategyFactory strategyFactory;
    private final ObjectMapper objectMapper;

    /**
     * 聚合结果封装类
     */
    @Data
    @Builder
    public static class AggregationResult {
        private boolean success;
        private Map<String, Object> globalParameters;
        private Map<String, Double> globalMetrics;
        private int participantCount;
        private long aggregationDuration;
        private String algorithm;
        private String errorMessage;
        private String modelType;

        public static AggregationResult success(Map<String, Object> globalParameters,
                                              Map<String, Double> globalMetrics,
                                              int participantCount,
                                              long aggregationDuration,
                                              String algorithm,
                                              String modelType) {
            return AggregationResult.builder()
                    .success(true)
                    .globalParameters(globalParameters)
                    .globalMetrics(globalMetrics)
                    .participantCount(participantCount)
                    .aggregationDuration(aggregationDuration)
                    .algorithm(algorithm)
                    .modelType(modelType)
                    .build();
        }

        public static AggregationResult failure(String errorMessage) {
            return AggregationResult.builder()
                    .success(false)
                    .errorMessage(errorMessage)
                    .build();
        }
    }

    /**
     * 通用聚合方法 - 支持多种模型类型
     *
     * @param models 本地模型列表
     * @param algorithm 联邦学习算法
     * @param taskConfig 任务配置
     * @return 聚合结果
     */
    public AggregationResult aggregate(List<VmRoundModel> models,
                                     FederatedAlgorithm algorithm,
                                     Map<String, Object> taskConfig) {

        if (models == null || models.isEmpty()) {
            return AggregationResult.failure("模型列表不能为空");
        }

        long startTime = System.currentTimeMillis();

        try {
            log.info("开始通用聚合: 算法={}, 模型数量={}", algorithm, models.size());

            // 检测模型类型
            String modelType = detectModelType(models);
            log.info("检测到模型类型: {}", modelType);

            // 验证模型一致性
            if (!validateModelConsistency(models)) {
                return AggregationResult.failure("模型参数结构不一致");
            }

            // 获取聚合策略
            AggregationStrategy strategy = strategyFactory.getStrategy(algorithm);

            // 执行聚合
            Map<String, Object> aggregatedParams = strategy.aggregate(models, taskConfig);

            // 计算全局指标
            Map<String, Double> globalMetrics = calculateGlobalMetrics(models, aggregatedParams);

            long duration = System.currentTimeMillis() - startTime;

            log.info("聚合成功完成: 模型类型={}, 参与者={}, 耗时={}ms",
                    modelType, models.size(), duration);

            return AggregationResult.success(
                    aggregatedParams,
                    globalMetrics,
                    models.size(),
                    duration,
                    algorithm.name(),
                    modelType
            );

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("聚合执行失败: 算法={}, 错误={}, 耗时={}ms",
                    algorithm, e.getMessage(), duration, e);
            return AggregationResult.failure("聚合执行异常: " + e.getMessage());
        }
    }

    /**
     * 自动检测模型类型
     * 通过分析第一个模型的参数结构来判断模型类型
     */
    private String detectModelType(List<VmRoundModel> models) {
        if (models.isEmpty()) {
            return "Unknown";
        }

        try {
            VmRoundModel firstModel = models.get(0);
            Map<String, Object> params = parseParameters(firstModel);

            if (params == null || params.isEmpty()) {
                return "Unknown";
            }

            // 检查是否为RandomForest模型
            if (containsRandomForestParams(params)) {
                return "RandomForest";
            }

            // 检查是否为神经网络模型
            if (containsNeuralNetworkParams(params)) {
                return "NeuralNetwork";
            }

            // 通过训练元数据判断
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) params.get("training_metadata");
            if (metadata != null) {
                String algorithm = (String) metadata.get("algorithm");
                if (algorithm != null) {
                    return algorithm;
                }
            }

            return "Unknown";

        } catch (Exception e) {
            log.warn("模型类型检测失败: {}", e.getMessage());
            return "Unknown";
        }
    }

    /**
     * 检查是否包含RandomForest参数
     */
    private boolean containsRandomForestParams(Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        Map<String, Object> modelParams = (Map<String, Object>) params.get("model_parameters");

        if (modelParams == null) {
            return false;
        }

        return modelParams.containsKey("feature_importances_") ||
               modelParams.containsKey("n_estimators");
    }

    /**
     * 检查是否包含神经网络参数
     */
    private boolean containsNeuralNetworkParams(Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        Map<String, Object> modelParams = (Map<String, Object>) params.get("model_parameters");

        if (modelParams == null) {
            return false;
        }

        return modelParams.containsKey("weights") ||
               modelParams.containsKey("layers") ||
               modelParams.containsKey("biases");
    }

    /**
     * 验证模型参数结构一致性
     */
    private boolean validateModelConsistency(List<VmRoundModel> models) {
        if (models.size() <= 1) {
            return true;
        }

        try {
            String referenceType = detectModelType(List.of(models.get(0)));

            for (VmRoundModel model : models) {
                String modelType = detectModelType(List.of(model));
                if (!referenceType.equals(modelType)) {
                    log.warn("检测到不一致的模型类型: 期望={}, 实际={}", referenceType, modelType);
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            log.error("模型一致性验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 计算全局聚合指标
     */
    private Map<String, Double> calculateGlobalMetrics(List<VmRoundModel> models,
                                                      Map<String, Object> aggregatedParams) {
        Map<String, Double> metrics = new HashMap<>();

        try {
            // 计算平均准确率
            double totalAccuracy = 0;
            int validAccuracyCount = 0;

            // 计算平均损失
            double totalLoss = 0;
            int validLossCount = 0;

            // 计算总样本数
            int totalSamples = 0;

            for (VmRoundModel model : models) {
                // 累计准确率
                if (model.getAccuracy() != null) {
                    totalAccuracy += model.getAccuracy().doubleValue();
                    validAccuracyCount++;
                }

                // 累计损失
                if (model.getLoss() != null) {
                    totalLoss += model.getLoss().doubleValue();
                    validLossCount++;
                }

                // 累计样本数
                Map<String, Object> params = parseParameters(model);
                if (params != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> metadata = (Map<String, Object>) params.get("training_metadata");
                    if (metadata != null && metadata.containsKey("samples_count")) {
                        Object samplesObj = metadata.get("samples_count");
                        if (samplesObj instanceof Number) {
                            totalSamples += ((Number) samplesObj).intValue();
                        }
                    }
                }
            }

            // 计算平均值
            if (validAccuracyCount > 0) {
                metrics.put("average_accuracy", totalAccuracy / validAccuracyCount);
            }

            if (validLossCount > 0) {
                metrics.put("average_loss", totalLoss / validLossCount);
            }

            metrics.put("total_samples", (double) totalSamples);
            metrics.put("participant_count", (double) models.size());

            // 添加聚合特定的指标
            metrics.put("aggregation_quality", calculateAggregationQuality(models));

        } catch (Exception e) {
            log.warn("计算全局指标失败: {}", e.getMessage());
        }

        return metrics;
    }

    /**
     * 计算聚合质量指标
     */
    private double calculateAggregationQuality(List<VmRoundModel> models) {
        try {
            // 简单的质量指标：基于准确率方差
            List<Double> accuracies = models.stream()
                    .filter(m -> m.getAccuracy() != null)
                    .map(m -> m.getAccuracy().doubleValue())
                    .toList();

            if (accuracies.size() < 2) {
                return 1.0; // 单个模型默认质量为1
            }

            double mean = accuracies.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double variance = accuracies.stream()
                    .mapToDouble(acc -> Math.pow(acc - mean, 2))
                    .average().orElse(0);

            // 方差越小，质量越高 (使用负指数函数映射到0-1区间)
            return Math.exp(-variance * 10);

        } catch (Exception e) {
            log.warn("聚合质量计算失败: {}", e.getMessage());
            return 0.5; // 默认中等质量
        }
    }

    /**
     * 解析模型参数JSON
     */
    private Map<String, Object> parseParameters(VmRoundModel model) {
        try {
            String parametersJson = model.getParameters();
            if (parametersJson == null || parametersJson.trim().isEmpty()) {
                return new HashMap<>();
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> params = objectMapper.readValue(parametersJson, Map.class);
            return params != null ? params : new HashMap<>();

        } catch (Exception e) {
            log.warn("解析模型参数失败: vmId={}, error={}", model.getVmId(), e.getMessage());
            return new HashMap<>();
        }
    }
}