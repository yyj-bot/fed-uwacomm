# FedUWAComm 联邦学习单元测试重构方案

**文档版本**: v1.0
**创建时间**: 2025-01-25
**最后更新**: 2025-01-25
**负责人**: FedUWAComm Team

## 📋 重构概述

基于新的联邦学习聚合架构重构，设计完整的单元测试体系，确保通用聚合引擎、WebSocket协议扩展、以及性能监控组件的质量和稳定性。

## 🎯 测试目标

1. **通用聚合引擎测试**：验证RandomForest和神经网络的聚合逻辑正确性
2. **策略模式测试**：确保多种聚合算法（FedAvg、FedProx等）实现正确
3. **WebSocket协议测试**：验证新增消息类型的处理逻辑
4. **集成测试增强**：完整联邦学习流程的端到端测试
5. **性能和并发测试**：验证系统在高负载下的稳定性

## 🧪 测试架构设计

### 1. 测试分层策略

```
┌─────────────────────────────────────────┐
│           E2E Integration Tests         │  ← 端到端集成测试
├─────────────────────────────────────────┤
│         Component Tests                 │  ← 组件测试
├─────────────────────────────────────────┤
│           Unit Tests                    │  ← 单元测试
└─────────────────────────────────────────┘
```

### 2. 测试覆盖范围

#### 单元测试（Unit Tests）
- `UniversalAggregationEngine` 聚合逻辑测试
- `AggregationStrategyFactory` 策略工厂测试
- 各种聚合策略（FedAvg、FedProx、Nova、Scaffold）测试
- `WebSocketProtocolService` 消息处理测试
- `AggregationMonitor` 性能监控测试

#### 组件测试（Component Tests）
- `FederatedAggregationService` 完整聚合流程测试
- WebSocket消息端到端测试
- 数据库操作集成测试

#### 集成测试（Integration Tests）
- 完整联邦学习流程测试
- 多VM并发聚合测试
- 异常场景恢复测试

## 🔧 核心测试实现

### 1. UniversalAggregationEngine 单元测试

```java
@ExtendWith(MockitoExtension.class)
class UniversalAggregationEngineTest {

    @Mock
    private AggregationStrategyFactory strategyFactory;

    @Mock
    private AggregationStrategy mockStrategy;

    @InjectMocks
    private UniversalAggregationEngine aggregationEngine;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("测试RandomForest模型聚合成功")
    void testRandomForestAggregation_Success() {
        // Given - 准备RandomForest测试数据
        List<VmRoundModel> models = createRandomForestModels();
        Map<String, Object> taskConfig = createTaskConfig();
        Map<String, Object> expectedResult = createExpectedRandomForestResult();

        when(strategyFactory.getStrategy(FederatedAlgorithm.FEDERATED_AVERAGING))
            .thenReturn(mockStrategy);
        when(mockStrategy.aggregate(models, taskConfig))
            .thenReturn(expectedResult);

        // When
        UniversalAggregationEngine.AggregationResult result =
            aggregationEngine.aggregate(models, FederatedAlgorithm.FEDERATED_AVERAGING, taskConfig);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getGlobalParameters()).isEqualTo(expectedResult);
        assertThat(result.getParticipantCount()).isEqualTo(3);
        assertThat(result.getAlgorithm()).isEqualTo("FEDERATED_AVERAGING");

        verify(strategyFactory).getStrategy(FederatedAlgorithm.FEDERATED_AVERAGING);
        verify(mockStrategy).aggregate(models, taskConfig);
    }

    @Test
    @DisplayName("测试神经网络模型聚合成功")
    void testNeuralNetworkAggregation_Success() {
        // Given - 准备神经网络测试数据
        List<VmRoundModel> models = createNeuralNetworkModels();
        Map<String, Object> taskConfig = createTaskConfig();
        Map<String, Object> expectedResult = createExpectedNeuralNetworkResult();

        when(strategyFactory.getStrategy(FederatedAlgorithm.FEDERATED_PROXIMAL))
            .thenReturn(mockStrategy);
        when(mockStrategy.aggregate(models, taskConfig))
            .thenReturn(expectedResult);

        // When
        UniversalAggregationEngine.AggregationResult result =
            aggregationEngine.aggregate(models, FederatedAlgorithm.FEDERATED_PROXIMAL, taskConfig);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getGlobalParameters()).containsKeys("weights", "biases");
        assertThat(result.getParticipantCount()).isEqualTo(5);
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
    @DisplayName("测试聚合异常处理")
    void testAggregation_ExceptionHandling() {
        // Given
        List<VmRoundModel> models = createRandomForestModels();
        when(strategyFactory.getStrategy(any())).thenThrow(new RuntimeException("策略获取失败"));

        // When
        UniversalAggregationEngine.AggregationResult result =
            aggregationEngine.aggregate(models, FederatedAlgorithm.FEDERATED_AVERAGING, new HashMap<>());

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("策略获取失败");
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

    // 测试数据构建方法
    private List<VmRoundModel> createRandomForestModels() {
        List<VmRoundModel> models = new ArrayList<>();

        // VM 1 - RandomForest参数
        VmRoundModel model1 = new VmRoundModel();
        model1.setId("model-1");
        model1.setVmId("vm-001");
        model1.setAccuracy(0.85);
        model1.setLoss(0.15);

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
        VmRoundModel model2 = new VmRoundModel();
        model2.setId("model-2");
        model2.setVmId("vm-002");
        model2.setAccuracy(0.82);
        model2.setLoss(0.18);

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
        VmRoundModel model3 = new VmRoundModel();
        model3.setId("model-3");
        model3.setVmId("vm-003");
        model3.setAccuracy(0.88);
        model3.setLoss(0.12);

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
        VmRoundModel model1 = new VmRoundModel();
        model1.setId("nn-model-1");
        model1.setVmId("vm-001");
        model1.setAccuracy(0.89);
        model1.setLoss(0.11);

        Map<String, Object> params1 = Map.of(
            "model_parameters", Map.of(
                "weights", Map.of(
                    "layer1.weight", Arrays.asList(Arrays.asList(0.1, 0.2, 0.3)),
                    "layer1.bias", Arrays.asList(0.1)
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

        return models;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
```

