package com.feduwacomm.service;

import com.feduwacomm.dto.*;
import com.feduwacomm.entity.TrainingData;
import com.feduwacomm.mapper.TrainingDatasetMapper;
import com.feduwacomm.service.impl.TrainingDataServiceImpl;
import com.feduwacomm.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 训练数据服务测试类
 */
@ExtendWith(MockitoExtension.class)
public class TrainingDataServiceTest {

    @Mock
    private TrainingDatasetMapper trainingDatasetMapper;

    @InjectMocks
    private TrainingDataServiceImpl trainingDataService;

    private TrainingDataUploadDTO uploadDTO;
    private TrainingDataTextDTO textDTO;
    private TrainingDataQueryDTO queryDTO;
    private TrainingData sampleTrainingData;
    private MockMultipartFile mockFile;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        uploadDTO = TrainingDataUploadDTO.builder()
                .vmId("a1b2c3d4e5f678901234567890123456")
                .dataType("ACOUSTIC")
                .description("测试声学数据")
                .tags(Arrays.asList("test", "acoustic"))
                .metadata(Map.of("source", "test", "version", "1.0"))
                .build();

        textDTO = TrainingDataTextDTO.builder()
                .vmId("a1b2c3d4e5f678901234567890123456")
                .dataType("ENVIRONMENT")
                .title("测试环境配置")
                .content("测试环境配置内容")
                .description("测试环境配置描述")
                .tags(Arrays.asList("test", "environment"))
                .metadata(Map.of("source", "test"))
                .build();

        queryDTO = TrainingDataQueryDTO.builder()
                .page(1)
                .size(20)
                .vmId("a1b2c3d4e5f678901234567890123456")
                .dataType("ACOUSTIC")
                .keyword("test")
                .build();

        sampleTrainingData = TrainingData.builder()
                .id("test123")
                .vmId("a1b2c3d4e5f678901234567890123456")
                .name("测试数据")
                .description("测试数据描述")
                .dataType("ACOUSTIC")
                .status("READY")
                .filePath("/path/to/file")
                .fileSize(1024L)
                .fileFormat("csv")
                .tags(Arrays.asList("test"))
                .metadata(Map.of("source", "test"))
                .uploadTime(LocalDateTime.now())
                .uploadedBy("testuser")
                .isValid(true)
                .isProcessed(false)
                .progress(100)
                .build();

