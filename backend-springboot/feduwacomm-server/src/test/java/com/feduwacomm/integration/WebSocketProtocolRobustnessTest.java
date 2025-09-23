package com.feduwacomm.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.dto.ProtocolAck;
import com.feduwacomm.dto.ProtocolMessage;
import com.feduwacomm.dto.ProtocolType;
import com.feduwacomm.entity.VmInstance;
import com.feduwacomm.entity.User;
import com.feduwacomm.enums.VmStatus;
import com.feduwacomm.enums.ConnectionStatus;
import com.feduwacomm.enums.UserRole;
import com.feduwacomm.enums.UserStatus;
import com.feduwacomm.mapper.*;
import com.feduwacomm.service.WebSocketProtocolService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WebSocket协议健壮性测试
 * 根据WebSocket协议文档-中心化实现 进行全面测试
 * 
 * 注意：该测试类不使用@Transactional，因为需要测试跳过事务的情况
 * 使用@DirtiesContext确保每个测试方法后清理上下文
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class WebSocketProtocolRobustnessTest {

    @Autowired
    private WebSocketProtocolService protocolService;

    @Autowired
    private TrainingDatasetMapper trainingDatasetMapper;

    @Autowired
    private TrainingDatasetRowMapper trainingDatasetRowMapper;

    @Autowired
    private FederatedTasksMapper federatedTasksMapper;

    @Autowired
    private VmRoundModelsMapper vmRoundModelsMapper;

    @Autowired
    private VmInstancesMapper vmInstancesMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TEST_VM_ID = "test-vm-fixed-id";
    private static final String TEST_DATASET_ID = "test-ds-fixed-id";
    private static final String TEST_TASK_ID = "test-task-fixed-id";

    @BeforeEach
    void setUp() {
        // 确保测试管理员用户在数据库中存在
        createTestAdminUser();
        // 确保测试VM在数据库中存在
        createTestVmInstance();
    }

    // ==================== 1. 连接管理测试 ====================
    
    @Test
    @Order(1)
    @DisplayName("测试CONNECT消息处理")
    void testConnectMessage() {
        Map<String, Object> data = new HashMap<>();
        data.put("version", "1.0.0");
        data.put("capabilities", Arrays.asList("FEDERATED_AVERAGING", "FEDERATED_PROXIMAL"));
        
        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("os", "Ubuntu 20.04");
        systemInfo.put("python", "3.8.10");
        systemInfo.put("memory", "4GB");
        data.put("systemInfo", systemInfo);

        ProtocolMessage msg = buildMessage(ProtocolType.CONNECT, TEST_VM_ID, data);
        ProtocolAck ack = protocolService.handle(msg);

        assertNotNull(ack);
        assertEquals(ProtocolType.CONNECT_ACK, ack.getType());
        assertNotNull(ack.getData().get("sessionId"));
        assertEquals(30, ack.getData().get("heartbeatInterval"));
        assertEquals(10485760, ack.getData().get("maxMessageSize"));
    }

    @Test
    @Order(2)
    @DisplayName("测试HEARTBEAT消息处理")
    void testHeartbeatMessage() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "IDLE");
        
        Map<String, Object> resourceUsage = new HashMap<>();
        resourceUsage.put("cpu", 25.5);
        resourceUsage.put("memory", 60.2);
        resourceUsage.put("disk", 45.8);
        data.put("resourceUsage", resourceUsage);

        ProtocolMessage msg = buildMessage(ProtocolType.HEARTBEAT, TEST_VM_ID, data);
        ProtocolAck ack = protocolService.handle(msg);

        assertNotNull(ack);
        assertEquals(ProtocolType.HEARTBEAT_ACK, ack.getType());
        assertEquals(30, ack.getData().get("nextHeartbeat"));
        assertEquals("NORMAL", ack.getData().get("systemStatus"));
    }

    // ==================== 2. 数据集管理测试 ====================
    
    @Test
    @Order(3)
    @DisplayName("测试数据集创建")
    void testDatasetCreate() {
        Map<String, Object> data = new HashMap<>();
        data.put("datasetId", TEST_DATASET_ID);
        data.put("datasetDescription", "测试数据集");
        data.put("datasetType", "ACOUSTIC");
        data.put("metadata", Map.of("source", "test", "version", "1.0"));

        System.out.println("创建数据集请求: " + data);
        ProtocolMessage msg = buildMessage(ProtocolType.DATASET_CREATE, TEST_VM_ID, data);

        // 在新事务中执行WebSocket协议处理
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        ProtocolAck ack = transactionTemplate.execute(status -> {
            return protocolService.handle(msg);
        });

        System.out.println("协议响应: " + ack);
        assertEquals(ProtocolType.DATASET_CREATE_ACK, ack.getType());
        assertEquals("READY", ack.getData().get("status"));

        // 在新事务中验证数据库
        System.out.println("查询数据集ID: " + TEST_DATASET_ID);
        Map<String, Object> dataset = transactionTemplate.execute(status -> {
            return trainingDatasetMapper.selectByIdAsMap(TEST_DATASET_ID);
        });
        System.out.println("查询结果: " + dataset);
        assertNotNull(dataset);
        assertEquals("测试数据集", dataset.get("description"));
    }

    @Test
    @Order(4)
    @DisplayName("测试数据集追加行")
    void testDatasetAppendRows() {
        // 先创建数据集
        createTestDataset();

        // 追加数据行
        Map<String, Object> data = new HashMap<>();
        data.put("datasetId", TEST_DATASET_ID);
        
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("f1", 0.1 * i);
            row.put("f2", 2.0 * i);
            row.put("label", i % 2);
            rows.add(Map.of("rowData", row));
        }
        data.put("rows", rows);

        ProtocolMessage msg = buildMessage(ProtocolType.DATASET_APPEND_ROWS, TEST_VM_ID, data);
        ProtocolAck ack = protocolService.handle(msg);

        assertEquals(ProtocolType.DATASET_APPEND_ROWS_ACK, ack.getType());
        assertEquals(10, ack.getData().get("accepted"));
        assertEquals(0, ack.getData().get("rejected"));

        // 验证数据库
        int rowCount = trainingDatasetRowMapper.countByDataset(TEST_DATASET_ID);
        assertEquals(10, rowCount);
    }

    @Test
    @Order(5)
    @DisplayName("测试数据集批量追加压力测试")
    void testDatasetBatchAppendStress() throws InterruptedException, ExecutionException {
        createTestDataset();

        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Future<ProtocolAck>> futures = new ArrayList<>();
        
        // 并发追加1000条数据
        for (int batch = 0; batch < 10; batch++) {
            final int batchId = batch;
            Future<ProtocolAck> future = executor.submit(() -> {
                Map<String, Object> data = new HashMap<>();
                data.put("datasetId", TEST_DATASET_ID);
                
                List<Map<String, Object>> rows = new ArrayList<>();
                for (int i = 0; i < 100; i++) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("batch", batchId);
                    row.put("index", i);
                    row.put("value", Math.random());
                    rows.add(Map.of("rowData", row));
                }
                data.put("rows", rows);

                ProtocolMessage msg = buildMessage(ProtocolType.DATASET_APPEND_ROWS, TEST_VM_ID, data);
                return protocolService.handle(msg);
            });
            futures.add(future);
        }

        // 等待所有任务完成
        for (Future<ProtocolAck> future : futures) {
            ProtocolAck ack = future.get();
            assertEquals(ProtocolType.DATASET_APPEND_ROWS_ACK, ack.getType());
            assertEquals(100, ack.getData().get("accepted"));
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // 验证总数
        int totalCount = trainingDatasetRowMapper.countByDataset(TEST_DATASET_ID);
        assertEquals(1000, totalCount);
    }

    // ==================== 3. 训练控制测试 ====================
    
    @Test
    @Order(6)
    @DisplayName("测试训练启动")
    void testTrainingStart() {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", TEST_TASK_ID);
        data.put("algorithm", "FEDERATED_AVERAGING");
        
        Map<String, Object> config = new HashMap<>();
        config.put("batchSize", 32);
        config.put("learningRate", 0.001);
        config.put("epochsPerRound", 5);
        config.put("totalRounds", 100);
        config.put("currentRound", 0);
        data.put("config", config);

        ProtocolMessage msg = buildMessage(ProtocolType.TRAINING_START, TEST_VM_ID, data);
        ProtocolAck ack = protocolService.handle(msg);

        assertEquals(ProtocolType.TRAINING_START, ack.getType());
        assertEquals("RECEIVED", ack.getData().get("status"));
    }

    @Test
    @Order(7)
    @DisplayName("测试训练进度报告")
    void testTrainingProgress() {
        // 先启动训练
        startTestTraining();

        Map<String, Object> data = new HashMap<>();
        data.put("taskId", TEST_TASK_ID);
        data.put("currentRound", 25);
        data.put("totalRounds", 100);
        data.put("currentEpoch", 3);
        data.put("epochsPerRound", 5);
        data.put("progress", 25.5);
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("accuracy", 0.85);
        metrics.put("loss", 0.15);
        metrics.put("valAccuracy", 0.82);
        metrics.put("valLoss", 0.18);
        data.put("metrics", metrics);
        
        data.put("status", "RUNNING");

        ProtocolMessage msg = buildMessage(ProtocolType.TRAINING_PROGRESS, TEST_VM_ID, data);
        ProtocolAck ack = protocolService.handle(msg);

        assertEquals(ProtocolType.TRAINING_PROGRESS, ack.getType());
        assertEquals("RECEIVED", ack.getData().get("status"));
    }

    @Test
    @Order(8)
    @DisplayName("测试模型上传")
    void testModelUpload() {
        startTestTraining();

        Map<String, Object> data = new HashMap<>();
        data.put("taskId", TEST_TASK_ID);
        data.put("round", 25);
        
        Map<String, Object> parameters = new HashMap<>();
        Map<String, Object> model = new HashMap<>();
        model.put("framework", "pytorch");
        model.put("format", "state_dict");
        parameters.put("model", model);
        data.put("parameters", parameters);
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("accuracy", 0.88);
        metrics.put("loss", 0.12);
        data.put("metrics", metrics);

        ProtocolMessage msg = buildMessage(ProtocolType.MODEL_UPLOAD, TEST_VM_ID, data);
        ProtocolAck ack = protocolService.handle(msg);

        assertEquals(ProtocolType.MODEL_UPLOAD, ack.getType());
        assertEquals("RECEIVED", ack.getData().get("status"));
    }

    // ==================== 4. 错误处理测试 ====================
    
    @Test
    @Order(9)
    @DisplayName("测试无效消息处理")
    void testInvalidMessage() {
        // 测试null消息
        ProtocolAck ack = protocolService.handle(null);
        assertEquals(ProtocolType.MESSAGE_ERROR, ack.getType());
        assertEquals("INVALID_MESSAGE", ack.getData().get("errorCode"));

        // 测试无类型消息
        ProtocolMessage msg = ProtocolMessage.builder()
                .id("test-msg")
                .timestamp(Instant.now())
                .vmId(TEST_VM_ID)
                .build();
        ack = protocolService.handle(msg);
        assertEquals(ProtocolType.MESSAGE_ERROR, ack.getType());
    }

    @Test
    @Order(10)
    @DisplayName("测试缺失必要字段")
    void testMissingRequiredFields() {
        // 测试缺少datasetId
        Map<String, Object> data = new HashMap<>();
        data.put("datasetDescription", "测试");
        
        ProtocolMessage msg = buildMessage(ProtocolType.DATASET_CREATE, TEST_VM_ID, data);
        ProtocolAck ack = protocolService.handle(msg);
        
        assertEquals(ProtocolType.MESSAGE_ERROR, ack.getType());
        assertEquals("INVALID_DATASET_ID", ack.getData().get("errorCode"));
    }

    @Test
    @Order(11)
    @DisplayName("测试错误消息传播")
    void testErrorMessagePropagation() {
        Map<String, Object> data = new HashMap<>();
        data.put("errorCode", "TRAINING_FAILED");
        data.put("errorMessage", "模型训练失败");
        data.put("severity", "HIGH");
        
        Map<String, Object> details = new HashMap<>();
        details.put("exception", "ValueError: Invalid input");
        data.put("details", details);

        ProtocolMessage msg = buildMessage(ProtocolType.ERROR, TEST_VM_ID, data);
        ProtocolAck ack = protocolService.handle(msg);

        assertEquals(ProtocolType.ERROR, ack.getType());
        assertEquals("RECEIVED", ack.getData().get("status"));
    }

    // ==================== 5. 并发测试 ====================
    
    @Test
    @Order(12)
    @DisplayName("测试并发消息处理")
    void testConcurrentMessageHandling() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<ProtocolAck>> futures = new ArrayList<>();
        
        // 混合不同类型的消息
        for (int i = 0; i < 100; i++) {
            final int index = i;
            Future<ProtocolAck> future = executor.submit(() -> {
                ProtocolType type = switch (index % 5) {
                    case 0 -> ProtocolType.HEARTBEAT;
                    case 1 -> ProtocolType.STATUS_QUERY;
                    case 2 -> ProtocolType.TRAINING_PROGRESS;
                    case 3 -> ProtocolType.STATUS_RESPONSE;
                    default -> ProtocolType.CONNECT;
                };
                
                Map<String, Object> data = new HashMap<>();
                data.put("index", index);
                data.put("timestamp", Instant.now());
                
                ProtocolMessage msg = buildMessage(type, TEST_VM_ID, data);
                return protocolService.handle(msg);
            });
            futures.add(future);
        }

        // 验证所有消息都得到处理
        for (Future<ProtocolAck> future : futures) {
            ProtocolAck ack = future.get();
            assertNotNull(ack);
            assertNotNull(ack.getType());
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }

    // ==================== 6. 状态查询测试 ====================
    
    @Test
    @Order(13)
    @DisplayName("测试状态查询与响应")
    void testStatusQueryAndResponse() {
        // 状态查询
        Map<String, Object> queryData = new HashMap<>();
        queryData.put("queryType", "FULL");
        queryData.put("includeResources", true);
        queryData.put("includeProcesses", true);
        
        ProtocolMessage queryMsg = buildMessage(ProtocolType.STATUS_QUERY, TEST_VM_ID, queryData);
        ProtocolAck queryAck = protocolService.handle(queryMsg);
        
        assertEquals(ProtocolType.STATUS_QUERY, queryAck.getType());
        assertEquals("FORWARDED", queryAck.getData().get("status"));

        // 状态响应
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("status", "RUNNING");
        responseData.put("uptime", 3600);
        
        Map<String, Object> resourceUsage = new HashMap<>();
        resourceUsage.put("cpu", 75.5);
        resourceUsage.put("memory", 82.3);
        responseData.put("resourceUsage", resourceUsage);
        
        ProtocolMessage responseMsg = buildMessage(ProtocolType.STATUS_RESPONSE, TEST_VM_ID, responseData);
        ProtocolAck responseAck = protocolService.handle(responseMsg);
        
        assertEquals(ProtocolType.STATUS_RESPONSE, responseAck.getType());
        assertEquals("UPDATED", responseAck.getData().get("status"));
    }

    // ==================== 7. 边界条件测试 ====================
    
    @Test
    @Order(14)
    @DisplayName("测试超大消息处理")
    void testLargeMessageHandling() {
        Map<String, Object> data = new HashMap<>();
        data.put("datasetId", TEST_DATASET_ID);
        
        // 创建大量数据行
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            Map<String, Object> row = new HashMap<>();
            // 每行包含较多字段
            for (int j = 0; j < 50; j++) {
                row.put("field_" + j, "value_" + i + "_" + j);
            }
            rows.add(Map.of("rowData", row));
        }
        data.put("rows", rows);

        ProtocolMessage msg = buildMessage(ProtocolType.DATASET_APPEND_ROWS, TEST_VM_ID, data);
        
        // 应该能够处理，但可能较慢
        long startTime = System.currentTimeMillis();
        ProtocolAck ack = protocolService.handle(msg);
        long endTime = System.currentTimeMillis();
        
        assertEquals(ProtocolType.DATASET_APPEND_ROWS_ACK, ack.getType());
        System.out.println("处理1000条大数据行耗时: " + (endTime - startTime) + "ms");
    }

    @Test
    @Order(15)
    @DisplayName("测试特殊字符处理")
    void testSpecialCharacterHandling() {
        String specialDatasetId = "ds-特殊字符-测试-😀";
        Map<String, Object> data = new HashMap<>();
        data.put("datasetId", specialDatasetId);
        data.put("datasetDescription", "包含特殊字符：\n\t\"'<>&");
        data.put("datasetType", "OTHER");
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("emoji", "🚀💻🔧");
        metadata.put("chinese", "中文测试");
        metadata.put("special", "\\n\\t\\r");
        data.put("metadata", metadata);

        ProtocolMessage msg = buildMessage(ProtocolType.DATASET_CREATE, TEST_VM_ID, data);
        ProtocolAck ack = protocolService.handle(msg);
        
        assertEquals(ProtocolType.DATASET_CREATE_ACK, ack.getType());
        assertEquals("READY", ack.getData().get("status"));
    }

    // ==================== 辅助方法 ====================
    
    private ProtocolMessage buildMessage(ProtocolType type, String vmId, Map<String, Object> data) {
        return ProtocolMessage.builder()
                .type(type)
                .id(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .vmId(vmId)
                .data(data)
                .signature("test-signature")
                .build();
    }

    private void createTestAdminUser() {
        // 检查管理员用户是否已存在
        try {
            var existingAdmin = userMapper.selectFirstAdmin();
            if (existingAdmin == null) {
                User adminUser = new User();
                adminUser.setId("test-admin-id");
                adminUser.setUsername("test-admin");
                adminUser.setEmail("test@example.com");
                adminUser.setPasswordHash("hashedPassword123");
                adminUser.setStatus(UserStatus.ACTIVE);
                adminUser.setRole(UserRole.ADMIN);
                adminUser.setCreatedAt(java.time.LocalDateTime.now());
                adminUser.setUpdatedAt(java.time.LocalDateTime.now());

                userMapper.insert(adminUser);
            }
        } catch (Exception e) {
            // 忽略创建错误，可能已存在
        }
    }

    private void createTestDataset() {
        Map<String, Object> data = new HashMap<>();
        data.put("datasetId", TEST_DATASET_ID);
        data.put("datasetDescription", "测试数据集");
        data.put("datasetType", "ACOUSTIC");

        ProtocolMessage msg = buildMessage(ProtocolType.DATASET_CREATE, TEST_VM_ID, data);
        protocolService.handle(msg);
    }

    private void startTestTraining() {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", TEST_TASK_ID);
        data.put("algorithm", "FEDERATED_AVERAGING");
        
        Map<String, Object> config = new HashMap<>();
        config.put("totalRounds", 100);
        config.put("currentRound", 0);
        data.put("config", config);
        
        ProtocolMessage msg = buildMessage(ProtocolType.TRAINING_START, TEST_VM_ID, data);
        protocolService.handle(msg);
    }

    private void createTestVmInstance() {
        // 检查VM实例是否已存在
        if (vmInstancesMapper.existsByVmId(TEST_VM_ID) == 0) {
            VmInstance vmInstance = new VmInstance();
            vmInstance.setId(TEST_VM_ID);
            vmInstance.setName("Test VM Instance");
            vmInstance.setIpAddress("127.0.0.1");
            vmInstance.setPort(8080);
            vmInstance.setOsType("Linux");
            vmInstance.setCpuCores(4);
            vmInstance.setMemoryMb(8192);
            vmInstance.setDiskGb(500);
            vmInstance.setStatus(VmStatus.RUNNING);
            vmInstance.setConnectionStatus(ConnectionStatus.CONNECTED);
            vmInstance.setCreatedAt(java.time.LocalDateTime.now());
            vmInstance.setUpdatedAt(java.time.LocalDateTime.now());
            vmInstance.setCreatedBy("test-admin");
            vmInstance.setUpdatedBy("test-admin");

            vmInstancesMapper.insert(vmInstance);
        }
    }

    @AfterEach
    void cleanup() {
        // 清理测试数据
        try {
            trainingDatasetRowMapper.deleteByDataset(TEST_DATASET_ID);
            trainingDatasetMapper.deleteById(TEST_DATASET_ID);
            // 清理测试VM实例
            vmInstancesMapper.deleteByVmId(TEST_VM_ID);
            // 清理测试管理员用户
            userMapper.deleteById("test-admin-id");
        } catch (Exception e) {
            // 忽略清理错误
        }
    }
}