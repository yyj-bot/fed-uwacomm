package com.feduwacomm.listener;

import com.feduwacomm.event.WorkflowStageCompletedEvent;
import com.feduwacomm.event.WorkflowStageStartedEvent;
import com.feduwacomm.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

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
            // TODO: 调用工作流服务更新阶段状态
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
            // TODO: 可以使用Redis或内存缓存记录开始时间，用于计算执行耗时
            String key = String.format("stage:start:%s:%s", orchestrationId, stageName);
            long startTime = System.currentTimeMillis();
            log.debug("记录阶段开始时间: key={}, startTime={}", key, startTime);
        } catch (Exception e) {
            log.warn("记录阶段开始时间失败", e);
        }
    }
    
    /**
     * 安排下一阶段执行
     */
    private void scheduleNextStage(WorkflowStageCompletedEvent event) {
        try {
            log.info("安排下一阶段执行: orchestrationId={}, nextStage={}", 
                    event.getOrchestrationId(), event.getNextStageName());
            
            // TODO: 调用工作流编排服务触发下一阶段
            // 这里可以有一个短暂的延迟，确保当前阶段的清理工作完成
            
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
            
            // TODO: 更新工作流状态为COMPLETED
            // TODO: 发送工作流完成通知
            
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
            
            // TODO: 根据失败策略处理
            // 1. 重试当前阶段
            // 2. 跳过当前阶段继续下一阶段
            // 3. 终止整个工作流
            // 4. 回滚到上一阶段
            
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
        // TODO: 根据阶段类型、错误类型、重试次数等确定策略
        // 这里简化处理，实际应该根据配置和上下文确定
        
        if (event.getStageName().contains("DISTRIBUTION") && 
            event.getErrorMessage() != null && event.getErrorMessage().contains("timeout")) {
            return "RETRY"; // 分发超时可以重试
        } else if (event.getStageName().contains("VALIDATION")) {
            return "SKIP"; // 验证失败可以跳过
        } else {
            return "TERMINATE"; // 其他情况终止工作流
        }
    }
    
    /**
     * 安排阶段重试
     */
    private void scheduleStageRetry(WorkflowStageCompletedEvent event) {
        log.info("安排阶段重试: orchestrationId={}, stage={}", 
                event.getOrchestrationId(), event.getStageName());
        // TODO: 实现重试逻辑
    }
    
    /**
     * 跳过到下一阶段
     */
    private void skipToNextStage(WorkflowStageCompletedEvent event) {
        log.info("跳过到下一阶段: orchestrationId={}, currentStage={}, nextStage={}", 
                event.getOrchestrationId(), event.getStageName(), event.getNextStageName());
        // TODO: 实现跳过逻辑
    }
    
    /**
     * 终止工作流
     */
    private void terminateWorkflow(String orchestrationId, String taskId, String reason) {
        log.error("终止工作流: orchestrationId={}, taskId={}, reason={}", 
                orchestrationId, taskId, reason);
        // TODO: 实现工作流终止逻辑
    }
    
    /**
     * 回滚到上一阶段
     */
    private void rollbackToPreviousStage(WorkflowStageCompletedEvent event) {
        log.info("回滚到上一阶段: orchestrationId={}, currentStage={}", 
                event.getOrchestrationId(), event.getStageName());
        // TODO: 实现回滚逻辑
    }
    
    /**
     * 发送阶段开始通知
     */
    private void sendStageStartNotification(WorkflowStageStartedEvent event) {
        // TODO: 实现通知逻辑
        log.debug("发送阶段开始通知: orchestrationId={}, stage={}", 
                event.getOrchestrationId(), event.getStageName());
    }
    
    /**
     * 发送阶段完成通知
     */
    private void sendStageCompletionNotification(WorkflowStageCompletedEvent event) {
        // TODO: 实现通知逻辑
        log.debug("发送阶段完成通知: orchestrationId={}, stage={}, success={}", 
                event.getOrchestrationId(), event.getStageName(), event.isSuccess());
    }
    
    /**
     * 更新性能指标
     */
    private void updatePerformanceMetrics(WorkflowStageCompletedEvent event) {
        try {
            // TODO: 更新性能监控指标
            // 1. 阶段执行时间
            // 2. 成功率
            // 3. 吞吐量
            // 4. 资源使用情况
            
            log.debug("更新性能指标: stage={}, duration={}ms, success={}", 
                    event.getStageName(), event.getExecutionDuration(), event.isSuccess());
            
        } catch (Exception e) {
            log.warn("更新性能指标失败", e);
        }
    }
}