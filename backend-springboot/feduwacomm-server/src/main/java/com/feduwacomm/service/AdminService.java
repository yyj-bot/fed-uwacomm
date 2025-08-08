package com.feduwacomm.service;

import com.feduwacomm.dto.*;
import com.feduwacomm.vo.*;

import java.util.List;

/**
 * 管理员服务接口
 * 提供管理员专用的用户管理功能
 */
public interface AdminService {

    // 用户管理
    /**
     * 获取用户列表
     */
    PageResponseDTO<UserListVO> getUserList(UserQueryDTO queryDTO);

    /**
     * 获取用户详情
     */
    UserDetailVO getUserDetail(String userId);

    /**
     * 创建用户
     */
    UserCreateResponseVO createUser(UserCreateDTO createDTO);

    /**
     * 更新用户
     */
    UserUpdateResponseVO updateUser(String userId, UserAdminUpdateDTO updateDTO);

    /**
     * 删除用户
     */
    void deleteUser(String userId);

    // 用户状态管理
    /**
     * 锁定用户
     */
    UserLockResponseVO lockUser(String userId, UserLockDTO lockDTO);

    /**
     * 解锁用户
     */
    UserUnlockResponseVO unlockUser(String userId);

    /**
     * 重置用户密码
     */
    void resetPassword(String userId, PasswordResetDTO resetDTO);

    // 权限管理
    /**
     * 获取用户权限
     */
    List<UserPermissionVO> getUserPermissions(String userId);

    /**
     * 授予用户权限
     */
    PermissionGrantResponseVO grantPermission(String userId, PermissionGrantDTO grantDTO);

    /**
     * 撤销用户权限
     */
    void revokePermission(String userId, String permissionId);
}