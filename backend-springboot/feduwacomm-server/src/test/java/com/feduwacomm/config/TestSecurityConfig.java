package com.feduwacomm.config;

import com.feduwacomm.interceptor.JwtAuthenticationInterceptor;
import com.feduwacomm.common.BaseContext;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 测试安全配置类
 * 用于在测试环境中Mock JWT认证拦截器
 */
@TestConfiguration
public class TestSecurityConfig {

    /**
     * Mock JWT认证拦截器
     * 在测试环境中使用Mock对象替代实际的JWT拦截器
     */
    @Bean
    @Primary
    public JwtAuthenticationInterceptor mockJwtInterceptor() {
        JwtAuthenticationInterceptor mockInterceptor = Mockito.mock(JwtAuthenticationInterceptor.class);

        // 配置Mock拦截器，在通过验证时设置测试用户上下文
        try {
            Mockito.when(mockInterceptor.preHandle(
                Mockito.any(HttpServletRequest.class),
                Mockito.any(HttpServletResponse.class),
                Mockito.any()
            )).thenAnswer(invocation -> {
                HttpServletRequest request = invocation.getArgument(0);
                String authHeader = request.getHeader("Authorization");

                // 如果有Authorization头，设置测试用户上下文
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    BaseContext.setUserInfo("test-user-123", "testuser", "ADMIN");
                }
                return true;
            });
        } catch (Exception e) {
            // 处理可能的异常
        }

        return mockInterceptor;
    }
}