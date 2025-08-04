package com.feduwacomm.controller;

import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "FedUWAComm Backend");
        response.put("version", "1.0.0");
        response.put("message", "水声联邦学习后端服务运行正常");
        
        return ResponseEntity.ok(response);
    }
} 