### 2. AggregationStrategy 策略测试

```java
@ExtendWith(MockitoExtension.class)
class FedAvgStrategyTest {

    private FedAvgStrategy fedAvgStrategy;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        fedAvgStrategy = new FedAvgStrategy();
    }

    @Test
    @DisplayName("测试RandomForest特征重要性聚合")
    void testRandomForestAggregation() {
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
        assertThat(sum).isCloseTo(1.0, within(0.001));

        // 验证加权平均结果合理性
        assertThat(globalImportances).hasSize(4);
        assertThat(globalImportances.get(0)).isPositive();
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
        assertThat(weights.stream().mapToDouble(Double::doubleValue).sum())
            .isCloseTo(1.0, within(0.001));

        // 验证权重与样本数量成正比
        assertThat(weights.get(0)).isLessThan(weights.get(2)); // vm1 < vm3 (1500 < 1800)
    }

    @Test
    @DisplayName("测试神经网络权重聚合")
    void testNeuralNetworkAggregation() {
        // Given
        List<VmRoundModel> models = createNeuralNetworkTestModels();
        Map<String, Object> taskConfig = new HashMap<>();

        // When
        Map<String, Object> result = fedAvgStrategy.aggregate(models, taskConfig);

        // Then
        assertThat(result).containsKey("weights");
        assertThat(result).containsKey("aggregation_method");
        assertThat(result.get("aggregation_method")).isEqualTo("FedAvg-NN");
    }

    private List<VmRoundModel> createRandomForestTestModels() {
        // 创建测试数据的辅助方法
        // ... (类似上面的实现)
    }
}
```

### 3. WebSocket协议测试扩展

