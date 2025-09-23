package com.feduwacomm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.dto.ProtocolAck;
import com.feduwacomm.dto.ProtocolMessage;
import com.feduwacomm.dto.ProtocolType;
import com.feduwacomm.mapper.FederatedTasksMapper;
import com.feduwacomm.mapper.TrainingDatasetMapper;
import com.feduwacomm.mapper.TrainingDatasetRowMapper;
import com.feduwacomm.mapper.UserMapper;
import com.feduwacomm.mapper.VmInstancesMapper;
import com.feduwacomm.mapper.VmRoundModelsMapper;
import com.feduwacomm.entity.FederatedTask;
import com.feduwacomm.enums.FederatedAlgorithm;
import com.feduwacomm.enums.FederatedTaskStatus;
import com.feduwacomm.utils.UuidUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WebSocketProtocolService单元测试类
 * 测试WebSocket协议处理服务的各种消息处理功能
 */
@ExtendWith(MockitoExtension.class)
public class WebSocketProtocolServiceTest {

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
    private ObjectMapper objectMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private UuidUtil uuidUtil;

    private WebSocketProtocolService protocolService;

    private ProtocolMessage sampleMessage;

    @BeforeEach
    void setUp() {
        // 重置mock对象
        reset(messagingTemplate, trainingDatasetMapper, trainingDatasetRowMapper,
            federatedTasksMapper, vmRoundModelsMapper, vmInstancesMapper, userMapper, objectMapper, eventPublisher, uuidUtil);

        // 创建服务实例
        protocolService = new WebSocketProtocolService(
            messagingTemplate,
            trainingDatasetMapper,
            trainingDatasetRowMapper,
            federatedTasksMapper,
            vmRoundModelsMapper,
            vmInstancesMapper,
            userMapper,
            objectMapper,
            eventPublisher,
            uuidUtil
        );

        // 准备测试数据
        setupTestData();
    }

    private void setupTestData() {
        // 设置UuidUtil Mock行为
        when(uuidUtil.generateUuid()).thenReturn("0199758a5ff272ba8fa24b79c22cbd70"); // 32位紧凑UUIDv7

        // v1.3: 更新CONNECT消息数据格式
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("version", "1.0.0");
        messageData.put("supportedMLAlgorithms", List.of("RandomForest", "SVM", "NeuralNetwork", "XGBoost"));
        messageData.put("systemInfo", Map.of(
                "os", "Ubuntu 20.04",
                "python", "3.8.10",
                "memory", "4GB",
                "cpu", "Intel Xeon E5-2680",
                "gpu", "NVIDIA Tesla V100"
        ));
        messageData.put("computeCapabilities", Map.of(
                "maxBatchSize", 1024,
                "gpuMemory", "16GB",
                "parallelProcessing", true,
                "frameworks", List.of("sklearn", "pytorch", "tensorflow")
        ));

        sampleMessage = ProtocolMessage.builder()
                .type(ProtocolType.CONNECT)
                .id("msg-001")
                .timestamp(Instant.now())
                .vmId("vm-001")
                .data(messageData)
                .signature("test-signature")
                .build();
    }

    /**
     * 测试处理空消息
     */
    @Test
    void testHandle_NullMessage() {
        // 执行测试
        ProtocolAck ack = protocolService.handle(null);

        // 验证结果
        assertNotNull(ack);
        assertEquals(ProtocolType.MESSAGE_ERROR, ack.getType());
        assertNotNull(ack.getData());
        assertEquals("INVALID_MESSAGE", ack.getData().get("errorCode"));
        assertEquals("缺少消息或类型", ack.getData().get("errorMessage"));
    }

    /**
     * 测试处理缺少类型的消息
     */
    @Test
    void testHandle_NullType() {
        ProtocolMessage messageWithoutType = ProtocolMessage.builder()
                .id("msg-002")
                .timestamp(Instant.now())
                .vmId("vm-001")
                .data(new HashMap<>())
                .build();

        // 执行测试
        ProtocolAck ack = protocolService.handle(messageWithoutType);

        // 验证结果
        assertNotNull(ack);
        assertEquals(ProtocolType.MESSAGE_ERROR, ack.getType());
        assertEquals("INVALID_MESSAGE", ack.getData().get("errorCode"));
    }

