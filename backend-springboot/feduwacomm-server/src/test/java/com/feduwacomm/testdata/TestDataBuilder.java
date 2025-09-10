package com.feduwacomm.testdata;

import com.feduwacomm.dto.*;
import com.feduwacomm.entity.User;
import com.feduwacomm.entity.UserPermission;
import com.feduwacomm.entity.VmInstance;
import com.feduwacomm.utils.PasswordUtil;
import com.feduwacomm.vo.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 统一的测试数据构建器
 * 提供创建各种测试对象的标准方法
 * 
 * 使用Builder模式创建测试数据，便于维护和复用
 */
public class TestDataBuilder {

    /**
     * 用户相关测试数据构建器
     */
    public static class Users {
        
        public static User.UserBuilder validUser() {
            return User.builder()
                    .id(generateTestId())
                    .username("testuser")
                    .email("test@example.com")
                    .passwordHash(PasswordUtil.encode("password123"))
                    .role("VIEWER")
                    .status("ACTIVE")
                    .loginAttempts(0)
                    .lastLoginTime(LocalDateTime.now().minusHours(1))
                    .lastLoginIp("192.168.1.100")
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .updatedAt(LocalDateTime.now())
                    .createdBy("system")
                    .updatedBy("system");
        }
        
        public static User.UserBuilder adminUser() {
            return validUser()
                    .username("admin")
                    .email("admin@example.com")
                    .role("ADMIN");
        }
        
        public static User.UserBuilder researcherUser() {
            return validUser()
                    .username("researcher")
                    .email("researcher@example.com")
                    .role("RESEARCHER");
        }
        
        public static User.UserBuilder lockedUser() {
            return validUser()
                    .status("LOCKED")
                    .loginAttempts(5)
                    .lockedUntil(LocalDateTime.now().plusHours(1));
        }
        
        public static User.UserBuilder expiredLockedUser() {
            return validUser()
                    .status("LOCKED")
                    .loginAttempts(3)
                    .lockedUntil(LocalDateTime.now().minusHours(1));
        }
    }

    /**
     * DTO相关测试数据构建器
     */
    public static class DTOs {
        
        public static UserRegisterDTO.UserRegisterDTOBuilder validRegisterDTO() {
            return UserRegisterDTO.builder()
                    .username("newuser")
                    .email("newuser@example.com")
                    .password("password123")
                    .confirmPassword("password123");
        }
        
        public static UserRegisterDTO.UserRegisterDTOBuilder invalidRegisterDTO() {
            return validRegisterDTO()
                    .confirmPassword("differentPassword");
        }
        
        public static UserLoginDTO.UserLoginDTOBuilder validLoginDTO() {
            return UserLoginDTO.builder()
                    .loginIdentifier("testuser")
                    .password("password123")
                    .rememberMe(false);
        }
        
        public static UserLoginDTO.UserLoginDTOBuilder loginWithCaptchaDTO() {
            return validLoginDTO()
                    .captcha("1234")
                    .captchaKey("key123")
                    .rememberMe(true);
        }
        
        public static UserUpdateDTO.UserUpdateDTOBuilder validUpdateDTO() {
            return UserUpdateDTO.builder()
                    .username("updateduser")
                    .email("updated@example.com");
        }
        
        public static PasswordChangeDTO.PasswordChangeDTOBuilder validPasswordChangeDTO() {
            return PasswordChangeDTO.builder()
                    .oldPassword("password123")
                    .newPassword("newpassword123")
                    .confirmPassword("newpassword123");
        }
        
        public static UserCreateDTO.UserCreateDTOBuilder validCreateDTO() {
            return UserCreateDTO.builder()
                    .username("newuser")
                    .email("newuser@example.com")
                    .password("password123")
                    .role("VIEWER");
        }
        
        public static UserAdminUpdateDTO.UserAdminUpdateDTOBuilder validAdminUpdateDTO() {
            return UserAdminUpdateDTO.builder()
                    .username("updateduser")
                    .email("updated@example.com")
                    .role("RESEARCHER")
                    .status("ACTIVE");
        }
        
        public static UserLockDTO.UserLockDTOBuilder validLockDTO() {
            return UserLockDTO.builder()
                    .duration(3600); // 1 hour
        }
        
        public static PasswordResetDTO.PasswordResetDTOBuilder validPasswordResetDTO() {
            return PasswordResetDTO.builder()
                    .newPassword("newpassword123");
        }
        
        public static UserQueryDTO.UserQueryDTOBuilder validQueryDTO() {
            return UserQueryDTO.builder()
                    .page(1)
                    .size(10)
                    .role("VIEWER")
                    .status("ACTIVE")
                    .keyword("test");
        }
        
        public static PermissionGrantDTO.PermissionGrantDTOBuilder validPermissionGrantDTO() {
            return PermissionGrantDTO.builder()
                    .resourceType("VM")
                    .resourceId(generateTestId())
                    .permission("READ")
                    .expiresAt(LocalDateTime.now().plusDays(30));
        }
    }

    /**
     * VO相关测试数据构建器
     */
    public static class VOs {
        
        public static UserInfoVO.UserInfoVOBuilder validUserInfoVO() {
            return UserInfoVO.builder()
                    .userId(generateTestId())
                    .username("testuser")
                    .email("test@example.com")
                    .role("VIEWER")
                    .status("ACTIVE")
                    .lastLoginTime(LocalDateTime.now().minusHours(1))
                    .lastLoginIp("192.168.1.100")
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .updatedAt(LocalDateTime.now());
        }
        