```java
@ExtendWith(MockitoExtension.class)
class WebSocketProtocolServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private VmRoundModelsMapper vmRoundModelsMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private UuidUtil uuidUtil;

    @InjectMocks
    private WebSocketProtocolService webSocketProtocolService;

    @Test
    @DisplayName("测试梯度上传消息处理 - RandomForest")
    void testGradientUpload_RandomForest() {
        // Given
        ProtocolMessage message = createGradientUploadMessage("RandomForest");
        when(uuidUtil.generateUuid()).thenReturn("model-uuid-123");

        // When
        ProtocolAck ack = webSocketProtocolService.handle(message);

        // Then
        assertThat(ack.getType()).isEqualTo(ProtocolType.GRADIENT_UPLOAD_ACK);
        assertThat(ack.getData()).containsEntry("status", "RECEIVED");

        // 验证模型存储调用
        verify(vmRoundModelsMapper).upsertRoundModel(
            eq("model-uuid-123"),
            eq("task-001"),
            eq("vm-001"),
            eq(5),
            eq(0.87),
            eq(null),
            contains("feature_importances_")
        );

        // 验证事件发布
        verify(eventPublisher).publishEvent(any(ModelUploadEvent.class));
    }

    @Test
    @DisplayName("测试梯度上传消息处理 - 神经网络")
    void testGradientUpload_NeuralNetwork() {
        // Given
        ProtocolMessage message = createGradientUploadMessage("NeuralNetwork");
        when(uuidUtil.generateUuid()).thenReturn("nn-model-uuid-123");

        // When
        ProtocolAck ack = webSocketProtocolService.handle(message);

        // Then
        assertThat(ack.getType()).isEqualTo(ProtocolType.GRADIENT_UPLOAD_ACK);

        // 验证神经网络参数正确存储
        verify(vmRoundModelsMapper).upsertRoundModel(
            eq("nn-model-uuid-123"),
            eq("task-001"),
            eq("vm-001"),
            eq(5),
            eq(0.91),
            eq(0.09),
            contains("weights")
        );
    }

    @Test
    @DisplayName("测试全局模型广播")
    void testGlobalModelBroadcast() {
        // Given
        GlobalModel globalModel = createTestGlobalModel();
        List<String> activeVms = Arrays.asList("vm-001", "vm-002", "vm-003");

        when(webSocketProtocolService.getActiveVmIds("task-001")).thenReturn(activeVms);

        // When
        webSocketProtocolService.broadcastGlobalModel("task-001", globalModel);

        // Then
        // 验证向每个VM发送了广播消息
        for (String vmId : activeVms) {
            verify(messagingTemplate).convertAndSend(
                eq("/topic/vm/" + vmId),
                argThat(msg -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> msgMap = (Map<String, Object>) msg;
                    return "GLOBAL_MODEL_BROADCAST".equals(msgMap.get("type"));
                })
            );
        }
    }

    @Test
    @DisplayName("测试聚合触发条件检查")
    void testAggregationTriggerConditions() {
        // Given
        ProtocolMessage message = createGradientUploadMessage("RandomForest");

        // 模拟已有2个模型上传，第3个触发聚合
        when(vmRoundModelsMapper.countReadyModels("task-001", 5)).thenReturn(3);

        // When
        ProtocolAck ack = webSocketProtocolService.handle(message);

        // Then
        verify(eventPublisher).publishEvent(any(ModelUploadEvent.class));

        // 验证聚合事件的参数
        ArgumentCaptor<ModelUploadEvent> eventCaptor = ArgumentCaptor.forClass(ModelUploadEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        ModelUploadEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getTaskId()).isEqualTo("task-001");
        assertThat(capturedEvent.getRoundNumber()).isEqualTo(5);
        assertThat(capturedEvent.getVmId()).isEqualTo("vm-001");
    }

    private ProtocolMessage createGradientUploadMessage(String modelType) {
        Map<String, Object> trainingResult = new HashMap<>();

        if ("RandomForest".equals(modelType)) {
            trainingResult.put("model_parameters", Map.of(
                "feature_importances_", Arrays.asList(0.25, 0.20, 0.30, 0.25),
                "n_estimators", 100
            ));
            trainingResult.put("training_metadata", Map.of(
                "samples_count", 1500,
                "local_accuracy", 0.87,
                "algorithm", "RandomForest",
                "framework", "sklearn"
            ));
        } else {
            trainingResult.put("model_parameters", Map.of(
                "weights", Map.of(
                    "layer1.weight", Arrays.asList(Arrays.asList(0.1, 0.2)),
                    "layer1.bias", Arrays.asList(0.1)
                )
            ));
            trainingResult.put("training_metadata", Map.of(
                "samples_count", 2000,
                "local_accuracy", 0.91,
                "loss", 0.09,
                "algorithm", "NeuralNetwork",
                "framework", "pytorch"
            ));
        }

        Map<String, Object> data = Map.of(
            "taskId", "task-001",
            "round", 5,
            "training_result", trainingResult
        );

        return ProtocolMessage.builder()
            .type(ProtocolType.GRADIENT_UPLOAD)
            .vmId("vm-001")
            .data(data)
            .timestamp(Instant.now())
            .build();
    }
}
```

