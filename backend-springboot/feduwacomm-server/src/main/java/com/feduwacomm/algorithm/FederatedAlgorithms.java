package com.feduwacomm.algorithm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 联邦学习聚合算法实现库
 * 支持 FedAvg, FedProx, FedNova 等主流算法
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class FederatedAlgorithms {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int DECIMAL_SCALE = 8;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * FedAvg (联邦平均) 聚合算法
     * 实现加权平均聚合，权重基于客户端数据样本数量
     *
     * @param clientParameters 客户端参数列表
     * @param weights          权重列表（通常是数据样本数量）
     * @return 聚合后的全局参数
     */
    public static Map<String, Object> fedAvgAggregate(
            List<Map<String, Object>> clientParameters, 
            List<Double> weights) {

        if (clientParameters == null || clientParameters.isEmpty()) {
            throw new IllegalArgumentException("客户端参数列表不能为空");
        }

        if (weights == null || weights.size() != clientParameters.size()) {
            throw new IllegalArgumentException("权重数量必须与客户端参数数量一致");
        }

        log.info("开始FedAvg聚合，参与客户端数量: {}", clientParameters.size());

        try {
            // 计算权重总和并归一化
            double totalWeight = weights.stream().mapToDouble(Double::doubleValue).sum();
            List<Double> normalizedWeights = weights.stream()
                    .map(w -> w / totalWeight)
                    .toList();

            Map<String, Object> globalParameters = new ConcurrentHashMap<>();

            // 获取第一个客户端的参数结构作为模板
            Map<String, Object> template = clientParameters.get(0);
            
            for (String paramName : template.keySet()) {
                Object aggregatedParam = aggregateParameterByName(
                        paramName, clientParameters, normalizedWeights);
                globalParameters.put(paramName, aggregatedParam);
            }

            log.info("FedAvg聚合完成，聚合参数数量: {}", globalParameters.size());
            return globalParameters;

        } catch (Exception e) {
            log.error("FedAvg聚合过程中发生错误: {}", e.getMessage(), e);
            throw new RuntimeException("FedAvg聚合失败: " + e.getMessage(), e);
        }
    }

    /**
     * FedProx 聚合算法
     * 基于FedAvg，增加正则化项处理
     *
     * @param clientParameters 客户端参数列表
     * @param serverParameters 服务器端全局参数（上一轮）
     * @param weights          权重列表
     * @param mu               正则化系数
     * @return 聚合后的全局参数
     */
    public static Map<String, Object> fedProxAggregate(
            List<Map<String, Object>> clientParameters,
            Map<String, Object> serverParameters,
            List<Double> weights,
            double mu) {

        log.info("开始FedProx聚合，正则化系数μ={}, 参与客户端数量: {}", mu, clientParameters.size());

        try {
            // 先进行标准FedAvg聚合
            Map<String, Object> fedAvgResult = fedAvgAggregate(clientParameters, weights);

            // 如果没有服务器参数或μ=0，则退化为FedAvg
            if (serverParameters == null || serverParameters.isEmpty() || mu == 0.0) {
                log.info("FedProx退化为FedAvg（无服务器参数或μ=0）");
                return fedAvgResult;
            }

            // 应用正则化项
            Map<String, Object> globalParameters = new ConcurrentHashMap<>();
            
            for (String paramName : fedAvgResult.keySet()) {
                Object fedAvgParam = fedAvgResult.get(paramName);
                Object serverParam = serverParameters.get(paramName);
                
                if (serverParam != null) {
                    Object regularizedParam = applyProximalRegularization(
                            fedAvgParam, serverParam, mu);
                    globalParameters.put(paramName, regularizedParam);
                } else {
                    globalParameters.put(paramName, fedAvgParam);
                }
            }

            log.info("FedProx聚合完成，聚合参数数量: {}", globalParameters.size());
            return globalParameters;

        } catch (Exception e) {
            log.error("FedProx聚合过程中发生错误: {}", e.getMessage(), e);
            throw new RuntimeException("FedProx聚合失败: " + e.getMessage(), e);
        }
    }

    /**
     * FedNova 聚合算法
     * 处理异构客户端场景，考虑局部更新步数差异
     *
     * @param clientParameters 客户端参数列表
     * @param localSteps       各客户端的本地训练步数
     * @param weights          权重列表
     * @return 聚合后的全局参数
     */
    public static Map<String, Object> fedNovaAggregate(
            List<Map<String, Object>> clientParameters,
            List<Integer> localSteps,
            List<Double> weights) {

        if (localSteps == null || localSteps.size() != clientParameters.size()) {
            throw new IllegalArgumentException("本地步数数量必须与客户端参数数量一致");
        }

        log.info("开始FedNova聚合，参与客户端数量: {}", clientParameters.size());

        try {
            // 计算归一化权重（考虑本地步数）
            double totalWeight = 0.0;
            List<Double> effectiveWeights = new ArrayList<>();
            
            for (int i = 0; i < weights.size(); i++) {
                double effectiveWeight = weights.get(i) * localSteps.get(i);
                effectiveWeights.add(effectiveWeight);
                totalWeight += effectiveWeight;
            }

            // 归一化
            final double finalTotalWeight = totalWeight; // 使用final变量供lambda使用
            List<Double> normalizedWeights = effectiveWeights.stream()
                    .map(w -> w / finalTotalWeight)
                    .toList();

            // 使用修正后的权重进行FedAvg聚合
            Map<String, Object> globalParameters = fedAvgAggregate(clientParameters, normalizedWeights);

            log.info("FedNova聚合完成");
            return globalParameters;

        } catch (Exception e) {
            log.error("FedNova聚合过程中发生错误: {}", e.getMessage(), e);
            throw new RuntimeException("FedNova聚合失败: " + e.getMessage(), e);
        }
    }

    /**
     * 计算全局指标（准确率、损失等）
     *
     * @param clientMetrics 客户端指标列表
     * @param weights       权重列表
     * @return 聚合后的全局指标
     */
    public static Map<String, BigDecimal> calculateGlobalMetrics(
            List<Map<String, Object>> clientMetrics,
            List<Double> weights) {

        if (clientMetrics == null || clientMetrics.isEmpty()) {
            return new HashMap<>();
        }

        log.info("计算全局指标，参与客户端数量: {}", clientMetrics.size());

        Map<String, BigDecimal> globalMetrics = new HashMap<>();

        try {
            double totalWeight = weights.stream().mapToDouble(Double::doubleValue).sum();

            // 获取指标名称
            Set<String> metricNames = clientMetrics.get(0).keySet();

            for (String metricName : metricNames) {
                double weightedSum = 0.0;
                
                for (int i = 0; i < clientMetrics.size(); i++) {
                    Object metricValue = clientMetrics.get(i).get(metricName);
                    if (metricValue instanceof Number) {
                        double value = ((Number) metricValue).doubleValue();
                        weightedSum += value * weights.get(i);
                    }
                }
                
                double globalMetric = weightedSum / totalWeight;
                globalMetrics.put(metricName, 
                    BigDecimal.valueOf(globalMetric).setScale(DECIMAL_SCALE, ROUNDING_MODE));
            }

            log.info("全局指标计算完成: {}", globalMetrics);
            return globalMetrics;

        } catch (Exception e) {
            log.error("全局指标计算失败: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /**
     * 按参数名聚合单个参数
     */
    private static Object aggregateParameterByName(
            String paramName,
            List<Map<String, Object>> clientParameters,
            List<Double> normalizedWeights) {

        List<Object> paramValues = new ArrayList<>();
        
        for (Map<String, Object> clientParams : clientParameters) {
            Object paramValue = clientParams.get(paramName);
            if (paramValue != null) {
                paramValues.add(paramValue);
            }
        }

        if (paramValues.isEmpty()) {
            return null;
        }

        Object firstValue = paramValues.get(0);

        // 处理数字类型参数
        if (firstValue instanceof Number) {
            return aggregateNumericParameter(paramValues, normalizedWeights);
        }

        // 处理数组类型参数
        if (firstValue instanceof List || firstValue.getClass().isArray()) {
            return aggregateArrayParameter(paramValues, normalizedWeights);
        }

        // 处理Map类型参数（嵌套参数）
        if (firstValue instanceof Map) {
            return aggregateMapParameter(paramValues, normalizedWeights);
        }

        // 其他类型直接返回第一个值（多数投票可在此扩展）
        return firstValue;
    }

    /**
     * 聚合数值类型参数
     */
    private static Object aggregateNumericParameter(List<Object> values, List<Double> weights) {
        double weightedSum = 0.0;
        
        for (int i = 0; i < values.size(); i++) {
            double value = ((Number) values.get(i)).doubleValue();
            weightedSum += value * weights.get(i);
        }
        
        return weightedSum;
    }

    /**
     * 聚合数组类型参数
     */
    @SuppressWarnings("unchecked")
    private static Object aggregateArrayParameter(List<Object> values, List<Double> weights) {
        try {
            List<List<Double>> arrays = new ArrayList<>();
            
            for (Object value : values) {
                if (value instanceof List) {
                    arrays.add((List<Double>) value);
                } else if (value.getClass().isArray()) {
                    // 处理数组到List的转换
                    arrays.add(Arrays.asList((Double[]) value));
                }
            }

            if (arrays.isEmpty()) {
                return values.get(0);
            }

            int arrayLength = arrays.get(0).size();
            List<Double> aggregatedArray = new ArrayList<>();

            for (int j = 0; j < arrayLength; j++) {
                double weightedSum = 0.0;
                for (int i = 0; i < arrays.size(); i++) {
                    double elementValue = arrays.get(i).get(j);
                    weightedSum += elementValue * weights.get(i);
                }
                aggregatedArray.add(weightedSum);
            }

            return aggregatedArray;

        } catch (Exception e) {
            log.warn("数组参数聚合失败，使用第一个值: {}", e.getMessage());
            return values.get(0);
        }
    }

    /**
     * 聚合Map类型参数（递归处理嵌套结构）
     */
    @SuppressWarnings("unchecked")
    private static Object aggregateMapParameter(List<Object> values, List<Double> weights) {
        try {
            List<Map<String, Object>> maps = values.stream()
                    .map(v -> (Map<String, Object>) v)
                    .toList();

            Map<String, Object> aggregatedMap = new HashMap<>();
            Set<String> allKeys = new HashSet<>();
            
            for (Map<String, Object> map : maps) {
                allKeys.addAll(map.keySet());
            }

            for (String key : allKeys) {
                List<Object> keyValues = new ArrayList<>();
                for (Map<String, Object> map : maps) {
                    Object keyValue = map.get(key);
                    if (keyValue != null) {
                        keyValues.add(keyValue);
                    }
                }
                
                if (!keyValues.isEmpty()) {
                    Object aggregatedKeyValue = aggregateParameterByName(key, 
                            maps, weights);
                    aggregatedMap.put(key, aggregatedKeyValue);
                }
            }

            return aggregatedMap;

        } catch (Exception e) {
            log.warn("Map参数聚合失败，使用第一个值: {}", e.getMessage());
            return values.get(0);
        }
    }

    /**
     * 应用FedProx正则化项
     */
    private static Object applyProximalRegularization(Object fedAvgParam, 
                                                       Object serverParam, 
                                                       double mu) {
        try {
            if (fedAvgParam instanceof Number && serverParam instanceof Number) {
                double fedAvgValue = ((Number) fedAvgParam).doubleValue();
                double serverValue = ((Number) serverParam).doubleValue();
                
                // 正则化公式: θ_new = θ_fedavg - μ * (θ_fedavg - θ_server)
                double regularizedValue = fedAvgValue - mu * (fedAvgValue - serverValue);
                return regularizedValue;
            }
            
            // 对于复杂类型，暂时返回FedAvg结果
            return fedAvgParam;
            
        } catch (Exception e) {
            log.warn("正则化处理失败，使用FedAvg结果: {}", e.getMessage());
            return fedAvgParam;
        }
    }
}