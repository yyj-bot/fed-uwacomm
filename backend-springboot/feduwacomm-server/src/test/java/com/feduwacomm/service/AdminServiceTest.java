package com.feduwacomm.service;

import com.feduwacomm.common.BaseContext;
import com.feduwacomm.dto.*;
import com.feduwacomm.entity.User;
import com.feduwacomm.entity.UserPermission;
import com.feduwacomm.exception.UserException;
import com.feduwacomm.mapper.AdminMapper;
import com.feduwacomm.mapper.UserMapper;
import com.feduwacomm.mapper.UserPermissionMapper;
import com.feduwacomm.service.impl.AdminServiceImpl;
import com.feduwacomm.utils.PasswordUtil;
import com.feduwacomm.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AdminService单元测试类
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private AdminMapper adminMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserPermissionMapper userPermissionMapper;

    @InjectMocks
    private AdminServiceImpl adminService;

    private User testUser;
    private UserCreateDTO createDTO;
    private UserAdminUpdateDTO updateDTO;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        testUser = User.builder()
                .id("a1b2c3d4e5f678901234567890123456")
                .username("testuser")
                .email("test@example.com")
                .passwordHash(PasswordUtil.encode("password123"))
                .role("VIEWER")
                .status("ACTIVE")
                .loginAttempts(0)
                .lastLoginTime(LocalDateTime.now())
                .lastLoginIp("192.168.1.100")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdBy("admin_1234567890")
                .updatedBy("admin_1234567890")
                .build();

        createDTO = UserCreateDTO.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("password123")
                .role("RESEARCHER")
                .build();

        updateDTO = UserAdminUpdateDTO.builder()
                .username("updateduser")
                .email("updated@example.com")
                .role("RESEARCHER")
                .status("ACTIVE")
                .build();
    }

    // 用户管理方法测试

    @Test
    void testGetUserList_Success() {
        // 准备测试数据
        UserQueryDTO queryDTO = UserQueryDTO.builder()
                .page(1)
                .size(10)
                .role("VIEWER")
                .status("ACTIVE")
                .keyword("test")
                .build();

        List<User> users = Arrays.asList(testUser);
        when(adminMapper.selectByCondition(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(users);
        when(adminMapper.countByCondition(anyString(), anyString(), anyString())).thenReturn(1);

        // 执行测试
        PageResponseDTO<UserListVO> result = adminService.getUserList(queryDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getPage());
        assertEquals(10, result.getSize());
        assertEquals(1, result.getList().size());
        assertEquals("testuser", result.getList().get(0).getUsername());
    }

    @Test
    void testGetUserList_WithDefaultValues() {
        // 准备测试数据
        UserQueryDTO queryDTO = UserQueryDTO.builder().build();

        List<User> users = Arrays.asList(testUser);
        when(adminMapper.selectByCondition(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(users);
        when(adminMapper.countByCondition(anyString(), anyString(), anyString())).thenReturn(1);

        // 执行测试
        PageResponseDTO<UserListVO> result = adminService.getUserList(queryDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getPage());
        assertEquals(10, result.getSize());
    }

    @Test
    void testGetUserDetail_Success() {
        // 准备测试数据
        String userId = "a1b2c3d4e5f678901234567890123456";
        when(adminMapper.selectById(userId)).thenReturn(testUser);

        // 执行测试
        UserDetailVO result = adminService.getUserDetail(userId);

        // 验证结果
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("VIEWER", result.getRole());
        assertEquals("ACTIVE", result.getStatus());
    }

    @Test
    void testGetUserDetail_UserNotFound() {
        // 准备测试数据
        String userId = "nonexistent_user";
        when(adminMapper.selectById(userId)).thenReturn(null);

        // 执行测试并验证异常
        UserException exception = assertThrows(UserException.class, () -> {
            adminService.getUserDetail(userId);
        });

        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    void testCreateUser_Success() {
        // 准备测试数据
        when(adminMapper.selectByEmail("newuser@example.com")).thenReturn(null);
        when(adminMapper.selectByUsername("newuser")).thenReturn(null);
        when(adminMapper.insert(any(User.class))).thenReturn(1);

        try (MockedStatic<BaseContext> baseContextMock = mockStatic(BaseContext.class)) {
            baseContextMock.when(BaseContext::getCurrentUserId).thenReturn("admin_1234567890");

            // 执行测试
            UserCreateResponseVO result = adminService.createUser(createDTO);

            // 验证结果
            assertNotNull(result);
            assertEquals("newuser", result.getUsername());
            assertEquals("newuser@example.com", result.getEmail());
            assertEquals("RESEARCHER", result.getRole());
            assertEquals("ACTIVE", result.getStatus());
        }

        verify(adminMapper, times(1)).insert(any(User.class));
    }

    @Test
    void testCreateUser_EmailExists() {
        // 准备测试数据
        when(adminMapper.selectByEmail("newuser@example.com")).thenReturn(testUser);

        // 执行测试并验证异常
        UserException exception = assertThrows(UserException.class, () -> {
            adminService.createUser(createDTO);
        });

        assertEquals("邮箱已存在", exception.getMessage());
        verify(adminMapper, never()).insert(any(User.class));
    }

    @Test
    void testCreateUser_UsernameExists() {
        // 准备测试数据
        when(adminMapper.selectByEmail("newuser@example.com")).thenReturn(null);
        when(adminMapper.selectByUsername("newuser")).thenReturn(testUser);

        // 执行测试并验证异常
        UserException exception = assertThrows(UserException.class, () -> {
            adminService.createUser(createDTO);
        });

        assertEquals("用户名已存在", exception.getMessage());
        verify(adminMapper, never()).insert(any(User.class));
    }

    @Test
    void testUpdateUser_Success() {
        // 准备测试数据
        String userId = "a1b2c3d4e5f678901234567890123456";
        when(adminMapper.selectById(userId)).thenReturn(testUser);
        when(adminMapper.selectByUsername("updateduser")).thenReturn(null);
        when(adminMapper.selectByEmail("updated@example.com")).thenReturn(null);
        when(adminMapper.update(any(User.class))).thenReturn(1);

        // 执行测试
        UserUpdateResponseVO result = adminService.updateUser(userId, updateDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals("updateduser", result.getUsername());
        assertEquals("updated@example.com", result.getEmail());
        assertEquals("RESEARCHER", result.getRole());
        assertEquals("ACTIVE", result.getStatus());

        verify(adminMapper, times(1)).update(any(User.class));
    }

    @Test
    void testUpdateUser_UserNotFound() {
        // 准备测试数据
        String userId = "nonexistent_user";
        when(adminMapper.selectById(userId)).thenReturn(null);

        // 执行测试并验证异常
        UserException exception = assertThrows(UserException.class, () -> {
            adminService.updateUser(userId, updateDTO);
        });

        assertEquals("用户不存在", exception.getMessage());
        verify(adminMapper, never()).update(any(User.class));
    }

    @Test
    void testUpdateUser_UsernameExists() {
        // 准备测试数据
        String userId = "a1b2c3d4e5f678901234567890123456";
        User existingUser = User.builder().id("other_user").username("updateduser").build();

        when(adminMapper.selectById(userId)).thenReturn(testUser);
        when(adminMapper.selectByUsername("updateduser")).thenReturn(existingUser);

        // 执行测试并验证异常
        UserException exception = assertThrows(UserException.class, () -> {
            adminService.updateUser(userId, updateDTO);
        });

        assertEquals("用户名已存在", exception.getMessage());
        verify(adminMapper, never()).update(any(User.class));
    }

    @Test
    void testDeleteUser_Success() {
        // 准备测试数据
        String userId = "a1b2c3d4e5f678901234567890123456";
        when(adminMapper.selectById(userId)).thenReturn(testUser);
        when(adminMapper.deleteById(userId)).thenReturn(1);

        // 执行测试
        assertDoesNotThrow(() -> {
            adminService.deleteUser(userId);
        });

        verify(adminMapper, times(1)).deleteById(userId);
    }

    @Test
    void testDeleteUser_UserNotFound() {
        // 准备测试数据
        String userId = "nonexistent_user";
        when(adminMapper.selectById(userId)).thenReturn(null);

        // 执行测试并验证异常
        UserException exception = assertThrows(UserException.class, () -> {
            adminService.deleteUser(userId);
        });

        assertEquals("用户不存在", exception.getMessage());
        verify(adminMapper, never()).deleteById(userId);
    }

    // 用户状态管理方法测试

    @Test
    void testLockUser_Success() {
        // 准备测试数据
        String userId = "a1b2c3d4e5f678901234567890123456";
        UserLockDTO lockDTO = UserLockDTO.builder()
                .duration(3600)
                .build();

        when(adminMapper.selectById(userId)).thenReturn(testUser);
        when(adminMapper.lockUser(eq(userId), anyString())).thenReturn(1);

        // 执行测试
        UserLockResponseVO result = adminService.lockUser(userId, lockDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertNotNull(result.getLockedUntil());

        verify(adminMapper, times(1)).lockUser(eq(userId), anyString());
    }

    @Test
    void testLockUser_UserNotFound() {
        // 准备测试数据
        String userId = "nonexistent_user";
        UserLockDTO lockDTO = UserLockDTO.builder()
                .duration(3600)
                .build();

        when(adminMapper.selectById(userId)).thenReturn(null);

        // 执行测试并验证异常
        UserException exception = assertThrows(UserException.class, () -> {
            adminService.lockUser(userId, lockDTO);
        });

        assertEquals("用户不存在", exception.getMessage());
        verify(adminMapper, never()).lockUser(anyString(), anyString());
    }

    @Test
    void testUnlockUser_Success() {
        // 准备测试数据
        String userId = "a1b2c3d4e5f678901234567890123456";
        User lockedUser = User.builder()
                .id("a1b2c3d4e5f678901234567890123456")
                .username("testuser")
                .email("test@example.com")
                .passwordHash(PasswordUtil.encode("password123"))
                .role("VIEWER")
                .status("LOCKED")
                .loginAttempts(0)
                .lastLoginTime(LocalDateTime.now())
                .lastLoginIp("192.168.1.100")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .lockedUntil(LocalDateTime.now().plusHours(1))
                .build();

        when(adminMapper.selectById(userId)).thenReturn(lockedUser);
        when(adminMapper.update(any(User.class))).thenReturn(1);

        // 执行测试
        UserUnlockResponseVO result = adminService.unlockUser(userId);

        // 验证结果
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals("ACTIVE", result.getStatus());

        verify(adminMapper, times(1)).update(any(User.class));
    }

    @Test
    void testUnlockUser_UserNotFound() {
        // 准备测试数据
        String userId = "nonexistent_user";
        when(adminMapper.selectById(userId)).thenReturn(null);

        // 执行测试并验证异常
        UserException exception = assertThrows(UserException.class, () -> {
            adminService.unlockUser(userId);
        });

        assertEquals("用户不存在", exception.getMessage());
        verify(adminMapper, never()).update(any(User.class));
    }

    @Test
    void testResetPassword_Success() {
        // 准备测试数据
        String userId = "a1b2c3d4e5f678901234567890123456";
        PasswordResetDTO resetDTO = PasswordResetDTO.builder()
                .newPassword("newpassword123")
                .build();

        when(adminMapper.selectById(userId)).thenReturn(testUser);
        when(adminMapper.updatePassword(userId, anyString())).thenReturn(1);

        // 执行测试
        assertDoesNotThrow(() -> {
            adminService.resetPassword(userId, resetDTO);
        });

        verify(adminMapper, times(1)).updatePassword(userId, anyString());
    }

    @Test
    void testResetPassword_UserNotFound() {
        // 准备测试数据
        String userId = "nonexistent_user";
        PasswordResetDTO resetDTO = PasswordResetDTO.builder()
                .newPassword("newpassword123")
                .build();

        when(adminMapper.selectById(userId)).thenReturn(null);

        // 执行测试并验证异常
        UserException exception = assertThrows(UserException.class, () -> {
            adminService.resetPassword(userId, resetDTO);
        });

        assertEquals("用户不存在", exception.getMessage());
        verify(adminMapper, never()).updatePassword(anyString(), anyString());
    }

    // 权限管理方法测试

    @Test
    void testGetUserPermissions_Success() {
        // 准备测试数据
        String userId = "a1b2c3d4e5f678901234567890123456";
        List<UserPermission> permissions = Arrays.asList(
                UserPermission.builder()
                        .id("perm_1234567890")
                        .userId(userId)
                        .resourceType("VM")
                        .resourceId("vm_1234567890")
                        .permission("READ")
                        .grantedAt(LocalDateTime.now())
                        .grantedBy("admin_1234567890")
                        .build());

        when(adminMapper.selectById(userId)).thenReturn(testUser);
        when(userPermissionMapper.selectByUserId(userId)).thenReturn(permissions);

        // 执行测试
        List<UserPermissionVO> result = adminService.getUserPermissions(userId);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("perm_1234567890", result.get(0).getPermissionId());
        assertEquals("VM", result.get(0).getResourceType());
        assertEquals("READ", result.get(0).getPermission());
    }

    @Test
    void testGetUserPermissions_UserNotFound() {
        // 准备测试数据
        String userId = "nonexistent_user";
        when(adminMapper.selectById(userId)).thenReturn(null);

        // 执行测试并验证异常
        UserException exception = assertThrows(UserException.class, () -> {
            adminService.getUserPermissions(userId);
        });

        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    void testGrantPermission_Success() {
        // 准备测试数据
        String userId = "a1b2c3d4e5f678901234567890123456";
        PermissionGrantDTO grantDTO = PermissionGrantDTO.builder()
                .resourceType("VM")
                .resourceId("vm_1234567890")
                .permission("READ")
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();

        when(adminMapper.selectById(userId)).thenReturn(testUser);
        when(userPermissionMapper.insert(any(UserPermission.class))).thenReturn(1);

        try (MockedStatic<BaseContext> baseContextMock = mockStatic(BaseContext.class)) {
            baseContextMock.when(BaseContext::getCurrentUserId).thenReturn("admin_1234567890");

            // 执行测试
            PermissionGrantResponseVO result = adminService.grantPermission(userId, grantDTO);

            // 验证结果
            assertNotNull(result);
            assertNotNull(result.getPermissionId());
            assertNotNull(result.getGrantedAt());
        }

        verify(userPermissionMapper, times(1)).insert(any(UserPermission.class));
    }

    @Test
    void testGrantPermission_UserNotFound() {
        // 准备测试数据
        String userId = "nonexistent_user";
        PermissionGrantDTO grantDTO = PermissionGrantDTO.builder()
                .resourceType("VM")
                .resourceId("vm_1234567890")
                .permission("READ")
                .build();

        when(adminMapper.selectById(userId)).thenReturn(null);

        // 执行测试并验证异常
        UserException exception = assertThrows(UserException.class, () -> {
            adminService.grantPermission(userId, grantDTO);
        });

        assertEquals("用户不存在", exception.getMessage());
        verify(userPermissionMapper, never()).insert(any(UserPermission.class));
    }

    @Test
    void testRevokePermission_Success() {
        // 准备测试数据
        String userId = "a1b2c3d4e5f678901234567890123456";
        String permissionId = "perm_1234567890";

        UserPermission permission = UserPermission.builder()
                .id(permissionId)
                .userId(userId)
                .build();

        when(adminMapper.selectById(userId)).thenReturn(testUser);
        when(userPermissionMapper.selectById(permissionId)).thenReturn(permission);
        when(userPermissionMapper.deleteById(permissionId)).thenReturn(1);

        // 执行测试
        assertDoesNotThrow(() -> {
            adminService.revokePermission(userId, permissionId);
        });

        verify(userPermissionMapper, times(1)).deleteById(permissionId);
    }

    @Test
    void testRevokePermission_UserNotFound() {
        // 准备测试数据
        String userId = "nonexistent_user";
        String permissionId = "perm_1234567890";

        when(adminMapper.selectById(userId)).thenReturn(null);

        // 执行测试并验证异常
        UserException exception = assertThrows(UserException.class, () -> {
            adminService.revokePermission(userId, permissionId);
        });

        assertEquals("用户不存在", exception.getMessage());
        verify(userPermissionMapper, never()).deleteById(anyString());
    }

    @Test
    void testRevokePermission_PermissionNotFound() {
        // 准备测试数据
        String userId = "a1b2c3d4e5f678901234567890123456";
        String permissionId = "nonexistent_permission";

        when(adminMapper.selectById(userId)).thenReturn(testUser);
        when(userPermissionMapper.selectById(permissionId)).thenReturn(null);

        // 执行测试并验证异常
        UserException exception = assertThrows(UserException.class, () -> {
            adminService.revokePermission(userId, permissionId);
        });

        assertEquals("权限不存在", exception.getMessage());
        verify(userPermissionMapper, never()).deleteById(anyString());
    }

    @Test
    void testRevokePermission_PermissionNotBelongToUser() {
        // 准备测试数据
        String userId = "a1b2c3d4e5f678901234567890123456";
        String permissionId = "perm_1234567890";

        UserPermission permission = UserPermission.builder()
                .id(permissionId)
                .userId("other_user")
                .build();

        when(adminMapper.selectById(userId)).thenReturn(testUser);
        when(userPermissionMapper.selectById(permissionId)).thenReturn(permission);

        // 执行测试并验证异常
        UserException exception = assertThrows(UserException.class, () -> {
            adminService.revokePermission(userId, permissionId);
        });

        assertEquals("权限不属于该用户", exception.getMessage());
        verify(userPermissionMapper, never()).deleteById(anyString());
    }
}