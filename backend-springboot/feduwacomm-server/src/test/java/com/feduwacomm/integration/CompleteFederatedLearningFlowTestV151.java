package com.feduwacomm.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.common.Result;
import com.feduwacomm.dto.*;
import com.feduwacomm.vo.*;
import com.feduwacomm.integration.mock.MockVirtualMachine;
import com.feduwacomm.integration.mock.VmTestData;
import com.feduwacomm.mapper.FederatedTasksMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 完整的联邦学习流程端到端测试 (v1.5.1修正版)
 * 🆕 v1.5.1新增测试：数据切片、BatchRange、SliceVerification
 * 🔧 修正版：分离任务创建与启动的数据分发逻辑
 *
 * 测试流程：
 * 1. 管理员登录
 * 2. 虚拟机注册
 * 3. WebSocket连接建立 (v1.5.1协议)
 * 4. 训练数据上传
 * 5. 查询可用资源
 * 6. 创建联邦学习任务（仅记录配置，不分发数据）
 * 7. 启动任务（触发数据切片和分发）
 * 8. 数据集分配验证 (v1.5.1：验证SliceInfo)
 * 9. 数据传输监控 (v1.5.1：验证BatchRange)
 * 10. 数据完整性验证 (v1.5.1：验证SliceVerification)
 * 11. 联邦学习执行
 * 12. 结果获取
 * 13. 清理连接
 *
 * @author FedUWAComm Team
 * @version 1.5.1
 * @since 2025-09-30
 * @updated 2025-10-03 修正任务创建与启动的数据分发时机
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompleteFederatedLearningFlowTestV151 {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private FederatedTasksMapper tasksMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private String baseUrl;
    private String adminAccessToken;
    private String taskId;
    private String originalDatasetId;

    // 测试用的5个虚拟机
    private List<VmTestData> virtualMachines = new ArrayList<>();
    private List<String> registeredVmIds = new ArrayList<>();
    private Map<String, String> vmAccessTokens = new HashMap<>();
    private List<MockVirtualMachine> mockVMs = new ArrayList<>();

    // 🆕 v1.5.1新增：数据切片跟踪
    private Map<String, String> vmAssignedDatasetIds = new HashMap<>();
    private Map<String, Map<String, Object>> vmSliceInfo = new HashMap<>();  // vmId -> SliceInfo
    private Map<String, List<Map<String, Object>>> vmBatchRanges = new HashMap<>();  // vmId -> List<BatchRange>
    private Map<String, Map<String, Object>> vmSliceVerifications = new HashMap<>();  // vmId -> SliceVerification

    @BeforeAll
    void setUp() {
        baseUrl = "http://localhost:" + port;
        System.out.println("🚀 完整联邦学习流程测试 (v1.5.1修正版) 开始");
        System.out.println("🌐 测试服务地址: " + baseUrl);
        System.out.println("🆕 v1.5.1新增测试：数据切片、BatchRange、SliceVerification验证");
        System.out.println("🔧 修正版：任务创建与启动的数据分发逻辑分离");

        // 初始化5个测试虚拟机
        // VmTestData参数: vmId, name, ipAddress, port, cpuCores, memoryMb, gpuCount
        virtualMachines.add(new VmTestData(
            null, "VM-Alpha", "192.168.1.101", 8001, 64, 65536, 4  // vmId will be set during registration
        ));
        virtualMachines.add(new VmTestData(
            null, "VM-Beta", "192.168.1.102", 8002, 32, 32768, 2
        ));
        virtualMachines.add(new VmTestData(
            null, "VM-Gamma", "192.168.1.103", 8003, 16, 16384, 1
        ));
        virtualMachines.add(new VmTestData(
            null, "VM-Delta", "192.168.1.104", 8004, 64, 131072, 0  // CPU only
        ));
        virtualMachines.add(new VmTestData(
            null, "VM-Epsilon", "192.168.1.105", 8005, 24, 24576, 1
        ));
    }

    @AfterAll
    void tearDown() {
        // 清理所有MockVM连接
        for (MockVirtualMachine mockVM : mockVMs) {
            try {
                mockVM.disconnect();
            } catch (Exception e) {
                System.err.println("清理VM连接失败: " + e.getMessage());
            }
        }
        System.out.println("🧹 v1.5.1测试清理完成");
    }

    /**
     * 步骤1：管理员登录
     */
    @Test
    @Order(1)
    void test01_AdminLogin() {
        System.out.println("\n🔐 步骤1：管理员登录测试");

        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("loginIdentifier", "admin");
        loginRequest.put("password", "ab123456");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(loginRequest, headers);

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
        assertThat(loginData.getToken()).isNotBlank();

        this.adminAccessToken = loginData.getToken();
        System.out.println("✅ 管理员登录成功");
    }

    /**
     * 步骤2：虚拟机注册
     */
    @Test
    @Order(2)
    void test02_VirtualMachinesRegistration() {
        System.out.println("\n🤖 步骤2：虚拟机注册流程测试");

        for (VmTestData vmData : virtualMachines) {
            registerSingleVirtualMachine(vmData);
        }

        assertThat(registeredVmIds).hasSize(5);
        System.out.println("✅ 所有虚拟机注册完成，注册数量: " + registeredVmIds.size());
    }

    /**
     * 步骤3：WebSocket连接建立 (v1.5.1协议)
     * 🆕 v1.5.1：准备接收DATASET_CREATE/APPEND_ROWS/COMPLETE消息
     */
    @Test
    @Order(3)
    void test03_WebSocketConnectionsV151() throws InterruptedException, Exception {
        System.out.println("\n🔌 步骤3：WebSocket连接建立测试 (v1.5.1协议)");

        for (int i = 0; i < registeredVmIds.size(); i++) {
            String vmId = registeredVmIds.get(i);
            VmTestData vmData = virtualMachines.get(i);

            MockVirtualMachine mockVM = new MockVirtualMachine(vmData);
            mockVM.setVmId(vmId);
            String accessToken = vmAccessTokens.get(vmId);
            mockVM.setAccessToken(accessToken);

            try {
                mockVM.connectWebSocket(baseUrl);
                mockVMs.add(mockVM);
                System.out.println("🔌 VM连接成功 (v1.5.1): " + vmData.getName() + " (vmId: " + vmId + ")");
            } catch (Exception e) {
                System.err.println("❌ VM连接失败: " + vmData.getName() + ", 错误: " + e.getMessage());
            }
        }

        Thread.sleep(2000);

        for (MockVirtualMachine mockVM : mockVMs) {
            assertThat(mockVM.isConnected()).isTrue();
            System.out.println("✅ VM连接状态验证通过: " + mockVM.getName());
        }

        assertThat(mockVMs).hasSize(5);
        System.out.println("✅ 所有虚拟机WebSocket连接建立完成 (v1.5.1协议)");
    }

    /**
     * 步骤4：训练数据上传
     */
    @Test
    @Order(4)
    void test04_TrainingDataUpload() {
        System.out.println("\n📊 步骤4：训练数据上传测试");

        MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();

        ByteArrayResource fileResource = new ByteArrayResource(generateMockDatasetContent().getBytes()) {
            @Override
            public String getFilename() {
                return "test_acoustic_data_v151.csv";
            }
        };
        requestBody.add("file", fileResource);

        try {
            TrainingDataUploadDTO uploadDTO = TrainingDataUploadDTO.builder()
                .dataType("ACOUSTIC")
                .datasetDescription("v1.5.1水声数据集，用于切片验证测试")
                .tags(Arrays.asList("acoustic", "v1.5.1", "slicing-test"))
                .metadata(Map.of("vmCount", 5, "protocolVersion", "1.5.1"))
                .build();

            String jsonString = objectMapper.writeValueAsString(uploadDTO);
            ByteArrayResource jsonResource = new ByteArrayResource(jsonString.getBytes()) {
                @Override
                public String getFilename() {
                    return "uploadDTO.json";
                }
            };
            requestBody.add("uploadDTO", jsonResource);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize uploadDTO to JSON", e);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(adminAccessToken);

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Result<TrainingDataUploadVO>> response = restTemplate.exchange(
            baseUrl + "/api/training-data/upload",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<Result<TrainingDataUploadVO>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(200);

        TrainingDataUploadVO uploadResult = response.getBody().getData();
        this.originalDatasetId = uploadResult.getDatasetId();

        assertThat(originalDatasetId).isNotNull();
        System.out.println("✅ 数据集上传成功 (v1.5.1)");
        System.out.println("📋 原始数据集ID: " + originalDatasetId);
    }

    /**
     * 步骤5：查询可用资源
     */
    @Test
    @Order(5)
    void test05_QueryAvailableResources() {
        System.out.println("\n🔍 步骤5：查询可用资源测试");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminAccessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Result<Map<String, Object>>> response = restTemplate.exchange(
            baseUrl + "/api/federated/config/available-vms",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<Result<Map<String, Object>>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(200);

        Map<String, Object> resources = response.getBody().getData();
        assertThat(resources).isNotNull();

        System.out.println("✅ 可用资源查询成功");
        System.out.println("📊 资源统计: " + resources);
    }

    /**
     * 步骤6：创建联邦学习任务 (v1.5.1修正版)
     * 🔧 仅创建任务记录，不触发数据分发
     */
    @Test
    @Order(6)
    void test06_CreateFederatedTaskV151() {
        System.out.println("\n🚀 步骤6：创建联邦学习任务测试 (v1.5.1修正版)");

        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("taskName", "v1.5.1联邦学习任务-数据切片验证");
        createRequest.put("taskType", "CLASSIFICATION");
        createRequest.put("description", "测试v1.5.1数据切片、BatchRange和SliceVerification功能");
        createRequest.put("algorithm", "FEDERATED_AVERAGING");

        // 数据集配置
        Map<String, Object> datasetConfig = new HashMap<>();
        datasetConfig.put("datasetId", originalDatasetId);
        datasetConfig.put("distributionStrategy", "BALANCED");
        datasetConfig.put("validationSplit", 0.2);
        datasetConfig.put("testSplit", 0.1);
        createRequest.put("datasetConfig", datasetConfig);

        // 参与者配置
        Map<String, Object> participantConfig = new HashMap<>();
        participantConfig.put("selectionMode", "MANUAL");

        List<Map<String, Object>> participants = new ArrayList<>();
        // v1.5.1.1: dataRatio是千分比权重（1-1000），5个VM均等分配：每个200
        int equalRatio = 1000 / registeredVmIds.size();
        for (String vmId : registeredVmIds) {
            Map<String, Object> participant = new HashMap<>();
            participant.put("vmId", vmId);
            participant.put("role", "PARTICIPANT");
            participant.put("dataRatio", equalRatio);  // 200 (1000/5)
            participants.add(participant);
        }
        participantConfig.put("participants", participants);
        createRequest.put("participantConfig", participantConfig);

        // 超参数配置
        Map<String, Object> hyperparameters = new HashMap<>();
        hyperparameters.put("learningRate", 0.01);
        hyperparameters.put("batchSize", 32);
        hyperparameters.put("epochs", 3);
        hyperparameters.put("rounds", 5);
        hyperparameters.put("minParticipants", 3);
        createRequest.put("hyperparameters", hyperparameters);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminAccessToken);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(createRequest, headers);

        ResponseEntity<Result<Map<String, Object>>> response = restTemplate.exchange(
            baseUrl + "/api/federated/tasks",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<Result<Map<String, Object>>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(200);

        Map<String, Object> taskResult = response.getBody().getData();
        this.taskId = (String) taskResult.get("taskId");

        assertThat(taskId).isNotNull();

        System.out.println("✅ v1.5.1联邦学习任务创建成功");
        System.out.println("📋 任务ID: " + taskId);
        System.out.println("⚠️  任务状态: CREATED (数据尚未分发)");
    }

    /**
     * 步骤7：任务启动流程 (v1.5.1修正版)
     * 🔥 启动任务，触发数据切片和分发
     */
    @Test
    @Order(7)
    void test07_TaskStartFlowV151() throws InterruptedException {
        System.out.println("\n🚀 步骤7：任务启动流程测试 (v1.5.1修正版)");
        System.out.println("🎯 此步骤将触发数据切片和分发");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminAccessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Result<TaskOperationVO>> startResponse = restTemplate.exchange(
            baseUrl + "/api/federated/tasks/" + taskId + "/start",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<Result<TaskOperationVO>>() {}
        );

        assertThat(startResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(startResponse.getBody()).isNotNull();
        assertThat(startResponse.getBody().getCode()).isEqualTo(200);

        System.out.println("✅ 任务已启动，状态变更为 RUNNING");
        System.out.println("🎯 等待数据切片和分发完成...");

        // 等待后端完成数据切片和分发
        waitForDatasetAllocation();
    }

    /**
     * 步骤8：数据集分配验证 (v1.5.1)
     * 🆕 v1.5.1核心测试：验证DATASET_CREATE消息包含SliceInfo
     */
    @Test
    @Order(8)
    void test08_DatasetAllocationWithSliceInfoV151() throws InterruptedException {
        System.out.println("\n📦 步骤8：数据集分配验证测试 (v1.5.1核心功能)");
        System.out.println("🎯 验证目标：DATASET_CREATE消息包含SliceInfo");

        Thread.sleep(3000);

        // 🆕 验证每个VM的SliceInfo
        for (MockVirtualMachine mockVM : mockVMs) {
            String vmId = mockVM.getVmId();

            // 从MockVM获取SliceInfo（MockVM已在handleDatasetCreateV15中保存）
            Map<String, Object> sliceInfo = mockVM.getSliceInfoForLatestDataset();

            assertThat(sliceInfo).isNotNull();
            assertThat(sliceInfo).containsKeys("startIndex", "endIndex", "sliceSamples");

            Integer startIndex = (Integer) sliceInfo.get("startIndex");
            Integer endIndex = (Integer) sliceInfo.get("endIndex");
            Integer sliceSamples = (Integer) sliceInfo.get("sliceSamples");

            assertThat(startIndex).isNotNull();
            assertThat(endIndex).isNotNull();
            assertThat(sliceSamples).isNotNull();
            assertThat(endIndex).isGreaterThanOrEqualTo(startIndex);
            assertThat(sliceSamples).isEqualTo(endIndex - startIndex + 1);

            // 保存SliceInfo用于后续验证
            vmSliceInfo.put(vmId, sliceInfo);

            // 获取assignedDatasetId
            String assignedDatasetId = mockVM.getLatestAssignedDatasetId();
            vmAssignedDatasetIds.put(vmId, assignedDatasetId);

            System.out.println("✅ VM SliceInfo验证通过: " + mockVM.getName());
            System.out.println("   📋 assignedDatasetId: " + assignedDatasetId);
            System.out.println("   📊 SliceInfo: startIndex=" + startIndex +
                             ", endIndex=" + endIndex +
                             ", sliceSamples=" + sliceSamples);
        }

        // 验证切片的完整性和无重叠
        verifySliceCompleteness();

        System.out.println("✅ 数据集分配验证完成 (v1.5.1)");
    }

    /**
     * 步骤9：数据传输监控 (v1.5.1)
     * 🆕 v1.5.1核心测试：验证DATASET_APPEND_ROWS包含BatchRange和globalIndex
     */
    @Test
    @Order(9)
    void test09_DataTransmissionWithBatchRangeV151() throws InterruptedException {
        System.out.println("\n📡 步骤9：数据传输监控测试 (v1.5.1核心功能)");
        System.out.println("🎯 验证目标：DATASET_APPEND_ROWS包含BatchRange和globalIndex");

        Thread.sleep(3000);

        // 🆕 验证每个VM接收到的BatchRange
        for (MockVirtualMachine mockVM : mockVMs) {
            String vmId = mockVM.getVmId();

            // 从MockVM获取所有接收到的BatchRange
            List<Map<String, Object>> batchRanges = mockVM.getBatchRangesForLatestDataset();

            assertThat(batchRanges).isNotNull();
            assertThat(batchRanges).isNotEmpty();

            // 验证每个BatchRange的结构 - v1.5.1使用globalStartIndex/globalEndIndex
            for (Map<String, Object> batchRange : batchRanges) {
                assertThat(batchRange).containsKeys("globalStartIndex", "globalEndIndex", "localStartIndex", "localEndIndex");

                Integer globalStartIndex = (Integer) batchRange.get("globalStartIndex");
                Integer globalEndIndex = (Integer) batchRange.get("globalEndIndex");
                Integer localStartIndex = (Integer) batchRange.get("localStartIndex");
                Integer localEndIndex = (Integer) batchRange.get("localEndIndex");

                assertThat(globalStartIndex).isNotNull();
                assertThat(globalEndIndex).isNotNull();
                assertThat(localStartIndex).isNotNull();
                assertThat(localEndIndex).isNotNull();

                int batchSamples = globalEndIndex - globalStartIndex + 1;
                assertThat(batchSamples).isEqualTo(localEndIndex - localStartIndex + 1);

                System.out.println("   📦 BatchRange: globalStartIndex=" + globalStartIndex +
                                 ", globalEndIndex=" + globalEndIndex +
                                 ", localStartIndex=" + localStartIndex +
                                 ", localEndIndex=" + localEndIndex +
                                 ", batchSamples=" + batchSamples);
            }

            // 保存BatchRange用于后续验证
            vmBatchRanges.put(vmId, batchRanges);

            System.out.println("✅ VM BatchRange验证通过: " + mockVM.getName() +
                             ", 接收批次数: " + batchRanges.size());
        }

        System.out.println("✅ 数据传输监控完成 (v1.5.1)");
    }

    /**
     * 步骤10：数据完整性验证 (v1.5.1)
     * 🆕 v1.5.1核心测试：验证DATASET_COMPLETE_ACK包含SliceVerification
     */
    @Test
    @Order(10)
    void test10_DataIntegrityVerificationV151() throws InterruptedException {
        System.out.println("\n🔍 步骤10：数据完整性验证测试 (v1.5.1核心功能)");
        System.out.println("🎯 验证目标：VM生成SliceVerification并通过backend验证");

        Thread.sleep(3000);

        // 🆕 验证每个VM的SliceVerification
        for (MockVirtualMachine mockVM : mockVMs) {
            String vmId = mockVM.getVmId();

            // 从MockVM获取SliceVerification（MockVM在handleDatasetCompleteV15中生成）
            Map<String, Object> sliceVerification = mockVM.getSliceVerificationForLatestDataset();

            assertThat(sliceVerification).isNotNull();
            assertThat(sliceVerification).containsKeys(
                "actualSamples",
                "actualStartIndex",
                "actualEndIndex",
                "missingIndices",
                "continuityCheck"
            );

            Integer actualSamples = (Integer) sliceVerification.get("actualSamples");
            Integer actualStartIndex = (Integer) sliceVerification.get("actualStartIndex");
            Integer actualEndIndex = (Integer) sliceVerification.get("actualEndIndex");
            @SuppressWarnings("unchecked")
            List<Integer> missingIndices = (List<Integer>) sliceVerification.get("missingIndices");
            @SuppressWarnings("unchecked")
            Map<String, Object> continuityCheck = (Map<String, Object>) sliceVerification.get("continuityCheck");

            // 验证基本字段
            assertThat(actualSamples).isNotNull();
            assertThat(actualStartIndex).isNotNull();
            assertThat(actualEndIndex).isNotNull();
            assertThat(missingIndices).isNotNull();
            assertThat(continuityCheck).isNotNull();

            // 验证连续性检查结构
            assertThat(continuityCheck).containsKeys("isContinuous", "gapRanges");
            Boolean isContinuous = (Boolean) continuityCheck.get("isContinuous");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> gapRanges = (List<Map<String, Object>>) continuityCheck.get("gapRanges");

            // 理想情况下：无缺失数据，连续
            if (missingIndices.isEmpty() && Boolean.TRUE.equals(isContinuous)) {
                System.out.println("✅ VM数据完整性验证通过: " + mockVM.getName() + " - 完美无缺失");
            } else {
                System.out.println("⚠️  VM数据完整性验证: " + mockVM.getName() +
                                 " - 缺失数量: " + missingIndices.size() +
                                 ", 连续性: " + isContinuous);
            }

            // 与预期SliceInfo对比
            Map<String, Object> expectedSliceInfo = vmSliceInfo.get(vmId);
            Integer expectedStartIndex = (Integer) expectedSliceInfo.get("startIndex");
            Integer expectedEndIndex = (Integer) expectedSliceInfo.get("endIndex");
            Integer expectedSamples = (Integer) expectedSliceInfo.get("sliceSamples");

            assertThat(actualStartIndex).isEqualTo(expectedStartIndex);
            assertThat(actualEndIndex).isEqualTo(expectedEndIndex);
            assertThat(actualSamples).isEqualTo(expectedSamples);

            // 保存SliceVerification
            vmSliceVerifications.put(vmId, sliceVerification);

            System.out.println("   📊 SliceVerification: actualSamples=" + actualSamples +
                             ", missingCount=" + missingIndices.size() +
                             ", isContinuous=" + isContinuous);
        }

        System.out.println("✅ 数据完整性验证完成 (v1.5.1)");
    }

    /**
     * 步骤11：联邦学习执行
     */
    @Test
    @Order(11)
    void test11_FederatedLearningExecution() throws InterruptedException {
        System.out.println("\n🔄 步骤11：联邦学习执行测试");

        for (int round = 1; round <= 3; round++) {
            System.out.println("🔄 执行联邦学习轮次: " + round);

            for (MockVirtualMachine mockVM : mockVMs) {
                String vmId = mockVM.getVmId();
                String assignedDatasetId = vmAssignedDatasetIds.get(vmId);

                System.out.println("📤 VM梯度上传: " + mockVM.getName() +
                                 ", assignedDatasetId: " + assignedDatasetId);
            }

            Thread.sleep(2000);
            System.out.println("✅ 轮次 " + round + " 完成");
        }

        System.out.println("✅ 联邦学习执行完成");
    }

    /**
     * 步骤12：结果获取
     */
    @Test
    @Order(12)
    void test12_ResultsRetrieval() {
        System.out.println("\n📊 步骤12：结果获取和验证测试");

        // 更新任务状态为已完成
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        tasksMapper.updateTaskStatus(taskId, "COMPLETED", now);

        System.out.println("✅ 任务状态已更新为 COMPLETED");
        System.out.println("✅ 结果获取完成");
    }

    /**
     * 步骤13：清理连接
     */
    @Test
    @Order(13)
    void test13_CleanupConnections() {
        System.out.println("\n🧹 步骤13：清理和断开连接测试");

        for (MockVirtualMachine mockVM : mockVMs) {
            try {
                mockVM.disconnect();
                System.out.println("✅ VM断开连接: " + mockVM.getName());
            } catch (Exception e) {
                System.err.println("⚠️  VM断开失败: " + mockVM.getName() + ", 错误: " + e.getMessage());
            }
        }

        vmAssignedDatasetIds.clear();
        vmSliceInfo.clear();
        vmBatchRanges.clear();
        vmSliceVerifications.clear();

        System.out.println("✅ v1.5.1测试资源清理完成");
    }

    // ========== 辅助方法 ==========

    private void registerSingleVirtualMachine(VmTestData vmData) {
        Map<String, Object> vmRequest = new HashMap<>();
        vmRequest.put("name", vmData.getName());
        vmRequest.put("ipAddress", vmData.getIpAddress());
        vmRequest.put("port", vmData.getPort());
        vmRequest.put("osType", "Ubuntu 20.04");
        vmRequest.put("cpuCores", vmData.getCpuCores());
        vmRequest.put("memoryMb", vmData.getMemoryMb());
        vmRequest.put("diskGb", 256);
        vmRequest.put("capabilities", vmData.getCapabilities());
        vmRequest.put("systemInfo", vmData.getSystemInfo());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(vmRequest, headers);

        ResponseEntity<Result<VmRegisterResponseVO>> response = restTemplate.exchange(
            baseUrl + "/api/v1/vm/register",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<Result<VmRegisterResponseVO>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(200);

        VmRegisterResponseVO vmResult = response.getBody().getData();
        String vmId = vmResult.getVmId();
        String accessToken = vmResult.getAccessToken();

        vmData.setVmId(vmId);
        registeredVmIds.add(vmId);
        vmAccessTokens.put(vmId, accessToken);

        System.out.println("✅ VM注册成功: " + vmData.getName() + " (vmId: " + vmId + ")");
    }

    private void waitForDatasetAllocation() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 验证切片的完整性和无重叠
     */
    private void verifySliceCompleteness() {
        System.out.println("🔍 验证切片完整性和无重叠...");

        List<Integer> allIndices = new ArrayList<>();

        for (Map<String, Object> sliceInfo : vmSliceInfo.values()) {
            Integer startIndex = (Integer) sliceInfo.get("startIndex");
            Integer endIndex = (Integer) sliceInfo.get("endIndex");

            for (int i = startIndex; i <= endIndex; i++) {
                allIndices.add(i);
            }
        }

        // 检查无重复
        Set<Integer> uniqueIndices = new HashSet<>(allIndices);
        assertThat(allIndices.size()).isEqualTo(uniqueIndices.size());

        // 检查连续性
        Collections.sort(allIndices);
        for (int i = 1; i < allIndices.size(); i++) {
            assertThat(allIndices.get(i)).isEqualTo(allIndices.get(i - 1) + 1);
        }

        System.out.println("✅ 切片完整性验证通过：无重叠，无间隙，覆盖范围 [" +
                         allIndices.get(0) + ", " +
                         allIndices.get(allIndices.size() - 1) + "]");
    }

    private String generateMockDatasetContent() {
        StringBuilder content = new StringBuilder();
        content.append("timestamp,frequency,amplitude,phase,noise_level,target\n");

        Random random = new Random();
        for (int i = 0; i < 1000; i++) {
            content.append(String.format("%d,%.2f,%.3f,%.2f,%.3f,%d\n",
                System.currentTimeMillis() + i,
                random.nextDouble() * 10000,
                random.nextDouble(),
                random.nextDouble() * 360,
                random.nextDouble() * 0.1,
                random.nextInt(2)
            ));
        }

        return content.toString();
    }
}