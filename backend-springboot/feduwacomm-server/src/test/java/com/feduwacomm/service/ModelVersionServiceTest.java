package com.feduwacomm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.common.exception.BusinessException;
import com.feduwacomm.dto.*;
import com.feduwacomm.entity.ModelVersion;
import com.feduwacomm.mapper.ModelVersionMapper;
import com.feduwacomm.service.impl.ModelVersionServiceImpl;
import com.feduwacomm.utils.UuidUtil;
import com.feduwacomm.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ModelVersionService 单元测试类
 */
@ExtendWith(MockitoExtension.class)
@SpringBootTest
public class ModelVersionServiceTest {

    @Mock
    private ModelVersionMapper modelVersionMapper;

    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private UuidUtil uuidUtil;

    @InjectMocks
    private ModelVersionServiceImpl modelVersionService;

    private String taskId;
    private String modelId;
    private ModelVersion mockModelVersion;
    private MockMultipartFile mockFile;

    @BeforeEach
    void setUp() {
        taskId = "test-task-id-12345678901234567890";
        modelId = "test-model-id-12345678901234567890";

        // 设置上传路径
        ReflectionTestUtils.setField(modelVersionService, "uploadPath", "/tmp/test/models");
        
        // 配置UuidUtil mock
        lenient().when(uuidUtil.generateUuid()).thenReturn(modelId);

        // 创建模拟模型版本实体
        mockModelVersion = ModelVersion.builder()
            .id(modelId)
            .taskId(taskId)
            .roundNumber(1)
            .aggregationMethod("FEDAVG")
            .clientCount(5)
            .accuracy(new BigDecimal("0.8500"))
            .loss(new BigDecimal("0.1234"))
            .status("UPLOADED")
            .description("测试模型")
            .filePath("/tmp/test/models/model.pth")
            .fileSize(1024L)
            .fileFormat(".pth")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .parameters("{\"learning_rate\": 0.001}")
            .metrics("{\"accuracy\": 0.85}")
            .build();

        // 创建模拟文件
        mockFile = new MockMultipartFile(
            "file", "model.pth", "application/octet-stream", "test model content".getBytes()
        );
    }

    @Test
    void testUploadModel_Success() {
        // 准备测试数据
        String parameters = "{\"learning_rate\": 0.001}";

        // 模拟数据库查询返回null（不存在重复轮次）
        when(modelVersionMapper.selectByTaskIdAndRound(taskId, 1)).thenReturn(null);

        // 模拟数据库插入成功
        when(modelVersionMapper.insert(any(ModelVersion.class))).thenReturn(1);

        // 模拟JSON解析
        Map<String, Object> parametersMap = new HashMap<>();
        parametersMap.put("learning_rate", 0.001);
        try {
            when(objectMapper.readValue(eq(parameters), eq(Map.class))).thenReturn(parametersMap);
        } catch (Exception e) {
            // ignore
        }

        // 执行测试
        ModelUploadResponseVO result = modelVersionService.uploadModel(
            taskId, 1, "测试模型", parameters, mockFile);

        // 验证结果
        assertNotNull(result);
        assertEquals(taskId, result.getTaskId());
        assertEquals(1, result.getRoundNumber());
        assertEquals("UPLOADED", result.getStatus());
        assertEquals("测试模型", result.getDescription());
        assertEquals(parametersMap, result.getParameters());

        // 验证方法调用
        verify(modelVersionMapper, times(1)).selectByTaskIdAndRound(taskId, 1);
        verify(modelVersionMapper, times(1)).insert(any(ModelVersion.class));
    }

