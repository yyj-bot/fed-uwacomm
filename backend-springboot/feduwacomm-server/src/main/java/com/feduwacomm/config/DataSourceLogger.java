package com.feduwacomm.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 启动时记录数据源连接信息，方便确认测试环境实际使用的数据库。
 */
@Component
public class DataSourceLogger {

    private static final Logger log = LoggerFactory.getLogger(DataSourceLogger.class);

    private final DataSource dataSource;

    public DataSourceLogger(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void logDataSourceDetails() {
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            String user = connection.getMetaData().getUserName();
            log.info("测试环境数据源已初始化: url={}, user={}", url, user);
        } catch (SQLException ex) {
            log.warn("无法获取数据源连接信息: {}", ex.getMessage(), ex);
        }
    }
}

