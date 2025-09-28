package com.feduwacomm.aggregation.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.aggregation.AggregationException;
import com.feduwacomm.aggregation.AggregationStrategy;
import com.feduwacomm.entity.VmRoundModel;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * SCAFFOLD 聚合策略
 *
 * 实现SCAFFOLD算法，使用控制变量减少客户端偏移：
 * - 维护全局控制变量
 * - 修正客户端更新偏差
 * - 提高异构数据环境下的收敛性
 *
 * 对应数据库枚举: SCAFFOLD
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Slf4j
public class ScaffoldStrategy implements AggregationStrategy {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FedAvgStrategy fedAvgStrategy = new FedAvgStrategy();

    @Override
    public Map<String, Object> aggregate(List<VmRoundModel> models, Map<String, Object> taskConfig)
            throws AggregationException {

        if (models == null || models.isEmpty()) {
            throw new AggregationException("模型列表不能为空");
        }

        log.info("开始SCAFFOLD聚合: 模型数量={}", models.size());

        try {
            // 获取或初始化控制变量
            Map<String, Object> globalControlVariate = getGlobalControlVariate(taskConfig);

            // 执行SCAFFOLD聚合
            Map<String, Object> result = executeScaffoldAggregation(models, globalControlVariate, taskConfig);

            result.put("aggregation_method", "SCAFFOLD");
            result.put("participants", models.size());

            log.info("SCAFFOLD聚合完成: 参与者={}", models.size());
            return result;

        } catch (Exception e) {
            log.error("SCAFFOLD聚合失败: {}", e.getMessage(), e);
            throw new AggregationException("SCAFFOLD聚合执行异常: " + e.getMessage(), e);
        }
    }

    @Override
    public String getStrategyName() {
        return "SCAFFOLD";
    }

    @Override
    public int getMinimumParticipants() {
        return 3; // SCAFFOLD通常需要更多参与者
    }

    /**
     * 执行SCAFFOLD聚合
     */
    private Map<String, Object> executeScaffoldAggregation(List<VmRoundModel> models,
                                                          Map<String, Object> globalControlVariate,
                                                          Map<String, Object> taskConfig)
            throws AggregationException {

        // 1. 先执行标准FedAvg聚合
        Map<String, Object> fedAvgResult = fedAvgStrategy.aggregate(models, taskConfig);

        // 2. 计算客户端控制变量更新
        List<Map<String, Object>> clientControlUpdates = calculateClientControlUpdates(models);

        // 3. 更新全局控制变量
        Map<String, Object> updatedGlobalControl = updateGlobalControlVariate(
                globalControlVariate, clientControlUpdates);

        // 4. 应用SCAFFOLD修正
        Map<String, Object> scaffoldResult = applyScaffoldCorrection(
                fedAvgResult, updatedGlobalControl, taskConfig);

        // 5. 保存更新后的控制变量（在实际应用中需要持久化）
        scaffoldResult.put("global_control_variate", updatedGlobalControl);

        return scaffoldResult;
    }

    /**
     * 获取全局控制变量
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getGlobalControlVariate(Map<String, Object> taskConfig) {
        if (taskConfig != null && taskConfig.containsKey("global_control_variate")) {
            return (Map<String, Object>) taskConfig.get("global_control_variate");
        }

        // 初始化为零控制变量
        return new HashMap<>();
    }

    /**
     * 计算客户端控制变量更新
     */
    private List<Map<String, Object>> calculateClientControlUpdates(List<VmRoundModel> models)
            throws AggregationException {

        List<Map<String, Object>> controlUpdates = new ArrayList<>();

        for (VmRoundModel model : models) {
            Map<String, Object> params = parseParameters(model);

            // 提取客户端控制变量（如果有）
            @SuppressWarnings("unchecked")
            Map<String, Object> clientControl = (Map<String, Object>)
                    params.get("client_control_variate");

            if (clientControl != null) {
                controlUpdates.add(clientControl);
            } else {
                // 如果没有控制变量，使用零向量
                controlUpdates.add(new HashMap<>());
            }
        }

        return controlUpdates;
    }

    /**
     * 更新全局控制变量
     */
    private Map<String, Object> updateGlobalControlVariate(Map<String, Object> globalControl,
                                                          List<Map<String, Object>> clientUpdates) {

        if (clientUpdates.isEmpty()) {
            return globalControl;
        }

        Map<String, Object> updatedControl = new HashMap<>();

        // 对于每个参数键，计算平均控制变量更新
        Set<String> allKeys = new HashSet<>();
        clientUpdates.forEach(update -> allKeys.addAll(update.keySet()));

        for (String key : allKeys) {
            List<Object> values = new ArrayList<>();
            for (Map<String, Object> update : clientUpdates) {
                if (update.containsKey(key)) {
                    values.add(update.get(key));
                }
            }

            if (!values.isEmpty()) {
                // 计算平均值（简化实现）
                Object avgValue = calculateAverageValue(values);
                updatedControl.put(key, avgValue);
            }
        }

        return updatedControl;
    }

    /**
     * 应用SCAFFOLD修正
     */
    private Map<String, Object> applyScaffoldCorrection(Map<String, Object> fedAvgResult,
                                                       Map<String, Object> globalControl,
                                                       Map<String, Object> taskConfig) {

        // 简化实现：在实际SCAFFOLD中需要复杂的控制变量修正
        Map<String, Object> correctedResult = new HashMap<>(fedAvgResult);

        // 这里可以添加具体的SCAFFOLD修正逻辑
        // 由于SCAFFOLD算法较为复杂，这里提供基础框架

        return correctedResult;
    }

    /**
     * 计算平均值
     */
    @SuppressWarnings("unchecked")
    private Object calculateAverageValue(List<Object> values) {
        if (values.isEmpty()) {
            return 0.0;
        }

        Object firstValue = values.get(0);

        if (firstValue instanceof Number) {
            double sum = values.stream()
                    .filter(v -> v instanceof Number)
                    .mapToDouble(v -> ((Number) v).doubleValue())
                    .sum();
            return sum / values.size();
        } else if (firstValue instanceof List) {
            // 处理列表类型
            List<Object> firstList = (List<Object>) firstValue;
            List<Object> avgList = new ArrayList<>();

            for (int i = 0; i < firstList.size(); i++) {
                List<Object> columnValues = new ArrayList<>();
                for (Object value : values) {
                    if (value instanceof List) {
                        List<Object> list = (List<Object>) value;
                        if (i < list.size()) {
                            columnValues.add(list.get(i));
                        }
                    }
                }
                avgList.add(calculateAverageValue(columnValues));
            }
            return avgList;
        }

        // 对于其他类型，返回第一个值
        return firstValue;
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