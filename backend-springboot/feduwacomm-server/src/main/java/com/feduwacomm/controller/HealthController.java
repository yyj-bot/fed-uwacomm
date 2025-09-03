package com.feduwacomm.controller;

import com.feduwacomm.common.Result;
import com.feduwacomm.service.DatabaseHealthService;
import com.feduwacomm.service.DatabaseInitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    @Autowired
    private DatabaseHealthService databaseHealthService;

    @Autowired
    private DatabaseInitService databaseInitService;

    /**
     * 基础健康检查接口
     * 
     * @return 健康状态信息
     */
    @GetMapping
    public Result<Map<String, Object>> healthCheck() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("timestamp", LocalDateTime.now());
        healthInfo.put("service", "FedUWAComm Backend");
        healthInfo.put("version", "1.0.0");
        healthInfo.put("message", "水声联邦学习后端服务运行正常");

        return Result.success("健康检查通过", healthInfo);
    }

    /**
     * 详细健康检查接口（包含数据库状态）
     * 
     * @return 详细的健康状态信息
     */
    @GetMapping("/detailed")
    public Result<Map<String, Object>> detailedHealthCheck() {
        Map<String, Object> healthInfo = new HashMap<>();
        
        // 基础信息
        healthInfo.put("status", "UP");
        healthInfo.put("timestamp", LocalDateTime.now());
        healthInfo.put("service", "FedUWAComm Backend");
        healthInfo.put("version", "1.0.0");
        
        // 数据库健康状态
        Map<String, Object> databaseInfo = new HashMap<>();
        databaseInfo.put("healthy", databaseHealthService.isHealthy());
        databaseInfo.put("lastCheckTime", databaseHealthService.getLastCheckTime());
        databaseInfo.put("lastError", databaseHealthService.getLastError());
        databaseInfo.put("statusSummary", databaseHealthService.getStatusSummary());
        
        healthInfo.put("database", databaseInfo);
        
        // 整体状态判断
        boolean overallHealthy = databaseHealthService.isHealthy();
        String message = overallHealthy ? "所有服务运行正常" : "部分服务异常";
        
        return Result.success(message, healthInfo);
    }

    /**
     * 强制数据库健康检查
     * 
     * @return 检查结果
     */
    @GetMapping("/database/check")
    public Result<Map<String, Object>> forceDatabaseCheck() {
        Map<String, Object> result = new HashMap<>();
        
        boolean isHealthy = databaseHealthService.forceHealthCheck();
        result.put("healthy", isHealthy);
        result.put("checkTime", LocalDateTime.now());
        result.put("statusSummary", databaseHealthService.getStatusSummary());
        
        String message = isHealthy ? "数据库健康检查通过" : "数据库健康检查失败";
        return Result.success(message, result);
    }

    /**
     * 数据库表结构检查
     * 
     * @return 表结构状态
     */
    @GetMapping("/database/tables")
    public Result<Map<String, Object>> checkDatabaseTables() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 检查并初始化表结构
            databaseInitService.checkAndInitDatabase();
            
            // 检查表数据
            databaseInitService.checkTableData();
            
            result.put("status", "SUCCESS");
            result.put("message", "数据库表结构检查完成");
            result.put("checkTime", LocalDateTime.now());
            
            return Result.success("数据库表结构检查完成", result);
            
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("message", "数据库表结构检查失败: " + e.getMessage());
            result.put("checkTime", LocalDateTime.now());
            result.put("error", e.getMessage());
            
            return Result.failure(500, "数据库表结构检查失败", result);
        }
    }
}