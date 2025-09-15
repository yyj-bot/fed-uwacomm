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
     * GET /api/admin/user/list
     */
    @GetMapping("/user/list")
    public Result<PageResponseDTO<UserListVO>> getUserList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        
        logger.info("管理员查询用户列表 - 页码: {}, 大小: {}, 用户名: {}, 邮箱: {}, 角色: {}, 状态: {}", 
                   page, size, username, email, role, status);
        
        // 构建查询DTO，将username和email合并为keyword
        String keyword = null;
        if (username != null && !username.trim().isEmpty()) {
            keyword = username;
        } else if (email != null && !email.trim().isEmpty()) {
            keyword = email;
        }
        
        UserQueryDTO queryDTO = UserQueryDTO.builder()
                .page(page)
                .size(size)
                .keyword(keyword)
                .role(role)
                .status(status)
                .build();
        
        try {
            PageResponseDTO<UserListVO> response = adminService.getUserList(queryDTO);
            logger.info("管理员查询用户列表成功 - 总数: {}, 页码: {}", response.getTotal(), response.getPage());
            return Result.success("获取成功", response);
        } catch (Exception e) {
            logger.error("管理员查询用户列表失败", e);
            throw e;
        }
    }

    /**
     * 获取用户详情
     * GET /api/admin/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public Result<UserDetailVO> getUserDetail(@PathVariable String userId) {
        logger.info("管理员查询用户详情 - 用户ID: {}", userId);
        
        try {
            UserDetailVO response = adminService.getUserDetail(userId);
            logger.info("管理员查询用户详情成功 - 用户ID: {}, 用户名: {}", userId, response.getUsername());
            return Result.success("获取成功", response);
        } catch (Exception e) {
            logger.error("管理员查询用户详情失败 - 用户ID: {}", userId, e);
            throw e;
        }
    }

    /**
     * 创建用户
     * POST /api/admin/user/create
     */
    @PostMapping("/user/create")
    public Result<UserCreateResponseVO> createUser(@Valid @RequestBody UserCreateDTO createDTO) {
        logger.info("管理员创建用户 - 用户名: {}, 邮箱: {}, 角色: {}", 
                   createDTO.getUsername(), createDTO.getEmail(), createDTO.getRole());
        
        try {
            UserCreateResponseVO response = adminService.createUser(createDTO);
            logger.info("管理员创建用户成功 - 用户ID: {}, 用户名: {}", response.getUserId(), response.getUsername());
            return Result.success("创建成功", response);
        } catch (Exception e) {
            logger.error("管理员创建用户失败 - 用户名: {}, 邮箱: {}", createDTO.getUsername(), createDTO.getEmail(), e);
            throw e;
        }
    }

    /**
     * 更新用户
     * PUT /api/admin/user/{userId}
     */
    @PutMapping("/user/{userId}")
    public Result<UserUpdateResponseVO> updateUser(@PathVariable String userId,
            @Valid @RequestBody UserAdminUpdateDTO updateDTO) {
        logger.info("管理员更新用户 - 用户ID: {}, 更新字段: {}", userId, 
                   getUpdateFields(updateDTO));
        
        try {
            UserUpdateResponseVO response = adminService.updateUser(userId, updateDTO);
            logger.info("管理员更新用户成功 - 用户ID: {}, 用户名: {}", userId, response.getUsername());
            return Result.success("更新成功", response);
        } catch (Exception e) {
            logger.error("管理员更新用户失败 - 用户ID: {}", userId, e);
            throw e;
        }
    }

    /**
     * 删除用户
     * DELETE /api/admin/user/{userId}
     */
    @DeleteMapping("/user/{userId}")
    public Result<String> deleteUser(@PathVariable String userId) {
        logger.warn("管理员删除用户 - 用户ID: {}", userId);
        
        try {
            adminService.deleteUser(userId);
            logger.warn("管理员删除用户成功 - 用户ID: {}", userId);
            return Result.success("删除成功", null);
        } catch (Exception e) {
            logger.error("管理员删除用户失败 - 用户ID: {}", userId, e);
            throw e;
        }
    }

    /**
     * 锁定用户
     * POST /api/admin/user/{userId}/lock
     */
    @PostMapping("/user/{userId}/lock")
    public Result<UserLockResponseVO> lockUser(@PathVariable String userId,
            @RequestBody(required = false) UserLockDTO lockDTO) {
        // 如果lockDTO为null，创建默认值
        if (lockDTO == null) {
            lockDTO = UserLockDTO.builder().duration(3600).build(); // 默认1小时
        }
        
        logger.warn("管理员锁定用户 - 用户ID: {}, 锁定时长: {}秒", userId, lockDTO.getDuration());
        
        try {
            UserLockResponseVO response = adminService.lockUser(userId, lockDTO);
            logger.warn("管理员锁定用户成功 - 用户ID: {}, 锁定截止时间: {}", userId, response.getLockedUntil());
            return Result.success("用户已锁定", response);
        } catch (Exception e) {
            logger.error("管理员锁定用户失败 - 用户ID: {}", userId, e);
            throw e;
        }
    }

    /**
     * 解锁用户
     * POST /api/admin/user/{userId}/unlock
     */
    @PostMapping("/user/{userId}/unlock")
    public Result<UserUnlockResponseVO> unlockUser(@PathVariable String userId) {
        logger.info("管理员解锁用户 - 用户ID: {}", userId);
        
        try {
            UserUnlockResponseVO response = adminService.unlockUser(userId);
            logger.info("管理员解锁用户成功 - 用户ID: {}, 新状态: {}", userId, response.getStatus());
            return Result.success("用户已解锁", response);
        } catch (Exception e) {
            logger.error("管理员解锁用户失败 - 用户ID: {}", userId, e);
            throw e;
        }
    }

    /**
     * 重置用户密码
     * POST /api/admin/user/{userId}/reset-password
     */
    @PostMapping("/user/{userId}/reset-password")
    public Result<String> resetPassword(@PathVariable String userId,
            @Valid @RequestBody PasswordResetDTO resetDTO) {
        logger.warn("管理员重置用户密码 - 用户ID: {}", userId);
        
        try {
            adminService.resetPassword(userId, resetDTO);
            logger.warn("管理员重置用户密码成功 - 用户ID: {}", userId);
            return Result.success("密码重置成功", null);
        } catch (Exception e) {
            logger.error("管理员重置用户密码失败 - 用户ID: {}", userId, e);
            throw e;
        }
    }

    /**
     * 获取用户统计信息
     * GET /api/admin/user/statistics
     */
    @GetMapping("/user/statistics")
    public Result<UserStatisticsVO> getUserStatistics() {
        logger.info("管理员查询用户统计信息");

        try {
            UserStatisticsVO response = adminService.getUserStatistics();
            logger.info("管理员查询用户统计信息成功 - 总用户数: {}", response.getTotalUsers());
            return Result.success("获取成功", response);
        } catch (Exception e) {
            logger.error("管理员查询用户统计信息失败", e);
            throw e;
        }
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