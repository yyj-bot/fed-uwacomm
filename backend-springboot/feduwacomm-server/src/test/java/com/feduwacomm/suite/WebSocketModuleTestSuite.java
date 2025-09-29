package com.feduwacomm.suite;

import com.feduwacomm.controller.WebSocketProtocolControllerTest;
import com.feduwacomm.service.WebSocketProtocolServiceTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * WebSocket通信模块测试套件
 * 包含WebSocket协议处理和连接管理相关测试
 */
@Suite
@SelectClasses({
    WebSocketProtocolControllerTest.class,
    WebSocketProtocolServiceTest.class
})
public class WebSocketModuleTestSuite {
    // 测试套件类，无需实现方法
}