package com.feduwacomm.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


/**
 * 基础上下文类
 * 使用ThreadLocal存储当前线程的用户信息
 * 注册为Spring Bean，保证全局唯一
 * 
 * 使用说明：
 * 1. BaseContext已注册为Spring Bean，保证全局唯一
 * 2. 使用单例模式确保整个应用中只有一个实例
 * 3. 所有方法都是静态方法，可以直接调用
 * 4. ThreadLocal确保每个线程的用户信息独立
 * 5. 在请求结束时会自动清理ThreadLocal
 * 
 * 示例用法：
 * // 设置用户信息
 * BaseContext.setUserInfo("user_123", "testuser", "ADMIN");
 * 
 * // 获取用户信息
 * String userId = BaseContext.getCurrentUserId();
 * String username = BaseContext.getUsername();
 * 
 * // 清除用户信息
 * BaseContext.clear();
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Component
public class BaseContext {

    private static final Logger log = LoggerFactory.getLogger(BaseContext.class);

    // 使用ThreadLocal存储线程独立的用户信息
    private static final ThreadLocal<String> userIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> usernameHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> userRoleHolder = new ThreadLocal<>();

    /**
     * 构造函数
     */
    public BaseContext() {
        log.debug("BaseContext实例已创建");
    }


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
     * 设置完整的用户信息
     */
    public static void setUserInfo(String userId, String username, String userRole) {
        setUserId(userId);
        setUsername(username);
        setUserRole(userRole);
        log.debug("设置完整用户信息 - 用户ID: {}, 用户名: {}, 角色: {}",
                userId, username, userRole);
    }

    /**
     * 清除当前线程的所有用户信息
     */
    public static void clear() {
        userIdHolder.remove();
        usernameHolder.remove();
        userRoleHolder.remove();
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
        return String.format("用户ID: %s, 用户名: %s, 角色: %s",
                getUserId(), getUsername(), getUserRole());
    }

    /**
     * 获取当前用户ID（别名方法）
     */
    public static String getCurrentUserId() {
        return getUserId();
    }
    
    /**
     * 获取当前用户ID（另一个别名方法）
     */
    public static String getCurrentId() {
        return getUserId();
    }
}