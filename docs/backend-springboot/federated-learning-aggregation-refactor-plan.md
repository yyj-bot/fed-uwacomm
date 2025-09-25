# FedUWAComm 联邦学习聚合与WebSocket重构方案

**文档版本**: v1.0
**创建时间**: 2025-01-25
**最后更新**: 2025-01-25
**负责人**: FedUWAComm Team

## 📋 重构概述

基于现有数据库结构和枚举设计，重构后端联邦学习聚合功能和WebSocket消息处理，实现通用的聚合引擎，支持RandomForest和神经网络等多种模型的联邦学习。

## 🎯 核心目标

1. **复用现有枚举**：使用已有的`FEDERATED_AVERAGING`、`FEDERATED_PROXIMAL`、`FEDERATED_NOVA`、`SCAFFOLD`算法
2. **通用聚合引擎**：后端实现统一的聚合逻辑，无论VM使用RF还是神经网络
3. **标准化WebSocket协议**：按梯度聚合指导文档实现标准消息格式
4. **性能监控增强**：添加聚合性能指标和异常处理

## 🔧 重构内容

### 1. 联邦学习聚合服务重构

#### 核心组件设计

```java
// 通用聚合引擎 - 支持多种模型类型
@Service
public class UniversalAggregationEngine {
    // 根据现有枚举实现不同聚合算法
    public AggregationResult aggregate(List<VmRoundModel> models, FederatedAlgorithm algorithm)
}

// 聚合策略工厂
@Component
public class AggregationStrategyFactory {
    // 基于现有枚举返回对应策略
    public AggregationStrategy getStrategy(FederatedAlgorithm algorithm)
}
```

#### 支持的聚合算法
- **FEDERATED_AVERAGING**: FedAvg通用实现，支持参数加权平均
- **FEDERATED_PROXIMAL**: FedProx带正则化的聚合
- **FEDERATED_NOVA**: Nova算法实现
- **SCAFFOLD**: Scaffold算法实现

### 2. WebSocket协议扩展

#### 新增消息类型（扩展现有ProtocolType）

```java
// 梯度上传相关
GRADIENT_UPLOAD,
GRADIENT_UPLOAD_ACK,
AGGREGATION_TRIGGERED,
AGGREGATION_COMPLETED,

// 全局模型分发相关
GLOBAL_MODEL_BROADCAST,
MODEL_DISTRIBUTION_ACK,
```

#### 标准消息格式

```json
// 梯度上传格式（支持任意模型类型）
{
  "type": "GRADIENT_UPLOAD",
  "vmId": "vm_001",
  "data": {
    "taskId": "task_001",
    "round": 5,
    "training_result": {
      "model_parameters": {
        // RandomForest参数示例
        "feature_importances_": [0.25, 0.20, 0.15, 0.18, 0.12, 0.10],
        "n_estimators": 100
        // 或神经网络权重参数
      },
      "parameter_deltas": {
        "feature_importances_delta": [0.02, -0.01, 0.03, -0.02, 0.01, -0.03]
      },
      "training_metadata": {
        "samples_count": 1500,
        "training_time": 45.2,
        "local_accuracy": 0.87,
        "convergence_status": "converged",
        "algorithm": "RandomForest", // 或 "NeuralNetwork"
        "framework": "sklearn" // 或 "pytorch"
      }
    }
  }
}

// 全局模型分发格式
{
  "type": "GLOBAL_MODEL_BROADCAST",
  "taskId": "task_001",
  "round": 6,
  "global_model": {
    "model_metadata": {
      "model_id": "global_model_task_001_round_6",
      "model_type": "universal", // 通用模型格式
      "algorithm": "RandomForest", // 或检测到的算法类型
      "created_at": "2025-01-25T10:35:45Z",
      "version": "round_6"
    },
    "parameters": {
      // 聚合后的全局参数（格式与本地参数保持一致）
    },
    "aggregation_info": {
      "method": "FEDERATED_AVERAGING",
      "participants": 8,
      "convergence_metrics": {
        "parameter_change": 0.0023,
        "improvement": 0.012
      }
    }
  }
}
```

### 3. 数据库结构利用

#### 现有表结构复用

