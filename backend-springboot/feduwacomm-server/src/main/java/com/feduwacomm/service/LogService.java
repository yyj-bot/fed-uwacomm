package com.feduwacomm.service;

import com.feduwacomm.common.PageResult;
import com.feduwacomm.dto.LogQueryDTO;
import com.feduwacomm.dto.LogExportDTO;
import com.feduwacomm.dto.LogCleanupDTO;
import com.feduwacomm.dto.LogConfigDTO;
import com.feduwacomm.vo.*;

import java.util.List;

public interface LogService {
    
    // 查询和检索方法
    PageResult<LogListVO> queryLogs(LogQueryDTO queryDTO);
    
    LogDetailVO getLogDetail(String logId);
    
    List<LogListVO> getRealtimeLogs(LogQueryDTO queryDTO);
    
    LogStatisticsVO getStatistics(LogQueryDTO queryDTO);
    
    // 日志导出方法
    byte[] generateLogFile(LogExportDTO exportDTO);
    
    // 日志清理方法
    LogCleanupTaskVO createCleanupTask(LogCleanupDTO cleanupDTO);
    
    LogCleanupTaskVO getCleanupStatus(String cleanupId);
    
    PageResult<LogCleanupTaskVO> getCleanupHistory(Integer page, Integer size, String status);
    
    // 系统监控方法
    LogMonitorVO getSystemMonitor();
    
    LogMonitorVO getLogMonitor(String timeRange, String level);
    
    LogMonitorVO getPerformanceMonitor(String timeRange, String endpoint);
    
    LogMonitorVO getAlerts();
    
    // 日志配置方法
    LogConfigVO getLogConfig();
    
    void updateLogConfig(LogConfigDTO configDTO);
    
    // 日志记录方法
    void logInfo(String message, String userId, String username, String requestUri, String clientIp);
    
    void logInfo(String message, String userId, String username, String requestUri, String clientIp, String category);
    
    void logWarn(String message, String userId, String username, String requestUri, String clientIp);
    
    void logWarn(String message, String userId, String username, String requestUri, String clientIp, String category);
    
    void logError(String message, String userId, String username, String requestUri, String clientIp, Throwable throwable);
    
    void logError(String message, String userId, String username, String requestUri, String clientIp, Throwable throwable, String category);
    
    void logDebug(String message, String userId, String username, String requestUri, String clientIp);
    
    void logDebug(String message, String userId, String username, String requestUri, String clientIp, String category);
    
    // 任务和虚拟机日志记录方法
    void logTask(String taskId, String level, String message, String source, String vmId, Object details);
    
    void logVm(String vmId, String level, String operation, String message, String taskId, Object details);
}