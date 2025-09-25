package com.feduwacomm.aggregation.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.aggregation.AggregationException;
import com.feduwacomm.aggregation.AggregationStrategy;
import com.feduwacomm.entity.VmRoundModel;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * FedAvg (联邦平均) 聚合策略
 *
 * 实现标准的联邦平均算法，支持：
 * - RandomForest特征重要性加权平均聚合
 * - 神经网络权重加权平均聚合
 * - 基于样本数量的权重计算
 *
 * 对应数据库枚举: FEDERATED_AVERAGING
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Slf4j
public class FedAvgStrategy implements AggregationStrategy {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> aggregate(List<VmRoundModel> models, Map<String, Object> taskConfig)
            throws AggregationException {

        if (models == null || models.isEmpty()) {
            throw new AggregationException("模型列表不能为空");
        }

        log.info("开始FedAvg聚合: 模型数量={}", models.size());

        try {
            // 计算权重（基于样本数量）
            List<Double> weights = calculateWeights(models);

            // 获取第一个模型的参数结构，用于确定模型类型
            Map<String, Object> firstParams = parseParameters(models.get(0));
            @SuppressWarnings("unchecked")
            Map<String, Object> modelParams = (Map<String, Object>) firstParams.get("model_parameters");

            if (modelParams == null) {
                throw new AggregationException("模型参数为空");
            }

            // 根据参数类型选择聚合方法
            if (isRandomForestModel(modelParams)) {
                return aggregateRandomForest(models, weights);
            } else if (isNeuralNetworkModel(modelParams)) {
                return aggregateNeuralNetwork(models, weights);
            } else {
                // 通用聚合方法
                return aggregateGeneric(models, weights);
            }

        } catch (Exception e) {
            log.error("FedAvg聚合失败: {}", e.getMessage(), e);
            throw new AggregationException("FedAvg聚合执行异常: " + e.getMessage(), e);
        }
    }

    @Override
    public String getStrategyName() {
        return "FedAvg";
    }

    /**
     * 计算基于样本数量的权重
     */
    private List<Double> calculateWeights(List<VmRoundModel> models) throws AggregationException {
        List<Integer> sampleCounts = new ArrayList<>();

        // 提取每个模型的样本数量
        for (VmRoundModel model : models) {
            Map<String, Object> params = parseParameters(model);
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) params.get("training_metadata");

            if (metadata != null && metadata.containsKey("samples_count")) {
                Object samplesObj = metadata.get("samples_count");
                if (samplesObj instanceof Number) {
                    sampleCounts.add(((Number) samplesObj).intValue());
                } else {
                    sampleCounts.add(1000); // 默认样本数
                }
            } else {
                sampleCounts.add(1000); // 默认样本数
            }
        }

        // 计算归一化权重
        int totalSamples = sampleCounts.stream().mapToInt(Integer::intValue).sum();
        List<Double> weights = sampleCounts.stream()
                .map(count -> count.doubleValue() / totalSamples)
                .collect(Collectors.toList());

