package com.feduwacomm.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 管理员日志工具类
 * 用于记录管理员操作的安全审计日志
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Component
public class AdminLogUtil {

    private static final Logger securityLogger = LogManager.getLogger("SECURITY_AUDIT");
    private static final Logger adminLogger = LogManager.getLogger(AdminLogUtil.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 记录管理员登录日志
     */
    public static void logAdminLogin(String adminId, String adminUsername, String clientIp, String userAgent) {
        String message = String.format("[ADMIN_LOGIN] 管理员登录 - ID: %s, 用户名: %s, IP: %s, 时间: %s, User-Agent: %s",
                adminId, adminUsername, clientIp, LocalDateTime.now().format(formatter), userAgent);
        securityLogger.info(message);
        adminLogger.info(message);
    }

    /**
     * 记录管理员登出日志
     */
    public static void logAdminLogout(String adminId, String adminUsername, String clientIp) {
        String message = String.format("[ADMIN_LOGOUT] 管理员登出 - ID: %s, 用户名: %s, IP: %s, 时间: %s",
                adminId, adminUsername, clientIp, LocalDateTime.now().format(formatter));
        securityLogger.info(message);
        adminLogger.info(message);
    }

    /**
     * 记录用户创建日志
     */
    public static void logUserCreation(String adminId, String adminUsername, String targetUserId, 
                                     String targetUsername, String targetEmail, String targetRole) {
        String message = String.format("[USER_CREATION] 管理员创建用户 - 管理员ID: %s, 管理员用户名: %s, " +
                        "目标用户ID: %s, 目标用户名: %s, 目标邮箱: %s, 目标角色: %s, 时间: %s",
                adminId, adminUsername, targetUserId, targetUsername, targetEmail, targetRole, 
                LocalDateTime.now().format(formatter));
        securityLogger.info(message);
        adminLogger.info(message);
    }

    /**
     * 记录用户更新日志
     */
    public static void logUserUpdate(String adminId, String adminUsername, String targetUserId, 
                                   String targetUsername, String updateFields, String oldValues, String newValues) {
        String message = String.format("[USER_UPDATE] 管理员更新用户 - 管理员ID: %s, 管理员用户名: %s, " +
                        "目标用户ID: %s, 目标用户名: %s, 更新字段: %s, 旧值: %s, 新值: %s, 时间: %s",
                adminId, adminUsername, targetUserId, targetUsername, updateFields, oldValues, newValues,
                LocalDateTime.now().format(formatter));
        securityLogger.info(message);
        adminLogger.info(message);
    }

    /**
     * 记录用户删除日志
     */
    public static void logUserDeletion(String adminId, String adminUsername, String targetUserId, 
                                     String targetUsername, String targetRole, String targetStatus) {
        String message = String.format("[USER_DELETION] 管理员删除用户 - 管理员ID: %s, 管理员用户名: %s, " +
                        "目标用户ID: %s, 目标用户名: %s, 目标角色: %s, 目标状态: %s, 时间: %s",
                adminId, adminUsername, targetUserId, targetUsername, targetRole, targetStatus,
                LocalDateTime.now().format(formatter));
        securityLogger.warn(message);
        adminLogger.warn(message);
    }

    /**
     * 记录用户锁定日志
     */
    public static void logUserLock(String adminId, String adminUsername, String targetUserId, 
                                 String targetUsername, int lockDuration, LocalDateTime lockedUntil) {
        String message = String.format("[USER_LOCK] 管理员锁定用户 - 管理员ID: %s, 管理员用户名: %s, " +
                        "目标用户ID: %s, 目标用户名: %s, 锁定时长: %d秒, 锁定截止时间: %s, 时间: %s",
                adminId, adminUsername, targetUserId, targetUsername, lockDuration, 
                lockedUntil.format(formatter), LocalDateTime.now().format(formatter));
        securityLogger.warn(message);
        adminLogger.warn(message);
    }

    /**
     * 记录用户解锁日志
     */
    public static void logUserUnlock(String adminId, String adminUsername, String targetUserId, 
                                   String targetUsername, String oldStatus) {
        String message = String.format("[USER_UNLOCK] 管理员解锁用户 - 管理员ID: %s, 管理员用户名: %s, " +
                        "目标用户ID: %s, 目标用户名: %s, 旧状态: %s, 时间: %s",
                adminId, adminUsername, targetUserId, targetUsername, oldStatus,
                LocalDateTime.now().format(formatter));
        securityLogger.info(message);
        adminLogger.info(message);
    }

    /**
     * 记录密码重置日志
     */
    public static void logPasswordReset(String adminId, String adminUsername, String targetUserId, 
                                      String targetUsername) {
        String message = String.format("[PASSWORD_RESET] 管理员重置用户密码 - 管理员ID: %s, 管理员用户名: %s, " +
                        "目标用户ID: %s, 目标用户名: %s, 时间: %s",
                adminId, adminUsername, targetUserId, targetUsername, LocalDateTime.now().format(formatter));
        securityLogger.warn(message);
        adminLogger.warn(message);
    }

    /**
     * 记录权限授予日志
     */
    public static void logPermissionGrant(String adminId, String adminUsername, String targetUserId, 
                                        String targetUsername, String permissionName, String permissionId) {
        String message = String.format("[PERMISSION_GRANT] 管理员授予用户权限 - 管理员ID: %s, 管理员用户名: %s, " +
                        "目标用户ID: %s, 目标用户名: %s, 权限名称: %s, 权限ID: %s, 时间: %s",
                adminId, adminUsername, targetUserId, targetUsername, permissionName, permissionId,
                LocalDateTime.now().format(formatter));
        securityLogger.info(message);
        adminLogger.info(message);
    }

    /**
     * 记录权限撤销日志
     */
    public static void logPermissionRevoke(String adminId, String adminUsername, String targetUserId, 
                                         String targetUsername, String permissionName, String permissionId) {
        String message = String.format("[PERMISSION_REVOKE] 管理员撤销用户权限 - 管理员ID: %s, 管理员用户名: %s, " +
                        "目标用户ID: %s, 目标用户名: %s, 权限名称: %s, 权限ID: %s, 时间: %s",
                adminId, adminUsername, targetUserId, targetUsername, permissionName, permissionId,
                LocalDateTime.now().format(formatter));
        securityLogger.warn(message);
        adminLogger.warn(message);
    }

    /**
     * 记录权限查询日志
     */
    public static void logPermissionQuery(String adminId, String adminUsername, String targetUserId, 
                                        String targetUsername, int permissionCount) {
        String message = String.format("[PERMISSION_QUERY] 管理员查询用户权限 - 管理员ID: %s, 管理员用户名: %s, " +
                        "目标用户ID: %s, 目标用户名: %s, 权限数量: %d, 时间: %s",
                adminId, adminUsername, targetUserId, targetUsername, permissionCount,
                LocalDateTime.now().format(formatter));
        adminLogger.info(message);
    }

    /**
     * 记录用户列表查询日志
     */
    public static void logUserListQuery(String adminId, String adminUsername, String queryParams, 
                                      int totalCount, int page, int size) {
        String message = String.format("[USER_LIST_QUERY] 管理员查询用户列表 - 管理员ID: %s, 管理员用户名: %s, " +
                        "查询参数: %s, 总数: %d, 页码: %d, 页面大小: %d, 时间: %s",
                adminId, adminUsername, queryParams, totalCount, page, size,
                LocalDateTime.now().format(formatter));
        adminLogger.info(message);
    }

    /**
     * 记录用户详情查询日志
     */
    public static void logUserDetailQuery(String adminId, String adminUsername, String targetUserId, 
                                        String targetUsername) {
        String message = String.format("[USER_DETAIL_QUERY] 管理员查询用户详情 - 管理员ID: %s, 管理员用户名: %s, " +
                        "目标用户ID: %s, 目标用户名: %s, 时间: %s",
                adminId, adminUsername, targetUserId, targetUsername, LocalDateTime.now().format(formatter));
        adminLogger.info(message);
    }

    /**
     * 记录管理员操作失败日志
     */
    public static void logAdminOperationFailure(String adminId, String adminUsername, String operation, 
                                              String targetId, String reason, Exception exception) {
        String message = String.format("[ADMIN_OPERATION_FAILURE] 管理员操作失败 - 管理员ID: %s, 管理员用户名: %s, " +
                        "操作: %s, 目标ID: %s, 失败原因: %s, 时间: %s",
                adminId, adminUsername, operation, targetId, reason, LocalDateTime.now().format(formatter));
        securityLogger.error(message, exception);
        adminLogger.error(message, exception);
    }

    /**
     * 记录管理员权限拒绝日志
     */
    public static void logAdminPermissionDenied(String adminId, String adminUsername, String operation, 
                                              String targetId, String reason) {
        String message = String.format("[ADMIN_PERMISSION_DENIED] 管理员权限被拒绝 - 管理员ID: %s, 管理员用户名: %s, " +
                        "操作: %s, 目标ID: %s, 拒绝原因: %s, 时间: %s",
                adminId, adminUsername, operation, targetId, reason, LocalDateTime.now().format(formatter));
        securityLogger.warn(message);
        adminLogger.warn(message);
    }
} 