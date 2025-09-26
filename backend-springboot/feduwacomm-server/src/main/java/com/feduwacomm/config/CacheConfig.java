package com.feduwacomm.config;

import com.feduwacomm.cache.CacheLifecycleManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PreDestroy;

/**
 * 缓存配置类
 * 启用定时任务和缓存生命周期管理
 *
 * @author FedUWAComm Team
 * @version 1.4.0
 */
@Configuration
@EnableScheduling
public class CacheConfig {

    private CacheLifecycleManager cacheLifecycleManager;

    @Bean
    public CacheLifecycleManager cacheLifecycleManager() {
        this.cacheLifecycleManager = new CacheLifecycleManager();
        return this.cacheLifecycleManager;
    }

    /**
     * 应用关闭时清理缓存
     */
    @PreDestroy
    public void cleanup() {
        if (cacheLifecycleManager != null) {
            cacheLifecycleManager.shutdown();
        }
    }
}