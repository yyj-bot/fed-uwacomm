package com.feduwacomm.suite;

import com.feduwacomm.controller.HealthControllerTest;
import com.feduwacomm.service.DatabaseHealthServiceTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * 健康检查模块测试套件
 * 包含健康检查控制器和相关服务的测试
 */
@Suite
@SelectClasses({
    HealthControllerTest.class,
    DatabaseHealthServiceTest.class
})
public class HealthModuleTestSuite {
    // 测试套件类，无需实现方法
}