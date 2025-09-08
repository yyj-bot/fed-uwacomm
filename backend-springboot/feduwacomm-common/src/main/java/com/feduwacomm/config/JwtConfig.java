package com.feduwacomm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT配置类
 * 从配置文件中读取JWT相关配置
 */
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    
    /**
     * JWT密钥
     */
    private String secret;
    
    /**
     * Token过期时间（秒）
     */
    private Long expiration;
    
    /**
     * 刷新Token过期时间（秒）
     */
    private Long refreshExpiration;
    
    // Getter和Setter方法
    public String getSecret() {
        return secret;
    }
    
    public void setSecret(String secret) {
        this.secret = secret;
    }
    
    public Long getExpiration() {
        return expiration;
    }
    
    public void setExpiration(Long expiration) {
        this.expiration = expiration;
    }
    
    public Long getRefreshExpiration() {
        return refreshExpiration;
    }
    
    public void setRefreshExpiration(Long refreshExpiration) {
        this.refreshExpiration = refreshExpiration;
    }
} 