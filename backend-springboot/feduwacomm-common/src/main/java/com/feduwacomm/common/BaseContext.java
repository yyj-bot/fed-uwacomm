package com.feduwacomm.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

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

    // 使用单例模式保证全局唯一
    private static final AtomicReference<BaseContext> INSTANCE = new AtomicReference<>();

    private final ThreadLocal<String> userIdHolder = new ThreadLocal<>();
    private final ThreadLocal<String> usernameHolder = new ThreadLocal<>();
    private final ThreadLocal<String> userRoleHolder = new ThreadLocal<>();

    /**
     * 私有构造函数，防止外部实例化
     */
    private BaseContext() {
        log.debug("BaseContext实例已创建");
    }

    /**
     * 获取BaseContext单例实例
     * 如果Spring容器中已有实例，则返回Spring管理的实例
     * 否则创建新的单例实例
     */
    public static BaseContext getInstance() {
        BaseContext instance = INSTANCE.get();
        if (instance == null) {
            instance = new BaseContext();
            if (INSTANCE.compareAndSet(null, instance)) {
                log.debug("创建BaseContext单例实例");
            } else {
                instance = INSTANCE.get();
            }
        }
        return instance;
    }

    /**
     * 设置Spring管理的实例（由Spring容器调用）
     */
    public void setSpringInstance(BaseContext instance) {
        INSTANCE.set(instance);
        log.debug("设置Spring管理的BaseContext实例");
    }

    /**
     * 设置当前线程的用户ID
     */
    public static void setUserId(String userId) {
        getInstance().userIdHolder.set(userId);
        log.debug("设置当前线程用户ID: {}", userId);
    }

    /**
     * 获取当前线程的用户ID
     */
    public static String getUserId() {
        String userId = getInstance().userIdHolder.get();
        log.debug("获取当前线程用户ID: {}", userId);
        return userId;
    }

    /**
     * 设置当前线程的用户名
     */
    public static void setUsername(String username) {
        getInstance().usernameHolder.set(username);
        log.debug("设置当前线程用户名: {}", username);
    }

    /**
     * 获取当前线程的用户名
     */
    public static String getUsername() {
        String username = getInstance().usernameHolder.get();
        log.debug("获取当前线程用户名: {}", username);
        return username;
    }

    /**
     * 设置当前线程的用户角色
     */
    public static void setUserRole(String userRole) {
        getInstance().userRoleHolder.set(userRole);
        log.debug("设置当前线程用户角色: {}", userRole);
    }

    /**
     * 获取当前线程的用户角色
     */
    public static String getUserRole() {
        String userRole = getInstance().userRoleHolder.get();
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
        getInstance().userIdHolder.remove();
        getInstance().usernameHolder.remove();
        getInstance().userRoleHolder.remove();
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