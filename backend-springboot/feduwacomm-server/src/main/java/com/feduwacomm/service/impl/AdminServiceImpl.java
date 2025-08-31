package com.feduwacomm.service.impl;

import com.feduwacomm.common.BaseContext;
import com.feduwacomm.dto.*;
import com.feduwacomm.entity.User;
import com.feduwacomm.entity.UserPermission;
import com.feduwacomm.exception.UserException;
import com.feduwacomm.mapper.AdminMapper;
import com.feduwacomm.mapper.UserPermissionMapper;
import com.feduwacomm.service.AdminService;
import com.feduwacomm.utils.PasswordUtil;
import com.feduwacomm.utils.UuidUtil;
import com.feduwacomm.utils.AdminLogUtil;
import com.feduwacomm.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员服务实现类
 */
@Service
public class AdminServiceImpl implements AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private UserPermissionMapper userPermissionMapper;

    @Autowired
    private UuidUtil uuidUtil;

    @Override
    public PageResponseDTO<UserListVO> getUserList(UserQueryDTO queryDTO) {
        log.info("执行用户列表查询 - 页码: {}, 大小: {}, 角色: {}, 状态: {}, 关键词: {}", 
                queryDTO.getPage(), queryDTO.getSize(), queryDTO.getRole(), queryDTO.getStatus(), queryDTO.getKeyword());
        
        // 设置默认值
        int page = queryDTO.getPage() != null ? queryDTO.getPage() : 1;
        int size = queryDTO.getSize() != null ? queryDTO.getSize() : 10;
        if (size > 100) {
            size = 100; // 限制最大页面大小
            log.warn("用户列表查询大小超过限制，已调整为100");
        }

        int offset = (page - 1) * size;

        // 查询用户列表
        List<User> users = adminMapper.selectByCondition(
                queryDTO.getRole(),
                queryDTO.getStatus(),
                queryDTO.getKeyword(),
                offset,
                size);

        // 查询总数
        int total = adminMapper.countByCondition(
                queryDTO.getRole(),
                queryDTO.getStatus(),
                queryDTO.getKeyword());

        // 转换为VO
        List<UserListVO> userList = users.stream()
                .map(this::convertToUserListVO)
                .collect(Collectors.toList());

        log.info("用户列表查询完成 - 总数: {}, 当前页数量: {}, 页码: {}", total, userList.size(), page);

        return PageResponseDTO.<UserListVO>builder()
                .total((long) total)
                .page(page)
                .size(size)
                .list(userList) // 使用list字段名
                .build();
    }

    @Override
    public UserDetailVO getUserDetail(String userId) {
        log.info("执行用户详情查询 - 用户ID: {}", userId);
        
        User user = getUserById(userId);
        if (user == null) {
            log.warn("用户不存在 - 用户ID: {}", userId);
            throw UserException.userNotFound();
        }

        log.info("用户详情查询成功 - 用户ID: {}, 用户名: {}, 角色: {}, 状态: {}", 
                userId, user.getUsername(), user.getRole(), user.getStatus());

        return UserDetailVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .lastLoginTime(user.getLastLoginTime())
                .lastLoginIp(user.getLastLoginIp())
                .loginAttempts(user.getLoginAttempts())
                .lockedUntil(user.getLockedUntil())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .createdBy(user.getCreatedBy())
                .updatedBy(user.getUpdatedBy())
                .build();
    }

    @Override
    @Transactional
    public UserCreateResponseVO createUser(UserCreateDTO createDTO) {
        log.info("执行用户创建 - 用户名: {}, 邮箱: {}, 角色: {}", 
                createDTO.getUsername(), createDTO.getEmail(), createDTO.getRole());
        
        // 验证参数
        validateCreateUserParams(createDTO);

        // 检查用户名是否已存在
        if (usernameExists(createDTO.getUsername())) {
            log.warn("用户名已存在 - 用户名: {}", createDTO.getUsername());
            throw UserException.usernameExists();
        }

        // 检查邮箱是否已存在
        if (emailExists(createDTO.getEmail())) {
            log.warn("邮箱已存在 - 邮箱: {}", createDTO.getEmail());
            throw UserException.emailExists();
        }

        // 创建用户
        User user = User.builder()
                .id(uuidUtil.generateUuid())
                .username(createDTO.getUsername())
                .email(createDTO.getEmail())
                .passwordHash(PasswordUtil.encode(createDTO.getPassword()))
                .role(createDTO.getRole() != null ? createDTO.getRole() : "VIEWER")
                .status("ACTIVE") // 默认状态
                .loginAttempts(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdBy(BaseContext.getCurrentUserId())
                .build();

        adminMapper.insert(user);
        log.info("用户创建成功 - 用户ID: {}, 用户名: {}, 角色: {}", user.getId(), user.getUsername(), user.getRole());

        // 记录安全审计日志
        AdminLogUtil.logUserCreation(
                BaseContext.getCurrentUserId(),
                BaseContext.getUsername(),
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );

        return UserCreateResponseVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public UserUpdateResponseVO updateUser(String userId, UserAdminUpdateDTO updateDTO) {
        log.info("执行用户更新 - 用户ID: {}, 更新字段: {}", userId, getUpdateFields(updateDTO));
        
        User user = getUserById(userId);
        if (user == null) {
            log.warn("用户不存在 - 用户ID: {}", userId);
            throw UserException.userNotFound();
        }

        // 检查用户名是否已被其他用户使用
        if (updateDTO.getUsername() != null && !updateDTO.getUsername().equals(user.getUsername())) {
            User existingUser = adminMapper.selectByUsername(updateDTO.getUsername());
            if (existingUser != null && !existingUser.getId().equals(userId)) {
                log.warn("用户名已被其他用户使用 - 用户名: {}, 当前用户ID: {}", updateDTO.getUsername(), userId);
                throw UserException.usernameExists();
            }
        }

        // 检查邮箱是否已被其他用户使用
        if (updateDTO.getEmail() != null && !updateDTO.getEmail().equals(user.getEmail())) {
            User existingUser = adminMapper.selectByEmail(updateDTO.getEmail());
            if (existingUser != null && !existingUser.getId().equals(userId)) {
                log.warn("邮箱已被其他用户使用 - 邮箱: {}, 当前用户ID: {}", updateDTO.getEmail(), userId);
                throw UserException.emailExists();
            }
        }

        // 处理密码修改（管理员可直接修改密码）
        if (updateDTO.getPassword() != null) {
            log.info("管理员修改用户密码 - 用户ID: {}", userId);
            // 验证新密码
            if (!PasswordUtil.isValidPassword(updateDTO.getPassword())) {
                log.warn("密码格式无效 - 用户ID: {}", userId);
                throw UserException.passwordTooShort();
            }

            // 更新密码
            String newPasswordHash = PasswordUtil.encode(updateDTO.getPassword());
            adminMapper.updatePassword(userId, newPasswordHash);
        }

        // 更新用户信息
        if (updateDTO.getUsername() != null) {
            user.setUsername(updateDTO.getUsername());
        }
        if (updateDTO.getEmail() != null) {
            user.setEmail(updateDTO.getEmail());
        }
        if (updateDTO.getRole() != null) {
            log.info("管理员修改用户角色 - 用户ID: {}, 旧角色: {}, 新角色: {}", 
                    userId, user.getRole(), updateDTO.getRole());
            user.setRole(updateDTO.getRole());
        }
        if (updateDTO.getStatus() != null) {
            log.info("管理员修改用户状态 - 用户ID: {}, 旧状态: {}, 新状态: {}", 
                    userId, user.getStatus(), updateDTO.getStatus());
            user.setStatus(updateDTO.getStatus());
        }
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(BaseContext.getCurrentUserId());

        adminMapper.update(user);
        log.info("用户更新成功 - 用户ID: {}, 用户名: {}, 角色: {}, 状态: {}", 
                userId, user.getUsername(), user.getRole(), user.getStatus());

        return UserUpdateResponseVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public void deleteUser(String userId) {
        log.warn("执行用户删除 - 用户ID: {}", userId);
        
        User user = getUserById(userId);
        if (user == null) {
            log.warn("用户不存在 - 用户ID: {}", userId);
            throw UserException.userNotFound();
        }

        // 不能删除管理员用户
        if ("ADMIN".equals(user.getRole())) {
            log.error("尝试删除管理员用户被拒绝 - 用户ID: {}, 用户名: {}", userId, user.getUsername());
            throw UserException.permissionDenied();
        }

        log.info("删除用户信息 - 用户ID: {}, 用户名: {}, 角色: {}, 状态: {}", 
                userId, user.getUsername(), user.getRole(), user.getStatus());

        // 删除用户权限
        int permissionCount = userPermissionMapper.deleteByUserId(userId);
        log.info("删除用户权限 - 用户ID: {}, 权限数量: {}", userId, permissionCount);

        // 删除用户
        adminMapper.deleteById(userId);
        log.warn("用户删除完成 - 用户ID: {}, 用户名: {}", userId, user.getUsername());
    }

    @Override
    @Transactional
    public UserLockResponseVO lockUser(String userId, UserLockDTO lockDTO) {
        log.warn("执行用户锁定 - 用户ID: {}, 锁定时长: {}秒", userId, lockDTO.getDuration());
        
        User user = getUserById(userId);
        if (user == null) {
            log.warn("用户不存在 - 用户ID: {}", userId);
            throw UserException.userNotFound();
        }

        // 不能锁定管理员用户
        if ("ADMIN".equals(user.getRole())) {
            log.error("尝试锁定管理员用户被拒绝 - 用户ID: {}, 用户名: {}", userId, user.getUsername());
            throw UserException.permissionDenied();
        }

        // 设置锁定时长
        int duration = lockDTO.getDuration() != null ? lockDTO.getDuration() : 3600; // 默认1小时
        LocalDateTime lockedUntil = LocalDateTime.now().plusSeconds(duration);

        log.info("锁定用户账户 - 用户ID: {}, 用户名: {}, 旧状态: {}, 锁定时长: {}秒, 锁定截止时间: {}", 
                userId, user.getUsername(), user.getStatus(), duration, lockedUntil);

        // 更新用户状态
        user.setStatus("LOCKED");
        user.setLockedUntil(lockedUntil);
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(BaseContext.getCurrentUserId());

        adminMapper.update(user);
        log.warn("用户锁定完成 - 用户ID: {}, 用户名: {}, 锁定截止时间: {}", userId, user.getUsername(), lockedUntil);

        return UserLockResponseVO.builder()
                .userId(user.getId())
                .lockedUntil(lockedUntil)
                .build();
    }

    @Override
    @Transactional
    public UserUnlockResponseVO unlockUser(String userId) {
        log.info("执行用户解锁 - 用户ID: {}", userId);
        
        User user = getUserById(userId);
        if (user == null) {
            log.warn("用户不存在 - 用户ID: {}", userId);
            throw UserException.userNotFound();
        }

        log.info("解锁用户账户 - 用户ID: {}, 用户名: {}, 旧状态: {}, 旧锁定时间: {}", 
                userId, user.getUsername(), user.getStatus(), user.getLockedUntil());

        // 更新用户状态
        user.setStatus("ACTIVE");
        user.setLockedUntil(null);
        user.setLoginAttempts(0); // 重置登录失败次数
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(BaseContext.getCurrentUserId());

        adminMapper.update(user);
        log.info("用户解锁完成 - 用户ID: {}, 用户名: {}, 新状态: {}", userId, user.getUsername(), user.getStatus());

        return UserUnlockResponseVO.builder()
                .userId(user.getId())
                .status("ACTIVE")
                .build();
    }

    @Override
    @Transactional
    public void resetPassword(String userId, PasswordResetDTO resetDTO) {
        log.warn("执行密码重置 - 用户ID: {}", userId);
        
        User user = getUserById(userId);
        if (user == null) {
            log.warn("用户不存在 - 用户ID: {}", userId);
            throw UserException.userNotFound();
        }

        // 验证新密码
        if (!PasswordUtil.isValidPassword(resetDTO.getNewPassword())) {
            log.warn("密码格式无效 - 用户ID: {}", userId);
            throw UserException.passwordTooShort();
        }

        log.info("管理员重置用户密码 - 用户ID: {}, 用户名: {}", userId, user.getUsername());

        // 更新密码
        String newPasswordHash = PasswordUtil.encode(resetDTO.getNewPassword());
        adminMapper.updatePassword(userId, newPasswordHash);
        log.warn("密码重置完成 - 用户ID: {}, 用户名: {}", userId, user.getUsername());
    }

    @Override
    public List<UserPermissionVO> getUserPermissions(String userId) {
        log.info("执行用户权限查询 - 用户ID: {}", userId);
        
        User user = getUserById(userId);
        if (user == null) {
            log.warn("用户不存在 - 用户ID: {}", userId);
            throw UserException.userNotFound();
        }

        List<UserPermission> permissions = userPermissionMapper.selectByUserId(userId);
        log.info("用户权限查询完成 - 用户ID: {}, 用户名: {}, 权限数量: {}", 
                userId, user.getUsername(), permissions.size());

        return permissions.stream()
                .map(this::convertToUserPermissionVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PermissionGrantResponseVO grantPermission(String userId, PermissionGrantDTO grantDTO) {
        log.info("执行权限授予 - 用户ID: {}, 权限: {}", userId, grantDTO.getPermission());
        
        User user = getUserById(userId);
        if (user == null) {
            log.warn("用户不存在 - 用户ID: {}", userId);
            throw UserException.userNotFound();
        }

        // 检查权限是否已存在
        UserPermission existingPermission = userPermissionMapper.selectByUserIdAndResource(
                userId, "SYSTEM", null);

        if (existingPermission != null) {
            log.warn("权限已存在 - 用户ID: {}, 权限: {}, 权限ID: {}", 
                    userId, grantDTO.getPermission(), existingPermission.getId());
            throw UserException.permissionDenied();
        }

        // 创建权限
        UserPermission permission = UserPermission.builder()
                .id(uuidUtil.generateUuid())
                .userId(userId)
                .resourceType("SYSTEM") // 默认系统权限
                .resourceId(null) // 系统级权限
                .permission(grantDTO.getPermission())
                .grantedAt(LocalDateTime.now())
                .grantedBy(BaseContext.getCurrentUserId())
                .expiresAt(grantDTO.getExpiresAt())
                .build();

        userPermissionMapper.insert(permission);
        log.info("权限授予成功 - 用户ID: {}, 用户名: {}, 权限: {}, 权限ID: {}", 
                userId, user.getUsername(), grantDTO.getPermission(), permission.getId());

        return PermissionGrantResponseVO.builder()
                .permissionId(permission.getId())
                .grantedAt(permission.getGrantedAt())
                .build();
    }

    @Override
    @Transactional
    public void revokePermission(String userId, String permissionId) {
        log.warn("执行权限撤销 - 用户ID: {}, 权限ID: {}", userId, permissionId);
        
        User user = getUserById(userId);
        if (user == null) {
            log.warn("用户不存在 - 用户ID: {}", userId);
            throw UserException.userNotFound();
        }

        UserPermission permission = userPermissionMapper.selectById(permissionId);
        if (permission == null || !permission.getUserId().equals(userId)) {
            log.warn("权限不存在或不属于该用户 - 用户ID: {}, 权限ID: {}", userId, permissionId);
            throw UserException.permissionDenied();
        }

        log.info("撤销用户权限 - 用户ID: {}, 用户名: {}, 权限: {}, 权限ID: {}", 
                userId, user.getUsername(), permission.getPermission(), permissionId);

        userPermissionMapper.deleteById(permissionId);
        log.warn("权限撤销完成 - 用户ID: {}, 用户名: {}, 权限: {}, 权限ID: {}", 
                userId, user.getUsername(), permission.getPermission(), permissionId);
    }

    // 私有辅助方法
    private User getUserById(String userId) {
        return adminMapper.selectById(userId);
    }

    private boolean usernameExists(String username) {
        return adminMapper.selectByUsername(username) != null;
    }

    private boolean emailExists(String email) {
        return adminMapper.selectByEmail(email) != null;
    }

    private void validateCreateUserParams(UserCreateDTO createDTO) {
        if (createDTO.getUsername() == null || createDTO.getUsername().trim().isEmpty()) {
            throw UserException.paramValidationError("username", "用户名不能为空");
        }
        if (createDTO.getEmail() == null || createDTO.getEmail().trim().isEmpty()) {
            throw UserException.paramValidationError("email", "邮箱不能为空");
        }
        if (createDTO.getPassword() == null || !PasswordUtil.isValidPassword(createDTO.getPassword())) {
            throw UserException.passwordTooShort();
        }
        if (createDTO.getRole() != null && !isValidRole(createDTO.getRole())) {
            throw UserException.paramValidationError("role", "无效的用户角色");
        }
    }

    private boolean isValidRole(String role) {
        return "ADMIN".equals(role) || "RESEARCHER".equals(role) || 
               "OPERATOR".equals(role) || "VIEWER".equals(role);
    }

    private boolean isValidStatus(String status) {
        return "ACTIVE".equals(status) || "INACTIVE".equals(status) || 
               "LOCKED".equals(status) || "DELETED".equals(status);
    }

    private String getPermissionDescription(String permissionName) {
        // 权限描述映射
        switch (permissionName) {
            case "READ_DATA": return "读取数据权限";
            case "WRITE_DATA": return "写入数据权限";
            case "DELETE_DATA": return "删除数据权限";
            case "EXECUTE_TASK": return "执行任务权限";
            case "ADMIN_SYSTEM": return "系统管理权限";
            default: return "自定义权限";
        }
    }

    private UserListVO convertToUserListVO(User user) {
        return UserListVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .lastLoginTime(user.getLastLoginTime())
                .lastLoginIp(user.getLastLoginIp())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private UserPermissionVO convertToUserPermissionVO(UserPermission permission) {
        return UserPermissionVO.builder()
                .id(permission.getId())
                .userId(permission.getUserId())
                .resourceType(permission.getResourceType())
                .resourceId(permission.getResourceId())
                .permission(permission.getPermission())
                .grantedAt(permission.getGrantedAt())
                .grantedBy(permission.getGrantedBy())
                .expiresAt(permission.getExpiresAt())
                .build();
    }

    /**
     * 获取更新字段信息
     */
    private String getUpdateFields(UserAdminUpdateDTO updateDTO) {
        StringBuilder fields = new StringBuilder();
        if (updateDTO.getUsername() != null) fields.append("用户名 ");
        if (updateDTO.getEmail() != null) fields.append("邮箱 ");
        if (updateDTO.getRole() != null) fields.append("角色 ");
        if (updateDTO.getStatus() != null) fields.append("状态 ");
        if (updateDTO.getPassword() != null) fields.append("密码 ");
        return fields.toString().trim();
    }
}