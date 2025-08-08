package com.feduwacomm.controller;

import com.feduwacomm.common.Result;
import com.feduwacomm.service.WebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
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
    public Result<String> sendBroadcast(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        webSocketService.sendBroadcastMessage(message);
        return Result.success("广播消息发送成功", "消息已广播");
    }

    /**
     * 发送私信
     */
    @PostMapping("/private")
    public Result<String> sendPrivateMessage(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String message = request.get("message");
        webSocketService.sendPrivateMessage(username, message);
        return Result.success("私信发送成功", "私信已发送");
    }

    /**
     * 发送系统通知
     */
    @PostMapping("/notification")
    public Result<String> sendNotification(@RequestBody Map<String, String> request) {
        String notification = request.get("notification");
        webSocketService.sendNotification(notification);
        return Result.success("系统通知发送成功", "通知已发送");
    }

    /**
     * 发送联邦学习消息
     */
    @PostMapping("/federated-learning")
    public Result<String> sendFederatedLearningMessage(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        webSocketService.sendFederatedLearningMessage(message);
        return Result.success("联邦学习消息发送成功", "消息已发送");
    }

    /**
     * 获取在线用户统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("onlineUserCount", webSocketService.getOnlineUserCount());
        stats.put("onlineUsers", webSocketService.getOnlineUsers().keySet());
        return Result.success("获取统计信息成功", stats);
    }

    /**
     * 检查用户是否在线
     */
    @GetMapping("/online/{username}")
    public Result<Boolean> checkUserOnline(@PathVariable String username) {
        boolean isOnline = webSocketService.isUserOnline(username);
        return Result.success("检查用户在线状态成功", isOnline);
    }

    /**
     * 发送联邦学习进度
     */
    @PostMapping("/fl-progress")
    public Result<String> sendFederatedLearningProgress(@RequestBody Map<String, Object> request) {
        Integer progress = (Integer) request.get("progress");
        String message = (String) request.get("message");
        webSocketService.sendFederatedLearningProgress(progress, message);
        return Result.success("联邦学习进度发送成功", "进度已发送");
    }

    /**
     * 发送联邦学习结果
     */
    @PostMapping("/fl-result")
    public Result<String> sendFederatedLearningResult(@RequestBody Map<String, Object> request) {
        Object result = request.get("result");
        webSocketService.sendFederatedLearningResult(result);
        return Result.success("联邦学习结果发送成功", "结果已发送");
    }
}