    /**
     * 测试连接消息处理
     */
    @Test
    void testHandle_ConnectMessage() {
        // 执行测试
        ProtocolAck ack = protocolService.handle(sampleMessage);

        // 验证结果
        assertNotNull(ack);
        assertEquals(ProtocolType.CONNECT_ACK, ack.getType());
        assertEquals("vm-001", ack.getVmId());
        assertNotNull(ack.getData());
        assertTrue(ack.getData().containsKey("sessionId"));
        assertTrue(ack.getData().containsKey("serverTime"));
        assertEquals(30, ack.getData().get("heartbeatInterval"));

        // 验证mock调用
        verify(vmInstancesMapper).updateConnection(eq("vm-001"), eq("CONNECTED"), anyString());
        verify(messagingTemplate).convertAndSend(eq("/topic/vm/vm-001"), any(Object.class));
    }

    /**
     * 测试心跳消息处理
     */
    @Test
    void testHandle_HeartbeatMessage() {
        ProtocolMessage heartbeatMessage = ProtocolMessage.builder()
                .type(ProtocolType.HEARTBEAT)
                .id("hb-001")
                .timestamp(Instant.now())
                .vmId("vm-001")
                .data(new HashMap<>())
                .build();

        // 执行测试
        ProtocolAck ack = protocolService.handle(heartbeatMessage);

        // 验证结果
        assertNotNull(ack);
        assertEquals(ProtocolType.HEARTBEAT_ACK, ack.getType());
        assertEquals("vm-001", ack.getVmId());
        assertNotNull(ack.getData());
        assertTrue(ack.getData().containsKey("serverTime"));
        assertEquals(30, ack.getData().get("nextHeartbeat"));
        assertEquals("NORMAL", ack.getData().get("systemStatus"));

        // 验证mock调用
        verify(vmInstancesMapper).updateConnection(eq("vm-001"), eq("CONNECTED"), anyString());
    }

    /**
     * 测试数据集创建消息处理 - 成功场景
     */
    @Test
    void testHandle_DatasetCreateMessage_Success() {
        Map<String, Object> data = new HashMap<>();
        data.put("datasetId", "dataset-001");
        data.put("datasetDescription", "Test dataset");
        data.put("datasetType", "ACOUSTIC");

        ProtocolMessage datasetMessage = ProtocolMessage.builder()
                .type(ProtocolType.DATASET_CREATE)
                .id("ds-001")
                .timestamp(Instant.now())
                .vmId("vm-001")
                .data(data)
                .build();

        // 执行测试
        ProtocolAck ack = protocolService.handle(datasetMessage);

        // 验证结果
        assertNotNull(ack);
        assertEquals(ProtocolType.DATASET_CREATE_ACK, ack.getType());
        assertEquals("vm-001", ack.getVmId());
        assertEquals("dataset-001", ack.getData().get("datasetId"));
        assertEquals("READY", ack.getData().get("status"));

        // 验证mock调用
        verify(trainingDatasetMapper).upsertDataset(
                eq("dataset-001"),
                eq("Test dataset"),
                eq("Test dataset"),
                eq("ACOUSTIC"),
                eq("READY"),
                isNull()  // metadataJson
        );
    }

