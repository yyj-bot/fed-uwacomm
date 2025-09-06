package com.feduwacomm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.dto.LogCleanupDTO;
import com.feduwacomm.dto.LogConfigDTO;
import com.feduwacomm.dto.LogExportDTO;
import com.feduwacomm.dto.LogQueryDTO;
import com.feduwacomm.enums.LogCleanupStrategy;
import com.feduwacomm.enums.LogExportFormat;
import com.feduwacomm.enums.LogLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LogController.class)
@DisplayName("日志管理控制器测试")
class LogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // 测试前置准备
    }

    @Test
    @DisplayName("测试日志列表查询")
    void testQueryLogs() throws Exception {
        mockMvc.perform(get("/api/log/list")
                .param("level", "INFO")
                .param("category", "SYSTEM")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.total").value(1000))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(10));
    }

    @Test
    @DisplayName("测试日志详情查询")
    void testGetLogDetail() throws Exception {
        String logId = "test-log-id-12345";
        
        mockMvc.perform(get("/api/log/detail/{logId}", logId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.logId").value(logId))
                .andExpect(jsonPath("$.data.message").value("日志详情信息"));
    }

    @Test
    @DisplayName("测试实时日志查询")
    void testGetRealtimeLogs() throws Exception {
        mockMvc.perform(get("/api/log/realtime")
                .param("level", "ERROR")
                .param("tail", "100"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalCount").value(100));
    }

    @Test
    @DisplayName("测试日志统计查询")
    void testGetStatistics() throws Exception {
        mockMvc.perform(get("/api/log/statistics")
                .param("startTime", "2024-01-01T00:00:00")
                .param("endTime", "2024-01-02T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalLogs").value(10000));
    }

    @Test
    @DisplayName("测试日志导出")
    void testExportLogs() throws Exception {
        LogExportDTO exportDTO = LogExportDTO.builder()
                .format(LogExportFormat.CSV)
                .level(LogLevel.INFO)
                .includeDetails(true)
                .build();

        mockMvc.perform(post("/api/log/export")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(exportDTO)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.exportId").exists());
    }

    @Test
    @DisplayName("测试导出状态查询")
    void testGetExportStatus() throws Exception {
        String exportId = "export_1234567890";
        
        mockMvc.perform(get("/api/log/export/status/{exportId}", exportId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.exportId").value(exportId));
    }

    @Test
    @DisplayName("测试导出文件下载")
    void testDownloadExport() throws Exception {
        String exportId = "export_1234567890";
        
        mockMvc.perform(get("/api/log/export/download/{exportId}", exportId))
                .andExpect(status().isOk())
                .andExpected(content().bytes("导出文件内容".getBytes()));
    }

    @Test
    @DisplayName("测试导出历史查询")
    void testGetExportHistory() throws Exception {
        mockMvc.perform(get("/api/log/export/history")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpected(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(50));
    }

    @Test
    @DisplayName("测试日志清理")
    void testCleanupLogs() throws Exception {
        LogCleanupDTO cleanupDTO = LogCleanupDTO.builder()
                .strategy(LogCleanupStrategy.TIME_BASED)
                .retentionDays(30)
                .dryRun(true)
                .build();

        mockMvc.perform(post("/api/log/cleanup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cleanupDTO)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.cleanupId").exists());
    }

    @Test
    @DisplayName("测试清理状态查询")
    void testGetCleanupStatus() throws Exception {
        String cleanupId = "cleanup_1234567890";
        
        mockMvc.perform(get("/api/log/cleanup/status/{cleanupId}", cleanupId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.cleanupId").value(cleanupId));
    }

    @Test
    @DisplayName("测试系统监控")
    void testGetSystemMonitor() throws Exception {
        mockMvc.perform(get("/api/log/monitor/system"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("测试日志监控")
    void testGetLogMonitor() throws Exception {
        mockMvc.perform(get("/api/log/monitor/logs")
                .param("timeRange", "1h")
                .param("level", "ERROR"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("测试日志配置查询")
    void testGetLogConfig() throws Exception {
        mockMvc.perform(get("/api/log/config"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("测试日志配置更新")
    void testUpdateLogConfig() throws Exception {
        LogConfigDTO configDTO = LogConfigDTO.builder()
                .logLevel(LogLevel.INFO)
                .retentionDays(30)
                .maxFileSize(104857600L)
                .build();

        mockMvc.perform(put("/api/log/config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(configDTO)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("测试无效参数验证")
    void testInvalidParameters() throws Exception {
        // 测试页码小于1的情况
        mockMvc.perform(get("/api/log/list")
                .param("page", "0")
                .param("size", "10"))
                .andExpected(status().isBadRequest());

        // 测试每页大小超过限制的情况
        mockMvc.perform(get("/api/log/list")
                .param("page", "1")
                .param("size", "101"))
                .andExpected(status().isBadRequest());
    }
}