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
import com.feduwacomm.testdata.TestDataBuilder;
import com.feduwacomm.testdata.TestHelper;
import com.feduwacomm.utils.PasswordUtil;
import com.feduwacomm.vo.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static com.feduwacomm.testdata.TestHelper.Matchers.*;
import static com.feduwacomm.testdata.TestHelper.Constants.*;

/**
 * AdminService单元测试类
 * 
 * 测试目的：验证管理员功能的所有业务逻辑
 * 主要测试场景：
 * 1. 用户管理（创建、查询、更新、删除）
 * 2. 用户状态管理（锁定、解锁、密码重置）
 * 3. 权限管理（授权、撤销、查询）
 * 4. 边界条件和异常处理
 * 5. 数据一致性和时序验证
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class AdminServiceTest {

    @Mock
    private AdminMapper adminMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserPermissionMapper userPermissionMapper;


    @InjectMocks
    private AdminServiceImpl adminService;

    private TestHelper.MockDatabase mockDatabase;
    private User testUser;
    private UserCreateDTO createDTO;
    private UserAdminUpdateDTO updateDTO;

    @BeforeEach
    void setUp() {
        // 初始化Mock数据库，提供状态一致性
        mockDatabase = new TestHelper.MockDatabase();
        
        // 使用TestDataBuilder创建标准测试数据
        testUser = TestDataBuilder.Users.validUser().build();
        createDTO = TestDataBuilder.DTOs.validCreateDTO().build();
        updateDTO = TestDataBuilder.DTOs.validAdminUpdateDTO().build();
        
        // 配置基础Mock行为
        configureMockMapperBehavior();
    }
    
    /**
     * 配置AdminMapper和相关Mapper的Mock行为
     */
    private void configureMockMapperBehavior() {
        // 用户查询相关Mock
        when(adminMapper.selectById(anyString())).thenAnswer(invocation -> 
            mockDatabase.selectUserById(invocation.getArgument(0)));
        
        when(adminMapper.selectByUsername(anyString())).thenAnswer(invocation -> 
            mockDatabase.selectUserByUsername(invocation.getArgument(0)));
        
        when(adminMapper.selectByEmail(anyString())).thenAnswer(invocation -> 
            mockDatabase.selectUserByEmail(invocation.getArgument(0)));
        
        // 用户操作相关Mock
        when(adminMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            mockDatabase.insertUser(user);
            return 1;
        });
        
        when(adminMapper.update(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return mockDatabase.updateUser(user);
        });
        
        when(adminMapper.deleteById(anyString())).thenAnswer(invocation -> {
            String userId = invocation.getArgument(0);
            return mockDatabase.deleteUser(userId);
        });
        
        when(adminMapper.updatePassword(anyString(), anyString())).thenAnswer(invocation -> {
            String userId = invocation.getArgument(0);
            String newPasswordHash = invocation.getArgument(1);
            User user = mockDatabase.selectUserById(userId);
            if (user != null) {
                User updatedUser = User.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .passwordHash(newPasswordHash)
                        .role(user.getRole())
                        .status(user.getStatus())
                        .loginAttempts(user.getLoginAttempts())
                        .lastLoginTime(user.getLastLoginTime())
                        .lastLoginIp(user.getLastLoginIp())
                        .createdAt(user.getCreatedAt())
                        .updatedAt(LocalDateTime.now())
                        .createdBy(user.getCreatedBy())
                        .updatedBy(user.getUpdatedBy())
                        .lockedUntil(user.getLockedUntil())
                        .build();
                return mockDatabase.updateUser(updatedUser);
            }
            return 0;
        });
        
        // 分页查询Mock
        when(adminMapper.selectByCondition(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenAnswer(invocation -> Arrays.asList(testUser));
        
        when(adminMapper.countByCondition(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> mockDatabase.countUsers());
        
        // 权限相关Mock
        when(userPermissionMapper.selectByUserId(anyString())).thenReturn(Arrays.asList());
        when(userPermissionMapper.selectById(anyString())).thenReturn(null);
        when(userPermissionMapper.insert(any(UserPermission.class))).thenReturn(1);
        when(userPermissionMapper.deleteById(anyString())).thenReturn(1);
    }

    // 用户管理方法测试

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testGetUserList_WithValidQuery_ShouldReturnPagedResults() {
        // 准备测试数据 - 预先在Mock数据库中插入多个用户
        User viewer1 = TestDataBuilder.Users.validUser()
                .username("viewer1")
                .email("viewer1@example.com")
                .role(ROLE_VIEWER)
                .status(STATUS_ACTIVE)
                .build();
        User viewer2 = TestDataBuilder.Users.validUser()
                .username("viewer2")
                .email("viewer2@example.com")
                .role(ROLE_VIEWER)
                .status(STATUS_ACTIVE)
                .build();
        User researcher = TestDataBuilder.Users.researcherUser().build();
        
        mockDatabase.insertUser(viewer1);
        mockDatabase.insertUser(viewer2);
        mockDatabase.insertUser(researcher);
        
        // 创建查询条件
        UserQueryDTO queryDTO = TestDataBuilder.DTOs.validQueryDTO()
                .role(ROLE_VIEWER)
                .status(STATUS_ACTIVE)
                .keyword("viewer")
                .build();

        // 配置特定的分页查询Mock
        when(adminMapper.selectByCondition(eq(ROLE_VIEWER), eq(STATUS_ACTIVE), eq("viewer"), eq(0), eq(10)))
                .thenReturn(Arrays.asList(viewer1, viewer2));
        when(adminMapper.countByCondition(eq(ROLE_VIEWER), eq(STATUS_ACTIVE), eq("viewer")))
                .thenReturn(2);

        // 执行测试
        PageResponseDTO<UserListVO> result = adminService.getUserList(queryDTO);

        // 验证结果 - 使用更详细的断言
        assertNotNull(result, "Result should not be null");
        assertEquals(2L, result.getTotal(), "Total count should match filtered users");
        assertEquals(1, result.getPage(), "Page number should be 1");
        assertEquals(10, result.getSize(), "Page size should be 10");
        assertEquals(1, result.getPages(), "Total pages should be 1");
        assertNotNull(result.getList(), "User list should not be null");
        assertEquals(2, result.getList().size(), "Should return 2 filtered users");
        
        // 验证返回的用户数据
        UserListVO firstUser = result.getList().get(0);
        assertEquals("viewer1", firstUser.getUsername());
        assertEquals(ROLE_VIEWER, firstUser.getRole());
        assertEquals(STATUS_ACTIVE, firstUser.getStatus());
        
        // 验证调用时序
        InOrder inOrder = inOrder(adminMapper);
        inOrder.verify(adminMapper).selectByCondition(ROLE_VIEWER, STATUS_ACTIVE, "viewer", 0, 10);
        inOrder.verify(adminMapper).countByCondition(ROLE_VIEWER, STATUS_ACTIVE, "viewer");
    }

    @Test
    void testGetUserList_WithDefaultValues() {
        // 准备测试数据
        UserQueryDTO queryDTO = UserQueryDTO.builder().build();

        List<User> users = Arrays.asList(testUser);
        when(adminMapper.selectByCondition(isNull(), isNull(), isNull(), eq(0), eq(10)))
                .thenReturn(users);
        when(adminMapper.countByCondition(isNull(), isNull(), isNull())).thenReturn(1);

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
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testCreateUser_WithValidData_ShouldCreateAndReturnUser() {
        // 准备测试数据 - 确保邮箱和用户名不冲突
        UserCreateDTO validCreateDTO = TestDataBuilder.DTOs.validCreateDTO()
                .username("uniqueuser")
                .email("unique@example.com")
                .password(VALID_PASSWORD)
                .role(ROLE_RESEARCHER)
                .build();

        try (MockedStatic<BaseContext> baseContextMock = mockStatic(BaseContext.class)) {
            String adminId = "admin_" + System.currentTimeMillis();
            baseContextMock.when(BaseContext::getCurrentUserId).thenReturn(adminId);

            // 执行测试
            UserCreateResponseVO result = adminService.createUser(validCreateDTO);

            // 验证结果
            assertNotNull(result, "Create result should not be null");
            assertEquals("uniqueuser", result.getUsername());
            assertEquals("unique@example.com", result.getEmail());
            assertEquals(ROLE_RESEARCHER, result.getRole());
            assertEquals(STATUS_ACTIVE, result.getStatus());
            assertNotNull(result.getCreatedAt(), "Created timestamp should be set");
            TestHelper.Assertions.assertReasonableTimestamp(result.getCreatedAt());
        }

        // 验证状态一致性 - 用户应该被保存到Mock数据库
        User savedUser = mockDatabase.selectUserByUsername("uniqueuser");
        assertNotNull(savedUser, "User should be saved to database");
        TestHelper.Assertions.assertUserValid(savedUser);
        assertEquals(ROLE_RESEARCHER, savedUser.getRole());
        assertEquals(STATUS_ACTIVE, savedUser.getStatus());
        TestHelper.Assertions.assertPasswordEncrypted(VALID_PASSWORD, savedUser.getPasswordHash());
        
        // 验证时序 - 应该先检查重复，再插入
        InOrder inOrder = inOrder(adminMapper);
        inOrder.verify(adminMapper).selectByEmail("unique@example.com");
        inOrder.verify(adminMapper).selectByUsername("uniqueuser");
        inOrder.verify(adminMapper).insert(userMatching(user -> 
            "uniqueuser".equals(user.getUsername()) &&
            ROLE_RESEARCHER.equals(user.getRole()) &&
            STATUS_ACTIVE.equals(user.getStatus()) &&
            user.getPasswordHash() != null &&
            user.getCreatedBy() != null
        ));
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
        lenient().when(adminMapper.selectByEmail("newuser@example.com")).thenReturn(null);
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
        when(adminMapper.update(any(User.class))).thenReturn(1);

        // 执行测试
        UserLockResponseVO result = adminService.lockUser(userId, lockDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertNotNull(result.getLockedUntil());

        verify(adminMapper, times(1)).update(any(User.class));
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
        verify(adminMapper, never()).update(any(User.class));
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
        when(adminMapper.updatePassword(eq(userId), anyString())).thenReturn(1);

        // 执行测试
        assertDoesNotThrow(() -> {
            adminService.resetPassword(userId, resetDTO);
        });

        verify(adminMapper, times(1)).updatePassword(eq(userId), anyString());
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
        assertEquals("perm_1234567890", result.get(0).getId());
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

        assertEquals("权限不足", exception.getMessage());
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

        assertEquals("权限不足", exception.getMessage());
        verify(userPermissionMapper, never()).deleteById(anyString());
    }

    // ======================== 边界条件和异常路径测试 ========================

    @Test
    void testCreateUser_WithNullDTO_ShouldThrowException() {
        // 执行测试并验证异常
        assertThrows(Exception.class, () -> adminService.createUser(null),
                "Should throw exception when DTO is null");
    }

    @Test 
    void testGetUserDetail_WithNullUserId_ShouldThrowException() {
        // 执行测试并验证异常
        assertThrows(Exception.class, () -> adminService.getUserDetail(null),
                "Should throw exception when userId is null");
    }

    @Test
    void testUpdateUser_WithEmptyFields_ShouldHandleGracefully() {
        // 准备测试数据 - 空字符串字段
        User existingUser = TestDataBuilder.Users.validUser().build();
        mockDatabase.insertUser(existingUser);
        
        UserAdminUpdateDTO emptyDTO = TestDataBuilder.DTOs.validAdminUpdateDTO()
                .username("")
                .email("")
                .build();

        // 执行测试并验证行为
        assertThrows(Exception.class, 
            () -> adminService.updateUser(existingUser.getId(), emptyDTO),
            "Should handle empty string fields appropriately");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testConcurrentUserCreation_ShouldMaintainDataIntegrity() throws InterruptedException {
        // 并发创建用户测试
        String commonEmail = "concurrent.admin@example.com";
        int threadCount = 5;
        Thread[] threads = new Thread[threadCount];
        UserCreateDTO[] dtos = new UserCreateDTO[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            dtos[i] = TestDataBuilder.DTOs.validCreateDTO()
                    .username("concurrentuser" + i)
                    .email(i == 0 ? commonEmail : "user" + i + "@example.com") // 第一个使用相同邮箱
                    .build();
        }

        try (MockedStatic<BaseContext> baseContextMock = mockStatic(BaseContext.class)) {
            baseContextMock.when(BaseContext::getCurrentUserId).thenReturn("admin_test");

            // 启动并发创建线程
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    try {
                        adminService.createUser(dtos[index]);
                    } catch (UserException e) {
                        // 预期第二个相同邮箱的会失败
                    }
                });
                threads[i].start();
            }

            // 等待所有线程完成
            for (Thread thread : threads) {
                thread.join(2000); // 最多等待2秒
            }

            // 验证数据一致性 - 应该只有一个用户使用commonEmail
            User userWithCommonEmail = mockDatabase.selectUserByEmail(commonEmail);
            assertNotNull(userWithCommonEmail, "One user should be created with common email");
            
            // 验证其他用户正常创建
            for (int i = 1; i < threadCount; i++) {
                User user = mockDatabase.selectUserByEmail("user" + i + "@example.com");
                assertNotNull(user, "User " + i + " should be created successfully");
            }
        }
    }

    @Test
    void testLockUser_WithExtremelyLongDuration_ShouldHandleCorrectly() {
        // 准备测试数据 - 极长的锁定时间
        User existingUser = TestDataBuilder.Users.validUser().build();
        mockDatabase.insertUser(existingUser);
        
        UserLockDTO extremeLockDTO = TestDataBuilder.DTOs.validLockDTO()
                .duration(Integer.MAX_VALUE) // 极大的锁定时间
                .build();

        // 执行测试
        UserLockResponseVO result = adminService.lockUser(existingUser.getId(), extremeLockDTO);

        // 验证结果
        assertNotNull(result, "Lock result should not be null");
        assertNotNull(result.getLockedUntil(), "Lock expiry should be set");
        
        // 验证状态一致性
        User lockedUser = mockDatabase.selectUserById(existingUser.getId());
        assertEquals(STATUS_LOCKED, lockedUser.getStatus());
    }

    @Test
    void testGrantPermission_WithNullValues_ShouldThrowException() {
        // 准备测试数据
        User existingUser = TestDataBuilder.Users.validUser().build();
        mockDatabase.insertUser(existingUser);

        // 测试null DTO
        assertThrows(Exception.class, 
            () -> adminService.grantPermission(existingUser.getId(), null),
            "Should throw exception when permission DTO is null");
    }

    // ======================== 性能测试 ========================

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testBulkUserOperations_ShouldCompleteWithinTimeLimit() {
        // 性能测试：批量用户操作
        int userCount = 50;
        
        try (MockedStatic<BaseContext> baseContextMock = mockStatic(BaseContext.class)) {
            baseContextMock.when(BaseContext::getCurrentUserId).thenReturn("admin_perf_test");

            // 批量创建用户
            for (int i = 0; i < userCount; i++) {
                UserCreateDTO dto = TestDataBuilder.DTOs.validCreateDTO()
                        .username("perfuser" + i)
                        .email("perfuser" + i + "@example.com")
                        .build();
                
                try {
                    UserCreateResponseVO result = adminService.createUser(dto);
                    assertNotNull(result, "User creation should succeed for user " + i);
                } catch (Exception e) {
                    fail("Bulk operation failed at user " + i + ": " + e.getMessage());
                }
            }

            // 验证所有用户都被创建
            // 注意：mockDatabase中已有初始用户，所以总数是userCount + 1
            assertTrue(mockDatabase.countUsers() >= userCount, 
                    "All users should be created successfully");
        }
    }
}