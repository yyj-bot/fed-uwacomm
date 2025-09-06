package com.feduwacomm.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * 数据库自动初始化配置类
 * 
 * 功能：
 * 1. 检测数据库是否已初始化
 * 2. 如果未初始化，自动执行初始化脚本
 * 3. 如果已初始化，跳过所有操作
 */
@Slf4j
@Component
@Order(1) // 确保在其他组件之前执行
public class DatabaseAutoInitConfig implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        log.info("=== 数据库初始化检测开始 ===");
        
        try {
            // 检测数据库是否已初始化
            if (isDatabaseInitialized()) {
                log.info("✅ 数据库已初始化，跳过初始化步骤");
                return;
            }
            
            log.info("📦 数据库未初始化，开始执行自动初始化...");
            initializeDatabase();
            
            log.info("✅ 数据库初始化完成");
            
        } catch (Exception e) {
            log.error("❌ 数据库初始化失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 检测数据库是否已初始化
     * 通过检查关键表是否存在来判断
     */
    private boolean isDatabaseInitialized() {
        try {
            // 检查用户表是否存在
            String checkUserTableSql = "SELECT COUNT(*) FROM information_schema.tables " +
                                      "WHERE table_schema = DATABASE() AND table_name = 'users'";
            Integer userTableCount = jdbcTemplate.queryForObject(checkUserTableSql, Integer.class);
            
            // 检查VM表是否存在
            String checkVmTableSql = "SELECT COUNT(*) FROM information_schema.tables " +
                                    "WHERE table_schema = DATABASE() AND table_name = 'vm_instances'";
            Integer vmTableCount = jdbcTemplate.queryForObject(checkVmTableSql, Integer.class);
            
            // 如果关键表都存在，认为数据库已初始化
            boolean isInitialized = (userTableCount != null && userTableCount > 0) && 
                                   (vmTableCount != null && vmTableCount > 0);
            
            if (isInitialized) {
                log.info("🔍 检测到关键表已存在 (users: {}, vm_instances: {})", userTableCount, vmTableCount);
            } else {
                log.info("🔍 关键表不存在，需要初始化数据库");
            }
            
            return isInitialized;
            
        } catch (Exception e) {
            log.warn("⚠️  数据库初始化检测失败，假设需要初始化: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 执行数据库初始化
     */
    private void initializeDatabase() throws Exception {
        try {
            ClassPathResource resource = new ClassPathResource("db/init_mysql.sql");
            if (!resource.exists()) {
                log.error("❌ 初始化脚本不存在: classpath:db/init_mysql.sql");
                throw new RuntimeException("数据库初始化脚本缺失");
            }
            
            log.info("📜 执行数据库初始化脚本: {}", resource.getFilename());
            
            try (Connection connection = dataSource.getConnection()) {
                ScriptUtils.executeSqlScript(connection, resource);
            }
            
            // 验证初始化结果
            validateInitialization();
            
        } catch (Exception e) {
            log.error("❌ 数据库初始化脚本执行失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 验证初始化结果
     */
    private void validateInitialization() {
        try {
            String countSql = "SELECT COUNT(*) FROM information_schema.tables " +
                             "WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'";
            Integer totalTables = jdbcTemplate.queryForObject(countSql, Integer.class);
            
            log.info("📊 初始化后数据库共有 {} 个表", totalTables);
            
            if (totalTables != null && totalTables >= 11) {
                log.info("✅ 数据库初始化验证通过 (期望≥11个表, 实际{}个)", totalTables);
            } else {
                log.warn("⚠️  初始化后表数量不足 (期望≥11个表, 实际{}个)", totalTables);
            }
            
        } catch (Exception e) {
            log.warn("⚠️  数据库初始化验证失败: {}", e.getMessage());
        }
    }
}