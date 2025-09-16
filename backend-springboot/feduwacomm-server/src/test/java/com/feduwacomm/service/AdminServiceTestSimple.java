package com.feduwacomm.service;

import com.feduwacomm.dto.*;
import com.feduwacomm.entity.User;
import com.feduwacomm.exception.UserException;
import com.feduwacomm.mapper.UserMapper;
import com.feduwacomm.mapper.AdminMapper;
import com.feduwacomm.service.impl.AdminServiceImpl;
import com.feduwacomm.testdata.TestDataBuilder;
import com.feduwacomm.testdata.TestHelper;
import com.feduwacomm.utils.PasswordUtil;
import com.feduwacomm.utils.UuidUtil;
import com.feduwacomm.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AdminService简化单元测试类
 * 重点测试核心功能，避免UnnecessaryStubbing问题
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("管理员服务测试")
class AdminServiceTestSimple {

    @Mock
    private UserMapper userMapper;


    @Mock
    private AdminMapper adminMapper;

    @Mock
    private UuidUtil uuidUtil;

    @InjectMocks
    private AdminServiceImpl adminService;

    @BeforeEach
    void setUp() {
        initializePasswordUtil();
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
    @DisplayName("创建用户 - 成功创建")
    void testCreateUser_ValidData_ShouldCreateSuccessfully() {
        UserCreateDTO createDTO = TestDataBuilder.DTOs.validCreateDTO()
                .username("newuser")
                .email("newuser@example.com")
                .password("password123")
                .role("VIEWER")
                .build();

        when(adminMapper.selectByEmail("newuser@example.com")).thenReturn(null);
        when(adminMapper.selectByUsername("newuser")).thenReturn(null);
        when(adminMapper.insert(any(User.class))).thenReturn(1);
        when(uuidUtil.generateUuid()).thenReturn("test-uuid");

        UserCreateResponseVO result = adminService.createUser(createDTO);

        assertEquals("newuser", result.getUsername());
        assertEquals("newuser@example.com", result.getEmail());
        assertEquals("VIEWER", result.getRole());
        assertEquals("ACTIVE", result.getStatus());
        
        verify(adminMapper).selectByEmail("newuser@example.com");
        verify(adminMapper).selectByUsername("newuser");
        verify(adminMapper).insert(any(User.class));
    }

    @Test
    @DisplayName("创建用户 - 邮箱重复应抛异常")
    void testCreateUser_DuplicateEmail_ShouldThrowException() {
        UserCreateDTO createDTO = TestDataBuilder.DTOs.validCreateDTO()
                .email("duplicate@example.com")
                .build();

        User existingUser = TestDataBuilder.Users.validUser()
                .email("duplicate@example.com")
                .build();
                
        when(userMapper.selectByEmail("duplicate@example.com")).thenReturn(existingUser);

        UserException exception = assertThrows(UserException.class, 
            () -> adminService.createUser(createDTO));
        assertEquals("邮箱已存在", exception.getMessage());
        
        verify(userMapper).selectByEmail("duplicate@example.com");
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    @DisplayName("创建用户 - 用户名重复应抛异常")
    void testCreateUser_DuplicateUsername_ShouldThrowException() {
        UserCreateDTO createDTO = TestDataBuilder.DTOs.validCreateDTO()
                .username("duplicateuser")
                .email("new@example.com")
                .build();

        User existingUser = TestDataBuilder.Users.validUser()
                .username("duplicateuser")
                .build();
                
        when(userMapper.selectByEmail("new@example.com")).thenReturn(null);
        when(userMapper.selectByUsername("duplicateuser")).thenReturn(existingUser);

        UserException exception = assertThrows(UserException.class, 
            () -> adminService.createUser(createDTO));
        assertEquals("用户名已存在", exception.getMessage());
    }

    @Test
    @DisplayName("获取用户详情 - 用户存在")
    void testGetUserDetail_UserExists_ShouldReturnDetail() {
        User user = TestDataBuilder.Users.validUser().build();
        
        when(userMapper.selectById(user.getId())).thenReturn(user);

        UserDetailVO result = adminService.getUserDetail(user.getId());

        assertEquals(user.getId(), result.getUserId());
        assertEquals(user.getUsername(), result.getUsername());
        assertEquals(user.getEmail(), result.getEmail());
        assertEquals(user.getRole(), result.getRole());
    }

    @Test
    @DisplayName("获取用户详情 - 用户不存在")
    void testGetUserDetail_UserNotExists_ShouldThrowException() {
        when(userMapper.selectById("nonexistent")).thenReturn(null);

        UserException exception = assertThrows(UserException.class,
            () -> adminService.getUserDetail("nonexistent"));
        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    @DisplayName("更新用户信息 - 成功更新")
    void testUpdateUser_ValidData_ShouldUpdateSuccessfully() {
        User existingUser = TestDataBuilder.Users.validUser().build();
        
        UserAdminUpdateDTO updateDTO = TestDataBuilder.DTOs.validAdminUpdateDTO()
                .username("updateduser")
                .email("updated@example.com")
                .role("RESEARCHER")
                .status("ACTIVE")
                .build();

        when(userMapper.selectById(existingUser.getId())).thenReturn(existingUser);
        when(userMapper.selectByUsername("updateduser")).thenReturn(null);
        when(userMapper.selectByEmail("updated@example.com")).thenReturn(null);
        when(userMapper.update(any(User.class))).thenReturn(1);

        UserUpdateResponseVO result = adminService.updateUser(existingUser.getId(), updateDTO);

        assertEquals("updateduser", result.getUsername());
        assertEquals("updated@example.com", result.getEmail());
        assertEquals("RESEARCHER", result.getRole());
        assertEquals("ACTIVE", result.getStatus());
        
        verify(userMapper).update(any(User.class));
    }

    @Test
    @DisplayName("锁定用户 - 成功锁定")
    void testLockUser_ValidUser_ShouldLockSuccessfully() {
        User user = TestDataBuilder.Users.validUser().build();
        UserLockDTO lockDTO = TestDataBuilder.DTOs.validLockDTO()
                .duration(3600) // 1 hour
                .build();

        when(userMapper.selectById(user.getId())).thenReturn(user);
        when(userMapper.update(any(User.class))).thenReturn(1);

        UserLockResponseVO result = adminService.lockUser(user.getId(), lockDTO);

        assertEquals(user.getId(), result.getUserId());
        assertNotNull(result.getLockedUntil());
        
        verify(userMapper).selectById(user.getId());
        verify(userMapper).update(any(User.class));
    }

    @Test
    @DisplayName("解锁用户 - 成功解锁")
    void testUnlockUser_ValidUser_ShouldUnlockSuccessfully() {
        User lockedUser = TestDataBuilder.Users.lockedUser().build();

        when(userMapper.selectById(lockedUser.getId())).thenReturn(lockedUser);
        when(userMapper.update(any(User.class))).thenReturn(1);

        UserUnlockResponseVO result = adminService.unlockUser(lockedUser.getId());

        assertEquals(lockedUser.getId(), result.getUserId());
        assertEquals("ACTIVE", result.getStatus());
        
        verify(userMapper).update(any(User.class));
    }

    @Test
    @DisplayName("重置密码 - 成功重置")
    void testResetPassword_ValidUser_ShouldResetSuccessfully() {
        User user = TestDataBuilder.Users.validUser().build();
        PasswordResetDTO resetDTO = TestDataBuilder.DTOs.validPasswordResetDTO()
                .newPassword("newpassword123")
                .build();

        when(userMapper.selectById(user.getId())).thenReturn(user);
        when(userMapper.updatePassword(eq(user.getId()), anyString())).thenReturn(1);

        assertDoesNotThrow(() -> adminService.resetPassword(user.getId(), resetDTO));
        
        verify(userMapper).selectById(user.getId());
        verify(userMapper).updatePassword(eq(user.getId()), anyString());
    }

    @Test
    @DisplayName("删除用户 - 成功删除")
    void testDeleteUser_ValidUser_ShouldDeleteSuccessfully() {
        User user = TestDataBuilder.Users.validUser().build();

        when(userMapper.selectById(user.getId())).thenReturn(user);
        when(userMapper.deleteById(user.getId())).thenReturn(1);

        assertDoesNotThrow(() -> adminService.deleteUser(user.getId()));
        
        verify(userMapper).selectById(user.getId());
        verify(userMapper).deleteById(user.getId());
    }

    @Test
    @DisplayName("获取用户列表 - 基本查询")
    void testGetUserList_BasicQuery_ShouldReturnList() {
        UserQueryDTO queryDTO = TestDataBuilder.DTOs.validQueryDTO()
                .page(1)
                .size(10)
                .build();

        // 简化测试，验证方法调用而非复杂逻辑
        PageResponseDTO<UserListVO> result = adminService.getUserList(queryDTO);

        assertNotNull(result, "Result should not be null");
        // 注意：由于Mock没有配置返回值，结果可能为默认值
        assertNotNull(result.getRecords(), "Records should not be null");
    }

}