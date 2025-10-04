package com.feduwacomm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.dto.TaskCreateDTO;
import com.feduwacomm.service.FederatedTaskService;
import com.feduwacomm.service.VmInstanceService;
import com.feduwacomm.service.TrainingDataService;
import com.feduwacomm.config.TestJwtInterceptorConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 联邦学习任务控制器权限测试类
 * 测试不同角色对联邦学习接口的访问权限
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtInterceptorConfig.class)
@DisplayName("联邦任务控制器权限测试")
public class FederatedTaskControllerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FederatedTaskService federatedTaskService;

    @MockBean
    private VmInstanceService vmInstanceService;

    @MockBean
    private TrainingDataService trainingDataService;

    // JWT认证由TestSecurityConfig处理

    @Autowired
    private ObjectMapper objectMapper;


    @BeforeEach
    void setUp() {
        // 不再需要Mock BaseContext，由TestSecurityConfig处理
    }


    // ========== ADMIN角色权限测试 ==========

    @Test
    @DisplayName("ADMIN角色应该能够访问获取可用VM接口")
    void testAdmin_ShouldAccessAvailableVms() throws Exception {
        // 准备
        String token = "admin_token";

        // 执行和验证
        mockMvc.perform(get("/api/federated/config/available-vms")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ADMIN角色应该能够访问任务创建接口")
    void testAdmin_ShouldAccessCreateTask() throws Exception {
        // 准备
        String token = "admin_token";

        TaskCreateDTO taskCreateDTO = createValidTaskCreateDTO();

        // 执行和验证
        mockMvc.perform(post("/api/federated/tasks")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(taskCreateDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ADMIN角色应该能够访问任务启动接口")
    void testAdmin_ShouldAccessStartTask() throws Exception {
        // 准备
        String token = "admin_token";

        // 执行和验证
        mockMvc.perform(post("/api/federated/tasks/task123/start")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ========== RESEARCHER角色权限测试 ==========

    @Test
    @DisplayName("RESEARCHER角色应该能够访问获取可用数据集接口")
    void testResearcher_ShouldAccessAvailableDatasets() throws Exception {
        // 准备
        String token = "researcher_token";

        // 执行和验证
        mockMvc.perform(get("/api/federated/config/available-datasets")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("RESEARCHER角色应该能够访问任务查询接口")
    void testResearcher_ShouldAccessTaskList() throws Exception {
        // 准备
        String token = "researcher_token";

        // 执行和验证
        mockMvc.perform(get("/api/federated/tasks")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("RESEARCHER角色应该能够访问任务详情接口")
    void testResearcher_ShouldAccessTaskDetail() throws Exception {
        // 准备
        String token = "researcher_token";

        // 执行和验证
        mockMvc.perform(get("/api/federated/tasks/task123")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ========== OPERATOR角色权限测试 ==========

    @Test
    @DisplayName("OPERATOR角色应该能够访问任务控制接口")
    void testOperator_ShouldAccessTaskControl() throws Exception {
        // 准备
        String token = "operator_token";

        // 执行和验证 - 暂停任务
        mockMvc.perform(post("/api/federated/tasks/task123/pause")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 执行和验证 - 恢复任务
        mockMvc.perform(post("/api/federated/tasks/task123/resume")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 执行和验证 - 停止任务
        mockMvc.perform(post("/api/federated/tasks/task123/stop")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("OPERATOR角色应该能够访问任务统计接口")
    void testOperator_ShouldAccessTaskStats() throws Exception {
        // 准备
        String token = "operator_token";

        // 执行和验证
        mockMvc.perform(get("/api/federated/tasks/stats")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ========== VIEWER角色权限测试（应该被拒绝） ==========

    @Test
    @DisplayName("VIEWER角色应该被拒绝访问获取可用VM接口")
    void testViewer_ShouldBeDeniedAvailableVms() throws Exception {
        // 准备
        String token = "viewer_token";

        // 执行和验证
        mockMvc.perform(get("/api/federated/config/available-vms")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("VIEWER角色应该被拒绝访问任务创建接口")
    void testViewer_ShouldBeDeniedCreateTask() throws Exception {
        // 准备
        String token = "viewer_token";

        TaskCreateDTO taskCreateDTO = createValidTaskCreateDTO();

        // 执行和验证
        mockMvc.perform(post("/api/federated/tasks")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(taskCreateDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("VIEWER角色应该被拒绝访问任务控制接口")
    void testViewer_ShouldBeDeniedTaskControl() throws Exception {
        // 准备
        String token = "viewer_token";

        // 执行和验证 - 启动任务
        mockMvc.perform(post("/api/federated/tasks/task123/start")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        // 执行和验证 - 停止任务
        mockMvc.perform(post("/api/federated/tasks/task123/stop")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        // 执行和验证 - 删除任务
        mockMvc.perform(delete("/api/federated/tasks/task123")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("VIEWER角色应该被拒绝访问任务查询接口")
    void testViewer_ShouldBeDeniedTaskQuery() throws Exception {
        // 准备
        String token = "viewer_token";

        // 执行和验证 - 任务列表
        mockMvc.perform(get("/api/federated/tasks")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        // 执行和验证 - 任务详情
        mockMvc.perform(get("/api/federated/tasks/task123")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        // 执行和验证 - 任务结果
        mockMvc.perform(get("/api/federated/tasks/task123/results")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ========== 无效Token测试 ==========

    @Test
    @DisplayName("无效Token应该被拒绝访问")
    void testInvalidToken_ShouldBeDenied() throws Exception {
        // 准备
        String invalidToken = "invalid_token";

        // 执行和验证
        mockMvc.perform(get("/api/federated/config/available-vms")
                .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("缺少Token应该被拒绝访问")
    void testMissingToken_ShouldBeDenied() throws Exception {
        // 执行和验证 - 不提供Authorization头
        mockMvc.perform(get("/api/federated/config/available-vms"))
                .andExpect(status().isUnauthorized());
    }

    // ========== 批量操作权限测试 ==========

    @Test
    @DisplayName("只有授权角色能够访问批量操作接口")
    void testBatchOperation_AuthorizedRoles() throws Exception {
        // 测试ADMIN角色
        String adminToken = "admin_token";

        mockMvc.perform(post("/api/federated/tasks/batch")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"operation\":\"start\",\"taskIds\":[\"task1\",\"task2\"]}"))
                .andExpect(status().isOk());

        // 测试RESEARCHER角色
        String researcherToken = "researcher_token";

        mockMvc.perform(post("/api/federated/tasks/batch")
                .header("Authorization", "Bearer " + researcherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"operation\":\"pause\",\"taskIds\":[\"task1\",\"task2\"]}"))
                .andExpect(status().isOk());

        // 测试OPERATOR角色
        String operatorToken = "operator_token";

        mockMvc.perform(post("/api/federated/tasks/batch")
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"operation\":\"stop\",\"taskIds\":[\"task1\",\"task2\"]}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("VIEWER角色应该被拒绝访问批量操作接口")
    void testBatchOperation_ViewerDenied() throws Exception {
        // 准备
        String viewerToken = "viewer_token";

        // 执行和验证
        mockMvc.perform(post("/api/federated/tasks/batch")
                .header("Authorization", "Bearer " + viewerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"operation\":\"start\",\"taskIds\":[\"task1\",\"task2\"]}"))
                .andExpect(status().isForbidden());
    }

    // ========== 辅助方法 ==========

    /**
     * 创建有效的任务创建DTO
     */
    private TaskCreateDTO createValidTaskCreateDTO() {
        TaskCreateDTO.ParticipantConfigDTO.SmartParticipantDTO participant =
                TaskCreateDTO.ParticipantConfigDTO.SmartParticipantDTO.builder()
                .vmId("vm1")
                .role("PARTICIPANT")
                .dataRatio(1000) // v1.5.1.1千分比权重：单VM占100%数据
                .build();

        TaskCreateDTO.ParticipantConfigDTO participantConfig = TaskCreateDTO.ParticipantConfigDTO.builder()
                .selectionMode("MANUAL")
                .participants(Arrays.asList(participant))
                .build();

        return TaskCreateDTO.builder()
                .taskName("测试任务")
                .description("权限测试任务")
                .algorithm("FedAvg")
                .participantConfig(participantConfig)
                .build();
    }
}