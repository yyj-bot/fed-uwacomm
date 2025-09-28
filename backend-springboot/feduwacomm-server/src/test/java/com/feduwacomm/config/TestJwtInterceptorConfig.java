package com.feduwacomm.config;

import com.feduwacomm.common.BaseContext;
import com.feduwacomm.exception.UserException;
import com.feduwacomm.interceptor.JwtAuthenticationInterceptor;
import com.feduwacomm.utils.UserJwtUtil;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 测试环境JWT拦截器配置
 * 提供测试专用的JWT认证模拟
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@TestConfiguration
@Profile("test")
public class TestJwtInterceptorConfig {

    @MockBean
    private UserJwtUtil userJwtUtil;

    /**
     * 测试专用JWT认证拦截器
     * 使用预定义的token-role映射进行认证
     */
    @Bean
    @Primary
    public JwtAuthenticationInterceptor testJwtAuthenticationInterceptor() {
        return new JwtAuthenticationInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                    throws Exception {

                // 获取Authorization头
                String authHeader = request.getHeader("Authorization");
                if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
                    throw UserException.tokenMissing();
                }

                // 提取Token
                String token = authHeader.substring(7).trim();
                if (!StringUtils.hasText(token)) {
                    throw UserException.tokenInvalid();
                }

                // 根据token确定角色（测试专用逻辑）
                String role = null;
                String userId = "testuser";
                String username = "testuser";

                if ("admin_token".equals(token)) {
                    role = "ADMIN";
                    userId = "admin123";
                    username = "admin";
                } else if ("researcher_token".equals(token)) {
                    role = "RESEARCHER";
                    userId = "researcher123";
                    username = "researcher";
                } else if ("operator_token".equals(token)) {
                    role = "OPERATOR";
                    userId = "operator123";
                    username = "operator";
                } else if ("viewer_token".equals(token)) {
                    role = "VIEWER";
                    userId = "viewer123";
                    username = "viewer";
                } else if ("invalid_token".equals(token)) {
                    throw UserException.tokenInvalid();
                } else {
                    // 未知token，给予默认角色
                    role = "USER";
                }

                // 设置用户上下文
                BaseContext.setUserInfo(userId, username, role);
                return true;
            }

            @Override
            public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
                    throws Exception {
                // 清理ThreadLocal
                BaseContext.clear();
            }
        };
    }
}