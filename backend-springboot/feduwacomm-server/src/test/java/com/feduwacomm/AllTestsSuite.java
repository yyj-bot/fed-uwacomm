package com.feduwacomm;

import com.feduwacomm.suite.FederatedTaskModuleTestSuite;
import com.feduwacomm.suite.HealthModuleTestSuite;
import com.feduwacomm.suite.VmModuleTestSuite;
import com.feduwacomm.suite.WebSocketModuleTestSuite;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * 水声联邦学习系统 - 全体测试套件
 * 
 * 包含以下测试套件：
 * - UserModuleTestSuite: 用户模块测试套件
 * - AdminModuleTestSuite: 管理员模块测试套件
 * - ModelVersionModuleTestSuite: 模型版本管理测试套件
 * - FederatedTaskModuleTestSuite: 联邦任务管理测试套件
 * - HealthModuleTestSuite: 健康检查模块测试套件
 * - VmModuleTestSuite: 虚拟机管理模块测试套件
 * - WebSocketModuleTestSuite: WebSocket通信模块测试套件
 * 
 * 使用方法：
 * 1. 单独运行：右键此文件 -> Run 'AllTestsSuite'
 * 2. 命令行运行：mvn test -Dtest=AllTestsSuite
 * 3. 脚本运行：./run-all-tests.bat 或 ./run-all-tests.sh
 * 
 * 测试覆盖范围：
 * - 用户管理模块
 * - 管理员模块
 * - 权限管理模块
 * - 集成测试
 * - 单元测试
 * - 控制器测试
 * - 服务层测试
 */
@Suite
@SuiteDisplayName("水声联邦学习系统 - 全体测试套件")
@SelectClasses({
        UserModuleTestSuite.class,
        AdminModuleTestSuite.class,
        ModelVersionModuleTestSuite.class,
        FederatedTaskModuleTestSuite.class,
        HealthModuleTestSuite.class,
        VmModuleTestSuite.class,
        WebSocketModuleTestSuite.class
})
public class AllTestsSuite {

    /**
     * 测试套件说明
     * 
     * 本测试套件包含系统所有模块的测试：
     * 
     * 1. 用户模块 (UserModuleTestSuite)
     * - UserControllerTest: 用户控制器测试
     * - UserServiceTest: 用户服务测试
     * - UserIntegrationTest: 用户集成测试
     * 
     * 2. 管理员模块 (AdminModuleTestSuite)
     * - AdminControllerTest: 管理员控制器测试
     * - AdminServiceTest: 管理员服务测试
     * 
     * 3. 工具类测试
     * - UuidUtilTest: UUID工具类测试
     * 
     * 4. 应用测试
     * - FedUWACommApplicationTests: 应用启动测试
     * 
     * 测试类型覆盖：
     * - 单元测试 (Unit Tests)
     * - 集成测试 (Integration Tests)
     * - 控制器测试 (Controller Tests)
     * - 服务层测试 (Service Tests)
     * - 工具类测试 (Utility Tests)
     * 
     * 预期测试结果：
     * - 所有测试应该通过
     * - 测试覆盖率 > 80%
     * - 无测试失败或错误
     * - 无内存泄漏
     * - 测试执行时间 < 30秒
     * 
     * 测试环境要求：
     * - Java 8+
     * - Maven 3.6+
     * - 测试数据库已配置
     * - 测试配置文件已设置
     */
}