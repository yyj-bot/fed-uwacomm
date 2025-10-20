package com.feduwacomm.listener;

import com.feduwacomm.common.BaseContext;
import com.feduwacomm.entity.ModelDistribution;
import com.feduwacomm.enums.InitialModelStatus;
import com.feduwacomm.event.InitialModelDistributionCompletedEvent;
import com.feduwacomm.event.InitialModelDistributionStartedEvent;
import com.feduwacomm.event.InitialModelGeneratedEvent;
import com.feduwacomm.mapper.InitialModelMapper;
import com.feduwacomm.mapper.ModelDistributionMapper;
import com.feduwacomm.service.LogService;
import com.feduwacomm.service.NotificationService;
import com.feduwacomm.service.RetryService;
import com.feduwacomm.service.WorkflowStageTransitionService;
import com.feduwacomm.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

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
    private final InitialModelMapper initialModelMapper;
    private final ModelDistributionMapper modelDistributionMapper;
    private final WebSocketService webSocketService;
    private final RetryService retryService;

    private static final String RETRY_TYPE_INITIAL_MODEL = "initial_model_distribution";
    
    /**
     * 处理初始模型生成完成事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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
            
            // 更新分发记录和模型状态
            persistDistributionOutcome(event);

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

            // 推送任务侧提示
            pushTaskDistributionUpdate(event);
            
            if (event.isCompleteSuccess()) {
                retryService.resetRetryCount(RETRY_TYPE_INITIAL_MODEL, event.getModelId());
            } else if (event.isPartialSuccess() || !event.isSuccess()) {
                handleDistributionRetry(event);
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
     * 持久化分发结果，更新模型与分发记录状态
     */
    private void persistDistributionOutcome(InitialModelDistributionCompletedEvent event) {
        LocalDateTime now = LocalDateTime.now();

        if (event.isSuccess()) {
            log.info("初始模型分发成功VM列表: {}", event.getSuccessfulVmIds());
            log.info("初始模型分发失败VM列表: {}", event.getFailedVmIds());
            // 更新成功与失败的分发记录
            updateDistributionStatus(event.getModelId(), event.getSuccessfulVmIds(), "COMPLETED", now, null, true);
            updateDistributionStatus(event.getModelId(), event.getFailedVmIds(), "FAILED", now, event.getErrorMessage(), false);

            String operator = resolveOperatorId();
            if (event.isCompleteSuccess()) {
                initialModelMapper.updateStatus(event.getModelId(), InitialModelStatus.DISTRIBUTED.getCode(), operator);
            } else if (event.isPartialSuccess()) {
                initialModelMapper.updateStatus(event.getModelId(), InitialModelStatus.READY.getCode(), operator);
            }
        } else {
            // 全量标记为失败
            markAllDistributionsFailed(event.getModelId(), event.getErrorMessage(), now);
            initialModelMapper.updateStatus(event.getModelId(), InitialModelStatus.READY.getCode(), resolveOperatorId());
        }
    }

    private void updateDistributionStatus(String modelId,
                                          List<String> vmIds,
                                          String status,
                                          LocalDateTime timestamp,
                                          String errorMessage,
                                          boolean verified) {
        if (CollectionUtils.isEmpty(vmIds)) {
            return;
        }
        for (String vmId : vmIds) {
            try {
                ModelDistribution record = modelDistributionMapper.selectByModelIdAndVmId(modelId, vmId);
                if (record == null) {
                    log.warn("分发记录查询为空: modelId={}, vmId={}", modelId, vmId);
                    continue;
                }
                log.info("更新分发状态: modelId={}, vmId={}, recordId={}, targetStatus={}", modelId, vmId, record.getId(), status);
                int statusUpdated = modelDistributionMapper.updateDistributionStatus(
                        record.getId(), status, timestamp, errorMessage);
                if (statusUpdated == 0) {
                    log.warn("分发状态更新未生效: recordId={}, targetStatus={}, modelId={}, vmId={}",
                            record.getId(), status, modelId, vmId);
                }

                Boolean verificationResult = verified ? Boolean.TRUE : Boolean.FALSE;
                LocalDateTime verificationTime = verified ? timestamp : null;
                int verificationUpdated = modelDistributionMapper.updateVerificationStatus(
                        record.getId(), verificationResult, verificationTime);
                if (verificationUpdated == 0) {
                    log.warn("分发记录校验状态更新未生效: recordId={}, verified={}, modelId={}, vmId={}",
                            record.getId(), verificationResult, modelId, vmId);
                }
            } catch (Exception e) {
                log.error("更新模型分发记录失败: modelId={}, vmId={}, status={}, error={}",
                    modelId, vmId, status, e.getMessage(), e);
            }
        }
    }

    private void markAllDistributionsFailed(String modelId, String errorMessage, LocalDateTime timestamp) {
        try {
            List<Map<String, Object>> records = modelDistributionMapper.selectByModelIdWithVmInfo(modelId);
            for (Map<String, Object> record : records) {
                String id = Objects.toString(record.get("id"), null);
                if (id != null) {
                    modelDistributionMapper.updateDistributionStatus(id, "FAILED", timestamp, errorMessage);
                    modelDistributionMapper.updateVerificationStatus(id, Boolean.FALSE, null);
                }
            }
        } catch (Exception e) {
            log.error("批量更新模型分发失败状态异常: modelId={}, error={}", modelId, e.getMessage(), e);
        }
    }

    private String resolveOperatorId() {
        String operator = BaseContext.getCurrentId();
        return operator != null ? operator : "SYSTEM";
    }

    private void pushTaskDistributionUpdate(InitialModelDistributionCompletedEvent event) {
        if (!StringUtils.hasText(event.getTaskId())) {
            return;
        }
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "INITIAL_MODEL_DISTRIBUTION");
            message.put("modelId", event.getModelId());
            message.put("status", event.isCompleteSuccess() ? "COMPLETED" :
                    event.isPartialSuccess() ? "PARTIAL_SUCCESS" : (event.isSuccess() ? "SUCCESS" : "FAILED"));
            message.put("total", event.getTotalDistributions());
            message.put("success", event.getSuccessfulDistributions());
            message.put("failed", event.getFailedDistributions());
            message.put("successRate", event.getSuccessRate());
            message.put("duration", event.getDuration());
            message.put("eventTime", event.getEventTime());

            Map<String, Object> refreshedStats = modelDistributionMapper.getDistributionProgress(event.getModelId());
            if (refreshedStats != null) {
                message.put("distributionStats", refreshedStats);
            }
            if (event.getErrorMessage() != null) {
                message.put("errorMessage", event.getErrorMessage());
            }
            if (!CollectionUtils.isEmpty(event.getSuccessfulVmIds())) {
                message.put("successfulVmIds", event.getSuccessfulVmIds());
            }
            if (!CollectionUtils.isEmpty(event.getFailedVmIds())) {
                message.put("failedVmIds", event.getFailedVmIds());
            }

            webSocketService.sendToTaskSubscribers(event.getTaskId(), message);
        } catch (Exception e) {
            log.error("推送任务端初始模型分发提示失败: taskId={}, error={}", event.getTaskId(), e.getMessage(), e);
        }
    }
    
    private void handleDistributionRetry(InitialModelDistributionCompletedEvent event) {
        List<String> retryTargets = resolveRetryTargets(event);
        if (CollectionUtils.isEmpty(retryTargets)) {
            log.warn("未找到可重试的目标虚拟机: modelId={}, taskId={}", event.getModelId(), event.getTaskId());
            retryService.resetRetryCount(RETRY_TYPE_INITIAL_MODEL, event.getModelId());
            if (!event.isSuccess()) {
                requestManualInterventionForDistribution(event);
            }
            return;
        }

        if (event.isPartialSuccess() && !shouldRetryPartialDistribution(event)) {
            log.info("部分成功但不满足自动重试条件: modelId={}, failedCount={}, successRate={}",
                    event.getModelId(), event.getFailedDistributions(), event.getSuccessRate());
            retryService.resetRetryCount(RETRY_TYPE_INITIAL_MODEL, event.getModelId());
            requestManualInterventionForDistribution(event);
            return;
        }

        int currentAttempt = retryService.getRetryCount(RETRY_TYPE_INITIAL_MODEL, event.getModelId());
        String retryReason = resolveRetryErrorMessage(event);
        RetryService.RetryResult retryDecision = retryService.shouldRetryDistribution(
                event.getModelId(), currentAttempt, retryReason);

        if (!retryDecision.shouldRetry()) {
            log.warn("重试服务拒绝自动重试: modelId={}, attempt={}, reason={}",
                    event.getModelId(), currentAttempt, retryDecision.getReason());
            retryService.resetRetryCount(RETRY_TYPE_INITIAL_MODEL, event.getModelId());
            requestManualInterventionForDistribution(event);
            return;
        }

        Map<String, Object> retryDetails = new HashMap<>();
        retryDetails.put("modelId", event.getModelId());
        retryDetails.put("taskId", event.getTaskId());
        retryDetails.put("orchestrationId", event.getOrchestrationId());
        retryDetails.put("failedVmIds", retryTargets);
        retryDetails.put("retryReason", retryReason);
        retryDetails.put("failedDistributions", event.getFailedDistributions());
        retryDetails.put("successDistributions", event.getSuccessfulDistributions());
        retryDetails.put("successRate", event.getSuccessRate());
        retryDetails.put("scheduledAt", System.currentTimeMillis());

        logService.logTask(event.getTaskId(), "WARN",
                String.format("安排初始模型分发自动重试: 第%d次，延迟%d毫秒，目标VM=%d",
                        retryDecision.getNextAttempt(), retryDecision.getDelayMillis(), retryTargets.size()),
                "InitialModelDistributionRetryService", null, retryDetails);

        retryService.scheduleRetry(
                RETRY_TYPE_INITIAL_MODEL,
                event.getModelId(),
                retryDecision.getDelayMillis(),
                retryDecision.getNextAttempt(),
                new HashMap<>(retryDetails)
        );
    }

    private boolean shouldRetryPartialDistribution(InitialModelDistributionCompletedEvent event) {
        return event.getFailedDistributions() <= 2 && event.getSuccessRate() >= 0.5;
    }

    private List<String> resolveRetryTargets(InitialModelDistributionCompletedEvent event) {
        if (!CollectionUtils.isEmpty(event.getFailedVmIds())) {
            return event.getFailedVmIds().stream()
                    .filter(StringUtils::hasText)
                    .distinct()
                    .collect(Collectors.toList());
        }

        try {
            List<Map<String, Object>> records = modelDistributionMapper.selectByModelIdWithVmInfo(event.getModelId());
            if (CollectionUtils.isEmpty(records)) {
                return Collections.emptyList();
            }
            return records.stream()
                    .map(record -> {
                        Object vmId = record.get("vm_id");
                        if (vmId == null) {
                            vmId = record.get("vmId");
                        }
                        return vmId != null ? vmId.toString() : null;
                    })
                    .filter(StringUtils::hasText)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("查询模型分发记录失败，无法确定重试目标: modelId={}", event.getModelId(), e);
            return Collections.emptyList();
        }
    }

    private String resolveRetryErrorMessage(InitialModelDistributionCompletedEvent event) {
        if (StringUtils.hasText(event.getErrorMessage())) {
            return event.getErrorMessage();
        }
        if (event.isPartialSuccess()) {
            return "partial model distribution failure due to network instability";
        }
        return "initial model distribution temporary failure";
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
