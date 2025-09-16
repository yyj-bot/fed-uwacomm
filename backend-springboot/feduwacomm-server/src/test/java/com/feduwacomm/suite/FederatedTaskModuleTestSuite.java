package com.feduwacomm.suite;

import com.feduwacomm.controller.FederatedTaskControllerTest;
import com.feduwacomm.service.FederatedTaskServiceTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * 联邦任务模块测试套件
 * 包含联邦学习任务管理相关测试
 */
@Suite
@SelectClasses({
    FederatedTaskControllerTest.class,
    FederatedTaskServiceTest.class
})
public class FederatedTaskModuleTestSuite {
    // 测试套件类，无需实现方法
}