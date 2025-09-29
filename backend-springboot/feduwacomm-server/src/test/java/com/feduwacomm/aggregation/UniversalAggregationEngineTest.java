package com.feduwacomm.aggregation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.entity.VmRoundModel;
import com.feduwacomm.enums.FederatedAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * UniversalAggregationEngine 单元测试
 *
 * 测试通用聚合引擎的核心功能：
 * - RandomForest和神经网络模型的自动检测
 * - 聚合策略的正确调用
 * - 全局指标的计算
 * - 异常处理
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("通用聚合引擎单元测试")
class UniversalAggregationEngineTest {

    @Mock
    private AggregationStrategyFactory strategyFactory;

    @Mock
    private AggregationStrategy mockStrategy;

    @InjectMocks
    private UniversalAggregationEngine aggregationEngine;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // 确保objectMapper被正确注入到aggregationEngine中
        ReflectionTestUtils.setField(aggregationEngine, "objectMapper", objectMapper);
    }

    @Test
    @DisplayName("测试RandomForest模型聚合成功")
    void testRandomForestAggregation_Success() throws AggregationException {
        // Given - 准备RandomForest测试数据
        List<VmRoundModel> models = createRandomForestModels();
        Map<String, Object> taskConfig = createTaskConfig();
        Map<String, Object> expectedResult = createExpectedRandomForestResult();

        when(strategyFactory.getStrategy(FederatedAlgorithm.FEDERATED_AVERAGING))
            .thenReturn(mockStrategy);
        when(mockStrategy.aggregate(eq(models), eq(taskConfig)))
            .thenReturn(expectedResult);

        // When
        UniversalAggregationEngine.AggregationResult result =
            aggregationEngine.aggregate(models, FederatedAlgorithm.FEDERATED_AVERAGING, taskConfig);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getGlobalParameters()).isEqualTo(expectedResult);
        assertThat(result.getParticipantCount()).isEqualTo(3);
        assertThat(result.getAlgorithm()).isEqualTo("FEDERATED_AVERAGING");
        assertThat(result.getModelType()).isEqualTo("RandomForest");
        assertThat(result.getAggregationDuration()).isGreaterThan(0);

        // 验证全局指标计算
        assertThat(result.getGlobalMetrics()).containsKeys("average_accuracy", "average_loss", "total_samples");
        assertThat(result.getGlobalMetrics().get("participant_count")).isEqualTo(3.0);

        verify(strategyFactory).getStrategy(FederatedAlgorithm.FEDERATED_AVERAGING);
        verify(mockStrategy).aggregate(eq(models), eq(taskConfig));
    }

    @Test
    @DisplayName("测试神经网络模型聚合成功")
    void testNeuralNetworkAggregation_Success() throws AggregationException {
        // Given - 准备神经网络测试数据
        List<VmRoundModel> models = createNeuralNetworkModels();
        Map<String, Object> taskConfig = createTaskConfig();
        Map<String, Object> expectedResult = createExpectedNeuralNetworkResult();

        when(strategyFactory.getStrategy(FederatedAlgorithm.FEDERATED_PROXIMAL))
            .thenReturn(mockStrategy);
        when(mockStrategy.aggregate(eq(models), eq(taskConfig)))
            .thenReturn(expectedResult);

        // When
        UniversalAggregationEngine.AggregationResult result =
            aggregationEngine.aggregate(models, FederatedAlgorithm.FEDERATED_PROXIMAL, taskConfig);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getGlobalParameters()).containsKeys("weights", "biases");
        assertThat(result.getParticipantCount()).isEqualTo(2);
        assertThat(result.getModelType()).isEqualTo("NeuralNetwork");
        assertThat(result.getAlgorithm()).isEqualTo("FEDERATED_PROXIMAL");

        // 验证神经网络特定指标
        Map<String, Double> metrics = result.getGlobalMetrics();
        assertThat(metrics).containsKey("average_accuracy");
        assertThat(metrics.get("average_accuracy")).isGreaterThan(0.8);
    }

    @Test
    @DisplayName("测试模型类型自动检测 - RandomForest")
    void testModelTypeDetection_RandomForest() {
        // Given
        List<VmRoundModel> models = createRandomForestModels();

        // When - 使用反射调用私有方法测试
        String modelType = ReflectionTestUtils.invokeMethod(aggregationEngine,
            "detectModelType", models);

        // Then
        assertThat(modelType).isEqualTo("RandomForest");
    }

    @Test
    @DisplayName("测试模型类型自动检测 - 神经网络")
    void testModelTypeDetection_NeuralNetwork() {
        // Given
        List<VmRoundModel> models = createNeuralNetworkModels();

        // When
        String modelType = ReflectionTestUtils.invokeMethod(aggregationEngine,
            "detectModelType", models);

        // Then
        assertThat(modelType).isEqualTo("NeuralNetwork");
    }

    @Test
    @DisplayName("测试混合模型类型处理")
    void testMixedModelTypeHandling() throws AggregationException {
        // Given - 创建混合模型列表（不应该出现在实际使用中，但需要测试容错性）
        List<VmRoundModel> models = new ArrayList<>();
        models.addAll(createRandomForestModels());
        models.addAll(createNeuralNetworkModels());

        when(strategyFactory.getStrategy(any())).thenReturn(mockStrategy);
        when(mockStrategy.aggregate(any(), any())).thenReturn(new HashMap<>());

        // When
        UniversalAggregationEngine.AggregationResult result =
            aggregationEngine.aggregate(models, FederatedAlgorithm.FEDERATED_AVERAGING, new HashMap<>());

        // Then - 应该失败，因为模型不一致
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("聚合执行异常");
    }

    @Test
    @DisplayName("测试聚合异常处理")
    void testAggregation_ExceptionHandling() {
        // Given
        List<VmRoundModel> models = createRandomForestModels();
        when(strategyFactory.getStrategy(any()))
            .thenThrow(new RuntimeException("策略获取失败"));

        // When
        UniversalAggregationEngine.AggregationResult result =
            aggregationEngine.aggregate(models, FederatedAlgorithm.FEDERATED_AVERAGING, new HashMap<>());

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("聚合执行异常");
    }

    @Test
    @DisplayName("测试空模型列表异常")
    void testEmptyModelsHandling() {
        // Given
        List<VmRoundModel> emptyModels = Collections.emptyList();

        // When
        UniversalAggregationEngine.AggregationResult result =
            aggregationEngine.aggregate(emptyModels, FederatedAlgorithm.FEDERATED_AVERAGING, new HashMap<>());

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("模型列表不能为空");
    }

    @Test
    @DisplayName("测试null模型列表异常")
    void testNullModelsHandling() {
        // When
        UniversalAggregationEngine.AggregationResult result =
            aggregationEngine.aggregate(null, FederatedAlgorithm.FEDERATED_AVERAGING, new HashMap<>());

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("模型列表不能为空");
    }

    @Test
    @DisplayName("测试模型一致性验证")
    void testModelConsistencyValidation() {
        // Given - 创建一致的RandomForest模型
        List<VmRoundModel> consistentModels = createRandomForestModels();

        // When - 使用反射测试私有方法
        Boolean isConsistent = ReflectionTestUtils.invokeMethod(aggregationEngine,
            "validateModelConsistency", consistentModels);

        // Then
        assertThat(isConsistent).isTrue();
    }

    @Test
    @DisplayName("测试全局指标计算")
    void testGlobalMetricsCalculation() {
        // Given
        List<VmRoundModel> models = createRandomForestModels();
        Map<String, Object> aggregatedParams = new HashMap<>();

        // When - 使用反射测试私有方法
        Map<String, Double> metrics = ReflectionTestUtils.invokeMethod(aggregationEngine,
            "calculateGlobalMetrics", models, aggregatedParams);

        // Then
        assertThat(metrics).containsKeys("average_accuracy", "average_loss", "total_samples", "participant_count");
        assertThat(metrics.get("participant_count")).isEqualTo(3.0);
        assertThat(metrics.get("total_samples")).isEqualTo(4500.0); // 1500 + 1200 + 1800
        assertThat(metrics.get("average_accuracy")).isBetween(0.82, 0.88); // (0.85 + 0.82 + 0.88) / 3
    }

    // ================= 测试数据构建方法 =================

    private List<VmRoundModel> createRandomForestModels() {
        List<VmRoundModel> models = new ArrayList<>();

        // VM 1 - RandomForest参数
        VmRoundModel model1 = createVmRoundModel("model-1", "vm-001", 0.85, 0.15);
        Map<String, Object> params1 = Map.of(
            "model_parameters", Map.of(
                "feature_importances_", Arrays.asList(0.25, 0.30, 0.20, 0.25),
                "n_estimators", 100
            ),
            "training_metadata", Map.of(
                "samples_count", 1500,
                "algorithm", "RandomForest",
                "framework", "sklearn"
            )
        );
        model1.setParameters(toJson(params1));
        models.add(model1);

        // VM 2 - RandomForest参数
        VmRoundModel model2 = createVmRoundModel("model-2", "vm-002", 0.82, 0.18);
        Map<String, Object> params2 = Map.of(
            "model_parameters", Map.of(
                "feature_importances_", Arrays.asList(0.20, 0.35, 0.25, 0.20),
                "n_estimators", 100
            ),
            "training_metadata", Map.of(
                "samples_count", 1200,
                "algorithm", "RandomForest",
                "framework", "sklearn"
            )
        );
        model2.setParameters(toJson(params2));
        models.add(model2);

        // VM 3 - RandomForest参数
        VmRoundModel model3 = createVmRoundModel("model-3", "vm-003", 0.88, 0.12);
        Map<String, Object> params3 = Map.of(
            "model_parameters", Map.of(
                "feature_importances_", Arrays.asList(0.30, 0.25, 0.15, 0.30),
                "n_estimators", 100
            ),
            "training_metadata", Map.of(
                "samples_count", 1800,
                "algorithm", "RandomForest",
                "framework", "sklearn"
            )
        );
        model3.setParameters(toJson(params3));
        models.add(model3);

        return models;
    }

    private List<VmRoundModel> createNeuralNetworkModels() {
        List<VmRoundModel> models = new ArrayList<>();

        // VM 1 - 神经网络参数
        VmRoundModel model1 = createVmRoundModel("nn-model-1", "vm-001", 0.89, 0.11);
        Map<String, Object> params1 = Map.of(
            "model_parameters", Map.of(
                "weights", Map.of(
                    "layer1.weight", Arrays.asList(Arrays.asList(0.1, 0.2, 0.3), Arrays.asList(0.4, 0.5, 0.6)),
                    "layer2.weight", Arrays.asList(Arrays.asList(0.7, 0.8))
                ),
                "biases", Map.of(
                    "layer1.bias", Arrays.asList(0.1, 0.2),
                    "layer2.bias", Arrays.asList(0.3)
                )
            ),
            "training_metadata", Map.of(
                "samples_count", 2000,
                "algorithm", "NeuralNetwork",
                "framework", "pytorch"
            )
        );
        model1.setParameters(toJson(params1));
        models.add(model1);

        // VM 2 - 神经网络参数
        VmRoundModel model2 = createVmRoundModel("nn-model-2", "vm-002", 0.91, 0.09);
        Map<String, Object> params2 = Map.of(
            "model_parameters", Map.of(
                "weights", Map.of(
                    "layer1.weight", Arrays.asList(Arrays.asList(0.15, 0.25, 0.35), Arrays.asList(0.45, 0.55, 0.65)),
                    "layer2.weight", Arrays.asList(Arrays.asList(0.75, 0.85))
                ),
                "biases", Map.of(
                    "layer1.bias", Arrays.asList(0.15, 0.25),
                    "layer2.bias", Arrays.asList(0.35)
                )
            ),
            "training_metadata", Map.of(
                "samples_count", 2200,
                "algorithm", "NeuralNetwork",
                "framework", "pytorch"
            )
        );
        model2.setParameters(toJson(params2));
        models.add(model2);

        return models;
    }

    private VmRoundModel createVmRoundModel(String id, String vmId, double accuracy, double loss) {
        VmRoundModel model = new VmRoundModel();
        model.setId(id);
        model.setVmId(vmId);
        model.setTaskId("test-task-001");
        model.setRoundNumber(1);
        model.setAccuracy(BigDecimal.valueOf(accuracy));
        model.setLoss(BigDecimal.valueOf(loss));
        return model;
    }

    private Map<String, Object> createTaskConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("taskId", "test-task-001");
        config.put("roundNumber", 1);
        config.put("algorithm", "FEDERATED_AVERAGING");
        config.put("learningRate", 0.01);
        config.put("batchSize", 32);
        config.put("minParticipants", 2);
        return config;
    }

    private Map<String, Object> createExpectedRandomForestResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("feature_importances_", Arrays.asList(0.25, 0.30, 0.20, 0.25));
        result.put("aggregation_method", "FedAvg-RF");
        result.put("n_estimators", 100);
        result.put("global_samples", 4500);
        return result;
    }

    private Map<String, Object> createExpectedNeuralNetworkResult() {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> weights = new HashMap<>();
        weights.put("layer1.weight", Arrays.asList(Arrays.asList(0.125, 0.225, 0.325), Arrays.asList(0.425, 0.525, 0.625)));
        weights.put("layer2.weight", Arrays.asList(Arrays.asList(0.725, 0.825)));

        Map<String, Object> biases = new HashMap<>();
        biases.put("layer1.bias", Arrays.asList(0.125, 0.225));
        biases.put("layer2.bias", Arrays.asList(0.325));

        result.put("weights", weights);
        result.put("biases", biases);
        result.put("aggregation_method", "FedAvg-NN");
        return result;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}