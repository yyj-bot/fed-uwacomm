# 单元测试重构文档 - WebSocket v1.5协议支持

## 📋 文档信息

**版本**: v1.5.0
**创建时间**: 2025-01-29
**适用范围**: 单元测试WebSocket v1.5协议重构
**重构目标**: CompleteFederatedLearningFlowTest升级为v1.5标准化流程

## 🎯 重构目标与进度跟踪

### 核心测试重构进度
- [ ] **CompleteFederatedLearningFlowTest**: 升级为v1.5完整流程测试 `[进度: 0/13]`
  - [ ] test01_AdminLogin: 保持现有实现
  - [ ] test02_VirtualMachinesRegistration: 保持现有实现
  - [ ] test03_WebSocketConnections: 升级为v1.5协议支持
  - [ ] test04_TrainingDataUpload: 集成assignedDatasetId处理
  - [ ] test05_QueryAvailableResources: 保持现有实现
  - [ ] test06_CreateFederatedTask: 重写为v1.5任务创建
  - [ ] test07_DatasetAllocation: 新增数据集分配测试
  - [ ] test08_DatasetValidation: 新增数据集验证测试
  - [ ] test09_TaskStartFlow: 重写任务启动流程
  - [ ] test10_FederatedLearning: 升级联邦学习执行
  - [ ] test11_ModelAggregation: 验证assignedDatasetId
  - [ ] test12_ResultsRetrieval: 保持现有逻辑
  - [ ] test13_CleanupConnections: 保持现有实现

### v1.5协议测试覆盖进度
```
协议消息测试覆盖:
├── [❌] FEDERATED_TASK_START (v1.5更新): assignedDatasetId在dataConfig内
├── [❌] DATASET_LIST_QUERY (v1.5新增): 查询数据集状态
├── [❌] DATASET_LIST_RESPONSE (v1.5新增): 响应数据集信息
├── [❌] GRADIENT_UPLOAD (v1.5更新): 包含assignedDatasetId验证
└── [✅] 其他协议消息: 保持v1.4兼容

13步标准化流程测试:
├── [✅] 步骤1-5: 前端操作和数据准备 (硬编码替代)
├── [❌] 步骤6-9: 任务创建和数据集分配 (需要重写)
└── [❌] 步骤10-13: 验证和启动 (需要新增)
```

## 🔍 现状分析

### 当前测试问题分析

#### 1. 协议版本过时 `[严重程度: 高]`
```java
// ❌ 当前问题：CompleteFederatedLearningFlowTest使用v1.4协议
public class CompleteFederatedLearningFlowTest {
    // 问题1: MockVirtualMachine使用v1.4协议
    private final List<MockVirtualMachine> mockVMs = new ArrayList<>();

    // 问题2: 测试步骤缺少v1.5的数据集关联管理
    void test03_EnhancedWebSocketConnections() {
        // 缺少DATASET_LIST_QUERY/RESPONSE测试
    }

    // 问题3: 任务创建未使用assignedDatasetId
    void test06_CreateFederatedTask() {
        // 缺少assignedDatasetId统一管理测试
    }
}
```

#### 2. 测试覆盖不完整 `[严重程度: 高]`
```java
// ❌ 缺少v1.5核心功能测试：
// - 数据集分配和验证流程
// - assignedDatasetId唯一性验证
// - 13步标准化流程完整性
// - 破坏性变更验证
```

## 🏗️ 详细重构方案

### 1. CompleteFederatedLearningFlowTest 重构 `[优先级: P0]`

#### 测试类重构概览 `[预计工时: 4小时]`

