package com.feduwacomm.aggregation.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.aggregation.AggregationException;
import com.feduwacomm.aggregation.AggregationStrategy;
import com.feduwacomm.entity.VmRoundModel;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * FedProx (联邦近端) 聚合策略
 *
 * 实现FedProx算法，通过添加正则化项处理数据异构性：
 * - 支持RandomForest和神经网络模型
 * - 可配置正则化系数μ
 * - 基于FedAvg结果应用正则化修正
 *
 * 对应数据库枚举: FEDERATED_PROXIMAL
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Slf4j
public class FedProxStrategy implements AggregationStrategy {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FedAvgStrategy fedAvgStrategy = new FedAvgStrategy();

    // 默认正则化系数
    private static final double DEFAULT_MU = 0.1;

    @Override
    public Map<String, Object> aggregate(List<VmRoundModel> models, Map<String, Object> taskConfig)
            throws AggregationException {

        if (models == null || models.isEmpty()) {
            throw new AggregationException("模型列表不能为空");
        }

        log.info("开始FedProx聚合: 模型数量={}", models.size());

        try {
            // 获取正则化系数
            double mu = getMuValue(taskConfig);
            log.debug("使用正则化系数: μ={}", mu);

            // 1. 首先执行标准FedAvg聚合
            Map<String, Object> fedAvgResult = fedAvgStrategy.aggregate(models, taskConfig);

            // 2. 获取全局模型（上一轮的结果，如果有）
            Map<String, Object> globalModel = getGlobalModel(taskConfig);

            // 3. 应用FedProx正则化
            Map<String, Object> proxResult = applyProxRegularization(fedAvgResult, globalModel, mu);

            // 4. 更新聚合方法标识
            proxResult.put("aggregation_method", getAggregationMethodName(fedAvgResult));
            proxResult.put("regularization_mu", mu);

            log.info("FedProx聚合完成: 参与者={}, μ={}", models.size(), mu);
            return proxResult;

        } catch (Exception e) {
            log.error("FedProx聚合失败: {}", e.getMessage(), e);
            throw new AggregationException("FedProx聚合执行异常: " + e.getMessage(), e);
        }
    }

    @Override
    public String getStrategyName() {
        return "FedProx";
    }

    @Override
    public int getMinimumParticipants() {
        return 2; // FedProx需要至少2个参与者
    }

