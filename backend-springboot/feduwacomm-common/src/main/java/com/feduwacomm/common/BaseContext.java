package com.feduwacomm.common;

import lombok.extern.slf4j.Slf4j;

/**
 * 基础上下文类
 * 使用ThreadLocal存储当前线程的用户信息
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Slf4j
public class BaseContext {

    private static final ThreadLocal<String> userIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> usernameHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> userRoleHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> userAccountHolder = new ThreadLocal<>();

    /**
     * 设置当前线程的用户ID
     */
    public static void setUserId(String userId) {
        userIdHolder.set(userId);
        log.debug("设置当前线程用户ID: {}", userId);
    }

    /**
     * 获取当前线程的用户ID
     */
    public static String getUserId() {
        String userId = userIdHolder.get();
        log.debug("获取当前线程用户ID: {}", userId);
        return userId;
    }

    /**
     * 设置当前线程的用户名
     */
    public static void setUsername(String username) {
        usernameHolder.set(username);
        log.debug("设置当前线程用户名: {}", username);
    }

    /**
     * 获取当前线程的用户名
     */
    public static String getUsername() {
        String username = usernameHolder.get();
        log.debug("获取当前线程用户名: {}", username);
        return username;
    }

    /**
     * 设置当前线程的用户角色
     */
    public static void setUserRole(String userRole) {
        userRoleHolder.set(userRole);
        log.debug("设置当前线程用户角色: {}", userRole);
    }

    /**
     * 获取当前线程的用户角色
     */
    public static String getUserRole() {
        String userRole = userRoleHolder.get();
        log.debug("获取当前线程用户角色: {}", userRole);
        return userRole;
    }

    /**
     * 设置当前线程的用户账号
     */
    public static void setUserAccount(String userAccount) {
        userAccountHolder.set(userAccount);
        log.debug("设置当前线程用户账号: {}", userAccount);
    }

    /**
     * 获取当前线程的用户账号
     */
    public static String getUserAccount() {
        String userAccount = userAccountHolder.get();
        log.debug("获取当前线程用户账号: {}", userAccount);
        return userAccount;
    }

    /**
     * 设置完整的用户信息
     */
    public static void setUserInfo(String userId, String username, String userRole, String userAccount) {
        setUserId(userId);
        setUsername(username);
        setUserRole(userRole);
        setUserAccount(userAccount);
        log.debug("设置完整用户信息 - 用户ID: {}, 用户名: {}, 角色: {}, 账号: {}",
                userId, username, userRole, userAccount);
    }

    /**
     * 清除当前线程的所有用户信息
     */
    public static void clear() {
        userIdHolder.remove();
        usernameHolder.remove();
        userRoleHolder.remove();
        userAccountHolder.remove();
        log.debug("清除当前线程所有用户信息");
    }

    /**
     * 检查当前线程是否有用户信息
     */
    public static boolean hasUserInfo() {
        return getUserId() != null;
    }

    /**
     * 获取当前用户信息的摘要
     */
    public static String getUserInfoSummary() {
        return String.format("用户ID: %s, 用户名: %s, 角色: %s, 账号: %s",
                getUserId(), getUsername(), getUserRole(), getUserAccount());
    }
}