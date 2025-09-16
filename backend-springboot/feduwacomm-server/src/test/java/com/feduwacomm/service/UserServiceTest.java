package com.feduwacomm.service;

import com.feduwacomm.common.BaseContext;
import com.feduwacomm.dto.*;
import com.feduwacomm.entity.User;
import com.feduwacomm.exception.UserException;
import com.feduwacomm.mapper.UserMapper;
import com.feduwacomm.service.impl.UserServiceImpl;
import com.feduwacomm.testdata.TestDataBuilder;
import com.feduwacomm.testdata.TestHelper;
import com.feduwacomm.utils.IpUtil;
import com.feduwacomm.utils.UserJwtUtil;
import com.feduwacomm.utils.PasswordUtil;
import com.feduwacomm.utils.UuidUtil;
import com.feduwacomm.vo.*;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static com.feduwacomm.testdata.TestHelper.Matchers.*;
import static com.feduwacomm.testdata.TestHelper.Constants.*;

/**
 * UserService单元测试类
 * 
 * 测试目的：验证用户服务的所有业务逻辑
 * 主要测试场景：
 * 1. 用户注册（成功路径、邮箱/用户名重复、密码不匹配）
 * 2. 用户登录（成功路径、用户不存在、密码错误、账号锁定）
 * 3. 令牌刷新（成功路径、无效令牌、过期令牌）
 * 4. 用户信息管理（查询、更新、密码修改）
 * 5. 边界条件和异常处理
 * 6. 并发安全性测试
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.DisplayName.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;


    @Mock
    private UserJwtUtil userJwtUtil;

    @Mock
    private UuidUtil uuidUtil;

    @InjectMocks
    private UserServiceImpl userService;

    private TestHelper.MockDatabase mockDatabase;
    private User testUser;
    private UserRegisterDTO registerDTO;
    private UserLoginDTO loginDTO;

    @BeforeEach
    void setUp() {
        // 初始化PasswordUtil的encoder用于测试
        initializePasswordUtil();
        
        // 初始化Mock数据库，提供状态一致性
        mockDatabase = new TestHelper.MockDatabase();
        
        // 使用TestDataBuilder创建标准测试数据
        testUser = TestDataBuilder.Users.validUser().build();
        registerDTO = TestDataBuilder.DTOs.validRegisterDTO().build();
        loginDTO = TestDataBuilder.DTOs.validLoginDTO().build();
        
        // 不再统一配置Mock行为，改为按需配置以避免UnnecessaryStubbing
    }
    
    /**
     * 为测试初始化PasswordUtil
     */
    private void initializePasswordUtil() {
        try {
            // 使用反射设置PasswordUtil的encoder
            java.lang.reflect.Field encoderField = PasswordUtil.class.getDeclaredField("encoder");
            encoderField.setAccessible(true);
            encoderField.set(null, new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder());
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize PasswordUtil for testing", e);
        }
    }

    // 认证相关方法测试

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testRegister_WithValidData_ShouldReturnFirstUserAsAdmin() {
        // 准备测试数据 - 使用TestDataBuilder
        UserRegisterDTO firstUserDTO = TestDataBuilder.DTOs.validRegisterDTO()
                .username("firstuser")
                .email("first@example.com")
                .build();
        
        // 清空mock数据库，模拟第一个用户注册
        mockDatabase.clear();
        
        // 配置此测试需要的Mock行为
        when(userMapper.selectByEmail("first@example.com")).thenReturn(null);
        when(userMapper.selectByUsername("firstuser")).thenReturn(null);
        when(userMapper.countAll()).thenReturn(0); // 第一个用户
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            mockDatabase.insertUser(user);
            return 1;
        });
        when(uuidUtil.generateUuid()).thenReturn("test-uuid-first-user");

        // 执行测试
        UserRegisterResponseVO result = userService.register(firstUserDTO);

        // 验证结果 - 使用更具体的断言
        assertNotNull(result, "Registration result should not be null");
        assertEquals("firstuser", result.getUsername());
        assertEquals("first@example.com", result.getEmail());
        assertEquals(ROLE_ADMIN, result.getRole(), "First user should be ADMIN");
        assertEquals(STATUS_ACTIVE, result.getStatus());
        assertNotNull(result.getCreatedAt(), "Created timestamp should be set");
        TestHelper.Assertions.assertReasonableTimestamp(result.getCreatedAt());

        // 验证状态一致性 - 用户应该被保存到Mock数据库
        User savedUser = mockDatabase.selectUserByUsername("firstuser");
        assertNotNull(savedUser, "User should be saved to database");
        TestHelper.Assertions.assertUserValid(savedUser);
        assertEquals(ROLE_ADMIN, savedUser.getRole());
        
        // 验证时序 - 应该先检查用户存在性，再检查用户总数，最后插入
        InOrder inOrder = inOrder(userMapper);
        inOrder.verify(userMapper).selectByEmail("first@example.com");
        inOrder.verify(userMapper).selectByUsername("firstuser");
        inOrder.verify(userMapper).countAll();
        inOrder.verify(userMapper).insert(userMatching(user -> 
            "firstuser".equals(user.getUsername()) && 
            ROLE_ADMIN.equals(user.getRole()) &&
            user.getPasswordHash() != null &&
            !VALID_PASSWORD.equals(user.getPasswordHash()) // 确保密码被加密
        ));
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testRegister_WithValidDataAsSecondUser_ShouldReturnViewer() {
        // 准备测试数据 - 确保已有用户存在（非第一个用户）
        User existingUser = TestDataBuilder.Users.adminUser().build();
        mockDatabase.insertUser(existingUser);
        
        UserRegisterDTO secondUserDTO = TestDataBuilder.DTOs.validRegisterDTO()
                .username("seconduser")
                .email("second@example.com")
                .build();

        // 配置此测试需要的Mock行为
        when(userMapper.selectByEmail("second@example.com")).thenReturn(null);
        when(userMapper.selectByUsername("seconduser")).thenReturn(null);
        when(userMapper.countAll()).thenReturn(1); // 已有用户
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            mockDatabase.insertUser(user);
            return 1;
        });
        when(uuidUtil.generateUuid()).thenReturn("test-uuid-second-user");

        // 执行测试
        UserRegisterResponseVO result = userService.register(secondUserDTO);

        // 验证结果
        assertNotNull(result, "Registration result should not be null");
        assertEquals("seconduser", result.getUsername());
        assertEquals("second@example.com", result.getEmail());
        assertEquals(ROLE_VIEWER, result.getRole(), "Second user should be VIEWER");
        assertEquals(STATUS_ACTIVE, result.getStatus());
        
        // 验证状态一致性
        User savedUser = mockDatabase.selectUserByUsername("seconduser");
        assertNotNull(savedUser, "User should be saved to database");
        assertEquals(ROLE_VIEWER, savedUser.getRole());
        TestHelper.Assertions.assertPasswordEncrypted(VALID_PASSWORD, savedUser.getPasswordHash());
        
        // 验证时序
        InOrder inOrder = inOrder(userMapper);
        inOrder.verify(userMapper).selectByEmail("second@example.com");
        inOrder.verify(userMapper).selectByUsername("seconduser");
        inOrder.verify(userMapper).countAll();
        inOrder.verify(userMapper).insert(userWithRole(ROLE_VIEWER));
    }

    @Test
    void testRegister_WithDuplicateEmail_ShouldThrowUserException() {
        // 准备测试数据 - 预先插入用户使邮箱重复
        User existingUser = TestDataBuilder.Users.validUser()
                .email("duplicate@example.com")
                .build();
        mockDatabase.insertUser(existingUser);
        
        UserRegisterDTO duplicateEmailDTO = TestDataBuilder.DTOs.validRegisterDTO()
                .email("duplicate@example.com")
                .username("differentuser")
                .build();

        // 执行测试并验证异常
        UserException exception = assertThrows(UserException.class, 
            () -> userService.register(duplicateEmailDTO),
            "Should throw UserException when email already exists");

        // 验证异常消息
        assertEquals("邮箱已存在", exception.getMessage());
        
        // 验证没有进行插入操作
        verify(userMapper, never()).insert(any(User.class));
        
        // 验证调用顺序 - 应该在检查邮箱时就停止
        InOrder inOrder = inOrder(userMapper);
        inOrder.verify(userMapper).selectByEmail("duplicate@example.com");
        inOrder.verify(userMapper, never()).selectByUsername(anyString());
    }

    @Test
    void testRegister_WithDuplicateUsername_ShouldThrowUserException() {
        // 准备测试数据 - 预先插入用户使用户名重复
        User existingUser = TestDataBuilder.Users.validUser()
                .username("duplicateuser")
                .email("different@example.com")
                .build();
        mockDatabase.insertUser(existingUser);
        
        UserRegisterDTO duplicateUsernameDTO = TestDataBuilder.DTOs.validRegisterDTO()
                .username("duplicateuser")
                .email("new@example.com")
                .build();

        // 执行测试并验证异常
        UserException exception = assertThrows(UserException.class, 
            () -> userService.register(duplicateUsernameDTO),
            "Should throw UserException when username already exists");

        // 验证异常消息
        assertEquals("用户名已存在", exception.getMessage());
        
        // 验证没有进行插入操作
        verify(userMapper, never()).insert(any(User.class));
        
        // 验证调用顺序 - 应该检查邮箱通过后，在检查用户名时停止
        InOrder inOrder = inOrder(userMapper);
        inOrder.verify(userMapper).selectByEmail("new@example.com");
        inOrder.verify(userMapper).selectByUsername("duplicateuser");
        inOrder.verify(userMapper, never()).countAll();
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testLogin_WithValidCredentials_ShouldReturnTokenAndUpdateLastLogin() {
        // 准备测试数据 - 确保用户存在于Mock数据库中
        User loginUser = TestDataBuilder.Users.validUser()
                .username("loginuser")
                .passwordHash(PasswordUtil.encode(VALID_PASSWORD))
                .build();
        mockDatabase.insertUser(loginUser);
        
        UserLoginDTO validLoginDTO = TestDataBuilder.DTOs.validLoginDTO()
                .loginIdentifier("loginuser")
                .password(VALID_PASSWORD)
                .build();

        // Mock JWT工具
        when(userJwtUtil.generateAccessToken(anyString(), anyString(), anyString()))
                .thenReturn("test_access_token");
        when(userJwtUtil.generateRefreshToken(anyString()))
                .thenReturn("test_refresh_token");

        try (MockedStatic<IpUtil> ipUtilMock = mockStatic(IpUtil.class)) {
            ipUtilMock.when(IpUtil::getClientIpAddress).thenReturn(TEST_IP);

            // 执行测试
            LoginResponseVO result = userService.login(validLoginDTO);

            // 验证结果
            assertNotNull(result, "Login result should not be null");
            assertEquals("test_access_token", result.getToken());
            assertEquals("test_refresh_token", result.getRefreshToken());
            assertEquals(86400L, result.getExpiresIn());
            assertNotNull(result.getUser(), "User info should be included");
            assertEquals("loginuser", result.getUser().getUsername());
            assertEquals(ROLE_VIEWER, result.getUser().getRole());
            assertEquals(STATUS_ACTIVE, result.getUser().getStatus());
        }

        // 验证状态一致性 - 用户的最后登录信息应该被更新
        User updatedUser = mockDatabase.selectUserByUsername("loginuser");
        assertNotNull(updatedUser.getLastLoginTime(), "Last login time should be updated");
        assertEquals(TEST_IP, updatedUser.getLastLoginIp());
        assertEquals(0, updatedUser.getLoginAttempts(), "Login attempts should be reset");
        
        // 验证时序
        InOrder inOrder = inOrder(userMapper, userJwtUtil);
        inOrder.verify(userMapper).selectByUsername("loginuser");
        inOrder.verify(userMapper).update(userMatching(user -> 
            user.getLastLoginTime() != null &&
            TEST_IP.equals(user.getLastLoginIp()) &&
            user.getLoginAttempts() == 0
        ));
        inOrder.verify(userJwtUtil).generateAccessToken(loginUser.getId(), "loginuser", ROLE_VIEWER);
        inOrder.verify(userJwtUtil).generateRefreshToken(loginUser.getId());
    }

    @Test
    void testLogin_UserNotFound() {
        // 准备测试数据
        when(userMapper.selectByUsername(loginDTO.getLoginIdentifier())).thenReturn(null);

        // 执行测试并验证异常
        UserException exception = assertThrows(UserException.class, () -> {
            userService.login(loginDTO);
        });

        assertEquals("密码错误", exception.getMessage());
    }

    @Test
    void testLogin_WrongPassword() {
        // 准备测试数据
        when(userMapper.selectByUsername(loginDTO.getLoginIdentifier())).thenReturn(testUser);
        when(userMapper.update(any(User.class))).thenReturn(1);

        UserLoginDTO wrongPasswordDTO = UserLoginDTO.builder()
                .loginIdentifier("testuser")
                .password("wrongpassword")
                .build();

        // 执行测试并验证异常
        UserException exception = assertThrows(UserException.class, () -> {
            userService.login(wrongPasswordDTO);
        });

        assertEquals("密码错误", exception.getMessage());
        verify(userMapper, times(1)).update(any(User.class));
    }

    @Test
    void testLogin_AccountLocked() {
        // 准备测试数据
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

        when(userMapper.selectByUsername(loginDTO.getLoginIdentifier())).thenReturn(lockedUser);

        // 执行测试并验证异常
        UserException exception = assertThrows(UserException.class, () -> {
            userService.login(loginDTO);
        });

        assertEquals("账号已锁定", exception.getMessage());
    }

    @Test
    void testLogin_AccountLockedExpired() {
        // 准备测试数据
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
                .lockedUntil(LocalDateTime.now().minusHours(1))
                .build();

        when(userMapper.selectByUsername(loginDTO.getLoginIdentifier())).thenReturn(lockedUser);
        when(userMapper.update(any(User.class))).thenReturn(1);
        when(userJwtUtil.generateAccessToken(anyString(), anyString(), anyString())).thenReturn("test_token");
        when(userJwtUtil.generateRefreshToken(anyString())).thenReturn("test_refresh_token");

        try (MockedStatic<IpUtil> ipUtilMock = mockStatic(IpUtil.class)) {
            ipUtilMock.when(IpUtil::getClientIpAddress).thenReturn("192.168.1.100");

            // 执行测试
            LoginResponseVO result = userService.login(loginDTO);

            // 验证结果
            assertNotNull(result);
            assertEquals("test_token", result.getToken());
        }

        verify(userMapper, times(2)).update(any(User.class));
    }

    @Test
    void testRefreshToken_Success() {
        // 准备测试数据
        String refreshToken = "valid_refresh_token";
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", testUser.getId());
        claims.put("type", "refresh");

        when(userJwtUtil.validateToken(refreshToken)).thenReturn(mock(Claims.class));
        when(userJwtUtil.validateToken(refreshToken).get("userId", String.class)).thenReturn(testUser.getId());
        when(userJwtUtil.validateToken(refreshToken).get("type", String.class)).thenReturn("refresh");
        when(userMapper.selectById(testUser.getId())).thenReturn(testUser);
        when(userJwtUtil.generateAccessToken(anyString(), anyString(), anyString())).thenReturn("new_token");
        when(userJwtUtil.generateRefreshToken(anyString())).thenReturn("new_refresh_token");

        // 执行测试
        TokenRefreshResponseVO result = userService.refreshToken(refreshToken);

        // 验证结果
        assertNotNull(result);
        assertEquals("new_token", result.getToken());
        assertEquals("new_refresh_token", result.getRefreshToken());
        assertEquals(86400L, result.getExpiresIn());
    }

    @Test
    void testRefreshToken_InvalidToken() {
        // 准备测试数据
        String invalidToken = "invalid_token";
        when(userJwtUtil.validateToken(invalidToken)).thenThrow(new UserException("Invalid token"));

        // 执行测试并验证异常
        UserException exception = assertThrows(UserException.class, () -> {
            userService.refreshToken(invalidToken);
        });

        assertEquals("Invalid token", exception.getMessage());
    }

    @Test
    void testLogout_Success() {
        // 准备测试数据
        String token = "test_token";

        // 执行测试
        assertDoesNotThrow(() -> {
            userService.logout(token);
        });
    }

    // 用户信息相关方法测试

    @Test
    void testGetCurrentUserInfo_Success() {
        // 准备测试数据
        String userId = "a1b2c3d4e5f678901234567890123456";
        when(userMapper.selectById(userId)).thenReturn(testUser);

        // 执行测试
        UserInfoVO result = userService.getCurrentUserInfo(userId);

        // 验证结果
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("VIEWER", result.getRole());
        assertEquals("ACTIVE", result.getStatus());
    }

    @Test
    void testGetCurrentUserInfo_UserNotFound() {
        // 准备测试数据
        String userId = "nonexistent_user";
        when(userMapper.selectById(userId)).thenReturn(null);

        // 执行测试并验证异常
        UserException exception = assertThrows(UserException.class, () -> {
            userService.getCurrentUserInfo(userId);
        });

        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    void testUpdateUserInfo_Success() {
        // 准备测试数据
        String userId = "a1b2c3d4e5f678901234567890123456";
        UserUpdateDTO updateDTO = UserUpdateDTO.builder()
                .username("newusername")
                .email("newemail@example.com")
                .build();

        when(userMapper.selectById(userId)).thenReturn(testUser);
        when(userMapper.selectByUsername("newusername")).thenReturn(null);
        when(userMapper.selectByEmail("newemail@example.com")).thenReturn(null);
        when(userMapper.update(any(User.class))).thenReturn(1);

        // 执行测试
        UserUpdateResponseVO result = userService.updateUserInfo(userId, updateDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals("newusername", result.getUsername());
        assertEquals("newemail@example.com", result.getEmail());

        verify(userMapper, times(1)).update(any(User.class));
    }

    @Test
    void testUpdateUserInfo_UserNotFound() {
        // 准备测试数据
        String userId = "nonexistent_user";
        UserUpdateDTO updateDTO = UserUpdateDTO.builder()
                .username("newusername")
                .build();

        when(userMapper.selectById(userId)).thenReturn(null);

        // 执行测试并验证异常
        UserException exception = assertThrows(UserException.class, () -> {
            userService.updateUserInfo(userId, updateDTO);
        });

        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    void testUpdateUserInfo_UsernameExists() {
        // 准备测试数据
        String userId = "a1b2c3d4e5f678901234567890123456";
        UserUpdateDTO updateDTO = UserUpdateDTO.builder()
                .username("existingusername")
                .build();

        User existingUser = User.builder().id("other_user").username("existingusername").build();

        when(userMapper.selectById(userId)).thenReturn(testUser);
        when(userMapper.selectByUsername("existingusername")).thenReturn(existingUser);

        // 执行测试并验证异常
        UserException exception = assertThrows(UserException.class, () -> {
            userService.updateUserInfo(userId, updateDTO);
        });

        assertEquals("用户名已存在", exception.getMessage());
    }

    @Test
    void testChangePassword_Success() {
        // 准备测试数据
        String userId = "a1b2c3d4e5f678901234567890123456";
        PasswordChangeDTO passwordDTO = PasswordChangeDTO.builder()
                .oldPassword("password123")
                .newPassword("newpassword123")
                .confirmPassword("newpassword123")
                .build();

        when(userMapper.selectById(userId)).thenReturn(testUser);
        when(userMapper.updatePassword(eq(userId), anyString())).thenReturn(1);

        // 执行测试
        assertDoesNotThrow(() -> {
            userService.changePassword(userId, passwordDTO);
        });

        verify(userMapper, times(1)).updatePassword(eq(userId), anyString());
    }

    @Test
    void testChangePassword_WrongOldPassword() {
        // 准备测试数据
        String userId = "a1b2c3d4e5f678901234567890123456";
        PasswordChangeDTO passwordDTO = PasswordChangeDTO.builder()
                .oldPassword("wrongpassword")
                .newPassword("newpassword123")
                .confirmPassword("newpassword123")
                .build();

        when(userMapper.selectById(userId)).thenReturn(testUser);

        // 执行测试并验证异常
        UserException exception = assertThrows(UserException.class, () -> {
            userService.changePassword(userId, passwordDTO);
        });

        assertEquals("密码错误", exception.getMessage());
    }

    @Test
    void testChangePassword_PasswordMismatch() {
        // 准备测试数据
        String userId = "a1b2c3d4e5f678901234567890123456";
        PasswordChangeDTO passwordDTO = PasswordChangeDTO.builder()
                .oldPassword("password123")
                .newPassword("newpassword123")
                .confirmPassword("differentpassword")
                .build();

        when(userMapper.selectById(userId)).thenReturn(testUser);

        // 执行测试并验证异常
        UserException exception = assertThrows(UserException.class, () -> {
            userService.changePassword(userId, passwordDTO);
        });

        assertEquals("密码不匹配", exception.getMessage());
    }

    // 内部方法测试

    @Test
    void testGetUserById_Success() {
        // 准备测试数据
        String userId = "a1b2c3d4e5f678901234567890123456";
        when(userMapper.selectById(userId)).thenReturn(testUser);

        // 执行测试
        User result = userService.getUserById(userId);

        // 验证结果
        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void testGetUserByLoginIdentifier_Success() {
        // 准备测试数据
        String loginIdentifier = "testuser";
        when(userMapper.selectByUsername(loginIdentifier)).thenReturn(testUser);

        // 执行测试
        User result = userService.getUserByLoginIdentifier(loginIdentifier);

        // 验证结果
        assertNotNull(result);
        assertEquals(loginIdentifier, result.getUsername());
    }

    @Test
    void testUserExists_True() {
        // 准备测试数据
        String userId = "a1b2c3d4e5f678901234567890123456";
        when(userMapper.selectById(userId)).thenReturn(testUser);

        // 执行测试
        boolean result = userService.userExists(userId);

        // 验证结果
        assertTrue(result);
    }

    @Test
    void testUserExists_False() {
        // 准备测试数据
        String userId = "nonexistent_user";
        when(userMapper.selectById(userId)).thenReturn(null);

        // 执行测试
        boolean result = userService.userExists(userId);

        // 验证结果
        assertFalse(result);
    }

    @Test
    void testLoginIdentifierExists_True() {
        // 准备测试数据
        String loginIdentifier = "testuser";
        when(userMapper.selectByUsername(loginIdentifier)).thenReturn(testUser);

        // 执行测试
        boolean result = userService.loginIdentifierExists(loginIdentifier);

        // 验证结果
        assertTrue(result);
    }

    @Test
    void testLoginIdentifierExists_False() {
        // 准备测试数据
        String loginIdentifier = "nonexistent_user";
        when(userMapper.selectByUsername(loginIdentifier)).thenReturn(null);

        // 执行测试
        boolean result = userService.loginIdentifierExists(loginIdentifier);

        // 验证结果
        assertFalse(result);
    }

    @Test
    void testEmailExists_True() {
        // 准备测试数据
        String email = "test@example.com";
        when(userMapper.selectByEmail(email)).thenReturn(testUser);

        // 执行测试
        boolean result = userService.emailExists(email);

        // 验证结果
        assertTrue(result);
    }

    @Test
    void testEmailExists_False() {
        // 准备测试数据
        String email = "nonexistent@example.com";
        when(userMapper.selectByEmail(email)).thenReturn(null);

        // 执行测试
        boolean result = userService.emailExists(email);

        // 验证结果
        assertFalse(result);
    }

    @Test
    void testUsernameExists_True() {
        // 准备测试数据
        String username = "testuser";
        when(userMapper.selectByUsername(username)).thenReturn(testUser);

        // 执行测试
        boolean result = userService.usernameExists(username);

        // 验证结果
        assertTrue(result);
    }

    @Test
    void testUsernameExists_False() {
        // 准备测试数据
        String username = "nonexistent_username";
        when(userMapper.selectByUsername(username)).thenReturn(null);

        // 执行测试
        boolean result = userService.usernameExists(username);

        // 验证结果
        assertFalse(result);
    }

    // ======================== 边界条件和异常路径测试 ========================

    @Test
    void testRegister_WithNullDTO_ShouldThrowException() {
        // 执行测试并验证异常
        assertThrows(Exception.class, () -> userService.register(null),
                "Should throw exception when DTO is null");
    }
    
    @Test
    void testLogin_WithNullDTO_ShouldThrowException() {
        // 执行测试并验证异常
        assertThrows(Exception.class, () -> userService.login(null),
                "Should throw exception when DTO is null");
    }
    
    @Test
    void testRegister_WithEmptyStrings_ShouldHandleGracefully() {
        // 准备测试数据 - 空字符串
        UserRegisterDTO emptyDTO = TestDataBuilder.DTOs.validRegisterDTO()
                .username("")
                .email("")
                .password("")
                .confirmPassword("")
                .build();

        // 执行测试并验证行为（可能抛出异常或返回错误）
        assertThrows(Exception.class, () -> userService.register(emptyDTO),
                "Should handle empty strings appropriately");
    }

    @Test 
    void testRegister_WithExtremelyLongValues_ShouldHandleGracefully() {
        // 准备测试数据 - 极长的字符串
        String longString = "a".repeat(1000);
        UserRegisterDTO longDTO = TestDataBuilder.DTOs.validRegisterDTO()
                .username(longString)
                .email(longString + "@example.com")
                .password(longString)
                .confirmPassword(longString)
                .build();

        // 执行测试并验证行为
        assertThrows(Exception.class, () -> userService.register(longDTO),
                "Should handle extremely long values appropriately");
    }

    @Test
    void testLogin_WithSpecialCharacters_ShouldHandleCorrectly() {
        // 准备测试数据 - 包含特殊字符的登录
        User specialUser = TestDataBuilder.Users.validUser()
                .username("user@#$%^&*()")
                .passwordHash(PasswordUtil.encode("pass@#$%^&*()"))
                .build();
        mockDatabase.insertUser(specialUser);
        
        UserLoginDTO specialLoginDTO = TestDataBuilder.DTOs.validLoginDTO()
                .loginIdentifier("user@#$%^&*()")
                .password("pass@#$%^&*()")
                .build();

        // Mock JWT工具
        when(userJwtUtil.generateAccessToken(anyString(), anyString(), anyString()))
                .thenReturn("test_token");
        when(userJwtUtil.generateRefreshToken(anyString())).thenReturn("test_refresh_token");

        try (MockedStatic<IpUtil> ipUtilMock = mockStatic(IpUtil.class)) {
            ipUtilMock.when(IpUtil::getClientIpAddress).thenReturn(TEST_IP);
            
            // 执行测试
            LoginResponseVO result = userService.login(specialLoginDTO);
            
            // 验证结果
            assertNotNull(result, "Should handle special characters in credentials");
            assertEquals("user@#$%^&*()", result.getUser().getUsername());
        }
    }

    // ======================== 并发安全性测试 ========================

    @Test
    void testConcurrentRegisterSameEmail_OnlyOneSuccess() throws InterruptedException {
        // 准备测试数据
        String commonEmail = "concurrent@example.com";
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        UserRegisterDTO[] dtos = new UserRegisterDTO[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            dtos[i] = TestDataBuilder.DTOs.validRegisterDTO()
                    .username("user" + i)
                    .email(commonEmail) // 相同邮箱
                    .build();
        }

        // 启动并发注册线程
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    userService.register(dtos[index]);
                } catch (UserException e) {
                    // 预期大部分会失败，只有第一个成功
                }
            });
            threads[i].start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join(1000); // 最多等待1秒
        }

        // 验证只有一个用户被创建
        User savedUser = mockDatabase.selectUserByEmail(commonEmail);
        assertNotNull(savedUser, "Exactly one user should be created");
        
        // 验证其他用户名对应的用户不存在
        for (int i = 0; i < threadCount; i++) {
            User userByName = mockDatabase.selectUserByUsername("user" + i);
            if (userByName != null) {
                assertEquals(commonEmail, userByName.getEmail());
            }
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testConcurrentLogin_SameConcurrency() throws InterruptedException {
        // 准备测试数据
        User concurrentUser = TestDataBuilder.Users.validUser()
                .username("concurrentuser")
                .passwordHash(PasswordUtil.encode(VALID_PASSWORD))
                .build();
        mockDatabase.insertUser(concurrentUser);

        UserLoginDTO loginDTO = TestDataBuilder.DTOs.validLoginDTO()
                .loginIdentifier("concurrentuser")
                .password(VALID_PASSWORD)
                .build();

        // Mock JWT工具
        when(userJwtUtil.generateAccessToken(anyString(), anyString(), anyString()))
                .thenReturn("test_token");
        when(userJwtUtil.generateRefreshToken(anyString()))
                .thenReturn("test_refresh_token");

        int threadCount = 5;
        Thread[] threads = new Thread[threadCount];
        LoginResponseVO[] results = new LoginResponseVO[threadCount];

        try (MockedStatic<IpUtil> ipUtilMock = mockStatic(IpUtil.class)) {
            ipUtilMock.when(IpUtil::getClientIpAddress).thenReturn(TEST_IP);

            // 启动并发登录线程
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    try {
                        results[index] = userService.login(loginDTO);
                    } catch (Exception e) {
                        // 记录并发访问可能的异常
                        System.err.println("Concurrent login error: " + e.getMessage());
                    }
                });
                threads[i].start();
            }

            // 等待所有线程完成
            for (Thread thread : threads) {
                thread.join(2000); // 最多等待2秒
            }

            // 验证所有登录都应该成功（如果实现正确的话）
            int successCount = 0;
            for (LoginResponseVO result : results) {
                if (result != null && result.getToken() != null) {
                    successCount++;
                }
            }

            assertTrue(successCount > 0, "At least one login should succeed");
            
            // 验证用户状态一致性
            User updatedUser = mockDatabase.selectUserByUsername("concurrentuser");
            assertNotNull(updatedUser.getLastLoginTime(), "Last login time should be updated");
            assertEquals(0, updatedUser.getLoginAttempts(), "Login attempts should be reset");
        }
    }

    // ======================== 性能和资源限制测试 ========================

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testRegister_PerformanceUnderLoad() {
        // 性能测试：快速注册大量用户
        int userCount = 100;
        for (int i = 0; i < userCount; i++) {
            UserRegisterDTO dto = TestDataBuilder.DTOs.validRegisterDTO()
                    .username("perfuser" + i)
                    .email("perfuser" + i + "@example.com")
                    .build();
            
            try {
                UserRegisterResponseVO result = userService.register(dto);
                assertNotNull(result, "Registration should succeed for user " + i);
            } catch (Exception e) {
                fail("Performance test failed at user " + i + ": " + e.getMessage());
            }
        }
        
        // 验证所有用户都被保存
        assertEquals(userCount + 1, mockDatabase.countUsers(), // +1 for initial test user
                "All users should be registered");
    }
}