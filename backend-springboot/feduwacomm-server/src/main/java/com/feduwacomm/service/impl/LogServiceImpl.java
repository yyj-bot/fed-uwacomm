package com.feduwacomm.service.impl;

import com.feduwacomm.common.PageResult;
import com.feduwacomm.dto.*;
import com.feduwacomm.entity.*;
import com.feduwacomm.enums.*;
import com.feduwacomm.mapper.*;
import com.feduwacomm.service.LogService;
import com.feduwacomm.vo.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.feduwacomm.utils.UuidUtil;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 日志服务实现类
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Service
public class LogServiceImpl implements LogService {

    private static final Logger logger = LogManager.getLogger(LogServiceImpl.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UuidUtil uuidUtil;
    
    @Autowired
    private SystemLogMapper systemLogMapper;
    
    @Autowired
    private VmRuntimeLogMapper vmRuntimeLogMapper;
    
    @Autowired
    private LogExportTaskMapper logExportTaskMapper;
    
    @Autowired
    private LogCleanupTaskMapper logCleanupTaskMapper;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Value("${logging.db.enabled:false}")
    private boolean dbLoggingEnabled;

    @Value("${spring.profiles.active:dev}")
    private String environment;
    
    @Value("${logging.export.base-path:/tmp/log-exports}")
    private String exportBasePath;
    
    @Value("${logging.export.retention-days:7}")
    private Integer exportRetentionDays;

    @Override
    public void logInfo(String message, String userId, String username, String requestUri, String clientIp) {
        logInfo(message, userId, username, requestUri, clientIp, "com.feduwacomm");
    }
    
    @Override
    public void logInfo(String message, String userId, String username, String requestUri, String clientIp, String category) {
        // 控制台输出
        logger.info("INFO - {} | 用户: {} | URI: {} | IP: {} | 类别: {}",
                message, getUserIdDisplay(userId, username), requestUri, clientIp, category);

        // 数据库存储（如果启用）
        if (dbLoggingEnabled) {
            saveToDatabase("INFO", message, userId, username, requestUri, clientIp, null, category);
        }
    }

    @Override
    public void logWarn(String message, String userId, String username, String requestUri, String clientIp) {
        logWarn(message, userId, username, requestUri, clientIp, "com.feduwacomm");
    }
    
    @Override
    public void logWarn(String message, String userId, String username, String requestUri, String clientIp, String category) {
        // 控制台输出
        logger.warn("WARN - {} | 用户: {} | URI: {} | IP: {} | 类别: {}",
                message, getUserIdDisplay(userId, username), requestUri, clientIp, category);

        // 数据库存储（如果启用）
        if (dbLoggingEnabled) {
            saveToDatabase("WARN", message, userId, username, requestUri, clientIp, null, category);
        }
    }

    @Override
    public void logError(String message, String userId, String username, String requestUri, String clientIp,
            Throwable throwable) {
        logError(message, userId, username, requestUri, clientIp, throwable, "com.feduwacomm");
    }
    
    @Override
    public void logError(String message, String userId, String username, String requestUri, String clientIp,
            Throwable throwable, String category) {
        // 控制台输出
        logger.error("ERROR - {} | 用户: {} | URI: {} | IP: {} | 类别: {} | 异常: {}",
                message, getUserIdDisplay(userId, username), requestUri, clientIp, category,
                throwable != null ? throwable.getMessage() : "无异常信息");

        // 数据库存储（如果启用）
        if (dbLoggingEnabled) {
            saveToDatabase("ERROR", message, userId, username, requestUri, clientIp,
                    throwable != null ? throwable.getMessage() : null, category);
        }
    }

    @Override
    public void logDebug(String message, String userId, String username, String requestUri, String clientIp) {
        logDebug(message, userId, username, requestUri, clientIp, "com.feduwacomm");
    }
    
    @Override
    public void logDebug(String message, String userId, String username, String requestUri, String clientIp, String category) {
        // 控制台输出
        logger.debug("DEBUG - {} | 用户: {} | URI: {} | IP: {} | 类别: {}",
                message, getUserIdDisplay(userId, username), requestUri, clientIp, category);

        // 数据库存储（如果启用）
        if (dbLoggingEnabled) {
            saveToDatabase("DEBUG", message, userId, username, requestUri, clientIp, null, category);
        }
    }

    /**
     * 保存日志到数据库
     */
    private void saveToDatabase(String level, String message, String userId, String username,
            String requestUri, String clientIp, String exception, String category) {
        try {
            String sql = "INSERT INTO system_logs (id, timestamp, level, logger, message, thread, " +
                    "user_id, username, request_uri, client_ip, environment, exception) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            jdbcTemplate.update(sql,
                    uuidUtil.generateUuid(),
                    LocalDateTime.now(),
                    level,
                    category != null ? category : "com.feduwacomm",
                    message,
                    Thread.currentThread().getName(),
                    userId,
                    username,
                    requestUri,
                    clientIp,
                    environment,
                    exception);
        } catch (Exception e) {
            logger.error("保存日志到数据库失败: {}", e.getMessage());
        }
    }

