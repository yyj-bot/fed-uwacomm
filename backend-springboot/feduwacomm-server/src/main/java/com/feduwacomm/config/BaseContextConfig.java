package com.feduwacomm.config;

import com.feduwacomm.common.BaseContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * BaseContext配置类
 * 确保BaseContext在Spring容器启动时被正确初始化
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Configuration
public class BaseContextConfig {

    private static final Logger log = LoggerFactory.getLogger(BaseContextConfig.class);

    @Autowired
    private BaseContext baseContext;

    /**
     * 在Spring容器启动后初始化BaseContext
     */
    @PostConstruct
    public void initBaseContext() {
        // BaseContext已由Spring管理，无需额外初始化
        log.info("BaseContext已成功注册为Spring Bean");
    }
}