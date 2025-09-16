package com.feduwacomm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT配置类
 * 从配置文件中读取JWT相关配置，支持用户和VM双套配置
 */
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    
    /**
     * 用户JWT配置
     */
    private UserConfig user = new UserConfig();
    
    /**
     * VM JWT配置
     */
    private VmConfig vm = new VmConfig();
    
    // Getter和Setter方法
    public UserConfig getUser() {
        return user;
    }
    
    public void setUser(UserConfig user) {
        this.user = user;
    }
    
    public VmConfig getVm() {
        return vm;
    }
    
    public void setVm(VmConfig vm) {
        this.vm = vm;
    }
    
    /**
     * 用户JWT配置类
     */
    public static class UserConfig {
        private String secret;
        private Long expiration;
        private Long refreshExpiration;
        
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
    
    /**
     * VM JWT配置类
     */
    public static class VmConfig {
        private String secret;
        private Long expiration;
        private Long refreshExpiration;
        
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
} 