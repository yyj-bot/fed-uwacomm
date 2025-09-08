package com.feduwacomm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.dto.*;
import com.feduwacomm.service.FederatedTaskService;
import com.feduwacomm.utils.UserJwtUtil;
import com.feduwacomm.config.JwtConfig;
import com.feduwacomm.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import com.feduwacomm.common.BaseContext;
import org.mockito.MockedStatic;

/**
 * 联邦学习任务控制器测试类
 * 测试任务管理相关接口
 */
@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("test")
public class FederatedTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FederatedTaskService federatedTaskService;
    
    @MockBean
    private UserJwtUtil userJwtUtil;
    
    @MockBean
    private JwtConfig jwtConfig;

    @Autowired
    private ObjectMapper objectMapper;

    private TaskCreateDTO taskCreateDTO;
    private TaskOperationVO taskOperationVO;
    private TaskDetailVO taskDetailVO;
    private TaskListVO taskListVO;
    private TaskResultVO taskResultVO;
    private TaskLogVO taskLogVO;
    private String validToken;
    private MockedStatic<BaseContext> baseContextMock;

    @BeforeEach
    void setUp() {
        // 设置测试数据
        setupTestData();
        
        // 设置JWT Mock
        validToken = "test_token";
        Claims claims = new DefaultClaims();
        claims.put("userId", "user123");
        claims.put("username", "testuser");
        claims.put("role", "RESEARCHER");
        claims.put("type", "access");
        
        when(userJwtUtil.validateToken(validToken)).thenReturn(claims);
        
        // Mock BaseContext static method
        baseContextMock = mockStatic(BaseContext.class);
        baseContextMock.when(BaseContext::getCurrentId).thenReturn("user123");
    }
    
    @AfterEach
    void tearDown() {
        if (baseContextMock != null) {
            baseContextMock.close();
        }
    }

    private void setupTestData() {
        // 创建任务请求DTO
        TaskCreateDTO.ParticipantDTO participant1 = TaskCreateDTO.ParticipantDTO.builder()
            .vmId("vm1")
            .role("PARTICIPANT")
            .dataSource("data1.csv")
            .build();

        TaskCreateDTO.ParticipantDTO participant2 = TaskCreateDTO.ParticipantDTO.builder()
            .vmId("vm2")
            .role("PARTICIPANT")
            .dataSource("data2.csv")
            .build();

        TaskCreateDTO.HyperparametersDTO hyperparameters = TaskCreateDTO.HyperparametersDTO.builder()
            .learningRate(0.01)
            .batchSize(32)
            .epochs(100)
            .rounds(10)
            .minParticipants(2)
            .build();

        TaskCreateDTO.ModelConfigDTO modelConfig = TaskCreateDTO.ModelConfigDTO.builder()
            .modelType("RANDOM_FOREST")
            .testSize(0.2)
            .randomState(42)
            .build();

        taskCreateDTO = TaskCreateDTO.builder()
            .taskName("测试任务")
            .taskType("CLASSIFICATION")
            .description("测试描述")
            .algorithm("FEDERATED_AVERAGING")
            .participants(Arrays.asList(participant1, participant2))
            .hyperparameters(hyperparameters)
            .modelConfig(modelConfig)
            .build();

        // 创建任务响应VO
        taskOperationVO = TaskOperationVO.builder()
            .taskId("task123")
            .taskName("测试任务")
            .status("CREATED")
            .createdAt(LocalDateTime.now())
            .participantCount(2)
            .estimatedDuration(3600)
            .build();

        // 任务详情VO
        TaskDetailVO.ParticipantVO participantVO1 = TaskDetailVO.ParticipantVO.builder()
            .vmId("vm1")
            .role("PARTICIPANT")
            .status("CONNECTED")
            .dataSource("data1.csv")
            .build();

        TaskDetailVO.ParticipantVO participantVO2 = TaskDetailVO.ParticipantVO.builder()
            .vmId("vm2")
            .role("PARTICIPANT")
            .status("CONNECTED")
            .dataSource("data2.csv")
            .build();

        TaskDetailVO.MetricsVO metrics = TaskDetailVO.MetricsVO.builder()
            .globalLoss(0.25)
            .globalAccuracy(0.85)
            .communicationRounds(5)
            .dataProcessed(1000)
            .estimatedTimeRemaining(1800)
            .build();

        taskDetailVO = TaskDetailVO.builder()
            .taskId("task123")
            .taskName("测试任务")
            .taskType("CLASSIFICATION")
            .status("RUNNING")
            .algorithm("FEDERATED_AVERAGING")
            .createdAt(LocalDateTime.now())
            .currentRound(5)
            .totalRounds(10)
            .progress(50.0)
            .participants(Arrays.asList(participantVO1, participantVO2))
            .metrics(metrics)
            .build();

        // 任务列表VO
        TaskVO taskVO = TaskVO.builder()
            .taskId("task123")
            .taskName("测试任务")
            .taskType("CLASSIFICATION")
            .status("RUNNING")
            .algorithm("FEDERATED_AVERAGING")
            .createdAt(LocalDateTime.now())
            .participantCount(2)
            .currentRound(5)
            .totalRounds(10)
            .progress(50.0)
            .build();

        taskListVO = TaskListVO.builder()
            .total(1)
            .page(1)
            .size(20)
            .tasks(Arrays.asList(taskVO))
            .build();

        // 任务结果VO
        TaskResultVO.FinalResultsVO finalResults = TaskResultVO.FinalResultsVO.builder()
            .accuracy(0.892)
            .loss(0.098)
            .precision(0.885)
            .recall(0.890)
            .f1Score(0.887)
            .confusionMatrix(new int[][]{{45, 5}, {8, 42}})
            .build();

        taskResultVO = TaskResultVO.builder()
            .taskId("task123")
            .taskName("测试任务")
            .status("COMPLETED")
            .finalResults(finalResults)
            .build();

        // 任务日志VO
        TaskLogVO.LogEntry logEntry = TaskLogVO.LogEntry.builder()
            .timestamp(LocalDateTime.now())
            .level("INFO")
            .message("任务启动成功")
            .source("TASK_MANAGER")
            .build();

        taskLogVO = TaskLogVO.builder()
            .taskId("task123")
            .total(1)
            .page(1)
            .size(10)
            .logs(Arrays.asList(logEntry))
            .build();
    }

    @Test
    void testCreateTask_Success() throws Exception {
        when(federatedTaskService.createTask(any(TaskCreateDTO.class), eq("user123")))
            .thenReturn(taskOperationVO);

        mockMvc.perform(post("/api/federated/tasks")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(taskCreateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("任务创建成功"))
                .andExpect(jsonPath("$.data.taskId").value("task123"))
                .andExpect(jsonPath("$.data.taskName").value("测试任务"))
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andExpect(jsonPath("$.data.participantCount").value(2));
    }

    @Test
    void testCreateTask_InvalidInput() throws Exception {
        TaskCreateDTO invalidDTO = TaskCreateDTO.builder().build(); // 缺少必填字段

        mockMvc.perform(post("/api/federated/tasks")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isOk()) // FedUWAComm uses HTTP 200 with internal error codes
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("验证失败"));
    }

    @Test
    void testConfigureTask_Success() throws Exception {
        TaskConfigDTO configDTO = TaskConfigDTO.builder()
            .algorithm("FEDERATED_AVERAGING")
            .build();

        TaskOperationVO configResponse = TaskOperationVO.builder()
            .taskId("task123")
            .status("CONFIGURED")
            .updatedAt(LocalDateTime.now())
            .configVersion("v1.1")
            .build();

        when(federatedTaskService.configureTask(eq("task123"), any(TaskConfigDTO.class), anyString()))
            .thenReturn(configResponse);

        mockMvc.perform(put("/api/federated/tasks/task123/config")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(configDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("任务配置成功"))
                .andExpect(jsonPath("$.data.taskId").value("task123"))
                .andExpect(jsonPath("$.data.status").value("CONFIGURED"));
    }

    @Test
    void testStartTask_Success() throws Exception {
        TaskOperationVO startResponse = TaskOperationVO.builder()
            .taskId("task123")
            .status("RUNNING")
            .startedAt(LocalDateTime.now())
            .currentRound(0)
            .build();

        when(federatedTaskService.startTask(eq("task123"), anyString()))
            .thenReturn(startResponse);

        mockMvc.perform(post("/api/federated/tasks/task123/start")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("任务启动成功"))
                .andExpect(jsonPath("$.data.status").value("RUNNING"));
    }

    @Test
    void testPauseTask_Success() throws Exception {
        TaskOperationVO pauseResponse = TaskOperationVO.builder()
            .taskId("task123")
            .status("PAUSED")
            .pausedAt(LocalDateTime.now())
            .currentRound(5)
            .build();

        when(federatedTaskService.pauseTask(eq("task123"), anyString()))
            .thenReturn(pauseResponse);

        mockMvc.perform(post("/api/federated/tasks/task123/pause")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("任务暂停成功"))
                .andExpect(jsonPath("$.data.status").value("PAUSED"));
    }

    @Test
    void testResumeTask_Success() throws Exception {
        TaskOperationVO resumeResponse = TaskOperationVO.builder()
            .taskId("task123")
            .status("RUNNING")
            .resumedAt(LocalDateTime.now())
            .currentRound(5)
            .build();

        when(federatedTaskService.resumeTask(eq("task123"), anyString()))
            .thenReturn(resumeResponse);

        mockMvc.perform(post("/api/federated/tasks/task123/resume")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("任务恢复成功"))
                .andExpect(jsonPath("$.data.status").value("RUNNING"));
    }

    @Test
    void testStopTask_Success() throws Exception {
        TaskStopDTO stopDTO = TaskStopDTO.builder()
            .reason("测试停止")
            .saveCheckpoint(true)
            .build();

        TaskOperationVO stopResponse = TaskOperationVO.builder()
            .taskId("task123")
            .status("STOPPED")
            .stoppedAt(LocalDateTime.now())
            .finalRound(8)
            .checkpointSaved(true)
            .build();

        when(federatedTaskService.stopTask(eq("task123"), any(TaskStopDTO.class), anyString()))
            .thenReturn(stopResponse);

        mockMvc.perform(post("/api/federated/tasks/task123/stop")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(stopDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("任务停止成功"))
                .andExpect(jsonPath("$.data.status").value("STOPPED"))
                .andExpect(jsonPath("$.data.checkpointSaved").value(true));
    }

    @Test
    void testCancelTask_Success() throws Exception {
        TaskCancelDTO cancelDTO = TaskCancelDTO.builder()
            .reason("测试取消")
            .build();

        TaskOperationVO cancelResponse = TaskOperationVO.builder()
            .taskId("task123")
            .status("CANCELLED")
            .cancelledAt(LocalDateTime.now())
            .reason("测试取消")
            .build();

        when(federatedTaskService.cancelTask(eq("task123"), any(TaskCancelDTO.class), anyString()))
            .thenReturn(cancelResponse);

        mockMvc.perform(post("/api/federated/tasks/task123/cancel")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cancelDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("任务取消成功"))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void testGetTaskDetail_Success() throws Exception {
        when(federatedTaskService.getTaskDetail(eq("task123")))
            .thenReturn(taskDetailVO);

        mockMvc.perform(get("/api/federated/tasks/task123")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("查询成功"))
                .andExpect(jsonPath("$.data.taskId").value("task123"))
                .andExpect(jsonPath("$.data.taskName").value("测试任务"))
                .andExpect(jsonPath("$.data.status").value("RUNNING"))
                .andExpect(jsonPath("$.data.participants").isArray())
                .andExpect(jsonPath("$.data.participants[0].vmId").value("vm1"));
    }

    @Test
    void testGetTaskList_Success() throws Exception {
        when(federatedTaskService.getTaskList(any(TaskQueryDTO.class)))
            .thenReturn(taskListVO);

        mockMvc.perform(get("/api/federated/tasks")
                        .header("Authorization", "Bearer " + validToken)
                        .param("page", "1")
                        .param("size", "20")
                        .param("status", "RUNNING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("查询成功"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.tasks").isArray())
                .andExpect(jsonPath("$.data.tasks[0].taskId").value("task123"));
    }

    @Test
    void testGetTaskResult_Success() throws Exception {
        when(federatedTaskService.getTaskResult(eq("task123")))
            .thenReturn(taskResultVO);

        mockMvc.perform(get("/api/federated/tasks/task123/results")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("查询成功"))
                .andExpect(jsonPath("$.data.taskId").value("task123"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.finalResults.accuracy").value(0.892));
    }

    @Test
    void testGetTaskLogs_Success() throws Exception {
        when(federatedTaskService.getTaskLogs(eq("task123"), any(TaskLogQueryDTO.class)))
            .thenReturn(taskLogVO);

        mockMvc.perform(get("/api/federated/tasks/task123/logs")
                        .header("Authorization", "Bearer " + validToken)
                        .param("page", "1")
                        .param("size", "10")
                        .param("level", "INFO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("查询成功"))
                .andExpect(jsonPath("$.data.taskId").value("task123"))
                .andExpect(jsonPath("$.data.logs").isArray())
                .andExpect(jsonPath("$.data.logs[0].level").value("INFO"));
    }

    @Test
    void testDeleteTask_Success() throws Exception {
        TaskDeleteDTO deleteDTO = TaskDeleteDTO.builder()
            .deleteData(true)
            .deleteModel(false)
            .build();

        TaskOperationVO deleteResponse = TaskOperationVO.builder()
            .taskId("task123")
            .deletedAt(LocalDateTime.now())
            .dataDeleted(true)
            .modelPreserved(true)
            .build();

        when(federatedTaskService.deleteTask(eq("task123"), any(TaskDeleteDTO.class), anyString()))
            .thenReturn(deleteResponse);

        mockMvc.perform(delete("/api/federated/tasks/task123")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deleteDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("任务删除成功"))
                .andExpect(jsonPath("$.data.dataDeleted").value(true))
                .andExpect(jsonPath("$.data.modelPreserved").value(true));
    }

    @Test
    void testGetTaskDetail_TaskNotFound() throws Exception {
        when(federatedTaskService.getTaskDetail(eq("nonexistent")))
            .thenThrow(new RuntimeException("任务不存在"));

        mockMvc.perform(get("/api/federated/tasks/nonexistent")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("查询失败: 任务不存在"));
    }

    @Test
    void testStartTask_InvalidStatus() throws Exception {
        when(federatedTaskService.startTask(eq("task123"), anyString()))
            .thenThrow(new RuntimeException("任务状态不允许启动"));

        mockMvc.perform(post("/api/federated/tasks/task123/start")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("任务启动失败: 任务状态不允许启动"));
    }

    @Test
    void testCreateTask_ServiceException() throws Exception {
        when(federatedTaskService.createTask(any(TaskCreateDTO.class), anyString()))
            .thenThrow(new RuntimeException("任务创建失败"));

        mockMvc.perform(post("/api/federated/tasks")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(taskCreateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("任务创建失败: 任务创建失败"));
    }
}