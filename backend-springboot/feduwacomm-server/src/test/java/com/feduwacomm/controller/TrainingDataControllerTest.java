package com.feduwacomm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.dto.*;
import com.feduwacomm.service.TrainingDataService;
import com.feduwacomm.utils.UserJwtUtil;
import com.feduwacomm.config.JwtConfig;
import com.feduwacomm.vo.*;
import com.feduwacomm.vo.TrainingDataListResponseVO;
import com.feduwacomm.vo.TrainingDataDetailResponseVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import com.feduwacomm.common.BaseContext;
import org.mockito.MockedStatic;
import org.junit.jupiter.api.AfterEach;

/**
 * 训练数据控制器测试类
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TrainingDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TrainingDataService trainingDataService;
    
    @MockBean
    private UserJwtUtil userJwtUtil;
    
    @MockBean
    private JwtConfig jwtConfig;

    @Autowired
    private ObjectMapper objectMapper;
    
    private String validToken;
    private MockedStatic<BaseContext> baseContextMock;

    private TrainingDataUploadDTO uploadDTO;
    private TrainingDataTextDTO textDTO;
    private TrainingDataQueryDTO queryDTO;
    private TrainingDataPreprocessDTO preprocessDTO;
    private TrainingDataValidateDTO validateDTO;

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
        
        // Setup service mocks
        setupServiceMocks();
        
        // 设置测试数据
        uploadDTO = TrainingDataUploadDTO.builder()
                .vmId("a1b2c3d4e5f678901234567890123456")
                .dataType("ACOUSTIC")
                .datasetDescription("测试声学数据")
                .tags(Arrays.asList("test", "acoustic"))
                .metadata(Map.of("source", "test", "version", "1.0"))
                .build();

        textDTO = TrainingDataTextDTO.builder()
                .vmId("a1b2c3d4e5f678901234567890123456")
                .dataType("ENVIRONMENT")
                .title("测试环境配置")
                .content("测试环境配置内容")
                .datasetDescription("测试环境配置描述")
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

        preprocessDTO = TrainingDataPreprocessDTO.builder()
                .methods(Arrays.asList("normalization", "feature_selection"))
                .parameters(Map.of("normalization", Map.of("method", "standard_scaler")))
                .outputFormat("csv")
                .build();

        validateDTO = TrainingDataValidateDTO.builder()
                .validationRules(TrainingDataValidateDTO.ValidationRules.builder()
                        .dataType("ACOUSTIC")
                        .requiredColumns(Arrays.asList("feature_1", "feature_2"))
                        .build())
                .build();
    }

    @Test
    void testUploadFile_Success() throws Exception {
        // 准备测试文件
        MockMultipartFile file = new MockMultipartFile(
                "file", 
                "test.csv", 
                "text/csv", 
                "test,data\n1,2\n".getBytes()
        );

        TrainingDataUploadVO expectedResponse = TrainingDataUploadVO.builder()
                .datasetId("test123")
                .datasetDescription("测试声学数据")
                .datasetType("ACOUSTIC")
                .vmId("a1b2c3d4e5f678901234567890123456")
                .status("UPLOADING")
                .uploadTime(LocalDateTime.now())
                .uploadedBy("testuser")
                .progress(0)
                .build();

        when(trainingDataService.uploadFile(any(TrainingDataUploadDTO.class), any(), anyString()))
                .thenReturn(expectedResponse);

        mockMvc.perform(multipart("/api/training-data/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + validToken)
                        .param("vmId", uploadDTO.getVmId())
                        .param("dataType", uploadDTO.getDataType())
                        .param("description", uploadDTO.getDatasetDescription())
                        .param("tags", String.join(",", "test", "acoustic"))
                        .param("metadata.source", "test")
                        .param("metadata.version", "1.0")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("文件上传成功"))
                .andExpect(jsonPath("$.data.datasetId").value("test123"))
                .andExpect(jsonPath("$.data.status").value("UPLOADING"));
    }

    @Test
    void testUploadFile_EmptyFile() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", 
                "empty.txt", 
                "text/plain", 
                new byte[0]
        );

        mockMvc.perform(multipart("/api/training-data/upload")
                        .file(emptyFile)
                        .header("Authorization", "Bearer " + validToken)
                        .param("vmId", uploadDTO.getVmId())
                        .param("dataType", uploadDTO.getDataType())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testUploadText_Success() throws Exception {
        TrainingDataUploadVO expectedResponse = TrainingDataUploadVO.builder()
                .datasetId("text123")
                .datasetDescription("测试环境配置描述")
                .datasetType("ENVIRONMENT")
                .vmId("a1b2c3d4e5f678901234567890123456")
                .status("READY")
                .uploadTime(LocalDateTime.now())
                .uploadedBy("testuser")
                .progress(100)
                .build();

        when(trainingDataService.uploadText(any(TrainingDataTextDTO.class), anyString()))
                .thenReturn(expectedResponse);

        mockMvc.perform(post("/api/training-data/text")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(textDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("文本信息上传成功"))
                .andExpect(jsonPath("$.data.datasetId").value("text123"))
                .andExpect(jsonPath("$.data.status").value("READY"));
    }

    @Test
    void testQueryDataList_Success() throws Exception {
        TrainingDataListVO.TrainingDataItemVO item = TrainingDataListVO.TrainingDataItemVO.builder()
                .datasetId("test123")
                .datasetDescription("测试数据")
                .datasetType("ACOUSTIC")
                .vmId("a1b2c3d4e5f678901234567890123456")
                .status("READY")
                .tags(Arrays.asList("test"))
                .build();

        TrainingDataListVO expectedResponse = TrainingDataListVO.builder()
                .total(1L)
                .page(1)
                .size(20)
                .dataList(Arrays.asList(item))
                .build();

        when(trainingDataService.queryDataList(any(TrainingDataQueryDTO.class)))
                .thenReturn(expectedResponse);

        mockMvc.perform(get("/api/training-data")
                        .header("Authorization", "Bearer " + validToken)
                        .param("page", "1")
                        .param("size", "20")
                        .param("vmId", "a1b2c3d4e5f678901234567890123456")
                        .param("dataType", "ACOUSTIC")
                        .param("keyword", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.dataList").isArray())
                .andExpect(jsonPath("$.data.dataList[0].datasetId").value("test123"));
    }

    @Test
    void testGetDataDetail_Success() throws Exception {
        String datasetId = "test123";
        TrainingDataVO expectedResponse = TrainingDataVO.builder()
                .datasetId(datasetId)
                .datasetDescription("测试数据详情")
                .datasetType("ACOUSTIC")
                .vmId("a1b2c3d4e5f678901234567890123456")
                .status("READY")
                .uploadTime(LocalDateTime.now())
                .uploadedBy("testuser")
                .tags(Arrays.asList("test"))
                .metadata(Map.of("source", "test"))
                .build();

        when(trainingDataService.getDataDetail(datasetId))
                .thenReturn(expectedResponse);

        mockMvc.perform(get("/api/training-data/{datasetId}", datasetId)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.datasetId").value(datasetId))
                .andExpect(jsonPath("$.data.status").value("READY"));
    }

    @Test
    void testGetDataDetail_NotFound() throws Exception {
        String datasetId = "nonexistent";

        when(trainingDataService.getDataDetail(datasetId))
                .thenThrow(new RuntimeException("数据不存在"));

        mockMvc.perform(get("/api/training-data/{datasetId}", datasetId)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void testDownloadData_Success() throws Exception {
        String datasetId = "test123";
        byte[] fileData = "test,data\n1,2\n".getBytes();

        when(trainingDataService.downloadData(datasetId))
                .thenReturn(fileData);

        mockMvc.perform(get("/api/training-data/{datasetId}/download", datasetId)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"" + datasetId + ".data\""))
                .andExpect(content().bytes(fileData));
    }

    @Test
    void testPreprocessData_Success() throws Exception {
        String datasetId = "test123";
        TrainingDataPreprocessVO expectedResponse = TrainingDataPreprocessVO.builder()
                .datasetId(datasetId)
                .taskId("preprocess_123")
                .status("PROCESSING")
                .methods(Arrays.asList("normalization", "feature_selection"))
                .startedAt(LocalDateTime.now())
                .estimatedTime(300)
                .build();

        when(trainingDataService.preprocessData(eq(datasetId), any(TrainingDataPreprocessDTO.class)))
                .thenReturn(expectedResponse);

        mockMvc.perform(post("/api/training-data/{datasetId}/preprocess", datasetId)
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(preprocessDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskId").value("preprocess_123"))
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));
    }

    @Test
    void testValidateData_Success() throws Exception {
        String datasetId = "test123";
        TrainingDataValidateVO expectedResponse = TrainingDataValidateVO.builder()
                .datasetId(datasetId)
                .isValid(true)
                .validationTime(LocalDateTime.now())
                .results(TrainingDataValidateVO.ValidationResults.builder()
                        .totalRows(100)
                        .validRows(98)
                        .invalidRows(2)
                        .build())
                .errors(Arrays.asList())
                .warnings(Arrays.asList())
                .build();

        when(trainingDataService.validateData(eq(datasetId), any(TrainingDataValidateDTO.class)))
                .thenReturn(expectedResponse);

        mockMvc.perform(post("/api/training-data/{datasetId}/validate", datasetId)
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.isValid").value(true))
                .andExpect(jsonPath("$.data.results.totalRows").value(100));
    }

    @Test
    void testUpdateData_Success() throws Exception {
        String datasetId = "test123";
        TrainingDataUpdateDTO updateDTO = TrainingDataUpdateDTO.builder()
                .datasetDescription("更新后的描述")
                .tags(Arrays.asList("updated", "test"))
                .metadata(Map.of("updated", "true"))
                .build();

        TrainingDataUpdateVO expectedResponse = TrainingDataUpdateVO.builder()
                .datasetId(datasetId)
                .updatedAt(LocalDateTime.now())
                .updatedBy("testuser")
                .build();

        when(trainingDataService.updateData(eq(datasetId), any(TrainingDataUpdateDTO.class), anyString()))
                .thenReturn(expectedResponse);

        mockMvc.perform(put("/api/training-data/{datasetId}", datasetId)
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.datasetId").value(datasetId));
    }

    @Test
    void testDeleteData_Success() throws Exception {
        String datasetId = "test123";
        TrainingDataDeleteDTO deleteDTO = TrainingDataDeleteDTO.builder()
                .reason("测试删除")
                .deleteFile(true)
                .deleteMetadata(false)
                .build();

        TrainingDataDeleteVO expectedResponse = TrainingDataDeleteVO.builder()
                .datasetId(datasetId)
                .deletedAt(LocalDateTime.now())
                .deletedBy("testuser")
                .fileDeleted(true)
                .metadataPreserved(true)
                .build();

        when(trainingDataService.deleteData(eq(datasetId), any(TrainingDataDeleteDTO.class), anyString()))
                .thenReturn(expectedResponse);

        mockMvc.perform(delete("/api/training-data/{datasetId}", datasetId)
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.datasetId").value(datasetId))
                .andExpect(jsonPath("$.data.fileDeleted").value(true));
    }

    @Test
    void testBatchOperation_Success() throws Exception {
        TrainingDataBatchDTO batchDTO = TrainingDataBatchDTO.builder()
                .operation("DELETE")
                .datasetIds(Arrays.asList("test1", "test2", "test3"))
                .parameters(Map.of("reason", "批量清理"))
                .build();

        List<TrainingDataBatchVO.BatchResult> results = Arrays.asList(
                TrainingDataBatchVO.BatchResult.builder()
                        .datasetId("test1")
                        .status("SUCCESS")
                        .message("删除成功")
                        .build(),
                TrainingDataBatchVO.BatchResult.builder()
                        .datasetId("test2")
                        .status("SUCCESS")
                        .message("删除成功")
                        .build(),
                TrainingDataBatchVO.BatchResult.builder()
                        .datasetId("test3")
                        .status("FAILED")
                        .message("数据不存在")
                        .build()
        );

        TrainingDataBatchVO expectedResponse = TrainingDataBatchVO.builder()
                .operation("DELETE")
                .total(3)
                .success(2)
                .failed(1)
                .results(results)
                .build();

        when(trainingDataService.batchOperation(any(TrainingDataBatchDTO.class)))
                .thenReturn(expectedResponse);

        mockMvc.perform(post("/api/training-data/batch")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.success").value(2))
                .andExpect(jsonPath("$.data.failed").value(1));
    }

    @Test
    void testGetStatistics_Success() throws Exception {
        Map<String, Integer> dataTypeDistribution = new HashMap<>();
        dataTypeDistribution.put("ACOUSTIC", 100);
        dataTypeDistribution.put("ENVIRONMENT", 50);

        TrainingDataStatisticsVO expectedResponse = TrainingDataStatisticsVO.builder()
                .totalCount(150L)
                .totalSize(1024000L)
                .dataTypeDistribution(dataTypeDistribution)
                .statusDistribution(Map.of("READY", 120, "PROCESSING", 30))
                .vmDistribution(Map.of("vm1", TrainingDataStatisticsVO.VmStatistic.builder()
                        .count(75)
                        .size(512000L)
                        .build()))
                .build();

        when(trainingDataService.getStatistics(any(TrainingDataStatisticsDTO.class)))
                .thenReturn(expectedResponse);

        mockMvc.perform(get("/api/training-data/statistics")
                        .header("Authorization", "Bearer " + validToken)
                        .param("vmId", "vm1")
                        .param("dataType", "ACOUSTIC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalCount").value(150))
                .andExpect(jsonPath("$.data.totalSize").value(1024000));
    }

    @Test
    void testExportData_Success() throws Exception {
        TrainingDataExportDTO exportDTO = TrainingDataExportDTO.builder()
                .exportType("CSV")
                .filters(TrainingDataExportDTO.ExportFilters.builder()
                        .dataType("ACOUSTIC")
                        .vmId("vm1")
                        .build())
                .fields(Arrays.asList("datasetId", "datasetName", "status"))
                .format("ZIP")
                .build();

        TrainingDataExportVO expectedResponse = TrainingDataExportVO.builder()
                .taskId("export_123")
                .status("PROCESSING")
                .format("CSV")
                .startedAt(LocalDateTime.now())
                .estimatedTime(60)
                .downloadUrl("/api/training-data/export/download/export_123")
                .build();

        when(trainingDataService.exportData(any(TrainingDataExportDTO.class)))
                .thenReturn(expectedResponse);

        mockMvc.perform(post("/api/training-data/export")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exportDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskId").value("export_123"))
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));
    }
    
    @AfterEach
    void tearDown() {
        if (baseContextMock != null) {
            baseContextMock.close();
        }
    }
    
    private void setupServiceMocks() {
        // 设置基本的服务Mock行为
        setupTrainingDataServiceMocks();
        setupFileOperationMocks();
    }

    /**
     * 设置TrainingDataService的Mock行为
     */
    private void setupTrainingDataServiceMocks() {
        // 模拟成功上传数据
        when(trainingDataService.uploadFile(any(), any(), any()))
            .thenAnswer(invocation -> {
                String datasetName = invocation.getArgument(1);
                return TrainingDataUploadVO.builder()
                    .datasetId("dataset_" + System.currentTimeMillis())
                    .datasetDescription(datasetName + "_test.csv")
                    .status("UPLOADED")
                    .build();
            });

        // 模拟查询数据集列表
        when(trainingDataService.queryDataList(any()))
            .thenReturn(createMockDatasetList());

        // 模拟查询数据集详情
        when(trainingDataService.getDataDetail(anyString()))
            .thenReturn(createMockDatasetDetail());

        // 模拟删除数据集
        when(trainingDataService.deleteData(anyString(), any(), anyString()))
            .thenReturn(TrainingDataDeleteVO.builder()
                .datasetId("dataset_001")
                .deletedAt(LocalDateTime.now())
                .deletedBy("admin")
                .fileDeleted(true)
                .metadataPreserved(false)
                .build());
    }


    /**
     * 设置文件操作Mock
     */
    private void setupFileOperationMocks() {
        // 如果有文件操作相关的Mock，可以在这里设置
        // 例如：文件存储、文件验证等
    }

    /**
     * 创建模拟的数据集列表
     */
    private TrainingDataListVO createMockDatasetList() {
        List<TrainingDataListVO.TrainingDataItemVO> datasets = List.of(
            TrainingDataListVO.TrainingDataItemVO.builder()
                .datasetId("dataset_001")
                .vmId("vm_001")
                .datasetDescription("水声特征数据集")
                .datasetType("ACOUSTIC_FEATURES")
                .status("READY")
                .build(),
            TrainingDataListVO.TrainingDataItemVO.builder()
                .datasetId("dataset_002")
                .vmId("vm_002")
                .datasetDescription("传播数据集")
                .datasetType("PROPAGATION_DATA")
                .status("PROCESSING")
                .build()
        );

        return TrainingDataListVO.builder()
            .total(2L)
            .page(1)
            .size(10)
            .dataList(datasets)
            .build();
    }

    /**
     * 创建模拟的数据集详情
     */
    private TrainingDataVO createMockDatasetDetail() {
        return TrainingDataVO.builder()
            .datasetId("dataset_001")
            .vmId("vm_001")
            .datasetDescription("水声特征数据集")
            .datasetType("ACOUSTIC_FEATURES")
            .uploadTime(LocalDateTime.of(2025, 1, 1, 10, 0, 0))
            .status("READY")
            .uploadedBy("admin")
            .metadata(Map.of(
                "mean_frequency", 1500.0,
                "std_amplitude", 0.1,
                "missing_values", 5
            ))
            .build();
    }
}