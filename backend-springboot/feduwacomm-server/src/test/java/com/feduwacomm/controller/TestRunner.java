package com.feduwacomm.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * 测试运行器
 * 用于运行控制器层的单元测试
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
class TestRunner {

    @Test
    void contextLoads() {
        // 测试Spring上下文是否正常加载
    }
}