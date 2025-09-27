package com.feduwacomm.aggregation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.aggregation.AggregationException;
import com.feduwacomm.aggregation.strategy.FedAvgStrategy;
import com.feduwacomm.entity.VmRoundModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.data.Offset.offset;

/**
 * FedAvgStrategy 单元测试
 *
 * 测试联邦平均算法的核心功能：
 * - RandomForest特征重要性聚合
 * - 神经网络权重聚合
 * - 权重计算正确性
 * - 异常处理
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@DisplayName("联邦平均算法策略单元测试")
class FedAvgStrategyTest {

    private FedAvgStrategy fedAvgStrategy;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        fedAvgStrategy = new FedAvgStrategy();
    }

    @Test
    @DisplayName("测试RandomForest特征重要性聚合")
    void testRandomForestAggregation() throws AggregationException {
        // Given
        List<VmRoundModel> models = createRandomForestTestModels();
        Map<String, Object> taskConfig = new HashMap<>();

        // When
        Map<String, Object> result = fedAvgStrategy.aggregate(models, taskConfig);

        // Then
        assertThat(result).containsKey("feature_importances_");
        assertThat(result).containsKey("aggregation_method");
        assertThat(result.get("aggregation_method")).isEqualTo("FedAvg-RF");

        @SuppressWarnings("unchecked")
        List<Double> globalImportances = (List<Double>) result.get("feature_importances_");

        // 验证特征重要性归一化
        double sum = globalImportances.stream().mapToDouble(Double::doubleValue).sum();
        assertThat(sum).isCloseTo(1.0, offset(0.001));

        // 验证特征数量正确
        assertThat(globalImportances).hasSize(4);

        // 验证所有重要性都是正数
        for (Double importance : globalImportances) {
            assertThat(importance).isPositive();
        }

        // 验证加权平均结果的合理性（根据样本数量加权）
        // VM1: 1500样本, VM2: 1200样本, VM3: 1800样本
        // 总权重: 4500, 权重比例: VM1=1/3, VM2=4/15, VM3=2/5
        // 预期第一个特征重要性: (0.25*1500 + 0.20*1200 + 0.30*1800) / 4500 ≈ 0.25
        assertThat(globalImportances.get(0)).isCloseTo(0.25, offset(0.05));
    }

    @Test
    @DisplayName("测试权重计算正确性")
    void testWeightCalculation() {
        // Given
        List<VmRoundModel> models = createRandomForestTestModels();

        // When - 使用反射测试私有方法
        List<Double> weights = ReflectionTestUtils.invokeMethod(fedAvgStrategy,
            "calculateWeights", models);

        // Then
        assertThat(weights).hasSize(3);

        // 验证权重总和为1
        assertThat(weights.stream().mapToDouble(Double::doubleValue).sum())
            .isCloseTo(1.0, offset(0.001));

        // 验证权重与样本数量成正比
        // VM1: 1500样本 -> 权重 1500/4500 = 1/3 ≈ 0.333
        // VM2: 1200样本 -> 权重 1200/4500 = 4/15 ≈ 0.267
        // VM3: 1800样本 -> 权重 1800/4500 = 2/5 = 0.4
        assertThat(weights.get(0)).isCloseTo(0.333, offset(0.01));
        assertThat(weights.get(1)).isCloseTo(0.267, offset(0.01));
        assertThat(weights.get(2)).isCloseTo(0.4, offset(0.01));

        // 验证权重排序：vm2 < vm1 < vm3
        assertThat(weights.get(1)).isLessThan(weights.get(0));
        assertThat(weights.get(0)).isLessThan(weights.get(2));
    }

    @Test
    @DisplayName("测试神经网络权重聚合")
    void testNeuralNetworkAggregation() throws AggregationException {
        // Given
        List<VmRoundModel> models = createNeuralNetworkTestModels();
        Map<String, Object> taskConfig = new HashMap<>();

        // When
        Map<String, Object> result = fedAvgStrategy.aggregate(models, taskConfig);

        // Then
        assertThat(result).containsKey("weights");
        assertThat(result).containsKey("aggregation_method");
        assertThat(result.get("aggregation_method")).isEqualTo("FedAvg-NN");

        @SuppressWarnings("unchecked")
        Map<String, Object> aggregatedWeights = (Map<String, Object>) result.get("weights");

        assertThat(aggregatedWeights).containsKeys("layer1.weight", "layer2.weight");

        // 验证layer1权重结构
        @SuppressWarnings("unchecked")
        List<List<Double>> layer1Weights = (List<List<Double>>) aggregatedWeights.get("layer1.weight");
        assertThat(layer1Weights).hasSize(2);
        assertThat(layer1Weights.get(0)).hasSize(3);
        assertThat(layer1Weights.get(1)).hasSize(3);

        // 验证聚合结果的合理性（应该是两个模型的加权平均）
        // VM1: 2000样本, VM2: 2200样本，权重比例约为 10:11
        double weight1 = 2000.0 / (2000 + 2200);
        double weight2 = 2200.0 / (2000 + 2200);

        // 验证第一个权重的聚合结果
        double expected = 0.1 * weight1 + 0.15 * weight2;
        assertThat(layer1Weights.get(0).get(0)).isCloseTo(expected, offset(0.01));
    }

    @Test
    @DisplayName("测试不同模型类型混合处理")
    void testMixedModelTypesHandling() throws AggregationException {
        // Given
        List<VmRoundModel> models = new ArrayList<>();
        models.addAll(createRandomForestTestModels());
        models.addAll(createNeuralNetworkTestModels());

        Map<String, Object> taskConfig = new HashMap<>();

        // When & Then - 应该抛出异常，因为混合了不同类型的模型
        assertThatThrownBy(() -> fedAvgStrategy.aggregate(models, taskConfig))
            .isInstanceOf(AggregationException.class)
            .hasMessageContaining("不支持的模型类型");
    }

    @Test
    @DisplayName("测试空模型列表异常")
    void testEmptyModelsException() {
        // Given
        List<VmRoundModel> emptyModels = Collections.emptyList();
        Map<String, Object> taskConfig = new HashMap<>();

        // When & Then
        assertThatThrownBy(() -> fedAvgStrategy.aggregate(emptyModels, taskConfig))
            .isInstanceOf(AggregationException.class)
            .hasMessageContaining("模型列表不能为空");
    }

    @Test
    @DisplayName("测试无效JSON参数异常")
    void testInvalidJsonParametersException() {
        // Given
        List<VmRoundModel> models = createRandomForestTestModels();
        models.get(0).setParameters("invalid json {");

        Map<String, Object> taskConfig = new HashMap<>();

        // When & Then
        assertThatThrownBy(() -> fedAvgStrategy.aggregate(models, taskConfig))
            .isInstanceOf(AggregationException.class)
            .hasMessageContaining("解析模型参数失败");
    }

    @Test
    @DisplayName("测试缺少训练元数据的情况")
    void testMissingTrainingMetadata() throws AggregationException {
        // Given - 创建缺少训练元数据的模型
        List<VmRoundModel> models = new ArrayList<>();
        VmRoundModel model = createVmRoundModel("model-1", "vm-001", 0.85, 0.15);

        Map<String, Object> params = Map.of(
            "model_parameters", Map.of(
                "feature_importances_", Arrays.asList(0.25, 0.30, 0.20, 0.25),
                "n_estimators", 100
            )
            // 注意：缺少training_metadata
        );
        model.setParameters(toJson(params));
        models.add(model);

        Map<String, Object> taskConfig = new HashMap<>();

        // When
        Map<String, Object> result = fedAvgStrategy.aggregate(models, taskConfig);

        // Then - 应该能够处理，但使用默认权重
        assertThat(result).containsKey("feature_importances_");
        assertThat(result).containsKey("aggregation_method");
    }

    @Test
    @DisplayName("测试单个模型的聚合")
    void testSingleModelAggregation() throws AggregationException {
        // Given
        List<VmRoundModel> models = Collections.singletonList(createRandomForestTestModels().get(0));
        Map<String, Object> taskConfig = new HashMap<>();

        // When
        Map<String, Object> result = fedAvgStrategy.aggregate(models, taskConfig);

        // Then
        assertThat(result).containsKey("feature_importances_");

        @SuppressWarnings("unchecked")
        List<Double> globalImportances = (List<Double>) result.get("feature_importances_");

        // 单个模型的聚合结果应该就是原模型的特征重要性
        assertThat(globalImportances).containsExactly(0.25, 0.30, 0.20, 0.25);
    }

    @Test
    @DisplayName("测试策略基础信息")
    void testStrategyBasicInfo() {
        // When & Then
        assertThat(fedAvgStrategy.getStrategyName()).isEqualTo("FedAvg");
        assertThat(fedAvgStrategy.getMinimumParticipants()).isEqualTo(2);

        List<String> supportedTypes = fedAvgStrategy.getSupportedModelTypes();
        assertThat(supportedTypes).containsExactlyInAnyOrder("UNIVERSAL", "RandomForest", "NeuralNetwork");
    }

    @Test
    @DisplayName("测试特征重要性归一化")
    void testFeatureImportanceNormalization() {
        // Given - 创建特征重要性不归一化的测试数据
        List<VmRoundModel> models = new ArrayList<>();
        VmRoundModel model = createVmRoundModel("model-1", "vm-001", 0.85, 0.15);

        // 故意设置不归一化的特征重要性
        Map<String, Object> params = Map.of(
            "model_parameters", Map.of(
                "feature_importances_", Arrays.asList(2.0, 3.0, 1.5, 2.5), // 总和=9
                "n_estimators", 100
            ),
            "training_metadata", Map.of(
                "samples_count", 1500,
                "algorithm", "RandomForest",
                "framework", "sklearn"
            )
        );
        model.setParameters(toJson(params));
        models.add(model);

        Map<String, Object> taskConfig = new HashMap<>();

        // When - 使用反射调用私有方法测试归一化
        @SuppressWarnings("unchecked")
        List<Double> normalized = ReflectionTestUtils.invokeMethod(fedAvgStrategy,
            "normalizeFeatureImportances", Arrays.asList(2.0, 3.0, 1.5, 2.5));

        // Then
        double sum = normalized.stream().mapToDouble(Double::doubleValue).sum();
        assertThat(sum).isCloseTo(1.0, offset(0.001));

        // 验证比例保持不变
        assertThat(normalized.get(0)).isCloseTo(2.0/9.0, offset(0.01));
        assertThat(normalized.get(1)).isCloseTo(3.0/9.0, offset(0.01));
        assertThat(normalized.get(2)).isCloseTo(1.5/9.0, offset(0.01));
        assertThat(normalized.get(3)).isCloseTo(2.5/9.0, offset(0.01));
    }

    // ================= 测试数据构建方法 =================

    private List<VmRoundModel> createRandomForestTestModels() {
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

    private List<VmRoundModel> createNeuralNetworkTestModels() {
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

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}