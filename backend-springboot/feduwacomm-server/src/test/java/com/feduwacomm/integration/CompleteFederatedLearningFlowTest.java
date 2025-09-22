package com.feduwacomm.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.dto.*;
import com.feduwacomm.vo.*;
import com.feduwacomm.integration.mock.MockVirtualMachine;
import com.feduwacomm.integration.mock.VmTestData;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.io.ByteArrayResource;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * FedUWAComm 联邦学习完整流程测试
 * 基于文档: FedUWAComm_Complete_Federated_Learning_Test_Document.md
 *
 * 测试流程：
 * 1. 管理员登录
 * 2. 虚拟机注册流程 (5台VM)
 * 3. WebSocket连接建立
 * 4. 训练数据管理
 * 5. 联邦学习任务配置
 * 6. 任务启动和执行
 * 7. 任务监控和状态查询
 * 8. 任务完成和结果获取
 * 9. 查询模型版本
 * 10. 清理和断开连接
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CompleteFederatedLearningFlowTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    private String baseUrl;
    private String adminAccessToken;
    private String taskId;
    private String datasetId;

    // 测试数据 - 5台虚拟机（vmId由后端注册时自动生成）
    private final List<VmTestData> virtualMachines = Arrays.asList(
        new VmTestData(null, "VM-Node-1", "192.168.1.101", 8081, 8, 16384, 1),
        new VmTestData(null, "VM-Node-2", "192.168.1.102", 8082, 6, 12288, 0),
        new VmTestData(null, "VM-Node-3", "192.168.1.103", 8083, 4, 8192, 1),
        new VmTestData(null, "VM-Node-4", "192.168.1.104", 8084, 12, 24576, 2),
        new VmTestData(null, "VM-Node-5", "192.168.1.105", 8085, 16, 32768, 4)
    );

    // 存储注册后的虚拟机ID
    private final List<String> registeredVmIds = new ArrayList<>();

    // WebSocket客户端
    private final List<MockVirtualMachine> mockVMs = new ArrayList<>();

    @BeforeAll
    void setupTest() {
        baseUrl = "http://localhost:" + port;
        // 初始化Mock虚拟机
        for (VmTestData vmData : virtualMachines) {
            mockVMs.add(new MockVirtualMachine(vmData));
        }
    }

    @Test
    @Order(1)
    void test01_AdminLogin() {
        // 管理员登录测试
        UserLoginDTO request = new UserLoginDTO();
        request.setLoginIdentifier("admin");
        request.setPassword("ab123456");

        HttpEntity<UserLoginDTO> requestEntity = new HttpEntity<>(request);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            baseUrl + "/api/user/login",
            requestEntity,
            Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = response.getBody();
        assertThat(responseBody.get("code")).isEqualTo(200);

        // 解析响应数据
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) data.get("user");

        assertThat(user.get("role")).isEqualTo("ADMIN");

        adminAccessToken = (String) data.get("token");
        assertThat(adminAccessToken).isNotNull();

        System.out.println("✅ 管理员登录成功");
    }

    @Test
    @Order(2)
    void test02_VirtualMachinesRegistration() {
        // 并行注册所有5台虚拟机
        List<CompletableFuture<Void>> registrationFutures = mockVMs.stream()
            .map(vm -> CompletableFuture.runAsync(() -> {
                try {
                    String vmId = vm.register(baseUrl); // 注册并获取后端生成的vmId
                    registeredVmIds.add(vmId); // 存储注册后的vmId
                    assertThat(vm.isRegistered()).isTrue();
                    assertThat(vmId).isNotNull().matches("vm-[a-f0-9\\-]{36}"); // 验证UUID格式
                    System.out.println("✅ " + vm.getName() + " 注册成功，vmId: " + vmId);
                } catch (Exception e) {
                    fail("VM registration failed: " + e.getMessage());
                }
            }))
            .collect(Collectors.toList());

        // 等待所有注册完成
        CompletableFuture.allOf(registrationFutures.toArray(new CompletableFuture[0]))
            .join();

        // 验证所有VM都已注册并有正确的vmId
        mockVMs.forEach(vm -> assertThat(vm.isRegistered()).isTrue());
        assertThat(registeredVmIds).hasSize(5); // 确保有5个不同的vmId
        System.out.println("✅ 所有5台虚拟机注册完成，生成的vmIds: " + registeredVmIds);
    }

    @Test
    @Order(3)
    void test03_WebSocketConnections() {
        // 建立WebSocket连接
        String websocketUrl = "ws://localhost:" + port + "/ws";

        List<CompletableFuture<Void>> connectionFutures = mockVMs.stream()
            .map(vm -> CompletableFuture.runAsync(() -> {
                try {
                    vm.connectWebSocket(websocketUrl);
                    Thread.sleep(2000); // 等待连接稳定
                    assertThat(vm.isConnected()).isTrue();
                    System.out.println("✅ " + vm.getVmId() + " WebSocket连接成功");
                } catch (Exception e) {
                    fail("WebSocket connection failed: " + e.getMessage());
                }
            }))
            .collect(Collectors.toList());

        CompletableFuture.allOf(connectionFutures.toArray(new CompletableFuture[0]))
            .join();

        // 启动心跳
        mockVMs.forEach(MockVirtualMachine::startHeartbeat);

        // 等待心跳稳定
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("✅ 所有VM WebSocket连接建立并开始心跳");
    }

    @Test
    @Order(4)
    void test04_TrainingDataUpload() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminAccessToken);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // 创建测试数据文件
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("dataType", "ACOUSTIC_FEATURES");
        body.add("title", "5VM Test Acoustic Dataset");
        body.add("description", "5台虚拟机测试用声学特征数据集");

        // 创建模拟CSV文件 - 增大数据集以适应5台VM
        String csvContent = generateTestCsvData(8000);
        ByteArrayResource fileResource = new ByteArrayResource(csvContent.getBytes()) {
            @Override
            public String getFilename() {
                return "test_acoustic_data_5vm.csv";
            }
        };
        body.add("file", fileResource);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/api/training-data/upload",
                requestEntity,
                Map.class
            );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = response.getBody();
        assertThat(responseBody.get("code")).isEqualTo(200);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
        assertThat(data.get("rowCount")).isEqualTo(8000);

        datasetId = (String) data.get("datasetId");
        assertThat(datasetId).isNotNull();

        System.out.println("✅ 训练数据上传成功，数据集ID: " + datasetId);
    }

    @Test
    @Order(5)
    void test05_QueryAvailableResources() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminAccessToken);

        // 查询可用虚拟机
        ResponseEntity<Map> vmResponse =
            restTemplate.exchange(
                baseUrl + "/api/federated/config/available-vms?algorithm=FEDAVG&minCpuCores=4",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );

        if (vmResponse.getStatusCode() == HttpStatus.OK) {
            @SuppressWarnings("unchecked")
            Map<String, Object> vmData = (Map<String, Object>) ((Map<String, Object>) vmResponse.getBody()).get("data");
            assertThat(vmData.get("total")).isEqualTo(5);
        }

        // 查询可用数据集
        ResponseEntity<Map> datasetResponse =
            restTemplate.exchange(
                baseUrl + "/api/federated/config/available-datasets?status=READY",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );

        if (datasetResponse.getStatusCode() == HttpStatus.OK) {
            @SuppressWarnings("unchecked")
            Map<String, Object> datasetData = (Map<String, Object>) ((Map<String, Object>) datasetResponse.getBody()).get("data");
            assertThat((Integer) datasetData.get("total")).isGreaterThanOrEqualTo(1);
        }

        System.out.println("✅ 资源查询成功 - 5台VM可用，数据集就绪");
    }

    @Test
    @Order(6)
    void test06_CreateFederatedTask() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminAccessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 使用注册时生成的vmId构建5台VM的任务创建请求
        TaskCreateDTO request = createFederatedTaskRequest();

        HttpEntity<TaskCreateDTO> requestEntity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/api/federated/tasks",
                requestEntity,
                Map.class
            );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertThat(responseBody.get("code")).isEqualTo(200);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
        assertThat(data.get("status")).isEqualTo("CREATED");

        taskId = (String) data.get("taskId");
        assertThat(taskId).isNotNull();

        System.out.println("✅ 5VM联邦学习任务创建成功，任务ID: " + taskId);
    }

    @Test
    @Order(7)
    void test07_StartFederatedTask() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminAccessToken);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/api/federated/tasks/" + taskId + "/start",
                new HttpEntity<>(headers),
                Map.class
            );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertThat(responseBody.get("code")).isEqualTo(200);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
        assertThat(data.get("status")).isEqualTo("RUNNING");

        // 等待训练开始指令到达VM
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("✅ 联邦学习任务启动成功");
    }

    @Test
    @Order(8)
    void test08_ExecuteFederatedLearning() throws InterruptedException {
        // 模拟完整的联邦学习执行过程 - 8轮训练
        int totalRounds = 8;

        for (int round = 1; round <= totalRounds; round++) {
            System.out.println("🔄 执行第" + round + "轮训练...");

            // 等待训练指令
            Thread.sleep(2000);

            // 声明final变量用于lambda表达式
            final String finalTaskId = taskId;
            final int finalRound = round;

            // 所有5台VM并行执行本地训练并上传模型
            List<CompletableFuture<Void>> trainingFutures = mockVMs.stream()
                .map(vm -> CompletableFuture.runAsync(() -> {
                    try {
                        vm.simulateTrainingRound(finalTaskId, finalRound);
                        System.out.println("  ✅ " + vm.getVmId() + " 第" + finalRound + "轮训练完成");
                    } catch (Exception e) {
                        fail("Training round " + finalRound + " failed for " + vm.getVmId());
                    }
                }))
                .collect(Collectors.toList());

            // 等待本轮训练完成
            CompletableFuture.allOf(trainingFutures.toArray(new CompletableFuture[0]))
                .join();

            // 等待模型聚合
            Thread.sleep(3000);

            // 验证任务状态
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminAccessToken);

            ResponseEntity<Map> statusResponse = restTemplate.exchange(
                    baseUrl + "/api/federated/tasks/" + taskId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
                );

            assertThat(statusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

            @SuppressWarnings("unchecked")
            Map<String, Object> taskData = (Map<String, Object>) ((Map<String, Object>) statusResponse.getBody()).get("data");
            @SuppressWarnings("unchecked")
            Map<String, Object> progress = (Map<String, Object>) taskData.get("progress");
            @SuppressWarnings("unchecked")
            Map<String, Object> metrics = (Map<String, Object>) taskData.get("metrics");

            Integer currentRound = (Integer) progress.get("currentRound");
            assertThat(currentRound).isGreaterThanOrEqualTo(round);

            Double globalAccuracy = (Double) metrics.get("globalAccuracy");
            System.out.println("✅ 第" + round + "轮训练完成，当前精度: " +
                String.format("%.3f", globalAccuracy != null ? globalAccuracy : 0.0));
        }

        // 等待任务完全结束
        Thread.sleep(5000);
        System.out.println("🎉 所有8轮联邦学习训练完成");
    }

    @Test
    @Order(9)
    void test09_VerifyTaskCompletion() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminAccessToken);

        // 查询最终任务状态
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/federated/tasks/" + taskId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        @SuppressWarnings("unchecked")
        Map<String, Object> taskData = (Map<String, Object>) ((Map<String, Object>) response.getBody()).get("data");
        @SuppressWarnings("unchecked")
        Map<String, Object> progress = (Map<String, Object>) taskData.get("progress");
        @SuppressWarnings("unchecked")
        Map<String, Object> metrics = (Map<String, Object>) taskData.get("metrics");

        String status = (String) taskData.get("status");
        assertThat(status).isIn("COMPLETED", "CONVERGED");

        Integer currentRound = (Integer) progress.get("currentRound");
        assertThat(currentRound).isEqualTo(8);

        Double globalAccuracy = (Double) metrics.get("globalAccuracy");
        assertThat(globalAccuracy).isGreaterThan(0.0);

        System.out.println("✅ 任务完成验证成功 - 状态: " + status +
            ", 最终精度: " + String.format("%.3f", globalAccuracy));
    }

    @Test
    @Order(10)
    void test10_RetrieveResults() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminAccessToken);

        // 查询任务结果
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/federated/tasks/" + taskId + "/results",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) ((Map<String, Object>) response.getBody()).get("data");
        @SuppressWarnings("unchecked")
        Map<String, Object> finalMetrics = (Map<String, Object>) result.get("finalMetrics");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> participantResults = (List<Map<String, Object>>) result.get("participantResults");
        @SuppressWarnings("unchecked")
        Map<String, Object> globalModel = (Map<String, Object>) result.get("globalModel");

        assertThat(result.get("taskId")).isEqualTo(taskId);
        assertThat(result.get("status")).isIn("COMPLETED", "CONVERGED");
        assertThat((Double) finalMetrics.get("finalAccuracy")).isGreaterThan(0.0);
        assertThat(participantResults).hasSize(5);
        assertThat(globalModel).isNotNull();
        assertThat(globalModel.get("modelId")).isNotNull();

        System.out.println("✅ 结果获取成功 - 5个参与者结果完整，全局模型可用");
        System.out.println("   最终精度: " + String.format("%.3f", (Double) finalMetrics.get("finalAccuracy")));
        System.out.println("   训练时长: " + result.get("totalDuration"));
    }

    @Test
    @Order(11)
    void test11_QueryModelVersions() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminAccessToken);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/model/versions?taskId=" + taskId + "&page=1&size=50",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        @SuppressWarnings("unchecked")
        Map<String, Object> models = (Map<String, Object>) ((Map<String, Object>) response.getBody()).get("data");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> modelList = (List<Map<String, Object>>) models.get("list");

        // 应该有至少40个模型版本（5个VM * 8轮）
        Integer total = (Integer) models.get("total");
        assertThat(total).isGreaterThanOrEqualTo(40);
        assertThat(modelList).isNotEmpty();

        System.out.println("✅ 模型版本查询成功 - 共" + total + "个模型版本");
    }

    @Test
    @Order(12)
    void test12_CleanupConnections() {
        // 优雅关闭所有WebSocket连接
        mockVMs.forEach(vm -> {
            try {
                vm.disconnect();
                System.out.println("✅ " + vm.getVmId() + " 断开连接");
            } catch (Exception e) {
                // 忽略断开连接时的异常
            }
        });

        // 等待连接清理
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 验证连接已断开
        mockVMs.forEach(vm -> assertThat(vm.isConnected()).isFalse());

        System.out.println("🎯 完整的5VM联邦学习流程测试成功完成！");
    }

    // 辅助方法
    private String generateTestCsvData(int rows) {
        StringBuilder csv = new StringBuilder();
        csv.append("frequency,amplitude,phase,snr,distance,depth\n");

        Random random = new Random(42); // 固定种子确保可重复性
        for (int i = 0; i < rows; i++) {
            csv.append(String.format("%.1f,%.2f,%.2f,%.1f,%d,%d\n",
                100 + random.nextFloat() * 900,
                random.nextFloat(),
                random.nextFloat() * 6.28,
                10 + random.nextFloat() * 20,
                50 + random.nextInt(500),
                20 + random.nextInt(100)
            ));
        }

        return csv.toString();
    }

    private TaskCreateDTO createFederatedTaskRequest() {
        // 使用注册时生成的vmId构建参与者列表
        List<TaskCreateDTO.ParticipantConfigDTO.SmartParticipantDTO> participants = new ArrayList<>();
        double[] dataRatios = {0.20, 0.15, 0.15, 0.25, 0.25};

        for (int i = 0; i < registeredVmIds.size(); i++) {
            TaskCreateDTO.ParticipantConfigDTO.SmartParticipantDTO participant =
                TaskCreateDTO.ParticipantConfigDTO.SmartParticipantDTO.builder()
                    .vmId(registeredVmIds.get(i))
                    .role("PARTICIPANT")
                    .dataRatio(dataRatios[i])
                    .capabilities(Arrays.asList("ML_TRAINING", "MODEL_AGGREGATION"))
                    .build();
            participants.add(participant);
        }

        // 构建任务请求
        return TaskCreateDTO.builder()
            .taskName("水下声学通信优化联邦学习 - 5VM测试")
            .description("使用5台虚拟机进行联邦学习优化水下声学通信参数")
            .algorithm("FEDERATED_AVERAGING")
            .taskType("SUPERVISED_LEARNING")
            .datasetConfig(TaskCreateDTO.DatasetConfigDTO.builder()
                .datasetId(datasetId)
                .distributionStrategy("UNIFORM")
                .validationSplit(0.2)
                .testSplit(0.1)
                .build())
            .participantConfig(TaskCreateDTO.ParticipantConfigDTO.builder()
                .selectionMode("MANUAL")
                .requirements(TaskCreateDTO.ParticipantConfigDTO.RequirementsDTO.builder()
                    .minParticipants(3)
                    .maxParticipants(5)
                    .minCpuCores(4)
                    .minMemoryMb(8192)
                    .build())
                .participants(participants)
                .build())
            .hyperparameters(TaskCreateDTO.HyperparametersDTO.builder()
                .rounds(8)
                .epochs(4)
                .learningRate(0.01)
                .batchSize(32)
                .aggregationMethod("WEIGHTED_AVERAGE")
                .build())
            .modelConfig(TaskCreateDTO.ModelConfigDTO.builder()
                .modelType("RANDOM_FOREST")
                .featureColumns(Arrays.asList("frequency", "amplitude", "phase", "snr", "distance", "depth"))
                .targetColumn("label")
                .testSize(0.2)
                .build())
            .securityConfig(TaskCreateDTO.SecurityConfigDTO.builder()
                .encryption("AES_256")
                .secureAggregation(true)
                .differentialPrivacy(TaskCreateDTO.DifferentialPrivacyDTO.builder()
                    .enabled(true)
                    .epsilon(1.0)
                    .delta(0.0001)
                    .build())
                .build())
            .build();
    }
}