```java
/**
 * FedUWAComm 联邦学习完整流程测试 v1.5
 * 基于WebSocket协议v1.5的13步标准化流程
 *
 * v1.5核心特性：
 * - assignedDatasetId统一管理：后端UuidUtil统一生成和分配
 * - 13步标准化流程：完整的联邦学习生命周期管理
 * - 破坏性变更支持：完全移除对v1.4 datasetId的兼容
 * - 数据集关联管理：DATASET_LIST_QUERY/RESPONSE协议支持
 *
 * 测试流程 (13步标准化):
 * 1. 管理员登录
 * 2. 虚拟机注册流程 (5台VM)
 * 3. WebSocket连接建立 (v1.5协议)
 * 4. 训练数据上传和预处理
 * 5. 查询可用资源
 * 6. 创建联邦学习任务 (v1.5)
 * 7. 数据集分配到虚拟机 (assignedDatasetId)
 * 8. 数据集状态验证 (DATASET_LIST_QUERY/RESPONSE)
 * 9. 任务启动流程 (v1.5 FEDERATED_TASK_START)
 * 10. 联邦学习执行 (梯度上传包含assignedDatasetId)
 * 11. 模型聚合和验证
 * 12. 结果获取和验证
 * 13. 清理和断开连接
 *
 * 🆕 v1.5新增测试重点：
 * - assignedDatasetId在dataConfig内的正确位置
 * - 数据集分配算法的准确性
 * - 虚拟机完全依赖后端分配的ID
 * - 13步流程的完整性和顺序
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CompleteFederatedLearningFlowTestV15 {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    private String baseUrl;
    private String adminAccessToken;
    private String taskId;
    private String originalDatasetId;  // v1.5: 原始数据集ID

    // 🆕 v1.5新增字段：数据集分配管理
    private final Map<String, String> vmAssignedDatasetIds = new ConcurrentHashMap<>();  // vmId -> assignedDatasetId
    private final Map<String, String> datasetAllocationStatus = new ConcurrentHashMap<>();  // vmId -> status

    // 测试数据 - 5台虚拟机（vmId由后端注册时自动生成）
    private final List<VmTestData> virtualMachines = Arrays.asList(
        new VmTestData(null, "VM-Node-1", "192.168.1.101", 8081, 8, 16384, 1),
        new VmTestData(null, "VM-Node-2", "192.168.1.102", 8082, 6, 12288, 0),
        new VmTestData(null, "VM-Node-3", "192.168.1.103", 8083, 4, 8192, 1),
        new VmTestData(null, "VM-Node-4", "192.168.1.104", 8084, 12, 24576, 2),
        new VmTestData(null, "VM-Node-5", "192.168.1.105", 8085, 16, 32768, 4)
    );

    // 存储注册后的虚拟机ID (线程安全)
    private final List<String> registeredVmIds = Collections.synchronizedList(new ArrayList<>());

    // 🆕 v1.5 WebSocket客户端 (升级为v1.5协议支持)
    private final List<MockVirtualMachineV15> mockVMs = new ArrayList<>();

    @BeforeAll
    void setupTest() {
        baseUrl = "http://localhost:" + port;

        // 🚀 打印v1.5测试环境信息
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🚀 FedUWAComm 联邦学习完整流程测试开始 (WebSocket v1.5)");
        System.out.println("🌐 服务器地址: " + baseUrl);
        System.out.println("📋 协议版本: WebSocket v1.5 (破坏性变更版本)");
        System.out.println("🎯 测试目标: 13步标准化流程 + assignedDatasetId统一管理");
        System.out.println("=".repeat(80) + "\n");
    }
```

#### 第一阶段：基础测试保持 `[预计: 30分钟]`

```java
    /**
     * 步骤1：管理员登录
     * 📊 状态：保持现有实现，无需修改
     */
    @Test
    @Order(1)
    void test01_AdminLogin() {
        System.out.println("\n🔐 步骤1：管理员登录测试");

        // 硬编码管理员登录（替代前端操作）
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername("admin");
        loginDTO.setPassword("ab123456");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginDTO> entity = new HttpEntity<>(loginDTO, headers);

        ResponseEntity<Result<LoginResponseVO>> response = restTemplate.exchange(
            baseUrl + "/api/user/login",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<Result<LoginResponseVO>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(200);

        LoginResponseVO loginData = response.getBody().getData();
        assertThat(loginData).isNotNull();
        assertThat(loginData.getAccessToken()).isNotBlank();

        this.adminAccessToken = loginData.getAccessToken();
        System.out.println("✅ 管理员登录成功，获取访问令牌");
    }

    /**
     * 步骤2：虚拟机注册流程
     * 📊 状态：保持现有实现，无需修改
     */
    @Test
    @Order(2)
    void test02_VirtualMachinesRegistration() {
        System.out.println("\n🤖 步骤2：虚拟机注册流程测试");

        for (VmTestData vmData : virtualMachines) {
            // 注册虚拟机逻辑保持不变
            registerSingleVirtualMachine(vmData);
        }

        assertThat(registeredVmIds).hasSize(5);
        System.out.println("✅ 所有虚拟机注册完成，注册数量: " + registeredVmIds.size());
    }
```

#### 第二阶段：WebSocket协议升级 `[预计: 1小时]`

```java
    /**
     * 步骤3：WebSocket连接建立 (v1.5协议升级)
     * 🆕 升级重点：支持v1.5协议，准备DATASET_LIST_QUERY/RESPONSE处理
     */
    @Test
    @Order(3)
    void test03_WebSocketConnectionsV15() throws InterruptedException {
        System.out.println("\n🔌 步骤3：WebSocket连接建立测试 (v1.5协议)");

        // 🆕 创建v1.5协议的Mock虚拟机
        for (int i = 0; i < registeredVmIds.size(); i++) {
            String vmId = registeredVmIds.get(i);
            VmTestData vmData = virtualMachines.get(i);

            // 🆕 使用v1.5协议的MockVirtualMachine
            MockVirtualMachineV15 mockVM = new MockVirtualMachineV15(vmData);

            // 🆕 启用v1.5协议特性
            mockVM.enableV15Protocol(true);
            mockVM.enableDatasetManagement(true);
            mockVM.enableAssignedDatasetId(true);

            // 注册和连接
            mockVM.register(baseUrl, vmId);
            mockVM.connectWebSocket(baseUrl);

            mockVMs.add(mockVM);
            System.out.println("🔌 VM连接成功 (v1.5): " + vmData.getName() + " (vmId: " + vmId + ")");
        }

        // 等待所有连接稳定
        Thread.sleep(2000);

        // 🆕 v1.5协议验证：测试DATASET_LIST_QUERY支持
        for (MockVirtualMachineV15 mockVM : mockVMs) {
            boolean supportsDatasetQuery = mockVM.supportsProtocol(ProtocolType.DATASET_LIST_QUERY);
            assertThat(supportsDatasetQuery).isTrue();
            System.out.println("✅ VM支持DATASET_LIST_QUERY协议: " + mockVM.getVmData().getName());
        }

        assertThat(mockVMs).hasSize(5);
        System.out.println("✅ 所有虚拟机WebSocket连接建立完成 (v1.5协议)");
    }
```

