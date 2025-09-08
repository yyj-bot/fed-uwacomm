package com.feduwacomm.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 日志工具类
 * 提供统一的日志记录方法和日志器管理
 */
public class LogUtil {

    private static final Map<String, Logger> loggerCache = new ConcurrentHashMap<>();

    // 预定义的日志器名称
    public static final String SECURITY_AUDIT = "SECURITY_AUDIT";
    public static final String PERFORMANCE = "PERFORMANCE";
    public static final String ACCESS_LOG = "ACCESS_LOG";
    public static final String BUSINESS = "BUSINESS";
    public static final String SYSTEM = "SYSTEM";

    /**
     * 获取指定名称的日志器
     */
    public static Logger getLogger(String name) {
        return loggerCache.computeIfAbsent(name, LoggerFactory::getLogger);
    }

    /**
     * 获取安全审计日志器
     */
    public static Logger getSecurityLogger() {
        return getLogger(SECURITY_AUDIT);
    }

    /**
     * 获取性能监控日志器
     */
    public static Logger getPerformanceLogger() {
        return getLogger(PERFORMANCE);
    }

    /**
     * 获取访问日志器
     */
    public static Logger getAccessLogger() {
        return getLogger(ACCESS_LOG);
    }

    /**
     * 获取业务日志器
     */
    public static Logger getBusinessLogger() {
        return getLogger(BUSINESS);
    }

    /**
     * 获取系统日志器
     */
    public static Logger getSystemLogger() {
        return getLogger(SYSTEM);
    }

    /**
     * 记录安全审计日志
     */
    public static void securityInfo(String message, Object... arguments) {
        getSecurityLogger().info(message, arguments);
    }

    public static void securityWarn(String message, Object... arguments) {
        getSecurityLogger().warn(message, arguments);
    }

    public static void securityError(String message, Object... arguments) {
        getSecurityLogger().error(message, arguments);
    }

    /**
     * 记录性能监控日志
     */
    public static void performanceInfo(String message, Object... arguments) {
        getPerformanceLogger().info(message, arguments);
    }

    public static void performanceDebug(String message, Object... arguments) {
        getPerformanceLogger().debug(message, arguments);
    }

    /**
     * 记录访问日志
     */
    public static void accessInfo(String message, Object... arguments) {
        getAccessLogger().info(message, arguments);
    }

    /**
     * 记录业务日志
     */
    public static void businessInfo(String message, Object... arguments) {
        getBusinessLogger().info(message, arguments);
    }

    public static void businessWarn(String message, Object... arguments) {
        getBusinessLogger().warn(message, arguments);
    }

    public static void businessError(String message, Object... arguments) {
        getBusinessLogger().error(message, arguments);
    }

    /**
     * 记录系统日志
     */
    public static void systemInfo(String message, Object... arguments) {
        getSystemLogger().info(message, arguments);
    }

    public static void systemWarn(String message, Object... arguments) {
        getSystemLogger().warn(message, arguments);
    }

    public static void systemError(String message, Object... arguments) {
        getSystemLogger().error(message, arguments);
    }

    /**
     * 记录方法执行时间
     */
    public static void logExecutionTime(String methodName, long startTime, Object... additionalInfo) {
        long duration = System.currentTimeMillis() - startTime;
        StringBuilder message = new StringBuilder("方法执行完成: method=").append(methodName).append(", duration=").append(duration).append("ms");
        
        if (additionalInfo.length > 0) {
            message.append(", additionalInfo=");
            for (Object info : additionalInfo) {
                message.append(info).append(", ");
            }
            message.setLength(message.length() - 2); // 移除最后的逗号和空格
        }
        
        getPerformanceLogger().info(message.toString());
    }

    /**
     * 记录用户操作日志
     */
    public static void logUserOperation(String operation, String userId, String username, String ip, Object... additionalInfo) {
        StringBuilder message = new StringBuilder("用户操作: operation=").append(operation)
                .append(", userId=").append(userId)
                .append(", username=").append(username)
                .append(", ip=").append(ip);
        
        if (additionalInfo.length > 0) {
            message.append(", additionalInfo=");
            for (Object info : additionalInfo) {
                message.append(info).append(", ");
            }
            message.setLength(message.length() - 2);
        }
        
        getBusinessLogger().info(message.toString());
    }

    /**
     * 记录安全事件日志
     */
    public static void logSecurityEvent(String eventType, String userId, String username, String ip, String reason, Object... additionalInfo) {
        StringBuilder message = new StringBuilder("安全事件: eventType=").append(eventType)
                .append(", userId=").append(userId)
                .append(", username=").append(username)
                .append(", ip=").append(ip)
                .append(", reason=").append(reason);
        
        if (additionalInfo.length > 0) {
            message.append(", additionalInfo=");
            for (Object info : additionalInfo) {
                message.append(info).append(", ");
            }
            message.setLength(message.length() - 2);
        }
        
        getSecurityLogger().warn(message.toString());
    }

    /**
     * 记录异常日志
     */
    public static void logException(String operation, String userId, String ip, Exception e, Object... additionalInfo) {
        StringBuilder message = new StringBuilder("操作异常: operation=").append(operation)
                .append(", userId=").append(userId)
                .append(", ip=").append(ip)
                .append(", error=").append(e.getMessage());
        
        if (additionalInfo.length > 0) {
            message.append(", additionalInfo=");
            for (Object info : additionalInfo) {
                message.append(info).append(", ");
            }
            message.setLength(message.length() - 2);
        }
        
        getBusinessLogger().error(message.toString(), e);
    }
} 