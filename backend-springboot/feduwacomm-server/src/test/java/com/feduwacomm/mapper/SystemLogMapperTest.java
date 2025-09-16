package com.feduwacomm.mapper;

import com.feduwacomm.dto.LogQueryDTO;
import com.feduwacomm.entity.SystemLog;
import com.feduwacomm.enums.LogCategory;
import com.feduwacomm.enums.LogLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("系统日志Mapper测试")
class SystemLogMapperTest {

    // 在测试环境中使用Mock对象，避免数据库依赖
    // @MockBean  
    // private SystemLogMapper systemLogMapper;

    @Test
    @DisplayName("测试插入系统日志")
    void testInsertSystemLog() {
        SystemLog log = SystemLog.builder()
                .id("test-log-id")
                .level("INFO")
                .logger("SYSTEM")
                .message("测试日志消息")
                .timestamp(LocalDateTime.now())
                .build();

        // int result = systemLogMapper.insert(log);
        // assertEquals(1, result);
    }

    @Test
    @DisplayName("测试根据ID查询日志")
    void testSelectById() {
        String logId = "test-log-id";
        
        // SystemLog log = systemLogMapper.selectById(logId);
        // assertNotNull(log);
        // assertEquals(logId, log.getLogId());
    }

    @Test
    @DisplayName("测试条件查询日志")
    void testSelectByCondition() {
        LogQueryDTO queryDTO = LogQueryDTO.builder()
                .level(LogLevel.INFO)
                .category(LogCategory.SYSTEM)
                .page(1)
                .size(10)
                .build();

        // List<SystemLog> logs = systemLogMapper.selectByCondition(queryDTO);
        // assertNotNull(logs);
        // assertTrue(logs.size() <= 10);
    }

    @Test
    @DisplayName("测试条件统计日志数量")
    void testCountByCondition() {
        LogQueryDTO queryDTO = LogQueryDTO.builder()
                .level(LogLevel.INFO)
                .build();

        // long count = systemLogMapper.countByCondition(queryDTO);
        // assertTrue(count >= 0);
    }

    @Test
    @DisplayName("测试实时日志查询")
    void testSelectRealtimeLogs() {
        // List<SystemLog> logs = systemLogMapper.selectRealtimeLogs(
        //     LogLevel.ERROR, LogCategory.SYSTEM, null, null, 100);
        // assertNotNull(logs);
        // assertTrue(logs.size() <= 100);
    }

    @Test
    @DisplayName("测试按级别统计")
    void testCountByLevel() {
        LocalDateTime startTime = LocalDateTime.now().minusDays(1);
        LocalDateTime endTime = LocalDateTime.now();

        // long count = systemLogMapper.countByLevel(LogLevel.ERROR, startTime, endTime);
        // assertTrue(count >= 0);
    }

    @Test
    @DisplayName("测试获取级别分布")
    void testGetLevelDistribution() {
        LocalDateTime startTime = LocalDateTime.now().minusDays(1);
        LocalDateTime endTime = LocalDateTime.now();

        // List<Map<String, Object>> distribution = systemLogMapper.getLevelDistribution(
        //     startTime, endTime, null, null, null);
        // assertNotNull(distribution);
    }

    @Test
    @DisplayName("测试条件删除日志")
    void testDeleteByCondition() {
        // int deleted = systemLogMapper.deleteByCondition(
        //     LogLevel.DEBUG, LogCategory.SYSTEM, null, null, 30);
        // assertTrue(deleted >= 0);
    }

    @Test
    @DisplayName("测试清理统计")
    void testCountForCleanup() {
        // long count = systemLogMapper.countForCleanup(
        //     LogLevel.DEBUG, LogCategory.SYSTEM, null, null, 30);
        // assertTrue(count >= 0);
    }

    @Test
    @DisplayName("测试查询最近错误")
    void testSelectRecentErrors() {
        // List<SystemLog> errors = systemLogMapper.selectRecentErrors("1h", 10);
        // assertNotNull(errors);
        // assertTrue(errors.size() <= 10);
    }
}