package com.feduwacomm.service;

import com.feduwacomm.common.PageResult;
import com.feduwacomm.entity.User;
import com.feduwacomm.entity.UserPermission;
import com.feduwacomm.entity.VmInstance;
import com.feduwacomm.mapper.UserMapper;
import com.feduwacomm.mapper.UserPermissionMapper;
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
    private UserPermissionMapper userPermissionMapper;

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
    private UserPermission testPermission;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = User.builder()
                .id("user123")
                .username("testuser")
                .email("test@example.com")
                .build();

        testAdmin = User.builder()
                .id("admin123")
                .username("admin")
                .email("admin@example.com")
                .build();

        // 创建测试虚拟机
        testVm = VmInstance.builder()
                .id("vm123")
                .name("Test VM")
                .ipAddress("192.168.1.100")
                .port(22)
                .osType("Ubuntu")
                .status("RUNNING")
                .connectionStatus("CONNECTED")
                .build();

        // 创建测试权限
        testPermission = UserPermission.builder()
                .id("perm123")
                .userId("user123")
                .resourceType("VM")
                .resourceId("vm123")
                .permission("READ")
                .grantedAt(LocalDateTime.now())
                .grantedBy("admin123")
                .build();
    }

    @Test
    void testAssignVmToUser_Success() {
        // Given
        Set<String> permissions = Set.of("READ", "WRITE");
        when(vmInstancesMapper.selectByVmId("vm123")).thenReturn(testVm);
        when(userMapper.selectById("user123")).thenReturn(testUser);
        when(userMapper.selectById("admin123")).thenReturn(testAdmin);
        when(userPermissionMapper.selectByUserIdAndResourceType("user123", "VM"))
                .thenReturn(new ArrayList<>());
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

        verify(userPermissionMapper, times(2)).insert(any(UserPermission.class));
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
        when(userPermissionMapper.selectByUserIdAndResourceType("user123", "VM"))
                .thenReturn(List.of(testPermission));
        when(vmInstancesMapper.selectByVmId("vm123")).thenReturn(testVm);
        when(userMapper.selectById("user123")).thenReturn(testUser);
        when(userMapper.selectById("admin123")).thenReturn(testAdmin);

        // When
        VmUnassignmentResponseVO result = vmAssignmentService.unassignVmFromUser(
                "vm123", "user123", "admin123");

        // Then
        assertNotNull(result);
        assertEquals("vm123", result.getVmId());
        assertEquals("Test VM", result.getVmName());
        assertEquals("user123", result.getUserId());
        assertEquals("testuser", result.getUsername());
        assertEquals("UNASSIGNED", result.getStatus());

        verify(userPermissionMapper).deleteById("perm123");
    }

    @Test
    void testGetUserAssignedVms_Success() {
        // Given
        when(userPermissionMapper.selectByUserIdAndResourceType("user123", "VM"))
                .thenReturn(List.of(testPermission));
        when(vmInstancesMapper.selectByVmId("vm123")).thenReturn(testVm);
        when(userMapper.selectById("admin123")).thenReturn(testAdmin);

        // When
        PageResult<UserVmListVO> result = vmAssignmentService.getUserAssignedVms(
                "user123", null, 1, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(1L, result.getTotal());

        UserVmListVO vmInfo = result.getRecords().get(0);
        assertEquals("vm123", vmInfo.getVmId());
        assertEquals("Test VM", vmInfo.getName());
    }

    @Test
    void testHasVmPermission_True() {
        // Given
        when(userPermissionMapper.checkPermission("user123", "VM", "vm123", "READ"))
                .thenReturn(1);

        // When
        boolean hasPermission = vmAssignmentService.hasVmPermission(
                "vm123", "user123", "read");

        // Then
        assertTrue(hasPermission);
    }

    @Test
    void testHasVmPermission_False() {
        // Given
        when(userPermissionMapper.checkPermission("user123", "VM", "vm123", "WRITE"))
                .thenReturn(0);

        // When
        boolean hasPermission = vmAssignmentService.hasVmPermission(
                "vm123", "user123", "write");

        // Then
        assertFalse(hasPermission);
    }
}