### 4. 性能和并发测试

```java
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FederatedAggregationPerformanceTest {

    @Autowired
    private UniversalAggregationEngine aggregationEngine;

    @Autowired
    private TestDataBuilder testDataBuilder;

    @Test
    @DisplayName("测试大规模RandomForest聚合性能")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testLargeScaleRandomForestAggregation() {
        // Given - 创建100个VM的模型数据
        List<VmRoundModel> models = testDataBuilder.createRandomForestModels(100);
        Map<String, Object> taskConfig = new HashMap<>();

        // When
        long startTime = System.currentTimeMillis();
        UniversalAggregationEngine.AggregationResult result =
            aggregationEngine.aggregate(models, FederatedAlgorithm.FEDERATED_AVERAGING, taskConfig);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(duration).isLessThan(10000); // 10秒内完成
        assertThat(result.getParticipantCount()).isEqualTo(100);

        System.out.println("100个VM聚合耗时: " + duration + "ms");
    }

    @Test
    @DisplayName("测试并发聚合安全性")
    void testConcurrentAggregationSafety() throws InterruptedException {
        // Given
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Future<UniversalAggregationEngine.AggregationResult>> futures = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // When - 并发执行聚合
        for (int i = 0; i < threadCount; i++) {
            final int taskIndex = i;
            Future<UniversalAggregationEngine.AggregationResult> future = executor.submit(() -> {
                try {
                    List<VmRoundModel> models = testDataBuilder.createRandomForestModels(5);
                    return aggregationEngine.aggregate(models,
                        FederatedAlgorithm.FEDERATED_AVERAGING, new HashMap<>());
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }

        latch.await(60, TimeUnit.SECONDS);

        // Then - 验证所有聚合都成功
        for (Future<UniversalAggregationEngine.AggregationResult> future : futures) {
            UniversalAggregationEngine.AggregationResult result = future.get();
            assertThat(result.isSuccess()).isTrue();
        }

        executor.shutdown();
    }

    @Test
    @DisplayName("测试内存使用效率")
    void testMemoryEfficiency() {
        // Given
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        // When - 执行大规模聚合
        for (int i = 0; i < 10; i++) {
            List<VmRoundModel> models = testDataBuilder.createRandomForestModels(50);
            aggregationEngine.aggregate(models, FederatedAlgorithm.FEDERATED_AVERAGING, new HashMap<>());

            // 强制垃圾回收
            System.gc();
        }

        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;

        // Then - 验证内存使用合理
        assertThat(memoryUsed).isLessThan(100 * 1024 * 1024); // 小于100MB
        System.out.println("内存使用: " + (memoryUsed / 1024 / 1024) + "MB");
    }
}
```

