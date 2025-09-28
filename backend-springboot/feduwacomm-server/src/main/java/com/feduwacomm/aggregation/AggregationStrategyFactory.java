package com.feduwacomm.aggregation;

import com.feduwacomm.aggregation.strategy.FedAvgStrategy;
import com.feduwacomm.aggregation.strategy.FedNovaStrategy;
import com.feduwacomm.aggregation.strategy.FedProxStrategy;
import com.feduwacomm.aggregation.strategy.ScaffoldStrategy;
import com.feduwacomm.enums.FederatedAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * 聚合策略工厂
 *
 * 根据联邦学习算法类型创建对应的聚合策略实例
 * 基于现有数据库枚举 (FEDERATED_AVERAGING, FEDERATED_PROXIMAL, FEDERATED_NOVA, SCAFFOLD)
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class AggregationStrategyFactory {

    private final Map<FederatedAlgorithm, AggregationStrategy> strategyMap;

    public AggregationStrategyFactory() {
        // 初始化策略映射表 - 基于现有数据库枚举
        strategyMap = new EnumMap<>(FederatedAlgorithm.class);
        initializeStrategies();
    }

    /**
     * 初始化所有支持的聚合策略
     */
    private void initializeStrategies() {
        // FedAvg策略 - 对应 FEDERATED_AVERAGING
        strategyMap.put(FederatedAlgorithm.FEDERATED_AVERAGING, new FedAvgStrategy());

        // FedProx策略 - 对应 FEDERATED_PROXIMAL
        strategyMap.put(FederatedAlgorithm.FEDERATED_PROXIMAL, new FedProxStrategy());

        // FedNova策略 - 对应 FEDERATED_NOVA
        strategyMap.put(FederatedAlgorithm.FEDERATED_NOVA, new FedNovaStrategy());

        // Scaffold策略 - 对应 FEDERATED_SCAFFOLD
        strategyMap.put(FederatedAlgorithm.FEDERATED_SCAFFOLD, new ScaffoldStrategy());

        log.info("聚合策略工厂初始化完成，支持的算法: {}", strategyMap.keySet());
    }

    /**
     * 根据算法类型获取聚合策略
     *
     * @param algorithm 联邦学习算法类型
     * @return 对应的聚合策略实例
     * @throws IllegalArgumentException 如果算法类型不支持
     */
    public AggregationStrategy getStrategy(FederatedAlgorithm algorithm) {
        if (algorithm == null) {
            throw new IllegalArgumentException("算法类型不能为空");
        }

        AggregationStrategy strategy = strategyMap.get(algorithm);
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的聚合算法: " + algorithm);
        }

        log.debug("获取聚合策略: 算法={}, 策略={}", algorithm, strategy.getStrategyName());
        return strategy;
    }

    /**
     * 检查是否支持指定算法
     *
     * @param algorithm 联邦学习算法类型
     * @return 是否支持
     */
    public boolean isSupported(FederatedAlgorithm algorithm) {
        return algorithm != null && strategyMap.containsKey(algorithm);
    }

    /**
     * 获取所有支持的算法类型
     *
     * @return 支持的算法类型集合
     */
    public java.util.Set<FederatedAlgorithm> getSupportedAlgorithms() {
        return strategyMap.keySet();
    }

    /**
     * 获取策略统计信息
     *
     * @return 策略统计信息
     */
    public Map<String, Object> getStrategyStatistics() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalStrategies", strategyMap.size());
        stats.put("supportedAlgorithms", strategyMap.keySet());

        // 统计各策略支持的模型类型
        Map<String, java.util.List<String>> modelTypeSupport = new java.util.HashMap<>();
        strategyMap.forEach((algorithm, strategy) -> {
            modelTypeSupport.put(algorithm.name(), strategy.getSupportedModelTypes());
        });
        stats.put("modelTypeSupport", modelTypeSupport);

        return stats;
    }
}