    /**
     * 测试数据集创建消息处理 - 缺少数据集ID
     */
    @Test
    void testHandle_DatasetCreateMessage_MissingId() {
        Map<String, Object> data = new HashMap<>();
        data.put("datasetDescription", "Test dataset");

        ProtocolMessage datasetMessage = ProtocolMessage.builder()
                .type(ProtocolType.DATASET_CREATE)
                .id("ds-002")
                .timestamp(Instant.now())
                .vmId("vm-001")
                .data(data)
                .build();

        // 执行测试
        ProtocolAck ack = protocolService.handle(datasetMessage);

        // 验证结果
        assertNotNull(ack);
        assertEquals(ProtocolType.MESSAGE_ERROR, ack.getType());
        assertEquals("INVALID_DATASET_ID", ack.getData().get("errorCode"));
        assertEquals("缺少 datasetId", ack.getData().get("errorMessage"));

        // 验证没有调用数据库操作
        verify(trainingDatasetMapper, never()).upsertDataset(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    /**
     * 测试数据集行追加消息处理
     */
    @Test
    void testHandle_DatasetAppendRowsMessage() {
        Map<String, Object> row1 = Map.of("feature1", 1.0, "feature2", 2.0);
        Map<String, Object> row2 = Map.of("feature1", 1.5, "feature2", 2.5);
        
        Map<String, Object> data = new HashMap<>();
        data.put("datasetId", "dataset-001");
        data.put("rows", Arrays.asList(row1, row2));

        ProtocolMessage appendMessage = ProtocolMessage.builder()
                .type(ProtocolType.DATASET_APPEND_ROWS)
                .id("append-001")
                .timestamp(Instant.now())
                .vmId("vm-001")
                .data(data)
                .build();

        // Mock ObjectMapper
        try {
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"feature1\":1.0,\"feature2\":2.0}");
        } catch (Exception e) {
            // Mock设置不会抛出异常
        }

        // 执行测试
        ProtocolAck ack = protocolService.handle(appendMessage);

        // 验证结果
        assertNotNull(ack);
        assertEquals(ProtocolType.DATASET_APPEND_ROWS_ACK, ack.getType());
        assertEquals("dataset-001", ack.getData().get("datasetId"));
        assertEquals(2, ack.getData().get("accepted"));
        assertEquals(0, ack.getData().get("rejected"));

        // 验证mock调用
        verify(trainingDatasetRowMapper).insertRows(eq("dataset-001"), any(List.class));
    }

    /**
     * 测试数据集完成消息处理
     */
    @Test
    void testHandle_DatasetCompleteMessage() {
        Map<String, Object> data = new HashMap<>();
        data.put("datasetId", "dataset-001");

        ProtocolMessage completeMessage = ProtocolMessage.builder()
                .type(ProtocolType.DATASET_COMPLETE)
                .id("complete-001")
                .timestamp(Instant.now())
                .vmId("vm-001")
                .data(data)
                .build();

        // Mock数据库返回
        when(trainingDatasetRowMapper.countByDataset("dataset-001")).thenReturn(100);

        // 执行测试
        ProtocolAck ack = protocolService.handle(completeMessage);

        // 验证结果
        assertNotNull(ack);
        assertEquals(ProtocolType.DATASET_COMPLETE_ACK, ack.getType());
        assertEquals("dataset-001", ack.getData().get("datasetId"));
        assertEquals(100, ack.getData().get("rowCount"));
        assertEquals("READY", ack.getData().get("status"));

        // 验证mock调用
        verify(trainingDatasetMapper).updateStatus("dataset-001", "READY");
        verify(trainingDatasetRowMapper).countByDataset("dataset-001");
    }

    /**
     * 测试训练开始消息处理
     */
    @Test
    void testHandle_TrainingStartMessage() {
        // v1.3: 使用新的消息格式
        Map<String, Object> hyperparameters = Map.of("n_estimators", 100, "max_depth", 10, "random_state", 42);
        Map<String, Object> trainingConfig = Map.of("epochs", 5, "batchSize", 32, "timeout", 300);

        Map<String, Object> data = new HashMap<>();
        data.put("taskId", "task-001");
        data.put("mlAlgorithm", "RandomForest");  // v1.3: 使用mlAlgorithm
        data.put("hyperparameters", hyperparameters);
        data.put("trainingConfig", trainingConfig);

        ProtocolMessage trainingMessage = ProtocolMessage.builder()
                .type(ProtocolType.TRAINING_START)
                .id("training-001")
                .timestamp(Instant.now())
                .vmId("vm-001")
                .data(data)
                .build();

        // Mock ObjectMapper for combined config (v1.3: 包含mlAlgorithm)
        try {
            when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"hyperparameters\":{\"n_estimators\":100,\"max_depth\":10,\"random_state\":42},\"trainingConfig\":{\"epochs\":5,\"batchSize\":32,\"timeout\":300},\"mlAlgorithm\":\"RandomForest\"}");
        } catch (Exception e) {
            // Mock设置不会抛出异常
        }

        // 执行测试
        ProtocolAck ack = protocolService.handle(trainingMessage);

        // 验证结果
        assertNotNull(ack);
        assertEquals(ProtocolType.TRAINING_START_ACK, ack.getType());
        assertEquals("COMMAND_SENT", ack.getData().get("status"));

        // 验证mock调用
        // v1.3: 验证使用联邦学习算法（后端管理）和epochs
        // 捕获传递给insertTask的FederatedTask实体
        ArgumentCaptor<FederatedTask> taskCaptor = ArgumentCaptor.forClass(FederatedTask.class);
        verify(federatedTasksMapper).insertTask(taskCaptor.capture());

        // 验证FederatedTask实体的属性
        FederatedTask capturedTask = taskCaptor.getValue();
        assertNotNull(capturedTask);
        assertEquals("task-001", capturedTask.getId());
        assertEquals("task-001", capturedTask.getTaskName());
        assertEquals(FederatedAlgorithm.FEDERATED_AVERAGING, capturedTask.getAlgorithm());
        assertEquals(FederatedTaskStatus.PENDING, capturedTask.getStatus());
        assertEquals(5, capturedTask.getEpochs());
        assertEquals(0, capturedTask.getCurrentRound());

        // 验证config JSON包含mlAlgorithm信息
        String config = capturedTask.getConfig();
        assertNotNull(config);
        assertTrue(config.contains("mlAlgorithm"));
        assertTrue(config.contains("RandomForest"));

        verify(messagingTemplate).convertAndSend(eq("/topic/vm/vm-001"), any(Object.class));
    }

