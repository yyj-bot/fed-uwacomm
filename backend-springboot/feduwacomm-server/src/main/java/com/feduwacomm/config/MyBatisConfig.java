package com.feduwacomm.config;

import com.feduwacomm.enums.DataStatus;
import com.feduwacomm.enums.DataType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * MyBatis配置类
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Configuration
@MapperScan("com.feduwacomm.mapper")
public class MyBatisConfig {

    /**
     * 配置SqlSessionFactory
     *
     * @param dataSource 数据源
     * @param jsonTypeHandler Spring管理的JSON类型处理器
     * @return SqlSessionFactory
     * @throws Exception 异常
     */
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource, JsonTypeHandler jsonTypeHandler) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);

        // 设置MyBatis配置
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);

        // 注册Spring管理的类型处理器实例
        configuration.getTypeHandlerRegistry().register(java.util.Map.class, jsonTypeHandler);

        sessionFactory.setConfiguration(configuration);

        // 设置Mapper XML文件位置
        sessionFactory.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath:mapper/*.xml"));

        return sessionFactory.getObject();
    }
}