    /**
     * 应用FedProx正则化
     */
    private Map<String, Object> applyProxRegularization(Map<String, Object> fedAvgResult,
                                                       Map<String, Object> globalModel,
                                                       double mu) throws AggregationException {

        if (globalModel == null || globalModel.isEmpty()) {
            log.debug("无全局模型，返回FedAvg结果");
            return fedAvgResult;
        }

        try {
            String aggregationMethod = (String) fedAvgResult.get("aggregation_method");

            if ("FedAvg-RF".equals(aggregationMethod)) {
                return applyProxToRandomForest(fedAvgResult, globalModel, mu);
            } else if ("FedAvg-NN".equals(aggregationMethod)) {
                return applyProxToNeuralNetwork(fedAvgResult, globalModel, mu);
            } else {
                return applyProxGeneric(fedAvgResult, globalModel, mu);
            }

        } catch (Exception e) {
            throw new AggregationException("应用FedProx正则化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 对RandomForest应用正则化
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> applyProxToRandomForest(Map<String, Object> fedAvgResult,
                                                        Map<String, Object> globalModel,
                                                        double mu) {

        Map<String, Object> result = new HashMap<>(fedAvgResult);

        // 对特征重要性应用正则化
        List<Double> fedAvgImportances = (List<Double>) fedAvgResult.get("feature_importances_");
        List<Double> globalImportances = (List<Double>) globalModel.get("feature_importances_");

        if (fedAvgImportances != null && globalImportances != null &&
            fedAvgImportances.size() == globalImportances.size()) {

            List<Double> proxImportances = new ArrayList<>();
            for (int i = 0; i < fedAvgImportances.size(); i++) {
                // FedProx公式：θ_new = (1-μ) * θ_fedavg + μ * θ_global
                double proxValue = (1 - mu) * fedAvgImportances.get(i) + mu * globalImportances.get(i);
                proxImportances.add(proxValue);
            }

            // 重新归一化
            double sum = proxImportances.stream().mapToDouble(Double::doubleValue).sum();
            if (sum > 0) {
                proxImportances = proxImportances.stream()
                        .map(imp -> imp / sum)
                        .collect(Collectors.toList());
            }

            result.put("feature_importances_", proxImportances);
            log.debug("RandomForest正则化完成: 特征数量={}", proxImportances.size());
        }

        return result;
    }

    /**
     * 对神经网络应用正则化
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> applyProxToNeuralNetwork(Map<String, Object> fedAvgResult,
                                                         Map<String, Object> globalModel,
                                                         double mu) {

        Map<String, Object> result = new HashMap<>(fedAvgResult);

        Map<String, Object> fedAvgWeights = (Map<String, Object>) fedAvgResult.get("weights");
        Map<String, Object> globalWeights = (Map<String, Object>) globalModel.get("weights");

        if (fedAvgWeights != null && globalWeights != null) {
            Map<String, Object> proxWeights = new HashMap<>();

            for (String layerName : fedAvgWeights.keySet()) {
                if (globalWeights.containsKey(layerName)) {
                    Object fedAvgParam = fedAvgWeights.get(layerName);
                    Object globalParam = globalWeights.get(layerName);

                    if (fedAvgParam instanceof List && globalParam instanceof List) {
                        proxWeights.put(layerName,
                            applyProxToList((List<Object>) fedAvgParam, (List<Object>) globalParam, mu));
                    } else if (fedAvgParam instanceof Number && globalParam instanceof Number) {
                        double proxValue = (1 - mu) * ((Number) fedAvgParam).doubleValue() +
                                         mu * ((Number) globalParam).doubleValue();
                        proxWeights.put(layerName, proxValue);
                    } else {
                        // 保持FedAvg值
                        proxWeights.put(layerName, fedAvgParam);
                    }
                } else {
                    // 全局模型中没有对应参数，保持FedAvg值
                    proxWeights.put(layerName, fedAvgWeights.get(layerName));
                }
            }

            result.put("weights", proxWeights);
            log.debug("神经网络正则化完成: 参数层数={}", proxWeights.size());
        }

        return result;
    }

    /**
     * 对列表参数应用正则化
     */
    @SuppressWarnings("unchecked")
    private List<Object> applyProxToList(List<Object> fedAvgList, List<Object> globalList, double mu) {
        List<Object> proxList = new ArrayList<>();

        for (int i = 0; i < fedAvgList.size() && i < globalList.size(); i++) {
            Object fedAvgItem = fedAvgList.get(i);
            Object globalItem = globalList.get(i);

            if (fedAvgItem instanceof Number && globalItem instanceof Number) {
                double proxValue = (1 - mu) * ((Number) fedAvgItem).doubleValue() +
                                 mu * ((Number) globalItem).doubleValue();
                proxList.add(proxValue);
            } else if (fedAvgItem instanceof List && globalItem instanceof List) {
                proxList.add(applyProxToList((List<Object>) fedAvgItem, (List<Object>) globalItem, mu));
            } else {
                proxList.add(fedAvgItem);
            }
        }

        // 如果FedAvg列表更长，添加剩余元素
        for (int i = globalList.size(); i < fedAvgList.size(); i++) {
            proxList.add(fedAvgList.get(i));
        }

        return proxList;
    }

    /**
     * 通用正则化应用
     */
    private Map<String, Object> applyProxGeneric(Map<String, Object> fedAvgResult,
                                                Map<String, Object> globalModel,
                                                double mu) {
        // 对于未知模型类型，简单返回FedAvg结果
        return new HashMap<>(fedAvgResult);
    }

    /**
     * 获取正则化系数μ
     */
    private double getMuValue(Map<String, Object> taskConfig) {
        if (taskConfig == null) {
            return DEFAULT_MU;
        }

        // 从任务配置中获取μ值
        Object muObj = taskConfig.get("mu");
        if (muObj instanceof Number) {
            double mu = ((Number) muObj).doubleValue();
            // 确保μ在合理范围内
            return Math.max(0.0, Math.min(1.0, mu));
        }

        // 尝试从其他可能的字段获取
        muObj = taskConfig.get("regularization_coefficient");
        if (muObj instanceof Number) {
            double mu = ((Number) muObj).doubleValue();
            return Math.max(0.0, Math.min(1.0, mu));
        }

        return DEFAULT_MU;
    }

    /**
     * 获取全局模型（上一轮结果）
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getGlobalModel(Map<String, Object> taskConfig) {
        if (taskConfig == null) {
            return null;
        }

        // 从任务配置中获取全局模型
        Object globalModelObj = taskConfig.get("global_model");
        if (globalModelObj instanceof Map) {
            return (Map<String, Object>) globalModelObj;
        }

        // 尝试从其他字段获取
        globalModelObj = taskConfig.get("previous_global_model");
        if (globalModelObj instanceof Map) {
            return (Map<String, Object>) globalModelObj;
        }

        return null;
    }

    /**
     * 获取聚合方法名称
     */
    private String getAggregationMethodName(Map<String, Object> fedAvgResult) {
        String originalMethod = (String) fedAvgResult.get("aggregation_method");
        if ("FedAvg-RF".equals(originalMethod)) {
            return "FedProx-RF";
        } else if ("FedAvg-NN".equals(originalMethod)) {
            return "FedProx-NN";
        } else {
            return "FedProx-Generic";
        }
    }
}