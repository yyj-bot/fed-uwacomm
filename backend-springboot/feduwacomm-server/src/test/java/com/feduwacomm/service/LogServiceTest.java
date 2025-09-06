package com.feduwacomm.service;

import com.feduwacomm.dto.LogQueryDTO;
import com.feduwacomm.enums.LogCategory;
import com.feduwacomm.enums.LogLevel;
import com.feduwacomm.mapper.SystemLogMapper;
import com.feduwacomm.mapper.VmRuntimeLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("日志服务测试")
class LogServiceTest {

    @Mock
    private SystemLogMapper systemLogMapper;

    @Mock
    private VmRuntimeLogMapper vmRuntimeLogMapper;

    private LogService logService;

    @BeforeEach
    void setUp() {
        // 这里可以初始化 logService 实现类
        // logService = new LogServiceImpl(systemLogMapper, vmRuntimeLogMapper);
    }

    @Test
    @DisplayName("测试日志查询参数验证")
    void testLogQueryParameterValidation() {
        LogQueryDTO queryDTO = LogQueryDTO.builder()
                .level(LogLevel.INFO)
                .category(LogCategory.SYSTEM)
                .page(1)
                .size(10)
                .build();

        // 验证参数设置正确
        assertEquals(LogLevel.INFO, queryDTO.getLevel());
        assertEquals(LogCategory.SYSTEM, queryDTO.getCategory());
        assertEquals(1, queryDTO.getPage());
        assertEquals(10, queryDTO.getSize());
    }

    @Test
    @DisplayName("测试日志查询默认值")
    void testLogQueryDefaultValues() {
        LogQueryDTO queryDTO = LogQueryDTO.builder().build();

        assertEquals(1, queryDTO.getPage());
        assertEquals(10, queryDTO.getSize());
        assertEquals("createdAt", queryDTO.getSort());
        assertEquals("desc", queryDTO.getOrder());
    }

    @Test
    @DisplayName("测试日志级别枚举")
    void testLogLevelEnum() {
        // 测试从代码获取枚举
        assertEquals(LogLevel.DEBUG, LogLevel.fromCode("DEBUG"));
        assertEquals(LogLevel.INFO, LogLevel.fromCode("INFO"));
        assertEquals(LogLevel.WARN, LogLevel.fromCode("WARN"));
        assertEquals(LogLevel.ERROR, LogLevel.fromCode("ERROR"));

        // 测试无效代码
        assertThrows(IllegalArgumentException.class, () -> {
            LogLevel.fromCode("INVALID");
        });
    }

    @Test
    @DisplayName("测试日志类别枚举")
    void testLogCategoryEnum() {
        // 测试从代码获取枚举
        assertEquals(LogCategory.SYSTEM, LogCategory.fromCode("SYSTEM"));
        assertEquals(LogCategory.USER, LogCategory.fromCode("USER"));
        assertEquals(LogCategory.VM, LogCategory.fromCode("VM"));

        // 测试无效代码
        assertThrows(IllegalArgumentException.class, () -> {
            LogCategory.fromCode("INVALID");
        });
    }

    @Test
    @DisplayName("测试Mapper调用")
    void testMapperCalls() {
        LogQueryDTO queryDTO = LogQueryDTO.builder()
                .level(LogLevel.INFO)
                .page(1)
                .size(10)
                .build();

        // 模拟mapper返回结果
        when(systemLogMapper.countByCondition(any(LogQueryDTO.class))).thenReturn(100L);

        // 这里需要实际的service实现来测试
        // PageResult<LogListVO> result = logService.queryLogs(queryDTO);
        
        // 验证mapper被调用
        // verify(systemLogMapper, times(1)).selectByCondition(any(LogQueryDTO.class));
        // verify(systemLogMapper, times(1)).countByCondition(any(LogQueryDTO.class));
    }
}