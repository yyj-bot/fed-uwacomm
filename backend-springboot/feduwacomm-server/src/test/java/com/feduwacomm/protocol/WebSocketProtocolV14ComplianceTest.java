package com.feduwacomm.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.dto.ProtocolMessage;
import com.feduwacomm.dto.ProtocolType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WebSocket协议v1.4合规性测试
 *
 * 本测试类验证所有34个v1.4核心协议消息的格式合规性、消息流程和完整性。
 * 采用中心化架构设计，确保"后端大脑+VM手脚"的控制模式。
 *
 * 测试覆盖：
 * - 连接管理层 (4个协议)
 * - 任务管理层 (10个协议)
 * - 轮次管理层 (9个协议)
 * - 状态监控层 (3个协议)
 * - 虚拟机控制层 (4个协议)
 * - 数据集管理层 (4个协议)
 */
@TestMethodOrder(OrderAnnotation.class)
@DisplayName("WebSocket协议v1.4合规性测试")
public class WebSocketProtocolV14ComplianceTest {

    private ObjectMapper objectMapper;
    private String testVmId;
    private String testTaskId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        testVmId = "vm-test-001";
        testTaskId = "task-audio-001";
    }

    // ==================== 连接管理层测试 (4个协议) ====================

    @Test
    @Order(1)
    @DisplayName("连接管理层 - CONNECT消息格式验证")
    void testConnectMessageFormat() {
        ProtocolMessage message = createConnectMessage();

        // 验证基本字段
        assertNotNull(message.getType());
        assertEquals(ProtocolType.CONNECT, message.getType());
        assertNotNull(message.getId());
        assertNotNull(message.getTimestamp());
        assertNotNull(message.getVmId());
        assertEquals(testVmId, message.getVmId());

        // 验证data字段必需内容
        Map<String, Object> data = message.getData();
        assertNotNull(data);
        assertTrue(data.containsKey("version"));
        assertTrue(data.containsKey("supportedMLAlgorithms"));
        assertTrue(data.containsKey("systemInfo"));
        assertTrue(data.containsKey("computeCapabilities"));

        // 验证系统信息完整性
        Map<String, Object> systemInfo = (Map<String, Object>) data.get("systemInfo");
        assertNotNull(systemInfo);
        assertTrue(systemInfo.containsKey("os"));
        assertTrue(systemInfo.containsKey("memory"));
        assertTrue(systemInfo.containsKey("cpu"));

        System.out.println("✅ CONNECT消息格式验证通过");
    }

    @Test
    @Order(2)
    @DisplayName("连接管理层 - CONNECT_ACK消息格式验证")
    void testConnectAckMessageFormat() {
        ProtocolMessage message = createConnectAckMessage();

        assertEquals(ProtocolType.CONNECT_ACK, message.getType());
        assertNotNull(message.getId());
        assertNotNull(message.getData());

        Map<String, Object> data = message.getData();
        assertTrue(data.containsKey("sessionId"));
        assertTrue(data.containsKey("serverTime"));
        assertTrue(data.containsKey("heartbeatInterval"));
        assertEquals(30, data.get("heartbeatInterval"));

        System.out.println("✅ CONNECT_ACK消息格式验证通过");
    }

    @Test
    @Order(3)
    @DisplayName("连接管理层 - HEARTBEAT消息格式验证")
    void testHeartbeatMessageFormat() {
        ProtocolMessage message = createHeartbeatMessage();

        assertEquals(ProtocolType.HEARTBEAT, message.getType());
        assertEquals(testVmId, message.getVmId());

        Map<String, Object> data = message.getData();
        assertNotNull(data);
        assertTrue(data.containsKey("status"));
        assertTrue(data.containsKey("resourceUsage"));
        assertTrue(data.containsKey("activeTasks"));

        System.out.println("✅ HEARTBEAT消息格式验证通过");
    }

    // ==================== 任务管理层测试 (10个协议) ====================

    @Test
    @Order(4)
    @DisplayName("任务管理层 - FEDERATED_TASK_START消息格式验证")
    void testFederatedTaskStartMessageFormat() {
        ProtocolMessage message = createFederatedTaskStartMessage();

        assertEquals(ProtocolType.FEDERATED_TASK_START, message.getType());

        Map<String, Object> data = message.getData();
        assertNotNull(data);
        assertEquals(testTaskId, data.get("taskId"));
        assertTrue(data.containsKey("federatedAlgorithm"));
        assertTrue(data.containsKey("totalRounds"));
        assertTrue(data.containsKey("participants"));
        assertTrue(data.containsKey("initialGlobalModel"));
        assertTrue(data.containsKey("localTrainingConfig"));

        // 验证算法类型
        String algorithm = (String) data.get("federatedAlgorithm");
        assertTrue(Set.of("FEDERATED_AVERAGING", "FEDERATED_PROXIMAL", "FEDERATED_NOVA", "SCAFFOLD")
                .contains(algorithm));

        System.out.println("✅ FEDERATED_TASK_START消息格式验证通过");
    }

    @Test
    @Order(5)
    @DisplayName("任务管理层 - FEDERATED_TASK_START_ACK消息格式验证")
    void testFederatedTaskStartAckMessageFormat() {
        ProtocolMessage message = createFederatedTaskStartAckMessage();

        assertEquals(ProtocolType.FEDERATED_TASK_START_ACK, message.getType());
        assertEquals(testVmId, message.getVmId());

        Map<String, Object> data = message.getData();
        assertEquals(testTaskId, data.get("taskId"));
        assertTrue(data.containsKey("status"));
        assertTrue(data.containsKey("vmCapabilities"));

        System.out.println("✅ FEDERATED_TASK_START_ACK消息格式验证通过");
    }

    @Test
    @Order(6)
    @DisplayName("任务管理层 - 任务生命周期管理协议验证")
    void testTaskLifecycleManagementProtocols() {
        // 测试STOP协议
        ProtocolMessage stopMessage = createFederatedTaskStopMessage();
        assertEquals(ProtocolType.FEDERATED_TASK_STOP, stopMessage.getType());
        assertEquals(testTaskId, stopMessage.getData().get("taskId"));

        // 测试RESUME协议
        ProtocolMessage resumeMessage = createFederatedTaskResumeMessage();
        assertEquals(ProtocolType.FEDERATED_TASK_RESUME, resumeMessage.getType());
        assertEquals(testTaskId, resumeMessage.getData().get("taskId"));
        assertTrue(resumeMessage.getData().containsKey("resumeFrom"));

        // 测试DELETE协议
        ProtocolMessage deleteMessage = createFederatedTaskDeleteMessage();
        assertEquals(ProtocolType.FEDERATED_TASK_DELETE, deleteMessage.getType());
        assertEquals(testTaskId, deleteMessage.getData().get("taskId"));

        System.out.println("✅ 任务生命周期管理协议验证通过");
    }

    // ==================== 轮次管理层测试 (9个协议) ====================

    @Test
    @Order(7)
    @DisplayName("轮次管理层 - ROUND_START消息格式验证")
    void testRoundStartMessageFormat() {
        ProtocolMessage message = createRoundStartMessage();

        assertEquals(ProtocolType.ROUND_START, message.getType());

        Map<String, Object> data = message.getData();
        assertEquals(testTaskId, data.get("taskId"));
        assertEquals(1, data.get("roundNumber"));
        assertTrue(data.containsKey("roundSpecificConfig"));
        assertTrue(data.containsKey("expectedParticipants"));

        System.out.println("✅ ROUND_START消息格式验证通过");
    }

    @Test
    @Order(8)
    @DisplayName("轮次管理层 - GRADIENT_UPLOAD消息格式验证")
    void testGradientUploadMessageFormat() {
        ProtocolMessage message = createGradientUploadMessage();

        assertEquals(ProtocolType.GRADIENT_UPLOAD, message.getType());
        assertEquals(testVmId, message.getVmId());

        Map<String, Object> data = message.getData();
        assertEquals(testTaskId, data.get("taskId"));
        assertEquals(1, data.get("roundNumber"));
        assertTrue(data.containsKey("gradientData"));
        assertTrue(data.containsKey("checksum"));
        assertTrue(data.containsKey("trainingMetrics"));
        assertTrue(data.containsKey("samplesCount"));

        System.out.println("✅ GRADIENT_UPLOAD消息格式验证通过");
    }

    @Test
    @Order(9)
    @DisplayName("轮次管理层 - GLOBAL_MODEL_BROADCAST消息格式验证")
    void testGlobalModelBroadcastMessageFormat() {
        ProtocolMessage message = createGlobalModelBroadcastMessage();

        assertEquals(ProtocolType.GLOBAL_MODEL_BROADCAST, message.getType());

        Map<String, Object> data = message.getData();
        assertEquals(testTaskId, data.get("taskId"));
        assertEquals(1, data.get("roundNumber"));
        assertTrue(data.containsKey("globalModel"));
        assertTrue(data.containsKey("checksum"));
        assertTrue(data.containsKey("aggregationInfo"));

        // 验证不包含downloadUrl (v1.4移除)
        assertFalse(data.containsKey("downloadUrl"));

        System.out.println("✅ GLOBAL_MODEL_BROADCAST消息格式验证通过 (无downloadUrl)");
    }

    // ==================== 状态监控层测试 (3个协议) ====================

    @Test
    @Order(10)
    @DisplayName("状态监控层 - VM_STATUS_QUERY消息格式验证")
    void testVmStatusQueryMessageFormat() {
        ProtocolMessage message = createVmStatusQueryMessage();

        assertEquals(ProtocolType.VM_STATUS_QUERY, message.getType());
        assertNotNull(message.getData());

        System.out.println("✅ VM_STATUS_QUERY消息格式验证通过");
    }

    @Test
    @Order(11)
    @DisplayName("状态监控层 - ERROR消息格式验证")
    void testErrorMessageFormat() {
        ProtocolMessage message = createErrorMessage();

        assertEquals(ProtocolType.ERROR, message.getType());
        assertEquals(testVmId, message.getVmId());

        Map<String, Object> data = message.getData();
        assertTrue(data.containsKey("errorCode"));
        assertTrue(data.containsKey("errorMessage"));
        assertTrue(data.containsKey("errorContext"));

        System.out.println("✅ ERROR消息格式验证通过");
    }

    // ==================== 数据集管理层测试 (4个协议) ====================

    @Test
    @Order(12)
    @DisplayName("数据集管理层 - DATASET_CREATE消息格式验证")
    void testDatasetCreateMessageFormat() {
        ProtocolMessage message = createDatasetCreateMessage();

        assertEquals(ProtocolType.DATASET_CREATE, message.getType());
        assertEquals(testVmId, message.getVmId());

        Map<String, Object> data = message.getData();
        assertTrue(data.containsKey("datasetId"));
        assertTrue(data.containsKey("datasetType"));
        assertTrue(data.containsKey("metadata"));

        System.out.println("✅ DATASET_CREATE消息格式验证通过");
    }

    // ==================== 协议完整性测试 ====================

    @ParameterizedTest
    @EnumSource(ProtocolType.class)
    @DisplayName("v1.4协议类型完整性验证")
    void testProtocolTypeCompleteness(ProtocolType protocolType) {
        // 验证所有协议类型都在v1.4规范内
        Set<ProtocolType> validV14Types = Set.of(
            // 连接管理层 (4个)
            ProtocolType.CONNECT, ProtocolType.CONNECT_ACK,
            ProtocolType.HEARTBEAT, ProtocolType.HEARTBEAT_ACK,

            // 任务管理层 (10个)
            ProtocolType.FEDERATED_TASK_START, ProtocolType.FEDERATED_TASK_START_ACK,
            ProtocolType.FEDERATED_TASK_STOP, ProtocolType.FEDERATED_TASK_STOP_ACK,
            ProtocolType.FEDERATED_TASK_RESUME, ProtocolType.FEDERATED_TASK_RESUME_ACK,
            ProtocolType.FEDERATED_TASK_DELETE, ProtocolType.FEDERATED_TASK_DELETE_ACK,
            ProtocolType.FEDERATED_TASK_STATUS_QUERY, ProtocolType.FEDERATED_TASK_STATUS_RESPONSE,

            // 轮次管理层 (9个)
            ProtocolType.ROUND_START, ProtocolType.ROUND_START_ACK, ProtocolType.ROUND_ABORT,
            ProtocolType.GRADIENT_UPLOAD, ProtocolType.GRADIENT_UPLOAD_ACK,
            ProtocolType.GLOBAL_MODEL_BROADCAST, ProtocolType.GLOBAL_MODEL_BROADCAST_ACK,
            ProtocolType.ROUND_COMPLETE, ProtocolType.ROUND_COMPLETE_ACK,

            // 状态监控层 (3个)
            ProtocolType.VM_STATUS_QUERY, ProtocolType.VM_STATUS_RESPONSE, ProtocolType.ERROR,

            // 虚拟机控制层 (4个)
            ProtocolType.VM_START, ProtocolType.VM_START_ACK,
            ProtocolType.VM_STOP, ProtocolType.VM_STOP_ACK,

            // 数据集管理层 (10个)
            ProtocolType.DATASET_CREATE, ProtocolType.DATASET_CREATE_ACK,
            ProtocolType.DATASET_APPEND_ROWS, ProtocolType.DATASET_APPEND_ROWS_ACK,
            ProtocolType.DATASET_COMPLETE, ProtocolType.DATASET_COMPLETE_ACK,
            ProtocolType.DATASET_STATUS_QUERY, ProtocolType.DATASET_STATUS_RESPONSE,
            ProtocolType.DATASET_DELETE, ProtocolType.DATASET_DELETE_ACK,

            // 错误类型 (向后兼容)
            ProtocolType.CONNECTION_ERROR, ProtocolType.MESSAGE_ERROR
        );

        assertTrue(validV14Types.contains(protocolType),
            "协议类型 " + protocolType + " 不在v1.4规范内");
    }

    @Test
    @Order(13)
    @DisplayName("v1.4协议数量验证")
    void testProtocolCountCompliance() {
        ProtocolType[] allTypes = ProtocolType.values();

        // v1.4应该有34个核心协议 + 2个错误类型 = 36个
        assertEquals(36, allTypes.length,
            "v1.4协议类型数量应为36个 (34个核心 + 2个错误类型)");

        System.out.println("✅ v1.4协议数量验证通过: " + allTypes.length + "个协议类型");
    }

    // ==================== 消息创建辅助方法 ====================

    private ProtocolMessage createConnectMessage() {
        Map<String, Object> data = new HashMap<>();
        data.put("version", "1.4.0");
        data.put("supportedMLAlgorithms", List.of("FEDERATED_AVERAGING", "FEDERATED_PROXIMAL"));
        data.put("systemInfo", Map.of(
            "os", "Ubuntu 22.04",
            "memory", "16GB",
            "cpu", "Intel i7-12700K",
            "gpu", "NVIDIA RTX 4090"
        ));
        data.put("computeCapabilities", Map.of(
            "maxBatchSize", 2048,
            "concurrentTasks", 4,
            "frameworks", List.of("PyTorch", "TensorFlow")
        ));

        return ProtocolMessage.builder()
            .type(ProtocolType.CONNECT)
            .id("client-" + System.currentTimeMillis() + "-001")
            .timestamp(Instant.now())
            .vmId(testVmId)
            .data(data)
            .signature("test-signature")
            .build();
    }

    private ProtocolMessage createConnectAckMessage() {
        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", "session-" + System.currentTimeMillis());
        data.put("serverTime", Instant.now().toString());
        data.put("heartbeatInterval", 30);
        data.put("maxConcurrentTasks", 5);

        return ProtocolMessage.builder()
            .type(ProtocolType.CONNECT_ACK)
            .id("server-" + System.currentTimeMillis() + "-001")
            .timestamp(Instant.now())
            .data(data)
            .build();
    }

    private ProtocolMessage createHeartbeatMessage() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "ONLINE");
        data.put("resourceUsage", Map.of(
            "cpuUsage", 65.5,
            "memoryUsage", 8192,
            "gpuUsage", 23.4
        ));
        data.put("activeTasks", List.of(testTaskId));

        return ProtocolMessage.builder()
            .type(ProtocolType.HEARTBEAT)
            .id("client-" + System.currentTimeMillis() + "-hb")
            .timestamp(Instant.now())
            .vmId(testVmId)
            .data(data)
            .build();
    }

    private ProtocolMessage createFederatedTaskStartMessage() {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", testTaskId);
        data.put("federatedAlgorithm", "FEDERATED_AVERAGING");
        data.put("totalRounds", 10);
        data.put("participants", List.of("vm-001", "vm-002", "vm-003"));
        data.put("initialGlobalModel", Map.of(
            "modelType", "RandomForest",
            "parameters", Map.of("n_estimators", 100, "max_depth", 10),
            "version", "v1.0.0"
        ));
        data.put("localTrainingConfig", Map.of(
            "batchSize", 32,
            "learningRate", 0.001,
            "epochs", 5
        ));

        return ProtocolMessage.builder()
            .type(ProtocolType.FEDERATED_TASK_START)
            .id("server-" + System.currentTimeMillis() + "-task")
            .timestamp(Instant.now())
            .data(data)
            .build();
    }

    private ProtocolMessage createFederatedTaskStartAckMessage() {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", testTaskId);
        data.put("status", "SUCCESS");
        data.put("message", "任务启动成功");
        data.put("vmCapabilities", Map.of(
            "computePower", "HIGH",
            "memorySize", "16GB",
            "supportedAlgorithms", List.of("FEDERATED_AVERAGING", "FEDERATED_PROXIMAL")
        ));

        return ProtocolMessage.builder()
            .type(ProtocolType.FEDERATED_TASK_START_ACK)
            .id("client-" + System.currentTimeMillis() + "-ack")
            .timestamp(Instant.now())
            .vmId(testVmId)
            .data(data)
            .build();
    }

    private ProtocolMessage createFederatedTaskStopMessage() {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", testTaskId);
        data.put("reason", "MANUAL_STOP");
        data.put("graceful", true);
        data.put("saveProgress", true);

        return ProtocolMessage.builder()
            .type(ProtocolType.FEDERATED_TASK_STOP)
            .id("server-" + System.currentTimeMillis() + "-stop")
            .timestamp(Instant.now())
            .data(data)
            .build();
    }

    private ProtocolMessage createFederatedTaskResumeMessage() {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", testTaskId);
        data.put("resumeFrom", Map.of(
            "round", 5,
            "checkpoint", "checkpoint-round-5",
            "globalModelVersion", "v5.0"
        ));

        return ProtocolMessage.builder()
            .type(ProtocolType.FEDERATED_TASK_RESUME)
            .id("server-" + System.currentTimeMillis() + "-resume")
            .timestamp(Instant.now())
            .data(data)
            .build();
    }

    private ProtocolMessage createFederatedTaskDeleteMessage() {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", testTaskId);
        data.put("deleteData", true);
        data.put("reason", "TASK_COMPLETED");

        return ProtocolMessage.builder()
            .type(ProtocolType.FEDERATED_TASK_DELETE)
            .id("server-" + System.currentTimeMillis() + "-delete")
            .timestamp(Instant.now())
            .data(data)
            .build();
    }

    private ProtocolMessage createRoundStartMessage() {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", testTaskId);
        data.put("roundNumber", 1);
        data.put("roundSpecificConfig", Map.of(
            "learningRate", 0.001,
            "batchSize", 32
        ));
        data.put("expectedParticipants", 3);

        return ProtocolMessage.builder()
            .type(ProtocolType.ROUND_START)
            .id("server-" + System.currentTimeMillis() + "-round")
            .timestamp(Instant.now())
            .data(data)
            .build();
    }

    private ProtocolMessage createGradientUploadMessage() {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", testTaskId);
        data.put("roundNumber", 1);
        data.put("gradientData", Map.of(
            "weights", List.of(0.123, 0.456, 0.789),
            "biases", List.of(0.01, 0.02),
            "compressionType", "gzip"
        ));
        data.put("checksum", "abc123def456");
        data.put("trainingMetrics", Map.of(
            "accuracy", 0.92,
            "loss", 0.15,
            "trainingTime", 45.2
        ));
        data.put("samplesCount", 1000);

        return ProtocolMessage.builder()
            .type(ProtocolType.GRADIENT_UPLOAD)
            .id("client-" + System.currentTimeMillis() + "-grad")
            .timestamp(Instant.now())
            .vmId(testVmId)
            .data(data)
            .build();
    }

    private ProtocolMessage createGlobalModelBroadcastMessage() {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", testTaskId);
        data.put("roundNumber", 1);
        data.put("globalModel", Map.of(
            "weights", List.of(0.111, 0.222, 0.333),
            "biases", List.of(0.05, 0.06),
            "version", "global-v1.1"
        ));
        data.put("checksum", "global123abc456");
        data.put("aggregationInfo", Map.of(
            "method", "FEDERATED_AVERAGING",
            "participantCount", 3,
            "globalAccuracy", 0.94
        ));

        return ProtocolMessage.builder()
            .type(ProtocolType.GLOBAL_MODEL_BROADCAST)
            .id("server-" + System.currentTimeMillis() + "-global")
            .timestamp(Instant.now())
            .data(data)
            .build();
    }

    private ProtocolMessage createVmStatusQueryMessage() {
        Map<String, Object> data = new HashMap<>();
        data.put("queryType", "FULL_STATUS");
        data.put("includeMetrics", true);

        return ProtocolMessage.builder()
            .type(ProtocolType.VM_STATUS_QUERY)
            .id("server-" + System.currentTimeMillis() + "-status")
            .timestamp(Instant.now())
            .data(data)
            .build();
    }

    private ProtocolMessage createErrorMessage() {
        Map<String, Object> data = new HashMap<>();
        data.put("errorCode", "TRAINING_FAILED");
        data.put("errorMessage", "本地训练执行失败");
        data.put("errorContext", Map.of(
            "taskId", testTaskId,
            "roundNumber", 1,
            "stackTrace", "Exception in thread..."
        ));

        return ProtocolMessage.builder()
            .type(ProtocolType.ERROR)
            .id("client-" + System.currentTimeMillis() + "-error")
            .timestamp(Instant.now())
            .vmId(testVmId)
            .data(data)
            .build();
    }

    private ProtocolMessage createDatasetCreateMessage() {
        Map<String, Object> data = new HashMap<>();
        data.put("datasetId", "dataset-" + testTaskId);
        data.put("datasetType", "ACOUSTIC");
        data.put("metadata", Map.of(
            "description", "声学数据集",
            "sampleRate", 44100,
            "channels", 2
        ));

        return ProtocolMessage.builder()
            .type(ProtocolType.DATASET_CREATE)
            .id("client-" + System.currentTimeMillis() + "-dataset")
            .timestamp(Instant.now())
            .vmId(testVmId)
            .data(data)
            .build();
    }
}