**federated_tasks表**：
- 使用现有`algorithm`枚举：`FEDERATED_AVERAGING`, `FEDERATED_PROXIMAL`, `FEDERATED_NOVA`, `SCAFFOLD`
- 利用`config`和`workflow_config` JSON字段存储聚合配置

**global_models表**：
- 复用`aggregation_method`字段对应现有算法枚举
- `global_parameters` JSON字段支持任意模型参数格式
- 利用现有`distribution_status`和`distributed_vms`字段管理模型分发

**vm_round_models表**：
- `parameters` JSON字段足够灵活，支持RandomForest特征重要性或神经网络权重
- 现有`accuracy`和`loss`字段继续使用

#### 无需数据库结构变更
- JSON字段足够灵活支持不同模型参数格式
- 现有枚举完全满足聚合算法需求
- 现有索引和约束保持不变

### 4. 重构的核心服务

#### UniversalAggregationEngine（新增）

```java
@Service
@Slf4j
public class UniversalAggregationEngine {

    public static class AggregationResult {
        private boolean success;
        private Map<String, Object> globalParameters;
        private Map<String, Double> globalMetrics;
        private int participantCount;
        private long aggregationDuration;
        private String algorithm;
        private String errorMessage;
        // getters and setters
    }

    // 通用聚合方法 - 支持多种模型类型
    public AggregationResult aggregate(List<VmRoundModel> models,
                                     FederatedAlgorithm algorithm,
                                     Map<String, Object> taskConfig) {

        long startTime = System.currentTimeMillis();

        try {
            // 检测模型类型
            String modelType = detectModelType(models);
            log.info("检测到模型类型: {}, 使用聚合算法: {}", modelType, algorithm);

            // 根据算法选择聚合策略
            AggregationStrategy strategy = strategyFactory.getStrategy(algorithm);

            // 执行聚合
            Map<String, Object> aggregatedParams = strategy.aggregate(models, taskConfig);
            Map<String, Double> metrics = calculateGlobalMetrics(models, aggregatedParams);

            return AggregationResult.success(
                aggregatedParams,
                metrics,
                models.size(),
                System.currentTimeMillis() - startTime,
                algorithm.name()
            );

        } catch (Exception e) {
            log.error("聚合执行失败: {}", e.getMessage(), e);
            return AggregationResult.failure(e.getMessage());
        }
    }

    // 自动检测模型类型
    private String detectModelType(List<VmRoundModel> models) {
        // 通过参数结构检测是RandomForest还是神经网络
        if (!models.isEmpty()) {
            Map<String, Object> params = parseParameters(models.get(0));
            if (params.containsKey("feature_importances_")) {
                return "RandomForest";
            } else if (params.containsKey("weights") || params.containsKey("layers")) {
                return "NeuralNetwork";
            }
        }
        return "Unknown";
    }
}
```

#### AggregationStrategyFactory（新增）

```java
@Component
public class AggregationStrategyFactory {

    private final Map<FederatedAlgorithm, AggregationStrategy> strategies;

    public AggregationStrategyFactory() {
        strategies = Map.of(
            FederatedAlgorithm.FEDERATED_AVERAGING, new FedAvgStrategy(),
            FederatedAlgorithm.FEDERATED_PROXIMAL, new FedProxStrategy(),
            FederatedAlgorithm.FEDERATED_NOVA, new FedNovaStrategy(),
            FederatedAlgorithm.SCAFFOLD, new ScaffoldStrategy()
        );
    }

    public AggregationStrategy getStrategy(FederatedAlgorithm algorithm) {
        AggregationStrategy strategy = strategies.get(algorithm);
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的聚合算法: " + algorithm);
        }
        return strategy;
    }
}
```

#### FederatedAggregationService（重构）

