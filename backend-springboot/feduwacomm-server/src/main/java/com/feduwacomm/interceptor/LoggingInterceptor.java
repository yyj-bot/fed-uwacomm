package com.feduwacomm.interceptor;

import com.feduwacomm.common.BaseContext;
import com.feduwacomm.utils.IpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);
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

        log.info("请求开始 - URI: {}, 方法: {}, IP: {}, 用户: {}, 用户代理: {}, 时间: {}",
                requestURI, method, clientIp,
                userId != null ? username + "(" + userId + ")" : "匿名",
                userAgent, LocalDateTime.now().format(formatter));

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

        if (ex != null) {
            log.error("请求异常 - URI: {}, 方法: {}, 状态: {}, 用户: {}, 异常: {}, 时间: {}",
                    requestURI, method, status,
                    userId != null ? username + "(" + userId + ")" : "匿名",
                    ex.getMessage(), LocalDateTime.now().format(formatter));
        } else {
            log.info("请求完成 - URI: {}, 方法: {}, 状态: {}, 用户: {}, 时间: {}",
                    requestURI, method, status,
                    userId != null ? username + "(" + userId + ")" : "匿名",
                    LocalDateTime.now().format(formatter));
        }
    }
}