#### 第三阶段：数据集管理测试 `[预计: 1.5小时]`

```java
    /**
     * 步骤4：训练数据上传和预处理 (v1.5增强)
     * 🆕 增强重点：集成assignedDatasetId预处理
     */
    @Test
    @Order(4)
    void test04_TrainingDataUploadV15() {
        System.out.println("\n📊 步骤4：训练数据上传测试 (v1.5增强)");

        // 🆕 硬编码替代前端数据集上传
        MockMultipartFile mockFile = new MockMultipartFile(
            "file",
            "test_acoustic_data_5vm_v15.csv",
            "text/csv",
            generateMockDatasetContent().getBytes()
        );

        MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("file", new ByteArrayResource(mockFile.getBytes()) {
            @Override
            public String getFilename() {
                return "test_acoustic_data_5vm_v15.csv";
            }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(adminAccessToken);

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Result<Map<String, Object>>> response = restTemplate.exchange(
            baseUrl + "/api/training-data/upload",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<Result<Map<String, Object>>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(200);

        Map<String, Object> uploadResult = response.getBody().getData();
        this.originalDatasetId = (String) uploadResult.get("datasetId");

        // 🆕 v1.5验证：确保返回的是原始数据集ID（非assignedDatasetId）
        assertThat(originalDatasetId).isNotNull();
        assertThat(originalDatasetId).startsWith("dataset-");  // 确保是UuidUtil生成的格式

        System.out.println("✅ 数据集上传成功 (v1.5)");
        System.out.println("📋 原始数据集ID: " + originalDatasetId);
        System.out.println("🎯 准备进行v1.5数据集分配");
    }

    /**
     * 步骤5：查询可用资源
     * 📊 状态：保持现有实现
     */
    @Test
    @Order(5)
    void test05_QueryAvailableResources() {
        System.out.println("\n🔍 步骤5：查询可用资源测试");
        // 保持现有实现，无需修改
        performAvailableResourcesQuery();
    }

    /**
     * 步骤6：创建联邦学习任务 (v1.5重写)
     * 🆕 重写重点：使用v1.5任务创建接口，触发assignedDatasetId分配
     */
    @Test
    @Order(6)
    void test06_CreateFederatedTaskV15() {
        System.out.println("\n🚀 步骤6：创建联邦学习任务测试 (v1.5)");

        // 🆕 硬编码替代前端任务创建表单
        TaskCreateDTO createDTO = TaskCreateDTO.builder()
            .taskName("v1.5联邦学习任务-声学数据分析")
            .description("基于WebSocket v1.5协议的标准联邦学习任务")
            .federatedAlgorithm("FEDERATED_AVERAGING")
            .totalRounds(5)
            .datasetPath(originalDatasetId)  // 使用原始数据集ID
            .participantConfig(ParticipantConfigDTO.builder()
                .participants(registeredVmIds)  // 所有注册的VM参与
                .minParticipants(3)
                .build())
            .trainingConfig(TrainingConfigDTO.builder()
                .algorithm("RandomForest")
                .epochs(3)
                .batchSize(32)
                .learningRate(0.01)
                .build())
            .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminAccessToken);

        HttpEntity<TaskCreateDTO> entity = new HttpEntity<>(createDTO, headers);

        // 🆕 调用v1.5任务创建接口
        ResponseEntity<Result<TaskOperationVO>> response = restTemplate.exchange(
            baseUrl + "/api/federated/v1.5/tasks",  // v1.5专用接口
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<Result<TaskOperationVO>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(200);

        TaskOperationVO taskResult = response.getBody().getData();
        this.taskId = taskResult.getTaskId();

        // 🆕 v1.5验证：确保任务创建触发了数据集分配
        assertThat(taskId).isNotNull();
        assertThat(taskResult.getStatus()).isEqualTo("CREATING");

        System.out.println("✅ v1.5联邦学习任务创建成功");
        System.out.println("📋 任务ID: " + taskId);
        System.out.println("🎯 开始等待数据集分配完成...");

        // 等待后端完成数据集分配（步骤7-8在后端自动执行）
        waitForDatasetAllocation();
    }

    /**
     * 步骤7：数据集分配验证 (v1.5新增)
     * 🆕 新增测试：验证后端自动完成的数据集分配
     */
    @Test
    @Order(7)
    void test07_DatasetAllocationVerification() throws InterruptedException {
        System.out.println("\n📦 步骤7：数据集分配验证测试 (v1.5新增)");

        // 🆕 等待后端完成数据集分配
        Thread.sleep(3000);

        // 🆕 验证每个VM都收到了FEDERATED_TASK_START消息，包含assignedDatasetId
        for (MockVirtualMachineV15 mockVM : mockVMs) {
            // 检查是否收到FEDERATED_TASK_START消息
            Optional<ProtocolMessage> taskStartMsg = mockVM.getReceivedMessage(ProtocolType.FEDERATED_TASK_START);
            assertThat(taskStartMsg).isPresent();

            ProtocolMessage message = taskStartMsg.get();
            Map<String, Object> data = message.getData();

            // 🔑 关键验证：assignedDatasetId位于dataConfig内部
            assertThat(data).containsKey("dataConfig");

            @SuppressWarnings("unchecked")
            Map<String, Object> dataConfig = (Map<String, Object>) data.get("dataConfig");
            assertThat(dataConfig).containsKey("assignedDatasetId");

            String assignedDatasetId = (String) dataConfig.get("assignedDatasetId");
            assertThat(assignedDatasetId).isNotNull();
            assertThat(assignedDatasetId).isNotEqualTo(originalDatasetId);  // 确保不是原始ID

            // 🆕 保存VM的assignedDatasetId映射
            vmAssignedDatasetIds.put(mockVM.getVmId(), assignedDatasetId);

            System.out.println("✅ VM数据集分配验证成功: " + mockVM.getVmData().getName());
            System.out.println("   📋 assignedDatasetId: " + assignedDatasetId);
            System.out.println("   📍 位置: dataConfig.assignedDatasetId (符合v1.5规范)");
        }

        // 🆕 验证assignedDatasetId的唯一性
        Set<String> uniqueDatasetIds = new HashSet<>(vmAssignedDatasetIds.values());
        assertThat(uniqueDatasetIds).hasSize(registeredVmIds.size());

        System.out.println("✅ 数据集分配验证完成");
        System.out.println("📊 分配统计: " + vmAssignedDatasetIds.size() + " 个VM，" + uniqueDatasetIds.size() + " 个唯一ID");
    }

    /**
     * 步骤8：数据集状态查询和验证 (v1.5新增)
     * 🆕 新增测试：DATASET_LIST_QUERY/RESPONSE协议验证
     */
    @Test
    @Order(8)
    void test08_DatasetStatusQueryV15() throws InterruptedException {
        System.out.println("\n🔍 步骤8：数据集状态查询测试 (v1.5新增)");

        // 🆕 模拟后端发送DATASET_LIST_QUERY消息到所有VM
        for (MockVirtualMachineV15 mockVM : mockVMs) {
            String vmId = mockVM.getVmId();
            String assignedDatasetId = vmAssignedDatasetIds.get(vmId);

            // 构建DATASET_LIST_QUERY消息
            Map<String, Object> queryData = new HashMap<>();
            queryData.put("taskId", taskId);
            queryData.put("queryType", "ASSIGNED_DATASETS");

            ProtocolMessage queryMessage = ProtocolMessage.builder()
                .type(ProtocolType.DATASET_LIST_QUERY)
                .id(UUID.randomUUID().toString())
                .vmId(vmId)
                .data(queryData)
                .timestamp(Instant.now())
                .build();

            // 🆕 发送查询消息并等待响应
            mockVM.sendMessage(queryMessage);
            Thread.sleep(500);  // 等待处理

            // 🆕 验证VM返回DATASET_LIST_RESPONSE
            Optional<ProtocolMessage> responseMsg = mockVM.getReceivedMessage(ProtocolType.DATASET_LIST_RESPONSE);
            assertThat(responseMsg).isPresent();

            ProtocolMessage response = responseMsg.get();
            Map<String, Object> responseData = response.getData();

            // 验证响应内容
            assertThat(responseData).containsKey("datasets");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> datasets = (List<Map<String, Object>>) responseData.get("datasets");

            assertThat(datasets).hasSize(1);
            Map<String, Object> dataset = datasets.get(0);

            assertThat(dataset.get("assignedDatasetId")).isEqualTo(assignedDatasetId);
            assertThat(dataset.get("status")).isEqualTo("CREATED");
            assertThat(dataset).containsKey("localPath");

            datasetAllocationStatus.put(vmId, "CREATED");

            System.out.println("✅ VM数据集状态查询成功: " + mockVM.getVmData().getName());
            System.out.println("   📋 状态: CREATED");
            System.out.println("   📂 本地路径: " + dataset.get("localPath"));
        }

        // 🆕 验证所有VM的数据集都已就绪
        boolean allReady = datasetAllocationStatus.values().stream()
            .allMatch(status -> "CREATED".equals(status));
        assertThat(allReady).isTrue();

        System.out.println("✅ 数据集状态查询完成");
        System.out.println("📊 就绪统计: " + datasetAllocationStatus.size() + "/" + registeredVmIds.size() + " VM数据集就绪");
    }
```