```java
@Service
@Slf4j
public class FederatedAggregationService {

    private final UniversalAggregationEngine aggregationEngine;
    private final GlobalModelMapper globalModelMapper;
    private final FederatedTasksMapper federatedTasksMapper;
    private final VmRoundModelsMapper vmRoundModelsMapper;

    // 重构聚合触发逻辑
    @EventListener
    @Async
    public void handleModelUploadEvent(ModelUploadEvent event) {
        String taskId = event.getTaskId();
        Integer roundNumber = event.getRoundNumber();

        log.info("处理模型上传事件: taskId={}, round={}, vmId={}",
                taskId, roundNumber, event.getVmId());

        // 检查聚合条件
        if (shouldTriggerAggregation(taskId, roundNumber)) {
            triggerUniversalAggregation(taskId, roundNumber);
        }
    }

    // 通用聚合执行方法
    private void triggerUniversalAggregation(String taskId, Integer roundNumber) {
        try {
            // 获取任务信息
            FederatedTask task = federatedTasksMapper.selectTaskById(taskId);
            if (task == null) {
                throw new IllegalArgumentException("任务不存在: " + taskId);
            }

            // 获取本轮所有本地模型
            List<VmRoundModel> localModels = vmRoundModelsMapper
                    .selectByTaskIdAndRound(taskId, roundNumber);

            if (localModels.isEmpty()) {
                throw new IllegalArgumentException("没有可用的本地模型");
            }

            // 使用通用聚合引擎执行聚合
            UniversalAggregationEngine.AggregationResult result =
                aggregationEngine.aggregate(localModels, task.getAlgorithm(),
                    parseTaskConfig(task.getConfig()));

            if (result.isSuccess()) {
                // 保存全局模型
                GlobalModel globalModel = saveGlobalModel(task, roundNumber, result);

                // 广播全局模型
                broadcastGlobalModel(task, globalModel);

                log.info("聚合成功完成: taskId={}, round={}, participants={}",
                        taskId, roundNumber, result.getParticipantCount());
            } else {
                throw new RuntimeException("聚合失败: " + result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("聚合执行异常: taskId={}, round={}, error={}",
                    taskId, roundNumber, e.getMessage(), e);
        }
    }
}
```

#### WebSocketProtocolService（扩展）

```java
@Service
public class WebSocketProtocolService {

    // 新增：处理梯度上传消息
    private ProtocolAck onGradientUpload(ProtocolMessage msg) {
        Map<String, Object> data = msg.getData();
        String vmId = msg.getVmId();
        String taskId = valueAsString(data, "taskId");
        Integer round = numberAsInt(data, "round");

        @SuppressWarnings("unchecked")
        Map<String, Object> trainingResult = (Map<String, Object>) data.get("training_result");

        if (trainingResult != null) {
            // 提取模型参数和元数据
            @SuppressWarnings("unchecked")
            Map<String, Object> modelParams = (Map<String, Object>)
                trainingResult.get("model_parameters");
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>)
                trainingResult.get("training_metadata");

            // 构建存储格式
            Map<String, Object> storeParams = Map.of(
                "model_parameters", modelParams,
                "training_metadata", metadata,
                "parameter_deltas", trainingResult.get("parameter_deltas")
            );

            Double accuracy = numberAsDouble(metadata, "local_accuracy");
            Double loss = numberAsDouble(metadata, "loss");
            String parametersJson = toJsonSafe(storeParams);

            // 存储到vm_round_models表
            vmRoundModelsMapper.upsertRoundModel(
                uuidUtil.generateUuid(), taskId, vmId, round,
                accuracy, loss, parametersJson);

            // 发布模型上传事件触发聚合检查
            ModelUploadEvent uploadEvent = new ModelUploadEvent(
                this, taskId, round, vmId,
                (long) parametersJson.length(), accuracy, loss);
            eventPublisher.publishEvent(uploadEvent);
        }

        return ackFor(msg, ProtocolType.GRADIENT_UPLOAD_ACK, Map.of(
            "status", "RECEIVED",
            "taskId", taskId,
            "round", round
        ));
    }

    // 新增：全局模型广播方法
    public void broadcastGlobalModel(String taskId, GlobalModel globalModel) {
        try {
            // 构建广播消息
            Map<String, Object> broadcastMsg = Map.of(
                "type", "GLOBAL_MODEL_BROADCAST",
                "taskId", taskId,
                "round", globalModel.getRoundNumber(),
                "global_model", Map.of(
                    "model_metadata", Map.of(
                        "model_id", globalModel.getId(),
                        "model_type", "universal",
                        "created_at", globalModel.getCreatedAt().toString(),
                        "version", "round_" + globalModel.getRoundNumber()
                    ),
                    "parameters", parseJsonSafe(globalModel.getGlobalParameters()),
                    "aggregation_info", Map.of(
                        "method", globalModel.getAggregationMethod().name(),
                        "participants", globalModel.getParticipantCount()
                    )
                )
            );

            // 广播给所有活跃的VM
            List<String> activeVms = getActiveVmIds(taskId);
            for (String vmId : activeVms) {
                messagingTemplate.convertAndSend("/topic/vm/" + vmId, broadcastMsg);
            }

            log.info("全局模型已广播: taskId={}, round={}, vmCount={}",
                    taskId, globalModel.getRoundNumber(), activeVms.size());

        } catch (Exception e) {
            log.error("全局模型广播失败: taskId={}, error={}", taskId, e.getMessage(), e);
        }
    }
}
```

