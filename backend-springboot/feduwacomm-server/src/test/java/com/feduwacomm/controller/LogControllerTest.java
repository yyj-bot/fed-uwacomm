package com.feduwacomm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.dto.LogCleanupDTO;
import com.feduwacomm.dto.LogConfigDTO;
import com.feduwacomm.dto.LogExportDTO;
import com.feduwacomm.dto.LogQueryDTO;
import com.feduwacomm.enums.LogCleanupStrategy;
import com.feduwacomm.enums.LogExportFormat;
import com.feduwacomm.enums.LogLevel;
import com.feduwacomm.enums.LogCategory;
import com.feduwacomm.utils.JwtUtil;
import com.feduwacomm.config.JwtConfig;
import com.feduwacomm.service.LogService;
import com.feduwacomm.common.PageResult;
import com.feduwacomm.vo.*;
import com.feduwacomm.common.BaseContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
import org.mockito.MockedStatic;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("日志管理控制器测试")
class LogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private JwtUtil jwtUtil;
    
    @MockBean
    private JwtConfig jwtConfig;
    
    @MockBean
    private LogService logService;
    
    private String validToken;
    private MockedStatic<BaseContext> baseContextMock;

    @BeforeEach
    void setUp() {
        // 设置JWT Mock
        validToken = "test_token";
        Claims claims = new DefaultClaims();
        claims.put("userId", "user123");
        claims.put("username", "testuser");
        claims.put("role", "RESEARCHER");
        claims.put("type", "access");
        
        when(jwtUtil.validateToken(validToken)).thenReturn(claims);
        
        // Mock BaseContext static method
        baseContextMock = mockStatic(BaseContext.class);
        baseContextMock.when(BaseContext::getCurrentId).thenReturn("user123");
        
        // 设置测试数据Mocks
        setupMockData();
    }
    
    @AfterEach
    void tearDown() {
        if (baseContextMock != null) {
            baseContextMock.close();
        }
    }
    
    private void setupMockData() {
        // Mock PageResult for log list
        LogListVO logVO = LogListVO.builder()
            .logId("test-log-id-12345")
            .message("日志详情信息")
            .level(LogLevel.INFO)
            .category(LogCategory.SYSTEM)
            .createdAt(LocalDateTime.now())
            .build();
        
        PageResult<LogListVO> pageResult = PageResult.<LogListVO>builder()
            .total(1000L)
            .current(1L)
            .size(10L)
            .records(Arrays.asList(logVO))
            .build();
        
        when(logService.queryLogs(any(LogQueryDTO.class))).thenReturn(pageResult);
        
        // Mock LogDetailVO
        LogDetailVO logDetail = LogDetailVO.builder()
            .logId("test-log-id-12345")
            .message("日志详情信息")
            .level(LogLevel.INFO)
            .category(LogCategory.SYSTEM)
            .createdAt(LocalDateTime.now())
            .build();
        
        when(logService.getLogDetail(eq("test-log-id-12345"))).thenReturn(logDetail);
        
        // Mock realtime logs
        List<LogListVO> realtimeLogs = Arrays.asList(
            LogListVO.builder().logId("realtime-1").message("实时日志1").level(LogLevel.ERROR).build(),
            LogListVO.builder().logId("realtime-2").message("实时日志2").level(LogLevel.ERROR).build()
        );
        // Create 100 logs for the realtime test
        realtimeLogs = new java.util.ArrayList<>(realtimeLogs);
        for (int i = 3; i <= 100; i++) {
            realtimeLogs.add(LogListVO.builder().logId("realtime-" + i).message("实时日志" + i).level(LogLevel.ERROR).build());
        }
        
        when(logService.getRealtimeLogs(any(LogQueryDTO.class))).thenReturn(realtimeLogs);
        
        // Mock statistics
        LogStatisticsVO statistics = LogStatisticsVO.builder()
            .totalLogs(10000L)
            .build();
        
        when(logService.getStatistics(any(LogQueryDTO.class))).thenReturn(statistics);
    }

    @Test
    @DisplayName("测试日志列表查询")
    void testQueryLogs() throws Exception {
        mockMvc.perform(get("/api/log/list")
                .header("Authorization", "Bearer " + validToken)
                .param("level", "INFO")
                .param("category", "SYSTEM")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.total").value(1000))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(10));
    }

    @Test
    @DisplayName("测试日志详情查询")
    void testGetLogDetail() throws Exception {
        String logId = "test-log-id-12345";
        
        mockMvc.perform(get("/api/log/detail/{logId}", logId)
                .header("Authorization", "Bearer " + validToken))
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
                .header("Authorization", "Bearer " + validToken)
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
                .header("Authorization", "Bearer " + validToken)
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
                .andExpect(content().bytes("导出文件内容".getBytes()));
    }

    @Test
    @DisplayName("测试导出历史查询")
    void testGetExportHistory() throws Exception {
        mockMvc.perform(get("/api/log/export/history")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
        // 测试页码小于1的情况 - FedUWAComm uses HTTP 200 with internal error codes
        mockMvc.perform(get("/api/log/list")
                .header("Authorization", "Bearer " + validToken)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        // 测试每页大小超过限制的情况 - FedUWAComm uses HTTP 200 with internal error codes
        mockMvc.perform(get("/api/log/list")
                .header("Authorization", "Bearer " + validToken)
                .param("page", "1")
                .param("size", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }
}