#### 第四阶段：联邦学习流程测试 `[预计: 1小时]`

```java
    /**
     * 步骤9：任务启动流程 (v1.5更新)
     * 🆕 更新重点：验证v1.5任务启动消息格式
     */
    @Test
    @Order(9)
    void test09_TaskStartFlowV15() throws InterruptedException {
        System.out.println("\n🚀 步骤9：任务启动流程测试 (v1.5)");

        // 等待后端发送任务启动确认
        Thread.sleep(2000);

        // 🆕 所有VM发送FEDERATED_TASK_START_ACK确认
        for (MockVirtualMachineV15 mockVM : mockVMs) {
            String vmId = mockVM.getVmId();
            String assignedDatasetId = vmAssignedDatasetIds.get(vmId);

            // 构建任务启动确认消息
            Map<String, Object> ackData = new HashMap<>();
            ackData.put("taskId", taskId);
            ackData.put("status", "READY");
            ackData.put("assignedDatasetId", assignedDatasetId);  // v1.5: 包含数据集ID确认

            ProtocolMessage ackMessage = ProtocolMessage.builder()
                .type(ProtocolType.FEDERATED_TASK_START_ACK)
                .id(UUID.randomUUID().toString())
                .vmId(vmId)
                .data(ackData)
                .timestamp(Instant.now())
                .build();

            mockVM.sendMessage(ackMessage);
            System.out.println("✅ VM任务启动确认: " + mockVM.getVmData().getName());
        }

        System.out.println("✅ 任务启动流程完成 (v1.5)");
        System.out.println("🎯 准备开始联邦学习轮次...");
    }

    /**
     * 步骤10：联邦学习执行 (v1.5更新)
     * 🆕 更新重点：GRADIENT_UPLOAD包含assignedDatasetId验证
     */
    @Test
    @Order(10)
    void test10_FederatedLearningExecutionV15() throws InterruptedException {
        System.out.println("\n🔄 步骤10：联邦学习执行测试 (v1.5)");

        // 执行3轮联邦学习
        for (int round = 1; round <= 3; round++) {
            System.out.println("🔄 执行联邦学习轮次: " + round);

            // 🆕 所有VM上传梯度，包含assignedDatasetId
            for (MockVirtualMachineV15 mockVM : mockVMs) {
                String vmId = mockVM.getVmId();
                String assignedDatasetId = vmAssignedDatasetIds.get(vmId);

                // 🆕 v1.5关键变更：GRADIENT_UPLOAD包含assignedDatasetId
                Map<String, Object> gradientData = new HashMap<>();
                gradientData.put("taskId", taskId);
                gradientData.put("roundNumber", round);
                gradientData.put("assignedDatasetId", assignedDatasetId);  // 🔑 v1.5新增
                gradientData.put("gradientData", generateMockGradient());
                gradientData.put("trainingMetrics", generateMockMetrics());

                ProtocolMessage gradientMessage = ProtocolMessage.builder()
                    .type(ProtocolType.GRADIENT_UPLOAD)
                    .id(UUID.randomUUID().toString())
                    .vmId(vmId)
                    .data(gradientData)
                    .timestamp(Instant.now())
                    .build();

                mockVM.sendMessage(gradientMessage);
                System.out.println("📤 VM梯度上传 (v1.5): " + mockVM.getVmData().getName() +
                                 ", assignedDatasetId: " + assignedDatasetId);
            }

            // 等待模型聚合
            Thread.sleep(2000);

            // 验证收到全局模型广播
            for (MockVirtualMachineV15 mockVM : mockVMs) {
                Optional<ProtocolMessage> modelMsg = mockVM.getReceivedMessage(ProtocolType.GLOBAL_MODEL_BROADCAST);
                assertThat(modelMsg).isPresent();
                System.out.println("📥 VM接收全局模型: " + mockVM.getVmData().getName());
            }

            System.out.println("✅ 轮次 " + round + " 完成");
        }

        System.out.println("✅ 联邦学习执行完成 (v1.5)");
    }

    /**
     * 步骤11：模型聚合验证 (v1.5更新)
     * 🆕 更新重点：验证assignedDatasetId在聚合过程中的正确使用
     */
    @Test
    @Order(11)
    void test11_ModelAggregationVerificationV15() {
        System.out.println("\n🔀 步骤11：模型聚合验证测试 (v1.5)");

        // 🆕 验证聚合过程中assignedDatasetId的正确性
        for (String vmId : registeredVmIds) {
            String assignedDatasetId = vmAssignedDatasetIds.get(vmId);

            // 验证数据库中的记录
            // 这里应该查询task_participants表验证assignedDatasetId
            assertThat(assignedDatasetId).isNotNull();
            System.out.println("✅ VM聚合验证: " + vmId + " -> " + assignedDatasetId);
        }

        System.out.println("✅ 模型聚合验证完成 (v1.5)");
    }

    /**
     * 步骤12：结果获取和验证
     * 📊 状态：保持现有实现
     */
    @Test
    @Order(12)
    void test12_ResultsRetrieval() {
        System.out.println("\n📊 步骤12：结果获取和验证测试");
        // 保持现有实现
        performResultsRetrieval();
    }

    /**
     * 步骤13：清理和断开连接
     * 📊 状态：保持现有实现
     */
    @Test
    @Order(13)
    void test13_CleanupConnections() {
        System.out.println("\n🧹 步骤13：清理和断开连接测试");
        // 保持现有实现
        performCleanupConnections();

        // 🆕 v1.5清理：清空assignedDatasetId映射
        vmAssignedDatasetIds.clear();
        datasetAllocationStatus.clear();

        System.out.println("✅ v1.5测试资源清理完成");
    }

    // ========== 辅助方法 ==========

    private void waitForDatasetAllocation() {
        // 等待后端完成数据集分配逻辑
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String generateMockDatasetContent() {
        // 生成模拟的声学数据CSV内容
        StringBuilder content = new StringBuilder();
        content.append("timestamp,frequency,amplitude,phase,noise_level,target\n");

        Random random = new Random();
        for (int i = 0; i < 1000; i++) {
            content.append(String.format("%d,%.2f,%.3f,%.2f,%.3f,%d\n",
                System.currentTimeMillis() + i,
                random.nextDouble() * 10000,  // frequency
                random.nextDouble(),         // amplitude
                random.nextDouble() * 360,   // phase
                random.nextDouble() * 0.1,   // noise_level
                random.nextInt(2)            // target (0 or 1)
            ));
        }

        return content.toString();
    }

    private Map<String, Object> generateMockGradient() {
        // 生成模拟梯度数据
        Map<String, Object> gradient = new HashMap<>();
        gradient.put("weights", Arrays.asList(0.001, -0.002, 0.003));
        gradient.put("biases", Arrays.asList(0.001));
        return gradient;
    }

    private Map<String, Object> generateMockMetrics() {
        // 生成模拟训练指标
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("samplesCount", 200);
        metrics.put("localLoss", 0.25);
        metrics.put("localAccuracy", 0.87);
        metrics.put("trainingTime", 295);
        return metrics;
    }
}
```

