package com.feduwacomm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

/**
 * 水声联邦学习后端服务主应用程序类
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@SpringBootApplication(exclude = {
    SecurityAutoConfiguration.class,
    SecurityFilterAutoConfiguration.class,
    UserDetailsServiceAutoConfiguration.class,
    ManagementWebSecurityAutoConfiguration.class
})
@EnableTransactionManagement
@EnableScheduling
@EnableAsync
public class FedUWACommApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(FedUWACommApplication.class, args);
        Environment env = context.getEnvironment();
        
        // 获取当前环境配置
        String activeProfile = env.getProperty("spring.profiles.active", "default");
        String port = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "");
        
        // 构建完整的服务地址
        String baseUrl = String.format("http://localhost:%s%s", port, contextPath);
        String apiUrl = baseUrl + "/api";
        
        System.out.println("=================================");
        System.out.println("水声联邦学习后端服务启动成功！");
        System.out.println("当前环境: " + activeProfile.toUpperCase());
        System.out.println("服务地址: " + apiUrl);
        System.out.println("静态页面: " + baseUrl);
        System.out.println("测试页面: " + baseUrl + "/test/index.html");
        System.out.println("=================================");
    }
} 