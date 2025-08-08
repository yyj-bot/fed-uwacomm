package com.feduwacomm.service.impl;

import com.feduwacomm.service.LogService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.feduwacomm.utils.UuidUtil;

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

    @Value("${logging.db.enabled:false}")
    private boolean dbLoggingEnabled;

    @Value("${spring.profiles.active:dev}")
    private String environment;

    @Override
    public void logInfo(String message, String userId, String username, String requestUri, String clientIp) {
        // 控制台输出
        logger.info("INFO - {} | 用户: {} | URI: {} | IP: {}",
                message, getUserIdDisplay(userId, username), requestUri, clientIp);

        // 数据库存储（如果启用）
        if (dbLoggingEnabled) {
            saveToDatabase("INFO", message, userId, username, requestUri, clientIp, null);
        }
    }

    @Override
    public void logWarn(String message, String userId, String username, String requestUri, String clientIp) {
        // 控制台输出
        logger.warn("WARN - {} | 用户: {} | URI: {} | IP: {}",
                message, getUserIdDisplay(userId, username), requestUri, clientIp);

        // 数据库存储（如果启用）
        if (dbLoggingEnabled) {
            saveToDatabase("WARN", message, userId, username, requestUri, clientIp, null);
        }
    }

    @Override
    public void logError(String message, String userId, String username, String requestUri, String clientIp,
            Throwable throwable) {
        // 控制台输出
        logger.error("ERROR - {} | 用户: {} | URI: {} | IP: {} | 异常: {}",
                message, getUserIdDisplay(userId, username), requestUri, clientIp,
                throwable != null ? throwable.getMessage() : "无异常信息");

        // 数据库存储（如果启用）
        if (dbLoggingEnabled) {
            saveToDatabase("ERROR", message, userId, username, requestUri, clientIp,
                    throwable != null ? throwable.getMessage() : null);
        }
    }

    @Override
    public void logDebug(String message, String userId, String username, String requestUri, String clientIp) {
        // 控制台输出
        logger.debug("DEBUG - {} | 用户: {} | URI: {} | IP: {}",
                message, getUserIdDisplay(userId, username), requestUri, clientIp);

        // 数据库存储（如果启用）
        if (dbLoggingEnabled) {
            saveToDatabase("DEBUG", message, userId, username, requestUri, clientIp, null);
        }
    }

    /**
     * 保存日志到数据库
     */
    private void saveToDatabase(String level, String message, String userId, String username,
            String requestUri, String clientIp, String exception) {
        try {
            String sql = "INSERT INTO system_logs (id, timestamp, level, logger, message, thread, " +
                    "user_id, username, request_uri, client_ip, environment, exception) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            jdbcTemplate.update(sql,
                    uuidUtil.generateLogId(),
                    LocalDateTime.now(),
                    level,
                    "com.feduwacomm",
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