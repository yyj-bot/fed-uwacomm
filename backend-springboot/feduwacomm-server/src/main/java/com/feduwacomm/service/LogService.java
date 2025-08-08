package com.feduwacomm.service;

/**
 * 日志服务接口
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
public interface LogService {

    /**
     * 记录信息日志
     */
    void logInfo(String message, String userId, String username, String requestUri, String clientIp);

    /**
     * 记录警告日志
     */
    void logWarn(String message, String userId, String username, String requestUri, String clientIp);

    /**
     * 记录错误日志
     */
    void logError(String message, String userId, String username, String requestUri, String clientIp,
            Throwable throwable);

    /**
     * 记录调试日志
     */
    void logDebug(String message, String userId, String username, String requestUri, String clientIp);
}