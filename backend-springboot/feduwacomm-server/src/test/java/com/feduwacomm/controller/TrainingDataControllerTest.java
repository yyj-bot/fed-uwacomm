package com.feduwacomm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.dto.*;
import com.feduwacomm.service.TrainingDataService;
import com.feduwacomm.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 训练数据控制器测试类
 */
@WebMvcTest(TrainingDataController.class)
public class TrainingDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TrainingDataService trainingDataService;

    @Autowired
    private ObjectMapper objectMapper;

    private TrainingDataUploadDTO uploadDTO;
    private TrainingDataTextDTO textDTO;
    private TrainingDataQueryDTO queryDTO;
    private TrainingDataPreprocessDTO preprocessDTO;
    private TrainingDataValidateDTO validateDTO;

    @BeforeEach
    void setUp() {
        // 设置测试数据
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
                        .param("vmId", uploadDTO.getVmId())
                        .param("dataType", uploadDTO.getDataType())
                        .param("description", uploadDTO.getDescription())
                        .param("tags", "[\"test\", \"acoustic\"]")
                        .param("metadata", "{\"source\": \"test\", \"version\": \"1.0\"}")
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(textDTO)))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.code").value(200))
                .andExpected(jsonPath("$.message").value("文本信息上传成功"))
                .andExpected(jsonPath("$.data.datasetId").value("text123"))
                .andExpected(jsonPath("$.data.status").value("READY"));
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
                        .param("page", "1")
                        .param("size", "20")
                        .param("vmId", "a1b2c3d4e5f678901234567890123456")
                        .param("dataType", "ACOUSTIC")
                        .param("keyword", "test"))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.code").value(200))
                .andExpected(jsonPath("$.data.total").value(1))
                .andExpected(jsonPath("$.data.dataList").isArray())
                .andExpected(jsonPath("$.data.dataList[0].datasetId").value("test123"));
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

        mockMvc.perform(get("/api/training-data/{datasetId}", datasetId))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.code").value(200))
                .andExpected(jsonPath("$.data.datasetId").value(datasetId))
                .andExpected(jsonPath("$.data.status").value("READY"));
    }

    @Test
    void testGetDataDetail_NotFound() throws Exception {
        String datasetId = "nonexistent";

        when(trainingDataService.getDataDetail(datasetId))
                .thenThrow(new RuntimeException("数据不存在"));

        mockMvc.perform(get("/api/training-data/{datasetId}", datasetId))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.code").value(500));
    }

    @Test
    void testDownloadData_Success() throws Exception {
        String datasetId = "test123";
        byte[] fileData = "test,data\n1,2\n".getBytes();

        when(trainingDataService.downloadData(datasetId))
                .thenReturn(fileData);

        mockMvc.perform(get("/api/training-data/{datasetId}/download", datasetId))
                .andExpect(status().isOk())
                .andExpected(header().string("Content-Disposition", "attachment; filename=\"" + datasetId + ".data\""))
                .andExpected(content().bytes(fileData));
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(preprocessDTO)))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.code").value(200))
                .andExpected(jsonPath("$.data.taskId").value("preprocess_123"))
                .andExpected(jsonPath("$.data.status").value("PROCESSING"));
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validateDTO)))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.code").value(200))
                .andExpected(jsonPath("$.data.isValid").value(true))
                .andExpected(jsonPath("$.data.results.totalRows").value(100));
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.code").value(200))
                .andExpected(jsonPath("$.data.datasetId").value(datasetId));
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteDTO)))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.code").value(200))
                .andExpected(jsonPath("$.data.datasetId").value(datasetId))
                .andExpected(jsonPath("$.data.fileDeleted").value(true));
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchDTO)))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.code").value(200))
                .andExpected(jsonPath("$.data.total").value(3))
                .andExpected(jsonPath("$.data.success").value(2))
                .andExpected(jsonPath("$.data.failed").value(1));
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
                        .param("vmId", "vm1")
                        .param("dataType", "ACOUSTIC"))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.code").value(200))
                .andExpected(jsonPath("$.data.totalCount").value(150))
                .andExpected(jsonPath("$.data.totalSize").value(1024000));
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exportDTO)))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.code").value(200))
                .andExpected(jsonPath("$.data.taskId").value("export_123"))
                .andExpected(jsonPath("$.data.status").value("PROCESSING"));
    }
}