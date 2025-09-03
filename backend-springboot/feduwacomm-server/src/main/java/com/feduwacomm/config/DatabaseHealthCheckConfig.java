package com.feduwacomm.config;

import com.feduwacomm.service.DatabaseInitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * 数据库健康检查配置类
 * 在应用启动时测试数据库连接
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Configuration
public class DatabaseHealthCheckConfig implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseHealthCheckConfig.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Environment environment;

    @Autowired
    private DatabaseInitService databaseInitService;

    @Override
    public void run(String... args) throws Exception {
        log.info("开始检查数据库连接...");
        
        try {
            // 测试数据库连接
            testDatabaseConnection();
            
            // 检查数据库版本和基本信息
            checkDatabaseInfo();
            
            // 检查并初始化必要的表
            log.info("开始检查数据库表结构...");
            databaseInitService.checkAndInitDatabase();
            
            // 检查表数据
            databaseInitService.checkTableData();
            
            log.info("✅ 数据库连接检查成功！");
            
        } catch (Exception e) {
            log.error("❌ 数据库连接检查失败！", e);
            log.error("请检查以下配置：");
            log.error("1. MySQL服务是否启动");
            log.error("2. 数据库连接URL: {}", environment.getProperty("spring.datasource.url"));
            log.error("3. 数据库用户名: {}", environment.getProperty("spring.datasource.username"));
            log.error("4. 数据库密码是否正确");
            log.error("5. 数据库是否存在: feduwacomm");
            
            // 在开发环境中，可以选择继续启动或退出
            if (isDevelopmentEnvironment()) {
                log.warn("⚠️  开发环境：应用将继续启动，但数据库功能可能不可用");
            } else {
                log.error("🚫 生产环境：数据库连接失败，应用将退出");
                System.exit(1);
            }
        }
    }

    /**
     * 测试数据库连接
     */
    private void testDatabaseConnection() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(5)) { // 5秒超时
                log.info("数据库连接有效");
            } else {
                throw new SQLException("数据库连接无效");
            }
        }
    }

    /**
     * 检查数据库信息
     */
    private void checkDatabaseInfo() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            log.info("数据库信息:");
            log.info("  数据库产品名称: {}", metaData.getDatabaseProductName());
            log.info("  数据库版本: {}", metaData.getDatabaseProductVersion());
            log.info("  驱动名称: {}", metaData.getDriverName());
            log.info("  驱动版本: {}", metaData.getDriverVersion());
            log.info("  URL: {}", metaData.getURL());
            log.info("  用户名: {}", metaData.getUserName());
            
            // 检查数据库是否存在
            String catalog = connection.getCatalog();
            if (catalog != null) {
                log.info("  当前数据库: {}", catalog);
            }
        }
    }

    /**
     * 判断是否为开发环境
     */
    private boolean isDevelopmentEnvironment() {
        String activeProfile = environment.getActiveProfiles().length > 0 
            ? environment.getActiveProfiles()[0] 
            : environment.getDefaultProfiles()[0];
        return "dev".equals(activeProfile) || "test".equals(activeProfile);
    }
} 