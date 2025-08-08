package com.feduwacomm.controller;

import com.feduwacomm.common.Result;
import com.feduwacomm.dto.*;
import com.feduwacomm.service.AdminService;
import com.feduwacomm.vo.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员控制器
 * 提供管理员专用的用户管理接口
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private AdminService adminService;

    /**
     * 获取用户列表
     */
    @GetMapping("/user/list")
    public Result<PageResponseDTO<UserListVO>> getUserList(@ModelAttribute UserQueryDTO queryDTO) {
        PageResponseDTO<UserListVO> response = adminService.getUserList(queryDTO);
        return Result.success("获取成功", response);
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/user/{userId}")
    public Result<UserDetailVO> getUserDetail(@PathVariable String userId) {
        UserDetailVO response = adminService.getUserDetail(userId);
        return Result.success("获取成功", response);
    }

    /**
     * 创建用户
     */
    @PostMapping("/user/create")
    public Result<UserCreateResponseVO> createUser(@Valid @RequestBody UserCreateDTO createDTO) {
        UserCreateResponseVO response = adminService.createUser(createDTO);
        return Result.success("创建成功", response);
    }

    /**
     * 更新用户
     */
    @PutMapping("/user/{userId}")
    public Result<UserUpdateResponseVO> updateUser(@PathVariable String userId,
            @Valid @RequestBody UserAdminUpdateDTO updateDTO) {
        UserUpdateResponseVO response = adminService.updateUser(userId, updateDTO);
        return Result.success("更新成功", response);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/user/{userId}")
    public Result<String> deleteUser(@PathVariable String userId) {
        adminService.deleteUser(userId);
        return Result.success("删除成功", null);
    }

    /**
     * 锁定用户
     */
    @PostMapping("/user/{userId}/lock")
    public Result<UserLockResponseVO> lockUser(@PathVariable String userId,
            @RequestBody UserLockDTO lockDTO) {
        UserLockResponseVO response = adminService.lockUser(userId, lockDTO);
        return Result.success("用户已锁定", response);
    }

    /**
     * 解锁用户
     */
    @PostMapping("/user/{userId}/unlock")
    public Result<UserUnlockResponseVO> unlockUser(@PathVariable String userId) {
        UserUnlockResponseVO response = adminService.unlockUser(userId);
        return Result.success("用户已解锁", response);
    }

    /**
     * 重置用户密码
     */
    @PostMapping("/user/{userId}/reset-password")
    public Result<String> resetPassword(@PathVariable String userId,
            @RequestBody PasswordResetDTO resetDTO) {
        adminService.resetPassword(userId, resetDTO);
        return Result.success("密码重置成功", null);
    }

    /**
     * 获取用户权限
     */
    @GetMapping("/user/{userId}/permissions")
    public Result<List<UserPermissionVO>> getUserPermissions(@PathVariable String userId) {
        List<UserPermissionVO> response = adminService.getUserPermissions(userId);
        return Result.success("获取成功", response);
    }

    /**
     * 授予用户权限
     */
    @PostMapping("/user/{userId}/permissions")
    public Result<PermissionGrantResponseVO> grantPermission(@PathVariable String userId,
            @RequestBody PermissionGrantDTO grantDTO) {
        PermissionGrantResponseVO response = adminService.grantPermission(userId, grantDTO);
        return Result.success("权限授予成功", response);
    }

    /**
     * 撤销用户权限
     */
    @DeleteMapping("/user/{userId}/permissions/{permissionId}")
    public Result<String> revokePermission(@PathVariable String userId,
            @PathVariable String permissionId) {
        adminService.revokePermission(userId, permissionId);
        return Result.success("权限撤销成功", null);
    }
}