package com.feduwacomm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

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
public class FedUWACommApplication {

    public static void main(String[] args) {
        SpringApplication.run(FedUWACommApplication.class, args);
        System.out.println("=================================");
        System.out.println("水声联邦学习后端服务启动成功！");
        System.out.println("服务地址: http://localhost:8080/api");
        System.out.println("=================================");
    }
} 