    @Override
    public PageResult<LogListVO> queryLogs(LogQueryDTO queryDTO) {
        List<SystemLog> logs = systemLogMapper.selectByCondition(queryDTO);
        long total = systemLogMapper.countByCondition(queryDTO);
        
        List<LogListVO> logVOs = logs.stream().map(this::convertToLogListVO).collect(Collectors.toList());
        
        return PageResult.<LogListVO>builder()
                .records(logVOs)
                .total(total)
                .current((long)queryDTO.getPage())
                .size((long)queryDTO.getSize())
                .pages((total + queryDTO.getSize() - 1) / queryDTO.getSize())
                .build();
    }
    
    @Override
    public LogDetailVO getLogDetail(String logId) {
        SystemLog log = systemLogMapper.selectById(logId);
        if (log == null) {
            return null;
        }
        return convertToLogDetailVO(log);
    }
    
    @Override
    public List<LogListVO> getRealtimeLogs(LogQueryDTO queryDTO) {
        // 实现实时日志查询逻辑
        List<SystemLog> logs = systemLogMapper.selectByCondition(queryDTO);
        return logs.stream().map(this::convertToLogListVO).collect(Collectors.toList());
    }
    
    @Override
    public LogStatisticsVO getStatistics(LogQueryDTO queryDTO) {
        // 实现统计逻辑
        return LogStatisticsVO.builder()
                .totalLogs(systemLogMapper.countByCondition(queryDTO))
                .build();
    }
    
    @Override
    public void logTask(String taskId, String level, String message, String source, String vmId, Object details) {
        try {
            String category = "TASK." + source;
            String detailsJson = details != null ? objectMapper.writeValueAsString(details) : null;
            String fullMessage = String.format("任务ID: %s | 来源: %s | 虚拟机: %s | %s", 
                    taskId, source, vmId, message);
            
            logInfo(fullMessage, null, null, null, null, category);
        } catch (Exception e) {
            logger.error("记录任务日志失败: {}", e.getMessage());
        }
    }
    
    @Override
    public void logVm(String vmId, String level, String operation, String message, String taskId, Object details) {
        try {
            String category = "VM." + operation;
            String detailsJson = details != null ? objectMapper.writeValueAsString(details) : null;
            String fullMessage = String.format("虚拟机ID: %s | 操作: %s | 任务ID: %s | %s", 
                    vmId, operation, taskId, message);
            
            switch (level.toUpperCase()) {
                case "INFO":
                    logInfo(fullMessage, null, null, null, null, category);
                    break;
                case "WARN":
                    logWarn(fullMessage, null, null, null, null, category);
                    break;
                case "ERROR":
                    logError(fullMessage, null, null, null, null, null, category);
                    break;
                case "DEBUG":
                    logDebug(fullMessage, null, null, null, null, category);
                    break;
            }
        } catch (Exception e) {
            logger.error("记录虚拟机日志失败: {}", e.getMessage());
        }
    }
    
