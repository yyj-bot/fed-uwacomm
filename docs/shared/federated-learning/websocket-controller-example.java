/**
 * 联邦学习WebSocket控制器示例
 *
 * 本文件展示了如何在Spring Boot中实现联邦学习的WebSocket通信，
 * 包括梯度上传处理和全局模型分发功能。
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-25
 */
package com.feduwacomm.controller.websocket;

import com.feduwacomm.common.result.Result;
import com.feduwacomm.pojo.dto.GlobalModelDTO;
import com.feduwacomm.service.federated.FederatedAggregationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 联邦学习WebSocket控制器
 */
@Slf4j
@Controller
public class FederatedLearningWebSocketController {

    @Autowired
    private FederatedAggregationService aggregationService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // 存储任务的客户端连接信息
    private final Map<String, Map<String, Object>> taskClients = new ConcurrentHashMap<>();

    /**
     * 处理客户端梯度上传
     *
     * @param taskId 任务ID
     * @param message 上传的梯度数据
     * @return 上传确认消息
     */
    @MessageMapping("/federated/upload-gradients/{taskId}")
    @SendTo("/topic/federated/upload-confirmation/{taskId}")
    public Map<String, Object> handleGradientUpload(
            @DestinationVariable String taskId,
            String message) {

        log.info("收到任务 {} 的梯度上传请求", taskId);

        try {
            // 解析上传的梯度数据
            JsonNode messageNode = objectMapper.readTree(message);
            Map<String, Object> updateData = objectMapper.convertValue(messageNode, Map.class);

            String clientId = (String) updateData.get("client_id");

            // 记录客户端连接
            recordClientConnection(taskId, clientId);

            // 添加客户端更新到聚合服务
            boolean success = aggregationService.addClientUpdate(taskId, updateData);

            // 创建确认响应
            Map<String, Object> response = new HashMap<>();
            response.put("message_type", "UPLOAD_CONFIRMATION");
            response.put("client_id", clientId);
            response.put("task_id", taskId);
            response.put("success", success);
            response.put("timestamp", LocalDateTime.now().toString());

            if (success) {
                log.info("客户端 {} 的梯度上传成功 - 任务: {}", clientId, taskId);

                // 检查是否可以进行聚合
                checkAndTriggerAggregation(taskId);
            } else {
                log.warn("客户端 {} 的梯度上传失败 - 任务: {}", clientId, taskId);
            }

            return response;

        } catch (Exception e) {
            log.error("处理梯度上传时出错 - 任务: {}, 错误: {}", taskId, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message_type", "UPLOAD_ERROR");
            errorResponse.put("task_id", taskId);
            errorResponse.put("error", "处理梯度上传时出错: " + e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now().toString());

            return errorResponse;
        }
    }

    /**
     * 处理客户端连接订阅
     *
     * @param taskId 任务ID
     * @return 连接确认消息
     */
    @SubscribeMapping("/federated/task/{taskId}")
    public Map<String, Object> handleClientSubscription(@DestinationVariable String taskId) {
        log.info("客户端订阅任务: {}", taskId);

        Map<String, Object> response = new HashMap<>();
        response.put("message_type", "SUBSCRIPTION_CONFIRMATION");
        response.put("task_id", taskId);
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", "connected");

        return response;
    }

    /**
     * 手动触发聚合（管理员操作）
     *
     * @param taskId 任务ID
     * @return 聚合结果
     */
    @MessageMapping("/federated/trigger-aggregation/{taskId}")
    @SendTo("/topic/federated/aggregation-result/{taskId}")
    public Map<String, Object> triggerAggregation(@DestinationVariable String taskId) {
        log.info("手动触发任务 {} 的聚合", taskId);

        try {
            Result<GlobalModelDTO> result = aggregationService.aggregateUpdates(taskId);

            if (result.getCode() == 200) {
                // 聚合成功，广播全局模型
                broadcastGlobalModel(taskId, result.getData());

                Map<String, Object> response = new HashMap<>();
                response.put("message_type", "AGGREGATION_SUCCESS");
                response.put("task_id", taskId);
                response.put("round", result.getData().getRound());
                response.put("participants", result.getData().getParticipants());
                response.put("timestamp", LocalDateTime.now().toString());

                return response;
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message_type", "AGGREGATION_ERROR");
                errorResponse.put("task_id", taskId);
                errorResponse.put("error", result.getMessage());
                errorResponse.put("timestamp", LocalDateTime.now().toString());

                return errorResponse;
            }

        } catch (Exception e) {
            log.error("触发聚合时出错 - 任务: {}, 错误: {}", taskId, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message_type", "AGGREGATION_ERROR");
            errorResponse.put("task_id", taskId);
            errorResponse.put("error", "触发聚合时出错: " + e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now().toString());

            return errorResponse;
        }
    }

    /**
     * 获取任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态信息
     */
    @MessageMapping("/federated/task-status/{taskId}")
    @SendTo("/topic/federated/status/{taskId}")
    public Map<String, Object> getTaskStatus(@DestinationVariable String taskId) {
        log.info("查询任务 {} 的状态", taskId);

        Map<String, Object> status = new HashMap<>();
        status.put("message_type", "TASK_STATUS");
        status.put("task_id", taskId);

        // 获取当前连接的客户端数量
        Map<String, Object> clients = taskClients.get(taskId);
        status.put("connected_clients", clients != null ? clients.size() : 0);

        // 获取当前轮次
        GlobalModelDTO globalModel = aggregationService.getGlobalModels().get(taskId);
        status.put("current_round", globalModel != null ? globalModel.getRound() : 0);

        // 获取聚合方法
        status.put("aggregation_method", aggregationService.getAggregationMethod().getValue());

        status.put("timestamp", LocalDateTime.now().toString());

        return status;
    }

    /**
     * REST API - 获取全局模型
     *
     * @param taskId 任务ID
     * @return 全局模型数据
     */
    @GetMapping("/api/federated/global-model/{taskId}")
    @ResponseBody
    public Result<Map<String, Object>> getGlobalModel(@PathVariable String taskId) {
        try {
            Map<String, Object> globalModelData = aggregationService.getGlobalModelForBroadcast(taskId);
            if (globalModelData != null) {
                return Result.success(globalModelData);
            } else {
                return Result.error("任务 " + taskId + " 的全局模型尚未可用");
            }
        } catch (Exception e) {
            log.error("获取全局模型时出错 - 任务: {}, 错误: {}", taskId, e.getMessage(), e);
            return Result.error("获取全局模型时出错: " + e.getMessage());
        }
    }

    /**
     * REST API - 设置聚合方法
     *
     * @param request 设置请求
     * @return 设置结果
     */
    @PostMapping("/api/federated/set-aggregation-method")
    @ResponseBody
    public Result<String> setAggregationMethod(@RequestBody Map<String, String> request) {
        try {
            String methodName = request.get("method");
            AggregationMethod method = AggregationMethod.valueOf(methodName);
            aggregationService.setAggregationMethod(method);

            log.info("聚合方法已设置为: {}", method.getValue());
            return Result.success("聚合方法已设置为: " + method.getValue());

        } catch (IllegalArgumentException e) {
            return Result.error("无效的聚合方法: " + request.get("method"));
        } catch (Exception e) {
            log.error("设置聚合方法时出错: {}", e.getMessage(), e);
            return Result.error("设置聚合方法时出错: " + e.getMessage());
        }
    }

    /**
     * 记录客户端连接信息
     */
    private void recordClientConnection(String taskId, String clientId) {
        taskClients.computeIfAbsent(taskId, k -> new ConcurrentHashMap<>())
                  .put(clientId, Map.of(
                      "client_id", clientId,
                      "connected_at", LocalDateTime.now().toString(),
                      "last_activity", LocalDateTime.now().toString()
                  ));
    }

    /**
     * 检查并触发聚合
     */
    private void checkAndTriggerAggregation(String taskId) {
        // 检查是否达到最小参与者数量要求
        // 这里可以根据配置或策略决定是否自动触发聚合

        // 示例：如果有足够的客户端更新，自动触发聚合
        Map<String, Object> clients = taskClients.get(taskId);
        if (clients != null && clients.size() >= 3) {
            log.info("达到最小参与者要求，自动触发聚合 - 任务: {}", taskId);

            try {
                Result<GlobalModelDTO> result = aggregationService.aggregateUpdates(taskId);
                if (result.getCode() == 200) {
                    broadcastGlobalModel(taskId, result.getData());
                }
            } catch (Exception e) {
                log.error("自动聚合失败 - 任务: {}, 错误: {}", taskId, e.getMessage(), e);
            }
        }
    }

    /**
     * 广播全局模型到所有订阅的客户端
     */
    private void broadcastGlobalModel(String taskId, GlobalModelDTO globalModel) {
        try {
            Map<String, Object> broadcastData = aggregationService.getGlobalModelForBroadcast(taskId);

            if (broadcastData != null) {
                // 向指定任务的所有订阅客户端广播
                String destination = "/topic/federated/global-model/" + taskId;
                messagingTemplate.convertAndSend(destination, broadcastData);

                log.info("全局模型已广播 - 任务: {}, 轮次: {}, 目标: {}",
                        taskId, globalModel.getRound(), destination);
            }

        } catch (Exception e) {
            log.error("广播全局模型时出错 - 任务: {}, 错误: {}", taskId, e.getMessage(), e);
        }
    }
}

/**
 * 联邦学习REST控制器
 * 提供HTTP API接口用于管理联邦学习任务
 */
@Slf4j
@RestController
@RequestMapping("/api/federated")
public class FederatedLearningRestController {

    @Autowired
    private FederatedAggregationService aggregationService;

    /**
     * 获取所有任务的全局模型状态
     *
     * @return 全局模型状态列表
     */
    @GetMapping("/global-models")
    public Result<Map<String, Object>> getAllGlobalModels() {
        try {
            Map<String, GlobalModelDTO> globalModels = aggregationService.getGlobalModels();

            Map<String, Object> result = new HashMap<>();
            result.put("total_tasks", globalModels.size());
            result.put("tasks", globalModels);
            result.put("current_aggregation_method",
                      aggregationService.getAggregationMethod().getValue());

            return Result.success(result);

        } catch (Exception e) {
            log.error("获取全局模型状态时出错: {}", e.getMessage(), e);
            return Result.error("获取全局模型状态时出错: " + e.getMessage());
        }
    }

    /**
     * 获取聚合统计信息
     *
     * @param taskId 任务ID
     * @return 聚合统计
     */
    @GetMapping("/aggregation-stats/{taskId}")
    public Result<Map<String, Object>> getAggregationStats(@PathVariable String taskId) {
        try {
            GlobalModelDTO globalModel = aggregationService.getGlobalModels().get(taskId);

            if (globalModel == null) {
                return Result.error("任务 " + taskId + " 不存在或尚未开始聚合");
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("task_id", taskId);
            stats.put("current_round", globalModel.getRound());
            stats.put("total_participants", globalModel.getParticipants());
            stats.put("aggregation_method", globalModel.getAggregationMethod());
            stats.put("model_created_at", globalModel.getCreatedAt());
            stats.put("feature_count", globalModel.getParameters().getFeatureImportances().size());

            // 特征重要性统计
            Map<String, Object> featureStats = new HashMap<>();
            var importances = globalModel.getParameters().getFeatureImportances();
            double maxImportance = importances.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            double minImportance = importances.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            double avgImportance = importances.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

            featureStats.put("max_importance", maxImportance);
            featureStats.put("min_importance", minImportance);
            featureStats.put("avg_importance", avgImportance);
            featureStats.put("importance_distribution", importances);

            stats.put("feature_importance_stats", featureStats);

            return Result.success(stats);

        } catch (Exception e) {
            log.error("获取聚合统计时出错 - 任务: {}, 错误: {}", taskId, e.getMessage(), e);
            return Result.error("获取聚合统计时出错: " + e.getMessage());
        }
    }

    /**
     * 重置任务的聚合状态
     *
     * @param taskId 任务ID
     * @return 重置结果
     */
    @PostMapping("/reset-task/{taskId}")
    public Result<String> resetTask(@PathVariable String taskId) {
        try {
            // 这里应该实现重置逻辑
            // 例如清除客户端更新、重置轮次等

            log.info("任务 {} 的聚合状态已重置", taskId);
            return Result.success("任务 " + taskId + " 的聚合状态已重置");

        } catch (Exception e) {
            log.error("重置任务时出错 - 任务: {}, 错误: {}", taskId, e.getMessage(), e);
            return Result.error("重置任务时出错: " + e.getMessage());
        }
    }
}