package com.feduwacomm.listener;

import com.feduwacomm.event.DataDistributionCompletedEvent;
import com.feduwacomm.event.DataDistributionStartedEvent;
import com.feduwacomm.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据分发事件监听器
 * 处理数据分发相关的事件，包括日志记录、状态更新等
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataDistributionEventListener {
    
    private final LogService logService;
    
    /**
     * 处理数据分发开始事件
     */
    @EventListener
    @Async
    public void handleDataDistributionStarted(DataDistributionStartedEvent event) {
        log.info("处理数据分发开始事件: distributionId={}, taskId={}, strategy={}", 
                event.getDistributionId(), event.getTaskId(), event.getDistributionStrategy());
        
        try {
            // 记录系统日志
            Map<String, Object> logDetails = new HashMap<>();
            logDetails.put("distributionId", event.getDistributionId());
            logDetails.put("distributionStrategy", event.getDistributionStrategy());
            logDetails.put("targetVmIds", event.getTargetVmIds());
            logDetails.put("datasetIds", event.getDatasetIds());
            logDetails.put("configParams", event.getConfigParams());
            logDetails.put("operatorId", event.getOperatorId());
            logDetails.put("eventTime", event.getEventTime());
            logDetails.put("totalDataSize", event.getTotalDataSize());
            logDetails.put("expectedVmCount", event.getExpectedVmCount());
            
            logService.logTask(event.getTaskId(), "INFO", 
                    String.format("数据分发开始执行: 分发策略=%s, 目标VM数量=%d, 操作者=%s", 
                            event.getDistributionStrategy(), 
                            event.getExpectedVmCount() != null ? event.getExpectedVmCount() : 0,
                            event.getOperatorId()),
                    "DataDistributionService", null, logDetails);
            
            // 记录性能监控指标
            recordDistributionStartMetrics(event);
            
            // 发送分发开始通知
            sendDistributionStartNotification(event);
            
        } catch (Exception e) {
            log.error("处理数据分发开始事件失败: distributionId={}", event.getDistributionId(), e);
        }
    }
    
    /**
     * 处理数据分发完成事件
     */
    @EventListener
    @Async
    public void handleDataDistributionCompleted(DataDistributionCompletedEvent event) {
        log.info("处理数据分发完成事件: distributionId={}, taskId={}, success={}", 
                event.getDistributionId(), event.getTaskId(), event.isSuccess());
        
        try {
            // 记录系统日志
            Map<String, Object> logDetails = new HashMap<>();
            logDetails.put("distributionId", event.getDistributionId());
            logDetails.put("success", event.isSuccess());
            logDetails.put("distributionStrategy", event.getDistributionStrategy());
            logDetails.put("successfulVmCount", event.getSuccessfulVmCount());
            logDetails.put("failedVmCount", event.getFailedVmCount());
            logDetails.put("executionDuration", event.getExecutionDuration());
            logDetails.put("totalDataSize", event.getTotalDataSize());
            logDetails.put("errorMessage", event.getErrorMessage());
            logDetails.put("distributionStatistics", event.getDistributionStatistics());
            logDetails.put("operatorId", event.getOperatorId());
            logDetails.put("eventTime", event.getEventTime());
            
            String logLevel = event.isSuccess() ? "INFO" : "ERROR";
            String logMessage;
            
            if (event.isSuccess()) {
                logMessage = String.format("数据分发执行成功: 成功VM数=%d, 失败VM数=%d, 耗时=%dms, 数据量=%d字节", 
                        event.getSuccessfulVmCount() != null ? event.getSuccessfulVmCount() : 0,
                        event.getFailedVmCount() != null ? event.getFailedVmCount() : 0,
                        event.getExecutionDuration() != null ? event.getExecutionDuration() : 0,
                        event.getTotalDataSize() != null ? event.getTotalDataSize() : 0);
            } else {
                logMessage = String.format("数据分发执行失败: 成功VM数=%d, 失败VM数=%d, 错误=%s", 
                        event.getSuccessfulVmCount() != null ? event.getSuccessfulVmCount() : 0,
                        event.getFailedVmCount() != null ? event.getFailedVmCount() : 0,
                        event.getErrorMessage() != null ? event.getErrorMessage() : "未知错误");
            }
            
            logService.logTask(event.getTaskId(), logLevel, logMessage, 
                    "DataDistributionService", null, logDetails);
            
            // 记录性能监控指标
            recordDistributionCompletedMetrics(event);
            
            // 发送分发完成通知
            sendDistributionCompletedNotification(event);
            
            // 如果成功且有下一阶段，触发工作流继续执行
            if (event.isSuccess() && event.hasNextStage()) {
                log.info("数据分发成功完成，准备触发下一阶段: taskId={}, nextStage={}", 
                        event.getTaskId(), event.getNextStageName());
                
                triggerNextWorkflowStage(event);
            } else if (!event.isSuccess()) {
                // 分发失败，处理失败情况
                log.error("数据分发失败，需要处理异常: taskId={}, error={}", 
                        event.getTaskId(), event.getErrorMessage());
                
                handleDistributionFailure(event);
            }
            
        } catch (Exception e) {
            log.error("处理数据分发完成事件失败: distributionId={}", event.getDistributionId(), e);
        }
    }
    
    /**
     * 记录分发开始的性能指标
     */
    private void recordDistributionStartMetrics(DataDistributionStartedEvent event) {
        try {
            log.debug("记录数据分发开始指标: distributionId={}, strategy={}", 
                    event.getDistributionId(), event.getDistributionStrategy());
            
            // TODO: 集成监控系统（如Prometheus）记录指标
            // 1. 分发任务启动计数器
            // 2. 按策略分组的启动计数器
            // 3. 目标VM数量分布
            // 4. 数据量分布
            
        } catch (Exception e) {
            log.warn("记录分发开始指标失败", e);
        }
    }
    
    /**
     * 记录分发完成的性能指标
     */
    private void recordDistributionCompletedMetrics(DataDistributionCompletedEvent event) {
        try {
            log.debug("记录数据分发完成指标: distributionId={}, success={}, duration={}ms", 
                    event.getDistributionId(), event.isSuccess(), event.getExecutionDuration());
            
            // TODO: 集成监控系统记录指标
            // 1. 分发任务完成计数器（按成功/失败分组）
            // 2. 分发执行时间直方图
            // 3. 成功率计算
            // 4. 数据传输速率
            // 5. VM分发成功率分布
            
        } catch (Exception e) {
            log.warn("记录分发完成指标失败", e);
        }
    }
    
    /**
     * 发送分发开始通知
     */
    private void sendDistributionStartNotification(DataDistributionStartedEvent event) {
        try {
            log.debug("发送数据分发开始通知: distributionId={}, strategy={}", 
                    event.getDistributionId(), event.getDistributionStrategy());
            
            // TODO: 实现通知逻辑
            // 1. WebSocket实时通知前端
            // 2. 邮件通知相关用户
            // 3. 消息队列通知其他服务
            
        } catch (Exception e) {
            log.warn("发送分发开始通知失败", e);
        }
    }
    
    /**
     * 发送分发完成通知
     */
    private void sendDistributionCompletedNotification(DataDistributionCompletedEvent event) {
        try {
            log.debug("发送数据分发完成通知: distributionId={}, success={}", 
                    event.getDistributionId(), event.isSuccess());
            
            // TODO: 实现通知逻辑
            // 1. WebSocket通知前端更新状态
            // 2. 发送完成报告邮件
            // 3. 记录到审计日志
            // 4. 更新仪表板数据
            
        } catch (Exception e) {
            log.warn("发送分发完成通知失败", e);
        }
    }
    
    /**
     * 触发工作流下一阶段
     */
    private void triggerNextWorkflowStage(DataDistributionCompletedEvent event) {
        try {
            log.info("触发工作流下一阶段: taskId={}, nextStage={}", 
                    event.getTaskId(), event.getNextStageName());
            
            // TODO: 集成工作流编排服务
            // 1. 调用OrchestrationService.triggerNextStage()
            // 2. 传递数据分发结果作为输入
            // 3. 处理阶段转换异常
            
            // 暂时记录日志，等待工作流编排服务实现
            Map<String, Object> stageInput = new HashMap<>();
            stageInput.put("distributionId", event.getDistributionId());
            stageInput.put("distributionResult", "SUCCESS");
            stageInput.put("successfulVmCount", event.getSuccessfulVmCount());
            stageInput.put("totalDataSize", event.getTotalDataSize());
            
            log.info("准备启动工作流阶段: {}, 输入参数: {}", event.getNextStageName(), stageInput);
            
        } catch (Exception e) {
            log.error("触发工作流下一阶段失败: taskId={}", event.getTaskId(), e);
        }
    }
    
    /**
     * 处理分发失败情况
     */
    private void handleDistributionFailure(DataDistributionCompletedEvent event) {
        try {
            log.error("处理数据分发失败: distributionId={}, error={}", 
                    event.getDistributionId(), event.getErrorMessage());
            
            // TODO: 实现失败处理逻辑
            // 1. 根据失败类型确定处理策略
            // 2. 自动重试机制
            // 3. 回退到上一阶段
            // 4. 终止整个工作流
            // 5. 发送告警通知
            
            String failureStrategy = determineFailureStrategy(event);
            
            switch (failureStrategy) {
                case "AUTO_RETRY":
                    scheduleAutoRetry(event);
                    break;
                case "MANUAL_INTERVENTION":
                    requestManualIntervention(event);
                    break;
                case "TERMINATE_WORKFLOW":
                    terminateWorkflow(event);
                    break;
                default:
                    log.warn("未知的失败处理策略: {}", failureStrategy);
                    requestManualIntervention(event);
            }
            
        } catch (Exception e) {
            log.error("处理分发失败时发生异常: distributionId={}", event.getDistributionId(), e);
        }
    }
    
    /**
     * 确定失败处理策略
     */
    private String determineFailureStrategy(DataDistributionCompletedEvent event) {
        // 简化的策略判断逻辑
        if (event.getFailedVmCount() != null && event.getSuccessfulVmCount() != null) {
            double failureRate = (double) event.getFailedVmCount() / 
                    (event.getFailedVmCount() + event.getSuccessfulVmCount());
            
            if (failureRate < 0.2) {
                // 失败率小于20%，自动重试
                return "AUTO_RETRY";
            } else if (failureRate < 0.5) {
                // 失败率20%-50%，需要人工干预
                return "MANUAL_INTERVENTION";
            } else {
                // 失败率超过50%，终止工作流
                return "TERMINATE_WORKFLOW";
            }
        }
        
        return "MANUAL_INTERVENTION";
    }
    
    /**
     * 安排自动重试
     */
    private void scheduleAutoRetry(DataDistributionCompletedEvent event) {
        log.info("安排数据分发自动重试: distributionId={}", event.getDistributionId());
        // TODO: 实现自动重试逻辑
    }
    
    /**
     * 请求人工干预
     */
    private void requestManualIntervention(DataDistributionCompletedEvent event) {
        log.warn("数据分发需要人工干预: distributionId={}, error={}", 
                event.getDistributionId(), event.getErrorMessage());
        // TODO: 发送告警通知，请求管理员处理
    }
    
    /**
     * 终止工作流
     */
    private void terminateWorkflow(DataDistributionCompletedEvent event) {
        log.error("数据分发严重失败，终止工作流: taskId={}, distributionId={}", 
                event.getTaskId(), event.getDistributionId());
        // TODO: 调用工作流编排服务终止整个工作流
    }
}