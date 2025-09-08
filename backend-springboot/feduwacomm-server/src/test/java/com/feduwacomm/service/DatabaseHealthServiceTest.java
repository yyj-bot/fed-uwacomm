package com.feduwacomm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DatabaseHealthService单元测试类
 * 测试数据库健康检查服务的各种功能
 */
@ExtendWith(MockitoExtension.class)
public class DatabaseHealthServiceTest {

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
    void setUp() {
        // 重置mock对象
        reset(dataSource, connection, preparedStatement, resultSet);
    }

    /**
     * 测试数据库健康检查 - 成功场景
     */
    @Test
    void testCheckHealth_Success() throws SQLException {
        // 准备mock数据
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);
        when(connection.prepareStatement("SELECT 1")).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(1);
        
        // Mock表存在检查 - 为具体的SQL设置Mock
        PreparedStatement tableStmt = mock(PreparedStatement.class);
        when(connection.prepareStatement(contains("information_schema.tables"))).thenReturn(tableStmt);
        when(connection.getCatalog()).thenReturn("feduwacomm");
        
        // Mock表检查查询结果
        ResultSet tableRs = mock(ResultSet.class);
        when(tableStmt.executeQuery()).thenReturn(tableRs);
        when(tableRs.next()).thenReturn(true);
        when(tableRs.getInt(1)).thenReturn(1); // 表存在

        // 执行测试
        boolean result = databaseHealthService.checkHealth();

        // 验证结果
        assertTrue(result);
        assertTrue(databaseHealthService.isHealthy());
        assertNotNull(databaseHealthService.getLastCheckTime());
        assertNull(databaseHealthService.getLastError());
        assertTrue(databaseHealthService.getStatusSummary().contains("健康"));

