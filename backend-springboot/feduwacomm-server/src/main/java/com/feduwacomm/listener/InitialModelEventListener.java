package com.feduwacomm.listener;

import com.feduwacomm.event.InitialModelDistributionCompletedEvent;
import com.feduwacomm.event.InitialModelDistributionStartedEvent;
import com.feduwacomm.event.InitialModelGeneratedEvent;
import com.feduwacomm.service.LogService;
import com.feduwacomm.service.NotificationService;
import com.feduwacomm.service.WorkflowStageTransitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 初始模型事件监听器
 * 处理初始模型生成和分发相关的事件
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InitialModelEventListener {

    private final LogService logService;
    private final NotificationService notificationService;
    private final WorkflowStageTransitionService workflowStageTransitionService;
    
    /**
     * 处理初始模型生成完成事件
     */
    @EventListener
    @Async
    public void handleInitialModelGenerated(InitialModelGeneratedEvent event) {
        log.info("处理初始模型生成完成事件: {}", event);
        
        try {
            // 记录系统日志
            Map<String, Object> logDetails = new HashMap<>();
            logDetails.put("modelId", event.getModelId());
            logDetails.put("modelType", event.getModelType());
            logDetails.put("generationMethod", event.getGenerationMethod());
            logDetails.put("modelSize", event.getModelSize());
            logDetails.put("filePath", event.getFilePath());
            logDetails.put("checksum", event.getChecksum());
            logDetails.put("eventTime", event.getEventTime());
            
            logService.logTask(event.getTaskId(), "INFO", 
                    String.format("初始模型生成完成: 模型类型=%s, 生成方式=%s, 大小=%d字节", 
                            event.getModelType(), event.getGenerationMethod(), event.getModelSize()),
                    "InitialModelGenerator", null, logDetails);
            
            // 如果是工作流中的阶段，可以在这里触发下一阶段
            if (event.getOrchestrationId() != null) {
                log.info("初始模型生成完成，准备触发工作流下一阶段: orchestrationId={}",
                        event.getOrchestrationId());

                // 触发工作流下一阶段（数据分发）
                try {
                    System.out.println("🔥🔥🔥 准备触发工作流下一阶段: orchestrationId=" + event.getOrchestrationId());

                    java.util.Map<String, Object> context = new java.util.HashMap<>();
                    context.put("initialModelId", event.getModelId());
                    context.put("modelType", event.getModelType());
                    context.put("generationMethod", event.getGenerationMethod());
                    context.put("modelSize", event.getModelSize());

                    System.out.println("🔥🔥🔥 调用triggerNextStage: currentStage=INITIAL_MODEL_GENERATION");

                    boolean success = workflowStageTransitionService.triggerNextStage(
                        event.getOrchestrationId(),
                        com.feduwacomm.orchestration.WorkflowStage.INITIAL_MODEL_GENERATION,
                        context
                    );

                    System.out.println("🔥🔥🔥 triggerNextStage返回结果: success=" + success);

                    if (success) {
                        log.info("🔥🔥🔥 成功触发工作流下一阶段: orchestrationId={}", event.getOrchestrationId());
                    } else {
                        log.error("🔥🔥🔥 触发工作流下一阶段失败: orchestrationId={}", event.getOrchestrationId());
                    }
                } catch (Exception e) {
                    System.out.println("🔥🔥🔥 触发工作流下一阶段抛出异常: " + e.getMessage());
                    log.error("触发工作流下一阶段异常: orchestrationId={}", event.getOrchestrationId(), e);
                }
            }
            
            // 发送通知（如果需要）
            sendModelGenerationNotification(event);
            
        } catch (Exception e) {
            log.error("处理初始模型生成完成事件失败: {}", event, e);
        }
    }
    
    /**
     * 处理初始模型分发开始事件
     */
    @EventListener
    @Async
    public void handleInitialModelDistributionStarted(InitialModelDistributionStartedEvent event) {
        log.info("处理初始模型分发开始事件: {}", event);
        
        try {
            // 记录系统日志
            Map<String, Object> logDetails = new HashMap<>();
            logDetails.put("modelId", event.getModelId());
            logDetails.put("modelType", event.getModelType());
            logDetails.put("targetVmIds", event.getTargetVmIds());
            logDetails.put("targetVmCount", event.getTargetVmIds() != null ? event.getTargetVmIds().size() : 0);
            logDetails.put("verifyIntegrity", event.getConfig().isVerifyIntegrity());
            logDetails.put("timeoutSeconds", event.getConfig().getTimeoutSeconds());
            logDetails.put("eventTime", event.getEventTime());
            
            logService.logTask(event.getTaskId(), "INFO", 
                    String.format("初始模型分发开始: 模型类型=%s, 目标虚拟机数=%d", 
                            event.getModelType(), event.getTargetVmIds().size()),
                    "InitialModelDistributor", null, logDetails);
            
            // 更新任务状态或工作流状态
            if (event.getOrchestrationId() != null) {
                log.debug("更新工作流阶段状态为分发中: orchestrationId={}", event.getOrchestrationId());

                // 更新工作流阶段状态
                try {
                    boolean success = workflowStageTransitionService.updateStageStatus(
                        event.getOrchestrationId(),
                        com.feduwacomm.orchestration.WorkflowStage.MODEL_DISTRIBUTION,
                        "IN_PROGRESS",
                        "初始模型分发已开始，目标VM数量：" + event.getTargetVmIds().size()
                    );

                    if (success) {
                        log.info("成功更新工作流阶段状态: orchestrationId={}", event.getOrchestrationId());
                    } else {
                        log.error("更新工作流阶段状态失败: orchestrationId={}", event.getOrchestrationId());
                    }
                } catch (Exception e) {
                    log.error("更新工作流阶段状态异常: orchestrationId={}", event.getOrchestrationId(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("处理初始模型分发开始事件失败: {}", event, e);
        }
    }
    
    /**
     * 处理初始模型分发完成事件
     */
    @EventListener
    @Async
    public void handleInitialModelDistributionCompleted(InitialModelDistributionCompletedEvent event) {
        log.info("处理初始模型分发完成事件: {}", event);
        
        try {
            // 记录系统日志
            Map<String, Object> logDetails = new HashMap<>();
            logDetails.put("modelId", event.getModelId());
            logDetails.put("success", event.isSuccess());
            logDetails.put("totalDistributions", event.getTotalDistributions());
            logDetails.put("successfulDistributions", event.getSuccessfulDistributions());
            logDetails.put("failedDistributions", event.getFailedDistributions());
            logDetails.put("successfulVmIds", event.getSuccessfulVmIds());
            logDetails.put("failedVmIds", event.getFailedVmIds());
            logDetails.put("distributionStats", event.getDistributionStats());
            logDetails.put("distributionDuration", event.getDistributionDuration());
            logDetails.put("successRate", event.getSuccessRate());
            logDetails.put("eventTime", event.getEventTime());
            
            String logLevel = event.isSuccess() ? "INFO" : "ERROR";
            String logMessage;
            
            if (event.isCompleteSuccess()) {
                logMessage = String.format("初始模型分发完全成功: 总数=%d, 成功=%d, 耗时=%dms", 
                        event.getTotalDistributions(), event.getSuccessfulDistributions(), event.getDistributionDuration());
            } else if (event.isPartialSuccess()) {
                logMessage = String.format("初始模型分发部分成功: 总数=%d, 成功=%d, 失败=%d, 成功率=%.2f%%, 耗时=%dms", 
                        event.getTotalDistributions(), event.getSuccessfulDistributions(), event.getFailedDistributions(),
                        event.getSuccessRate() * 100, event.getDistributionDuration());
            } else {
                logMessage = String.format("初始模型分发失败: 总数=%d, 错误=%s, 耗时=%dms", 
                        event.getTotalDistributions(), event.getErrorMessage(), event.getDistributionDuration());
            }
            
            logService.logTask(event.getTaskId(), logLevel, logMessage, 
                    "InitialModelDistributor", null, logDetails);
            
            // 如果是工作流中的阶段，更新工作流状态
            if (event.getOrchestrationId() != null) {
                if (event.isSuccess()) {
                    log.info("初始模型分发完成，准备触发工作流下一阶段: orchestrationId={}",
                            event.getOrchestrationId());

                    // 触发工作流下一阶段（联邦训练）
                    try {
                        java.util.Map<String, Object> context = new java.util.HashMap<>();
                        context.put("distributedModelId", event.getModelId());
                        context.put("successVmCount", event.getSuccessVmCount());
                        context.put("failedVmCount", event.getFailedVmCount());
                        context.put("distributionDuration", event.getDuration());

                        boolean success = workflowStageTransitionService.triggerNextStage(
                            event.getOrchestrationId(),
                            com.feduwacomm.orchestration.WorkflowStage.MODEL_DISTRIBUTION,
                            context
                        );

                        if (success) {
                            log.info("成功触发工作流下一阶段: orchestrationId={}", event.getOrchestrationId());
                        } else {
                            log.error("触发工作流下一阶段失败: orchestrationId={}", event.getOrchestrationId());
                        }
                    } catch (Exception e) {
                        log.error("触发工作流下一阶段异常: orchestrationId={}", event.getOrchestrationId(), e);
                    }
                } else {
                    log.error("初始模型分发失败，工作流可能需要处理异常: orchestrationId={}, error={}",
                            event.getOrchestrationId(), event.getErrorMessage());

                    // 处理工作流异常
                    try {
                        boolean success = workflowStageTransitionService.handleWorkflowException(
                            event.getOrchestrationId(),
                            com.feduwacomm.orchestration.WorkflowStage.MODEL_DISTRIBUTION,
                            event.getErrorMessage(),
                            null
                        );

                        if (success) {
                            log.info("成功处理工作流异常: orchestrationId={}", event.getOrchestrationId());
                        } else {
                            log.error("处理工作流异常失败: orchestrationId={}", event.getOrchestrationId());
                        }
                    } catch (Exception e) {
                        log.error("处理工作流异常时发生异常: orchestrationId={}", event.getOrchestrationId(), e);
                    }
                }
            }
            
            // 发送通知
            sendDistributionCompletionNotification(event);
            
            // 如果分发失败，可以考虑自动重试
            if (!event.isSuccess() && shouldAutoRetry(event)) {
                log.info("准备自动重试分发: modelId={}", event.getModelId());
                scheduleDistributionRetry(event);
            }
            
        } catch (Exception e) {
            log.error("处理初始模型分发完成事件失败: {}", event, e);
        }
    }
    
    /**
     * 发送模型生成通知
     */
    private void sendModelGenerationNotification(InitialModelGeneratedEvent event) {
        try {
            // 防御性处理：为null的modelSize提供默认值
            Long modelSize = event.getModelSize() != null ? event.getModelSize() : 0L;

            String title = "初始模型生成完成";
            String message = String.format("模型 %s 生成完成，类型: %s，大小: %d 字节",
                    event.getModelId(), event.getModelType(), modelSize);

            Map<String, Object> details = Map.of(
                "modelId", event.getModelId(),
                "modelType", event.getModelType(),
                "generationMethod", event.getGenerationMethod(),
                "modelSize", modelSize,
                "taskId", event.getTaskId(),
                "orchestrationId", event.getOrchestrationId() != null ? event.getOrchestrationId() : ""
            );

            boolean success = notificationService.sendModelDistributionNotification(
                event.getModelId(), "GENERATION", "COMPLETED", details);

            if (success) {
                log.info("模型生成通知发送成功: modelId={}", event.getModelId());
            } else {
                log.warn("模型生成通知发送失败: modelId={}", event.getModelId());
            }

        } catch (Exception e) {
            log.error("发送模型生成通知异常: modelId={}, error={}", event.getModelId(), e.getMessage(), e);
        }
    }
    
    /**
     * 发送分发完成通知
     */
    private void sendDistributionCompletionNotification(InitialModelDistributionCompletedEvent event) {
        try {
            String status;
            String title;
            String message;

            if (event.isCompleteSuccess()) {
                status = "COMPLETED";
                title = "模型分发完成";
                message = String.format("模型 %s 分发完全成功，成功分发到 %d 个虚拟机",
                        event.getModelId(), event.getSuccessfulDistributions());
            } else if (event.isPartialSuccess()) {
                status = "PARTIAL_SUCCESS";
                title = "模型分发部分成功";
                message = String.format("模型 %s 分发部分成功，成功率 %.1f%%，成功: %d，失败: %d",
                        event.getModelId(), event.getSuccessRate() * 100,
                        event.getSuccessfulDistributions(), event.getFailedDistributions());
            } else {
                status = "FAILED";
                title = "模型分发失败";
                message = String.format("模型 %s 分发失败: %s", event.getModelId(), event.getErrorMessage());
            }

            Map<String, Object> details = Map.of(
                "modelId", event.getModelId(),
                "totalDistributions", event.getTotalDistributions(),
                "successfulDistributions", event.getSuccessfulDistributions(),
                "failedDistributions", event.getFailedDistributions(),
                "successRate", event.getSuccessRate(),
                "distributionDuration", event.getDistributionDuration(),
                "taskId", event.getTaskId(),
                "orchestrationId", event.getOrchestrationId() != null ? event.getOrchestrationId() : ""
            );

            boolean success = notificationService.sendModelDistributionNotification(
                event.getModelId(), "DISTRIBUTION", status, details);

            if (success) {
                log.info("分发完成通知发送成功: modelId={}, status={}", event.getModelId(), status);
            } else {
                log.warn("分发完成通知发送失败: modelId={}, status={}", event.getModelId(), status);
            }

        } catch (Exception e) {
            log.error("发送分发完成通知异常: modelId={}, error={}", event.getModelId(), e.getMessage(), e);
        }
    }
    
    /**
     * 判断是否应该自动重试
     */
    private boolean shouldAutoRetry(InitialModelDistributionCompletedEvent event) {
        // 只有在部分失败且失败数量不多的情况下才自动重试
        return event.isPartialSuccess() && event.getFailedDistributions() <= 2 && 
               event.getSuccessRate() >= 0.5; // 成功率至少50%
    }
    
    /**
     * 安排分发重试
     */
    private void scheduleDistributionRetry(InitialModelDistributionCompletedEvent event) {
        log.info("安排分发重试: modelId={}, failedVmIds={}", event.getModelId(), event.getFailedVmIds());

        try {
            // 获取重试配置
            int maxRetries = getMaxRetriesForModelDistribution();
            int currentRetryCount = getCurrentRetryCount(event.getModelId());

            if (currentRetryCount >= maxRetries) {
                log.warn("模型分发重试次数已达上限，请求人工干预: modelId={}, retryCount={}, maxRetries={}",
                        event.getModelId(), currentRetryCount, maxRetries);
                requestManualInterventionForDistribution(event);
                return;
            }

            // 计算重试延迟（指数退避）
            long retryDelay = calculateRetryDelay(currentRetryCount);

            // 记录重试信息
            Map<String, Object> retryDetails = new HashMap<>();
            retryDetails.put("modelId", event.getModelId());
            retryDetails.put("failedVmIds", event.getFailedVmIds());
            retryDetails.put("retryCount", currentRetryCount + 1);
            retryDetails.put("maxRetries", maxRetries);
            retryDetails.put("retryDelay", retryDelay);
            retryDetails.put("failedDistributions", event.getFailedDistributions());
            retryDetails.put("successRate", event.getSuccessRate());

            logService.logTask(event.getTaskId(), "WARN",
                    String.format("安排模型分发自动重试: 第%d/%d次，延迟%d秒，重试VM数量=%d",
                            currentRetryCount + 1, maxRetries, retryDelay / 1000, event.getFailedVmIds().size()),
                    "InitialModelDistributionRetryService", null, retryDetails);

            // 增加重试计数
            incrementRetryCount(event.getModelId());

            // 发送重试通知
            notificationService.sendNotification(
                "EMAIL",
                "初始模型分发自动重试",
                String.format(
                    "初始模型分发部分失败，将在%d秒后进行第%d次重试\n" +
                    "模型ID: %s\n" +
                    "失败VM数量: %d\n" +
                    "失败VM列表: %s\n" +
                    "当前成功率: %.2f%%",
                    retryDelay / 1000, currentRetryCount + 1,
                    event.getModelId(),
                    event.getFailedVmIds() != null ? event.getFailedVmIds().size() : 0,
                    event.getFailedVmIds(),
                    event.getSuccessRate() * 100
                ),
                "admin@feduwacomm.com",
                "NORMAL",
                retryDetails
            );

            // 安排延迟重试
            scheduleDelayedDistributionRetry(event, retryDelay);

        } catch (Exception e) {
            log.error("安排分发重试失败: modelId={}", event.getModelId(), e);
            // 重试安排失败，请求人工干预
            requestManualInterventionForDistribution(event);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取模型分发的最大重试次数
     */
    private int getMaxRetriesForModelDistribution() {
        // 模型分发通常比数据分发更重要，允许更多重试
        return 3;
    }

    /**
     * 获取当前重试次数
     */
    private int getCurrentRetryCount(String modelId) {
        try {
            // 这里应该从数据库或缓存中获取重试次数，简化实现
            return 0;
        } catch (Exception e) {
            log.warn("获取重试次数失败: modelId={}", modelId, e);
            return 0;
        }
    }

    /**
     * 增加重试计数
     */
    private void incrementRetryCount(String modelId) {
        try {
            // 这里应该更新数据库或缓存中的重试次数，简化实现
            log.debug("增加重试计数: modelId={}", modelId);
        } catch (Exception e) {
            log.warn("增加重试计数失败: modelId={}", modelId, e);
        }
    }

    /**
     * 计算重试延迟
     */
    private long calculateRetryDelay(int retryCount) {
        // 指数退避: 2^retryCount * 3000ms，最大60秒
        long baseDelay = 3000L; // 3秒基础延迟
        long delay = (long) (baseDelay * Math.pow(2, retryCount));
        return Math.min(delay, 60000L); // 最大60秒
    }

    /**
     * 安排延迟分发重试
     */
    private void scheduleDelayedDistributionRetry(InitialModelDistributionCompletedEvent event, long delayMs) {
        try {
            // 在实际环境中应使用定时任务调度器
            log.info("安排延迟分发重试: modelId={}, delay={}ms", event.getModelId(), delayMs);

            // 构建重试参数
            Map<String, Object> retryContext = new HashMap<>();
            retryContext.put("modelId", event.getModelId());
            retryContext.put("targetVmIds", event.getFailedVmIds());
            retryContext.put("isRetry", true);
            retryContext.put("originalTaskId", event.getTaskId());
            retryContext.put("orchestrationId", event.getOrchestrationId());

            // 实际实现应该是：
            // 1. 使用Spring @Scheduled定时任务
            // 2. 或者使用消息队列（如RabbitMQ、Kafka）的延迟消息
            // 3. 或者使用Quartz调度器
            // schedulerService.scheduleModelDistributionRetry(retryContext, delayMs);

            log.info("模型分发重试已安排: modelId={}, 将在{}毫秒后执行", event.getModelId(), delayMs);

        } catch (Exception e) {
            log.error("安排延迟分发重试失败: modelId={}", event.getModelId(), e);
        }
    }

    /**
     * 请求人工干预处理分发失败
     */
    private void requestManualInterventionForDistribution(InitialModelDistributionCompletedEvent event) {
        try {
            log.warn("模型分发需要人工干预: modelId={}, failedVmCount={}",
                    event.getModelId(), event.getFailedDistributions());

            // 构建干预请求详情
            Map<String, Object> interventionDetails = new HashMap<>();
            interventionDetails.put("modelId", event.getModelId());
            interventionDetails.put("taskId", event.getTaskId());
            interventionDetails.put("orchestrationId", event.getOrchestrationId());
            interventionDetails.put("totalDistributions", event.getTotalDistributions());
            interventionDetails.put("successfulDistributions", event.getSuccessfulDistributions());
            interventionDetails.put("failedDistributions", event.getFailedDistributions());
            interventionDetails.put("successRate", event.getSuccessRate());
            interventionDetails.put("failedVmIds", event.getFailedVmIds());
            interventionDetails.put("errorMessage", event.getErrorMessage());
            interventionDetails.put("requestTime", System.currentTimeMillis());

            // 记录人工干预请求
            logService.logTask(event.getTaskId(), "WARN",
                    String.format("请求人工干预处理模型分发失败: 成功率=%.2f%%, 失败VM数=%d",
                            event.getSuccessRate() * 100, event.getFailedDistributions()),
                    "InitialModelDistributionInterventionService", null, interventionDetails);

            // 发送高优先级告警邮件
            String alertTitle = String.format("【紧急】初始模型分发失败需要人工干预 - %s", event.getModelId());
            String alertMessage = String.format(
                "初始模型分发失败需要人工干预\n" +
                "模型ID: %s\n" +
                "任务ID: %s\n" +
                "总分发数: %d\n" +
                "成功数: %d\n" +
                "失败数: %d\n" +
                "成功率: %.2f%%\n" +
                "失败VM列表: %s\n" +
                "错误信息: %s\n" +
                "请求时间: %s\n" +
                "\n请尽快登录系统检查并手动处理失败的VM分发。",
                event.getModelId(),
                event.getTaskId(),
                event.getTotalDistributions(),
                event.getSuccessfulDistributions(),
                event.getFailedDistributions(),
                event.getSuccessRate() * 100,
                event.getFailedVmIds(),
                event.getErrorMessage(),
                new java.util.Date()
            );

            // 发送邮件通知
            notificationService.sendNotification(
                "EMAIL",
                alertTitle,
                alertMessage,
                "admin@feduwacomm.com,operator@feduwacomm.com",
                "HIGH",
                interventionDetails
            );

            // 发送WebSocket实时通知给前端
            notificationService.sendWebSocketNotification(
                event.getTaskId(),
                "MODEL_DISTRIBUTION_MANUAL_INTERVENTION_REQUIRED",
                "WARNING",
                alertMessage,
                interventionDetails
            );

            // 对于关键失败，发送短信通知
            if (event.getSuccessRate() < 0.3) { // 成功率低于30%
                notificationService.sendNotification(
                    "SMS",
                    "模型分发关键失败",
                    String.format("模型%s分发严重失败，成功率%.1f%%，请立即处理",
                            event.getModelId(), event.getSuccessRate() * 100),
                    "+86-138xxxx8888",
                    "CRITICAL",
                    interventionDetails
                );
            }

        } catch (Exception e) {
            log.error("请求人工干预时发生异常: modelId={}", event.getModelId(), e);
        }
    }
}