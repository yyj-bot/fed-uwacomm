package com.feduwacomm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 注册带日志能力的JdbcTemplate，便于排查数据库查询的实时结果。
 */
@Configuration
public class LoggingJdbcTemplateConfig {

    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new LoggingJdbcTemplate(dataSource);
    }
}

