package com.feduwacomm.aggregation;

import com.feduwacomm.aggregation.strategy.FedAvgStrategy;
import com.feduwacomm.aggregation.strategy.FedNovaStrategy;
import com.feduwacomm.aggregation.strategy.FedProxStrategy;
import com.feduwacomm.aggregation.strategy.ScaffoldStrategy;
import com.feduwacomm.enums.FederatedAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * AggregationStrategyFactory 单元测试
 *
 * 测试聚合策略工厂的功能：
 * - 策略的正确获取
 * - 支持的算法类型检查
 * - 异常处理
 * - 策略统计信息
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@DisplayName("聚合策略工厂单元测试")
class AggregationStrategyFactoryTest {

    private AggregationStrategyFactory strategyFactory;

    @BeforeEach
    void setUp() {
        strategyFactory = new AggregationStrategyFactory();
    }

    @Test
    @DisplayName("测试获取FedAvg策略")
    void testGetFedAvgStrategy() {
        // When
        AggregationStrategy strategy = strategyFactory.getStrategy(FederatedAlgorithm.FEDERATED_AVERAGING);

        // Then
        assertThat(strategy).isNotNull();
        assertThat(strategy).isInstanceOf(FedAvgStrategy.class);
        assertThat(strategy.getStrategyName()).isEqualTo("FedAvg");
        assertThat(strategy.getMinimumParticipants()).isEqualTo(2);
    }

    @Test
    @DisplayName("测试获取FedProx策略")
    void testGetFedProxStrategy() {
        // When
        AggregationStrategy strategy = strategyFactory.getStrategy(FederatedAlgorithm.FEDERATED_PROXIMAL);

        // Then
        assertThat(strategy).isNotNull();
        assertThat(strategy).isInstanceOf(FedProxStrategy.class);
        assertThat(strategy.getStrategyName()).isEqualTo("FedProx");
        assertThat(strategy.getMinimumParticipants()).isEqualTo(2);
    }

    @Test
    @DisplayName("测试获取FedNova策略")
    void testGetFedNovaStrategy() {
        // When
        AggregationStrategy strategy = strategyFactory.getStrategy(FederatedAlgorithm.FEDERATED_NOVA);

        // Then
        assertThat(strategy).isNotNull();
        assertThat(strategy).isInstanceOf(FedNovaStrategy.class);
        assertThat(strategy.getStrategyName()).isEqualTo("FedNova");
        assertThat(strategy.getMinimumParticipants()).isEqualTo(2);
    }

    @Test
    @DisplayName("测试获取Scaffold策略")
    void testGetScaffoldStrategy() {
        // When
        AggregationStrategy strategy = strategyFactory.getStrategy(FederatedAlgorithm.FEDERATED_SCAFFOLD);

        // Then
        assertThat(strategy).isNotNull();
        assertThat(strategy).isInstanceOf(ScaffoldStrategy.class);
        assertThat(strategy.getStrategyName()).isEqualTo("SCAFFOLD");
        assertThat(strategy.getMinimumParticipants()).isEqualTo(3); // SCAFFOLD需要更多参与者
    }

    @Test
    @DisplayName("测试多次获取相同策略返回相同实例")
    void testGetSameStrategyMultipleTimes() {
        // When
        AggregationStrategy strategy1 = strategyFactory.getStrategy(FederatedAlgorithm.FEDERATED_AVERAGING);
        AggregationStrategy strategy2 = strategyFactory.getStrategy(FederatedAlgorithm.FEDERATED_AVERAGING);

        // Then
        assertThat(strategy1).isSameAs(strategy2); // 应该返回相同实例（单例）
    }

