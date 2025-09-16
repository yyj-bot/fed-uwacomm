package com.feduwacomm.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static com.feduwacomm.testdata.TestHelper.Constants.*;

/**
 * UserService简化单元测试类
 * 重点测试核心功能，避免UnnecessaryStubbing问题
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("用户服务测试")
class UserServiceTestSimple {

    @Mock
    private UserMapper userMapper;


    @Mock
    private UserJwtUtil userJwtUtil;

    @Mock
    private UuidUtil uuidUtil;

    @InjectMocks
    private UserServiceImpl userService;

    private TestHelper.MockDatabase mockDatabase;

    @BeforeEach
    void setUp() {
        initializePasswordUtil();
        mockDatabase = new TestHelper.MockDatabase();
    }

    private void initializePasswordUtil() {
        try {
            java.lang.reflect.Field encoderField = PasswordUtil.class.getDeclaredField("encoder");
            encoderField.setAccessible(true);
            encoderField.set(null, new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder());
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize PasswordUtil for testing", e);
        }
    }

    @Test
    @DisplayName("用户注册 - 第一个用户应为管理员")
    void testRegister_FirstUser_ShouldReturnAdmin() {
        UserRegisterDTO dto = TestDataBuilder.DTOs.validRegisterDTO()
                .username("firstuser")
                .email("first@example.com")
                .build();

        // 配置Mock - 只设置当前测试需要的
        when(userMapper.selectByEmail("first@example.com")).thenReturn(null);
        when(userMapper.selectByUsername("firstuser")).thenReturn(null);
        when(userMapper.countAll()).thenReturn(0);
        when(userMapper.insert(any(User.class))).thenReturn(1);
        when(uuidUtil.generateUuid()).thenReturn("test-uuid");

        UserRegisterResponseVO result = userService.register(dto);

        assertEquals("firstuser", result.getUsername());
        assertEquals("first@example.com", result.getEmail());
        assertEquals("ADMIN", result.getRole());
        assertEquals("ACTIVE", result.getStatus());
        
        verify(userMapper).selectByEmail("first@example.com");
        verify(userMapper).selectByUsername("firstuser");
        verify(userMapper).countAll();
        verify(userMapper).insert(any(User.class));
    }

    @Test
    @DisplayName("用户注册 - 后续用户应为查看者")
    void testRegister_SubsequentUser_ShouldReturnViewer() {
        UserRegisterDTO dto = TestDataBuilder.DTOs.validRegisterDTO()
                .username("seconduser")
                .email("second@example.com")
                .build();

        when(userMapper.selectByEmail("second@example.com")).thenReturn(null);
        when(userMapper.selectByUsername("seconduser")).thenReturn(null);
        when(userMapper.countAll()).thenReturn(1);
        when(userMapper.insert(any(User.class))).thenReturn(1);
        when(uuidUtil.generateUuid()).thenReturn("test-uuid");

        UserRegisterResponseVO result = userService.register(dto);

        assertEquals("seconduser", result.getUsername());
        assertEquals("VIEWER", result.getRole());
    }

    @Test
    @DisplayName("用户注册 - 邮箱重复应抛异常")
    void testRegister_DuplicateEmail_ShouldThrowException() {
        UserRegisterDTO dto = TestDataBuilder.DTOs.validRegisterDTO()
                .email("duplicate@example.com")
                .build();

        User existingUser = TestDataBuilder.Users.validUser()
                .email("duplicate@example.com")
                .build();
                
        when(userMapper.selectByEmail("duplicate@example.com")).thenReturn(existingUser);

        UserException exception = assertThrows(UserException.class, 
            () -> userService.register(dto));
        assertEquals("邮箱已存在", exception.getMessage());
        
        verify(userMapper).selectByEmail("duplicate@example.com");
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    @DisplayName("用户登录 - 成功登录应返回令牌")
    void testLogin_ValidCredentials_ShouldReturnToken() {
        User loginUser = TestDataBuilder.Users.validUser()
                .username("loginuser")
                .passwordHash(PasswordUtil.encode("password123"))
                .build();

        UserLoginDTO loginDTO = TestDataBuilder.DTOs.validLoginDTO()
                .loginIdentifier("loginuser")
                .password("password123")
                .build();

        when(userMapper.selectByUsername("loginuser")).thenReturn(loginUser);
        when(userMapper.update(any(User.class))).thenReturn(1);
        when(userJwtUtil.generateAccessToken(anyString(), anyString(), anyString()))
                .thenReturn("access_token");
        when(userJwtUtil.generateRefreshToken(anyString()))
                .thenReturn("refresh_token");

        try (MockedStatic<IpUtil> ipUtilMock = mockStatic(IpUtil.class)) {
            ipUtilMock.when(IpUtil::getClientIpAddress).thenReturn("127.0.0.1");

            LoginResponseVO result = userService.login(loginDTO);

            assertEquals("access_token", result.getToken());
            assertEquals("refresh_token", result.getRefreshToken());
            assertEquals("loginuser", result.getUser().getUsername());
        }
    }

    @Test
    @DisplayName("用户登录 - 错误密码应抛异常")
    void testLogin_WrongPassword_ShouldThrowException() {
        User loginUser = TestDataBuilder.Users.validUser()
                .username("testuser")
                .passwordHash(PasswordUtil.encode("correctPassword"))
                .build();

        UserLoginDTO loginDTO = TestDataBuilder.DTOs.validLoginDTO()
                .loginIdentifier("testuser")
                .password("wrongPassword")
                .build();

        when(userMapper.selectByUsername("testuser")).thenReturn(loginUser);
        when(userMapper.update(any(User.class))).thenReturn(1);

        UserException exception = assertThrows(UserException.class,
            () -> userService.login(loginDTO));
        assertEquals("密码错误", exception.getMessage());
    }

    @Test
    @DisplayName("获取用户信息 - 用户存在应返回信息")
    void testGetCurrentUserInfo_UserExists_ShouldReturnInfo() {
        User user = TestDataBuilder.Users.validUser().build();
        
        when(userMapper.selectById(user.getId())).thenReturn(user);

        UserInfoVO result = userService.getCurrentUserInfo(user.getId());

        assertEquals(user.getId(), result.getUserId());
        assertEquals(user.getUsername(), result.getUsername());
        assertEquals(user.getEmail(), result.getEmail());
    }

    @Test
    @DisplayName("获取用户信息 - 用户不存在应抛异常")
    void testGetCurrentUserInfo_UserNotExists_ShouldThrowException() {
        when(userMapper.selectById("nonexistent")).thenReturn(null);

        UserException exception = assertThrows(UserException.class,
            () -> userService.getCurrentUserInfo("nonexistent"));
        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    @DisplayName("修改密码 - 成功修改")
    void testChangePassword_ValidOldPassword_ShouldSucceed() {
        User user = TestDataBuilder.Users.validUser()
                .passwordHash(PasswordUtil.encode("oldPassword"))
                .build();
                
        PasswordChangeDTO dto = TestDataBuilder.DTOs.validPasswordChangeDTO()
                .oldPassword("oldPassword")
                .newPassword("newPassword123")
                .confirmPassword("newPassword123")
                .build();

        when(userMapper.selectById(user.getId())).thenReturn(user);
        when(userMapper.updatePassword(eq(user.getId()), anyString())).thenReturn(1);

        assertDoesNotThrow(() -> userService.changePassword(user.getId(), dto));
        
        verify(userMapper).selectById(user.getId());
        verify(userMapper).updatePassword(eq(user.getId()), anyString());
    }

    @Test
    @DisplayName("修改密码 - 旧密码错误应抛异常")
    void testChangePassword_WrongOldPassword_ShouldThrowException() {
        User user = TestDataBuilder.Users.validUser()
                .passwordHash(PasswordUtil.encode("correctPassword"))
                .build();
                
        PasswordChangeDTO dto = TestDataBuilder.DTOs.validPasswordChangeDTO()
                .oldPassword("wrongPassword")
                .build();

        when(userMapper.selectById(user.getId())).thenReturn(user);

        UserException exception = assertThrows(UserException.class,
            () -> userService.changePassword(user.getId(), dto));
        assertEquals("密码错误", exception.getMessage());
    }

    @Test
    @DisplayName("修改密码 - 确认密码不匹配应抛异常")
    void testChangePassword_PasswordMismatch_ShouldThrowException() {
        User user = TestDataBuilder.Users.validUser()
                .passwordHash(PasswordUtil.encode("oldPassword"))
                .build();
                
        PasswordChangeDTO dto = TestDataBuilder.DTOs.validPasswordChangeDTO()
                .oldPassword("oldPassword")
                .newPassword("newPassword123")
                .confirmPassword("differentPassword")
                .build();

        when(userMapper.selectById(user.getId())).thenReturn(user);

        UserException exception = assertThrows(UserException.class,
            () -> userService.changePassword(user.getId(), dto));
        assertEquals("密码不匹配", exception.getMessage());
    }

    @Test
    @DisplayName("检查用户是否存在 - 存在")
    void testUserExists_UserExists_ShouldReturnTrue() {
        User user = TestDataBuilder.Users.validUser().build();
        
        when(userMapper.selectById(user.getId())).thenReturn(user);

        boolean result = userService.userExists(user.getId());
        assertTrue(result);
    }

    @Test
    @DisplayName("检查用户是否存在 - 不存在")
    void testUserExists_UserNotExists_ShouldReturnFalse() {
        when(userMapper.selectById("nonexistent")).thenReturn(null);

        boolean result = userService.userExists("nonexistent");
        assertFalse(result);
    }

    @Test
    @DisplayName("检查邮箱是否存在 - 存在")
    void testEmailExists_EmailExists_ShouldReturnTrue() {
        User user = TestDataBuilder.Users.validUser().build();
        
        when(userMapper.selectByEmail(user.getEmail())).thenReturn(user);

        boolean result = userService.emailExists(user.getEmail());
        assertTrue(result);
    }

    @Test
    @DisplayName("检查邮箱是否存在 - 不存在") 
    void testEmailExists_EmailNotExists_ShouldReturnFalse() {
        when(userMapper.selectByEmail("nonexistent@example.com")).thenReturn(null);

        boolean result = userService.emailExists("nonexistent@example.com");
        assertFalse(result);
    }
}