    private LogListVO convertToLogListVO(SystemLog log) {
        return LogListVO.builder()
                .logId(log.getLogId())
                .createdAt(log.getCreatedAt() != null ? log.getCreatedAt() : log.getTimestamp())
                .level(LogLevel.valueOf(log.getLevel().toUpperCase()))
                .category(log.getCategory())
                .message(log.getMessage())
                .vmId(log.getVmId())
                .taskId(log.getTaskId())
                .build();
    }
    
    private LogDetailVO convertToLogDetailVO(SystemLog log) {
        return LogDetailVO.builder()
                .logId(log.getLogId())
                .createdAt(log.getCreatedAt() != null ? log.getCreatedAt() : log.getTimestamp())
                .level(LogLevel.valueOf(log.getLevel().toUpperCase()))
                .category(log.getCategory())
                .message(log.getMessage())
                .vmId(log.getVmId())
                .taskId(log.getTaskId())
                .build();
    }
    
    // ==================== 日志导出方法 ====================
    
    @Override
    public LogExportTaskVO createExportTask(LogExportDTO exportDTO) {
        try {
            String exportId = "export_" + System.currentTimeMillis() + "_" + UuidUtil.generateShortUuid();
            String taskId = uuidUtil.generateUuid();
            
            LogExportTask task = LogExportTask.builder()
                    .id(taskId)
                    .exportId(exportId)
                    .status(TaskStatus.PENDING)
                    .format(exportDTO.getFormat() != null ? exportDTO.getFormat() : LogExportFormat.CSV)
                    .filterConditions(objectMapper.writeValueAsString(exportDTO))
                    .progress(0)
                    .totalRecords(0L)
                    .processedRecords(0L)
                    .includeDetails(exportDTO.getIncludeDetails() != null ? exportDTO.getIncludeDetails() : true)
                    .estimatedTime(30)
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusDays(exportRetentionDays))
                    .build();
            
            logExportTaskMapper.insert(task);
            
            // 异步处理导出任务
            processExportTaskAsync(task, exportDTO);
            
            return convertToLogExportTaskVO(task);
        } catch (Exception e) {
            logger.error("创建导出任务失败: {}", e.getMessage());
            throw new RuntimeException("创建导出任务失败: " + e.getMessage());
        }
    }
    
    @Override
    public LogExportTaskVO getExportStatus(String exportId) {
        LogExportTask task = logExportTaskMapper.selectById(exportId);
        if (task == null) {
            throw new RuntimeException("导出任务不存在: " + exportId);
        }
        return convertToLogExportTaskVO(task);
    }
    
    @Override
    public byte[] downloadExport(String exportId) {
        LogExportTask task = logExportTaskMapper.selectById(exportId);
        if (task == null) {
            throw new RuntimeException("导出任务不存在: " + exportId);
        }
        
        if (task.getStatus() != TaskStatus.COMPLETED) {
            throw new RuntimeException("导出任务未完成");
        }
        
        if (task.getExpiresAt() != null && LocalDateTime.now().isAfter(task.getExpiresAt())) {
            throw new RuntimeException("导出文件已过期");
        }
        
        try {
            return Files.readAllBytes(Paths.get(task.getFilePath()));
        } catch (IOException e) {
            logger.error("读取导出文件失败: {}", e.getMessage());
            throw new RuntimeException("读取导出文件失败: " + e.getMessage());
        }
    }
    
    @Override
    public PageResult<LogExportTaskVO> getExportHistory(Integer page, Integer size, String status) {
        List<LogExportTask> tasks = logExportTaskMapper.selectByStatus(
                status != null ? TaskStatus.valueOf(status.toUpperCase()) : null, 
                (page - 1) * size, size);
        long total = logExportTaskMapper.countByStatus(
                status != null ? TaskStatus.valueOf(status.toUpperCase()) : null);
        
        List<LogExportTaskVO> taskVOs = tasks.stream()
                .map(this::convertToLogExportTaskVO)
                .collect(Collectors.toList());
        
        return PageResult.<LogExportTaskVO>builder()
                .records(taskVOs)
                .total(total)
                .current((long)page)
                .size((long)size)
                .pages((total + size - 1) / size)
                .build();
    }
    
    // ==================== 日志清理方法 ====================
    
    @Override
    public LogCleanupTaskVO createCleanupTask(LogCleanupDTO cleanupDTO) {
        try {
            String cleanupId = "cleanup_" + System.currentTimeMillis() + "_" + UuidUtil.generateShortUuid();
            String taskId = uuidUtil.generateUuid();
            
            LogCleanupTask task = LogCleanupTask.builder()
                    .cleanupId(cleanupId)
                    .status(TaskStatus.PENDING)
                    .strategy(cleanupDTO.getStrategy() != null ? 
                             cleanupDTO.getStrategy() : 
                             LogCleanupStrategy.TIME_BASED)
                    .cleanupConditions(objectMapper.writeValueAsString(cleanupDTO))
                    .progress(0)
                    .estimatedRecords(0L)
                    .deletedRecords(0L)
                    .estimatedSize(0L)
                    .freedSpace(0L)
                    .dryRun(cleanupDTO.getDryRun() != null ? cleanupDTO.getDryRun() : false)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            logCleanupTaskMapper.insert(task);
            
            // 异步处理清理任务
            processCleanupTaskAsync(task, cleanupDTO);
            
            return convertToLogCleanupTaskVO(task);
        } catch (Exception e) {
            logger.error("创建清理任务失败: {}", e.getMessage());
            throw new RuntimeException("创建清理任务失败: " + e.getMessage());
        }
    }
    
    @Override
    public LogCleanupTaskVO getCleanupStatus(String cleanupId) {
        LogCleanupTask task = logCleanupTaskMapper.selectById(cleanupId);
        if (task == null) {
            throw new RuntimeException("清理任务不存在: " + cleanupId);
        }
        return convertToLogCleanupTaskVO(task);
    }
    
    @Override
    public PageResult<LogCleanupTaskVO> getCleanupHistory(Integer page, Integer size, String status) {
        List<LogCleanupTask> tasks = logCleanupTaskMapper.selectByStatus(
                status != null ? TaskStatus.valueOf(status.toUpperCase()) : null, 
                (page - 1) * size, size);
        long total = logCleanupTaskMapper.countByStatus(
                status != null ? TaskStatus.valueOf(status.toUpperCase()) : null);
        
        List<LogCleanupTaskVO> taskVOs = tasks.stream()
                .map(this::convertToLogCleanupTaskVO)
                .collect(Collectors.toList());
        
        return PageResult.<LogCleanupTaskVO>builder()
                .records(taskVOs)
                .total(total)
                .current((long)page)
                .size((long)size)
                .pages((total + size - 1) / size)
                .build();
    }
    
    // ==================== 系统监控方法 ====================
    
    @Override
    public LogMonitorVO getSystemMonitor() {
        // 实现系统监控数据收集
        Map<String, Object> systemInfoMap = getSystemInfo();
        Map<String, Object> resourceUsageMap = getResourceUsage();
        Map<String, Object> appMetricsMap = getApplicationMetrics();
        Map<String, Object> dbMetricsMap = getDatabaseMetrics();
        
        return LogMonitorVO.builder()
                .systemInfo(LogMonitorVO.SystemInfo.builder()
                        .version((String) systemInfoMap.get("version"))
                        .uptime((Long) systemInfoMap.get("uptime"))
                        .build())
                .resourceUsage(LogMonitorVO.ResourceUsage.builder()
                        .cpuUsage((Double) resourceUsageMap.get("cpuUsage"))
                        .memoryUsage((Double) resourceUsageMap.get("memoryUsage"))
                        .build())
                .applicationMetrics(LogMonitorVO.ApplicationMetrics.builder()
                        .activeConnections((Integer) appMetricsMap.get("activeConnections"))
                        .requestPerSecond((Double) appMetricsMap.get("requestPerSecond"))
                        .build())
                .databaseMetrics(LogMonitorVO.DatabaseMetrics.builder()
                        .activeConnections((Integer) dbMetricsMap.get("activeConnections"))
                        .queryPerSecond((Double) dbMetricsMap.get("queryPerSecond"))
                        .build())
                .build();
    }
    
    @Override
    public LogMonitorVO getLogMonitor(String timeRange, String level) {
        // 实现日志监控数据收集
        Map<String, Object> logMetricsMap = getLogMetrics(timeRange, level);
        
        return LogMonitorVO.builder()
                .logMetrics(LogMonitorVO.LogMetrics.builder()
                        .totalLogs((Long) logMetricsMap.get("totalLogs"))
                        .errorCount((Long) logMetricsMap.get("errorCount"))
                        .build())
                .levelTrend(getLevelTrend(timeRange).stream().map(data -> {
                        Map<String, Long> dataMap = new HashMap<>();
                        data.forEach((key, value) -> {
                            if (!"timestamp".equals(key) && value instanceof Number) {
                                dataMap.put(key, ((Number) value).longValue());
                            }
                        });
                        return LogMonitorVO.TimeSeriesData.builder()
                                .timestamp(LocalDateTime.parse((String) data.get("timestamp")))
                                .data(dataMap)
                                .build();
                }).collect(Collectors.toList()))
                .categoryTrend(getCategoryTrend(timeRange).stream().map(data -> {
                        Map<String, Long> dataMap = new HashMap<>();
                        data.forEach((key, value) -> {
                            if (!"timestamp".equals(key) && value instanceof Number) {
                                dataMap.put(key, ((Number) value).longValue());
                            }
                        });
                        return LogMonitorVO.TimeSeriesData.builder()
                                .timestamp(LocalDateTime.parse((String) data.get("timestamp")))
                                .data(dataMap)
                                .build();
                }).collect(Collectors.toList()))
                .recentErrors(getRecentErrors(timeRange).stream().map(data ->
                        LogMonitorVO.RecentError.builder()
                                .message((String) data.get("message"))
                                .createdAt(LocalDateTime.parse((String) data.get("createdAt")))
                                .build()).collect(Collectors.toList()))
                .build();
    }
    
    @Override
    public LogMonitorVO getPerformanceMonitor(String timeRange, String endpoint) {
        // 实现性能监控数据收集
        Map<String, Object> apiMetricsMap = getApiMetrics(timeRange, endpoint);
        
        return LogMonitorVO.builder()
                .apiMetrics(LogMonitorVO.ApiMetrics.builder()
                        .totalRequests((Long) apiMetricsMap.get("totalRequests"))
                        .successRate((Double) apiMetricsMap.get("successRate"))
                        .build())
                .endpointMetrics(getEndpointMetrics(timeRange).stream().map(data ->
                        LogMonitorVO.EndpointMetric.builder()
                                .endpoint((String) data.get("endpoint"))
                                .requestCount((Long) data.get("requestCount"))
                                .build()).collect(Collectors.toList()))
                .responseTimeTrend(getResponseTimeTrend(timeRange).stream().map(data ->
                        LogMonitorVO.ResponseTimeData.builder()
                                .timestamp(LocalDateTime.parse((String) data.get("timestamp")))
                                .average((Double) data.get("average"))
                                .build()).collect(Collectors.toList()))
                .build();
    }
    
    @Override
    public LogMonitorVO getAlerts() {
        // 实现告警数据收集
        return LogMonitorVO.builder()
                .alerts(getAlertsData().stream().map(data ->
                        LogMonitorVO.Alert.builder()
                                .name((String) data.get("name"))
                                .status((String) data.get("status"))
                                .build()).collect(Collectors.toList()))
                .alertHistory(getAlertHistory().stream().map(data ->
                        LogMonitorVO.AlertHistory.builder()
                                .message((String) data.get("message"))
                                .severity((String) data.get("severity"))
                                .build()).collect(Collectors.toList()))
                .build();
    }
    
    // ==================== 日志配置方法 ====================
    
    @Override
    public LogConfigVO getLogConfig() {
        // 实现配置查询
        return LogConfigVO.builder()
                .logLevel(LogLevel.fromCode("INFO"))
                .retentionDays(30)
                .maxFileSize(104857600L)
                .categories(null)
                .exportSettings(null)
                .build();
    }
    
    @Override
    public void updateLogConfig(LogConfigDTO configDTO) {
        // 实现配置更新逻辑
        logger.info("更新日志配置: {}", configDTO);
        // TODO: 实现动态配置更新
    }
    
    // ==================== 异步任务处理方法 ====================
    
    @Async
    public void processExportTaskAsync(LogExportTask task, LogExportDTO exportDTO) {
        try {
            // 更新任务状态为处理中
            task.setStatus(TaskStatus.PROCESSING);
            task.setStartedAt(LocalDateTime.now());
            logExportTaskMapper.updateStatus(task.getExportId(), TaskStatus.PROCESSING, 0, 0L, 0L, null, null);
            
            // 模拟导出处理
            Thread.sleep(5000); // 模拟处理时间
            
            // 创建导出文件
            String fileName = task.getExportId() + "." + task.getFormat().name().toLowerCase();
            String filePath = exportBasePath + "/" + fileName;
            Files.createDirectories(Paths.get(exportBasePath));
            
            // 生成示例导出内容
            String content = generateExportContent(exportDTO, task.getFormat());
            Files.write(Paths.get(filePath), content.getBytes());
            
            // 更新任务完成状态
            task.setStatus(TaskStatus.COMPLETED);
            task.setProgress(100);
            task.setFileSize((long) content.length());
            task.setFilePath(filePath);
            task.setDownloadUrl("/api/log/export/download/" + task.getExportId());
            task.setCompletedAt(LocalDateTime.now());
            
            logExportTaskMapper.updateStatus(task.getExportId(), TaskStatus.COMPLETED, 
                    100, task.getProcessedRecords(), task.getFileSize(), 
                    LocalDateTime.now(), null);
            
        } catch (Exception e) {
            logger.error("导出任务处理失败: {}", e.getMessage());
            logExportTaskMapper.updateStatus(task.getExportId(), TaskStatus.FAILED, 
                    task.getProgress(), task.getProcessedRecords(), 0L, 
                    LocalDateTime.now(), e.getMessage());
        }
    }
    
    @Async
    public void processCleanupTaskAsync(LogCleanupTask task, LogCleanupDTO cleanupDTO) {
        try {
            // 更新任务状态为处理中
            task.setStatus(TaskStatus.PROCESSING);
            logCleanupTaskMapper.updateStatus(task.getCleanupId(), TaskStatus.PROCESSING, 0, 0L, 0L, null, null);
            
            // 模拟清理处理
            Thread.sleep(3000); // 模拟处理时间
            
            // 更新任务完成状态
            task.setStatus(TaskStatus.COMPLETED);
            task.setProgress(100);
            task.setDeletedRecords(1000L);
            task.setFreedSpace(10485760L); // 10MB
            
            logCleanupTaskMapper.updateStatus(task.getCleanupId(), TaskStatus.COMPLETED,
                    100, 1000L, 10485760L, LocalDateTime.now(), null);
            
        } catch (Exception e) {
            logger.error("清理任务处理失败: {}", e.getMessage());
            logCleanupTaskMapper.updateStatus(task.getCleanupId(), TaskStatus.FAILED,
                    task.getProgress(), 0L, 0L, LocalDateTime.now(), e.getMessage());
        }
    }
    
    // ==================== 辅助方法 ====================
    
    private LogExportTaskVO convertToLogExportTaskVO(LogExportTask task) {
        return LogExportTaskVO.builder()
                .exportId(task.getExportId())
                .status(task.getStatus())
                .format(task.getFormat())
                .progress(task.getProgress())
                .totalRecords(task.getTotalRecords())
                .processedRecords(task.getProcessedRecords())
                .fileSize(task.getFileSize())
                .downloadUrl(task.getDownloadUrl())
                .estimatedTime(task.getEstimatedTime())
                .expiresAt(task.getExpiresAt())
                .createdAt(task.getCreatedAt())
                .completedAt(task.getCompletedAt())
                .errorMessage(task.getErrorMessage())
                .build();
    }
    
    private LogCleanupTaskVO convertToLogCleanupTaskVO(LogCleanupTask task) {
        return LogCleanupTaskVO.builder()
                .cleanupId(task.getCleanupId())
                .status(task.getStatus())
                .strategy(task.getStrategy())
                .progress(task.getProgress())
                .deletedRecords(task.getDeletedRecords())
                .freedSpace(task.getFreedSpace())
                .createdAt(task.getCreatedAt())
                .completedAt(task.getCompletedAt())
                .build();
    }
    
    private String generateExportContent(LogExportDTO exportDTO, LogExportFormat format) {
        // 简单的示例导出内容生成
        if (format == LogExportFormat.CSV) {
            return "timestamp,level,category,message\n2024-01-01 10:00:00,INFO,SYSTEM,系统启动成功\n";
        } else if (format == LogExportFormat.JSON) {
            return "[{\"timestamp\":\"2024-01-01T10:00:00\",\"level\":\"INFO\",\"category\":\"SYSTEM\",\"message\":\"系统启动成功\"}]";
        }
        return "导出内容";
    }
    
    // 监控数据收集方法（示例实现）
    private Map<String, Object> getSystemInfo() { 
        return Map.of("version", "1.0.0", "uptime", 86400);
    }
    private Map<String, Object> getResourceUsage() { 
        return Map.of("cpuUsage", 25.5, "memoryUsage", 60.2);
    }
    private Map<String, Object> getApplicationMetrics() { 
        return Map.of("activeConnections", 150, "requestPerSecond", 25.5);
    }
    private Map<String, Object> getDatabaseMetrics() { 
        return Map.of("activeConnections", 20, "queryPerSecond", 100);
    }
    private Map<String, Object> getLogMetrics(String timeRange, String level) { 
        return Map.of("totalLogs", 10000, "errorCount", 50);
    }
    private List<Map<String, Object>> getLevelTrend(String timeRange) { 
        return List.of(Map.of("timestamp", "2024-01-01T10:00:00", "INFO", 500, "ERROR", 10));
    }
    private List<Map<String, Object>> getCategoryTrend(String timeRange) { 
        return List.of(Map.of("timestamp", "2024-01-01T10:00:00", "SYSTEM", 200, "USER", 150));
    }
    private List<Map<String, Object>> getRecentErrors(String timeRange) { 
        return List.of(Map.of("message", "任务执行失败", "createdAt", "2024-01-01T10:00:00"));
    }
    private Map<String, Object> getApiMetrics(String timeRange, String endpoint) { 
        return Map.of("totalRequests", 50000, "successRate", 99.0);
    }
    private List<Map<String, Object>> getEndpointMetrics(String timeRange) { 
        return List.of(Map.of("endpoint", "/api/user/login", "requestCount", 1000));
    }
    private List<Map<String, Object>> getResponseTimeTrend(String timeRange) { 
        return List.of(Map.of("timestamp", "2024-01-01T10:00:00", "average", 200));
    }
    private List<Map<String, Object>> getAlertsData() { 
        return List.of(Map.of("name", "错误率告警", "status", "ACTIVE"));
    }
    private List<Map<String, Object>> getAlertHistory() { 
        return List.of(Map.of("message", "错误率超过阈值", "severity", "HIGH"));
    }
    private Map<String, Object> getLogCategories() {
        return Map.of("SYSTEM", Map.of("level", "INFO", "enabled", true));
    }
    private Map<String, Object> getExportSettings() {
        return Map.of("maxRecordsPerExport", 100000, "exportRetentionDays", 7);
    }
    
    /**
     * 获取用户显示信息
     */
    private String getUserIdDisplay(String userId, String username) {
        if (userId != null && username != null) {
            return username + "(" + userId + ")";
        } else if (username != null) {
            return username;
        } else {
            return "匿名";
        }
    }
}