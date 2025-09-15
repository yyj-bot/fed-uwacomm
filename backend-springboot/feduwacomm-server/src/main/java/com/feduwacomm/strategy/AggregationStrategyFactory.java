package com.feduwacomm.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 聚合策略工厂
 * 根据算法类型选择合适的聚合策略实现
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AggregationStrategyFactory {

    private final List<AggregationStrategy> strategies;

    /**
     * 根据算法名称获取对应的聚合策略
     *
     * @param algorithm 算法名称（如：FEDAVG, FEDPROX, FEDNOVA）
     * @return 聚合策略实现
     */
    public AggregationStrategy getStrategy(String algorithm) {
        if (algorithm == null || algorithm.trim().isEmpty()) {
            log.warn("算法名称为空，使用默认FedAvg策略");
            return getDefaultStrategy();
        }

        String normalizedAlgorithm = algorithm.trim().toUpperCase();
        
        Optional<AggregationStrategy> strategy = strategies.stream()
                .filter(s -> s.supports(normalizedAlgorithm))
                .findFirst();

        if (strategy.isPresent()) {
            log.debug("找到算法 {} 对应的策略: {}", normalizedAlgorithm, strategy.get().getStrategyName());
            return strategy.get();
        } else {
            log.warn("未找到算法 {} 对应的策略实现，使用默认FedAvg策略", normalizedAlgorithm);
            return getDefaultStrategy();
        }
    }

    /**
     * 获取默认策略（FedAvg）
     */
    public AggregationStrategy getDefaultStrategy() {
        return strategies.stream()
                .filter(s -> s.supports("FEDAVG"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("未找到默认的FedAvg策略实现"));
    }

    /**
     * 获取所有支持的算法列表
     */
    public List<String> getSupportedAlgorithms() {
        return List.of("FEDAVG", "FEDPROX", "FEDNOVA");
    }

    /**
     * 检查是否支持指定算法
     */
    public boolean isSupported(String algorithm) {
        if (algorithm == null || algorithm.trim().isEmpty()) {
            return false;
        }

        String normalizedAlgorithm = algorithm.trim().toUpperCase();
        return strategies.stream()
                .anyMatch(s -> s.supports(normalizedAlgorithm));
    }

    /**
     * 根据任务特性推荐最佳算法
     */
    public AlgorithmRecommendation recommendAlgorithm(int participantCount, 
                                                      boolean isHeterogeneous, 
                                                      int totalRounds,
                                                      String currentAlgorithm) {
        
        String recommendedAlgorithm;
        String reasoning;
        int priority;

        if (isHeterogeneous && participantCount > 5) {
            // 数据异构且参与者较多，推荐FedProx
            recommendedAlgorithm = "FEDPROX";
            reasoning = "数据异构环境下，FedProx的正则化机制有助于提升收敛稳定性";
            priority = 90;
        } else if (participantCount > 20 && totalRounds > 50) {
            // 大规模长期训练，考虑FedNova
            recommendedAlgorithm = "FEDNOVA";
            reasoning = "大规模长期训练场景下，FedNova能更好处理客户端异构性";
            priority = 85;
        } else {
            // 标准场景，推荐FedAvg
            recommendedAlgorithm = "FEDAVG";
            reasoning = "标准联邦学习场景，FedAvg提供良好的性能和稳定性平衡";
            priority = 80;
        }

        // 如果当前算法已经在运行且合理，降低推荐优先级
        if (currentAlgorithm != null && !currentAlgorithm.equalsIgnoreCase(recommendedAlgorithm)) {
            if (isSupported(currentAlgorithm)) {
                priority -= 10;
                reasoning += "，但当前算法也适用，建议根据实际效果决定是否切换";
            }
        }

        return new AlgorithmRecommendation(recommendedAlgorithm, reasoning, priority);
    }

    /**
     * 算法推荐结果
     */
    public static class AlgorithmRecommendation {
        private final String algorithm;
        private final String reasoning;
        private final int priority;

        public AlgorithmRecommendation(String algorithm, String reasoning, int priority) {
            this.algorithm = algorithm;
            this.reasoning = reasoning;
            this.priority = priority;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public String getReasoning() {
            return reasoning;
        }

        public int getPriority() {
            return priority;
        }

        public boolean isHighPriority() {
            return priority >= 85;
        }

        @Override
        public String toString() {
            return String.format("推荐算法: %s (优先级: %d) - %s", algorithm, priority, reasoning);
        }
    }
}