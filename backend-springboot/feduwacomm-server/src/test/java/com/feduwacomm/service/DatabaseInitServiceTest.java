package com.feduwacomm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DatabaseInitService单元测试类
 * 测试数据库初始化服务的各种功能
 */
@ExtendWith(MockitoExtension.class)
public class DatabaseInitServiceTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @InjectMocks
    private DatabaseInitService databaseInitService;

    @BeforeEach
    void setUp() {
        // 重置mock对象
        reset(dataSource, connection, preparedStatement, resultSet);
    }

    /**
     * 测试数据库初始化 - 所有表已存在
     */
    @Test
    void testCheckAndInitDatabase_AllTablesExist() throws SQLException {
        // 准备mock数据 - 所有表都存在
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("feduwacomm");
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(1); // 表存在

        // 执行测试
        assertDoesNotThrow(() -> databaseInitService.checkAndInitDatabase());

        // 验证mock调用 - 应该检查3个表
        verify(dataSource, atLeast(3)).getConnection();
        verify(preparedStatement, atLeast(3)).setString(1, "feduwacomm");
        verify(preparedStatement, times(1)).setString(2, "users");
        verify(preparedStatement, times(1)).setString(2, "user_permissions");
        verify(preparedStatement, times(1)).setString(2, "vm_instances");
        
        // 验证没有执行创建表的操作
        verify(preparedStatement, never()).executeUpdate();
    }

    /**
     * 测试数据库初始化 - users表不存在
     */
    @Test
    void testCheckAndInitDatabase_UsersTableNotExist() throws SQLException {
        // 准备mock数据
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("feduwacomm");
        
        // Mock查询表存在检查
        PreparedStatement queryStmt = mock(PreparedStatement.class);
        when(connection.prepareStatement(contains("information_schema.tables"))).thenReturn(queryStmt);
        when(queryStmt.executeQuery()).thenReturn(resultSet);
        
        // Mock创建表语句
        PreparedStatement createStmt = mock(PreparedStatement.class);
        when(connection.prepareStatement(contains("CREATE TABLE"))).thenReturn(createStmt);
        
        // 设置表存在检查结果：users表不存在，其他表存在
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(0, 1, 1); // users不存在，其他两个表存在

        // 执行测试
        assertDoesNotThrow(() -> databaseInitService.checkAndInitDatabase());

        // 验证mock调用
        verify(queryStmt, times(3)).setString(eq(2), anyString());
        verify(createStmt, times(1)).executeUpdate(); // 只创建users表
    }

    /**
     * 测试数据库初始化 - 所有表都不存在
     */
    @Test
    void testCheckAndInitDatabase_NoTablesExist() throws SQLException {
        // 准备mock数据
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("feduwacomm");
        
        // Mock查询表存在检查
        PreparedStatement queryStmt = mock(PreparedStatement.class);
        when(connection.prepareStatement(contains("information_schema.tables"))).thenReturn(queryStmt);
        when(queryStmt.executeQuery()).thenReturn(resultSet);
        
        // Mock创建表语句
        PreparedStatement createStmt = mock(PreparedStatement.class);
        when(connection.prepareStatement(contains("CREATE TABLE"))).thenReturn(createStmt);
        
        // 所有表都不存在
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(0); // 所有表都不存在

        // 执行测试
        assertDoesNotThrow(() -> databaseInitService.checkAndInitDatabase());

        // 验证mock调用 - 应该创建3个表
        verify(createStmt, times(3)).executeUpdate();
    }

    /**
     * 测试数据库初始化失败 - 连接失败
     */
    @Test
    void testCheckAndInitDatabase_ConnectionFailed() throws SQLException {
        // 准备mock数据 - 连接失败
        when(dataSource.getConnection()).thenThrow(new SQLException("连接数据库失败"));

        // 执行测试并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> databaseInitService.checkAndInitDatabase());
        
        assertEquals("数据库初始化失败", exception.getMessage());
        assertTrue(exception.getCause() instanceof SQLException);
        assertEquals("连接数据库失败", exception.getCause().getMessage());

        // 验证mock调用
        verify(dataSource).getConnection();
    }

    /**
     * 测试数据库初始化失败 - 创建表失败
     */
    @Test
    void testCheckAndInitDatabase_CreateTableFailed() throws SQLException {
        // 准备mock数据
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("feduwacomm");
        
        // Mock查询表存在检查 - users表不存在
        PreparedStatement queryStmt = mock(PreparedStatement.class);
        when(connection.prepareStatement(contains("information_schema.tables"))).thenReturn(queryStmt);
        when(queryStmt.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(0); // users表不存在
        
        // Mock创建表语句 - 抛出异常
        PreparedStatement createStmt = mock(PreparedStatement.class);
        when(connection.prepareStatement(contains("CREATE TABLE"))).thenReturn(createStmt);
        when(createStmt.executeUpdate()).thenThrow(new SQLException("创建表失败"));

        // 执行测试并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> databaseInitService.checkAndInitDatabase());
        
        assertEquals("数据库初始化失败", exception.getMessage());
        assertTrue(exception.getCause() instanceof SQLException);
        assertEquals("创建表失败", exception.getCause().getMessage());
    }

    /**
     * 测试检查表数据 - 正常情况
     */
    @Test
    void testCheckTableData_Success() throws SQLException {
        // 准备mock数据
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("count")).thenReturn(10, 25, 5); // 分别对应3个表的记录数

        // 执行测试
        assertDoesNotThrow(() -> databaseInitService.checkTableData());

        // 验证mock调用 - 应该查询3个表的数据量
        verify(dataSource, times(3)).getConnection();
        verify(preparedStatement, times(3)).executeQuery();
        verify(resultSet, times(3)).getInt("count");
    }

    /**
     * 测试检查表数据 - 查询失败
     */
    @Test
    void testCheckTableData_QueryFailed() throws SQLException {
        // 准备mock数据 - 查询失败
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenThrow(new SQLException("查询失败"));

        // 执行测试 - 不应该抛出异常（异常被捕获并记录）
        assertDoesNotThrow(() -> databaseInitService.checkTableData());

        // 验证mock调用
        verify(dataSource).getConnection();
        verify(preparedStatement).executeQuery();
    }

    /**
     * 测试检查user_permissions表不存在的情况
     */
    @Test
    void testCheckAndInitDatabase_UserPermissionsTableNotExist() throws SQLException {
        // 准备mock数据
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("feduwacomm");
        
        // Mock查询表存在检查
        PreparedStatement queryStmt = mock(PreparedStatement.class);
        when(connection.prepareStatement(contains("information_schema.tables"))).thenReturn(queryStmt);
        when(queryStmt.executeQuery()).thenReturn(resultSet);
        
        // Mock创建表语句
        PreparedStatement createStmt = mock(PreparedStatement.class);
        when(connection.prepareStatement(contains("CREATE TABLE"))).thenReturn(createStmt);
        
        // 设置表存在检查结果：user_permissions表不存在，其他表存在
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(1, 0, 1); // users存在，user_permissions不存在，vm_instances存在

        // 执行测试
        assertDoesNotThrow(() -> databaseInitService.checkAndInitDatabase());

        // 验证mock调用
        verify(queryStmt, times(3)).setString(eq(2), anyString());
        verify(createStmt, times(1)).executeUpdate(); // 只创建user_permissions表
    }

    /**
     * 测试检查vm_instances表不存在的情况
     */
    @Test
    void testCheckAndInitDatabase_VmInstancesTableNotExist() throws SQLException {
        // 准备mock数据
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("feduwacomm");
        
        // Mock查询表存在检查
        PreparedStatement queryStmt = mock(PreparedStatement.class);
        when(connection.prepareStatement(contains("information_schema.tables"))).thenReturn(queryStmt);
        when(queryStmt.executeQuery()).thenReturn(resultSet);
        
        // Mock创建表语句
        PreparedStatement createStmt = mock(PreparedStatement.class);
        when(connection.prepareStatement(contains("CREATE TABLE"))).thenReturn(createStmt);
        
        // 设置表存在检查结果：vm_instances表不存在，其他表存在
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(1, 1, 0); // users存在，user_permissions存在，vm_instances不存在

        // 执行测试
        assertDoesNotThrow(() -> databaseInitService.checkAndInitDatabase());

        // 验证mock调用
        verify(queryStmt, times(3)).setString(eq(2), anyString());
        verify(createStmt, times(1)).executeUpdate(); // 只创建vm_instances表
    }

    /**
     * 测试检查表数据的各个方法 - 空结果集
     */
    @Test
    void testCheckTableData_EmptyResultSet() throws SQLException {
        // 准备mock数据 - 空结果集
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false); // 没有结果

        // 执行测试
        assertDoesNotThrow(() -> databaseInitService.checkTableData());

        // 验证mock调用
        verify(dataSource, times(3)).getConnection();
        verify(preparedStatement, times(3)).executeQuery();
        // 由于没有结果，不会调用getInt方法
        verify(resultSet, never()).getInt("count");
    }

    /**
     * 测试表存在检查的边界情况
     */
    @Test
    void testTableExists_BoundaryConditions() throws SQLException {
        // 准备mock数据
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("feduwacomm");
        
        // Mock查询表存在检查
        PreparedStatement queryStmt = mock(PreparedStatement.class);
        when(connection.prepareStatement(contains("information_schema.tables"))).thenReturn(queryStmt);
        when(queryStmt.executeQuery()).thenReturn(resultSet);
        
        // Mock创建表语句（不会被调用）
        PreparedStatement createStmt = mock(PreparedStatement.class);
        when(connection.prepareStatement(contains("CREATE TABLE"))).thenReturn(createStmt);
        
        // 设置返回值大于1的情况（理论上应该不会发生，但测试边界情况）
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(2, 1, 0); // 第一个表返回2（边界情况），第二个表存在，第三个表不存在

        // 执行测试
        assertDoesNotThrow(() -> databaseInitService.checkAndInitDatabase());

        // 验证mock调用 - 前两个表被认为存在，不会创建
        verify(createStmt, times(1)).executeUpdate(); // 只创建第三个表
    }
}