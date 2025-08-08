package com.feduwacomm.config;

import com.feduwacomm.interceptor.JwtAuthenticationInterceptor;
import com.feduwacomm.interceptor.PermissionInterceptor;
import com.feduwacomm.interceptor.LoggingInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 用于注册拦截器等Web相关配置
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtAuthenticationInterceptor jwtAuthenticationInterceptor;

    @Autowired
    private PermissionInterceptor permissionInterceptor;

    @Autowired
    private LoggingInterceptor loggingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册JWT认证拦截器
        registry.addInterceptor(jwtAuthenticationInterceptor)
                .addPathPatterns("/api/user/**") // 需要认证的路径
                .excludePathPatterns(
                        "/api/user/register", // 注册接口
                        "/api/user/login", // 登录接口
                        "/api/health", // 健康检查
                        "/api/websocket/**", // WebSocket接口
                        "/error" // 错误页面
                );

        // 注册权限拦截器
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/api/user/**") // 需要权限检查的路径
                .excludePathPatterns(
                        "/api/user/register", // 注册接口
                        "/api/user/login", // 登录接口
                        "/api/user/profile", // 获取个人信息
                        "/api/user/logout", // 登出接口
                        "/api/health", // 健康检查
                        "/api/websocket/**", // WebSocket接口
                        "/error" // 错误页面
                );

        // 注册日志拦截器（最外层，记录所有请求）
        registry.addInterceptor(loggingInterceptor)
                .addPathPatterns("/**") // 记录所有请求
                .excludePathPatterns(
                        "/error", // 错误页面
                        "/favicon.ico" // 网站图标
                );
    }
}