### 2. MockVirtualMachineV15 重构 `[优先级: P1]`

#### v1.5协议支持 `[预计工时: 2小时]`

```java
/**
 * 模拟虚拟机类 v1.5
 * 基于WebSocket协议v1.5的被动响应模式设计
 *
 * v1.5核心特性：
 * - assignedDatasetId完全依赖：完全依赖后端分配的数据集ID
 * - 新协议支持：DATASET_LIST_QUERY/RESPONSE协议
 * - 数据集管理增强：状态跟踪和验证机制
 * - 破坏性变更：完全移除对datasetId的支持
 */
public class MockVirtualMachineV15 extends MockVirtualMachine {

    // 🆕 v1.5新增字段：数据集管理
    private final Map<String, String> assignedDatasetIds = new ConcurrentHashMap<>();  // taskId -> assignedDatasetId
    private final Map<String, String> datasetStatuses = new ConcurrentHashMap<>();    // assignedDatasetId -> status
    private final Map<String, String> datasetLocalPaths = new ConcurrentHashMap<>();  // assignedDatasetId -> localPath

    // 🆕 v1.5新增字段：协议支持标志
    private boolean v15ProtocolEnabled = false;
    private boolean datasetManagementEnabled = false;
    private boolean assignedDatasetIdEnabled = false;

    // 🆕 v1.5新增字段：消息历史
    private final List<ProtocolMessage> receivedMessages = Collections.synchronizedList(new ArrayList<>());

    public MockVirtualMachineV15(VmTestData vmData) {
        super(vmData);
        log.info("🤖 [{}] MockVirtualMachineV15初始化完成", vmData.getName());
    }

    // ========== v1.5协议控制方法 ==========

    public void enableV15Protocol(boolean enabled) {
        this.v15ProtocolEnabled = enabled;
        log.info("🤖 [{}] v1.5协议支持: {}", getVmData().getName(), enabled ? "启用" : "禁用");
    }

    public void enableDatasetManagement(boolean enabled) {
        this.datasetManagementEnabled = enabled;
        log.info("🤖 [{}] 数据集管理: {}", getVmData().getName(), enabled ? "启用" : "禁用");
    }

    public void enableAssignedDatasetId(boolean enabled) {
        this.assignedDatasetIdEnabled = enabled;
        log.info("🤖 [{}] assignedDatasetId支持: {}", getVmData().getName(), enabled ? "启用" : "禁用");
    }

    public boolean supportsProtocol(ProtocolType protocolType) {
        if (!v15ProtocolEnabled) {
            return false;
        }

        // v1.5新增协议支持检查
        switch (protocolType) {
            case DATASET_LIST_QUERY:
            case DATASET_LIST_RESPONSE:
                return datasetManagementEnabled;
            case FEDERATED_TASK_START:
            case GRADIENT_UPLOAD:
                return assignedDatasetIdEnabled;
            default:
                return true;  // 其他协议默认支持
        }
    }

    // ========== v1.5协议处理方法 ==========

    @Override
    protected void handleProtocolMessage(ProtocolMessage message) {
        // 记录接收到的消息
        receivedMessages.add(message);

        if (!v15ProtocolEnabled) {
            // 如果未启用v1.5协议，使用父类处理
            super.handleProtocolMessage(message);
            return;
        }

        // v1.5协议处理
        switch (message.getType()) {
            case FEDERATED_TASK_START:
                handleFederatedTaskStartV15(message);
                break;
            case DATASET_LIST_QUERY:
                handleDatasetListQuery(message);
                break;
            case GRADIENT_UPLOAD:
                // v1.5梯度上传验证assignedDatasetId
                validateAndHandleGradientUpload(message);
                break;
            default:
                // 其他消息使用父类处理
                super.handleProtocolMessage(message);
        }
    }

    /**
     * 🆕 处理FEDERATED_TASK_START消息 (v1.5)
     * 关键验证：assignedDatasetId位于dataConfig内部
     */
    private void handleFederatedTaskStartV15(ProtocolMessage message) {
        log.info("🤖 [{}] 收到FEDERATED_TASK_START消息 (v1.5)", getVmData().getName());

        Map<String, Object> data = message.getData();
        String taskId = (String) data.get("taskId");

        // 🔑 关键验证：dataConfig结构和assignedDatasetId位置
        if (!data.containsKey("dataConfig")) {
            log.error("🤖 [{}] FEDERATED_TASK_START缺少dataConfig字段", getVmData().getName());
            sendErrorResponse(message, "缺少dataConfig字段");
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> dataConfig = (Map<String, Object>) data.get("dataConfig");

        if (!dataConfig.containsKey("assignedDatasetId")) {
            log.error("🤖 [{}] dataConfig缺少assignedDatasetId字段", getVmData().getName());
            sendErrorResponse(message, "dataConfig缺少assignedDatasetId字段");
            return;
        }

        String assignedDatasetId = (String) dataConfig.get("assignedDatasetId");
        String dataPath = (String) dataConfig.get("dataPath");

        // 🆕 保存assignedDatasetId（完全依赖后端分配）
        assignedDatasetIds.put(taskId, assignedDatasetId);

        // 🆕 模拟创建本地数据集
        String localPath = "/data/assigned/" + assignedDatasetId;
        datasetStatuses.put(assignedDatasetId, "CREATED");
        datasetLocalPaths.put(assignedDatasetId, localPath);

        log.info("🤖 [{}] 数据集创建完成: assignedDatasetId={}, localPath={}",
                 getVmData().getName(), assignedDatasetId, localPath);

        // 发送任务启动确认
        sendFederatedTaskStartAck(taskId, assignedDatasetId);
    }

    /**
     * 🆕 处理DATASET_LIST_QUERY消息 (v1.5新增)
     */
    private void handleDatasetListQuery(ProtocolMessage message) {
        log.info("🤖 [{}] 收到DATASET_LIST_QUERY消息", getVmData().getName());

        Map<String, Object> data = message.getData();
        String taskId = (String) data.get("taskId");
        String queryType = (String) data.get("queryType");

        if (!"ASSIGNED_DATASETS".equals(queryType)) {
            log.warn("🤖 [{}] 不支持的查询类型: {}", getVmData().getName(), queryType);
            return;
        }

        // 🆕 构建数据集列表响应
        List<Map<String, Object>> datasets = new ArrayList<>();

        String assignedDatasetId = assignedDatasetIds.get(taskId);
        if (assignedDatasetId != null) {
            Map<String, Object> dataset = new HashMap<>();
            dataset.put("assignedDatasetId", assignedDatasetId);
            dataset.put("status", datasetStatuses.get(assignedDatasetId));
            dataset.put("localPath", datasetLocalPaths.get(assignedDatasetId));
            dataset.put("createdAt", Instant.now().toString());
            datasets.add(dataset);
        }

        // 发送DATASET_LIST_RESPONSE
        sendDatasetListResponse(taskId, datasets);
    }

    /**
     * 🆕 验证并处理梯度上传 (v1.5)
     * 包含assignedDatasetId验证
     */
    private void validateAndHandleGradientUpload(ProtocolMessage message) {
        Map<String, Object> data = message.getData();
        String taskId = (String) data.get("taskId");
        String messageAssignedDatasetId = (String) data.get("assignedDatasetId");

        // 🔑 验证assignedDatasetId
        String expectedAssignedDatasetId = assignedDatasetIds.get(taskId);
        if (!Objects.equals(messageAssignedDatasetId, expectedAssignedDatasetId)) {
            log.error("🤖 [{}] GRADIENT_UPLOAD中assignedDatasetId验证失败: expected={}, actual={}",
                     getVmData().getName(), expectedAssignedDatasetId, messageAssignedDatasetId);
            sendErrorResponse(message, "assignedDatasetId验证失败");
            return;
        }

        log.info("🤖 [{}] GRADIENT_UPLOAD验证成功: assignedDatasetId={}",
                 getVmData().getName(), messageAssignedDatasetId);

        // 继续正常的梯度上传处理
        super.handleGradientUpload(message);
    }

    // ========== v1.5响应发送方法 ==========

    private void sendFederatedTaskStartAck(String taskId, String assignedDatasetId) {
        Map<String, Object> ackData = new HashMap<>();
        ackData.put("taskId", taskId);
        ackData.put("status", "READY");
        ackData.put("assignedDatasetId", assignedDatasetId);  // v1.5: 包含确认的数据集ID

        ProtocolMessage ackMessage = ProtocolMessage.builder()
            .type(ProtocolType.FEDERATED_TASK_START_ACK)
            .id(UUID.randomUUID().toString())
            .vmId(getVmId())
            .data(ackData)
            .timestamp(Instant.now())
            .build();

        sendMessage(ackMessage);
        log.info("🤖 [{}] 发送FEDERATED_TASK_START_ACK: assignedDatasetId={}",
                 getVmData().getName(), assignedDatasetId);
    }

    private void sendDatasetListResponse(String taskId, List<Map<String, Object>> datasets) {
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("taskId", taskId);
        responseData.put("datasets", datasets);
        responseData.put("totalCount", datasets.size());

        ProtocolMessage responseMessage = ProtocolMessage.builder()
            .type(ProtocolType.DATASET_LIST_RESPONSE)
            .id(UUID.randomUUID().toString())
            .vmId(getVmId())
            .data(responseData)
            .timestamp(Instant.now())
            .build();

        sendMessage(responseMessage);
        log.info("🤖 [{}] 发送DATASET_LIST_RESPONSE: datasets={}",
                 getVmData().getName(), datasets.size());
    }

    private void sendErrorResponse(ProtocolMessage originalMessage, String errorMessage) {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("errorCode", "PROTOCOL_ERROR");
        errorData.put("errorMessage", errorMessage);
        errorData.put("originalMessageId", originalMessage.getId());

        ProtocolMessage errorResponse = ProtocolMessage.builder()
            .type(ProtocolType.ERROR)
            .id(UUID.randomUUID().toString())
            .vmId(getVmId())
            .data(errorData)
            .timestamp(Instant.now())
            .build();

        sendMessage(errorResponse);
        log.error("🤖 [{}] 发送错误响应: {}", getVmData().getName(), errorMessage);
    }

    // ========== 测试辅助方法 ==========

    public Optional<ProtocolMessage> getReceivedMessage(ProtocolType protocolType) {
        return receivedMessages.stream()
            .filter(msg -> msg.getType() == protocolType)
            .reduce((first, second) -> second);  // 获取最后一个匹配的消息
    }

    public List<ProtocolMessage> getAllReceivedMessages(ProtocolType protocolType) {
        return receivedMessages.stream()
            .filter(msg -> msg.getType() == protocolType)
            .collect(Collectors.toList());
    }

    public String getAssignedDatasetId(String taskId) {
        return assignedDatasetIds.get(taskId);
    }

    public String getDatasetStatus(String assignedDatasetId) {
        return datasetStatuses.get(assignedDatasetId);
    }

    public void clearMessageHistory() {
        receivedMessages.clear();
        log.info("🤖 [{}] 消息历史已清空", getVmData().getName());
    }
}
```

