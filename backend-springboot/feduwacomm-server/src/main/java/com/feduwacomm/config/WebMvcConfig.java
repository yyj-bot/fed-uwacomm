package com.feduwacomm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC配置类
 * 配置内容协商、HTTP方法处理等
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 配置内容协商
     * 确保严格验证Content-Type
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
            // 优先使用请求头中的Accept
            .favorParameter(false)
            .favorPathExtension(false)
            // 默认媒体类型
            .defaultContentType(MediaType.APPLICATION_JSON)
            // 严格模式 - 不允许不匹配的媒体类型
            .ignoreUnknownPathExtensions(true);
    }
}