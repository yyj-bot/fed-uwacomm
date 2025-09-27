package com.feduwacomm.service;

import com.feduwacomm.dto.InitialModelGenerationDTO;
import com.feduwacomm.entity.InitialModel;
import com.feduwacomm.enums.GenerationMethod;
import com.feduwacomm.enums.InitialModelStatus;
import com.feduwacomm.enums.ModelType;
import com.feduwacomm.mapper.InitialModelMapper;
import com.feduwacomm.mapper.ModelDistributionMapper;
import com.feduwacomm.service.impl.InitialModelGenerationServiceImpl;
import com.feduwacomm.utils.UuidUtil;
import com.feduwacomm.vo.InitialModelInfoVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 初始模型生成服务单元测试
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("初始模型生成服务测试")
class InitialModelGenerationServiceTest {
    
    @Mock
    private InitialModelMapper initialModelMapper;
    
    @Mock
    private ModelDistributionMapper modelDistributionMapper;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private UuidUtil uuidUtil;
    
    @InjectMocks
    private InitialModelGenerationServiceImpl initialModelService;
    
    private String taskId;
    private String modelId;
    private String userId;
    
    @BeforeEach
    void setUp() {
        taskId = "test-task-id";
        modelId = "test-model-id";
        userId = "test-user-id";
    }
    
    // ======================== 模型生成测试 ========================
    