## 📊 聚合算法实现策略

### FedAvg策略（通用加权平均）

```java
@Component
public class FedAvgStrategy implements AggregationStrategy {

    @Override
    public Map<String, Object> aggregate(List<VmRoundModel> models,
                                       Map<String, Object> taskConfig) {

        // 计算权重（基于样本数量）
        List<Double> weights = calculateWeights(models);

        // 获取第一个模型的参数结构
        Map<String, Object> firstParams = parseParameters(models.get(0));

        // 根据参数类型选择聚合方法
        if (firstParams.containsKey("feature_importances_")) {
            return aggregateRandomForest(models, weights);
        } else {
            return aggregateNeuralNetwork(models, weights);
        }
    }

    // RandomForest特征重要性聚合
    private Map<String, Object> aggregateRandomForest(List<VmRoundModel> models,
                                                    List<Double> weights) {
        // 提取特征重要性
        List<double[]> importancesList = new ArrayList<>();
        for (VmRoundModel model : models) {
            Map<String, Object> params = parseParameters(model);
            @SuppressWarnings("unchecked")
            List<Double> importances = (List<Double>)
                params.get("feature_importances_");
            importancesList.add(importances.stream()
                .mapToDouble(Double::doubleValue).toArray());
        }

        // 加权平均聚合
        int featureCount = importancesList.get(0).length;
        double[] globalImportances = new double[featureCount];

        for (int i = 0; i < models.size(); i++) {
            double[] localImportances = importancesList.get(i);
            double weight = weights.get(i);

            for (int j = 0; j < featureCount; j++) {
                globalImportances[j] += weight * localImportances[j];
            }
        }

        // 归一化
        double sum = Arrays.stream(globalImportances).sum();
        for (int i = 0; i < globalImportances.length; i++) {
            globalImportances[i] /= sum;
        }

        // 构建返回参数
        Map<String, Object> result = new HashMap<>();
        result.put("feature_importances_",
            Arrays.stream(globalImportances).boxed().collect(Collectors.toList()));
        result.put("n_estimators",
            parseParameters(models.get(0)).get("n_estimators"));
        result.put("aggregation_method", "FedAvg-RF");

        return result;
    }

    // 神经网络权重聚合
    private Map<String, Object> aggregateNeuralNetwork(List<VmRoundModel> models,
                                                     List<Double> weights) {
        // TODO: 实现神经网络权重的加权平均聚合
        return new HashMap<>();
    }
}
```

## 🗂️ 实施进度跟踪

### 第一阶段：核心聚合组件 ✅
- [x] ~~创建重构文档并保存重构计划~~
- [ ] 实现UniversalAggregationEngine通用聚合引擎
- [ ] 实现AggregationStrategyFactory策略工厂
- [ ] 实现FedAvgStrategy和FedProxStrategy

### 第二阶段：WebSocket协议扩展
- [ ] 扩展ProtocolType枚举添加新消息类型
- [ ] 扩展WebSocketProtocolService消息处理
- [ ] 实现全局模型广播机制

### 第三阶段：聚合服务重构
- [ ] 重构FederatedAggregationService聚合逻辑
- [ ] 集成UniversalAggregationEngine
- [ ] 完善异常处理和日志记录

