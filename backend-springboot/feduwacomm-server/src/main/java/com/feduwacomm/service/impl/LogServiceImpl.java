package com.feduwacomm.service.impl;

import com.feduwacomm.common.PageResult;
import com.feduwacomm.dto.LogQueryDTO;
import com.feduwacomm.entity.SystemLog;
import com.feduwacomm.enums.LogLevel;
import com.feduwacomm.mapper.SystemLogMapper;
import com.feduwacomm.service.LogService;
import com.feduwacomm.vo.LogDetailVO;
import com.feduwacomm.vo.LogListVO;
import com.feduwacomm.vo.LogStatisticsVO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.feduwacomm.utils.UuidUtil;
import java.util.List;
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
    private ObjectMapper objectMapper;

    @Value("${logging.db.enabled:false}")
    private boolean dbLoggingEnabled;

    @Value("${spring.profiles.active:dev}")
    private String environment;

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