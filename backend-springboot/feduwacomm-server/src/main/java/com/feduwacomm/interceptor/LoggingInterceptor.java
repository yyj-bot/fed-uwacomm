package com.feduwacomm.interceptor;

import com.feduwacomm.common.BaseContext;
import com.feduwacomm.service.LogService;
import com.feduwacomm.utils.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日志拦截器
 * 用于记录请求日志
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    @Autowired
    private LogService logService;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        String clientIp = IpUtil.getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String userId = BaseContext.getUserId();
        String username = BaseContext.getUsername();

        logService.logInfo(
                "请求开始 - URI: " + requestURI + ", 方法: " + method + ", 用户代理: " + userAgent + ", 时间: "
                        + LocalDateTime.now().format(formatter),
                userId, username, requestURI, clientIp);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        int status = response.getStatus();
        String userId = BaseContext.getUserId();
        String username = BaseContext.getUsername();
        String clientIp = IpUtil.getClientIpAddress(request);

        if (ex != null) {
            logService.logError(
                    "请求异常 - URI: " + requestURI + ", 方法: " + method + ", 状态: " + status + ", 时间: "
                            + LocalDateTime.now().format(formatter),
                    userId, username, requestURI, clientIp, ex);
        } else {
            logService.logInfo(
                    "请求完成 - URI: " + requestURI + ", 方法: " + method + ", 状态: " + status + ", 时间: "
                            + LocalDateTime.now().format(formatter),
                    userId, username, requestURI, clientIp);
        }
    }
}