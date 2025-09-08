package com.feduwacomm.service;

import com.feduwacomm.common.BaseContext;
import com.feduwacomm.dto.*;
import com.feduwacomm.entity.User;
import com.feduwacomm.entity.UserPermission;
import com.feduwacomm.exception.UserException;
import com.feduwacomm.mapper.UserMapper;
import com.feduwacomm.mapper.UserPermissionMapper;
import com.feduwacomm.service.impl.UserServiceImpl;
import com.feduwacomm.utils.IpUtil;
import com.feduwacomm.utils.UserJwtUtil;
import com.feduwacomm.utils.PasswordUtil;
import com.feduwacomm.utils.UuidUtil;
import com.feduwacomm.vo.*;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserService单元测试类
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserPermissionMapper userPermissionMapper;

    @Mock
    private UserJwtUtil userJwtUtil;

    @Mock
    private UuidUtil uuidUtil;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserRegisterDTO registerDTO;
    private UserLoginDTO loginDTO;

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
                .build();

        registerDTO = UserRegisterDTO.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .confirmPassword("password123")
                .build();

        loginDTO = UserLoginDTO.builder()
                .loginIdentifier("testuser")
                .password("password123")
                .build();

        // Mock UuidUtil生成一致的UUID (lenient mode to avoid unnecessary stubbing warnings)
        lenient().when(uuidUtil.generateUuid()).thenReturn("a1b2c3d4e5f678901234567890123456");
    }

    // 认证相关方法测试

    @Test
    void testRegister_Success() {
        // 准备测试数据
        when(userMapper.selectByEmail(registerDTO.getEmail())).thenReturn(null);
        when(userMapper.selectByUsername(registerDTO.getUsername())).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenReturn(1);

        // 执行测试
        UserRegisterResponseVO result = userService.register(registerDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("VIEWER", result.getRole());
        assertEquals("ACTIVE", result.getStatus());

        verify(userMapper, times(1)).insert(any(User.class));
    }

    @Test
    void testRegister_EmailExists() {
        // 准备测试数据
        when(userMapper.selectByEmail(registerDTO.getEmail())).thenReturn(testUser);

        // 执行测试并验证异常
        UserException exception = assertThrows(UserException.class, () -> {
            userService.register(registerDTO);
        });

        assertEquals("邮箱已存在", exception.getMessage());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void testRegister_UsernameExists() {
        // 准备测试数据
        when(userMapper.selectByEmail(registerDTO.getEmail())).thenReturn(null);
        when(userMapper.selectByUsername(registerDTO.getUsername())).thenReturn(testUser);

        // 执行测试并验证异常
        UserException exception = assertThrows(UserException.class, () -> {
            userService.register(registerDTO);
        });

        assertEquals("用户名已存在", exception.getMessage());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void testLogin_Success() {
        // 准备测试数据
        when(userMapper.selectByUsername(loginDTO.getLoginIdentifier())).thenReturn(testUser);
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
            assertEquals("test_refresh_token", result.getRefreshToken());
            assertEquals(86400L, result.getExpiresIn());
            assertNotNull(result.getUser());
            assertEquals("testuser", result.getUser().getUsername());
        }

        verify(userMapper, times(1)).update(any(User.class));
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
}