### 第四阶段：性能监控组件
- [ ] 添加AggregationMonitor性能监控
- [ ] 实现聚合性能指标收集
- [ ] 添加聚合异常告警机制

### 第五阶段：集成测试
- [ ] 编写单元测试用例
- [ ] 执行集成测试验证
- [ ] 性能测试和优化

## 🗂️ 文件变更清单

### 新增文件 (6个)
- `UniversalAggregationEngine.java` - 通用聚合引擎
- `AggregationStrategyFactory.java` - 策略工厂
- `AggregationStrategy.java` - 聚合策略接口
- `FedAvgStrategy.java` - FedAvg实现
- `FedProxStrategy.java` - FedProx实现
- `AggregationMonitor.java` - 性能监控

### 修改文件 (4个)
- `FederatedAggregationService.java` - 重构聚合逻辑
- `WebSocketProtocolService.java` - 扩展消息处理
- `ProtocolType.java` - 添加新消息类型
- `GlobalModelDistributionService.java` - 增强分发功能

### 数据库变更 (0个)
- **无需任何数据库结构变更**
- 完全利用现有表结构和枚举
- JSON字段提供足够灵活性

## ⚡ 实施优势

1. **最小侵入性**：无需修改数据库结构，复用现有枚举
2. **向后兼容**：保持现有API接口不变
3. **通用性强**：支持RF、NN等多种模型类型
4. **扩展性好**：策略模式便于添加新算法
5. **性能优化**：基于现有并发和缓存机制

## 📈 预期效果

- 实现标准化的联邦学习聚合流程
- 支持多种模型的统一聚合处理
- 提供完善的WebSocket通信协议
- 保持系统架构的简洁性和一致性

## 🧪 单元测试重构方案

### 1. 测试架构设计

#### 测试分层策略
```
┌─────────────────────────────────────────┐
│           E2E Integration Tests         │  ← 端到端集成测试
├─────────────────────────────────────────┤
│         Component Tests                 │  ← 组件测试
├─────────────────────────────────────────┤
│           Unit Tests                    │  ← 单元测试
└─────────────────────────────────────────┘
```

#### 核心测试组件

**UniversalAggregationEngine 测试**：
```java
@ExtendWith(MockitoExtension.class)
class UniversalAggregationEngineTest {

    @Mock
    private AggregationStrategyFactory strategyFactory;

    @InjectMocks
    private UniversalAggregationEngine aggregationEngine;

    @Test
    @DisplayName("测试RandomForest模型聚合成功")
    void testRandomForestAggregation_Success() {
        // Given - 准备RandomForest测试数据
        List<VmRoundModel> models = createRandomForestModels();

        // When
        AggregationResult result = aggregationEngine.aggregate(
            models, FederatedAlgorithm.FEDERATED_AVERAGING, taskConfig);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getGlobalParameters())
            .containsKey("feature_importances_");
        assertThat(result.getParticipantCount()).isEqualTo(3);
    }
}
```

**聚合策略测试**：
```java
@ExtendWith(MockitoExtension.class)
class FedAvgStrategyTest {

    @Test
    @DisplayName("测试RandomForest特征重要性聚合")
    void testRandomForestAggregation() {
        // 验证特征重要性加权平均和归一化
        List<VmRoundModel> models = createRandomForestTestModels();

        Map<String, Object> result = fedAvgStrategy.aggregate(models, taskConfig);

        @SuppressWarnings("unchecked")
        List<Double> globalImportances = (List<Double>) result.get("feature_importances_");

        // 验证归一化：总和为1
        double sum = globalImportances.stream().mapToDouble(Double::doubleValue).sum();
        assertThat(sum).isCloseTo(1.0, within(0.001));
    }
}
```

**WebSocket协议测试扩展**：
```java
@ExtendWith(MockitoExtension.class)
class WebSocketProtocolServiceTest {

    @Test
    @DisplayName("测试梯度上传消息处理 - RandomForest")
    void testGradientUpload_RandomForest() {
        // Given
        ProtocolMessage message = createGradientUploadMessage("RandomForest");

        // When
        ProtocolAck ack = webSocketProtocolService.handle(message);

        // Then
        assertThat(ack.getType()).isEqualTo(ProtocolType.GRADIENT_UPLOAD_ACK);
        verify(vmRoundModelsMapper).upsertRoundModel(
            any(), eq("task-001"), eq("vm-001"), eq(5),
            eq(0.87), any(), contains("feature_importances_"));
        verify(eventPublisher).publishEvent(any(ModelUploadEvent.class));
    }
}
```

