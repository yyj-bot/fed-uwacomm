package com.feduwacomm.service.impl;

import com.feduwacomm.common.BaseContext;
import com.feduwacomm.dto.*;
import com.feduwacomm.entity.User;
import com.feduwacomm.exception.UserException;
import com.feduwacomm.mapper.UserMapper;
import com.feduwacomm.service.UserService;
import com.feduwacomm.utils.IpUtil;
import com.feduwacomm.utils.UserJwtUtil;
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
    private static final Logger securityLog = LoggerFactory.getLogger("SECURITY_AUDIT");
    private static final Logger performanceLog = LoggerFactory.getLogger("PERFORMANCE");

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserJwtUtil userJwtUtil;

    @Autowired
    private UuidUtil uuidUtil;

    // 认证相关方法实现
    @Override
    @Transactional
    public UserRegisterResponseVO register(UserRegisterDTO registerDTO) {
        long startTime = System.currentTimeMillis();
        String clientIp = IpUtil.getClientIpAddress();
        
        log.info("开始用户注册流程: username={}, email={}, ip={}", 
            registerDTO.getUsername(), registerDTO.getEmail(), clientIp);
        
        try {
            log.info("开始验证注册参数");
            // 验证参数
            validateRegisterParams(registerDTO);
            log.info("注册参数验证通过");

            log.info("开始检查邮箱是否已存在: {}", registerDTO.getEmail());
            // 检查邮箱是否已存在
            if (emailExists(registerDTO.getEmail())) {
                log.warn("用户注册失败 - 邮箱已存在: email={}, ip={}", registerDTO.getEmail(), clientIp);
                throw UserException.emailExists();
            }
            log.info("邮箱检查通过，不存在重复");

            log.info("开始检查用户名是否已存在: {}", registerDTO.getUsername());
            // 检查用户名是否已存在
            if (usernameExists(registerDTO.getUsername())) {
                log.warn("用户注册失败 - 用户名已存在: username={}, ip={}", registerDTO.getUsername(), clientIp);
                throw UserException.usernameExists();
            }
            log.info("用户名检查通过，不存在重复");

            log.info("开始创建用户对象");
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
            log.info("用户对象创建完成，userId: {}", user.getId());

            log.info("开始插入用户到数据库");
            userMapper.insert(user);
            log.info("用户数据库插入成功");
            
            log.info("用户注册成功: userId={}, username={}, email={}, role={}, ip={}", 
                user.getId(), user.getUsername(), user.getEmail(), user.getRole(), clientIp);
            
            performanceLog.info("用户注册完成: duration={}ms, username={}", 
                System.currentTimeMillis() - startTime, user.getUsername());

            log.info("开始构建响应对象");
            UserRegisterResponseVO response = UserRegisterResponseVO.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .status(user.getStatus())
                    .createdAt(user.getCreatedAt())
                    .build();
            log.info("响应对象构建完成，准备返回");
            
            return response;
                    
        } catch (Exception e) {
            log.error("用户注册异常: username={}, email={}, ip={}, error={}", 
                registerDTO.getUsername(), registerDTO.getEmail(), clientIp, e.getMessage());
            log.error("异常详情:", e);
            throw e;
        }
    }

    @Override
    public LoginResponseVO login(UserLoginDTO loginDTO) {
        long startTime = System.currentTimeMillis();
        String clientIp = IpUtil.getClientIpAddress();
        
        log.info("开始用户登录流程: identifier={}, ip={}", loginDTO.getLoginIdentifier(), clientIp);
        
        try {
            // 验证参数
            if (loginDTO.getLoginIdentifier() == null || loginDTO.getPassword() == null) {
                log.warn("用户登录失败 - 参数缺失: identifier={}, ip={}", loginDTO.getLoginIdentifier(), clientIp);
                throw UserException.paramValidationError("loginIdentifier", "登录标识符和密码不能为空");
            }

            // 获取用户
            User user = getUserByLoginIdentifier(loginDTO.getLoginIdentifier());
            if (user == null) {
                log.warn("用户登录失败 - 用户不存在: identifier={}, ip={}", loginDTO.getLoginIdentifier(), clientIp);
                throw UserException.passwordError();
            }

            // 检查用户状态
            if ("LOCKED".equals(user.getStatus())) {
                if (user.getLockedUntil() != null && LocalDateTime.now().isBefore(user.getLockedUntil())) {
                    log.warn("用户登录失败 - 账户已锁定: userId={}, username={}, ip={}, lockedUntil={}", 
                        user.getId(), user.getUsername(), clientIp, user.getLockedUntil());
                    throw UserException.accountLocked();
                } else {
                    // 锁定时间已过，解锁用户
                    log.info("用户账户自动解锁: userId={}, username={}, ip={}", 
                        user.getId(), user.getUsername(), clientIp);
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

                log.warn("用户登录失败 - 密码错误: userId={}, username={}, ip={}, attempts={}", 
                    user.getId(), user.getUsername(), clientIp, user.getLoginAttempts());

                // 如果失败次数达到5次，锁定账户
                if (user.getLoginAttempts() >= 5) {
                    user.setStatus("LOCKED");
                    user.setLockedUntil(LocalDateTime.now().plusHours(1));
                    userMapper.update(user);
                    
                    securityLog.error("用户账户因登录失败过多被锁定: userId={}, username={}, ip={}, attempts={}, lockedUntil={}", 
                        user.getId(), user.getUsername(), clientIp, user.getLoginAttempts(), user.getLockedUntil());
                    
                    throw UserException.accountLocked();
                }

                throw UserException.passwordError();
            }

            // 登录成功，重置失败次数
            user.setLoginAttempts(0);
            user.setLastLoginTime(LocalDateTime.now());
            user.setLastLoginIp(clientIp);
            userMapper.update(user);

            // 生成Token
            String token = userJwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
            String refreshToken = userJwtUtil.generateRefreshToken(user.getId());

            // 构建用户信息
            UserInfoVO userInfo = UserInfoVO.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .status(user.getStatus())
                    .lastLoginTime(LocalDateTime.now())
                    .lastLoginIp(clientIp)
                    .createdAt(user.getCreatedAt())
                    .updatedAt(user.getUpdatedAt())
                    .build();

            log.info("用户登录成功: userId={}, username={}, role={}, ip={}", 
                user.getId(), user.getUsername(), user.getRole(), clientIp);
            
            securityLog.info("用户登录成功: userId={}, username={}, role={}, ip={}, userAgent={}", 
                user.getId(), user.getUsername(), user.getRole(), clientIp, getCurrentUserAgent());
            
            performanceLog.info("用户登录完成: duration={}ms, username={}", 
                System.currentTimeMillis() - startTime, user.getUsername());

            return LoginResponseVO.builder()
                    .token(token)
                    .refreshToken(refreshToken)
                    .expiresIn(86400L) // 24小时
                    .user(userInfo)
                    .build();

        } catch (Exception e) {
            log.error("用户登录异常: identifier={}, ip={}, error={}", 
                loginDTO.getLoginIdentifier(), clientIp, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public TokenRefreshResponseVO refreshToken(String refreshToken) {
        long startTime = System.currentTimeMillis();
        String clientIp = IpUtil.getClientIpAddress();
        
        log.info("开始Token刷新流程: ip={}", clientIp);
        
        try {
            // 验证刷新Token
            var claims = userJwtUtil.validateToken(refreshToken);
            String userId = claims.get("userId", String.class);
            String type = claims.get("type", String.class);

            if (!"refresh".equals(type)) {
                log.warn("Token刷新失败 - 无效的Token类型: userId={}, type={}, ip={}", userId, type, clientIp);
                throw UserException.refreshTokenInvalid();
            }

            // 获取用户信息
            User user = getUserById(userId);
            if (user == null) {
                log.warn("Token刷新失败 - 用户不存在: userId={}, ip={}", userId, clientIp);
                throw UserException.userNotFound();
            }

            // 生成新的Token
            String newToken = userJwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
            String newRefreshToken = userJwtUtil.generateRefreshToken(user.getId());

            log.info("Token刷新成功: userId={}, username={}, ip={}", 
                user.getId(), user.getUsername(), clientIp);
            
            performanceLog.info("Token刷新完成: duration={}ms, userId={}", 
                System.currentTimeMillis() - startTime, userId);

            return TokenRefreshResponseVO.builder()
                    .token(newToken)
                    .refreshToken(newRefreshToken)
                    .expiresIn(86400L) // 24小时
                    .build();

        } catch (Exception e) {
            log.error("Token刷新异常: ip={}, error={}", clientIp, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void logout(String token) {
        String clientIp = IpUtil.getClientIpAddress();
        
        if (token != null && !token.trim().isEmpty()) {
            try {
                // 解析Token获取用户信息
                var claims = userJwtUtil.validateToken(token);
                String userId = claims.get("userId", String.class);
                String username = claims.get("username", String.class);
                
                log.info("用户登出: userId={}, username={}, ip={}", userId, username, clientIp);
                securityLog.info("用户登出: userId={}, username={}, ip={}, logoutTime={}", 
                    userId, username, clientIp, LocalDateTime.now());
                
                // 将Token加入黑名单（如果实现了Token黑名单功能）
                // jwtUtil.invalidateToken(token);
                
            } catch (Exception e) {
                log.warn("登出时Token解析失败: token={}, ip={}, error={}", 
                    token.substring(0, Math.min(20, token.length())), clientIp, e.getMessage());
            }
        } else {
            log.warn("登出时Token为空: ip={}", clientIp);
        }
    }

    // 用户信息相关方法实现
    @Override
    public UserInfoVO getCurrentUserInfo(String userId) {
        long startTime = System.currentTimeMillis();
        String clientIp = IpUtil.getClientIpAddress();
        
        log.debug("获取用户信息: userId={}, ip={}", userId, clientIp);
        
        try {
            User user = getUserById(userId);
            if (user == null) {
                log.warn("获取用户信息失败 - 用户不存在: userId={}, ip={}", userId, clientIp);
                throw UserException.userNotFound();
            }

            UserInfoVO userInfo = UserInfoVO.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .status(user.getStatus())
                    .lastLoginTime(user.getLastLoginTime())
                    .lastLoginIp(user.getLastLoginIp())
                    .createdAt(user.getCreatedAt())
                    .updatedAt(user.getUpdatedAt())
                    .build();

            performanceLog.debug("获取用户信息完成: duration={}ms, userId={}", 
                System.currentTimeMillis() - startTime, userId);

            return userInfo;
            
        } catch (Exception e) {
            log.error("获取用户信息异常: userId={}, ip={}, error={}", userId, clientIp, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public UserUpdateResponseVO updateUserInfo(String userId, UserUpdateDTO updateDTO) {
        long startTime = System.currentTimeMillis();
        String clientIp = IpUtil.getClientIpAddress();
        
        log.info("开始更新用户信息: userId={}, updates={}, ip={}", userId, updateDTO, clientIp);
        
        try {
            User user = getUserById(userId);
            if (user == null) {
                log.warn("更新用户信息失败 - 用户不存在: userId={}, ip={}", userId, clientIp);
                throw UserException.userNotFound();
            }

            // 检查用户名是否已被其他用户使用
            if (updateDTO.getUsername() != null && !updateDTO.getUsername().equals(user.getUsername())) {
                User existingUser = userMapper.selectByUsername(updateDTO.getUsername());
                if (existingUser != null && !existingUser.getId().equals(userId)) {
                    log.warn("更新用户信息失败 - 用户名已存在: userId={}, newUsername={}, ip={}", 
                        userId, updateDTO.getUsername(), clientIp);
                    throw UserException.usernameExists();
                }
            }

            // 检查邮箱是否已被其他用户使用
            if (updateDTO.getEmail() != null && !updateDTO.getEmail().equals(user.getEmail())) {
                User existingUser = userMapper.selectByEmail(updateDTO.getEmail());
                if (existingUser != null && !existingUser.getId().equals(userId)) {
                    log.warn("更新用户信息失败 - 邮箱已存在: userId={}, newEmail={}, ip={}", 
                        userId, updateDTO.getEmail(), clientIp);
                    throw UserException.emailExists();
                }
            }

            // 处理密码修改
            if (updateDTO.getNewPassword() != null) {
                // 验证旧密码
                if (updateDTO.getOldPassword() == null) {
                    log.warn("密码修改失败 - 缺少旧密码: userId={}, ip={}", userId, clientIp);
                    throw UserException.passwordError();
                }

                if (!PasswordUtil.matches(updateDTO.getOldPassword(), user.getPasswordHash())) {
                    log.warn("密码修改失败 - 旧密码错误: userId={}, ip={}", userId, clientIp);
                    throw UserException.passwordError();
                }

                // 验证新密码
                if (!PasswordUtil.isValidPassword(updateDTO.getNewPassword())) {
                    log.warn("密码修改失败 - 新密码格式无效: userId={}, ip={}", userId, clientIp);
                    throw UserException.passwordTooShort();
                }

                // 更新密码
                String newPasswordHash = PasswordUtil.encode(updateDTO.getNewPassword());
                userMapper.updatePassword(userId, newPasswordHash);
                
                log.info("用户密码修改成功: userId={}, username={}, ip={}", 
                    userId, user.getUsername(), clientIp);
                securityLog.info("用户密码修改: userId={}, username={}, ip={}, changeTime={}", 
                    userId, user.getUsername(), clientIp, LocalDateTime.now());
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

            UserUpdateResponseVO response = UserUpdateResponseVO.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .status(user.getStatus())
                    .updatedAt(user.getUpdatedAt())
                    .build();

            log.info("用户信息更新成功: userId={}, username={}, updates={}, ip={}", 
                userId, user.getUsername(), updateDTO, clientIp);
            
            performanceLog.info("用户信息更新完成: duration={}ms, userId={}", 
                System.currentTimeMillis() - startTime, userId);

            return response;
            
        } catch (Exception e) {
            log.error("更新用户信息异常: userId={}, ip={}, error={}", userId, clientIp, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void changePassword(String userId, PasswordChangeDTO passwordDTO) {
        long startTime = System.currentTimeMillis();
        String clientIp = IpUtil.getClientIpAddress();
        
        log.info("开始修改密码: userId={}, ip={}", userId, clientIp);
        
        try {
            User user = getUserById(userId);
            if (user == null) {
                log.warn("修改密码失败 - 用户不存在: userId={}, ip={}", userId, clientIp);
                throw UserException.userNotFound();
            }

            // 验证旧密码
            if (!PasswordUtil.matches(passwordDTO.getOldPassword(), user.getPasswordHash())) {
                log.warn("修改密码失败 - 旧密码错误: userId={}, username={}, ip={}", 
                    userId, user.getUsername(), clientIp);
                throw UserException.passwordError();
            }

            // 验证新密码
            if (!PasswordUtil.isValidPassword(passwordDTO.getNewPassword())) {
                log.warn("修改密码失败 - 新密码格式无效: userId={}, username={}, ip={}", 
                    userId, user.getUsername(), clientIp);
                throw UserException.passwordTooShort();
            }

            // 验证确认密码
            if (!passwordDTO.getNewPassword().equals(passwordDTO.getConfirmPassword())) {
                log.warn("修改密码失败 - 确认密码不匹配: userId={}, username={}, ip={}", 
                    userId, user.getUsername(), clientIp);
                throw UserException.passwordMismatch();
            }

            // 更新密码
            String newPasswordHash = PasswordUtil.encode(passwordDTO.getNewPassword());
            userMapper.updatePassword(userId, newPasswordHash);
            
            log.info("用户密码修改成功: userId={}, username={}, ip={}", 
                userId, user.getUsername(), clientIp);
            securityLog.info("用户密码修改: userId={}, username={}, ip={}, changeTime={}", 
                userId, user.getUsername(), clientIp, LocalDateTime.now());
            
            performanceLog.info("密码修改完成: duration={}ms, userId={}", 
                System.currentTimeMillis() - startTime, userId);
                
        } catch (Exception e) {
            log.error("修改密码异常: userId={}, ip={}, error={}", userId, clientIp, e.getMessage(), e);
            throw e;
        }
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

    /**
     * 获取当前用户的User-Agent信息
     */
    private String getCurrentUserAgent() {
        try {
            // 这里可以通过HttpServletRequest获取User-Agent
            // 暂时返回默认值
            return "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }
}