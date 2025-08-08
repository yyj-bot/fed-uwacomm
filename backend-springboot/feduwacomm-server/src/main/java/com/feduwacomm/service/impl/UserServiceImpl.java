package com.feduwacomm.service.impl;

import com.feduwacomm.common.BaseContext;
import com.feduwacomm.dto.*;
import com.feduwacomm.entity.User;
import com.feduwacomm.exception.UserException;
import com.feduwacomm.mapper.UserMapper;
import com.feduwacomm.service.UserService;
import com.feduwacomm.utils.IpUtil;
import com.feduwacomm.utils.JwtUtil;
import com.feduwacomm.utils.PasswordUtil;
import com.feduwacomm.utils.UuidUtil;
import com.feduwacomm.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户服务实现类
 * 提供用户自助操作功能
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UuidUtil uuidUtil;

    // 认证相关方法实现
    @Override
    @Transactional
    public UserRegisterResponseVO register(UserRegisterDTO registerDTO) {
        // 验证参数
        validateRegisterParams(registerDTO);

        // 检查邮箱是否已存在
        if (emailExists(registerDTO.getEmail())) {
            throw UserException.emailExists();
        }

        // 检查用户名是否已存在
        if (usernameExists(registerDTO.getUsername())) {
            throw UserException.usernameExists();
        }

        // 创建用户
        User user = User.builder()
                .id(uuidUtil.generateUuid())
                .username(registerDTO.getUsername())
                .email(registerDTO.getEmail())
                .passwordHash(PasswordUtil.encode(registerDTO.getPassword()))
                .role("VIEWER")
                .status("ACTIVE")
                .loginAttempts(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userMapper.insert(user);

        return UserRegisterResponseVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    public LoginResponseVO login(UserLoginDTO loginDTO) {
        // 验证参数
        if (loginDTO.getLoginIdentifier() == null || loginDTO.getPassword() == null) {
            throw UserException.paramValidationError("loginIdentifier", "登录标识符和密码不能为空");
        }

        // 获取用户
        User user = getUserByLoginIdentifier(loginDTO.getLoginIdentifier());
        if (user == null) {
            throw UserException.passwordError();
        }

        // 检查用户状态
        if ("LOCKED".equals(user.getStatus())) {
            if (user.getLockedUntil() != null && LocalDateTime.now().isBefore(user.getLockedUntil())) {
                throw UserException.accountLocked();
            } else {
                // 锁定时间已过，解锁用户
                user.setStatus("ACTIVE");
                user.setLockedUntil(null);
                user.setLoginAttempts(0);
                userMapper.update(user);
            }
        }

        // 验证密码
        if (!PasswordUtil.matches(loginDTO.getPassword(), user.getPasswordHash())) {
            // 增加登录失败次数
            user.setLoginAttempts(user.getLoginAttempts() + 1);
            userMapper.update(user);

            // 如果失败次数达到5次，锁定账户
            if (user.getLoginAttempts() >= 5) {
                user.setStatus("LOCKED");
                user.setLockedUntil(LocalDateTime.now().plusHours(1));
                userMapper.update(user);
                throw UserException.accountLocked();
            }

            throw UserException.passwordError();
        }

        // 登录成功，重置失败次数
        user.setLoginAttempts(0);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(IpUtil.getClientIpAddress());
        userMapper.update(user);

        // 生成Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        // 构建用户信息
        UserInfoVO userInfo = UserInfoVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .lastLoginTime(LocalDateTime.now())
                .lastLoginIp(IpUtil.getClientIpAddress())
                .build();

        return LoginResponseVO.builder()
                .token(token)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.getTokenExpireTime())
                .user(userInfo)
                .build();
    }

    @Override
    public TokenRefreshResponseVO refreshToken(String refreshToken) {
        try {
            // 验证刷新Token
            var claims = jwtUtil.validateToken(refreshToken);
            String userId = claims.get("userId", String.class);
            String type = claims.get("type", String.class);

            if (!"refresh".equals(type)) {
                throw UserException.refreshTokenInvalid();
            }

            // 获取用户信息
            User user = getUserById(userId);
            if (user == null) {
                throw UserException.userNotFound();
            }

            // 生成新的Token
            String newToken = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
            String newRefreshToken = jwtUtil.generateRefreshToken(user.getId());

            return TokenRefreshResponseVO.builder()
                    .token(newToken)
                    .refreshToken(newRefreshToken)
                    .expiresIn(jwtUtil.getTokenExpireTime())
                    .build();

        } catch (Exception e) {
            throw UserException.refreshTokenInvalid();
        }
    }

    @Override
    public void logout(String token) {
        if (token != null && !token.trim().isEmpty()) {
            // 将Token加入黑名单（如果实现了Token黑名单功能）
            // jwtUtil.invalidateToken(token);
            log.info("用户登出，Token: {}", token);
        }
    }

    // 用户信息相关方法实现
    @Override
    public UserInfoVO getCurrentUserInfo(String userId) {
        User user = getUserById(userId);
        if (user == null) {
            throw UserException.userNotFound();
        }

        return UserInfoVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .lastLoginTime(user.getLastLoginTime())
                .lastLoginIp(user.getLastLoginIp())
                .build();
    }

    @Override
    @Transactional
    public UserUpdateResponseVO updateUserInfo(String userId, UserUpdateDTO updateDTO) {
        User user = getUserById(userId);
        if (user == null) {
            throw UserException.userNotFound();
        }

        // 检查用户名是否已被其他用户使用
        if (updateDTO.getUsername() != null && !updateDTO.getUsername().equals(user.getUsername())) {
            User existingUser = userMapper.selectByUsername(updateDTO.getUsername());
            if (existingUser != null && !existingUser.getId().equals(userId)) {
                throw UserException.usernameExists();
            }
        }

        // 检查邮箱是否已被其他用户使用
        if (updateDTO.getEmail() != null && !updateDTO.getEmail().equals(user.getEmail())) {
            User existingUser = userMapper.selectByEmail(updateDTO.getEmail());
            if (existingUser != null && !existingUser.getId().equals(userId)) {
                throw UserException.emailExists();
            }
        }

        // 处理密码修改
        if (updateDTO.getNewPassword() != null) {
            // 验证旧密码
            if (updateDTO.getOldPassword() == null) {
                throw UserException.passwordError();
            }

            if (!PasswordUtil.matches(updateDTO.getOldPassword(), user.getPasswordHash())) {
                throw UserException.passwordError();
            }

            // 验证新密码
            if (!PasswordUtil.isValidPassword(updateDTO.getNewPassword())) {
                throw UserException.passwordTooShort();
            }

            // 更新密码
            String newPasswordHash = PasswordUtil.encode(updateDTO.getNewPassword());
            userMapper.updatePassword(userId, newPasswordHash);
        }

        // 更新用户信息
        if (updateDTO.getUsername() != null) {
            user.setUsername(updateDTO.getUsername());
        }
        if (updateDTO.getEmail() != null) {
            user.setEmail(updateDTO.getEmail());
        }
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(userId);

        userMapper.update(user);

        return UserUpdateResponseVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public void changePassword(String userId, PasswordChangeDTO passwordDTO) {
        User user = getUserById(userId);
        if (user == null) {
            throw UserException.userNotFound();
        }

        // 验证旧密码
        if (!PasswordUtil.matches(passwordDTO.getOldPassword(), user.getPasswordHash())) {
            throw UserException.passwordError();
        }

        // 验证新密码
        if (!PasswordUtil.isValidPassword(passwordDTO.getNewPassword())) {
            throw UserException.passwordTooShort();
        }

        // 验证确认密码
        if (!passwordDTO.getNewPassword().equals(passwordDTO.getConfirmPassword())) {
            throw UserException.passwordMismatch();
        }

        // 更新密码
        String newPasswordHash = PasswordUtil.encode(passwordDTO.getNewPassword());
        userMapper.updatePassword(userId, newPasswordHash);
    }

    // 内部方法实现
    @Override
    public User getUserById(String userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public User getUserByLoginIdentifier(String loginIdentifier) {
        // 先尝试按用户名查找
        User user = userMapper.selectByUsername(loginIdentifier);
        if (user != null) {
            return user;
        }

        // 再尝试按邮箱查找
        return userMapper.selectByEmail(loginIdentifier);
    }

    @Override
    public boolean userExists(String userId) {
        return getUserById(userId) != null;
    }

    @Override
    public boolean loginIdentifierExists(String loginIdentifier) {
        return getUserByLoginIdentifier(loginIdentifier) != null;
    }

    @Override
    public boolean emailExists(String email) {
        return userMapper.selectByEmail(email) != null;
    }

    @Override
    public boolean usernameExists(String username) {
        return userMapper.selectByUsername(username) != null;
    }

    // 私有辅助方法
    private void validateRegisterParams(UserRegisterDTO registerDTO) {
        if (registerDTO.getUsername() == null || registerDTO.getUsername().trim().isEmpty()) {
            throw UserException.paramValidationError("username", "用户名不能为空");
        }
        if (registerDTO.getEmail() == null || registerDTO.getEmail().trim().isEmpty()) {
            throw UserException.paramValidationError("email", "邮箱不能为空");
        }
        if (registerDTO.getPassword() == null || !PasswordUtil.isValidPassword(registerDTO.getPassword())) {
            throw UserException.passwordTooShort();
        }
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw UserException.passwordMismatch();
        }
    }
}