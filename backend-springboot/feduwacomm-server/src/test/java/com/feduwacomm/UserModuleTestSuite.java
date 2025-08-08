package com.feduwacomm;

import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * 用户模块测试套件
 * 
 * 该测试套件包含了用户模块的所有测试类：
 * - UserControllerTest: 控制器层单元测试
 * - UserServiceTest: 服务层单元测试
 * - UserIntegrationTest: 集成测试
 * 
 * 运行此测试套件将执行用户模块的完整测试覆盖
 */
@Suite
@SuiteDisplayName("用户模块测试套件")
@SelectPackages({
        "com.feduwacomm.controller",
        "com.feduwacomm.service",
        "com.feduwacomm.integration"
})
@IncludeClassNamePatterns(".*Test")
public class UserModuleTestSuite {
    // 测试套件配置
    // 通过@SelectPackages注解来选择要包含的测试包
    // 通过@IncludeClassNamePatterns注解来指定测试类的命名模式
}