        mockFile = new MockMultipartFile(
                "file", 
                "test.csv", 
                "text/csv", 
                "test,data\n1,2\n".getBytes()
        );
    }

    @Test
    void testUploadFile_Success() {
        // 模拟插入操作成功
        when(trainingDatasetMapper.insertTrainingData(any(TrainingData.class)))
                .thenReturn(1);

        // 执行测试
        TrainingDataUploadVO result = trainingDataService.uploadFile(uploadDTO, mockFile, "testuser");

        // 验证结果
        assertNotNull(result);
        assertNotNull(result.getDatasetId());
        assertEquals(uploadDTO.getVmId(), result.getVmId());
        assertEquals(uploadDTO.getDataType(), result.getDatasetType());
        assertEquals(uploadDTO.getDescription(), result.getDatasetDescription());
        assertEquals("UPLOADING", result.getStatus());
        assertEquals("testuser", result.getUploadedBy());

        // 验证调用
        verify(trainingDatasetMapper, times(1)).insertTrainingData(any(TrainingData.class));
    }

    @Test
    void testUploadFile_EmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", 
                "empty.txt", 
                "text/plain", 
                new byte[0]
        );

        // 执行测试并验证异常
        assertThrows(RuntimeException.class, () -> {
            trainingDataService.uploadFile(uploadDTO, emptyFile, "testuser");
        });

        // 验证没有调用插入操作
        verify(trainingDatasetMapper, never()).insertTrainingData(any(TrainingData.class));
    }

    @Test
    void testUploadText_Success() {
        // 模拟插入操作成功
        when(trainingDatasetMapper.insertTrainingData(any(TrainingData.class)))
                .thenReturn(1);

        // 执行测试
        TrainingDataUploadVO result = trainingDataService.uploadText(textDTO, "testuser");

        // 验证结果
        assertNotNull(result);
        assertNotNull(result.getDatasetId());
        assertEquals(textDTO.getVmId(), result.getVmId());
        assertEquals(textDTO.getDataType(), result.getDatasetType());
        assertEquals(textDTO.getDescription(), result.getDatasetDescription());
        assertEquals("READY", result.getStatus());
        assertEquals("testuser", result.getUploadedBy());
        assertEquals(100, result.getProgress());

        // 验证调用
        verify(trainingDatasetMapper, times(1)).insertTrainingData(any(TrainingData.class));
    }

    @Test
    void testQueryDataList_Success() {
        // 准备模拟数据
        List<TrainingData> mockDataList = Arrays.asList(sampleTrainingData);
        Long mockTotal = 1L;

        when(trainingDatasetMapper.selectByQuery(
                anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyList(), anyInt(), anyInt()))
                .thenReturn(mockDataList);
        when(trainingDatasetMapper.countByQuery(
                anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyList()))
                .thenReturn(mockTotal);

        // 执行测试
        TrainingDataListVO result = trainingDataService.queryDataList(queryDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals(mockTotal, result.getTotal());
        assertEquals(queryDTO.getPage(), result.getPage());
        assertEquals(queryDTO.getSize(), result.getSize());
        assertNotNull(result.getDataList());
        assertEquals(1, result.getDataList().size());

        TrainingDataListVO.TrainingDataItemVO item = result.getDataList().get(0);
        assertEquals(sampleTrainingData.getId(), item.getDatasetId());
        assertEquals(sampleTrainingData.getDescription(), item.getDatasetDescription());
        assertEquals(sampleTrainingData.getDataType(), item.getDatasetType());

        // 验证调用
        verify(trainingDatasetMapper, times(1)).selectByQuery(
                anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyList(), anyInt(), anyInt());
        verify(trainingDatasetMapper, times(1)).countByQuery(
                anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyList());
    }

    @Test
    void testGetDataDetail_Success() {
        String datasetId = "test123";
        
        when(trainingDatasetMapper.selectByIdEntity(datasetId))
                .thenReturn(sampleTrainingData);

        // 执行测试
        TrainingDataVO result = trainingDataService.getDataDetail(datasetId);

        // 验证结果
        assertNotNull(result);
        assertEquals(datasetId, result.getDatasetId());
        assertEquals(sampleTrainingData.getDescription(), result.getDatasetDescription());
        assertEquals(sampleTrainingData.getDataType(), result.getDatasetType());
        assertEquals(sampleTrainingData.getVmId(), result.getVmId());
        assertEquals(sampleTrainingData.getStatus(), result.getStatus());

        // 验证调用
        verify(trainingDatasetMapper, times(1)).selectByIdEntity(datasetId);
    }

    @Test
    void testGetDataDetail_NotFound() {
        String datasetId = "nonexistent";
        
        when(trainingDatasetMapper.selectByIdEntity(datasetId))
                .thenReturn(null);

        // 执行测试并验证异常
        assertThrows(RuntimeException.class, () -> {
            trainingDataService.getDataDetail(datasetId);
        });

        // 验证调用
        verify(trainingDatasetMapper, times(1)).selectByIdEntity(datasetId);
    }

    @Test
    void testPreprocessData_Success() {
        String datasetId = "test123";
        TrainingDataPreprocessDTO preprocessDTO = TrainingDataPreprocessDTO.builder()
                .methods(Arrays.asList("normalization", "feature_selection"))
                .parameters(Map.of("normalization", Map.of("method", "standard_scaler")))
                .outputFormat("csv")
                .build();

        when(trainingDatasetMapper.selectByIdEntity(datasetId))
                .thenReturn(sampleTrainingData);
        when(trainingDatasetMapper.updateStatus(datasetId, "PROCESSING"))
                .thenReturn(1);

        // 执行测试
        TrainingDataPreprocessVO result = trainingDataService.preprocessData(datasetId, preprocessDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals(datasetId, result.getDatasetId());
        assertNotNull(result.getTaskId());
        assertEquals("PROCESSING", result.getStatus());
        assertEquals(preprocessDTO.getMethods(), result.getMethods());
        assertNotNull(result.getStartedAt());
        assertEquals(300, result.getEstimatedTime());

        // 验证调用
        verify(trainingDatasetMapper, times(1)).selectByIdEntity(datasetId);
        verify(trainingDatasetMapper, times(1)).updateStatus(datasetId, "PROCESSING");
    }

    @Test
    void testValidateData_Success() {
        String datasetId = "test123";
        TrainingDataValidateDTO validateDTO = TrainingDataValidateDTO.builder()
                .validationRules(TrainingDataValidateDTO.ValidationRules.builder()
                        .dataType("ACOUSTIC")
                        .requiredColumns(Arrays.asList("feature_1", "feature_2"))
                        .build())
                .build();

        when(trainingDatasetMapper.selectByIdEntity(datasetId))
                .thenReturn(sampleTrainingData);
        when(trainingDatasetMapper.updateValidationResult(eq(datasetId), anyBoolean(), anyString()))
                .thenReturn(1);

        // 执行测试
        TrainingDataValidateVO result = trainingDataService.validateData(datasetId, validateDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals(datasetId, result.getDatasetId());
        assertNotNull(result.getIsValid());
        assertNotNull(result.getValidationTime());
        assertNotNull(result.getResults());
        assertNotNull(result.getErrors());
        assertNotNull(result.getWarnings());

        // 验证调用
        verify(trainingDatasetMapper, times(1)).selectByIdEntity(datasetId);
        verify(trainingDatasetMapper, times(1)).updateValidationResult(eq(datasetId), anyBoolean(), anyString());
    }

    @Test
    void testUpdateData_Success() {
        String datasetId = "test123";
        String userId = "testuser";
        TrainingDataUpdateDTO updateDTO = TrainingDataUpdateDTO.builder()
                .datasetDescription("更新后的描述")
                .tags(Arrays.asList("updated", "test"))
                .metadata(Map.of("updated", "true"))
                .build();

        when(trainingDatasetMapper.selectByIdEntity(datasetId))
                .thenReturn(sampleTrainingData);
        when(trainingDatasetMapper.updateTrainingData(any(TrainingData.class)))
                .thenReturn(1);

        // 执行测试
        TrainingDataUpdateVO result = trainingDataService.updateData(datasetId, updateDTO, userId);

        // 验证结果
        assertNotNull(result);
        assertEquals(datasetId, result.getDatasetId());
        assertEquals(userId, result.getUpdatedBy());
        assertNotNull(result.getUpdatedAt());

        // 验证调用
        verify(trainingDatasetMapper, times(1)).selectByIdEntity(datasetId);
        verify(trainingDatasetMapper, times(1)).updateTrainingData(any(TrainingData.class));
    }

    @Test
    void testDeleteData_Success() {
        String datasetId = "test123";
        String userId = "testuser";
        TrainingDataDeleteDTO deleteDTO = TrainingDataDeleteDTO.builder()
                .reason("测试删除")
                .deleteFile(true)
                .deleteMetadata(false)
                .build();

        when(trainingDatasetMapper.selectByIdEntity(datasetId))
                .thenReturn(sampleTrainingData);
        when(trainingDatasetMapper.deleteById(datasetId))
                .thenReturn(1);

        // 执行测试
        TrainingDataDeleteVO result = trainingDataService.deleteData(datasetId, deleteDTO, userId);

        // 验证结果
        assertNotNull(result);
        assertEquals(datasetId, result.getDatasetId());
        assertEquals(userId, result.getDeletedBy());
        assertNotNull(result.getDeletedAt());
        assertEquals(true, result.getMetadataPreserved());

        // 验证调用
        verify(trainingDatasetMapper, times(1)).selectByIdEntity(datasetId);
        verify(trainingDatasetMapper, times(1)).deleteById(datasetId);
    }

    @Test
    void testBatchOperation_Success() {
        TrainingDataBatchDTO batchDTO = TrainingDataBatchDTO.builder()
                .operation("DELETE")
                .datasetIds(Arrays.asList("test1", "test2", "test3"))
                .parameters(Map.of("reason", "批量清理"))
                .build();

        when(trainingDatasetMapper.deleteById("test1")).thenReturn(1);
        when(trainingDatasetMapper.deleteById("test2")).thenReturn(1);
        when(trainingDatasetMapper.deleteById("test3")).thenThrow(new RuntimeException("数据不存在"));

        // 执行测试
        TrainingDataBatchVO result = trainingDataService.batchOperation(batchDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals("DELETE", result.getOperation());
        assertEquals(3, result.getTotal());
        assertEquals(2, result.getSuccess());
        assertEquals(1, result.getFailed());
        assertNotNull(result.getResults());
        assertEquals(3, result.getResults().size());

        // 验证成功的结果
        assertEquals("SUCCESS", result.getResults().get(0).getStatus());
        assertEquals("SUCCESS", result.getResults().get(1).getStatus());
        assertEquals("FAILED", result.getResults().get(2).getStatus());

        // 验证调用
        verify(trainingDatasetMapper, times(1)).deleteById("test1");
        verify(trainingDatasetMapper, times(1)).deleteById("test2");
        verify(trainingDatasetMapper, times(1)).deleteById("test3");
    }

    @Test
    void testGetStatistics_Success() {
        TrainingDataStatisticsDTO statisticsDTO = TrainingDataStatisticsDTO.builder()
                .vmId("vm1")
                .dataType("ACOUSTIC")
                .build();

        // 准备模拟数据
        when(trainingDatasetMapper.getTotalCount()).thenReturn(1000L);
        when(trainingDatasetMapper.getTotalSize()).thenReturn(1024000L);
        when(trainingDatasetMapper.getDataTypeDistribution())
                .thenReturn(Map.of("ACOUSTIC", 500, "ENVIRONMENT", 300, "MODEL", 200));
        when(trainingDatasetMapper.getStatusDistribution())
                .thenReturn(Map.of("READY", 800, "PROCESSING", 150, "ERROR", 50));
        when(trainingDatasetMapper.getVmDistribution())
                .thenReturn(Arrays.asList(
                        Map.of("vmId", "vm1", "count", 300, "size", 322122547L),
                        Map.of("vmId", "vm2", "count", 400, "size", 429496730L)
                ));
        when(trainingDatasetMapper.getTopDataTypes(10))
                .thenReturn(Arrays.asList(
                        Map.of("dataType", "ACOUSTIC", "count", 500, "percentage", 50.0),
                        Map.of("dataType", "ENVIRONMENT", "count", 300, "percentage", 30.0)
                ));

        // 执行测试
        TrainingDataStatisticsVO result = trainingDataService.getStatistics(statisticsDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals(1000L, result.getTotalCount());
        assertEquals(1024000L, result.getTotalSize());
        assertNotNull(result.getDataTypeDistribution());
        assertNotNull(result.getStatusDistribution());
        assertNotNull(result.getVmDistribution());
        assertNotNull(result.getUploadTrend());
        assertNotNull(result.getTopDataTypes());

        // 验证分布数据
        assertEquals(3, result.getDataTypeDistribution().size());
        assertEquals(3, result.getStatusDistribution().size());
        assertEquals(2, result.getVmDistribution().size());
        assertEquals(2, result.getTopDataTypes().size());

        // 验证调用
        verify(trainingDatasetMapper, times(1)).getTotalCount();
        verify(trainingDatasetMapper, times(1)).getTotalSize();
        verify(trainingDatasetMapper, times(1)).getDataTypeDistribution();
        verify(trainingDatasetMapper, times(1)).getStatusDistribution();
        verify(trainingDatasetMapper, times(1)).getVmDistribution();
        verify(trainingDatasetMapper, times(1)).getTopDataTypes(10);
    }

    @Test
    void testExportData_Success() {
        TrainingDataExportDTO exportDTO = TrainingDataExportDTO.builder()
                .exportType("CSV")
                .filters(TrainingDataExportDTO.ExportFilters.builder()
                        .dataType("ACOUSTIC")
                        .vmId("vm1")
                        .build())
                .fields(Arrays.asList("datasetId", "datasetName", "status"))
                .format("ZIP")
                .build();

        // 执行测试
        TrainingDataExportVO result = trainingDataService.exportData(exportDTO);

        // 验证结果
        assertNotNull(result);
        assertNotNull(result.getTaskId());
        assertTrue(result.getTaskId().startsWith("export_"));
        assertEquals("PROCESSING", result.getStatus());
        assertEquals("CSV", result.getFormat());
        assertNotNull(result.getStartedAt());
        assertEquals(60, result.getEstimatedTime());
        assertNotNull(result.getDownloadUrl());
        assertTrue(result.getDownloadUrl().contains(result.getTaskId()));
    }
}