        // 验证mock调用
        verify(dataSource, times(3)).getConnection(); // testConnection + testSimpleQuery + testTableExistence
        verify(connection, atLeastOnce()).isValid(5);
        verify(preparedStatement, atLeastOnce()).executeQuery();
    }

    /**
     * 测试数据库健康检查 - 连接失败
     */
    @Test
    void testCheckHealth_ConnectionFailed() throws SQLException {
        // 准备mock数据 - 连接失败
        when(dataSource.getConnection()).thenThrow(new SQLException("连接数据库失败"));

        // 执行测试
        boolean result = databaseHealthService.checkHealth();

        // 验证结果
        assertFalse(result);
        assertFalse(databaseHealthService.isHealthy());
        assertNotNull(databaseHealthService.getLastCheckTime());
        assertNotNull(databaseHealthService.getLastError());
        assertTrue(databaseHealthService.getLastError().contains("连接数据库失败"));
        assertTrue(databaseHealthService.getStatusSummary().contains("异常"));

        // 验证mock调用
        verify(dataSource).getConnection();
    }

    /**
     * 测试数据库健康检查 - 连接无效
     */
    @Test
    void testCheckHealth_InvalidConnection() throws SQLException {
        // 准备mock数据 - 连接无效
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(false);

        // 执行测试
        boolean result = databaseHealthService.checkHealth();

        // 验证结果
        assertFalse(result);
        assertFalse(databaseHealthService.isHealthy());
        assertNotNull(databaseHealthService.getLastError());
        assertTrue(databaseHealthService.getLastError().contains("数据库连接无效"));

        // 验证mock调用
        verify(dataSource).getConnection();
        verify(connection).isValid(5);
    }

    /**
     * 测试数据库健康检查 - 简单查询失败
     */
    @Test
    void testCheckHealth_SimpleQueryFailed() throws SQLException {
        // 准备mock数据 - 连接正常但查询失败
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);
        when(connection.prepareStatement("SELECT 1")).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false); // 查询无结果

        // 执行测试
        boolean result = databaseHealthService.checkHealth();

        // 验证结果
        assertFalse(result);
        assertFalse(databaseHealthService.isHealthy());
        assertNotNull(databaseHealthService.getLastError());
        assertTrue(databaseHealthService.getLastError().contains("简单查询测试失败"));

        // 验证mock调用
        verify(dataSource, times(2)).getConnection(); // testConnection + testSimpleQuery (在testSimpleQuery阶段失败)
        verify(connection).isValid(5);
        verify(preparedStatement).executeQuery();
    }

    /**
     * 测试数据库健康检查 - 查询结果错误
     */
    @Test
    void testCheckHealth_WrongQueryResult() throws SQLException {
        // 准备mock数据 - 连接正常但查询结果错误
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);
        when(connection.prepareStatement("SELECT 1")).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(2); // 期望返回1，实际返回2

        // 执行测试
        boolean result = databaseHealthService.checkHealth();

        // 验证结果
        assertFalse(result);
        assertFalse(databaseHealthService.isHealthy());
        assertNotNull(databaseHealthService.getLastError());
        assertTrue(databaseHealthService.getLastError().contains("简单查询测试失败"));

        // 验证mock调用
        verify(resultSet).getInt(1);
    }

    /**
     * 测试获取健康状态 - 初始状态
     */
    @Test
    void testGetStatusSummary_Initial() {
        // 验证初始状态
        assertEquals("未检查", databaseHealthService.getStatusSummary());
        assertNull(databaseHealthService.getLastCheckTime());
        assertNull(databaseHealthService.getLastError());
    }

    /**
     * 测试强制健康检查
     */
    @Test
    void testForceHealthCheck() throws SQLException {
        // 准备mock数据
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);
        when(connection.prepareStatement("SELECT 1")).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(1);
        
        // Mock表存在检查 - 为具体的SQL设置Mock
        PreparedStatement tableStmt = mock(PreparedStatement.class);
        when(connection.prepareStatement(contains("information_schema.tables"))).thenReturn(tableStmt);
        when(connection.getCatalog()).thenReturn("feduwacomm");
        
        // Mock表检查查询结果
        ResultSet tableRs = mock(ResultSet.class);
        when(tableStmt.executeQuery()).thenReturn(tableRs);
        when(tableRs.next()).thenReturn(true);
        when(tableRs.getInt(1)).thenReturn(1); // 表存在

        // 执行测试
        boolean result = databaseHealthService.forceHealthCheck();

        // 验证结果
        assertTrue(result);
        assertTrue(databaseHealthService.isHealthy());

        // 验证mock调用
        verify(dataSource, times(3)).getConnection(); // testConnection + testSimpleQuery + testTableExistence
        verify(connection, atLeastOnce()).isValid(5);
    }

    /**
     * 测试定期健康检查
     */
    @Test
    void testScheduledHealthCheck() throws SQLException {
        // 准备mock数据
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);
        when(connection.prepareStatement("SELECT 1")).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(1);
        
        // Mock表存在检查 - 为具体的SQL设置Mock
        PreparedStatement tableStmt = mock(PreparedStatement.class);
        when(connection.prepareStatement(contains("information_schema.tables"))).thenReturn(tableStmt);
        when(connection.getCatalog()).thenReturn("feduwacomm");
        
        // Mock表检查查询结果
        ResultSet tableRs = mock(ResultSet.class);
        when(tableStmt.executeQuery()).thenReturn(tableRs);
        when(tableRs.next()).thenReturn(true);
        when(tableRs.getInt(1)).thenReturn(1); // 表存在

        // 执行测试
        databaseHealthService.scheduledHealthCheck();

        // 验证结果
        assertTrue(databaseHealthService.isHealthy());
        assertNotNull(databaseHealthService.getLastCheckTime());

        // 验证mock调用
        verify(dataSource, times(3)).getConnection(); // testConnection + testSimpleQuery + testTableExistence
    }

    /**
     * 测试表不存在的情况（不应该导致健康检查失败）
     */
    @Test
    void testCheckHealth_TableNotExist() throws SQLException {
        // 准备mock数据 - 基本检查通过
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);
        
        // Mock简单查询成功
        PreparedStatement selectStmt = mock(PreparedStatement.class);
        when(connection.prepareStatement("SELECT 1")).thenReturn(selectStmt);
        when(selectStmt.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(1);
        
        // Mock表检查 - 表不存在
        PreparedStatement tableStmt = mock(PreparedStatement.class);
        when(connection.prepareStatement(contains("information_schema.tables"))).thenReturn(tableStmt);
        when(connection.getCatalog()).thenReturn("feduwacomm");
        
        ResultSet tableRs = mock(ResultSet.class);
        when(tableStmt.executeQuery()).thenReturn(tableRs);
        when(tableRs.next()).thenReturn(true);
        when(tableRs.getInt(1)).thenReturn(0); // 表不存在

        // 执行测试
        boolean result = databaseHealthService.checkHealth();

        // 验证结果 - 即使表不存在，健康检查也应该通过
        assertTrue(result);
        assertTrue(databaseHealthService.isHealthy());

        // 验证mock调用
        verify(tableStmt).setString(1, "feduwacomm");
        verify(tableStmt).setString(2, "user");
        verify(tableStmt).executeQuery();
    }

    /**
     * 测试连接资源释放 - SQLException情况
     */
    @Test
    void testCheckHealth_ResourceCleanup() throws SQLException {
        // 准备mock数据 - 连接成功但查询时抛异常
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);
        when(connection.prepareStatement("SELECT 1")).thenThrow(new SQLException("查询异常"));

        // 执行测试
        boolean result = databaseHealthService.checkHealth();

        // 验证结果
        assertFalse(result);
        assertFalse(databaseHealthService.isHealthy());
        assertTrue(databaseHealthService.getLastError().contains("查询异常"));

        // 验证连接被正确获取（资源管理由try-with-resources处理）
        verify(dataSource, times(2)).getConnection(); // testConnection + testSimpleQuery (在testSimpleQuery阶段失败)
        verify(connection).isValid(5);
    }

    /**
     * 测试多次健康检查状态变化
     */
    @Test
    void testMultipleHealthChecks() throws SQLException {
        // 第一次检查成功
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);
        when(connection.prepareStatement("SELECT 1")).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(1);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(connection.getCatalog()).thenReturn("feduwacomm");

        boolean result1 = databaseHealthService.checkHealth();
        assertTrue(result1);
        assertTrue(databaseHealthService.isHealthy());
        LocalDateTime firstCheckTime = databaseHealthService.getLastCheckTime();

        // 重置mock，模拟第二次检查失败
        reset(dataSource, connection);
        when(dataSource.getConnection()).thenThrow(new SQLException("连接失败"));

        boolean result2 = databaseHealthService.checkHealth();
        assertFalse(result2);
        assertFalse(databaseHealthService.isHealthy());
        assertTrue(databaseHealthService.getLastCheckTime().isAfter(firstCheckTime));
        assertNotNull(databaseHealthService.getLastError());
    }
}