    @Test
    void testUploadModel_DuplicateRound() {
        // 模拟数据库查询返回已存在的轮次
        when(modelVersionMapper.selectByTaskIdAndRound(taskId, 1)).thenReturn(mockModelVersion);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            modelVersionService.uploadModel(taskId, 1, "测试模型", null, mockFile);
        });

        assertEquals("模型上传失败: 该轮次的模型已存在", exception.getMessage());

        // 验证方法调用
        verify(modelVersionMapper, times(1)).selectByTaskIdAndRound(taskId, 1);
        verify(modelVersionMapper, never()).insert(any(ModelVersion.class));
    }

    @Test
    void testUploadModel_DatabaseInsertFailed() {
        // 模拟数据库查询返回null
        when(modelVersionMapper.selectByTaskIdAndRound(taskId, 1)).thenReturn(null);

        // 模拟数据库插入失败
        when(modelVersionMapper.insert(any(ModelVersion.class))).thenReturn(0);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            modelVersionService.uploadModel(taskId, 1, "测试模型", null, mockFile);
        });

        assertEquals("模型上传失败: 模型版本保存失败", exception.getMessage());

        // 验证方法调用
        verify(modelVersionMapper, times(1)).selectByTaskIdAndRound(taskId, 1);
        verify(modelVersionMapper, times(1)).insert(any(ModelVersion.class));
    }

    @Test
    void testGetModelVersions_Success() {
        // 准备查询参数
        ModelQueryDTO queryDTO = ModelQueryDTO.builder()
            .taskId(taskId)
            .roundNumber(1)
            .status("UPLOADED")
            .page(1)
            .size(10)
            .sort("createdAt")
            .order("desc")
            .build();

        // 模拟数据库查询
        List<ModelVersion> mockModels = Arrays.asList(mockModelVersion);
        when(modelVersionMapper.selectByPage(anyInt(), anyInt(), anyString(), any(), anyString(), anyString(), anyString()))
            .thenReturn(mockModels);
        when(modelVersionMapper.countByCondition(anyString(), any(), anyString())).thenReturn(1);

        // 模拟JSON解析
        try {
            Map<String, Object> metricsMap = Map.of("accuracy", 0.85);
            Map<String, Object> parametersMap = Map.of("learning_rate", 0.001);
            when(objectMapper.readValue(eq("{\"accuracy\": 0.85}"), eq(Map.class))).thenReturn(metricsMap);
            when(objectMapper.readValue(eq("{\"learning_rate\": 0.001}"), eq(Map.class))).thenReturn(parametersMap);
        } catch (Exception e) {
            // ignore
        }

        // 执行测试
        PageResponseDTO<ModelVersionVO> result = modelVersionService.getModelVersions(queryDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getPages());
        assertEquals(1, result.getCurrent());
        assertEquals(10, result.getSize());
        assertEquals(1, result.getRecords().size());

        ModelVersionVO modelVO = result.getRecords().get(0);
        assertEquals(modelId, modelVO.getModelId());
        assertEquals(taskId, modelVO.getTaskId());
        assertEquals(1, modelVO.getRoundNumber());
        assertEquals("UPLOADED", modelVO.getStatus());

        // 验证方法调用
        verify(modelVersionMapper, times(1)).selectByPage(anyInt(), anyInt(), anyString(), any(), anyString(), anyString(), anyString());
        verify(modelVersionMapper, times(1)).countByCondition(anyString(), any(), anyString());
    }

    @Test
    void testGetModelVersionById_Success() {
        // 模拟数据库查询
        when(modelVersionMapper.selectById(modelId)).thenReturn(mockModelVersion);

        // 模拟JSON解析
        try {
            Map<String, Object> metricsMap = Map.of("accuracy", 0.85);
            Map<String, Object> parametersMap = Map.of("learning_rate", 0.001);
            when(objectMapper.readValue(eq("{\"accuracy\": 0.85}"), eq(Map.class))).thenReturn(metricsMap);
            when(objectMapper.readValue(eq("{\"learning_rate\": 0.001}"), eq(Map.class))).thenReturn(parametersMap);
        } catch (Exception e) {
            // ignore
        }

        // 执行测试
        ModelVersionVO result = modelVersionService.getModelVersionById(modelId);

        // 验证结果
        assertNotNull(result);
        assertEquals(modelId, result.getModelId());
        assertEquals(taskId, result.getTaskId());
        assertEquals(1, result.getRoundNumber());
        assertEquals("UPLOADED", result.getStatus());
        assertEquals("测试模型", result.getDescription());

        // 验证方法调用
        verify(modelVersionMapper, times(1)).selectById(modelId);
    }

    @Test
    void testGetModelVersionById_NotFound() {
        // 模拟数据库查询返回null
        when(modelVersionMapper.selectById(modelId)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            modelVersionService.getModelVersionById(modelId);
        });

        assertEquals("模型版本不存在", exception.getMessage());

        // 验证方法调用
        verify(modelVersionMapper, times(1)).selectById(modelId);
    }

    @Test
    void testGetModelVersionsByTaskId_Success() {
        // 模拟数据库查询
        List<ModelVersion> mockModels = Arrays.asList(mockModelVersion);
        when(modelVersionMapper.selectByTaskId(taskId)).thenReturn(mockModels);

        // 模拟JSON解析
        try {
            Map<String, Object> metricsMap = Map.of("accuracy", 0.85);
            Map<String, Object> parametersMap = Map.of("learning_rate", 0.001);
            when(objectMapper.readValue(eq("{\"accuracy\": 0.85}"), eq(Map.class))).thenReturn(metricsMap);
            when(objectMapper.readValue(eq("{\"learning_rate\": 0.001}"), eq(Map.class))).thenReturn(parametersMap);
        } catch (Exception e) {
            // ignore
        }

        // 执行测试
        ModelTaskVersionsVO result = modelVersionService.getModelVersionsByTaskId(
            taskId, 1, "UPLOADED", "roundNumber", "asc");

        // 验证结果
        assertNotNull(result);
        assertEquals(taskId, result.getTaskId());
        assertEquals("水声分类任务", result.getTaskName());
        assertEquals(1, result.getTotalModels());
        assertEquals(1, result.getVersions().size());

        ModelVersionVO modelVO = result.getVersions().get(0);
        assertEquals(modelId, modelVO.getModelId());
        assertEquals(taskId, modelVO.getTaskId());

        // 验证方法调用
        verify(modelVersionMapper, times(1)).selectByTaskId(taskId);
    }

    @Test
    void testEvaluateModel_Success() {
        // 准备评估请求
        ModelEvaluateDTO evaluateDTO = ModelEvaluateDTO.builder()
            .modelId(modelId)
            .testDataPath("/data/test.csv")
            .metrics(Arrays.asList("accuracy", "loss"))
            .batchSize(32)
            .device("cpu")
            .build();

        // 模拟数据库查询
        when(modelVersionMapper.selectById(modelId)).thenReturn(mockModelVersion);

        // 执行测试
        ModelEvaluateResponseVO result = modelVersionService.evaluateModel(evaluateDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals(modelId, result.getModelId());
        assertNotNull(result.getEvaluationId());
        assertEquals("COMPLETED", result.getStatus());
        assertTrue(result.getEvaluationId().startsWith("eval_"));
        assertEquals(5, result.getMetrics().size());
        assertEquals(15.5, result.getEvaluationTime());
        assertEquals(1000, result.getTestSamples());

        // 验证指标
        assertTrue(result.getMetrics().containsKey("accuracy"));
        assertTrue(result.getMetrics().containsKey("loss"));
        assertTrue(result.getMetrics().containsKey("precision"));
        assertTrue(result.getMetrics().containsKey("recall"));
        assertTrue(result.getMetrics().containsKey("f1"));

        // 验证方法调用
        verify(modelVersionMapper, times(1)).selectById(modelId);
    }

    @Test
    void testEvaluateModel_ModelNotFound() {
        // 准备评估请求
        ModelEvaluateDTO evaluateDTO = ModelEvaluateDTO.builder()
            .modelId(modelId)
            .testDataPath("/data/test.csv")
            .build();

        // 模拟数据库查询返回null
        when(modelVersionMapper.selectById(modelId)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            modelVersionService.evaluateModel(evaluateDTO);
        });

        assertEquals("模型版本不存在", exception.getMessage());

        // 验证方法调用
        verify(modelVersionMapper, times(1)).selectById(modelId);
    }

    @Test
    void testDeleteModelVersion_Success() {
        // 模拟数据库查询
        when(modelVersionMapper.selectById(modelId)).thenReturn(mockModelVersion);

        // 模拟数据库删除成功
        when(modelVersionMapper.deleteById(modelId)).thenReturn(1);

        // 执行测试
        Map<String, Object> result = modelVersionService.deleteModelVersion(modelId, false, true);

        // 验证结果
        assertNotNull(result);
        assertEquals(modelId, result.get("modelId"));
        assertNotNull(result.get("deletedAt"));

        // 验证方法调用
        verify(modelVersionMapper, times(1)).selectById(modelId);
        verify(modelVersionMapper, times(1)).deleteById(modelId);
    }

    @Test
    void testDeleteModelVersion_NotFound() {
        // 模拟数据库查询返回null
        when(modelVersionMapper.selectById(modelId)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            modelVersionService.deleteModelVersion(modelId, false, true);
        });

        assertEquals("模型版本不存在", exception.getMessage());

        // 验证方法调用
        verify(modelVersionMapper, times(1)).selectById(modelId);
        verify(modelVersionMapper, never()).deleteById(anyString());
    }

    @Test
    void testDeleteModelVersion_DeployedModelWithoutForce() {
        // 创建已部署的模型
        ModelVersion deployedModel = ModelVersion.builder()
            .id(modelId)
            .taskId(taskId)
            .status("DEPLOYED")
            .build();

        // 模拟数据库查询
        when(modelVersionMapper.selectById(modelId)).thenReturn(deployedModel);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            modelVersionService.deleteModelVersion(modelId, false, true);
        });

        assertEquals("已部署的模型无法删除，请先下线或使用强制删除", exception.getMessage());

        // 验证方法调用
        verify(modelVersionMapper, times(1)).selectById(modelId);
        verify(modelVersionMapper, never()).deleteById(anyString());
    }

    @Test
    void testDeleteModelVersion_DatabaseDeleteFailed() {
        // 模拟数据库查询
        when(modelVersionMapper.selectById(modelId)).thenReturn(mockModelVersion);

        // 模拟数据库删除失败
        when(modelVersionMapper.deleteById(modelId)).thenReturn(0);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            modelVersionService.deleteModelVersion(modelId, false, true);
        });

        assertEquals("删除失败", exception.getMessage());

        // 验证方法调用
        verify(modelVersionMapper, times(1)).selectById(modelId);
        verify(modelVersionMapper, times(1)).deleteById(modelId);
    }

    @Test
    void testGetModelStatistics_Success() {
        // 模拟数据库查询
        when(modelVersionMapper.countByTaskId(taskId)).thenReturn(50);
        when(modelVersionMapper.selectAverageAccuracy(taskId)).thenReturn("0.8500");
        when(modelVersionMapper.selectAverageLoss(taskId)).thenReturn("0.1234");

        // 模拟上传趋势数据
        Map<String, Object> trendData = new HashMap<>();
        trendData.put("date", "2024-01-01");
        trendData.put("count", 5);
        when(modelVersionMapper.selectUploadTrendByDateRange(anyString(), anyString(), eq(taskId)))
            .thenReturn(Arrays.asList(trendData));

        // 模拟准确率趋势数据
        ModelVersion trendModel = ModelVersion.builder()
            .roundNumber(1)
            .accuracy(new BigDecimal("0.8500"))
            .build();
        when(modelVersionMapper.selectAccuracyTrendByTaskId(taskId))
            .thenReturn(Arrays.asList(trendModel));

        // 执行测试
        ModelStatisticsVO result = modelVersionService.getModelStatistics(taskId, "7d");

        // 验证结果
        assertNotNull(result);
        assertEquals(50, result.getTotalModels());
        assertEquals(new BigDecimal("0.8500"), result.getAverageAccuracy());
        assertEquals(new BigDecimal("0.1234"), result.getAverageLoss());
        assertEquals(1, result.getUploadTrend().size());
        assertEquals(1, result.getAccuracyTrend().size());

        // 验证上传趋势
        ModelStatisticsVO.UploadTrendVO uploadTrend = result.getUploadTrend().get(0);
        assertEquals("2024-01-01", uploadTrend.getDate());
        assertEquals(5, uploadTrend.getCount());

        // 验证准确率趋势
        ModelStatisticsVO.AccuracyTrendVO accuracyTrend = result.getAccuracyTrend().get(0);
        assertEquals(1, accuracyTrend.getRoundNumber());
        assertEquals(new BigDecimal("0.8500"), accuracyTrend.getAccuracy());

        // 验证方法调用
        verify(modelVersionMapper, times(1)).countByTaskId(taskId);
        verify(modelVersionMapper, times(1)).selectAverageAccuracy(taskId);
        verify(modelVersionMapper, times(1)).selectAverageLoss(taskId);
        verify(modelVersionMapper, times(1)).selectUploadTrendByDateRange(anyString(), anyString(), eq(taskId));
        verify(modelVersionMapper, times(1)).selectAccuracyTrendByTaskId(taskId);
    }

    @Test
    void testGetTaskModelStatistics_Success() {
        // 模拟数据库查询
        when(modelVersionMapper.countByTaskId(taskId)).thenReturn(10);
        when(modelVersionMapper.selectBestAccuracyByTaskId(taskId)).thenReturn(mockModelVersion);
        when(modelVersionMapper.selectAverageAccuracy(taskId)).thenReturn("0.8000");

        // 执行测试
        Map<String, Object> result = modelVersionService.getTaskModelStatistics(taskId);

        // 验证结果
        assertNotNull(result);
        assertEquals(taskId, result.get("taskId"));
        assertEquals("水声分类任务", result.get("taskName"));
        assertEquals(10, result.get("totalRounds"));
        assertEquals(10, result.get("completedRounds"));

        @SuppressWarnings("unchecked")
        Map<String, Object> performanceMetrics = (Map<String, Object>) result.get("performanceMetrics");
        assertEquals(new BigDecimal("0.8500"), performanceMetrics.get("bestAccuracy"));
        assertEquals(1, performanceMetrics.get("bestRound"));
        assertEquals(new BigDecimal("0.8000"), performanceMetrics.get("averageAccuracy"));

        // 验证方法调用
        verify(modelVersionMapper, times(1)).countByTaskId(taskId);
        verify(modelVersionMapper, times(1)).selectBestAccuracyByTaskId(taskId);
        verify(modelVersionMapper, times(1)).selectAverageAccuracy(taskId);
    }

    @Test
    void testBatchDeleteModels_Success() {
        // 准备删除请求
        List<String> modelIds = Arrays.asList("model1", "model2", "model3");
        ModelBatchDeleteDTO batchDeleteDTO = ModelBatchDeleteDTO.builder()
            .modelIds(modelIds)
            .force(false)
            .deleteFile(true)
            .build();

        // 模拟每个模型的查询和删除
        for (String id : modelIds) {
            ModelVersion model = ModelVersion.builder()
                .id(id)
                .taskId(taskId)
                .status("UPLOADED")
                .build();
            when(modelVersionMapper.selectById(id)).thenReturn(model);
            when(modelVersionMapper.deleteById(id)).thenReturn(1);
        }

        // 执行测试
        Map<String, Object> result = modelVersionService.batchDeleteModels(batchDeleteDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals(3, result.get("successCount"));
        assertEquals(0, result.get("failedCount"));

        // 验证每个模型都被查询和删除
        for (String id : modelIds) {
            verify(modelVersionMapper, times(1)).selectById(id);
            verify(modelVersionMapper, times(1)).deleteById(id);
        }
    }

    @Test
    void testBatchDeleteModels_PartialFailure() {
        // 准备删除请求
        List<String> modelIds = Arrays.asList("model1", "model2", "model3");
        ModelBatchDeleteDTO batchDeleteDTO = ModelBatchDeleteDTO.builder()
            .modelIds(modelIds)
            .force(false)
            .deleteFile(true)
            .build();

        // 模拟第一个模型成功，第二个不存在，第三个成功
        ModelVersion model1 = ModelVersion.builder()
            .id("model1")
            .taskId(taskId)
            .status("UPLOADED")
            .build();
        when(modelVersionMapper.selectById("model1")).thenReturn(model1);
        when(modelVersionMapper.deleteById("model1")).thenReturn(1);

        when(modelVersionMapper.selectById("model2")).thenReturn(null); // 不存在

        ModelVersion model3 = ModelVersion.builder()
            .id("model3")
            .taskId(taskId)
            .status("UPLOADED")
            .build();
        when(modelVersionMapper.selectById("model3")).thenReturn(model3);
        when(modelVersionMapper.deleteById("model3")).thenReturn(1);

        // 执行测试
        Map<String, Object> result = modelVersionService.batchDeleteModels(batchDeleteDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.get("successCount"));
        assertEquals(1, result.get("failedCount"));

        // 验证方法调用
        verify(modelVersionMapper, times(1)).selectById("model1");
        verify(modelVersionMapper, times(1)).deleteById("model1");
        verify(modelVersionMapper, times(1)).selectById("model2");
        verify(modelVersionMapper, never()).deleteById("model2");
        verify(modelVersionMapper, times(1)).selectById("model3");
        verify(modelVersionMapper, times(1)).deleteById("model3");
    }

    @Test
    void testDeployModel_Success() {
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

        // 执行测试
        ModelDeployResponseVO result = modelVersionService.deployModel(deployDTO);

        // 验证结果
        assertNotNull(result);
        assertNotNull(result.getDeploymentId());
        assertEquals(modelId, result.getModelId());
        assertEquals("水声分类模型_v1.0", result.getDeploymentName());
        assertEquals(Arrays.asList("vm_1", "vm_2"), result.getTargetVms());
        assertEquals("DEPLOYED", result.getStatus());
        assertEquals(deploymentConfig, result.getDeploymentConfig());
        assertEquals(2, result.getEndpoints().size());
        assertTrue(result.getDeploymentId().startsWith("deploy_"));
    }

    @Test
    void testDownloadModel_Success() {
        // 执行测试（简化版实现返回空数组）
        byte[] result = modelVersionService.downloadModel(modelId, "original", true);

        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.length); // 简化实现返回空数组
    }

    @Test
    void testBatchDownloadModels_Success() {
        // 准备批量下载请求
        ModelBatchDownloadDTO batchDownloadDTO = ModelBatchDownloadDTO.builder()
            .modelIds(Arrays.asList("model1", "model2"))
            .format("original")
            .compressed(true)
            .build();

        // 执行测试（简化版实现返回空数组）
        byte[] result = modelVersionService.batchDownloadModels(batchDownloadDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.length); // 简化实现返回空数组
    }
}