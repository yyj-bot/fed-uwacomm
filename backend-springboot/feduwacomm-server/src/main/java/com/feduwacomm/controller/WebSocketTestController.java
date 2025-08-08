package com.feduwacomm.controller;

import com.feduwacomm.service.WebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket测试控制器
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/websocket")
@CrossOrigin(origins = "*")
public class WebSocketTestController {

    @Autowired
    private WebSocketService webSocketService;

    /**
     * 发送广播消息
     */
    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, Object>> sendBroadcast(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String message = request.get("message");
            webSocketService.sendBroadcastMessage(message);

            response.put("success", true);
            response.put("message", "广播消息发送成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "广播消息发送失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 发送私信
     */
    @PostMapping("/private")
    public ResponseEntity<Map<String, Object>> sendPrivateMessage(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = request.get("username");
            String message = request.get("message");

            if (username == null || message == null) {
                response.put("success", false);
                response.put("message", "用户名和消息不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            webSocketService.sendPrivateMessage(username, message);

            response.put("success", true);
            response.put("message", "私信发送成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "私信发送失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 发送系统通知
     */
    @PostMapping("/notification")
    public ResponseEntity<Map<String, Object>> sendNotification(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String notification = request.get("notification");
            webSocketService.sendNotification(notification);

            response.put("success", true);
            response.put("message", "系统通知发送成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "系统通知发送失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 发送联邦学习消息
     */
    @PostMapping("/federated-learning")
    public ResponseEntity<Map<String, Object>> sendFederatedLearningMessage(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String message = request.get("message");
            webSocketService.sendFederatedLearningMessage(message);

            response.put("success", true);
            response.put("message", "联邦学习消息发送成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "联邦学习消息发送失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取在线用户统计
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("onlineUserCount", webSocketService.getOnlineUserCount());
            stats.put("onlineUsers", webSocketService.getOnlineUsers().keySet());

            response.put("success", true);
            response.put("data", stats);
            response.put("message", "获取统计信息成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取统计信息失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 检查用户是否在线
     */
    @GetMapping("/online/{username}")
    public ResponseEntity<Map<String, Object>> checkUserOnline(@PathVariable String username) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean isOnline = webSocketService.isUserOnline(username);

            response.put("success", true);
            response.put("data", isOnline);
            response.put("message", "检查用户在线状态成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "检查用户在线状态失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 发送联邦学习进度
     */
    @PostMapping("/fl-progress")
    public ResponseEntity<Map<String, Object>> sendFederatedLearningProgress(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Integer progress = (Integer) request.get("progress");
            String message = (String) request.get("message");

            if (progress == null || message == null) {
                response.put("success", false);
                response.put("message", "进度和消息不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            webSocketService.sendFederatedLearningProgress(progress, message);

            response.put("success", true);
            response.put("message", "联邦学习进度发送成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "联邦学习进度发送失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 发送联邦学习结果
     */
    @PostMapping("/fl-result")
    public ResponseEntity<Map<String, Object>> sendFederatedLearningResult(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Object result = request.get("result");
            webSocketService.sendFederatedLearningResult(result);

            response.put("success", true);
            response.put("message", "联邦学习结果发送成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "联邦学习结果发送失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}