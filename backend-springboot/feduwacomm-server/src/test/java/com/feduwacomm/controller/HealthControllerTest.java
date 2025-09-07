package com.feduwacomm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.service.DatabaseHealthService;
import com.feduwacomm.service.DatabaseInitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 健康检查控制器测试类
 * 测试系统健康检查和数据库状态监控功能
 */
@SpringBootTest
@AutoConfigureMockMvc
public class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DatabaseHealthService databaseHealthService;

    @MockBean
    private DatabaseInitService databaseInitService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // 重置所有mock对象
        reset(databaseHealthService, databaseInitService);
    }

    /**
     * 测试基础健康检查接口
     */
    @Test
    void testHealthCheck_Success() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("健康检查通过"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.service").value("FedUWAComm Backend"))
                .andExpect(jsonPath("$.data.version").value("1.0.0"))
                .andExpect(jsonPath("$.data.message").value("水声联邦学习后端服务运行正常"))
                .andExpect(jsonPath("$.data.timestamp").exists());
    }

    /**
     * 测试详细健康检查接口 - 数据库健康
     */
    @Test
    void testDetailedHealthCheck_DatabaseHealthy() throws Exception {
        // 模拟数据库健康状态
        when(databaseHealthService.isHealthy()).thenReturn(true);
        when(databaseHealthService.getLastCheckTime()).thenReturn(LocalDateTime.now());
        when(databaseHealthService.getLastError()).thenReturn(null);
        when(databaseHealthService.getStatusSummary()).thenReturn("数据库连接正常");

        mockMvc.perform(get("/health/detailed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("所有服务运行正常"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.service").value("FedUWAComm Backend"))
                .andExpect(jsonPath("$.data.database.healthy").value(true))
                .andExpect(jsonPath("$.data.database.statusSummary").value("数据库连接正常"))
                .andExpect(jsonPath("$.data.database.lastCheckTime").exists())
                .andExpect(jsonPath("$.data.database.lastError").isEmpty());

        // 验证服务调用
        verify(databaseHealthService, times(2)).isHealthy();
        verify(databaseHealthService).getLastCheckTime();
        verify(databaseHealthService).getLastError();
        verify(databaseHealthService).getStatusSummary();
    }

    /**
     * 测试详细健康检查接口 - 数据库异常
     */
    @Test
    void testDetailedHealthCheck_DatabaseUnhealthy() throws Exception {
        // 模拟数据库异常状态
        when(databaseHealthService.isHealthy()).thenReturn(false);
        when(databaseHealthService.getLastCheckTime()).thenReturn(LocalDateTime.now());
        when(databaseHealthService.getLastError()).thenReturn("连接超时");
        when(databaseHealthService.getStatusSummary()).thenReturn("数据库连接异常");

        mockMvc.perform(get("/health/detailed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("部分服务异常"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.database.healthy").value(false))
                .andExpect(jsonPath("$.data.database.lastError").value("连接超时"))
                .andExpect(jsonPath("$.data.database.statusSummary").value("数据库连接异常"));

        // 验证服务调用
        verify(databaseHealthService, times(2)).isHealthy();
        verify(databaseHealthService).getLastCheckTime();
        verify(databaseHealthService).getLastError();
        verify(databaseHealthService).getStatusSummary();
    }

    /**
     * 测试强制数据库健康检查 - 检查通过
     */
    @Test
    void testForceDatabaseCheck_Success() throws Exception {
        // 模拟强制检查成功
        when(databaseHealthService.forceHealthCheck()).thenReturn(true);
        when(databaseHealthService.getStatusSummary()).thenReturn("数据库连接正常");

        mockMvc.perform(get("/health/database/check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("数据库健康检查通过"))
                .andExpect(jsonPath("$.data.healthy").value(true))
                .andExpect(jsonPath("$.data.statusSummary").value("数据库连接正常"))
                .andExpect(jsonPath("$.data.checkTime").exists());

        // 验证服务调用
        verify(databaseHealthService).forceHealthCheck();
        verify(databaseHealthService).getStatusSummary();
    }

    /**
     * 测试强制数据库健康检查 - 检查失败
     */
    @Test
    void testForceDatabaseCheck_Failed() throws Exception {
        // 模拟强制检查失败
        when(databaseHealthService.forceHealthCheck()).thenReturn(false);
        when(databaseHealthService.getStatusSummary()).thenReturn("数据库连接失败");

        mockMvc.perform(get("/health/database/check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("数据库健康检查失败"))
                .andExpect(jsonPath("$.data.healthy").value(false))
                .andExpect(jsonPath("$.data.statusSummary").value("数据库连接失败"))
                .andExpect(jsonPath("$.data.checkTime").exists());

        // 验证服务调用
        verify(databaseHealthService).forceHealthCheck();
        verify(databaseHealthService).getStatusSummary();
    }

    /**
     * 测试数据库表结构检查 - 成功
     */
    @Test
    void testCheckDatabaseTables_Success() throws Exception {
        // 模拟表结构检查成功
        doNothing().when(databaseInitService).checkAndInitDatabase();
        doNothing().when(databaseInitService).checkTableData();

        mockMvc.perform(get("/health/database/tables"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("数据库表结构检查完成"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.message").value("数据库表结构检查完成"))
                .andExpect(jsonPath("$.data.checkTime").exists());

        // 验证服务调用
        verify(databaseInitService).checkAndInitDatabase();
        verify(databaseInitService).checkTableData();
    }

    /**
     * 测试数据库表结构检查 - 失败
     */
    @Test
    void testCheckDatabaseTables_Failed() throws Exception {
        // 模拟表结构检查异常
        String errorMessage = "表创建失败";
        doThrow(new RuntimeException(errorMessage))
                .when(databaseInitService).checkAndInitDatabase();

        mockMvc.perform(get("/health/database/tables"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("数据库表结构检查失败"))
                .andExpect(jsonPath("$.data.status").value("ERROR"))
                .andExpect(jsonPath("$.data.message").value("数据库表结构检查失败: " + errorMessage))
                .andExpect(jsonPath("$.data.error").value(errorMessage))
                .andExpect(jsonPath("$.data.checkTime").exists());

        // 验证服务调用
        verify(databaseInitService).checkAndInitDatabase();
        // checkTableData不应该被调用，因为checkAndInitDatabase抛出了异常
        verify(databaseInitService, never()).checkTableData();
    }

    /**
     * 测试数据库表数据检查异常
     */
    @Test
    void testCheckDatabaseTables_CheckDataFailed() throws Exception {
        // 模拟表结构初始化成功，但数据检查失败
        doNothing().when(databaseInitService).checkAndInitDatabase();
        String errorMessage = "数据验证失败";
        doThrow(new RuntimeException(errorMessage))
                .when(databaseInitService).checkTableData();

        mockMvc.perform(get("/health/database/tables"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("数据库表结构检查失败"))
                .andExpect(jsonPath("$.data.status").value("ERROR"))
                .andExpect(jsonPath("$.data.message").value("数据库表结构检查失败: " + errorMessage))
                .andExpect(jsonPath("$.data.error").value(errorMessage));

        // 验证服务调用
        verify(databaseInitService).checkAndInitDatabase();
        verify(databaseInitService).checkTableData();
    }

    /**
     * 测试所有健康检查端点的HTTP方法
     */
    @Test
    void testHealthCheckEndpoints_HTTPMethods() throws Exception {
        // 基础健康检查只支持GET
        mockMvc.perform(post("/health"))
                .andExpect(status().isMethodNotAllowed());

        // 详细健康检查只支持GET
        mockMvc.perform(put("/health/detailed"))
                .andExpect(status().isMethodNotAllowed());

        // 强制数据库检查只支持GET
        mockMvc.perform(delete("/health/database/check"))
                .andExpect(status().isMethodNotAllowed());

        // 表结构检查只支持GET
        mockMvc.perform(patch("/health/database/tables"))
                .andExpect(status().isMethodNotAllowed());
    }

    /**
     * 测试不存在的健康检查端点
     */
    @Test
    void testNonExistentHealthEndpoint() throws Exception {
        mockMvc.perform(get("/health/nonexistent"))
                .andExpect(status().isNotFound());
    }

    /**
     * 测试Content-Type处理
     */
    @Test
    void testHealthCheckContentType() throws Exception {
        mockMvc.perform(get("/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}