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
        // 设置默认值
        int page = queryDTO.getPage() != null ? queryDTO.getPage() : 1;
        int size = queryDTO.getSize() != null ? queryDTO.getSize() : 10;
        if (size > 100)
            size = 100;

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

        return PageResponseDTO.<UserListVO>builder()
                .total((long) total)
                .page(page)
                .size(size)
                .list(userList)
                .build();
    }

    @Override
    public UserDetailVO getUserDetail(String userId) {
        User user = getUserById(userId);
        if (user == null) {
            throw UserException.userNotFound();
        }

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
        // 验证参数
        validateCreateUserParams(createDTO);

        // 检查邮箱是否已存在
        if (emailExists(createDTO.getEmail())) {
            throw UserException.emailExists();
        }

        // 创建用户
        User user = User.builder()
                .id(uuidUtil.generateUuid())
                .username(createDTO.getUsername())
                .email(createDTO.getEmail())
                .passwordHash(PasswordUtil.encode(createDTO.getPassword()))
                .role(createDTO.getRole() != null ? createDTO.getRole() : "VIEWER")
                .status("ACTIVE")
                .loginAttempts(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdBy(BaseContext.getCurrentUserId())
                .build();

        adminMapper.insert(user);

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
        User user = getUserById(userId);
        if (user == null) {
            throw UserException.userNotFound();
        }

        // 检查用户名是否已被其他用户使用
        if (updateDTO.getUsername() != null && !updateDTO.getUsername().equals(user.getUsername())) {
            User existingUser = adminMapper.selectByUsername(updateDTO.getUsername());
            if (existingUser != null && !existingUser.getId().equals(userId)) {
                throw UserException.usernameExists();
            }
        }

        // 检查邮箱是否已被其他用户使用
        if (updateDTO.getEmail() != null && !updateDTO.getEmail().equals(user.getEmail())) {
            User existingUser = adminMapper.selectByEmail(updateDTO.getEmail());
            if (existingUser != null && !existingUser.getId().equals(userId)) {
                throw UserException.emailExists();
            }
        }

        // 处理密码修改（管理员可直接修改密码）
        if (updateDTO.getPassword() != null) {
            // 验证新密码
            if (!PasswordUtil.isValidPassword(updateDTO.getPassword())) {
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
            user.setRole(updateDTO.getRole());
        }
        if (updateDTO.getStatus() != null) {
            user.setStatus(updateDTO.getStatus());
        }
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(BaseContext.getCurrentUserId());

        adminMapper.update(user);

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
        User user = getUserById(userId);
        if (user == null) {
            throw UserException.userNotFound();
        }

        // 删除用户权限
        userPermissionMapper.deleteByUserId(userId);

        // 删除用户
        adminMapper.deleteById(userId);
    }

    @Override
    @Transactional
    public UserLockResponseVO lockUser(String userId, UserLockDTO lockDTO) {
        User user = getUserById(userId);
        if (user == null) {
            throw UserException.userNotFound();
        }

        // 设置锁定时长
        int duration = lockDTO.getDuration() != null ? lockDTO.getDuration() : 3600; // 默认1小时
        LocalDateTime lockedUntil = LocalDateTime.now().plusSeconds(duration);

        // 更新用户状态
        user.setStatus("LOCKED");
        user.setLockedUntil(lockedUntil);
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(BaseContext.getCurrentUserId());

        adminMapper.update(user);

        return UserLockResponseVO.builder()
                .userId(user.getId())
                .lockedUntil(lockedUntil)
                .build();
    }

    @Override
    @Transactional
    public UserUnlockResponseVO unlockUser(String userId) {
        User user = getUserById(userId);
        if (user == null) {
            throw UserException.userNotFound();
        }

        // 更新用户状态
        user.setStatus("ACTIVE");
        user.setLockedUntil(null);
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(BaseContext.getCurrentUserId());

        adminMapper.update(user);

        return UserUnlockResponseVO.builder()
                .userId(user.getId())
                .status("ACTIVE")
                .build();
    }

    @Override
    @Transactional
    public void resetPassword(String userId, PasswordResetDTO resetDTO) {
        User user = getUserById(userId);
        if (user == null) {
            throw UserException.userNotFound();
        }

        // 验证新密码
        if (!PasswordUtil.isValidPassword(resetDTO.getNewPassword())) {
            throw UserException.passwordTooShort();
        }

        // 更新密码
        String newPasswordHash = PasswordUtil.encode(resetDTO.getNewPassword());
        adminMapper.updatePassword(userId, newPasswordHash);
    }

    @Override
    public List<UserPermissionVO> getUserPermissions(String userId) {
        User user = getUserById(userId);
        if (user == null) {
            throw UserException.userNotFound();
        }

        List<UserPermission> permissions = userPermissionMapper.selectByUserId(userId);
        return permissions.stream()
                .map(this::convertToUserPermissionVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PermissionGrantResponseVO grantPermission(String userId, PermissionGrantDTO grantDTO) {
        User user = getUserById(userId);
        if (user == null) {
            throw UserException.userNotFound();
        }

        // 检查权限是否已存在
        UserPermission existingPermission = userPermissionMapper.selectByUserIdAndResource(
                userId, grantDTO.getResourceType(), grantDTO.getResourceId());

        if (existingPermission != null) {
            throw UserException.permissionDenied();
        }

        // 创建权限
        UserPermission permission = UserPermission.builder()
                .id(uuidUtil.generateUuid())
                .userId(userId)
                .resourceType(grantDTO.getResourceType())
                .resourceId(grantDTO.getResourceId())
                .permission(grantDTO.getPermission())
                .grantedAt(LocalDateTime.now())
                .grantedBy(BaseContext.getCurrentUserId())
                .expiresAt(grantDTO.getExpiresAt())
                .build();

        userPermissionMapper.insert(permission);

        return PermissionGrantResponseVO.builder()
                .permissionId(permission.getId())
                .grantedAt(permission.getGrantedAt())
                .build();
    }

    @Override
    @Transactional
    public void revokePermission(String userId, String permissionId) {
        User user = getUserById(userId);
        if (user == null) {
            throw UserException.userNotFound();
        }

        UserPermission permission = userPermissionMapper.selectById(permissionId);
        if (permission == null || !permission.getUserId().equals(userId)) {
            throw UserException.userNotFound();
        }

        userPermissionMapper.deleteById(permissionId);
    }

    // 私有辅助方法
    private User getUserById(String userId) {
        return adminMapper.selectById(userId);
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
                .permissionId(permission.getId())
                .resourceType(permission.getResourceType())
                .resourceId(permission.getResourceId())
                .permission(permission.getPermission())
                .grantedAt(permission.getGrantedAt())
                .grantedBy(permission.getGrantedBy())
                .expiresAt(permission.getExpiresAt())
                .build();
    }
}