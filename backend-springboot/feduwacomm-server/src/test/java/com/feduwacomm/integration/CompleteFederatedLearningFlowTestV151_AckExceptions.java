package com.feduwacomm.integration;

import com.feduwacomm.common.Result;
import com.feduwacomm.dto.ProtocolType;
import com.feduwacomm.entity.FederatedTask;
import com.feduwacomm.entity.VmAckTracking;
import com.feduwacomm.integration.mock.MockVirtualMachine;
import com.feduwacomm.integration.mock.VmTestData;
import com.feduwacomm.mapper.FederatedTasksMapper;
import com.feduwacomm.service.AckCacheService;
import com.feduwacomm.service.cache.model.AckProgress;
import com.feduwacomm.vo.LoginResponseVO;
import com.feduwacomm.vo.TaskOperationVO;
import com.feduwacomm.vo.TrainingDataUploadVO;
import java.time.Duration;
import java.util.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * v1.5.1 端到端异常流程（ACK 超时/失败）
 * 使用 Mock VM 的 ACK 模拟功能构造异常场景，并校验：
 * - 任务状态未错误推进
 * - ACK 进度与失败/未完成统计
 * - 失败 ACK 的错误原因回写
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    // 缩短数据集 ACK 等待时间，提升用例执行效率
    "federated.datasetAck.timeoutSeconds=2"
})
class CompleteFederatedLearningFlowTestV151_AckExceptions {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private FederatedTasksMapper tasksMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AckCacheService ackCacheService;

    private String baseUrl;
    private String adminAccessToken;
    private String datasetId;

    private final List<MockVirtualMachine> mockVMs = new ArrayList<>();
    private final List<VmTestData> vmDataList = new ArrayList<>();
    private final List<String> vmIds = new ArrayList<>();
    private final Map<String, String> vmTokens = new HashMap<>();

    @BeforeAll
    void setUp() throws Exception {
        baseUrl = "http://localhost:" + port;
        // 仅使用3台VM，缩短场景流程
        vmDataList.add(new VmTestData(null, "VM-Ack-1", "127.0.0.1", 9101, 8, 8192, 0));
        vmDataList.add(new VmTestData(null, "VM-Ack-2", "127.0.0.1", 9102, 8, 8192, 0));
        vmDataList.add(new VmTestData(null, "VM-Ack-3", "127.0.0.1", 9103, 8, 8192, 0));
    }

    @AfterAll
    void tearDown() {
        for (MockVirtualMachine vm : mockVMs) {
            try { vm.disconnect(); } catch (Exception ignored) {}
        }
    }

    @Test
    @Order(1)
    void adminLogin() {
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
        this.adminAccessToken = response.getBody().getData().getToken();
        assertThat(this.adminAccessToken).isNotBlank();
    }

    @Test
    @Order(2)
    void registerAndConnectVms() throws Exception {
        // 注册
        for (VmTestData vmData : vmDataList) {
            Map<String, Object> vmRequest = new HashMap<>();
            vmRequest.put("name", vmData.getName());
            vmRequest.put("ipAddress", vmData.getIpAddress());
            vmRequest.put("port", vmData.getPort());
            vmRequest.put("osType", "Ubuntu 20.04");
            vmRequest.put("cpuCores", vmData.getCpuCores());
            vmRequest.put("memoryMb", vmData.getMemoryMb());
            vmRequest.put("diskGb", 128);
            vmRequest.put("capabilities", Map.of(
                "TRAINING", true,
                "SUPPORTED_ALGOS", List.of("FEDERATED_AVERAGING")
            ));
            vmRequest.put("systemInfo", Map.of(
                "os", "Ubuntu 20.04",
                "python", "3.10"
            ));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(vmRequest, headers);

            ResponseEntity<Result<Map<String, Object>>> resp = restTemplate.exchange(
                baseUrl + "/api/v1/vm/register",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Result<Map<String, Object>>>() {}
            );

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().getCode()).isEqualTo(200);
            Map<String, Object> data = resp.getBody().getData();
            String vmId = (String) data.get("vmId");
            String token = (String) data.get("accessToken");
            vmIds.add(vmId);
            vmTokens.put(vmId, token);
        }