    @Test
    @DisplayName("成功生成初始模型")
    void testGenerateInitialModel_Success() throws JsonProcessingException {
        // Given
        InitialModelGenerationDTO request = InitialModelGenerationDTO.builder()
                .taskId(taskId)
                .modelType("NEURAL_NETWORK")
                .generationMethod("RANDOM")
                .architectureParams(Map.of("layers", 3, "units", 128))
                .build();
        
        when(uuidUtil.generateUuid()).thenReturn(modelId);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(initialModelMapper.insertInitialModel(any(InitialModel.class))).thenReturn(1);
        
        // When
        InitialModelInfoVO result = initialModelService.generateInitialModel(request, userId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(modelId);
        assertThat(result.getTaskId()).isEqualTo(taskId);
        assertThat(result.getModelType()).isEqualTo("NEURAL_NETWORK");
        assertThat(result.getStatus()).isEqualTo("GENERATING");
        
        verify(initialModelMapper).insertInitialModel(any(InitialModel.class));
        verify(objectMapper).writeValueAsString(request.getArchitectureParams());
    }
    
    @Test
    @DisplayName("成功获取初始模型信息")
    void testGetModelInfo_Success() {
        // Given
        InitialModel mockModel = createMockInitialModel();
        when(initialModelMapper.selectById(modelId)).thenReturn(mockModel);
        when(modelDistributionMapper.getDistributionProgress(modelId)).thenReturn(new HashMap<>());
        
        // When
        InitialModelInfoVO result = initialModelService.getModelInfo(modelId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(modelId);
        assertThat(result.getTaskId()).isEqualTo(taskId);
        assertThat(result.getModelType()).isEqualTo("NEURAL_NETWORK");
        
        verify(initialModelMapper).selectById(modelId);
    }
    
    @Test
    @DisplayName("获取不存在的模型信息应该失败")
    void testGetModelInfo_NotExists_ShouldFail() {
        // Given
        when(initialModelMapper.selectById(modelId)).thenReturn(null);
        
        // When & Then
        assertThatThrownBy(() -> initialModelService.getModelInfo(modelId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("模型不存在");
        
        verify(initialModelMapper).selectById(modelId);
    }
    
    @Test
    @DisplayName("成功获取任务的初始模型列表")
    void testGetTaskModels_Success() {
        // Given
        List<InitialModel> mockModels = Arrays.asList(
                createMockInitialModel(),
                createMockInitialModel()
        );
        when(initialModelMapper.selectByTaskId(taskId)).thenReturn(mockModels);
        when(modelDistributionMapper.getDistributionProgress(anyString())).thenReturn(new HashMap<>());
        
        // When
        List<InitialModelInfoVO> result = initialModelService.getTaskModels(taskId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        
        verify(initialModelMapper).selectByTaskId(taskId);
        verify(modelDistributionMapper, times(2)).getDistributionProgress(anyString());
    }
    
    // ======================== 模型管理测试 ========================
    
    @Test
    @DisplayName("成功删除初始模型")
    void testDeleteModel_Success() {
        // Given
        InitialModel mockModel = createMockInitialModel();
        when(initialModelMapper.selectById(modelId)).thenReturn(mockModel);
        when(modelDistributionMapper.deleteByModelId(modelId)).thenReturn(1);
        when(initialModelMapper.deleteById(modelId)).thenReturn(1);
        
        // When
        boolean result = initialModelService.deleteModel(modelId, userId);
        
        // Then
        assertThat(result).isTrue();
        
        verify(initialModelMapper).selectById(modelId);
        verify(modelDistributionMapper).deleteByModelId(modelId);
        verify(initialModelMapper).deleteById(modelId);
    }
    
    @Test
    @DisplayName("删除不存在的模型应该返回false")
    void testDeleteModel_NotExists_ShouldReturnFalse() {
        // Given
        when(initialModelMapper.selectById(modelId)).thenReturn(null);
        
        // When
        boolean result = initialModelService.deleteModel(modelId, userId);
        
        // Then
        assertThat(result).isFalse();
        
        verify(initialModelMapper).selectById(modelId);
        verify(modelDistributionMapper, never()).deleteByModelId(anyString());
        verify(initialModelMapper, never()).deleteById(anyString());
    }
    
    @Test
    @DisplayName("成功更新模型状态")
    void testUpdateModelStatus_Success() {
        // Given
        when(initialModelMapper.updateStatus(modelId, "READY")).thenReturn(1);
        
        // When
        boolean result = initialModelService.updateModelStatus(modelId, "READY");
        
        // Then
        assertThat(result).isTrue();
        
        verify(initialModelMapper).updateStatus(modelId, "READY");
    }
    
    @Test
    @DisplayName("成功验证模型完整性")
    void testValidateModelIntegrity_NoModelData_ShouldReturnFalse() {
        // Given
        InitialModel mockModel = createMockInitialModel();
        mockModel.setModelData(null); // 没有模型数据
        when(initialModelMapper.selectById(modelId)).thenReturn(mockModel);

        // When
        boolean result = initialModelService.validateModelIntegrity(modelId);

        // Then
        assertThat(result).isFalse();

        verify(initialModelMapper).selectById(modelId);
    }
    
    @Test
    @DisplayName("获取模型生成进度")
    void testGetGenerationProgress_GeneratingStatus() {
        // Given
        InitialModel mockModel = createMockInitialModel();
        mockModel.setStatus(InitialModelStatus.fromCode("GENERATING"));
        when(initialModelMapper.selectById(modelId)).thenReturn(mockModel);
        
        // When
        Integer progress = initialModelService.getGenerationProgress(modelId);
        
        // Then
        assertThat(progress).isEqualTo(50);
        
        verify(initialModelMapper).selectById(modelId);
    }
    
    @Test
    @DisplayName("获取模型生成进度 - 已就绪状态")
    void testGetGenerationProgress_ReadyStatus() {
        // Given
        InitialModel mockModel = createMockInitialModel();
        mockModel.setStatus(InitialModelStatus.fromCode("READY"));
        when(initialModelMapper.selectById(modelId)).thenReturn(mockModel);
        
        // When
        Integer progress = initialModelService.getGenerationProgress(modelId);
        
        // Then
        assertThat(progress).isEqualTo(100);
        
        verify(initialModelMapper).selectById(modelId);
    }
    
    @Test
    @DisplayName("成功取消模型生成")
    void testCancelGeneration_Success() {
        // Given
        InitialModel mockModel = createMockInitialModel();
        mockModel.setStatus(InitialModelStatus.fromCode("GENERATING"));
        when(initialModelMapper.selectById(modelId)).thenReturn(mockModel);
        when(initialModelMapper.updateStatus(modelId, "FAILED")).thenReturn(1);
        
        // When
        boolean result = initialModelService.cancelGeneration(modelId, userId);
        
        // Then
        assertThat(result).isTrue();
        
        verify(initialModelMapper).selectById(modelId);
        verify(initialModelMapper).updateStatus(modelId, "FAILED");
    }
    
    @Test
    @DisplayName("成功清理过期失败模型")
    void testCleanupFailedModels_Success() {
        // Given
        List<InitialModel> failedModels = Arrays.asList(
                createMockInitialModel(),
                createMockInitialModel()
        );
        when(initialModelMapper.selectFailedModelsOlderThan(7)).thenReturn(failedModels);
        when(initialModelMapper.selectById(anyString())).thenReturn(createMockInitialModel());
        when(modelDistributionMapper.deleteByModelId(anyString())).thenReturn(1);
        when(initialModelMapper.deleteById(anyString())).thenReturn(1);
        
        // When
        int result = initialModelService.cleanupFailedModels(7);
        
        // Then
        assertThat(result).isEqualTo(2);
        
        verify(initialModelMapper).selectFailedModelsOlderThan(7);
        verify(initialModelMapper, times(2)).selectById(anyString());
        verify(modelDistributionMapper, times(2)).deleteByModelId(anyString());
        verify(initialModelMapper, times(2)).deleteById(anyString());
    }
    
    @Test
    @DisplayName("获取模型生成统计信息")
    void testGetGenerationStats_Success() {
        // Given
        List<InitialModel> mockModels = Arrays.asList(
                createMockModelWithStatus("READY"),
                createMockModelWithStatus("GENERATING"),
                createMockModelWithStatus("FAILED")
        );
        when(initialModelMapper.selectByTaskId(taskId)).thenReturn(mockModels);
        
        // When
        InitialModelInfoVO.ModelGenerationStats result = initialModelService.getGenerationStats(taskId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalModels()).isEqualTo(3);
        assertThat(result.getReady()).isEqualTo(1);
        assertThat(result.getGenerating()).isEqualTo(1);
        assertThat(result.getFailed()).isEqualTo(1);
        
        verify(initialModelMapper).selectByTaskId(taskId);
    }
    
    // ======================== 辅助方法 ========================
    
    private InitialModel createMockInitialModel() {
        return InitialModel.builder()
                .id(modelId)
                .taskId(taskId)
                .modelType(ModelType.fromCode("NEURAL_NETWORK"))
                .generationMethod(GenerationMethod.fromCode("RANDOM"))
                .modelSize(1024L)
                .status(InitialModelStatus.fromCode("READY"))
                .modelData("{\"r2\":-0.001,\"mse\":1.0,\"model_parameters\":{\"hidden_layers\":[128,64,32]}}")
                .architectureParams("{\"layers\":3,\"units\":128}")
                .createdAt(LocalDateTime.now())
                .createdBy(userId)
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    private InitialModel createMockModelWithStatus(String status) {
        InitialModel model = createMockInitialModel();
        model.setStatus(InitialModelStatus.fromCode(status));
        return model;
    }
}