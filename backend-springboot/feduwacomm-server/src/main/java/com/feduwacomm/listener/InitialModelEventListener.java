package com.feduwacomm.listener;

import com.feduwacomm.event.InitialModelDistributionCompletedEvent;
import com.feduwacomm.event.InitialModelDistributionStartedEvent;
import com.feduwacomm.event.InitialModelGeneratedEvent;
import com.feduwacomm.service.LogService;
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
                // TODO: 触发工作流下一阶段（数据分发）
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
                // TODO: 更新工作流阶段状态
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
                    // TODO: 触发工作流下一阶段（联邦训练）
                } else {
                    log.error("初始模型分发失败，工作流可能需要处理异常: orchestrationId={}, error={}", 
                            event.getOrchestrationId(), event.getErrorMessage());
                    // TODO: 处理工作流异常
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
        // TODO: 实现通知逻辑（邮件、WebSocket、消息队列等）
        log.debug("发送模型生成通知: modelId={}, createdBy={}", event.getModelId(), event.getCreatedBy());
    }
    
    /**
     * 发送分发完成通知
     */
    private void sendDistributionCompletionNotification(InitialModelDistributionCompletedEvent event) {
        // TODO: 实现通知逻辑
        if (event.isCompleteSuccess()) {
            log.debug("发送分发成功通知: modelId={}", event.getModelId());
        } else if (event.isPartialSuccess()) {
            log.debug("发送分发部分成功通知: modelId={}, successRate={}%", 
                    event.getModelId(), event.getSuccessRate() * 100);
        } else {
            log.debug("发送分发失败通知: modelId={}, error={}", event.getModelId(), event.getErrorMessage());
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
        // TODO: 实现重试逻辑
        // 可以使用Spring的@Scheduled或消息队列来实现延迟重试
        log.info("安排分发重试: modelId={}, failedVmIds={}", event.getModelId(), event.getFailedVmIds());
    }
}