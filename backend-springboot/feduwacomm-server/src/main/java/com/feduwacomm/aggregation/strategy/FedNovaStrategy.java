package com.feduwacomm.aggregation.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.aggregation.AggregationException;
import com.feduwacomm.aggregation.AggregationStrategy;
import com.feduwacomm.entity.VmRoundModel;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * FedNova 聚合策略
 *
 * 实现FedNova算法，处理客户端计算异构性：
 * - 基于有效批次数量归一化
 * - 支持不同的本地训练轮数
 *
 * 对应数据库枚举: FEDERATED_NOVA
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Slf4j
public class FedNovaStrategy implements AggregationStrategy {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FedAvgStrategy fedAvgStrategy = new FedAvgStrategy();

    @Override
    public Map<String, Object> aggregate(List<VmRoundModel> models, Map<String, Object> taskConfig)
            throws AggregationException {

        if (models == null || models.isEmpty()) {
            throw new AggregationException("模型列表不能为空");
        }

        log.info("开始FedNova聚合: 模型数量={}", models.size());

        try {
            // 计算有效批次数量权重
            List<Double> novaWeights = calculateNovaWeights(models, taskConfig);

            // 基于FedAvg执行聚合，但使用Nova权重
            Map<String, Object> result = executeNovaAggregation(models, novaWeights, taskConfig);

            result.put("aggregation_method", "FedNova");
            result.put("participants", models.size());

            log.info("FedNova聚合完成: 参与者={}", models.size());
            return result;

        } catch (Exception e) {
            log.error("FedNova聚合失败: {}", e.getMessage(), e);
            throw new AggregationException("FedNova聚合执行异常: " + e.getMessage(), e);
        }
    }

    @Override
    public String getStrategyName() {
        return "FedNova";
    }

    /**
     * 计算FedNova权重（基于有效批次数量）
     */
    private List<Double> calculateNovaWeights(List<VmRoundModel> models, Map<String, Object> taskConfig)
            throws AggregationException {

        List<Double> effectiveBatches = new ArrayList<>();

        // 提取每个客户端的有效批次数量
        for (VmRoundModel model : models) {
            Map<String, Object> params = parseParameters(model);
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) params.get("training_metadata");

            double effectiveBatch = calculateEffectiveBatch(metadata);
            effectiveBatches.add(effectiveBatch);
        }

        // 计算总有效批次
        double totalEffectiveBatches = effectiveBatches.stream()
                .mapToDouble(Double::doubleValue).sum();

        // 计算归一化权重
        List<Double> weights = new ArrayList<>();
        for (double batch : effectiveBatches) {
            weights.add(batch / totalEffectiveBatches);
        }

        log.debug("FedNova权重计算完成: 有效批次={}, 权重={}", effectiveBatches, weights);
        return weights;
    }

    /**
     * 计算有效批次数量
     */
    private double calculateEffectiveBatch(Map<String, Object> metadata) {
        if (metadata == null) {
            return 1.0; // 默认值
        }

        // 尝试从元数据获取训练轮数和批次信息
        double localEpochs = getDoubleValue(metadata, "local_epochs", 1.0);
        double batchSize = getDoubleValue(metadata, "batch_size", 32.0);
        double sampleCount = getDoubleValue(metadata, "samples_count", 1000.0);

        // 计算有效批次：本地轮数 * (样本数 / 批次大小)
        double batchesPerEpoch = Math.max(1.0, sampleCount / batchSize);
        return localEpochs * batchesPerEpoch;
    }

    /**
     * 执行Nova聚合
     */
    private Map<String, Object> executeNovaAggregation(List<VmRoundModel> models,
                                                      List<Double> novaWeights,
                                                      Map<String, Object> taskConfig)
            throws AggregationException {

        // 创建临时任务配置，传递Nova权重给FedAvg
        Map<String, Object> tempConfig = new HashMap<>();
        if (taskConfig != null) {
            tempConfig.putAll(taskConfig);
        }
        tempConfig.put("custom_weights", novaWeights);

        // 使用修改后的FedAvg进行聚合
        return fedAvgStrategy.aggregate(models, tempConfig);
    }

    /**
     * 解析模型参数
     */
    private Map<String, Object> parseParameters(VmRoundModel model) throws AggregationException {
        try {
            String parametersJson = model.getParameters();
            if (parametersJson == null || parametersJson.trim().isEmpty()) {
                return new HashMap<>();
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> params = objectMapper.readValue(parametersJson, Map.class);
            return params != null ? params : new HashMap<>();

        } catch (Exception e) {
            throw new AggregationException("解析模型参数失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取数值参数
     */
    private double getDoubleValue(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }
}