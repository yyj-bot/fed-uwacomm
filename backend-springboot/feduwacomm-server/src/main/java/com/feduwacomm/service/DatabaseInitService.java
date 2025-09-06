package com.feduwacomm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库初始化服务
 * 检查并创建必要的数据库表
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Service
public class DatabaseInitService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitService.class);

    @Autowired
    private DataSource dataSource;

    /**
     * 检查并初始化数据库
     */
    public void checkAndInitDatabase() {
        log.info("开始检查数据库初始化状态...");
        
        try {
            // 检查users表是否存在
            if (!tableExists("users")) {
                log.warn("users表不存在，开始创建...");
                createUsersTable();
                log.info("users表创建成功");
            } else {
                log.info("users表已存在");
            }

            // 检查user_permissions表是否存在
            if (!tableExists("user_permissions")) {
                log.warn("user_permissions表不存在，开始创建...");
                createUserPermissionsTable();
                log.info("user_permissions表创建成功");
            } else {
                log.info("user_permissions表已存在");
            }

            // 检查vm_instances表是否存在
            if (!tableExists("vm_instances")) {
                log.warn("vm_instances表不存在，开始创建...");
                createVmInstancesTable();
                log.info("vm_instances表创建成功");
            } else {
                log.info("vm_instances表已存在");
            }

            log.info("数据库初始化检查完成");
            
        } catch (Exception e) {
            log.error("数据库初始化检查失败", e);
            throw new RuntimeException("数据库初始化失败", e);
        }
    }

    /**
     * 检查表是否存在
     */
    private boolean tableExists(String tableName) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? AND table_name = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, connection.getCatalog());
                stmt.setString(2, tableName);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        }
    }

    /**
     * 创建users表
     */
    private void createUsersTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                id VARCHAR(32) PRIMARY KEY COMMENT '用户唯一标识(32位UUID)',
                username VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名',
                email VARCHAR(100) UNIQUE NOT NULL COMMENT '邮箱地址',
                password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希值',
                role ENUM('ADMIN', 'RESEARCHER', 'OPERATOR', 'VIEWER') NOT NULL DEFAULT 'VIEWER' COMMENT '用户角色',
                status ENUM('ACTIVE', 'INACTIVE', 'LOCKED', 'DELETED') NOT NULL DEFAULT 'ACTIVE' COMMENT '用户状态',
                last_login_time TIMESTAMP NULL COMMENT '最后登录时间',
                last_login_ip VARCHAR(45) COMMENT '最后登录IP',
                login_attempts INT DEFAULT 0 COMMENT '登录失败次数',
                locked_until TIMESTAMP NULL COMMENT '锁定截止时间',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                created_by VARCHAR(32) NULL COMMENT '创建者ID(32位UUID)',
                updated_by VARCHAR(32) NULL COMMENT '更新者ID(32位UUID)'
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表'
            """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    /**
     * 创建user_permissions表
     */
    private void createUserPermissionsTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS user_permissions (
                id VARCHAR(32) PRIMARY KEY COMMENT '权限唯一标识(32位UUID)',
                user_id VARCHAR(32) NOT NULL COMMENT '用户ID(32位UUID)',
                resource_type ENUM('VM', 'TASK', 'DATA', 'MODEL', 'SYSTEM', 'USER') NOT NULL COMMENT '资源类型',
                resource_id VARCHAR(32) NULL COMMENT '资源ID(32位UUID，NULL表示所有资源)',
                permission ENUM('READ', 'WRITE', 'DELETE', 'EXECUTE', 'ADMIN') NOT NULL COMMENT '权限类型',
                granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                granted_by VARCHAR(32) NOT NULL COMMENT '授权者ID(32位UUID)',
                expires_at TIMESTAMP NULL COMMENT '权限过期时间'
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户权限表'
            """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    /**
     * 创建vm_instances表
     */
    private void createVmInstancesTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS vm_instances (
                id VARCHAR(32) PRIMARY KEY COMMENT '虚拟机唯一标识(32位UUID)',
                name VARCHAR(100) NOT NULL COMMENT '虚拟机名称',
                ip_address VARCHAR(45) NOT NULL COMMENT 'IP地址',
                port INT NOT NULL DEFAULT 22 COMMENT 'SSH端口',
                os_type VARCHAR(50) NOT NULL COMMENT '操作系统类型',
                cpu_cores INT NOT NULL COMMENT 'CPU核心数',
                memory_mb INT NOT NULL COMMENT '内存大小(MB)',
                disk_gb INT NOT NULL COMMENT '磁盘大小(GB)',
                status ENUM('OFFLINE', 'RUNNING', 'STOPPED', 'STARTING', 'STOPPING', 'ERROR') NOT NULL DEFAULT 'OFFLINE' COMMENT '虚拟机状态',
                connection_status ENUM('DISCONNECTED', 'CONNECTED', 'CONNECTING', 'RECONNECTING') NOT NULL DEFAULT 'DISCONNECTED' COMMENT 'WebSocket连接状态',
                ws_session_id VARCHAR(32) NULL COMMENT 'WebSocket会话ID',
                last_heartbeat TIMESTAMP NULL COMMENT '最后心跳时间',
                secret_id VARCHAR(128) NULL COMMENT '长期刷新凭证',
                secret_expire_time TIMESTAMP NULL COMMENT '刷新凭证过期时间',
                system_info JSON NULL COMMENT '系统信息(JSON格式)',
                capabilities JSON NULL COMMENT '能力信息(JSON格式)',
                network_config JSON NULL COMMENT '网络配置信息(JSON格式)',
                metadata JSON NULL COMMENT '元数据信息(JSON格式)',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                created_by VARCHAR(32) NULL COMMENT '创建者ID(32位UUID)',
                updated_by VARCHAR(32) NULL COMMENT '更新者ID(32位UUID)',
                INDEX idx_vm_status (status),
                INDEX idx_vm_connection_status (connection_status),
                INDEX idx_vm_ip_address (ip_address),
                INDEX idx_vm_secret_id (secret_id),
                INDEX idx_vm_created_at (created_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='虚拟机实例表'
            """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    /**
     * 检查表数据
     */
    public void checkTableData() {
        try {
            checkUsersTableData();
            checkUserPermissionsTableData();
            checkVmInstancesTableData();
        } catch (Exception e) {
            log.error("检查表数据失败", e);
        }
    }

    /**
     * 检查users表数据
     */
    private void checkUsersTableData() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            String sql = "SELECT COUNT(*) as count FROM users";
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    log.info("users表中共有 {} 条记录", count);
                }
            }
        }
    }

    /**
     * 检查user_permissions表数据
     */
    private void checkUserPermissionsTableData() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            String sql = "SELECT COUNT(*) as count FROM user_permissions";
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    log.info("user_permissions表中共有 {} 条记录", count);
                }
            }
        }
    }

    /**
     * 检查vm_instances表数据
     */
    private void checkVmInstancesTableData() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            String sql = "SELECT COUNT(*) as count FROM vm_instances";
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    log.info("vm_instances表中共有 {} 条记录", count);
                }
            }
        }
    }
} 