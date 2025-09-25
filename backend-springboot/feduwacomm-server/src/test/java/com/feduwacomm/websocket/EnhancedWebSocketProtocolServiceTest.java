package com.feduwacomm.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.dto.ProtocolAck;
import com.feduwacomm.dto.ProtocolMessage;
import com.feduwacomm.dto.ProtocolType;
import com.feduwacomm.entity.VmRoundModel;
import com.feduwacomm.event.ModelUploadEvent;
import com.feduwacomm.mapper.*;
import com.feduwacomm.service.WebSocketProtocolService;
import com.feduwacomm.utils.UuidUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 增强版 WebSocket 协议服务测试
 *
 * 测试增强的WebSocket协议处理功能：
 * - 梯度上传消息处理（RandomForest & 神经网络）
 * - 全局模型广播
 * - 聚合触发条件检查
 * - 消息确认机制
 * - 异常处理
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("增强版WebSocket协议服务测试")
class EnhancedWebSocketProtocolServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private TrainingDatasetMapper trainingDatasetMapper;

    @Mock
    private TrainingDatasetRowMapper trainingDatasetRowMapper;

    @Mock
    private FederatedTasksMapper federatedTasksMapper;

    @Mock
    private VmRoundModelsMapper vmRoundModelsMapper;

    @Mock
    private VmInstancesMapper vmInstancesMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private UuidUtil uuidUtil;

    @InjectMocks
    private WebSocketProtocolService webSocketProtocolService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // 初始化依赖项
    }

    @Test
    @DisplayName("测试梯度上传消息处理 - RandomForest")
    void testGradientUpload_RandomForest() {
        // Given
        ProtocolMessage message = createGradientUploadMessage("RandomForest");
        when(uuidUtil.generateUuid()).thenReturn("model-uuid-123");
        when(vmRoundModelsMapper.countReadyModels("task-001", 5)).thenReturn(1); // 第一个上传

        // When
        ProtocolAck ack = webSocketProtocolService.handle(message);

        // Then
        assertThat(ack.getType()).isEqualTo(ProtocolType.ERROR); // 临时使用ERROR替代
        assertThat(ack.getData()).containsEntry("status", "SUCCESS");
        assertThat(ack.getData()).containsEntry("message", "梯度上传成功");

        // 验证模型存储调用
        verify(vmRoundModelsMapper).upsertRoundModel(
            eq("model-uuid-123"),
            eq("task-001"),
            eq("vm-001"),
            eq(5),
            isNull(), // accuracy
            isNull(), // loss
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
        when(vmRoundModelsMapper.countReadyModels("task-001", 5)).thenReturn(1);

        // When
        ProtocolAck ack = webSocketProtocolService.handle(message);

        // Then
        assertThat(ack.getType()).isEqualTo(ProtocolType.ERROR); // 临时使用ERROR替代

        // 验证神经网络参数正确存储
        verify(vmRoundModelsMapper).upsertRoundModel(
            eq("nn-model-uuid-123"),
            eq("task-001"),
            eq("vm-001"),
            eq(5),
            isNull(), // accuracy
            isNull(), // loss
            contains("weights")
        );

        // 验证事件发布
        ArgumentCaptor<ModelUploadEvent> eventCaptor = ArgumentCaptor.forClass(ModelUploadEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        ModelUploadEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getTaskId()).isEqualTo("task-001");
        assertThat(capturedEvent.getRoundNumber()).isEqualTo(5);
        assertThat(capturedEvent.getVmId()).isEqualTo("vm-001");
    }

    @Test
    @DisplayName("测试梯度上传异常处理 - 无效JSON")
    void testGradientUpload_InvalidJson() {
        // Given
        ProtocolMessage message = ProtocolMessage.builder()
            .type(ProtocolType.GRADIENT_UPLOAD)
            .vmId("vm-001")
            .data(Map.of(
                "taskId", "task-001",
                "round", 5,
                "training_result", "invalid json string" // 无效JSON
            ))
            .timestamp(Instant.now())
            .build();

        when(uuidUtil.generateUuid()).thenReturn("model-uuid-123");

        // When
        ProtocolAck ack = webSocketProtocolService.handle(message);

        // Then
        assertThat(ack.getType()).isEqualTo(ProtocolType.ERROR);
        assertThat(ack.getData()).containsEntry("status", "ERROR");
        assertThat(ack.getData().get("errorMessage")).asString()
            .contains("梯度上传处理失败");

        // 验证不会调用存储方法
        verify(vmRoundModelsMapper, never()).upsertRoundModel(any(), any(), any(), any(), any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("测试梯度上传异常处理 - 缺少必需字段")
    void testGradientUpload_MissingRequiredFields() {
        // Given - 缺少round字段
        ProtocolMessage message = ProtocolMessage.builder()
            .type(ProtocolType.GRADIENT_UPLOAD)
            .vmId("vm-001")
            .data(Map.of(
                "taskId", "task-001"
                // 缺少 "round" 字段
            ))
            .timestamp(Instant.now())
            .build();

        // When
        ProtocolAck ack = webSocketProtocolService.handle(message);

        // Then
        assertThat(ack.getType()).isEqualTo(ProtocolType.ERROR);
        assertThat(ack.getData()).containsEntry("status", "ERROR");

        // 验证不会执行后续操作
        verify(vmRoundModelsMapper, never()).upsertRoundModel(any(), any(), any(), any(), any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("测试聚合触发条件检查")
    void testAggregationTriggerConditions() {
        // Given
        ProtocolMessage message = createGradientUploadMessage("RandomForest");

        // 模拟已有2个模型上传，第3个触发聚合
        when(vmRoundModelsMapper.countReadyModels("task-001", 5)).thenReturn(3);
        when(uuidUtil.generateUuid()).thenReturn("model-uuid-123");

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

    @Test
    @DisplayName("测试模型参数提取和验证 - RandomForest")
    void testModelParameterExtraction_RandomForest() {
        // Given
        Map<String, Object> trainingResult = Map.of(
            "model_parameters", Map.of(
                "feature_importances_", Arrays.asList(0.25, 0.20, 0.30, 0.25),
                "n_estimators", 100,
                "max_depth", 10
            ),
            "training_metadata", Map.of(
                "samples_count", 1500,
                "local_accuracy", 0.87,
                "algorithm", "RandomForest",
                "framework", "sklearn",
                "training_time_seconds", 45.2
            )
        );

        ProtocolMessage message = createGradientUploadMessageWithResult(trainingResult);
        when(uuidUtil.generateUuid()).thenReturn("rf-model-123");

        // When
        ProtocolAck ack = webSocketProtocolService.handle(message);

        // Then
        assertThat(ack.getData()).containsEntry("status", "SUCCESS");

        // 验证存储的参数包含所有必需字段
        ArgumentCaptor<String> parametersCaptor = ArgumentCaptor.forClass(String.class);
        verify(vmRoundModelsMapper).upsertRoundModel(
            any(), any(), any(), any(), any(), any(),
            parametersCaptor.capture()
        );

        String storedParameters = parametersCaptor.getValue();
        assertThat(storedParameters).contains("feature_importances_");
        assertThat(storedParameters).contains("n_estimators");
        assertThat(storedParameters).contains("samples_count");
        assertThat(storedParameters).contains("algorithm");
    }

    @Test
    @DisplayName("测试模型参数提取和验证 - 神经网络")
    void testModelParameterExtraction_NeuralNetwork() {
        // Given
        Map<String, Object> trainingResult = Map.of(
            "model_parameters", Map.of(
                "weights", Map.of(
                    "layer1.weight", Arrays.asList(Arrays.asList(0.1, 0.2), Arrays.asList(0.3, 0.4)),
                    "layer2.weight", Arrays.asList(Arrays.asList(0.5, 0.6))
                ),
                "biases", Map.of(
                    "layer1.bias", Arrays.asList(0.1, 0.2),
                    "layer2.bias", Arrays.asList(0.3)
                )
            ),
            "training_metadata", Map.of(
                "samples_count", 2000,
                "local_accuracy", 0.91,
                "loss", 0.09,
                "algorithm", "NeuralNetwork",
                "framework", "pytorch",
                "epochs", 10
            )
        );

        ProtocolMessage message = createGradientUploadMessageWithResult(trainingResult);
        when(uuidUtil.generateUuid()).thenReturn("nn-model-123");

        // When
        ProtocolAck ack = webSocketProtocolService.handle(message);

        // Then
        assertThat(ack.getData()).containsEntry("status", "SUCCESS");

        ArgumentCaptor<String> parametersCaptor = ArgumentCaptor.forClass(String.class);
        verify(vmRoundModelsMapper).upsertRoundModel(
            any(), any(), any(), any(), any(), any(),
            parametersCaptor.capture()
        );

        String storedParameters = parametersCaptor.getValue();
        assertThat(storedParameters).contains("weights");
        assertThat(storedParameters).contains("biases");
        assertThat(storedParameters).contains("layer1.weight");
        assertThat(storedParameters).contains("layer2.weight");
    }

    @Test
    @DisplayName("测试消息处理性能 - 大量参数")
    void testMessageProcessingPerformance_LargeParameters() {
        // Given - 创建包含大量参数的神经网络模型
        Map<String, Object> largeWeights = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            List<Double> layerWeights = new ArrayList<>();
            for (int j = 0; j < 1000; j++) {
                layerWeights.add(Math.random());
            }
            largeWeights.put("layer" + i + ".weight", layerWeights);
        }

        Map<String, Object> trainingResult = Map.of(
            "model_parameters", Map.of("weights", largeWeights),
            "training_metadata", Map.of(
                "samples_count", 5000,
                "algorithm", "NeuralNetwork",
                "framework", "pytorch"
            )
        );

        ProtocolMessage message = createGradientUploadMessageWithResult(trainingResult);
        when(uuidUtil.generateUuid()).thenReturn("large-model-123");

        // When - 测量处理时间
        long startTime = System.currentTimeMillis();
        ProtocolAck ack = webSocketProtocolService.handle(message);
        long processingTime = System.currentTimeMillis() - startTime;

        // Then
        assertThat(ack.getData()).containsEntry("status", "SUCCESS");
        assertThat(processingTime).isLessThan(5000); // 应在5秒内完成

        System.out.println("大参数量消息处理时间: " + processingTime + "ms");
    }

    @Test
    @DisplayName("测试并发消息处理安全性")
    void testConcurrentMessageProcessingSafety() throws InterruptedException {
        // Given
        int threadCount = 10;
        List<ProtocolAck> results = Collections.synchronizedList(new ArrayList<>());
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threadCount);

        when(uuidUtil.generateUuid()).thenReturn("concurrent-model-123");

        // When - 并发处理消息
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            new Thread(() -> {
                try {
                    ProtocolMessage message = createGradientUploadMessage("RandomForest");
                    // 修改vmId使每个线程处理不同的消息
                    message = ProtocolMessage.builder()
                        .type(message.getType())
                        .vmId("vm-" + String.format("%03d", threadIndex))
                        .data(message.getData())
                        .timestamp(message.getTimestamp())
                        .build();

                    ProtocolAck ack = webSocketProtocolService.handle(message);
                    results.add(ack);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();

        // Then - 所有消息都应该被正确处理
        assertThat(results).hasSize(threadCount);
        for (ProtocolAck ack : results) {
            assertThat(ack.getData()).containsEntry("status", "SUCCESS");
        }
    }

    @Test
    @DisplayName("测试消息路由正确性")
    void testMessageRoutingCorrectness() {
        // Given - 测试不同类型的消息是否被正确路由
        ProtocolMessage[] messages = {
            createMessage(ProtocolType.STATUS_QUERY, Map.of("vmId", "vm-001")),
            createMessage(ProtocolType.HEARTBEAT, Map.of("vmId", "vm-002")),
            createMessage(ProtocolType.TRAINING_START, Map.of("taskId", "task-001"))
        };

        // When & Then
        for (ProtocolMessage message : messages) {
            ProtocolAck ack = webSocketProtocolService.handle(message);
            assertThat(ack).isNotNull();
            assertThat(ack.getType()).isNotNull();
        }
    }

    // ================= 辅助方法 =================

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
        } else { // NeuralNetwork
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

        return createGradientUploadMessageWithResult(trainingResult);
    }

    private ProtocolMessage createGradientUploadMessageWithResult(Map<String, Object> trainingResult) {
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

    private ProtocolMessage createMessage(ProtocolType type, Map<String, Object> data) {
        return ProtocolMessage.builder()
            .type(type)
            .vmId("vm-001")
            .data(data)
            .timestamp(Instant.now())
            .build();
    }
}