        // 连接
        for (int i = 0; i < vmIds.size(); i++) {
            MockVirtualMachine mockVM = new MockVirtualMachine(vmDataList.get(i));
            mockVM.setVmId(vmIds.get(i));
            mockVM.setAccessToken(vmTokens.get(vmIds.get(i)));
            mockVM.connectWebSocket(baseUrl);
            mockVMs.add(mockVM);
            assertThat(mockVM.isConnected()).isTrue();
        }
    }

    @Test
    @Order(3)
    void uploadDataset() {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        ByteArrayResource fileResource = new ByteArrayResource("feature1,feature2\n1,2\n3,4\n5,6\n".getBytes()) {
            @Override
            public String getFilename() { return "ack_exception_ds.csv"; }
        };
        body.add("file", fileResource);

        // 添加uploadDTO JSON部件，满足控制器@RequestPart校验
        String uploadDtoJson = "{" +
                "\"dataType\":\"ACOUSTIC\"," +
                "\"datasetDescription\":\"v1.5.1 ack exception dataset\"" +
                "}";
        ByteArrayResource jsonPart = new ByteArrayResource(uploadDtoJson.getBytes()) {
            @Override
            public String getFilename() { return "uploadDTO.json"; }
        };
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ByteArrayResource> uploadDtoEntity = new HttpEntity<>(jsonPart, partHeaders);
        body.add("uploadDTO", uploadDtoEntity);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(adminAccessToken);

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Result<TrainingDataUploadVO>> response = restTemplate.exchange(
            baseUrl + "/api/training-data/upload",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<Result<TrainingDataUploadVO>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(200);
        this.datasetId = response.getBody().getData().getDatasetId();
        assertThat(this.datasetId).isNotBlank();
    }

    @Test
    @Order(4)
    void taskStart_withDatasetAckFailure_shouldAbortAndRecordFailure() {
        // 创建任务
        String taskId = createTask(datasetId, vmIds);

        // 配置一个VM模拟 DATASET_COMPLETE_ACK 失败
        String failingVmId = vmIds.get(0);
        MockVirtualMachine failingVm = mockVMs.stream()
            .filter(vm -> failingVmId.equals(vm.getVmId()))
            .findFirst().orElseThrow();
        failingVm.clearAckSimulationEvents();
        failingVm.simulateAckFailure(ProtocolType.DATASET_COMPLETE_ACK, "FAILED", "checksum mismatch");

        // 启动任务（期望失败，控制器统一返回200+业务错误码）
        ResponseEntity<Result<TaskOperationVO>> startResp = startTask(taskId);
        assertThat(startResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(startResp.getBody()).isNotNull();
        assertThat(startResp.getBody().getCode()).isNotEqualTo(200);

        // 校验任务未进入RUNNING
        FederatedTask task = tasksMapper.selectTaskById(taskId);
        assertThat(task).isNotNull();
        assertThat(task.getStatus().getCode()).isNotEqualTo("RUNNING");

        // 校验ACK进度：应全部完成但包含失败
        AckProgress progress = ackCacheService.getAckProgress(taskId, VmAckTracking.AckType.DATASET_COMPLETE);
        assertThat(progress).isNotNull();
        assertThat(progress.isAllCompleted()).isTrue();
        assertThat(progress.isAllSuccess()).isFalse();
        assertThat(progress.getFailedVmCount()).isEqualTo(1);
        assertThat(progress.getFailedVms()).contains(failingVmId);

        // 校验失败ACK的错误原因（作为“告警”元数据）
        var failedStatusOpt = ackCacheService.getAckStatus(taskId, failingVmId, VmAckTracking.AckType.DATASET_COMPLETE);
        assertThat(failedStatusOpt).isPresent();
        assertThat(failedStatusOpt.get().getStatus()).isEqualTo(VmAckTracking.AckStatus.FAILED);
        assertThat(failedStatusOpt.get().getErrorMessage()).contains("checksum mismatch");

        // 清理模拟配置，防止影响后续用例
        failingVm.clearAllAckSimulations();
    }

    @Test
    @Order(5)
    void taskStart_withDatasetAckTimeout_shouldAbortWithinConfiguredTimeout() {
        // 创建任务
        String taskId = createTask(datasetId, vmIds);

        // 配置一个VM模拟 DATASET_COMPLETE_ACK 超时（跳过发送）
        String timeoutVmId = vmIds.get(1);
        MockVirtualMachine timeoutVm = mockVMs.stream()
            .filter(vm -> timeoutVmId.equals(vm.getVmId()))
            .findFirst().orElseThrow();
        timeoutVm.clearAckSimulationEvents();
        timeoutVm.simulateAckTimeout(ProtocolType.DATASET_COMPLETE_ACK, Duration.ofMillis(500));

        // 启动任务（期望在2秒内失败，因@PropertySource已缩短超时）
        long t0 = System.currentTimeMillis();
        ResponseEntity<Result<TaskOperationVO>> startResp = startTask(taskId);
        long cost = System.currentTimeMillis() - t0;
        assertThat(startResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(startResp.getBody()).isNotNull();
        assertThat(startResp.getBody().getCode()).isNotEqualTo(200);
        assertThat(cost).isGreaterThanOrEqualTo(1500); // 轮询间隔与处理开销
        assertThat(cost).isLessThan(6000);             // 未使用默认120s

        // 校验任务未进入RUNNING
        FederatedTask task = tasksMapper.selectTaskById(taskId);
        assertThat(task).isNotNull();
        assertThat(task.getStatus().getCode()).isNotEqualTo("RUNNING");

        // 校验ACK进度：已被标记为TIMEOUT（不再Pending）
        AckProgress progress = ackCacheService.getAckProgress(taskId, VmAckTracking.AckType.DATASET_COMPLETE);
        assertThat(progress).isNotNull();
        assertThat(progress.isAllCompleted()).isTrue();
        assertThat(progress.isAllSuccess()).isFalse();
        assertThat(progress.getTimeoutVmCount()).isEqualTo(1);
        assertThat(progress.getTimeoutVms()).contains(timeoutVmId);

        // 清理模拟
        timeoutVm.clearAllAckSimulations();
        ackCacheService.clearAckTypeCache(taskId, VmAckTracking.AckType.DATASET_COMPLETE);
    }

    // ----------------- 辅助方法 -----------------

    private String createTask(String datasetId, List<String> participants) {
        // 兜底：若未成功从上传响应获取datasetId，则从数据库获取最新数据集ID
        if (datasetId == null || datasetId.isBlank()) {
            try {
                String latestId = jdbcTemplate.queryForObject(
                        "SELECT id FROM training_dataset ORDER BY upload_time DESC LIMIT 1",
                        String.class
                );
                if (latestId != null && !latestId.isBlank()) {
                    datasetId = latestId;
                    this.datasetId = latestId;
                }
            } catch (Exception ignore) { }
        }

        // 再次保证不为空
        org.assertj.core.api.Assertions.assertThat(datasetId)
                .as("datasetId 应在创建任务前准备好")
                .isNotBlank();
        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("taskName", "v151-ack-exception-task-" + System.currentTimeMillis());
        createRequest.put("taskType", "CLASSIFICATION");
        createRequest.put("description", "v1.5.1 ack exception test");
        createRequest.put("algorithm", "FEDERATED_AVERAGING");

        Map<String, Object> datasetConfig = new HashMap<>();
        datasetConfig.put("datasetId", datasetId);
        datasetConfig.put("distributionStrategy", "BALANCED");
        createRequest.put("datasetConfig", datasetConfig);

        Map<String, Object> participantConfig = new HashMap<>();
        participantConfig.put("selectionMode", "MANUAL");
        List<Map<String, Object>> vmList = new ArrayList<>();
        for (String vmId : participants) {
            Map<String, Object> vm = new HashMap<>();
            vm.put("vmId", vmId);
            vm.put("role", "PARTICIPANT");
            vm.put("dataSource", "ASSIGNED_DATASET");
            vmList.add(vm);
        }
        participantConfig.put("participants", vmList);
        createRequest.put("participantConfig", participantConfig);

        Map<String, Object> hyper = new HashMap<>();
        hyper.put("rounds", 1);
        hyper.put("epochs", 1);
        hyper.put("batchSize", 8);
        hyper.put("learningRate", 0.01);
        createRequest.put("hyperparameters", hyper);

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
        String taskId = (String) response.getBody().getData().get("taskId");
        assertThat(taskId).isNotBlank();
        return taskId;
    }

    private ResponseEntity<Result<TaskOperationVO>> startTask(String taskId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminAccessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(
            baseUrl + "/api/federated/tasks/" + taskId + "/start",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<Result<TaskOperationVO>>() {}
        );
    }
}