    /**
     * 测试模型上传消息处理
     */
    @Test
    void testHandle_ModelUploadMessage() {
        Map<String, Object> parameters = Map.of("weights", Arrays.asList(1.0, 2.0, 3.0));
        Map<String, Object> metrics = Map.of("accuracy", 0.85, "loss", 0.25);
        
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", "task-001");
        data.put("round", 1);
        data.put("parameters", parameters);
        data.put("metrics", metrics);

        ProtocolMessage modelMessage = ProtocolMessage.builder()
                .type(ProtocolType.MODEL_UPLOAD)
                .id("model-001")
                .timestamp(Instant.now())
                .vmId("vm-001")
                .data(data)
                .build();

        // Mock ObjectMapper
        try {
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"parameters\":{},\"metrics\":{}}");
        } catch (Exception e) {
            // Mock设置不会抛出异常
        }

        // 执行测试
        ProtocolAck ack = protocolService.handle(modelMessage);

        // 验证结果
        assertNotNull(ack);
        assertEquals(ProtocolType.MODEL_UPLOAD, ack.getType());
        assertEquals("RECEIVED", ack.getData().get("status"));

        // 验证mock调用
        verify(vmRoundModelsMapper).upsertRoundModel(
                anyString(),      // random UUID
                eq("task-001"), 
                eq("vm-001"), 
                eq(1), 
                eq(0.85), 
                eq(0.25), 
                anyString()
        );
        verify(messagingTemplate).convertAndSend(eq("/topic/vm/vm-001"), any(Object.class));
    }

    /**
     * 测试不支持的消息类型
     */
    @Test
    void testHandle_UnsupportedMessageType() {
        // 由于我们无法直接创建新的ProtocolType，我们测试错误消息类型
        ProtocolMessage errorMessage = ProtocolMessage.builder()
                .type(ProtocolType.ERROR)
                .id("error-001")
                .timestamp(Instant.now())
                .vmId("vm-001")
                .data(Map.of("errorCode", "TEST_ERROR"))
                .build();

        // 执行测试
        ProtocolAck ack = protocolService.handle(errorMessage);

        // 验证结果
        assertNotNull(ack);
        assertEquals(ProtocolType.ERROR, ack.getType());
        assertEquals("RECEIVED", ack.getData().get("status"));

        // 验证mock调用
        verify(messagingTemplate).convertAndSend(eq("/topic/vm/vm-001"), any(Object.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/errors"), any(Object.class));
    }

    /**
     * 测试VM启动消息处理
     */
    @Test
    void testHandle_VmStartMessage() {
        ProtocolMessage vmStartMessage = ProtocolMessage.builder()
                .type(ProtocolType.VM_START)
                .id("vmstart-001")
                .timestamp(Instant.now())
                .vmId("vm-001")
                .data(Map.of("status", "starting"))
                .build();

        // 执行测试
        ProtocolAck ack = protocolService.handle(vmStartMessage);

        // 验证结果
        assertNotNull(ack);
        assertEquals(ProtocolType.VM_START, ack.getType());
        assertEquals("RECEIVED", ack.getData().get("status"));

        // 验证mock调用
        verify(vmInstancesMapper).updateConnection(eq("vm-001"), eq("CONNECTED"), anyString());
        verify(messagingTemplate).convertAndSend(eq("/topic/vm/vm-001"), any(Object.class));
    }

    /**
     * 测试VM停止消息处理
     */
    @Test
    void testHandle_VmStopMessage() {
        ProtocolMessage vmStopMessage = ProtocolMessage.builder()
                .type(ProtocolType.VM_STOP)
                .id("vmstop-001")
                .timestamp(Instant.now())
                .vmId("vm-001")
                .data(Map.of("status", "stopping"))
                .build();

        // 执行测试
        ProtocolAck ack = protocolService.handle(vmStopMessage);

        // 验证结果
        assertNotNull(ack);
        assertEquals(ProtocolType.VM_STOP, ack.getType());
        assertEquals("RECEIVED", ack.getData().get("status"));

        // 验证mock调用
        verify(vmInstancesMapper).updateConnection(eq("vm-001"), eq("DISCONNECTED"), anyString());
        verify(messagingTemplate).convertAndSend(eq("/topic/vm/vm-001"), any(Object.class));
    }
}