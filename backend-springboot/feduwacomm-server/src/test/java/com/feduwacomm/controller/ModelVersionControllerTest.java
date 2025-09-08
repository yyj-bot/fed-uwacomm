package com.feduwacomm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.dto.*;
import com.feduwacomm.service.ModelVersionService;
import com.feduwacomm.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import com.feduwacomm.common.BaseContext;
import com.feduwacomm.utils.UserJwtUtil;
import com.feduwacomm.config.JwtConfig;
import org.mockito.MockedStatic;
import org.junit.jupiter.api.AfterEach;

/**
 * ModelVersionController 单元测试类
 */
@SpringBootTest
@AutoConfigureMockMvc
public class ModelVersionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private ModelVersionService modelVersionService;
    
    @MockBean
    private UserJwtUtil userJwtUtil;
    
    @MockBean
    private JwtConfig jwtConfig;

    @Autowired
    private ObjectMapper objectMapper;
    
    private String validToken;
    private MockedStatic<BaseContext> baseContextMock;

    private String taskId;
    private String modelId;
    private ModelVersionVO mockModelVersionVO;
    private ModelUploadResponseVO mockUploadResponse;

    @BeforeEach
    void setUp() {
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
        
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        taskId = "test-task-id-12345678901234567890";
        modelId = "test-model-id-12345678901234567890";
        
        // 创建模拟数据
        mockModelVersionVO = ModelVersionVO.builder()
            .modelId(modelId)
            .taskId(taskId)
            .roundNumber(1)
            .aggregationMethod("FEDAVG")
            .clientCount(5)
            .accuracy(new BigDecimal("0.8500"))
            .loss(new BigDecimal("0.1234"))
            .status("UPLOADED")
            .description("测试模型")
            .fileSize(1024L)
            .fileFormat(".pth")
            .createdAt(LocalDateTime.now())
            .build();

        mockUploadResponse = ModelUploadResponseVO.builder()
            .modelId(modelId)
            .taskId(taskId)
            .roundNumber(1)
            .status("UPLOADED")
            .description("测试模型")
            .createdAt(LocalDateTime.now())
            .build();
    }

    @Test
    void testUploadModel_Success() throws Exception {
        // 准备测试数据
        MockMultipartFile file = new MockMultipartFile(
            "file", "model.pth", "application/octet-stream", "test model content".getBytes()
        );

        // 模拟服务返回
        when(modelVersionService.uploadModel(anyString(), anyInt(), anyString(), anyString(), any()))
            .thenReturn(mockUploadResponse);

        // 执行测试
        mockMvc.perform(multipart("/api/model/upload")
                .file(file)
                .param("taskId", taskId)
                .param("roundNumber", "1")
                .param("description", "测试模型")
                .param("parameters", "{\"learning_rate\": 0.001}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("模型上传成功"))
            .andExpect(jsonPath("$.data.modelId").value(modelId))
            .andExpect(jsonPath("$.data.taskId").value(taskId))
            .andExpect(jsonPath("$.data.roundNumber").value(1))
            .andExpect(jsonPath("$.data.status").value("UPLOADED"));

        // 验证服务方法被调用
        verify(modelVersionService, times(1))
            .uploadModel(eq(taskId), eq(1), eq("测试模型"), eq("{\"learning_rate\": 0.001}"), any());
    }

    @Test
    void testUploadModel_MissingFile() throws Exception {
        // 对于缺少文件的情况，模拟服务抛出异常
        when(modelVersionService.uploadModel(anyString(), anyInt(), anyString(), anyString(), any()))
                .thenThrow(new IllegalArgumentException("文件不能为空"));
        
        // 测试缺少文件的情况 - FedUWAComm uses HTTP 200 with internal error codes
        mockMvc.perform(multipart("/api/model/upload")
                .header("Authorization", "Bearer " + validToken)
                .param("taskId", taskId)
                .param("roundNumber", "1")
                .param("description", "测试模型")
                .param("hyperparameters", "{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void testGetModelVersions_Success() throws Exception {
        // 准备分页响应数据
        PageResponseDTO<ModelVersionVO> pageResponse = PageResponseDTO.<ModelVersionVO>builder()
            .total(1L)
            .pages(1L)
            .current(1)
            .size(10)
            .records(Arrays.asList(mockModelVersionVO))
            .build();

        // 模拟服务返回
        when(modelVersionService.getModelVersions(any(ModelQueryDTO.class)))
            .thenReturn(pageResponse);

        // 执行测试
        mockMvc.perform(get("/api/model/versions")
                .param("taskId", taskId)
                .param("page", "1")
                .param("size", "10")
                .param("sort", "createdAt")
                .param("order", "desc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("查询成功"))
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.records[0].modelId").value(modelId))
            .andExpect(jsonPath("$.data.records[0].taskId").value(taskId));

        // 验证服务方法被调用
        verify(modelVersionService, times(1)).getModelVersions(any(ModelQueryDTO.class));
    }

    @Test
    void testGetModelVersionById_Success() throws Exception {
        // 模拟服务返回
        when(modelVersionService.getModelVersionById(modelId))
            .thenReturn(mockModelVersionVO);

        // 执行测试
        mockMvc.perform(get("/api/model/versions/{modelId}", modelId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("查询成功"))
            .andExpect(jsonPath("$.data.modelId").value(modelId))
            .andExpect(jsonPath("$.data.taskId").value(taskId))
            .andExpect(jsonPath("$.data.roundNumber").value(1))
            .andExpect(jsonPath("$.data.status").value("UPLOADED"));

        // 验证服务方法被调用
        verify(modelVersionService, times(1)).getModelVersionById(modelId);
    }

    @Test
    void testGetModelVersionsByTaskId_Success() throws Exception {
        // 准备任务版本响应数据
        ModelTaskVersionsVO taskVersionsVO = ModelTaskVersionsVO.builder()
            .taskId(taskId)
            .taskName("水声分类任务")
            .totalModels(1)
            .versions(Arrays.asList(mockModelVersionVO))
            .build();

        // 模拟服务返回
        when(modelVersionService.getModelVersionsByTaskId(anyString(), any(), any(), any(), any()))
            .thenReturn(taskVersionsVO);

        // 执行测试
        mockMvc.perform(get("/api/model/versions/task/{taskId}", taskId)
                .param("roundNumber", "1")
                .param("status", "UPLOADED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("查询成功"))
            .andExpect(jsonPath("$.data.taskId").value(taskId))
            .andExpect(jsonPath("$.data.taskName").value("水声分类任务"))
            .andExpect(jsonPath("$.data.totalModels").value(1))
            .andExpect(jsonPath("$.data.versions[0].modelId").value(modelId));

        // 验证服务方法被调用
        verify(modelVersionService, times(1))
            .getModelVersionsByTaskId(eq(taskId), eq(1), eq("UPLOADED"), any(), any());
    }

    @Test
    void testEvaluateModel_Success() throws Exception {
        // 准备评估请求
        ModelEvaluateDTO evaluateDTO = ModelEvaluateDTO.builder()
            .modelId(modelId)
            .testDataPath("/data/test.csv")
            .metrics(Arrays.asList("accuracy", "loss"))
            .batchSize(32)
            .device("cpu")
            .build();

        // 准备评估响应
        Map<String, BigDecimal> metrics = new HashMap<>();
        metrics.put("accuracy", new BigDecimal("0.8500"));
        metrics.put("loss", new BigDecimal("0.1234"));

        ModelEvaluateResponseVO evaluateResponse = ModelEvaluateResponseVO.builder()
            .modelId(modelId)
            .evaluationId("eval_123456789")
            .metrics(metrics)
            .evaluationTime(15.5)
            .testSamples(1000)
            .status("COMPLETED")
            .createdAt(LocalDateTime.now())
            .build();

        // 模拟服务返回
        when(modelVersionService.evaluateModel(any(ModelEvaluateDTO.class)))
            .thenReturn(evaluateResponse);

        // 执行测试
        mockMvc.perform(post("/api/model/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(evaluateDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("评估完成"))
            .andExpect(jsonPath("$.data.modelId").value(modelId))
            .andExpect(jsonPath("$.data.evaluationId").value("eval_123456789"))
            .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // 验证服务方法被调用
        verify(modelVersionService, times(1)).evaluateModel(any(ModelEvaluateDTO.class));
    }

    @Test
    void testDeployModel_Success() throws Exception {
        // 准备部署请求
        Map<String, Object> deploymentConfig = new HashMap<>();
        deploymentConfig.put("replicas", 2);
        deploymentConfig.put("resources", Map.of("cpu", "1", "memory", "2Gi"));

        ModelDeployDTO deployDTO = ModelDeployDTO.builder()
            .modelId(modelId)
            .deploymentName("水声分类模型_v1.0")
            .targetVms(Arrays.asList("vm_1", "vm_2"))
            .deploymentConfig(deploymentConfig)
            .description("生产环境部署")
            .build();

        // 准备部署响应
        ModelDeployResponseVO deployResponse = ModelDeployResponseVO.builder()
            .deploymentId("deploy_123456789")
            .modelId(modelId)
            .deploymentName("水声分类模型_v1.0")
            .targetVms(Arrays.asList("vm_1", "vm_2"))
            .status("DEPLOYED")
            .deploymentConfig(deploymentConfig)
            .endpoints(Arrays.asList("http://vm_1:8080/predict", "http://vm_2:8080/predict"))
            .createdAt(LocalDateTime.now())
            .build();

        // 模拟服务返回
        when(modelVersionService.deployModel(any(ModelDeployDTO.class)))
            .thenReturn(deployResponse);

        // 执行测试
        mockMvc.perform(post("/api/model/deploy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deployDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("部署成功"))
            .andExpect(jsonPath("$.data.deploymentId").value("deploy_123456789"))
            .andExpect(jsonPath("$.data.modelId").value(modelId))
            .andExpect(jsonPath("$.data.status").value("DEPLOYED"));

        // 验证服务方法被调用
        verify(modelVersionService, times(1)).deployModel(any(ModelDeployDTO.class));
    }

    @Test
    void testGetModelStatistics_Success() throws Exception {
        // 准备统计响应
        ModelStatisticsVO statisticsVO = ModelStatisticsVO.builder()
            .totalModels(50)
            .averageAccuracy(new BigDecimal("0.8500"))
            .averageLoss(new BigDecimal("0.1234"))
            .uploadTrend(Arrays.asList(
                ModelStatisticsVO.UploadTrendVO.builder()
                    .date("2024-01-01")
                    .count(5)
                    .build()
            ))
            .accuracyTrend(Arrays.asList(
                ModelStatisticsVO.AccuracyTrendVO.builder()
                    .roundNumber(1)
                    .accuracy(new BigDecimal("0.8500"))
                    .build()
            ))
            .build();

        // 模拟服务返回
        when(modelVersionService.getModelStatistics(anyString(), anyString()))
            .thenReturn(statisticsVO);

        // 执行测试
        mockMvc.perform(get("/api/model/statistics")
                .param("taskId", taskId)
                .param("timeRange", "7d"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("查询成功"))
            .andExpect(jsonPath("$.data.totalModels").value(50))
            .andExpect(jsonPath("$.data.averageAccuracy").value(0.8500));

        // 验证服务方法被调用
        verify(modelVersionService, times(1)).getModelStatistics(taskId, "7d");
    }

    @Test
    void testDeleteModelVersion_Success() throws Exception {
        // 准备删除响应
        Map<String, Object> deleteResponse = new HashMap<>();
        deleteResponse.put("modelId", modelId);
        deleteResponse.put("deletedAt", LocalDateTime.now().toString());

        // 模拟服务返回
        when(modelVersionService.deleteModelVersion(anyString(), anyBoolean(), anyBoolean()))
            .thenReturn(deleteResponse);

        // 执行测试
        mockMvc.perform(delete("/api/model/versions/{modelId}", modelId)
                .param("force", "false")
                .param("deleteFile", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("删除成功"))
            .andExpect(jsonPath("$.data.modelId").value(modelId));

        // 验证服务方法被调用
        verify(modelVersionService, times(1)).deleteModelVersion(modelId, false, true);
    }

    @Test
    void testBatchUploadModels_Success() throws Exception {
        // 准备批量上传响应
        ModelBatchUploadResponseVO batchResponse = ModelBatchUploadResponseVO.builder()
            .successCount(2)
            .failedCount(0)
            .models(Arrays.asList(
                ModelBatchUploadResponseVO.ModelBatchUploadResultVO.builder()
                    .modelId("model1")
                    .status("UPLOADED")
                    .message("上传成功")
                    .build(),
                ModelBatchUploadResponseVO.ModelBatchUploadResultVO.builder()
                    .modelId("model2")
                    .status("UPLOADED")
                    .message("上传成功")
                    .build()
            ))
            .build();

        // 模拟服务返回
        when(modelVersionService.batchUploadModels(any(ModelBatchUploadDTO.class)))
            .thenReturn(batchResponse);

        // 准备简化的批量上传请求，避免MultipartFile序列化问题
        String batchUploadJson = "{"
            + "\"taskId\": \"" + taskId + "\","
            + "\"models\": ["
                + "{"
                    + "\"taskId\": \"" + taskId + "\","
                    + "\"roundNumber\": 1,"
                    + "\"description\": \"测试模型1\""
                + "},"
                + "{"
                    + "\"taskId\": \"" + taskId + "\","
                    + "\"roundNumber\": 2,"
                    + "\"description\": \"测试模型2\""
                + "}"
            + "]"
            + "}";

        // 执行测试
        mockMvc.perform(post("/api/model/upload/batch")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(batchUploadJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("批量上传成功"))
            .andExpect(jsonPath("$.data.successCount").value(2))
            .andExpect(jsonPath("$.data.failedCount").value(0));

        // 验证服务方法被调用
        verify(modelVersionService, times(1)).batchUploadModels(any(ModelBatchUploadDTO.class));
    }

    @Test
    void testGetDeploymentStatus_Success() throws Exception {
        // 准备部署状态响应
        Map<String, Object> statusResponse = new HashMap<>();
        statusResponse.put("deploymentId", "deploy_123456789");
        statusResponse.put("status", "RUNNING");
        statusResponse.put("replicas", Map.of("desired", 2, "available", 2, "ready", 2));

        // 模拟服务返回
        when(modelVersionService.getDeploymentStatus(anyString()))
            .thenReturn(statusResponse);

        // 执行测试
        mockMvc.perform(get("/api/model/deploy/status/{deploymentId}", "deploy_123456789"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("查询成功"))
            .andExpect(jsonPath("$.data.deploymentId").value("deploy_123456789"))
            .andExpect(jsonPath("$.data.status").value("RUNNING"));

        // 验证服务方法被调用
        verify(modelVersionService, times(1)).getDeploymentStatus("deploy_123456789");
    }

    @Test
    void testRollbackModel_Success() throws Exception {
        // 准备回滚请求
        ModelRollbackDTO rollbackDTO = ModelRollbackDTO.builder()
            .deploymentId("deploy_123456789")
            .targetModelId("target_model_id")
            .rollbackReason("性能下降")
            .force(false)
            .build();

        // 准备回滚响应
        Map<String, Object> rollbackResponse = new HashMap<>();
        rollbackResponse.put("rollbackId", "rollback_123456789");
        rollbackResponse.put("status", "COMPLETED");

        // 模拟服务返回
        when(modelVersionService.rollbackModel(any(ModelRollbackDTO.class)))
            .thenReturn(rollbackResponse);

        // 执行测试
        mockMvc.perform(post("/api/model/rollback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rollbackDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("回滚成功"))
            .andExpect(jsonPath("$.data.rollbackId").value("rollback_123456789"))
            .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // 验证服务方法被调用
        verify(modelVersionService, times(1)).rollbackModel(any(ModelRollbackDTO.class));
    }
    
    @AfterEach
    void tearDown() {
        if (baseContextMock != null) {
            baseContextMock.close();
        }
    }
}