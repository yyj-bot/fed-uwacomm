package com.feduwacomm.listener;

import com.feduwacomm.event.WorkflowStageCompletedEvent;
import com.feduwacomm.event.WorkflowStageStartedEvent;
import com.feduwacomm.service.LogService;
import com.feduwacomm.service.FederatedOrchestrationService;
import com.feduwacomm.service.NotificationService;
import com.feduwacomm.service.PerformanceMonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流事件监听器
 * 处理工作流阶段执行相关的事件
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowEventListener {

    private final LogService logService;
    private final FederatedOrchestrationService orchestrationService;
    private final NotificationService notificationService;
    private final PerformanceMonitorService performanceMonitorService;

    // 内存缓存存储阶段开始时间（实际生产环境应使用Redis）
    private final Map<String, Long> stageStartTimeCache = new ConcurrentHashMap<>();
    
    /**
     * 处理工作流阶段开始事件
     */
    @EventListener
    @Async
    public void handleWorkflowStageStarted(WorkflowStageStartedEvent event) {
        log.info("处理工作流阶段开始事件: {}", event);
        
        try {
            // 记录系统日志
            Map<String, Object> logDetails = new HashMap<>();
            logDetails.put("orchestrationId", event.getOrchestrationId());
            logDetails.put("stageName", event.getStageName());
            logDetails.put("stageOrder", event.getStageOrder());
            logDetails.put("inputData", event.getInputData());
            logDetails.put("stageConfig", event.getStageConfig());
            logDetails.put("operator", event.getOperator());
            logDetails.put("eventTime", event.getEventTime());
            
            logService.logTask(event.getTaskId(), "INFO", 
                    String.format("工作流阶段开始执行: 阶段=%s (顺序=%d), 操作者=%s", 
                            event.getStageName(), event.getStageOrder(), event.getOperator()),
                    "WorkflowOrchestrator", null, logDetails);
            
            // 更新工作流状态
            updateWorkflowStageStatus(event.getOrchestrationId(), event.getStageName(), "IN_PROGRESS");
            
            // 记录阶段开始时间（用于计算执行耗时）
            recordStageStartTime(event.getOrchestrationId(), event.getStageName());
            
            // 发送阶段开始通知
            sendStageStartNotification(event);
            
        } catch (Exception e) {
            log.error("处理工作流阶段开始事件失败: {}", event, e);
        }
    }
    
    /**
     * 处理工作流阶段完成事件
     */
    @EventListener
    @Async
    public void handleWorkflowStageCompleted(WorkflowStageCompletedEvent event) {
        log.info("处理工作流阶段完成事件: {}", event);
        
        try {
            // 记录系统日志
            Map<String, Object> logDetails = new HashMap<>();
            logDetails.put("orchestrationId", event.getOrchestrationId());
            logDetails.put("stageName", event.getStageName());
            logDetails.put("stageOrder", event.getStageOrder());
            logDetails.put("success", event.isSuccess());
            logDetails.put("outputData", event.getOutputData());
            logDetails.put("executionDuration", event.getExecutionDuration());
            logDetails.put("errorMessage", event.getErrorMessage());
            logDetails.put("nextStageName", event.getNextStageName());
            logDetails.put("eventTime", event.getEventTime());
            
            String logLevel = event.isSuccess() ? "INFO" : "ERROR";
            String logMessage;
            
            if (event.isSuccess()) {
                logMessage = String.format("工作流阶段执行成功: 阶段=%s (顺序=%d), 耗时=%dms, 下一阶段=%s", 
                        event.getStageName(), event.getStageOrder(), event.getExecutionDuration(),
                        event.getNextStageName() != null ? event.getNextStageName() : "无");
            } else {
                logMessage = String.format("工作流阶段执行失败: 阶段=%s (顺序=%d), 耗时=%dms, 错误=%s", 
                        event.getStageName(), event.getStageOrder(), event.getExecutionDuration(), 
                        event.getErrorMessage());
            }
            
            logService.logTask(event.getTaskId(), logLevel, logMessage, 
                    "WorkflowOrchestrator", null, logDetails);
            
            // 更新工作流状态
            String stageStatus = event.isSuccess() ? "COMPLETED" : "FAILED";
            updateWorkflowStageStatus(event.getOrchestrationId(), event.getStageName(), stageStatus);
            
            // 如果成功且有下一阶段，准备触发下一阶段
            if (event.isSuccess() && event.hasNextStage()) {
                log.info("准备触发工作流下一阶段: current={}, next={}, orchestrationId={}", 
                        event.getStageName(), event.getNextStageName(), event.getOrchestrationId());
                
                scheduleNextStage(event);
            } else if (event.isSuccess() && !event.hasNextStage()) {
                // 工作流完成
                log.info("工作流执行完成: orchestrationId={}", event.getOrchestrationId());
                completeWorkflow(event.getOrchestrationId(), event.getTaskId());
            } else {
                // 阶段失败，处理工作流异常
                log.error("工作流阶段失败，需要处理异常: stage={}, orchestrationId={}, error={}", 
                        event.getStageName(), event.getOrchestrationId(), event.getErrorMessage());
                
                handleWorkflowStageFailure(event);
            }
            
            // 发送阶段完成通知
            sendStageCompletionNotification(event);
            
            // 更新性能指标
            updatePerformanceMetrics(event);
            
        } catch (Exception e) {
            log.error("处理工作流阶段完成事件失败: {}", event, e);
        }
    }
    
    /**
     * 更新工作流阶段状态
     */
    private void updateWorkflowStageStatus(String orchestrationId, String stageName, String status) {
        try {
            log.debug("更新工作流阶段状态: orchestrationId={}, stage={}, status={}",
                    orchestrationId, stageName, status);

            // 调用工作流编排服务更新阶段状态
            boolean updated = orchestrationService.updateStageStatus(orchestrationId, stageName, status);
            if (updated) {
                log.info("工作流阶段状态更新成功: orchestrationId={}, stage={}, status={}",
                        orchestrationId, stageName, status);
            } else {
                log.warn("工作流阶段状态更新失败，可能编排不存在: orchestrationId={}, stage={}, status={}",
                        orchestrationId, stageName, status);
            }

        } catch (Exception e) {
            log.error("更新工作流阶段状态失败: orchestrationId={}, stage={}, status={}",
                    orchestrationId, stageName, status, e);
        }
    }
    
    /**
     * 记录阶段开始时间
     */
    private void recordStageStartTime(String orchestrationId, String stageName) {
        try {
            // 使用内存缓存记录开始时间，用于计算执行耗时
            // 在生产环境中应该使用Redis或其他分布式缓存
            String key = String.format("stage:start:%s:%s", orchestrationId, stageName);
            long startTime = System.currentTimeMillis();

            stageStartTimeCache.put(key, startTime);
            log.debug("记录阶段开始时间: key={}, startTime={}", key, startTime);

            // 为了防止内存泄漏，设置清理机制（生产环境应使用TTL）
            cleanupOldStageStartTimes();

        } catch (Exception e) {
            log.warn("记录阶段开始时间失败: orchestrationId={}, stageName={}", orchestrationId, stageName, e);
        }
    }

    /**
     * 清理过期的阶段开始时间记录
     */
    private void cleanupOldStageStartTimes() {
        try {
            long currentTime = System.currentTimeMillis();
            long expireTime = 24 * 60 * 60 * 1000L; // 24小时过期

            stageStartTimeCache.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > expireTime);

        } catch (Exception e) {
            log.warn("清理过期阶段开始时间记录失败", e);
        }
    }
    
    /**
     * 安排下一阶段执行
     */
    private void scheduleNextStage(WorkflowStageCompletedEvent event) {
        try {
            log.info("安排下一阶段执行: orchestrationId={}, nextStage={}",
                    event.getOrchestrationId(), event.getNextStageName());

            // 短暂延迟，确保当前阶段的清理工作完成
            Thread.sleep(500);

            // 准备下一阶段的输入数据
            Map<String, Object> nextStageInput = new HashMap<>();
            if (event.getOutputData() != null) {
                nextStageInput.putAll(event.getOutputData());
            }
            nextStageInput.put("previousStage", event.getStageName());
            nextStageInput.put("previousStageOutput", event.getOutputData());

            // 调用工作流编排服务触发下一阶段
            orchestrationService.triggerNextStage(
                event.getOrchestrationId(),
                "WorkflowEventListener",
                nextStageInput
            );
            boolean triggered = true;

            if (triggered) {
                log.info("下一阶段触发成功: orchestrationId={}, nextStage={}",
                        event.getOrchestrationId(), event.getNextStageName());
            } else {
                log.error("下一阶段触发失败: orchestrationId={}, nextStage={}",
                        event.getOrchestrationId(), event.getNextStageName());
            }

        } catch (Exception e) {
            log.error("安排下一阶段执行失败: orchestrationId={}, nextStage={}",
                    event.getOrchestrationId(), event.getNextStageName(), e);
        }
    }
    
    /**
     * 完成工作流
     */
    private void completeWorkflow(String orchestrationId, String taskId) {
        try {
            log.info("完成工作流: orchestrationId={}, taskId={}", orchestrationId, taskId);
            
            // 记录工作流完成日志
            Map<String, Object> logDetails = new HashMap<>();
            logDetails.put("orchestrationId", orchestrationId);
            logDetails.put("completionTime", System.currentTimeMillis());
            
            logService.logTask(taskId, "INFO", "工作流执行完成",
                    "WorkflowOrchestrator", null, logDetails);

            // 更新工作流状态为COMPLETED
            boolean updated = orchestrationService.updateOrchestrationStatus(orchestrationId, "COMPLETED");
            if (updated) {
                log.info("工作流状态更新为COMPLETED成功: orchestrationId={}", orchestrationId);
            } else {
                log.warn("工作流状态更新为COMPLETED失败: orchestrationId={}", orchestrationId);
            }

            // 发送工作流完成通知
            notificationService.sendWorkflowNotification(
                orchestrationId,
                "WORKFLOW_COMPLETED",
                "COMPLETED",
                String.format("工作流 %s 执行完成", orchestrationId)
            );
            
        } catch (Exception e) {
            log.error("完成工作流失败: orchestrationId={}, taskId={}", orchestrationId, taskId, e);
        }
    }
    
    /**
     * 处理工作流阶段失败
     */
    private void handleWorkflowStageFailure(WorkflowStageCompletedEvent event) {
        try {
            log.error("处理工作流阶段失败: orchestrationId={}, stage={}, error={}", 
                    event.getOrchestrationId(), event.getStageName(), event.getErrorMessage());
            
            // 根据失败策略处理
            // 1. 重试当前阶段 - 适用于临时性错误，如网络超时、资源暂时不可用
            // 2. 跳过当前阶段继续下一阶段 - 适用于非关键阶段失败
            // 3. 终止整个工作流 - 适用于严重错误，如数据损坏、权限不足
            // 4. 回滚到上一阶段 - 适用于需要重新处理的情况
            
            String failureStrategy = determineFailureStrategy(event);
            
            switch (failureStrategy) {
                case "RETRY":
                    scheduleStageRetry(event);
                    break;
                case "SKIP":
                    skipToNextStage(event);
                    break;
                case "TERMINATE":
                    terminateWorkflow(event.getOrchestrationId(), event.getTaskId(), event.getErrorMessage());
                    break;
                case "ROLLBACK":
                    rollbackToPreviousStage(event);
                    break;
                default:
                    log.warn("未知的失败处理策略: {}", failureStrategy);
                    terminateWorkflow(event.getOrchestrationId(), event.getTaskId(), event.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("处理工作流阶段失败时发生异常", e);
        }
    }
    
    /**
     * 确定失败处理策略
     */
    private String determineFailureStrategy(WorkflowStageCompletedEvent event) {
        // 根据阶段类型、错误类型、重试次数等确定策略
        String stageName = event.getStageName();
        String errorMessage = event.getErrorMessage();
        int currentRetryCount = getCurrentRetryCount(event.getOrchestrationId(), stageName);

        // 基于阶段类型的策略决定
        FailureStrategy strategy = determineStrategyByStageType(stageName, errorMessage, currentRetryCount);

        // 基于错误类型的策略调整
        strategy = adjustStrategyByErrorType(strategy, errorMessage);

        // 基于重试次数的策略调整
        strategy = adjustStrategyByRetryCount(strategy, currentRetryCount);

        return strategy.name();
    }

    /**
     * 失败策略枚举
     */
    private enum FailureStrategy {
        RETRY,      // 重试
        SKIP,       // 跳过
        TERMINATE,  // 终止
        ROLLBACK    // 回滚
    }

    /**
     * 获取当前重试次数
     */
    private int getCurrentRetryCount(String orchestrationId, String stageName) {
        // 这里应该从缓存或数据库查询重试次数
        // 为简化实现，这里返回模拟值
        String retryKey = orchestrationId + "_" + stageName + "_retry_count";
        // 模拟从缓存获取重试次数
        return 0; // 默认返回0
    }

    /**
     * 根据阶段类型确定策略
     */
    private FailureStrategy determineStrategyByStageType(String stageName, String errorMessage, int retryCount) {
        if (stageName.contains("DISTRIBUTION")) {
            return FailureStrategy.RETRY; // 分发阶段优先重试
        } else if (stageName.contains("VALIDATION")) {
            return FailureStrategy.SKIP; // 验证阶段可以跳过
        } else if (stageName.contains("TRAINING")) {
            return FailureStrategy.RETRY; // 训练阶段优先重试
        } else if (stageName.contains("AGGREGATION")) {
            return FailureStrategy.RETRY; // 聚合阶段优先重试
        } else {
            return FailureStrategy.TERMINATE; // 其他阶段默认终止
        }
    }

    /**
     * 根据错误类型调整策略
     */
    private FailureStrategy adjustStrategyByErrorType(FailureStrategy currentStrategy, String errorMessage) {
        if (errorMessage == null) {
            return currentStrategy;
        }

        if (errorMessage.contains("timeout")) {
            return FailureStrategy.RETRY; // 超时错误可以重试
        } else if (errorMessage.contains("network")) {
            return FailureStrategy.RETRY; // 网络错误可以重试
        } else if (errorMessage.contains("invalid") || errorMessage.contains("corrupt")) {
            return FailureStrategy.SKIP; // 数据无效或损坏，跳过
        } else if (errorMessage.contains("critical") || errorMessage.contains("fatal")) {
            return FailureStrategy.TERMINATE; // 严重错误直接终止
        }

        return currentStrategy;
    }

    /**
     * 根据重试次数调整策略
     */
    private FailureStrategy adjustStrategyByRetryCount(FailureStrategy currentStrategy, int retryCount) {
        if (currentStrategy == FailureStrategy.RETRY) {
            if (retryCount >= 3) {
                return FailureStrategy.TERMINATE; // 重试超过3次，终止
            } else if (retryCount >= 2) {
                return FailureStrategy.SKIP; // 重试超过2次，跳过
            }
        }
        return currentStrategy;
    }
    
    /**
     * 安排阶段重试
     */
    private void scheduleStageRetry(WorkflowStageCompletedEvent event) {
        log.info("安排阶段重试: orchestrationId={}, stage={}",
                event.getOrchestrationId(), event.getStageName());

        try {
            // 获取重试配置
            int maxRetries = getMaxRetriesForStage(event.getStageName());
            int currentRetryCount = getCurrentRetryCount(event.getOrchestrationId(), event.getStageName());

            if (currentRetryCount >= maxRetries) {
                log.warn("阶段重试次数已达上限，终止工作流: stage={}, retryCount={}, maxRetries={}",
                        event.getStageName(), currentRetryCount, maxRetries);
                terminateWorkflow(event.getOrchestrationId(), event.getTaskId(),
                        "重试次数超过限制: " + event.getErrorMessage());
                return;
            }

            // 记录重试信息
            Map<String, Object> logDetails = new HashMap<>();
            logDetails.put("orchestrationId", event.getOrchestrationId());
            logDetails.put("stageName", event.getStageName());
            logDetails.put("retryCount", currentRetryCount + 1);
            logDetails.put("maxRetries", maxRetries);
            logDetails.put("originalError", event.getErrorMessage());

            logService.logTask(event.getTaskId(), "WARN",
                    String.format("安排阶段重试: 阶段=%s, 重试次数=%d/%d, 原始错误=%s",
                            event.getStageName(), currentRetryCount + 1, maxRetries, event.getErrorMessage()),
                    "WorkflowOrchestrator", null, logDetails);

            // 增加重试计数
            incrementRetryCount(event.getOrchestrationId(), event.getStageName());

            // 计算重试延迟（指数退避）
            long retryDelay = calculateRetryDelay(currentRetryCount);

            // 延迟后重新触发阶段
            Thread.sleep(retryDelay);

            // 准备重试的输入数据（保持原始输入）
            Map<String, Object> retryInput = new HashMap<>();
            // 注意：WorkflowStageCompletedEvent 通常不包含输入数据，应从其他地方获取
            // if (event.getInputData() != null) {
            //     retryInput.putAll(event.getInputData());
            // }
            retryInput.put("isRetry", true);
            retryInput.put("retryCount", currentRetryCount + 1);

            // 重新触发阶段
            boolean triggered = orchestrationService.triggerStage(
                event.getOrchestrationId(),
                event.getStageName(),
                retryInput
            );

            if (triggered) {
                log.info("阶段重试触发成功: orchestrationId={}, stage={}, retryCount={}",
                        event.getOrchestrationId(), event.getStageName(), currentRetryCount + 1);
            } else {
                log.error("阶段重试触发失败，终止工作流: orchestrationId={}, stage={}",
                        event.getOrchestrationId(), event.getStageName());
                terminateWorkflow(event.getOrchestrationId(), event.getTaskId(), "重试触发失败");
            }

        } catch (Exception e) {
            log.error("安排阶段重试失败: orchestrationId={}, stage={}",
                    event.getOrchestrationId(), event.getStageName(), e);
            terminateWorkflow(event.getOrchestrationId(), event.getTaskId(), "重试安排失败: " + e.getMessage());
        }
    }
    
    /**
     * 跳过到下一阶段
     */
    private void skipToNextStage(WorkflowStageCompletedEvent event) {
        log.info("跳过到下一阶段: orchestrationId={}, currentStage={}, nextStage={}",
                event.getOrchestrationId(), event.getStageName(), event.getNextStageName());

        try {
            // 检查是否有下一阶段
            if (!event.hasNextStage() || event.getNextStageName() == null) {
                log.warn("没有下一阶段可跳过，完成工作流: orchestrationId={}, currentStage={}",
                        event.getOrchestrationId(), event.getStageName());
                completeWorkflow(event.getOrchestrationId(), event.getTaskId());
                return;
            }

            // 记录跳过信息
            Map<String, Object> logDetails = new HashMap<>();
            logDetails.put("orchestrationId", event.getOrchestrationId());
            logDetails.put("skippedStage", event.getStageName());
            logDetails.put("nextStage", event.getNextStageName());
            logDetails.put("skipReason", event.getErrorMessage());

            logService.logTask(event.getTaskId(), "WARN",
                    String.format("跳过失败阶段: 跳过阶段=%s, 下一阶段=%s, 跳过原因=%s",
                            event.getStageName(), event.getNextStageName(), event.getErrorMessage()),
                    "WorkflowOrchestrator", null, logDetails);

            // 更新当前阶段状态为SKIPPED
            updateWorkflowStageStatus(event.getOrchestrationId(), event.getStageName(), "SKIPPED");

            // 准备下一阶段的输入数据（不包含失败阶段的输出）
            Map<String, Object> nextStageInput = new HashMap<>();
            // 注意：WorkflowStageCompletedEvent 通常不包含输入数据，应从其他地方获取
            // if (event.getInputData() != null) {
            //     nextStageInput.putAll(event.getInputData());
            // }

            // 添加跳过相关信息
            nextStageInput.put("skippedStage", event.getStageName());
            nextStageInput.put("skipReason", event.getErrorMessage());
            nextStageInput.put("hasSkippedStage", true);

            // 触发下一阶段
            orchestrationService.triggerNextStage(
                event.getOrchestrationId(),
                "WorkflowEventListener",
                nextStageInput
            );
            boolean triggered = true;

            if (triggered) {
                log.info("跳过后下一阶段触发成功: orchestrationId={}, skippedStage={}, nextStage={}",
                        event.getOrchestrationId(), event.getStageName(), event.getNextStageName());

                // 发送跳过通知
                notificationService.sendWorkflowNotification(
                    event.getOrchestrationId(),
                    "STAGE_SKIPPED",
                    "WARNING",
                    String.format("工作流阶段 %s 因错误被跳过，继续执行下一阶段 %s",
                            event.getStageName(), event.getNextStageName())
                );
            } else {
                log.error("跳过后下一阶段触发失败，终止工作流: orchestrationId={}, nextStage={}",
                        event.getOrchestrationId(), event.getNextStageName());
                terminateWorkflow(event.getOrchestrationId(), event.getTaskId(), "跳过后下一阶段触发失败");
            }

        } catch (Exception e) {
            log.error("跳过到下一阶段失败: orchestrationId={}, currentStage={}, nextStage={}",
                    event.getOrchestrationId(), event.getStageName(), event.getNextStageName(), e);
            terminateWorkflow(event.getOrchestrationId(), event.getTaskId(), "跳过阶段处理失败: " + e.getMessage());
        }
    }
    
    /**
     * 终止工作流
     */
    private void terminateWorkflow(String orchestrationId, String taskId, String reason) {
        log.error("终止工作流: orchestrationId={}, taskId={}, reason={}",
                orchestrationId, taskId, reason);

        try {
            // 记录工作流终止日志
            Map<String, Object> logDetails = new HashMap<>();
            logDetails.put("orchestrationId", orchestrationId);
            logDetails.put("terminationReason", reason);
            logDetails.put("terminationTime", System.currentTimeMillis());

            logService.logTask(taskId, "ERROR",
                    String.format("工作流被终止: 原因=%s", reason),
                    "WorkflowOrchestrator", null, logDetails);

            // 更新工作流状态为TERMINATED
            boolean updated = orchestrationService.updateOrchestrationStatus(orchestrationId, "TERMINATED");
            if (updated) {
                log.info("工作流状态更新为TERMINATED成功: orchestrationId={}", orchestrationId);
            } else {
                log.warn("工作流状态更新为TERMINATED失败: orchestrationId={}", orchestrationId);
            }

            // 清理相关资源
            cleanupWorkflowResources(orchestrationId);

            // 发送工作流终止通知
            notificationService.sendWorkflowNotification(
                orchestrationId,
                "WORKFLOW_TERMINATED",
                "ERROR",
                String.format("工作流 %s 因错误被终止: %s", orchestrationId, reason)
            );

            // 更新相关任务状态
            try {
                // 这里可以调用任务服务更新任务状态
                // taskService.updateTaskStatus(taskId, "FAILED", reason);
                log.info("工作流终止处理完成: orchestrationId={}, taskId={}", orchestrationId, taskId);
            } catch (Exception e) {
                log.warn("更新任务状态失败: taskId={}", taskId, e);
            }

        } catch (Exception e) {
            log.error("终止工作流处理失败: orchestrationId={}, taskId={}", orchestrationId, taskId, e);
        }
    }
    
    /**
     * 回滚到上一阶段
     */
    private void rollbackToPreviousStage(WorkflowStageCompletedEvent event) {
        log.info("回滚到上一阶段: orchestrationId={}, currentStage={}",
                event.getOrchestrationId(), event.getStageName());

        try {
            // 获取工作流阶段历史
            String previousStage = getPreviousStage(event.getOrchestrationId(), event.getStageName());

            if (previousStage == null) {
                log.warn("没有上一阶段可回滚，终止工作流: orchestrationId={}, currentStage={}",
                        event.getOrchestrationId(), event.getStageName());
                terminateWorkflow(event.getOrchestrationId(), event.getTaskId(), "无法回滚：没有上一阶段");
                return;
            }

            // 记录回滚信息
            Map<String, Object> logDetails = new HashMap<>();
            logDetails.put("orchestrationId", event.getOrchestrationId());
            logDetails.put("currentStage", event.getStageName());
            logDetails.put("previousStage", previousStage);
            logDetails.put("rollbackReason", event.getErrorMessage());

            logService.logTask(event.getTaskId(), "WARN",
                    String.format("回滚工作流: 当前阶段=%s, 回滚到=%s, 回滚原因=%s",
                            event.getStageName(), previousStage, event.getErrorMessage()),
                    "WorkflowOrchestrator", null, logDetails);

            // 更新当前阶段状态为ROLLED_BACK
            updateWorkflowStageStatus(event.getOrchestrationId(), event.getStageName(), "ROLLED_BACK");

            // 重置上一阶段状态为PENDING（准备重新执行）
            updateWorkflowStageStatus(event.getOrchestrationId(), previousStage, "PENDING");

            // 获取上一阶段的输入数据（如果有的话）
            Map<String, Object> rollbackInput = getPreviousStageInput(event.getOrchestrationId(), previousStage);
            if (rollbackInput == null) {
                rollbackInput = new HashMap<>();
            }

            // 添加回滚相关信息
            rollbackInput.put("isRollback", true);
            rollbackInput.put("rolledBackFromStage", event.getStageName());
            rollbackInput.put("rollbackReason", event.getErrorMessage());

            // 重新触发上一阶段
            boolean triggered = orchestrationService.triggerStage(
                event.getOrchestrationId(),
                previousStage,
                rollbackInput
            );

            if (triggered) {
                log.info("回滚触发成功: orchestrationId={}, rolledBackTo={}",
                        event.getOrchestrationId(), previousStage);

                // 发送回滚通知
                notificationService.sendWorkflowNotification(
                    event.getOrchestrationId(),
                    "WORKFLOW_ROLLBACK",
                    "WARNING",
                    String.format("工作流从阶段 %s 回滚到阶段 %s", event.getStageName(), previousStage)
                );
            } else {
                log.error("回滚触发失败，终止工作流: orchestrationId={}, rollbackStage={}",
                        event.getOrchestrationId(), previousStage);
                terminateWorkflow(event.getOrchestrationId(), event.getTaskId(), "回滚触发失败");
            }

        } catch (Exception e) {
            log.error("回滚到上一阶段失败: orchestrationId={}, currentStage={}",
                    event.getOrchestrationId(), event.getStageName(), e);
            terminateWorkflow(event.getOrchestrationId(), event.getTaskId(), "回滚处理失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送阶段开始通知
     */
    private void sendStageStartNotification(WorkflowStageStartedEvent event) {
        log.debug("发送阶段开始通知: orchestrationId={}, stage={}",
                event.getOrchestrationId(), event.getStageName());

        try {
            // 构建通知内容
            String title = String.format("工作流阶段开始: %s", event.getStageName());
            String message = String.format(
                "工作流 %s 的阶段 %s (顺序: %d) 开始执行\n" +
                "操作者: %s\n" +
                "开始时间: %s",
                event.getOrchestrationId(),
                event.getStageName(),
                event.getStageOrder(),
                event.getOperator() != null ? event.getOperator() : "系统",
                java.util.Date.from(event.getEventTime().atZone(java.time.ZoneId.systemDefault()).toInstant())
            );

            // 准备通知元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("orchestrationId", event.getOrchestrationId());
            metadata.put("stageName", event.getStageName());
            metadata.put("stageOrder", event.getStageOrder());
            metadata.put("operator", event.getOperator());
            metadata.put("inputDataSize", event.getInputData() != null ? event.getInputData().size() : 0);
            metadata.put("eventType", "STAGE_STARTED");

            // 发送工作流通知
            notificationService.sendWorkflowNotification(
                event.getOrchestrationId(),
                "STAGE_STARTED",
                "INFO",
                message
            );

            // 根据阶段重要性决定是否发送额外通知
            if (isImportantStage(event.getStageName())) {
                // 对于重要阶段，可能需要发送邮件或其他高优先级通知
                notificationService.sendNotification(
                    "EMAIL", // 通知类型
                    title,
                    message,
                    getStageNotificationRecipients(event.getStageName()), // 获取通知接收者
                    "NORMAL", // 普通优先级
                    metadata
                );
            }

            log.debug("阶段开始通知发送完成: orchestrationId={}, stage={}",
                    event.getOrchestrationId(), event.getStageName());

        } catch (Exception e) {
            log.warn("发送阶段开始通知失败: orchestrationId={}, stage={}",
                    event.getOrchestrationId(), event.getStageName(), e);
        }
    }
    
    /**
     * 发送阶段完成通知
     */
    private void sendStageCompletionNotification(WorkflowStageCompletedEvent event) {
        log.debug("发送阶段完成通知: orchestrationId={}, stage={}, success={}",
                event.getOrchestrationId(), event.getStageName(), event.isSuccess());

        try {
            String statusText = event.isSuccess() ? "成功完成" : "执行失败";
            String title = String.format("工作流阶段%s: %s", statusText, event.getStageName());

            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append(String.format("工作流 %s 的阶段 %s (顺序: %d) %s\n",
                    event.getOrchestrationId(), event.getStageName(), event.getStageOrder(), statusText));
            messageBuilder.append(String.format("执行时长: %d 毫秒\n", event.getExecutionDuration()));

            if (event.isSuccess()) {
                if (event.hasNextStage()) {
                    messageBuilder.append(String.format("下一阶段: %s\n", event.getNextStageName()));
                } else {
                    messageBuilder.append("这是最后一个阶段，工作流即将完成\n");
                }

                if (event.getOutputData() != null && !event.getOutputData().isEmpty()) {
                    messageBuilder.append(String.format("输出数据项数: %d\n", event.getOutputData().size()));
                }
            } else {
                messageBuilder.append(String.format("错误信息: %s\n", event.getErrorMessage()));
            }

            messageBuilder.append(String.format("完成时间: %s",
                java.util.Date.from(event.getEventTime().atZone(java.time.ZoneId.systemDefault()).toInstant())));

            // 准备通知元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("orchestrationId", event.getOrchestrationId());
            metadata.put("stageName", event.getStageName());
            metadata.put("stageOrder", event.getStageOrder());
            metadata.put("success", event.isSuccess());
            metadata.put("executionDuration", event.getExecutionDuration());
            metadata.put("hasNextStage", event.hasNextStage());
            metadata.put("nextStageName", event.getNextStageName());
            metadata.put("eventType", "STAGE_COMPLETED");

            if (!event.isSuccess()) {
                metadata.put("errorMessage", event.getErrorMessage());
            }

            // 确定通知类型和优先级
            String notificationType = event.isSuccess() ? "STAGE_COMPLETED" : "STAGE_FAILED";
            String priority = event.isSuccess() ? "INFO" : "ERROR";

            // 发送工作流通知
            notificationService.sendWorkflowNotification(
                event.getOrchestrationId(),
                notificationType,
                priority,
                messageBuilder.toString()
            );

            // 对于失败或重要阶段，发送额外通知
            if (!event.isSuccess() || isImportantStage(event.getStageName())) {
                String emailPriority = event.isSuccess() ? "NORMAL" : "HIGH";

                notificationService.sendNotification(
                    "EMAIL",
                    title,
                    messageBuilder.toString(),
                    getStageNotificationRecipients(event.getStageName()),
                    emailPriority,
                    metadata
                );
            }

            log.debug("阶段完成通知发送完成: orchestrationId={}, stage={}, success={}",
                    event.getOrchestrationId(), event.getStageName(), event.isSuccess());

        } catch (Exception e) {
            log.warn("发送阶段完成通知失败: orchestrationId={}, stage={}, success={}",
                    event.getOrchestrationId(), event.getStageName(), event.isSuccess(), e);
        }
    }
    
    /**
     * 更新性能指标
     */
    private void updatePerformanceMetrics(WorkflowStageCompletedEvent event) {
        try {
            log.debug("更新性能指标: stage={}, duration={}ms, success={}",
                    event.getStageName(), event.getExecutionDuration(), event.isSuccess());

            // 1. 记录阶段执行时间
            performanceMonitorService.recordStageExecutionTime(
                event.getStageName(),
                event.getExecutionDuration()
            );

            // 2. 更新阶段成功率统计
            performanceMonitorService.recordStageResult(
                event.getStageName(),
                event.isSuccess()
            );

            // 3. 记录工作流级别的性能指标
            performanceMonitorService.recordWorkflowStageMetrics(
                event.getOrchestrationId(),
                event.getStageName(),
                event.getStageOrder(),
                event.getExecutionDuration(),
                event.isSuccess()
            );

            // 4. 如果有输入/输出数据，记录数据处理量
            // 注意：WorkflowStageCompletedEvent 通常不包含输入数据
            // if (event.getInputData() != null) {
            //     performanceMonitorService.recordDataProcessingMetrics(
            //         event.getStageName(),
            //         "input",
            //         event.getInputData().size()
            //     );
            // }

            if (event.getOutputData() != null) {
                performanceMonitorService.recordDataProcessingMetrics(
                    event.getStageName(),
                    "output",
                    event.getOutputData().size()
                );
            }

            // 5. 记录资源使用情况（如果事件中包含资源信息）
            // 注意：WorkflowStageCompletedEvent 通常不包含资源使用信息
            // if (event.getResourceUsage() != null) {
            //     performanceMonitorService.recordResourceUsage(
            //         event.getStageName(),
            //         event.getResourceUsage()
            //     );
            // }

            // 6. 计算和记录吞吐量指标
            long currentTime = System.currentTimeMillis();
            performanceMonitorService.updateThroughputMetrics(
                event.getStageName(),
                currentTime
            );

            // 7. 如果阶段失败，记录错误类型统计
            if (!event.isSuccess() && event.getErrorMessage() != null) {
                performanceMonitorService.recordErrorMetrics(
                    event.getStageName(),
                    categorizeError(event.getErrorMessage())
                );
            }

            // 8. 记录并发性能指标（如果有相关数据）
            performanceMonitorService.recordConcurrencyMetrics(
                event.getOrchestrationId(),
                event.getStageName(),
                getCurrentConcurrentStages(event.getOrchestrationId())
            );

            log.debug("性能指标更新完成: stage={}, orchestrationId={}",
                    event.getStageName(), event.getOrchestrationId());

        } catch (Exception e) {
            log.warn("更新性能指标失败: stage={}, orchestrationId={}",
                    event.getStageName(), event.getOrchestrationId(), e);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取阶段的最大重试次数
     */
    private int getMaxRetriesForStage(String stageName) {
        // 根据阶段类型返回不同的重试次数
        switch (stageName.toUpperCase()) {
            case "DATA_DISTRIBUTION":
            case "MODEL_AGGREGATION":
                return 3; // 数据分发和模型聚合允许3次重试
            case "MODEL_TRAINING":
                return 2; // 模型训练允许2次重试
            case "VALIDATION":
                return 1; // 验证只允许1次重试
            default:
                return 2; // 默认2次重试
        }
    }


    /**
     * 增加重试计数
     */
    private void incrementRetryCount(String orchestrationId, String stageName) {
        try {
            String key = String.format("retry:count:%s:%s:retry", orchestrationId, stageName);
            int currentCount = stageStartTimeCache.getOrDefault(key, 0L).intValue();
            stageStartTimeCache.put(key, (long) (currentCount + 1));
        } catch (Exception e) {
            log.warn("增加重试计数失败: orchestrationId={}, stageName={}", orchestrationId, stageName, e);
        }
    }

    /**
     * 计算重试延迟（指数退避）
     */
    private long calculateRetryDelay(int retryCount) {
        // 指数退避: 2^retryCount * 1000ms，最大30秒
        long baseDelay = 1000L; // 1秒基础延迟
        long delay = (long) (baseDelay * Math.pow(2, retryCount));
        return Math.min(delay, 30000L); // 最大30秒
    }

    /**
     * 获取上一阶段名称
     */
    private String getPreviousStage(String orchestrationId, String currentStage) {
        try {
            // 这里应该调用编排服务获取阶段历史
            // 简化实现，返回常见的上一阶段映射
            switch (currentStage.toUpperCase()) {
                case "MODEL_TRAINING":
                    return "DATA_DISTRIBUTION";
                case "MODEL_AGGREGATION":
                    return "MODEL_TRAINING";
                case "VALIDATION":
                    return "MODEL_AGGREGATION";
                default:
                    return null; // 没有上一阶段
            }
        } catch (Exception e) {
            log.warn("获取上一阶段失败: orchestrationId={}, currentStage={}", orchestrationId, currentStage, e);
            return null;
        }
    }

    /**
     * 获取上一阶段的输入数据
     */
    private Map<String, Object> getPreviousStageInput(String orchestrationId, String stageName) {
        try {
            // 这里应该从数据库或缓存中获取上一阶段的输入数据
            // 简化实现，返回空Map
            return new HashMap<>();
        } catch (Exception e) {
            log.warn("获取上一阶段输入数据失败: orchestrationId={}, stageName={}", orchestrationId, stageName, e);
            return new HashMap<>();
        }
    }

    /**
     * 清理工作流相关资源
     */
    private void cleanupWorkflowResources(String orchestrationId) {
        try {
            // 清理缓存中的阶段开始时间
            stageStartTimeCache.entrySet().removeIf(entry ->
                entry.getKey().contains(orchestrationId));

            // 这里可以添加其他资源清理逻辑
            // 例如：清理临时文件、释放内存资源等

            log.debug("工作流资源清理完成: orchestrationId={}", orchestrationId);
        } catch (Exception e) {
            log.warn("清理工作流资源失败: orchestrationId={}", orchestrationId, e);
        }
    }

    /**
     * 判断是否为重要阶段
     */
    private boolean isImportantStage(String stageName) {
        // 定义重要阶段列表
        return stageName != null && (
            stageName.toUpperCase().contains("TRAINING") ||
            stageName.toUpperCase().contains("AGGREGATION") ||
            stageName.toUpperCase().contains("VALIDATION") ||
            stageName.toUpperCase().contains("COMPLETION")
        );
    }

    /**
     * 获取阶段通知接收者
     */
    private String getStageNotificationRecipients(String stageName) {
        // 根据阶段类型返回不同的通知接收者
        // 这里应该从配置或数据库中获取
        return "admin@feduwacomm.com"; // 简化实现
    }

    /**
     * 分类错误信息
     */
    private String categorizeError(String errorMessage) {
        if (errorMessage == null) {
            return "UNKNOWN";
        }

        String upperError = errorMessage.toUpperCase();
        if (upperError.contains("TIMEOUT")) {
            return "TIMEOUT";
        } else if (upperError.contains("CONNECTION")) {
            return "CONNECTION";
        } else if (upperError.contains("PERMISSION") || upperError.contains("ACCESS")) {
            return "PERMISSION";
        } else if (upperError.contains("VALIDATION")) {
            return "VALIDATION";
        } else if (upperError.contains("RESOURCE")) {
            return "RESOURCE";
        } else {
            return "BUSINESS_LOGIC";
        }
    }

    /**
     * 获取当前并发阶段数量
     */
    private int getCurrentConcurrentStages(String orchestrationId) {
        try {
            // 这里应该查询当前正在执行的阶段数量
            // 简化实现，返回固定值
            return 1;
        } catch (Exception e) {
            log.warn("获取并发阶段数量失败: orchestrationId={}", orchestrationId, e);
            return 1;
        }
    }
}