    @Test
    @DisplayName("测试null算法抛出异常")
    void testGetStrategyWithNullAlgorithm() {
        // When & Then
        assertThatThrownBy(() -> strategyFactory.getStrategy(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("算法类型不能为空");
    }

    @Test
    @DisplayName("测试算法支持检查 - 支持的算法")
    void testIsSupportedWithSupportedAlgorithms() {
        // Given
        FederatedAlgorithm[] supportedAlgorithms = {
            FederatedAlgorithm.FEDERATED_AVERAGING,
            FederatedAlgorithm.FEDERATED_PROXIMAL,
            FederatedAlgorithm.FEDERATED_NOVA,
            FederatedAlgorithm.FEDERATED_SCAFFOLD
        };

        // When & Then
        for (FederatedAlgorithm algorithm : supportedAlgorithms) {
            assertThat(strategyFactory.isSupported(algorithm))
                .as("算法 %s 应该被支持", algorithm)
                .isTrue();
        }
    }

    @Test
    @DisplayName("测试算法支持检查 - null算法")
    void testIsSupportedWithNullAlgorithm() {
        // When & Then
        assertThat(strategyFactory.isSupported(null)).isFalse();
    }

    @Test
    @DisplayName("测试获取支持的算法列表")
    void testGetSupportedAlgorithms() {
        // When
        Set<FederatedAlgorithm> supportedAlgorithms = strategyFactory.getSupportedAlgorithms();

        // Then
        assertThat(supportedAlgorithms).hasSize(4);
        assertThat(supportedAlgorithms).containsExactlyInAnyOrder(
            FederatedAlgorithm.FEDERATED_AVERAGING,
            FederatedAlgorithm.FEDERATED_PROXIMAL,
            FederatedAlgorithm.FEDERATED_NOVA,
            FederatedAlgorithm.FEDERATED_SCAFFOLD
        );

        // 确保返回的是不可变集合或副本
        assertThatThrownBy(() -> supportedAlgorithms.clear())
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("测试策略统计信息")
    void testGetStrategyStatistics() {
        // When
        Map<String, Object> statistics = strategyFactory.getStrategyStatistics();

        // Then
        assertThat(statistics).isNotNull();
        assertThat(statistics).containsKeys("totalStrategies", "supportedAlgorithms", "modelTypeSupport");

        // 验证总数量
        assertThat(statistics.get("totalStrategies")).isEqualTo(4);

        // 验证支持的算法
        @SuppressWarnings("unchecked")
        Set<FederatedAlgorithm> supportedAlgorithms = (Set<FederatedAlgorithm>) statistics.get("supportedAlgorithms");
        assertThat(supportedAlgorithms).hasSize(4);

        // 验证模型类型支持统计
        @SuppressWarnings("unchecked")
        Map<String, java.util.List<String>> modelTypeSupport =
            (Map<String, java.util.List<String>>) statistics.get("modelTypeSupport");
        assertThat(modelTypeSupport).hasSize(4);

        // 验证每种算法都有模型类型支持信息
        assertThat(modelTypeSupport).containsKeys(
            "FEDERATED_AVERAGING",
            "FEDERATED_PROXIMAL",
            "FEDERATED_NOVA",
            "FEDERATED_SCAFFOLD"
        );

        // 验证每种策略都支持通用模型类型
        for (java.util.List<String> supportedTypes : modelTypeSupport.values()) {
            assertThat(supportedTypes).contains("UNIVERSAL");
        }
    }

    @Test
    @DisplayName("测试所有策略都实现了必需的方法")
    void testAllStrategiesImplementRequiredMethods() {
        // Given
        FederatedAlgorithm[] allAlgorithms = {
            FederatedAlgorithm.FEDERATED_AVERAGING,
            FederatedAlgorithm.FEDERATED_PROXIMAL,
            FederatedAlgorithm.FEDERATED_NOVA,
            FederatedAlgorithm.FEDERATED_SCAFFOLD
        };

        // When & Then
        for (FederatedAlgorithm algorithm : allAlgorithms) {
            AggregationStrategy strategy = strategyFactory.getStrategy(algorithm);

            // 验证所有必需方法都有实现
            assertThat(strategy.getStrategyName()).isNotNull();
            assertThat(strategy.getStrategyName()).isNotEmpty();
            assertThat(strategy.getMinimumParticipants()).isGreaterThan(0);
            assertThat(strategy.getSupportedModelTypes()).isNotNull();
            assertThat(strategy.getSupportedModelTypes()).isNotEmpty();
        }
    }

    @Test
    @DisplayName("测试策略工厂的线程安全性")
    void testStrategyFactoryThreadSafety() throws InterruptedException {
        // Given
        int threadCount = 10;
        java.util.List<AggregationStrategy> results =
            java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        java.util.concurrent.CountDownLatch latch =
            new java.util.concurrent.CountDownLatch(threadCount);

        // When - 并发获取策略
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    AggregationStrategy strategy = strategyFactory.getStrategy(FederatedAlgorithm.FEDERATED_AVERAGING);
                    results.add(strategy);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();

        // Then - 所有线程应该获取到相同的策略实例
        assertThat(results).hasSize(threadCount);
        AggregationStrategy firstStrategy = results.get(0);
        for (AggregationStrategy strategy : results) {
            assertThat(strategy).isSameAs(firstStrategy);
        }
    }

    @Test
    @DisplayName("测试策略的不同配置和特性")
    void testStrategySpecificCharacteristics() {
        // Given & When & Then

        // FedAvg - 基础联邦平均算法
        AggregationStrategy fedAvg = strategyFactory.getStrategy(FederatedAlgorithm.FEDERATED_AVERAGING);
        assertThat(fedAvg.getStrategyName()).isEqualTo("FedAvg");
        assertThat(fedAvg.getMinimumParticipants()).isEqualTo(2);

        // FedProx - 带正则化的联邦算法
        AggregationStrategy fedProx = strategyFactory.getStrategy(FederatedAlgorithm.FEDERATED_PROXIMAL);
        assertThat(fedProx.getStrategyName()).isEqualTo("FedProx");
        assertThat(fedProx.getMinimumParticipants()).isEqualTo(2);

        // FedNova - 处理客户端异构性
        AggregationStrategy fedNova = strategyFactory.getStrategy(FederatedAlgorithm.FEDERATED_NOVA);
        assertThat(fedNova.getStrategyName()).isEqualTo("FedNova");
        assertThat(fedNova.getMinimumParticipants()).isEqualTo(2);

        // Scaffold - 更复杂的控制变量算法
        AggregationStrategy scaffold = strategyFactory.getStrategy(FederatedAlgorithm.FEDERATED_SCAFFOLD);
        assertThat(scaffold.getStrategyName()).isEqualTo("SCAFFOLD");
        assertThat(scaffold.getMinimumParticipants()).isEqualTo(3); // 需要更多参与者
    }
}