        log.debug("计算权重完成: 样本数={}, 权重={}", sampleCounts, weights);
        return weights;
    }

    /**
     * RandomForest模型聚合
     */
    private Map<String, Object> aggregateRandomForest(List<VmRoundModel> models, List<Double> weights)
            throws AggregationException {

        log.info("执行RandomForest聚合");

        try {
            // 提取特征重要性
            List<List<Double>> importancesList = new ArrayList<>();
            Integer nEstimators = null;

            for (VmRoundModel model : models) {
                Map<String, Object> params = parseParameters(model);
                @SuppressWarnings("unchecked")
                Map<String, Object> modelParams = (Map<String, Object>) params.get("model_parameters");

                @SuppressWarnings("unchecked")
                List<Double> importances = (List<Double>) modelParams.get("feature_importances_");
                if (importances != null) {
                    importancesList.add(importances);
                }

                // 获取估计器数量
                if (nEstimators == null && modelParams.containsKey("n_estimators")) {
                    nEstimators = ((Number) modelParams.get("n_estimators")).intValue();
                }
            }

            if (importancesList.isEmpty()) {
                throw new AggregationException("没有有效的特征重要性数据");
            }

            // 验证特征数量一致性
            int featureCount = importancesList.get(0).size();
            for (List<Double> importances : importancesList) {
                if (importances.size() != featureCount) {
                    throw new AggregationException("特征重要性维度不一致");
                }
            }

            // 加权平均聚合特征重要性
            double[] globalImportances = new double[featureCount];
            for (int i = 0; i < models.size(); i++) {
                List<Double> localImportances = importancesList.get(i);
                double weight = weights.get(i);

                for (int j = 0; j < featureCount; j++) {
                    globalImportances[j] += weight * localImportances.get(j);
                }
            }

            // 归一化特征重要性
            double sum = Arrays.stream(globalImportances).sum();
            if (sum > 0) {
                for (int i = 0; i < globalImportances.length; i++) {
                    globalImportances[i] /= sum;
                }
            }

            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("feature_importances_",
                    Arrays.stream(globalImportances).boxed().collect(Collectors.toList()));
            result.put("n_estimators", nEstimators != null ? nEstimators : 100);
            result.put("aggregation_method", "FedAvg-RF");
            result.put("participants", models.size());

            log.info("RandomForest聚合完成: 特征数量={}, 参与者={}", featureCount, models.size());
            return result;

        } catch (Exception e) {
            throw new AggregationException("RandomForest聚合失败: " + e.getMessage(), e);
        }
    }

    /**
     * 神经网络模型聚合
     */
    private Map<String, Object> aggregateNeuralNetwork(List<VmRoundModel> models, List<Double> weights)
            throws AggregationException {

        log.info("执行神经网络聚合");

        try {
            // 提取权重参数
            List<Map<String, Object>> weightsList = new ArrayList<>();

            for (VmRoundModel model : models) {
                Map<String, Object> params = parseParameters(model);
                @SuppressWarnings("unchecked")
                Map<String, Object> modelParams = (Map<String, Object>) params.get("model_parameters");

                @SuppressWarnings("unchecked")
                Map<String, Object> weightsMap = (Map<String, Object>) modelParams.get("weights");
                if (weightsMap != null) {
                    weightsList.add(weightsMap);
                }
            }

            if (weightsList.isEmpty()) {
                throw new AggregationException("没有有效的神经网络权重数据");
            }

            // 获取参数结构
            Map<String, Object> referenceWeights = weightsList.get(0);
            Map<String, Object> aggregatedWeights = new HashMap<>();

            // 对每个参数层进行加权平均
            for (String layerName : referenceWeights.keySet()) {
                if (isNumericParameter(referenceWeights.get(layerName))) {
                    aggregatedWeights.put(layerName, aggregateNumericParameter(layerName, weightsList, weights));
                } else {
                    // 保持第一个模型的非数值参数
                    aggregatedWeights.put(layerName, referenceWeights.get(layerName));
                }
            }

            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("weights", aggregatedWeights);
            result.put("aggregation_method", "FedAvg-NN");
            result.put("participants", models.size());

            log.info("神经网络聚合完成: 参数层数={}, 参与者={}", aggregatedWeights.size(), models.size());
            return result;

        } catch (Exception e) {
            throw new AggregationException("神经网络聚合失败: " + e.getMessage(), e);
        }
    }

    /**
     * 通用模型聚合（用于未知模型类型）
     */
    private Map<String, Object> aggregateGeneric(List<VmRoundModel> models, List<Double> weights)
            throws AggregationException {

        log.info("执行通用模型聚合");

        // 简单策略：使用第一个模型的参数作为基础，添加聚合元信息
        Map<String, Object> firstParams = parseParameters(models.get(0));
        @SuppressWarnings("unchecked")
        Map<String, Object> result = new HashMap<>((Map<String, Object>) firstParams.get("model_parameters"));

        result.put("aggregation_method", "FedAvg-Generic");
        result.put("participants", models.size());

        log.info("通用模型聚合完成: 参与者={}", models.size());
        return result;
    }

    /**
     * 聚合数值参数
     */
    @SuppressWarnings("unchecked")
    private Object aggregateNumericParameter(String paramName, List<Map<String, Object>> weightsList,
                                           List<Double> weights) {
        Object firstParam = weightsList.get(0).get(paramName);

        if (firstParam instanceof List) {
            return aggregateListParameter((List<Object>) firstParam, paramName, weightsList, weights);
        } else if (firstParam instanceof Number) {
            return aggregateScalarParameter(paramName, weightsList, weights);
        }

        return firstParam; // 无法聚合的参数保持不变
    }

    /**
     * 聚合列表参数
     */
    @SuppressWarnings("unchecked")
    private List<Object> aggregateListParameter(List<Object> referenceList, String paramName,
                                              List<Map<String, Object>> weightsList, List<Double> weights) {
        if (referenceList.isEmpty()) {
            return referenceList;
        }

        List<Object> result = new ArrayList<>();

        for (int i = 0; i < referenceList.size(); i++) {
            if (referenceList.get(i) instanceof Number) {
                // 数值类型，进行加权平均
                double sum = 0.0;
                for (int j = 0; j < weightsList.size(); j++) {
                    List<Object> paramList = (List<Object>) weightsList.get(j).get(paramName);
                    if (paramList != null && i < paramList.size() && paramList.get(i) instanceof Number) {
                        sum += weights.get(j) * ((Number) paramList.get(i)).doubleValue();
                    }
                }
                result.add(sum);
            } else {
                // 非数值类型，保持第一个值
                result.add(referenceList.get(i));
            }
        }

        return result;
    }

    /**
     * 聚合标量参数
     */
    private Double aggregateScalarParameter(String paramName, List<Map<String, Object>> weightsList,
                                          List<Double> weights) {
        double sum = 0.0;
        for (int i = 0; i < weightsList.size(); i++) {
            Object param = weightsList.get(i).get(paramName);
            if (param instanceof Number) {
                sum += weights.get(i) * ((Number) param).doubleValue();
            }
        }
        return sum;
    }

    /**
     * 判断是否为RandomForest模型
     */
    private boolean isRandomForestModel(Map<String, Object> modelParams) {
        return modelParams.containsKey("feature_importances_") ||
               modelParams.containsKey("n_estimators");
    }

    /**
     * 判断是否为神经网络模型
     */
    private boolean isNeuralNetworkModel(Map<String, Object> modelParams) {
        return modelParams.containsKey("weights") ||
               modelParams.containsKey("layers") ||
               modelParams.containsKey("biases");
    }

    /**
     * 判断参数是否为数值类型
     */
    private boolean isNumericParameter(Object param) {
        if (param instanceof Number) {
            return true;
        }
        if (param instanceof List) {
            List<?> list = (List<?>) param;
            return !list.isEmpty() && (list.get(0) instanceof Number || list.get(0) instanceof List);
        }
        return false;
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
}