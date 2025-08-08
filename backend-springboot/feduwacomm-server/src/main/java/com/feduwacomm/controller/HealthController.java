package com.feduwacomm.controller;

import com.feduwacomm.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    /**
     * 健康检查接口
     * 
     * @return 健康状态信息
     */
    @GetMapping
    public Result<Map<String, Object>> healthCheck() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("timestamp", LocalDateTime.now());
        healthInfo.put("service", "FedUWAComm Backend");
        healthInfo.put("version", "1.0.0");
        healthInfo.put("message", "水声联邦学习后端服务运行正常");

        return Result.success("健康检查通过", healthInfo);
    }
}