### 5. 集成测试重构

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnhancedFederatedLearningIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UniversalAggregationEngine aggregationEngine;

    @LocalServerPort
    private int port;

    private String baseUrl;
    private String adminAccessToken;
    private List<String> registeredVmIds;

    @BeforeAll
    void setupIntegrationTest() {
        baseUrl = "http://localhost:" + port;
        registeredVmIds = new ArrayList<>();

        System.out.println("🚀 增强版联邦学习集成测试开始");
        System.out.println("🌐 服务器地址: " + baseUrl);
    }

    @Test
    @Order(1)
    @DisplayName("1. 管理员登录获取访问令牌")
    void test01_AdminLogin() {
        // Given
        Map<String, String> loginRequest = Map.of(
            "username", "admin",
            "password", "ab123456"
        );

        // When
        ResponseEntity<Map> response = restTemplate.postForEntity(
            baseUrl + "/api/user/login", loginRequest, Map.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody.get("code")).isEqualTo(200);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
        adminAccessToken = (String) data.get("accessToken");

        assertThat(adminAccessToken).isNotNull();
        System.out.println("✅ 管理员登录成功，Token: " + adminAccessToken.substring(0, 20) + "...");
    }

    @Test
    @Order(2)
    @DisplayName("2. 注册多台虚拟机支持不同模型类型")
    void test02_RegisterMultipleVms() {
        HttpHeaders headers = createAuthHeaders();

        // 注册支持RandomForest的VM
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> vmRequest = Map.of(
                "vmName", "RandomForest-VM-" + i,
                "ipAddress", "192.168.1.10" + i,
                "port", 8080 + i,
                "cpuCores", 8,
                "memoryMb", 16384,
                "gpuCount", 0,
                "capabilities", Map.of(
                    "supportedMLAlgorithms", Arrays.asList("RandomForest", "DecisionTree"),
                    "framework", "sklearn"
                )
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(vmRequest, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/api/admin/vm/register", entity, Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            String vmId = (String) data.get("vmId");
            registeredVmIds.add(vmId);
        }

        // 注册支持神经网络的VM
        for (int i = 4; i <= 5; i++) {
            Map<String, Object> vmRequest = Map.of(
                "vmName", "NeuralNet-VM-" + i,
                "ipAddress", "192.168.1.10" + i,
                "port", 8080 + i,
                "cpuCores", 16,
                "memoryMb", 32768,
                "gpuCount", 2,
                "capabilities", Map.of(
                    "supportedMLAlgorithms", Arrays.asList("CNN", "LSTM", "Transformer"),
                    "framework", "pytorch"
                )
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(vmRequest, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/api/admin/vm/register", entity, Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            String vmId = (String) data.get("vmId");
            registeredVmIds.add(vmId);
        }

        assertThat(registeredVmIds).hasSize(5);
        System.out.println("✅ 成功注册5台VM，支持多种模型类型");
    }

    @Test
    @Order(3)
    @DisplayName("3. 创建混合模型类型的联邦学习任务")
    void test03_CreateMixedModelTask() {
        HttpHeaders headers = createAuthHeaders();

        // 创建支持多种模型的联邦学习任务
        Map<String, Object> taskRequest = Map.of(
            "taskName", "Mixed-Model-Federated-Task",
            "algorithm", "FEDERATED_AVERAGING", // 后端统一聚合算法
            "totalRounds", 10,
            "batchSize", 32,
            "learningRate", 0.001,
            "participantConfig", Map.of(
                "minParticipants", 3,
                "maxParticipants", 5,
                "participantSelection", "ALL"
            ),
            "datasetConfig", Map.of(
                "datasetType", "MIXED",
                "distributionMethod", "UNIFORM"
            ),
            "config", Map.of(
                "supportMixedModels", true,
                "aggregationStrategy", "UNIVERSAL",
                "convergenceThreshold", 0.001
            )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(taskRequest, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
            baseUrl + "/api/federated-task/create-smart", entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        System.out.println("✅ 混合模型联邦学习任务创建成功");
    }

    @Test
    @Order(4)
    @DisplayName("4. 测试通用聚合引擎直接调用")
    void test04_DirectUniversalAggregationTest() {
        // Given - 创建混合模型数据
        List<VmRoundModel> mixedModels = createMixedModelTestData();
        Map<String, Object> taskConfig = Map.of(
            "supportMixedModels", true,
            "aggregationStrategy", "UNIVERSAL"
        );

        // When - 直接调用聚合引擎
        UniversalAggregationEngine.AggregationResult result =
            aggregationEngine.aggregate(mixedModels, FederatedAlgorithm.FEDERATED_AVERAGING, taskConfig);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getParticipantCount()).isEqualTo(5);
        assertThat(result.getAggregationDuration()).isPositive();

        System.out.println("✅ 通用聚合引擎测试成功");
        System.out.println("   - 参与者数量: " + result.getParticipantCount());
        System.out.println("   - 聚合耗时: " + result.getAggregationDuration() + "ms");
    }

    private List<VmRoundModel> createMixedModelTestData() {
        List<VmRoundModel> models = new ArrayList<>();

        // 创建3个RandomForest模型
        for (int i = 0; i < 3; i++) {
            VmRoundModel model = new VmRoundModel();
            model.setId("rf-model-" + i);
            model.setTaskId("test-task");
            model.setVmId("vm-00" + (i + 1));
            model.setRoundNumber(1);
            model.setAccuracy(0.85 + i * 0.02);

            Map<String, Object> params = Map.of(
                "model_parameters", Map.of(
                    "feature_importances_", Arrays.asList(0.25, 0.30, 0.20, 0.25),
                    "n_estimators", 100
                ),
                "training_metadata", Map.of(
                    "algorithm", "RandomForest",
                    "framework", "sklearn",
                    "samples_count", 1500 + i * 200
                )
            );
            model.setParameters(toJson(params));
            models.add(model);
        }

        // 创建2个神经网络模型
        for (int i = 0; i < 2; i++) {
            VmRoundModel model = new VmRoundModel();
            model.setId("nn-model-" + i);
            model.setTaskId("test-task");
            model.setVmId("vm-00" + (i + 4));
            model.setRoundNumber(1);
            model.setAccuracy(0.89 + i * 0.01);
            model.setLoss(0.11 - i * 0.01);

            Map<String, Object> params = Map.of(
                "model_parameters", Map.of(
                    "weights", Map.of(
                        "layer1.weight", Arrays.asList(Arrays.asList(0.1 + i * 0.1, 0.2 + i * 0.1)),
                        "layer1.bias", Arrays.asList(0.1 + i * 0.05)
                    )
                ),
                "training_metadata", Map.of(
                    "algorithm", "NeuralNetwork",
                    "framework", "pytorch",
                    "samples_count", 2000 + i * 300
                )
            );
            model.setParameters(toJson(params));
            models.add(model);
        }

        return models;
    }

    private String toJson(Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminAccessToken);
        return headers;
    }
}
```

## 📊 测试覆盖率目标

### 代码覆盖率要求
- **单元测试**: 行覆盖率 ≥ 85%
- **分支覆盖率**: ≥ 80%
- **核心聚合逻辑**: 100%覆盖率

### 测试场景覆盖
- ✅ 正常流程测试
- ✅ 异常场景测试
- ✅ 边界条件测试
- ✅ 并发安全测试
- ✅ 性能基准测试

## 🗂️ 测试文件结构

```
src/test/java/com/feduwacomm/
├── aggregation/
│   ├── UniversalAggregationEngineTest.java
│   ├── AggregationStrategyFactoryTest.java
│   ├── FedAvgStrategyTest.java
│   ├── FedProxStrategyTest.java
│   └── AggregationMonitorTest.java
├── websocket/
│   ├── WebSocketProtocolServiceTest.java
│   └── ProtocolMessageHandlerTest.java
├── service/
│   └── FederatedAggregationServiceTest.java
├── integration/
│   ├── EnhancedFederatedLearningIntegrationTest.java
│   └── MixedModelAggregationIntegrationTest.java
└── performance/
    ├── FederatedAggregationPerformanceTest.java
    └── ConcurrentAggregationTest.java
```

## 📈 持续集成配置

### Maven测试配置

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
                <includes>
                    <include>**/*Test.java</include>
                    <include>**/*Tests.java</include>
                </includes>
                <excludes>
                    <exclude>**/performance/**/*Test.java</exclude>
                </excludes>
            </configuration>
        </plugin>

        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <executions>
                <execution>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

## ⚡ 测试执行策略

### 开发阶段
```bash
# 快速单元测试
mvn test -Dtest="**/aggregation/*Test"

# 完整测试套件
mvn test

# 集成测试
mvn test -Dtest="**/integration/*Test"
```

### 持续集成阶段
```bash
# 包含覆盖率报告的完整测试
mvn clean test jacoco:report

# 性能测试（可选）
mvn test -Dtest="**/performance/*Test" -DskipTests=false
```

## 📋 实施进度

- [x] ~~分析现有联邦学习单元测试结构~~
- [x] ~~设计新的聚合引擎单元测试~~
- [ ] 重构WebSocket协议测试
- [ ] 创建集成测试用例
- [ ] 添加性能测试和并发测试
- [ ] 完善测试文档和CI配置

## 🎯 预期效果

1. **质量保证**: 通过全面的单元测试确保聚合逻辑正确性
2. **回归预防**: 完整的测试套件防止代码变更引入bug
3. **性能监控**: 性能测试确保系统在高负载下稳定运行
4. **维护性提升**: 良好的测试覆盖率提升代码可维护性

本测试重构方案与联邦学习聚合重构方案紧密配合，确保新架构的质量和稳定性。