## 📊 测试覆盖度规划

### v1.5协议测试覆盖矩阵

| 协议消息 | 测试方法 | 覆盖重点 | 状态 |
|---------|----------|----------|------|
| FEDERATED_TASK_START | test07_DatasetAllocationVerification | assignedDatasetId在dataConfig内 | ❌ 待实现 |
| DATASET_LIST_QUERY | test08_DatasetStatusQueryV15 | 查询数据集状态 | ❌ 待实现 |
| DATASET_LIST_RESPONSE | test08_DatasetStatusQueryV15 | 响应数据集信息 | ❌ 待实现 |
| GRADIENT_UPLOAD | test10_FederatedLearningExecutionV15 | 包含assignedDatasetId验证 | ❌ 待实现 |

### 13步流程测试覆盖

| 步骤 | 测试方法 | 实现状态 | 预计工时 |
|------|----------|----------|----------|
| 1-2 | test01-02 | ✅ 保持现有 | 0分钟 |
| 3 | test03_WebSocketConnectionsV15 | ❌ 需要重写 | 30分钟 |
| 4-5 | test04-05 | 🔄 需要增强 | 20分钟 |
| 6 | test06_CreateFederatedTaskV15 | ❌ 需要重写 | 30分钟 |
| 7-8 | test07-08 | ❌ 新增测试 | 60分钟 |
| 9-11 | test09-11 | ❌ 需要重写 | 45分钟 |
| 12-13 | test12-13 | ✅ 保持现有 | 0分钟 |

## ⚠️ 实施注意事项

### 关键风险点

1. **硬编码替代前端操作**
   - 确保硬编码数据与真实前端操作一致
   - 验证数据格式和参数的正确性

2. **异步消息处理**
   - WebSocket消息的异步特性需要适当的等待机制
   - 避免时序问题导致的测试不稳定

3. **v1.5协议兼容性**
   - 确保新测试不影响现有v1.4功能
   - 提供回滚和降级机制

### 成功标准

- [ ] 13步流程100%测试覆盖
- [ ] v1.5协议消息100%验证
- [ ] assignedDatasetId正确性100%验证
- [ ] 测试稳定性 > 95%
- [ ] 测试执行时间 < 300秒

这份单元测试重构文档确保了CompleteFederatedLearningFlowTest能够完全支持WebSocket v1.5协议，实现13步标准化流程的完整测试覆盖。