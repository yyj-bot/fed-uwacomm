package com.feduwacomm;

import com.feduwacomm.controller.AdminControllerTest;
import com.feduwacomm.service.AdminServiceTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Admin模块测试套件
 * 
 * 包含以下测试类：
 * - AdminControllerTest: 管理员控制器测试
 * - AdminServiceTest: 管理员服务测试
 * 
 * 使用方法：
 * 1. 单独运行：右键此文件 -> Run 'AdminModuleTestSuite'
 * 2. 命令行运行：mvn test -Dtest=AdminModuleTestSuite
 * 3. 脚本运行：./run-admin-tests.bat 或 ./run-admin-tests.sh
 */
@Suite
@SuiteDisplayName("Admin模块测试套件")
@SelectClasses({
        AdminControllerTest.class,
        AdminServiceTest.class
})
public class AdminModuleTestSuite {

    /**
     * 测试套件说明
     * 
     * 本测试套件覆盖Admin模块的所有功能：
     * 
     * 1. 用户管理功能
     * - 获取用户列表
     * - 获取用户详情
     * - 创建用户
     * - 更新用户信息
     * - 删除用户
     * 
     * 2. 用户状态管理
     * - 锁定用户
     * - 解锁用户
     * - 重置用户密码
     * 
     * 3. 权限管理
     * - 获取用户权限
     * - 授予用户权限
     * - 撤销用户权限
     * 
     * 测试覆盖范围：
     * - 正常流程测试
     * - 异常情况测试
     * - 边界条件测试
     * - 权限验证测试
     * 
     * 预期测试结果：
     * - 所有测试应该通过
     * - 测试覆盖率 > 80%
     * - 无测试失败或错误
     */
}