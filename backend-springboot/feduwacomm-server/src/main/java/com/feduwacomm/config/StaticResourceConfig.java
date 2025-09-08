package com.feduwacomm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 静态资源配置类
 * 专门用于配置静态资源访问，与API路径完全分离
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置测试页面访问 - 使用 /pages 前缀，完全独立于API路径
        registry.addResourceHandler("/pages/**")
                .addResourceLocations("classpath:/static/test/")
                .setCachePeriod(0);
        
        // 配置静态资源访问 - 使用 /assets 前缀，完全独立于API路径
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(0);
        
        // 配置根路径下的静态资源，但明确排除API路径
        registry.addResourceHandler("/")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(0)
                .resourceChain(true);
        
        // 配置其他静态资源，但排除API路径
        registry.addResourceHandler("/favicon.ico", "/robots.txt", "/sitemap.xml")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(0);
        
        // 重要：明确排除API路径，确保不会被当作静态资源处理
        // 这里通过resourceChain(true)来优化资源处理
        // 并且通过Spring Boot的自动配置，API路径会被Controller处理，不会被静态资源处理器拦截
    }
} 