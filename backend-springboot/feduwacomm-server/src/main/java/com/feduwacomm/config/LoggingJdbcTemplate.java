package com.feduwacomm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;

import javax.sql.DataSource;
import java.util.Arrays;

/**
 * 自定义JdbcTemplate，用于在测试时记录关键SQL的查询结果。
 */
public class LoggingJdbcTemplate extends JdbcTemplate {

    private static final Logger log = LoggerFactory.getLogger(LoggingJdbcTemplate.class);

    public LoggingJdbcTemplate(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
        T result = super.queryForObject(sql, requiredType, args);
        if (sql != null && sql.contains("vm_round_models")) {
            log.info("JdbcTemplate queryForObject -> sql={}, args={}, result={}",
                    sql, Arrays.toString(args), result);
        }
        return result;
    }

    @Override
    public <T> T queryForObject(String sql, @Nullable Object[] args, Class<T> requiredType) {
        T result = super.queryForObject(sql, args, requiredType);
        if (sql != null && sql.contains("vm_round_models")) {
            log.info("JdbcTemplate queryForObject -> sql={}, args={}, result={}",
                    sql, args != null ? Arrays.toString(args) : "[]", result);
        }
        return result;
    }
}

