package com.feduwacomm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码加密配置类
 * 提供BCrypt密码编码器用于密码和API Key加密
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Configuration
public class CryptoConfig {
    
    /**
     * BCrypt密码编码器Bean
     * 用于密码和API Key的加密与验证
     *
     * @return BCrypt密码编码器实例
     */
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}