package com.feduwacomm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security配置类
 * 配置HTTP安全策略、CORS和基本安全响应头
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 配置安全过滤器链
     *
     * @param http HttpSecurity对象
     * @return SecurityFilterChain
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF（使用JWT Token，不需要CSRF保护）
            .csrf(csrf -> csrf.disable())

            // 配置CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 配置会话管理（无状态，使用JWT）
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 配置基本安全响应头
            .headers(headers -> headers
                // 防止点击劫持
                .frameOptions().deny()

                // 防止内容类型嗅探
                .contentTypeOptions().and())

            // 配置授权规则（允许所有请求，HTTP方法限制由Controller注解处理）
            .authorizeHttpRequests(authz -> authz
                // 允许所有请求通过Spring Security（认证由自定义拦截器处理）
                .anyRequest().permitAll());

        return http.build();
    }

    /**
     * 配置CORS
     *
     * @return CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允许的源
        configuration.setAllowedOrigins(Arrays.asList("*"));

        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // 允许的请求头
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 允许携带凭证（设置为false以支持通配符源）
        configuration.setAllowCredentials(false);

        // 暴露的响应头
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization", "Content-Disposition", "X-Total-Count"));

        // 预检请求缓存时间
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}