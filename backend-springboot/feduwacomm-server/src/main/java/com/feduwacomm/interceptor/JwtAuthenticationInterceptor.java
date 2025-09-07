package com.feduwacomm.interceptor;

import com.feduwacomm.common.BaseContext;
import com.feduwacomm.exception.UserException;
import com.feduwacomm.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * JWT认证拦截器
 * 用于验证JWT Token并设置用户上下文信息
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationInterceptor.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        log.debug("JWT认证拦截器开始处理请求: {}", request.getRequestURI());

        // 获取Authorization头
        String authHeader = request.getHeader("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            log.warn("请求缺少有效的Authorization头: {}", request.getRequestURI());
            throw UserException.tokenMissing();
        }

        // 提取Token
        String token = authHeader.substring(7);
        if (!StringUtils.hasText(token)) {
            log.warn("Token为空: {}", request.getRequestURI());
            throw UserException.tokenInvalid();
        }

        try {
            // 验证Token
            var claims = jwtUtil.validateToken(token);

            // 从Token中提取用户信息
            String userId = claims.get("userId", String.class);
            String username = claims.get("username", String.class);
            String role = claims.get("role", String.class);
            String type = claims.get("type", String.class);

            // 验证Token类型，根据请求路径确定允许的token类型
            boolean isRefreshEndpoint = request.getRequestURI().endsWith("/refresh");
            if (isRefreshEndpoint) {
                // refresh端点允许refresh token
                if (!"refresh".equals(type)) {
                    log.warn("Token类型错误，refresh端点期望refresh类型: {}", type);
                    throw UserException.tokenInvalid();
                }
            } else {
                // 其他端点只允许access token
                if (!"access".equals(type)) {
                    log.warn("Token类型错误，期望access类型: {}", type);
                    throw UserException.tokenInvalid();
                }
            }

            // 设置用户上下文
            if (isRefreshEndpoint) {
                // refresh token只包含userId，设置默认值
                BaseContext.setUserInfo(userId, null, null);
            } else {
                // access token包含完整用户信息
                BaseContext.setUserInfo(userId, username, role);
            }

            log.debug("JWT认证成功 - 用户ID: {}, 用户名: {}, 角色: {}", userId, username, role);
            return true;

        } catch (Exception e) {
            log.warn("JWT Token验证失败: {}", e.getMessage());
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
        log.debug("JWT认证拦截器完成处理，已清理用户上下文");
    }
}