### 2. 性能和并发测试

**大规模聚合性能测试**：
```java
@Test
@DisplayName("测试大规模RandomForest聚合性能")
@Timeout(value = 30, unit = TimeUnit.SECONDS)
void testLargeScaleRandomForestAggregation() {
    // Given - 创建100个VM的模型数据
    List<VmRoundModel> models = testDataBuilder.createRandomForestModels(100);

    // When
    long startTime = System.currentTimeMillis();
    AggregationResult result = aggregationEngine.aggregate(
        models, FederatedAlgorithm.FEDERATED_AVERAGING, taskConfig);
    long duration = System.currentTimeMillis() - startTime;

    // Then
    assertThat(result.isSuccess()).isTrue();
    assertThat(duration).isLessThan(10000); // 10秒内完成
    assertThat(result.getParticipantCount()).isEqualTo(100);
}
```

**并发安全测试**：
```java
@Test
@DisplayName("测试并发聚合安全性")
void testConcurrentAggregationSafety() throws InterruptedException {
    int threadCount = 10;
    CountDownLatch latch = new CountDownLatch(threadCount);
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);

    // 并发执行聚合操作，验证线程安全性
    for (int i = 0; i < threadCount; i++) {
        executor.submit(() -> {
            try {
                List<VmRoundModel> models = testDataBuilder.createRandomForestModels(5);
                AggregationResult result = aggregationEngine.aggregate(
                    models, FederatedAlgorithm.FEDERATED_AVERAGING, new HashMap<>());
                assertThat(result.isSuccess()).isTrue();
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await(60, TimeUnit.SECONDS);
}
```

### 3. 集成测试增强

**混合模型类型集成测试**：
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EnhancedFederatedLearningIntegrationTest {

    @Test
    @Order(4)
    @DisplayName("测试通用聚合引擎直接调用")
    void test04_DirectUniversalAggregationTest() {
        // Given - 创建混合模型数据（RF + NN）
        List<VmRoundModel> mixedModels = createMixedModelTestData();
        Map<String, Object> taskConfig = Map.of(
            "supportMixedModels", true,
            "aggregationStrategy", "UNIVERSAL"
        );

        // When - 直接调用聚合引擎
        AggregationResult result = aggregationEngine.aggregate(
            mixedModels, FederatedAlgorithm.FEDERATED_AVERAGING, taskConfig);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getParticipantCount()).isEqualTo(5);
        assertThat(result.getAggregationDuration()).isPositive();
    }
}
```

### 4. 测试覆盖率目标

- **单元测试**: 行覆盖率 ≥ 85%
- **分支覆盖率**: ≥ 80%
- **核心聚合逻辑**: 100%覆盖率

### 5. 测试文件结构

```
src/test/java/com/feduwacomm/
├── aggregation/
│   ├── UniversalAggregationEngineTest.java
│   ├── AggregationStrategyFactoryTest.java
│   ├── FedAvgStrategyTest.java
│   └── FedProxStrategyTest.java
├── websocket/
│   └── WebSocketProtocolServiceTest.java
├── integration/
│   └── EnhancedFederatedLearningIntegrationTest.java
└── performance/
    └── FederatedAggregationPerformanceTest.java
```

### 6. 持续集成配置

```bash
# 快速单元测试
mvn test -Dtest="**/aggregation/*Test"

# 包含覆盖率的完整测试
mvn clean test jacoco:report

# 性能测试
mvn test -Dtest="**/performance/*Test"
```

## 📝 备注

本重构方案严格遵循现有系统架构和数据库设计，通过最小化的代码变更实现最大的功能提升。所有新增组件都与现有系统保持良好的兼容性和一致性。

完整的单元测试重构方案确保新架构的质量和稳定性，包含：
- 全面的单元测试覆盖
- 性能和并发安全测试
- 增强的集成测试
- 持续集成配置