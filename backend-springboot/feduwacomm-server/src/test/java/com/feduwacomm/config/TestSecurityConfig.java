package com.feduwacomm.config;

import com.feduwacomm.interceptor.JwtAuthenticationInterceptor;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

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

        // 配置Mock拦截器始终允许请求通过
        try {
            Mockito.when(mockInterceptor.preHandle(
                Mockito.any(), Mockito.any(), Mockito.any()
            )).thenReturn(true);
        } catch (Exception e) {
            // 处理可能的异常
        }

        return mockInterceptor;
    }
}