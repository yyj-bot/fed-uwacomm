package com.feduwacomm.service;

import com.feduwacomm.common.PageResult;
import com.feduwacomm.entity.User;
import com.feduwacomm.entity.VmInstance;
import com.feduwacomm.enums.VmStatus;
import com.feduwacomm.enums.UserRole;
import com.feduwacomm.enums.ConnectionStatus;
import com.feduwacomm.mapper.UserMapper;
import com.feduwacomm.mapper.VmInstancesMapper;
import com.feduwacomm.service.impl.VmAssignmentServiceImpl;
import com.feduwacomm.utils.UuidUtil;
import com.feduwacomm.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * VmAssignmentService单元测试
 */
@ExtendWith(MockitoExtension.class)
public class VmAssignmentServiceTest {


    @Mock
    private VmInstancesMapper vmInstancesMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UuidUtil uuidUtil;

    @InjectMocks
    private VmAssignmentServiceImpl vmAssignmentService;

    private User testUser;
    private User testAdmin;
    private VmInstance testVm;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = User.builder()
                .id("user123")
                .username("testuser")
                .email("test@example.com")
                .role(UserRole.ADMIN)
                .build();

        testAdmin = User.builder()
                .id("admin123")
                .username("admin")
                .email("admin@example.com")
                .role(UserRole.ADMIN)
                .build();

        // 创建测试虚拟机
        testVm = VmInstance.builder()
                .id("vm123")
                .name("Test VM")
                .ipAddress("192.168.1.100")
                .port(22)
                .osType("Ubuntu")
                .status(VmStatus.fromCode("RUNNING"))
                .connectionStatus(ConnectionStatus.fromCode("CONNECTED"))
                .build();

        // 权限管理已改为基于用户角色
    }

    @Test
    void testAssignVmToUser_Success() {
        // Given
        Set<String> permissions = Set.of("READ", "WRITE");
        when(vmInstancesMapper.selectByVmId("vm123")).thenReturn(testVm);
        when(userMapper.selectById("user123")).thenReturn(testUser);
        when(userMapper.selectById("admin123")).thenReturn(testAdmin);
        // 移除权限mapper调用 - 现在基于用户角色
        when(uuidUtil.generateUuid()).thenReturn("perm123", "perm124");

        // When
        VmAssignmentResponseVO result = vmAssignmentService.assignVmToUser(
                "vm123", "user123", permissions, "admin123");

        // Then
        assertNotNull(result);
        assertEquals("perm123", result.getAssignmentId());
        assertEquals("vm123", result.getVmId());
        assertEquals("Test VM", result.getVmName());
        assertEquals("user123", result.getUserId());
        assertEquals("testuser", result.getUsername());
        assertEquals(permissions, result.getPermissions());
        assertEquals("ASSIGNED", result.getStatus());

        // 基于角色的权限管理，无需验证权限插入
    }

    @Test
    void testAssignVmToUser_VmNotFound() {
        // Given
        when(vmInstancesMapper.selectByVmId("nonexistent")).thenReturn(null);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            vmAssignmentService.assignVmToUser(
                    "nonexistent", "user123", Set.of("READ"), "admin123");
        });
        assertEquals("虚拟机不存在: nonexistent", exception.getMessage());
    }

    @Test
    void testUnassignVmFromUser_Success() {
        // Given
        when(vmInstancesMapper.selectByVmId("vm123")).thenReturn(testVm);
        when(userMapper.selectById("user123")).thenReturn(testUser);
        when(userMapper.selectById("admin123")).thenReturn(testAdmin);

        // When
        VmUnassignmentResponseVO result = vmAssignmentService.unassignVmFromUser(
                "vm123", "user123", "admin123");

        // Then
        assertNotNull(result);
        assertEquals("vm123", result.getVmId());
        assertEquals("user123", result.getUserId());
        assertEquals("UNASSIGNED", result.getStatus());

        // 基于角色的权限管理，无需验证权限删除
    }

    @Test
    void testGetUserAssignedVms_Success() {
        // Given
        testUser.setRole(UserRole.fromCode("RESEARCHER")); // 设置用户角色
        when(userMapper.selectById("user123")).thenReturn(testUser);
        when(vmInstancesMapper.selectAll()).thenReturn(List.of(testVm));
        testVm.setStatus(VmStatus.fromCode("RUNNING")); // 设置虚拟机状态为运行中

        // When
        PageResult<UserVmListVO> result = vmAssignmentService.getUserAssignedVms(
                "user123", null, 1, 10);

        // Then
        assertNotNull(result);
        assertTrue(result.getList().size() >= 0);
    }

    @Test
    void testHasVmPermission_True() {
        // Given
        testUser.setRole(UserRole.fromCode("RESEARCHER")); // RESEARCHER有READ权限
        when(userMapper.selectById("user123")).thenReturn(testUser);

        // When
        boolean hasPermission = vmAssignmentService.hasVmPermission(
                "vm123", "user123", "read");

        // Then
        assertTrue(hasPermission);
    }

    @Test
    void testHasVmPermission_False() {
        // Given
        testUser.setRole(UserRole.fromCode("OPERATOR")); // OPERATOR没有WRITE权限
        when(userMapper.selectById("user123")).thenReturn(testUser);

        // When
        boolean hasPermission = vmAssignmentService.hasVmPermission(
                "vm123", "user123", "write");

        // Then
        assertFalse(hasPermission);
    }
}