        public static LoginResponseVO.LoginResponseVOBuilder validLoginResponseVO() {
            return LoginResponseVO.builder()
                    .token("test_access_token")
                    .refreshToken("test_refresh_token")
                    .expiresIn(86400L)
                    .user(validUserInfoVO().build());
        }
        
        public static TokenRefreshResponseVO.TokenRefreshResponseVOBuilder validTokenRefreshResponseVO() {
            return TokenRefreshResponseVO.builder()
                    .token("new_access_token")
                    .refreshToken("new_refresh_token")
                    .expiresIn(86400L);
        }
        
        public static UserRegisterResponseVO.UserRegisterResponseVOBuilder validRegisterResponseVO() {
            return UserRegisterResponseVO.builder()
                    .userId(generateTestId())
                    .username("testuser")
                    .email("test@example.com")
                    .role("VIEWER")
                    .status("ACTIVE")
                    .createdAt(LocalDateTime.now());
        }
        
        public static UserUpdateResponseVO.UserUpdateResponseVOBuilder validUpdateResponseVO() {
            return UserUpdateResponseVO.builder()
                    .userId(generateTestId())
                    .username("updateduser")
                    .email("updated@example.com")
                    .role("VIEWER")
                    .status("ACTIVE")
                    .updatedAt(LocalDateTime.now());
        }
        
        public static UserCreateResponseVO.UserCreateResponseVOBuilder validCreateResponseVO() {
            return UserCreateResponseVO.builder()
                    .userId(generateTestId())
                    .username("newuser")
                    .email("newuser@example.com")
                    .role("VIEWER")
                    .status("ACTIVE")
                    .createdAt(LocalDateTime.now());
        }
        
        public static UserDetailVO.UserDetailVOBuilder validUserDetailVO() {
            return UserDetailVO.builder()
                    .userId(generateTestId())
                    .username("testuser")
                    .email("test@example.com")
                    .role("VIEWER")
                    .status("ACTIVE")
                    .loginAttempts(0)
                    .lastLoginTime(LocalDateTime.now().minusHours(1))
                    .lastLoginIp("192.168.1.100")
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .updatedAt(LocalDateTime.now())
                    .createdBy("system")
                    .updatedBy("system");
        }
        
        public static UserListVO.UserListVOBuilder validUserListVO() {
            return UserListVO.builder()
                    .userId(generateTestId())
                    .username("testuser")
                    .email("test@example.com")
                    .role("VIEWER")
                    .status("ACTIVE")
                    .lastLoginTime(LocalDateTime.now().minusHours(1))
                    .createdAt(LocalDateTime.now().minusDays(1));
        }
        
        public static UserLockResponseVO.UserLockResponseVOBuilder validLockResponseVO() {
            return UserLockResponseVO.builder()
                    .userId(generateTestId())
                    .lockedUntil(LocalDateTime.now().plusHours(1));
        }
        
        public static UserUnlockResponseVO.UserUnlockResponseVOBuilder validUnlockResponseVO() {
            return UserUnlockResponseVO.builder()
                    .userId(generateTestId())
                    .status("ACTIVE");
        }
        
        public static UserPermissionVO.UserPermissionVOBuilder validPermissionVO() {
            return UserPermissionVO.builder()
                    .id(generateTestId())
                    .resourceType("VM")
                    .resourceId(generateTestId())
                    .permission("READ")
                    .grantedAt(LocalDateTime.now())
                    .grantedBy(generateTestId())
                    .expiresAt(LocalDateTime.now().plusDays(30));
        }
        
        public static PermissionGrantResponseVO.PermissionGrantResponseVOBuilder validPermissionGrantResponseVO() {
            return PermissionGrantResponseVO.builder()
                    .permissionId(generateTestId())
                    .grantedAt(LocalDateTime.now());
        }
    }

    /**
     * 实体相关测试数据构建器
     */
    public static class Entities {
        
        public static UserPermission.UserPermissionBuilder validUserPermission() {
            return UserPermission.builder()
                    .id(generateTestId())
                    .userId(generateTestId())
                    .resourceType("VM")
                    .resourceId(generateTestId())
                    .permission("read")
                    .grantedAt(LocalDateTime.now())
                    .grantedBy(generateTestId())
                    .expiresAt(LocalDateTime.now().plusDays(30));
        }
        
        public static VmInstance.VmInstanceBuilder validVmInstance() {
            return VmInstance.builder()
                    .id(generateTestId())
                    .name("Test VM")
                    .status("RUNNING")
                    .ipAddress("192.168.1.10")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now());
        }
    }

    /**
     * 分页响应构建器
     */
    public static class Paging {
        
        public static <T> PageResponseDTO.PageResponseDTOBuilder<T> validPageResponse() {
            return PageResponseDTO.<T>builder()
                    .total(100L)
                    .page(1)
                    .size(10)
                    .pages(10L);
        }
    }

    /**
     * 通用工具方法
     */
    public static class Utils {
        
        public static String validToken() {
            return "Bearer test_token_" + System.currentTimeMillis();
        }
        
        public static String invalidToken() {
            return "Bearer invalid_token";
        }
        
        public static String expiredToken() {
            return "Bearer expired_token";
        }
        
        public static LocalDateTime futureTime(int hours) {
            return LocalDateTime.now().plusHours(hours);
        }
        
        public static LocalDateTime pastTime(int hours) {
            return LocalDateTime.now().minusHours(hours);
        }
    }

    /**
     * 生成测试用的ID
     * 使用UUID确保唯一性，但去掉连字符保持32位长度
     */
    private static String generateTestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}