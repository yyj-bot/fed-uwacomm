package com.feduwacomm.controller;

import com.feduwacomm.dto.InitialModelGenerationDTO;
import com.feduwacomm.dto.ModelDistributionDTO;
import com.feduwacomm.service.InitialModelGenerationService;
import com.feduwacomm.vo.InitialModelInfoVO;
import com.feduwacomm.vo.ModelDistributionTaskVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 初始模型控制器集成测试
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@WebMvcTest(InitialModelController.class)
@DisplayName("初始模型控制器集成测试")
class InitialModelControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private InitialModelGenerationService initialModelService;
    
    private String taskId;
    private String modelId;
    
    @BeforeEach
    void setUp() {
        taskId = "test-task-id";
        modelId = "test-model-id";
    }
    
    // ======================== 模型生成测试 ========================
    
    @Test
    @DisplayName("成功生成随机初始模型")
    @WithMockUser(roles = "RESEARCHER")
    void testGenerateRandomModel_Success() throws Exception {
        // Given
        InitialModelGenerationDTO request = InitialModelGenerationDTO.builder()
                .taskId(taskId)
                .modelType("CNN")
                .generationMethod("RANDOM")
                .architectureParams(Map.of("layers", 3, "units", 128))
                .autoDistribute(false)
                .build();
        
        InitialModelInfoVO mockResponse = createMockModelInfoVO();
        when(initialModelService.generateRandomModel(any(InitialModelGenerationDTO.class), anyString()))
                .thenReturn(mockResponse);
        
        // When & Then
        mockMvc.perform(post("/api/initial-models/generate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.id").value(modelId))
                .andExpect(jsonPath("$.data.taskId").value(taskId))
                .andExpect(jsonPath("$.data.modelType").value("CNN"))
                .andExpect(jsonPath("$.data.generationMethod").value("RANDOM"))
                .andExpect(jsonPath("$.data.status").value("GENERATING"));
        
        verify(initialModelService).generateRandomModel(any(InitialModelGenerationDTO.class), anyString());
    }
    
    @Test
    @DisplayName("无权限用户生成模型应该返回403")
    @WithMockUser(roles = "VIEWER")
    void testGenerateRandomModel_Forbidden() throws Exception {
        // Given
        InitialModelGenerationDTO request = InitialModelGenerationDTO.builder()
                .taskId(taskId)
                .modelType("CNN")
                .generationMethod("RANDOM")
                .build();
        
        // When & Then
        mockMvc.perform(post("/api/initial-models/generate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        
        verify(initialModelService, never()).generateRandomModel(any(), anyString());
    }
    
    @Test
    @DisplayName("请求参数验证失败应该返回400")
    @WithMockUser(roles = "RESEARCHER")
    void testGenerateRandomModel_InvalidRequest() throws Exception {
        // Given
        InitialModelGenerationDTO invalidRequest = InitialModelGenerationDTO.builder()
                .modelType("CNN") // 缺少taskId
                .generationMethod("RANDOM")
                .build();
        
        // When & Then
        mockMvc.perform(post("/api/initial-models/generate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
        
        verify(initialModelService, never()).generateRandomModel(any(), anyString());
    }
    
    @Test
    @DisplayName("成功上传自定义模型")
    @WithMockUser(roles = "RESEARCHER")
    void testUploadCustomModel_Success() throws Exception {
        // Given
        InitialModelGenerationDTO request = InitialModelGenerationDTO.builder()
                .taskId(taskId)
                .modelType("LSTM")
                .generationMethod("CUSTOM_UPLOAD")
                .build();
        
        MockMultipartFile modelFile = new MockMultipartFile(
                "modelFile", "test-model.h5", "application/octet-stream",
                "mock model content".getBytes());
        
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", "application/json",
                objectMapper.writeValueAsBytes(request));
        
        InitialModelInfoVO mockResponse = createMockModelInfoVO();
        mockResponse.setGenerationMethod("CUSTOM_UPLOAD");
        mockResponse.setModelType("LSTM");
        
        when(initialModelService.uploadCustomModel(any(InitialModelGenerationDTO.class), 
                any(), anyString())).thenReturn(mockResponse);
        
        // When & Then
        mockMvc.perform(multipart("/api/initial-models/upload")
                        .file(modelFile)
                        .file(requestPart)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.generationMethod").value("CUSTOM_UPLOAD"))
                .andExpect(jsonPath("$.data.modelType").value("LSTM"));
        
        verify(initialModelService).uploadCustomModel(any(InitialModelGenerationDTO.class), any(), anyString());
    }
    
    // ======================== 模型查询测试 ========================
    
    @Test
    @DisplayName("成功获取初始模型详情")
    @WithMockUser(roles = "VIEWER")
    void testGetInitialModelInfo_Success() throws Exception {
        // Given
        InitialModelInfoVO mockResponse = createMockModelInfoVO();
        when(initialModelService.getInitialModelInfo(modelId)).thenReturn(mockResponse);
        
        // When & Then
        mockMvc.perform(get("/api/initial-models/{modelId}", modelId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(modelId))
                .andExpect(jsonPath("$.data.taskId").value(taskId));
        
        verify(initialModelService).getInitialModelInfo(modelId);
    }
    
    @Test
    @DisplayName("成功获取任务的初始模型列表")
    @WithMockUser(roles = "VIEWER")
    void testGetInitialModelsByTask_Success() throws Exception {
        // Given
        List<InitialModelInfoVO> mockResponse = Arrays.asList(
                createMockModelInfoVO(),
                createMockModelInfoVO()
        );
        when(initialModelService.getInitialModelsByTaskId(taskId)).thenReturn(mockResponse);
        
        // When & Then
        mockMvc.perform(get("/api/initial-models/task/{taskId}", taskId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
        
        verify(initialModelService).getInitialModelsByTaskId(taskId);
    }
    
    @Test
    @DisplayName("成功获取任务的当前初始模型")
    @WithMockUser(roles = "VIEWER")
    void testGetCurrentInitialModel_Success() throws Exception {
        // Given
        InitialModelInfoVO mockResponse = createMockModelInfoVO();
        mockResponse.setStatus("READY");
        when(initialModelService.getCurrentInitialModel(taskId)).thenReturn(mockResponse);
        
        // When & Then
        mockMvc.perform(get("/api/initial-models/task/{taskId}/current", taskId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("READY"));
        
        verify(initialModelService).getCurrentInitialModel(taskId);
    }
    
    @Test
    @DisplayName("获取模型生成进度")
    @WithMockUser(roles = "VIEWER")
    void testGetGenerationProgress_Success() throws Exception {
        // Given
        Double mockProgress = 75.5;
        when(initialModelService.getGenerationProgress(modelId)).thenReturn(mockProgress);
        
        // When & Then
        mockMvc.perform(get("/api/initial-models/{modelId}/progress", modelId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(75.5));
        
        verify(initialModelService).getGenerationProgress(modelId);
    }
    
    // ======================== 模型分发测试 ========================
    
    @Test
    @DisplayName("成功分发初始模型")
    @WithMockUser(roles = "RESEARCHER")
    void testDistributeModel_Success() throws Exception {
        // Given
        ModelDistributionDTO request = ModelDistributionDTO.builder()
                .modelId(modelId)
                .targetVmIds(Arrays.asList("vm1", "vm2", "vm3"))
                .verifyIntegrity(true)
                .build();
        
        ModelDistributionTaskVO mockResponse = createMockDistributionTaskVO();
        when(initialModelService.distributeModel(any(ModelDistributionDTO.class))).thenReturn(mockResponse);
        
        // When & Then
        mockMvc.perform(post("/api/initial-models/distribute")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.modelId").value(modelId))
                .andExpect(jsonPath("$.data.targetVmCount").value(3));
        
        verify(initialModelService).distributeModel(any(ModelDistributionDTO.class));
    }
    
    @Test
    @DisplayName("成功获取模型分发状态")
    @WithMockUser(roles = "VIEWER")
    void testGetDistributionStatus_Success() throws Exception {
        // Given
        ModelDistributionTaskVO mockResponse = createMockDistributionTaskVO();
        when(initialModelService.getDistributionStatus(modelId)).thenReturn(mockResponse);
        
        // When & Then
        mockMvc.perform(get("/api/initial-models/{modelId}/distribution", modelId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.modelId").value(modelId));
        
        verify(initialModelService).getDistributionStatus(modelId);
    }
    
    @Test
    @DisplayName("成功重试模型分发")
    @WithMockUser(roles = "RESEARCHER")
    void testRetryDistribution_Success() throws Exception {
        // Given
        ModelDistributionTaskVO mockResponse = createMockDistributionTaskVO();
        when(initialModelService.retryDistribution(eq(modelId), anyList())).thenReturn(mockResponse);
        
        // When & Then
        mockMvc.perform(post("/api/initial-models/{modelId}/distribution/retry", modelId)
                        .with(csrf())
                        .param("vmIds", "vm1", "vm2"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        verify(initialModelService).retryDistribution(eq(modelId), anyList());
    }
    
    // ======================== 模型验证测试 ========================
    
    @Test
    @DisplayName("成功验证模型完整性")
    @WithMockUser(roles = "RESEARCHER")
    void testVerifyModelIntegrity_Success() throws Exception {
        // Given
        when(initialModelService.verifyModelIntegrity(modelId, "vm1")).thenReturn(true);
        
        // When & Then
        mockMvc.perform(post("/api/initial-models/{modelId}/verify", modelId)
                        .with(csrf())
                        .param("vmId", "vm1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
        
        verify(initialModelService).verifyModelIntegrity(modelId, "vm1");
    }
    
    @Test
    @DisplayName("成功验证自定义模型文件")
    @WithMockUser(roles = "RESEARCHER")
    void testValidateCustomModel_Success() throws Exception {
        // Given
        MockMultipartFile modelFile = new MockMultipartFile(
                "modelFile", "test.h5", "application/octet-stream",
                "valid content".getBytes());
        
        InitialModelGenerationService.ValidationResult mockResult = 
                InitialModelGenerationService.ValidationResult.success();
        when(initialModelService.validateCustomModel(any(), eq("CNN"))).thenReturn(mockResult);
        
        // When & Then
        mockMvc.perform(multipart("/api/initial-models/validate")
                        .file(modelFile)
                        .param("modelType", "CNN")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.valid").value(true));
        
        verify(initialModelService).validateCustomModel(any(), eq("CNN"));
    }
    
    // ======================== 模型管理测试 ========================
    
    @Test
    @DisplayName("成功删除初始模型")
    @WithMockUser(roles = "RESEARCHER")
    void testDeleteInitialModel_Success() throws Exception {
        // Given
        when(initialModelService.deleteInitialModel(modelId, true)).thenReturn(true);
        
        // When & Then
        mockMvc.perform(delete("/api/initial-models/{modelId}", modelId)
                        .with(csrf())
                        .param("deleteFile", "true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
        
        verify(initialModelService).deleteInitialModel(modelId, true);
    }
    
    @Test
    @DisplayName("只有ADMIN用户可以清理过期模型")
    @WithMockUser(roles = "ADMIN")
    void testCleanupFailedModels_Success() throws Exception {
        // Given
        when(initialModelService.cleanupFailedModels(7)).thenReturn(3);
        
        // When & Then
        mockMvc.perform(post("/api/initial-models/cleanup")
                        .with(csrf())
                        .param("daysOld", "7"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(3));
        
        verify(initialModelService).cleanupFailedModels(7);
    }
    
    @Test
    @DisplayName("非ADMIN用户清理模型应该返回403")
    @WithMockUser(roles = "RESEARCHER")
    void testCleanupFailedModels_Forbidden() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/initial-models/cleanup")
                        .with(csrf()))
                .andExpect(status().isForbidden());
        
        verify(initialModelService, never()).cleanupFailedModels(anyInt());
    }
    
    // ======================== 辅助方法 ========================
    
    private InitialModelInfoVO createMockModelInfoVO() {
        return InitialModelInfoVO.builder()
                .id(modelId)
                .taskId(taskId)
                .modelType("CNN")
                .generationMethod("RANDOM")
                .modelSize(2048L)
                .modelSizeFormatted("2.00 KB")
                .status("GENERATING")
                .statusDescription("生成中")
                .filePath("/tmp/models/test-model.h5")
                .checksum("test-checksum")
                .createdAt(LocalDateTime.now())
                .createdBy("test-user")
                .updatedAt(LocalDateTime.now())
                .architectureParams(Map.of("layers", 3, "units", 128))
                .build();
    }
    
    private ModelDistributionTaskVO createMockDistributionTaskVO() {
        ModelDistributionTaskVO.DistributionStats stats = ModelDistributionTaskVO.DistributionStats.builder()
                .total(3)
                .pending(1)
                .inProgress(1)
                .completed(1)
                .failed(0)
                .progressPercentage(33.3)
                .build();
        
        return ModelDistributionTaskVO.builder()
                .taskId("distribution-task-id")
                .modelId(modelId)
                .modelType("CNN")
                .targetVmCount(3)
                .stats(stats)
                .createdAt(LocalDateTime.now())
                .build();
    }
}