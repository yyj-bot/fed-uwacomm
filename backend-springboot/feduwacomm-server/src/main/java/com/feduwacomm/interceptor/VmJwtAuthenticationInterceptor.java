package com.feduwacomm.interceptor;

import com.feduwacomm.common.BaseContext;
import com.feduwacomm.exception.UserException;
import com.feduwacomm.utils.VmJwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * VM JWT认证拦截器
 * 用于验证VM JWT Token并设置VM上下文信息
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Component
public class VmJwtAuthenticationInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(VmJwtAuthenticationInterceptor.class);

    @Autowired
    private VmJwtUtil vmJwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        log.debug("VM JWT认证拦截器开始处理请求: {}", request.getRequestURI());

        // 获取Authorization头
        String authHeader = request.getHeader("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            log.warn("VM请求缺少有效的Authorization头: {}", request.getRequestURI());
            throw UserException.tokenMissing();
        }

        // 提取Token并清理空白字符
        String token = authHeader.substring(7).trim();
        if (!StringUtils.hasText(token)) {
            log.warn("VM Token为空: {}", request.getRequestURI());
            throw UserException.tokenInvalid();
        }

        try {
            // 验证Token
            var claims = vmJwtUtil.validateToken(token);

            // 从Token中提取VM信息
            String vmId = claims.get("vmId", String.class);
            String vmName = claims.get("vmName", String.class);
            String status = claims.get("status", String.class);
            String type = claims.get("type", String.class);

            // 验证Token类型，只允许access token
            if (!"access".equals(type)) {
                log.warn("VM Token类型错误，期望access类型: {}", type);
                throw UserException.tokenInvalid();
            }

            // 设置VM上下文信息（复用BaseContext，vmId作为userId）
            BaseContext.setUserInfo(vmId, vmName, "VM");

            log.debug("VM JWT认证成功 - VM ID: {}, VM名称: {}, 状态: {}", vmId, vmName, status);
            return true;

        } catch (Exception e) {
            log.warn("VM JWT Token验证失败: {}", e.getMessage());
            if (e instanceof UserException) {
                throw e;
            } else {
                throw UserException.tokenInvalid();
            }
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        // 清理ThreadLocal
        BaseContext.clear();
        log.debug("VM JWT认证拦截器完成处理，已清理VM上下文");
    }
}