package com.feduwacomm.service;

import com.feduwacomm.dto.InitialModelGenerationDTO;
import com.feduwacomm.dto.ModelDistributionDTO;
import com.feduwacomm.entity.InitialModel;
import com.feduwacomm.entity.VmInstance;
import com.feduwacomm.mapper.InitialModelMapper;
import com.feduwacomm.mapper.ModelDistributionMapper;
import com.feduwacomm.service.impl.InitialModelGenerationServiceImpl;
import com.feduwacomm.vo.InitialModelInfoVO;
import com.feduwacomm.vo.ModelDistributionTaskVO;
import com.feduwacomm.vo.VmDetailVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

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
    private FederatedTaskService federatedTaskService;
    
    @Mock
    private VmInstanceService vmInstanceService;
    
    @Mock
    private ObjectMapper objectMapper;
    
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
        
        // 设置配置属性
        ReflectionTestUtils.setField(initialModelService, "modelStoragePath", "/tmp/models");
        ReflectionTestUtils.setField(initialModelService, "maxModelSize", "100MB");
        ReflectionTestUtils.setField(initialModelService, "allowedModelTypes", "h5,pkl,pth,onnx");
    }
    
    // ======================== 模型生成测试 ========================
    
    @Test
    @DisplayName("成功生成随机初始模型")
    void testGenerateRandomModel_Success() throws Exception {
        // Given
        InitialModelGenerationDTO request = InitialModelGenerationDTO.builder()
                .taskId(taskId)
                .modelType("CNN")
                .generationMethod("RANDOM")
                .architectureParams(Map.of("layers", 3, "units", 128))
                .autoDistribute(false)
                .build();
        
        when(federatedTaskService.taskExists(taskId)).thenReturn(true);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"layers\":3,\"units\":128}");
        when(initialModelMapper.insertInitialModel(any(InitialModel.class))).thenReturn(1);
        
        // When
        InitialModelInfoVO result = initialModelService.generateRandomModel(request, userId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTaskId()).isEqualTo(taskId);
        assertThat(result.getModelType()).isEqualTo("CNN");
        assertThat(result.getGenerationMethod()).isEqualTo("RANDOM");
        assertThat(result.getStatus()).isEqualTo("GENERATING");
        assertThat(result.getCreatedBy()).isEqualTo(userId);
        
        verify(federatedTaskService).taskExists(taskId);
        verify(initialModelMapper).insertInitialModel(any(InitialModel.class));
        verify(objectMapper).writeValueAsString(request.getArchitectureParams());
    }
    
    @Test
    @DisplayName("任务不存在时生成模型应该失败")
    void testGenerateRandomModel_TaskNotExists_ShouldFail() {
        // Given
        InitialModelGenerationDTO request = InitialModelGenerationDTO.builder()
                .taskId(taskId)
                .modelType("CNN")
                .generationMethod("RANDOM")
                .build();
        
        when(federatedTaskService.taskExists(taskId)).thenReturn(false);
        
        // When & Then
        assertThatThrownBy(() -> initialModelService.generateRandomModel(request, userId))
                .hasMessageContaining("任务不存在");
        
        verify(federatedTaskService).taskExists(taskId);
        verify(initialModelMapper, never()).insertInitialModel(any());
    }
    
    @Test
    @DisplayName("成功上传自定义初始模型")
    void testUploadCustomModel_Success() throws Exception {
        // Given
        InitialModelGenerationDTO request = InitialModelGenerationDTO.builder()
                .taskId(taskId)
                .modelType("LSTM")
                .generationMethod("CUSTOM_UPLOAD")
                .build();
        
        MockMultipartFile mockFile = new MockMultipartFile(
                "modelFile", "test-model.h5", "application/octet-stream", 
                "mock model content".getBytes());
        
        when(federatedTaskService.taskExists(taskId)).thenReturn(true);
        when(initialModelMapper.insertInitialModel(any(InitialModel.class))).thenReturn(1);
        
        // When
        InitialModelInfoVO result = initialModelService.uploadCustomModel(request, mockFile, userId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTaskId()).isEqualTo(taskId);
        assertThat(result.getModelType()).isEqualTo("LSTM");
        assertThat(result.getGenerationMethod()).isEqualTo("CUSTOM_UPLOAD");
        assertThat(result.getCreatedBy()).isEqualTo(userId);
        
        verify(federatedTaskService).taskExists(taskId);
        verify(initialModelMapper).insertInitialModel(any(InitialModel.class));
    }
    
    @Test
    @DisplayName("上传空文件应该失败")
    void testUploadCustomModel_EmptyFile_ShouldFail() {
        // Given
        InitialModelGenerationDTO request = InitialModelGenerationDTO.builder()
                .taskId(taskId)
                .modelType("LSTM")
                .generationMethod("CUSTOM_UPLOAD")
                .build();
        
        MockMultipartFile emptyFile = new MockMultipartFile(
                "modelFile", "empty.h5", "application/octet-stream", new byte[0]);
        
        when(federatedTaskService.taskExists(taskId)).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> initialModelService.uploadCustomModel(request, emptyFile, userId))
                .hasMessageContaining("模型文件验证失败");
        
        verify(federatedTaskService).taskExists(taskId);
        verify(initialModelMapper, never()).insertInitialModel(any());
    }
    
    // ======================== 模型查询测试 ========================
    
    @Test
    @DisplayName("成功获取初始模型信息")
    void testGetInitialModelInfo_Success() {
        // Given
        InitialModel mockModel = createMockInitialModel();
        when(initialModelMapper.selectById(modelId)).thenReturn(mockModel);
        
        Map<String, Object> distributionStats = Map.of(
                "total", 3, "completed", 2, "inProgress", 1, "failed", 0);
        when(modelDistributionMapper.getDistributionProgress(modelId)).thenReturn(distributionStats);
        
        // When
        InitialModelInfoVO result = initialModelService.getInitialModelInfo(modelId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(modelId);
        assertThat(result.getTaskId()).isEqualTo(taskId);
        assertThat(result.getModelType()).isEqualTo("CNN");
        assertThat(result.getDistributionStats()).isNotNull();
        assertThat(result.getDistributionStats().getTotal()).isEqualTo(3);
        assertThat(result.getDistributionStats().getCompleted()).isEqualTo(2);
        
        verify(initialModelMapper).selectById(modelId);
        verify(modelDistributionMapper).getDistributionProgress(modelId);
    }
    
    @Test
    @DisplayName("获取不存在的模型信息应该失败")
    void testGetInitialModelInfo_NotExists_ShouldFail() {
        // Given
        when(initialModelMapper.selectById(modelId)).thenReturn(null);
        
        // When & Then
        assertThatThrownBy(() -> initialModelService.getInitialModelInfo(modelId))
                .hasMessageContaining("初始模型不存在");
        
        verify(initialModelMapper).selectById(modelId);
    }
    
    @Test
    @DisplayName("成功获取任务的初始模型列表")
    void testGetInitialModelsByTaskId_Success() {
        // Given
        List<InitialModel> mockModels = Arrays.asList(
                createMockInitialModel(),
                createMockInitialModel()
        );
        when(initialModelMapper.selectByTaskId(taskId)).thenReturn(mockModels);
        
        // When
        List<InitialModelInfoVO> result = initialModelService.getInitialModelsByTaskId(taskId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTaskId()).isEqualTo(taskId);
        
        verify(initialModelMapper).selectByTaskId(taskId);
    }
    
    @Test
    @DisplayName("成功获取任务的当前初始模型")
    void testGetCurrentInitialModel_Success() {
        // Given
        InitialModel readyModel = createMockInitialModel();
        readyModel.setStatus("READY");
        readyModel.setCreatedAt(LocalDateTime.now());
        
        List<InitialModel> mockModels = Arrays.asList(readyModel);
        when(initialModelMapper.selectByTaskIdAndMethod(taskId, "READY")).thenReturn(mockModels);
        
        // When
        InitialModelInfoVO result = initialModelService.getCurrentInitialModel(taskId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("READY");
        
        verify(initialModelMapper).selectByTaskIdAndMethod(taskId, "READY");
    }
    
    @Test
    @DisplayName("没有当前模型时应该返回null")
    void testGetCurrentInitialModel_NoCurrentModel_ShouldReturnNull() {
        // Given
        when(initialModelMapper.selectByTaskIdAndMethod(taskId, "READY")).thenReturn(Collections.emptyList());
        
        // When
        InitialModelInfoVO result = initialModelService.getCurrentInitialModel(taskId);
        
        // Then
        assertThat(result).isNull();
        
        verify(initialModelMapper).selectByTaskIdAndMethod(taskId, "READY");
    }
    
    // ======================== 模型分发测试 ========================
    
    @Test
    @DisplayName("成功分发模型到虚拟机")
    void testDistributeModel_Success() {
        // Given
        ModelDistributionDTO request = ModelDistributionDTO.builder()
                .modelId(modelId)
                .targetVmIds(Arrays.asList("vm1", "vm2", "vm3"))
                .build();
        
        InitialModel mockModel = createMockInitialModel();
        mockModel.setStatus("READY");
        when(initialModelMapper.selectById(modelId)).thenReturn(mockModel);
        
        // Mock VM可用性检查
        when(vmInstanceService.getVmDetail(eq("vm1"), any())).thenReturn(createMockVmDetailVO("vm1"));
        when(vmInstanceService.getVmDetail(eq("vm2"), any())).thenReturn(createMockVmDetailVO("vm2"));
        when(vmInstanceService.getVmDetail(eq("vm3"), any())).thenReturn(createMockVmDetailVO("vm3"));
        
        when(modelDistributionMapper.batchInsertModelDistributions(anyList())).thenReturn(3);
        
        // Mock分发状态查询
        List<Map<String, Object>> distributionDetails = Arrays.asList(
                createMockDistributionDetail("vm1", "PENDING"),
                createMockDistributionDetail("vm2", "PENDING"),
                createMockDistributionDetail("vm3", "PENDING")
        );
        when(modelDistributionMapper.selectByModelIdWithVmInfo(modelId)).thenReturn(distributionDetails);
        
        // When
        ModelDistributionTaskVO result = initialModelService.distributeModel(request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getModelId()).isEqualTo(modelId);
        assertThat(result.getTargetVmCount()).isEqualTo(3);
        assertThat(result.getStats().getTotal()).isEqualTo(3);
        
        verify(initialModelMapper).selectById(modelId);
        verify(modelDistributionMapper).batchInsertModelDistributions(anyList());
    }
    
    @Test
    @DisplayName("模型状态不是READY时分发应该失败")
    void testDistributeModel_ModelNotReady_ShouldFail() {
        // Given
        ModelDistributionDTO request = ModelDistributionDTO.builder()
                .modelId(modelId)
                .targetVmIds(Arrays.asList("vm1"))
                .build();
        
        InitialModel mockModel = createMockInitialModel();
        mockModel.setStatus("GENERATING");
        when(initialModelMapper.selectById(modelId)).thenReturn(mockModel);
        
        // When & Then
        assertThatThrownBy(() -> initialModelService.distributeModel(request))
                .hasMessageContaining("模型状态不是就绪状态");
        
        verify(initialModelMapper).selectById(modelId);
        verify(modelDistributionMapper, never()).batchInsertModelDistributions(anyList());
    }
    
    @Test
    @DisplayName("没有可用虚拟机时分发应该失败")
    void testDistributeModel_NoAvailableVms_ShouldFail() {
        // Given
        ModelDistributionDTO request = ModelDistributionDTO.builder()
                .modelId(modelId)
                .targetVmIds(Arrays.asList("vm1", "vm2"))
                .build();
        
        InitialModel mockModel = createMockInitialModel();
        mockModel.setStatus("READY");
        when(initialModelMapper.selectById(modelId)).thenReturn(mockModel);
        
        // Mock VM不可用
        when(vmInstanceService.getVmDetail(eq("vm1"), any())).thenReturn(null);
        when(vmInstanceService.getVmDetail(eq("vm2"), any())).thenReturn(null);
        
        // When & Then
        assertThatThrownBy(() -> initialModelService.distributeModel(request))
                .hasMessageContaining("没有可用的虚拟机");
        
        verify(initialModelMapper).selectById(modelId);
        verify(modelDistributionMapper, never()).batchInsertModelDistributions(anyList());
    }
    
    // ======================== 模型验证测试 ========================
    
    @Test
    @DisplayName("成功验证有效的自定义模型文件")
    void testValidateCustomModel_ValidFile_Success() {
        // Given
        MockMultipartFile validFile = new MockMultipartFile(
                "modelFile", "valid-model.h5", "application/octet-stream", 
                "valid model content".getBytes());
        
        // When
        InitialModelGenerationService.ValidationResult result = 
                initialModelService.validateCustomModel(validFile, "CNN");
        
        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrorMessage()).isNull();
    }
    
    @Test
    @DisplayName("验证空文件应该失败")
    void testValidateCustomModel_EmptyFile_ShouldFail() {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "modelFile", "empty.h5", "application/octet-stream", new byte[0]);
        
        // When
        InitialModelGenerationService.ValidationResult result = 
                initialModelService.validateCustomModel(emptyFile, "CNN");
        
        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("模型文件为空");
    }
    
    @Test
    @DisplayName("验证不支持的文件类型应该失败")
    void testValidateCustomModel_UnsupportedFileType_ShouldFail() {
        // Given
        MockMultipartFile unsupportedFile = new MockMultipartFile(
                "modelFile", "model.txt", "text/plain", 
                "not a model file".getBytes());
        
        // When
        InitialModelGenerationService.ValidationResult result = 
                initialModelService.validateCustomModel(unsupportedFile, "CNN");
        
        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("不支持的文件类型");
    }
    
    // ======================== 模型管理测试 ========================
    
    @Test
    @DisplayName("成功删除初始模型")
    void testDeleteInitialModel_Success() {
        // Given
        InitialModel mockModel = createMockInitialModel();
        when(initialModelMapper.selectById(modelId)).thenReturn(mockModel);
        when(modelDistributionMapper.deleteByModelId(modelId)).thenReturn(3);
        when(initialModelMapper.deleteById(modelId)).thenReturn(1);
        
        // When
        boolean result = initialModelService.deleteInitialModel(modelId, false);
        
        // Then
        assertThat(result).isTrue();
        
        verify(initialModelMapper).selectById(modelId);
        verify(modelDistributionMapper).deleteByModelId(modelId);
        verify(initialModelMapper).deleteById(modelId);
    }
    
    @Test
    @DisplayName("删除不存在的模型应该返回false")
    void testDeleteInitialModel_NotExists_ShouldReturnFalse() {
        // Given
        when(initialModelMapper.selectById(modelId)).thenReturn(null);
        
        // When
        boolean result = initialModelService.deleteInitialModel(modelId, false);
        
        // Then
        assertThat(result).isFalse();
        
        verify(initialModelMapper).selectById(modelId);
        verify(modelDistributionMapper, never()).deleteByModelId(anyString());
        verify(initialModelMapper, never()).deleteById(anyString());
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
        
        // Mock删除操作
        when(initialModelMapper.selectById(anyString())).thenReturn(createMockInitialModel());
        when(modelDistributionMapper.deleteByModelId(anyString())).thenReturn(0);
        when(initialModelMapper.deleteById(anyString())).thenReturn(1);
        
        // When
        int result = initialModelService.cleanupFailedModels(7);
        
        // Then
        assertThat(result).isEqualTo(2);
        
        verify(initialModelMapper).selectFailedModelsOlderThan(7);
    }
    
    // ======================== 辅助方法 ========================
    
    private InitialModel createMockInitialModel() {
        return InitialModel.builder()
                .id(modelId)
                .taskId(taskId)
                .modelType("CNN")
                .generationMethod("RANDOM")
                .modelSize(1024L)
                .status("READY")
                .filePath("/tmp/models/test-model.h5")
                .checksum("test-checksum")
                .createdAt(LocalDateTime.now())
                .createdBy(userId)
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    private VmInstance createMockVmInstance(String vmId) {
        VmInstance vm = new VmInstance();
        vm.setId(vmId);
        vm.setName("VM-" + vmId);
        vm.setIpAddress("192.168.1." + vmId.charAt(vmId.length() - 1));
        vm.setStatus("RUNNING");
        vm.setConnectionStatus("CONNECTED");
        return vm;
    }

    private VmDetailVO createMockVmDetailVO(String vmId) {
        return VmDetailVO.builder()
                .vmId(vmId)
                .name("VM-" + vmId)
                .ipAddress("192.168.1." + vmId.charAt(vmId.length() - 1))
                .status("RUNNING")
                .connectionStatus("CONNECTED")
                .build();
    }
    
    private Map<String, Object> createMockDistributionDetail(String vmId, String status) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("vm_id", vmId);
        detail.put("vm_name", "VM-" + vmId);
        detail.put("vm_ip", "192.168.1." + vmId.charAt(vmId.length() - 1));
        detail.put("distribution_status", status);
        detail.put("checksum_verified", false);
        return detail;
    }
}