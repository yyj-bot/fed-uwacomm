package com.feduwacomm.suite;

import com.feduwacomm.controller.VmInstanceControllerTest;
import com.feduwacomm.controller.VmRoundModelControllerTest;
import com.feduwacomm.service.VmInstanceServiceTest;
import com.feduwacomm.service.VmRoundModelServiceTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * 虚拟机管理模块测试套件
 * 包含VM实例管理和轮次模型相关测试
 */
@Suite
@SelectClasses({
    VmInstanceControllerTest.class,
    VmRoundModelControllerTest.class,
    VmInstanceServiceTest.class,
    VmRoundModelServiceTest.class
})
public class VmModuleTestSuite {
    // 测试套件类，无需实现方法
}