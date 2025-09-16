package com.feduwacomm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 数据库健康检查服务
 * 提供数据库状态监控和定期检查功能
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Service
public class DatabaseHealthService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseHealthService.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private DataSource dataSource;

    private final AtomicBoolean isHealthy = new AtomicBoolean(false);
    private LocalDateTime lastCheckTime;
    private String lastError;

    /**
     * 检查数据库健康状态
     */
    public boolean checkHealth() {
        try {
            // 测试基本连接
            testConnection();
            
            // 测试简单查询
            testSimpleQuery();
            
            // 测试表是否存在
            testTableExistence();
            
            isHealthy.set(true);
            lastCheckTime = LocalDateTime.now();
            lastError = null;
            
            log.debug("数据库健康检查通过 - {}", lastCheckTime.format(formatter));
            return true;
            
        } catch (Exception e) {
            isHealthy.set(false);
            lastError = e.getMessage();
            lastCheckTime = LocalDateTime.now();
            
            log.error("数据库健康检查失败 - {}: {}", lastCheckTime.format(formatter), e.getMessage());
            return false;
        }
    }

    /**
     * 测试数据库连接
     */
    private void testConnection() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(5)) {
                throw new SQLException("数据库连接无效");
            }
        }
    }

    /**
     * 测试简单查询
     */
    private void testSimpleQuery() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement("SELECT 1");
             ResultSet rs = stmt.executeQuery()) {
            
            if (!rs.next() || rs.getInt(1) != 1) {
                throw new SQLException("简单查询测试失败");
            }
        }
    }

    /**
     * 测试关键表是否存在
     */
    private void testTableExistence() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            
            // 获取当前数据库名用于调试
            String currentDatabase;
            try (PreparedStatement dbStmt = connection.prepareStatement("SELECT DATABASE()");
                 ResultSet dbRs = dbStmt.executeQuery()) {
                currentDatabase = dbRs.next() ? dbRs.getString(1) : "unknown";
            }
            
            log.debug("当前连接数据库: {}", currentDatabase);
            
            // 检查用户表是否存在
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? AND table_name = ?")) {
                
                stmt.setString(1, currentDatabase);
                stmt.setString(2, "users");
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int count = rs.getInt(1);
                        if (count == 0) {
                            log.warn("用户表不存在，数据库可能未初始化 (数据库: {})", currentDatabase);
                        } else {
                            log.debug("用户表存在，数据库状态正常 (数据库: {}, 表数量: {})", currentDatabase, count);
                        }
                    }
                }
            }
            
            // 额外检查：直接尝试查询用户表
            try (PreparedStatement directStmt = connection.prepareStatement("SELECT COUNT(*) FROM users LIMIT 1")) {
                try (ResultSet directRs = directStmt.executeQuery()) {
                    if (directRs.next()) {
                        log.debug("直接查询用户表成功，表确实存在");
                    }
                }
            } catch (SQLException e) {
                log.warn("直接查询用户表失败: {}", e.getMessage());
                throw e;
            }
        }
    }

    /**
     * 获取数据库健康状态
     */
    public boolean isHealthy() {
        return isHealthy.get();
    }

    /**
     * 获取最后检查时间
     */
    public LocalDateTime getLastCheckTime() {
        return lastCheckTime;
    }

    /**
     * 获取最后错误信息
     */
    public String getLastError() {
        return lastError;
    }

    /**
     * 获取数据库状态摘要
     */
    public String getStatusSummary() {
        if (lastCheckTime == null) {
            return "未检查";
        }
        
        if (isHealthy.get()) {
            return String.format("健康 - 最后检查: %s", lastCheckTime.format(formatter));
        } else {
            return String.format("异常 - 最后检查: %s, 错误: %s", 
                lastCheckTime.format(formatter), lastError);
        }
    }

    /**
     * 定期健康检查（每5分钟）
     */
    @Scheduled(fixedRate = 300000) // 5分钟 = 300000毫秒
    public void scheduledHealthCheck() {
        log.debug("执行定期数据库健康检查...");
        checkHealth();
    }

    /**
     * 强制健康检查
     */
    public boolean forceHealthCheck() {
        log.info("执行强制数据库健康检查...");
        return checkHealth();
    }
} 