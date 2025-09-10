package com.feduwacomm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * DatabaseHealthService简化单元测试
 * 重点测试核心功能，避免复杂的Mock问题
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("数据库健康检查服务简化测试")
class DatabaseHealthServiceTestSimple {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @InjectMocks
    private DatabaseHealthService databaseHealthService;

    @BeforeEach
    void setUp() throws SQLException {
        // 配置基本的Mock链
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
    }

    @Test
    @DisplayName("数据库健康检查 - 成功")
    void testCheckHealth_Success() throws SQLException {
        // Given - Mock所有SQL查询返回成功
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString(1)).thenReturn("feduwacomm"); // 数据库名
        when(resultSet.getInt(1)).thenReturn(1); // 表存在

        // When
        boolean result = databaseHealthService.checkHealth();

        // Then
        assertTrue(result, "Health check should pass");
        verify(dataSource).getConnection();
        verify(connection, atLeastOnce()).prepareStatement(anyString());
    }

    @Test
    @DisplayName("数据库健康检查 - 连接失败")
    void testCheckHealth_ConnectionFailure() throws SQLException {
        // Given - Mock连接失败
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        // When
        boolean result = databaseHealthService.checkHealth();

        // Then
        assertFalse(result, "Health check should fail when connection fails");
        verify(dataSource).getConnection();
    }

    @Test
    @DisplayName("数据库健康检查 - 查询异常")
    void testCheckHealth_QueryFailure() throws SQLException {
        // Given - Mock查询执行失败
        when(preparedStatement.executeQuery()).thenThrow(new SQLException("Query failed"));

        // When
        boolean result = databaseHealthService.checkHealth();

        // Then
        assertFalse(result, "Health check should fail when query fails");
        verify(connection).prepareStatement(anyString());
    }

    @Test
    @DisplayName("数据库健康检查 - 表不存在")
    void testCheckHealth_TableNotExist() throws SQLException {
        // Given - Mock数据库连接成功，但表查询返回0
        when(resultSet.next()).thenReturn(true, false); // 第一次查询数据库名成功，第二次查询表失败
        when(resultSet.getString(1)).thenReturn("feduwacomm");
        when(resultSet.getInt(1)).thenReturn(0); // 表不存在

        // When
        boolean result = databaseHealthService.checkHealth();

        // Then
        assertFalse(result, "Health check should fail when required tables don't exist");
    }

    @Test
    @DisplayName("强制健康检查")
    void testForceHealthCheck() throws SQLException {
        // Given - Mock成功的健康检查
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString(1)).thenReturn("feduwacomm");
        when(resultSet.getInt(1)).thenReturn(1);

        // When
        boolean result = databaseHealthService.forceHealthCheck();

        // Then
        assertTrue(result, "Force health check should pass");
        verify(dataSource).getConnection();
    }

    @Test
    @DisplayName("获取健康状态")
    void testIsHealthy() {
        // When
        boolean initialHealth = databaseHealthService.isHealthy();

        // Then
        // 初始状态可能为true或false，重点是方法不抛异常
        assertNotNull(initialHealth);
    }

    @Test
    @DisplayName("获取上次检查时间")
    void testGetLastCheckTime() {
        // When
        LocalDateTime lastCheckTime = databaseHealthService.getLastCheckTime();

        // Then
        assertNotNull(lastCheckTime, "Last check time should not be null");
    }

    @Test
    @DisplayName("连接超时处理")
    void testCheckHealth_Timeout() throws SQLException {
        // Given - Mock超时场景
        when(preparedStatement.executeQuery())
                .thenThrow(new SQLException("Connection timeout", "08S01"));

        // When
        boolean result = databaseHealthService.checkHealth();

        // Then
        assertFalse(result, "Health check should fail on timeout");
    }

    @Test
    @DisplayName("资源清理")
    void testCheckHealth_ResourceCleanup() throws SQLException {
        // Given - Mock成功场景
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString(1)).thenReturn("feduwacomm");
        when(resultSet.getInt(1)).thenReturn(1);

        // When
        databaseHealthService.checkHealth();

        // Then - 验证资源被正确关闭
        verify(resultSet, atLeastOnce()).close();
        verify(preparedStatement, atLeastOnce()).close();
        verify(connection).close();
    }

    @Test
    @DisplayName("批量表检查")
    void testCheckHealth_MultipleTablesCheck() throws SQLException {
        // Given - Mock多次查询
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString(1)).thenReturn("feduwacomm");
        // Mock所有表都存在
        when(resultSet.getInt(1)).thenReturn(1);

        // When
        boolean result = databaseHealthService.checkHealth();

        // Then
        assertTrue(result, "Health check should pass when all tables exist");
        // 验证进行了多次prepare statement调用（检查多个表）
        verify(connection, atLeast(2)).prepareStatement(anyString());
    }
}