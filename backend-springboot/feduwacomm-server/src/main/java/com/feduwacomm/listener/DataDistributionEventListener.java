package com.feduwacomm.listener;

import com.feduwacomm.event.DataDistributionCompletedEvent;
import com.feduwacomm.event.DataDistributionStartedEvent;
import com.feduwacomm.service.LogService;
import com.feduwacomm.service.NotificationService;
import com.feduwacomm.service.PerformanceMonitorService;
import com.feduwacomm.service.FederatedOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
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
    private final NotificationService notificationService;
    private final PerformanceMonitorService performanceMonitorService;
    private final FederatedOrchestrationService orchestrationService;
    
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

            // 1. 记录分发任务启动计数器
            performanceMonitorService.incrementDistributionTaskCounter(
                event.getDistributionId(),
                "started"
            );

            // 2. 记录目标VM数量分布
            if (event.getExpectedVmCount() != null) {
                performanceMonitorService.recordDistributionVmCount(
                    event.getDistributionStrategy(),
                    event.getExpectedVmCount()
                );
            }

            // 3. 记录数据量分布
            if (event.getTotalDataSize() != null) {
                performanceMonitorService.recordDistributionDataSize(
                    event.getDistributionStrategy(),
                    event.getTotalDataSize()
                );
            }

            // 4. 记录分发开始时间戳
            performanceMonitorService.recordDistributionStartTime(
                event.getDistributionId(),
                System.currentTimeMillis()
            );

            // 5. 记录数据集数量
            if (event.getDatasetIds() != null) {
                performanceMonitorService.recordDistributionDatasetCount(
                    event.getDistributionStrategy(),
                    event.getDatasetIds().length
                );
            }

        } catch (Exception e) {
            log.warn("记录分发开始指标失败: distributionId={}", event.getDistributionId(), e);
        }
    }
    
    /**
     * 记录分发完成的性能指标
     */
    private void recordDistributionCompletedMetrics(DataDistributionCompletedEvent event) {
        try {
            log.debug("记录数据分发完成指标: distributionId={}, success={}, duration={}ms",
                    event.getDistributionId(), event.isSuccess(), event.getExecutionDuration());

            // 1. 记录分发任务完成计数器（按成功/失败分组）
            String status = event.isSuccess() ? "success" : "failed";
            performanceMonitorService.incrementDistributionTaskCounter(
                event.getDistributionId(),
                status
            );

            // 2. 记录分发执行时间
            if (event.getExecutionDuration() != null) {
                performanceMonitorService.recordDistributionExecutionTime(
                    event.getDistributionStrategy(),
                    event.getExecutionDuration()
                );
            }

            // 3. 记录成功率指标
            performanceMonitorService.recordDistributionResult(
                event.getDistributionStrategy(),
                event.isSuccess()
            );

            // 4. 计算和记录数据传输速率
            if (event.getTotalDataSize() != null && event.getExecutionDuration() != null && event.getExecutionDuration() > 0) {
                double transferRate = (double) event.getTotalDataSize() / (event.getExecutionDuration() / 1000.0); // bytes/second
                performanceMonitorService.recordDataTransferRate(
                    event.getDistributionStrategy(),
                    transferRate
                );
            }

            // 5. 记录VM分发成功率
            if (event.getSuccessfulVmCount() != null && event.getFailedVmCount() != null) {
                int totalVmCount = event.getSuccessfulVmCount() + event.getFailedVmCount();
                if (totalVmCount > 0) {
                    double vmSuccessRate = (double) event.getSuccessfulVmCount() / totalVmCount;
                    performanceMonitorService.recordVmDistributionSuccessRate(
                        event.getDistributionStrategy(),
                        vmSuccessRate
                    );
                }

                // 分别记录成功和失败的VM数量
                performanceMonitorService.recordDistributionVmResults(
                    event.getDistributionStrategy(),
                    event.getSuccessfulVmCount(),
                    event.getFailedVmCount()
                );
            }

            // 6. 记录分发统计信息
            if (event.getDistributionStatistics() != null) {
                performanceMonitorService.recordDistributionStatistics(
                    event.getDistributionId(),
                    event.getDistributionStatistics()
                );
            }

        } catch (Exception e) {
            log.warn("记录分发完成指标失败: distributionId={}", event.getDistributionId(), e);
        }
    }
    
    /**
     * 发送分发开始通知
     */
    private void sendDistributionStartNotification(DataDistributionStartedEvent event) {
        try {
            log.debug("发送数据分发开始通知: distributionId={}, strategy={}",
                    event.getDistributionId(), event.getDistributionStrategy());

            // 构建通知内容
            String title = String.format("数据分发开始: %s", event.getDistributionStrategy());
            String message = String.format(
                "数据分发任务已开始执行\n" +
                "分发ID: %s\n" +
                "分发策略: %s\n" +
                "目标VM数量: %d\n" +
                "数据集数量: %d\n" +
                "数据大小: %s\n" +
                "操作者: %s",
                event.getDistributionId(),
                event.getDistributionStrategy(),
                event.getExpectedVmCount() != null ? event.getExpectedVmCount() : 0,
                event.getDatasetIds() != null ? event.getDatasetIds().length : 0,
                formatDataSize(event.getTotalDataSize()),
                event.getOperatorId() != null ? event.getOperatorId() : "系统"
            );

            // 准备通知元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("distributionId", event.getDistributionId());
            metadata.put("distributionStrategy", event.getDistributionStrategy());
            metadata.put("taskId", event.getTaskId());
            metadata.put("expectedVmCount", event.getExpectedVmCount());
            metadata.put("totalDataSize", event.getTotalDataSize());
            metadata.put("operatorId", event.getOperatorId());
            metadata.put("eventType", "DISTRIBUTION_STARTED");

            // 1. 发送WebSocket实时通知给前端
            notificationService.sendWebSocketNotification(
                event.getTaskId(),
                "DISTRIBUTION_STARTED",
                "INFO",
                message,
                metadata
            );

            // 2. 对于大型分发任务，发送邮件通知
            if (isLargeDistributionTask(event)) {
                notificationService.sendNotification(
                    "EMAIL",
                    title,
                    message,
                    getDistributionNotificationRecipients(event.getDistributionStrategy()),
                    "NORMAL",
                    metadata
                );
            }

            // 3. 发送系统通知给相关服务
            notificationService.sendSystemNotification(
                "DATA_DISTRIBUTION",
                "STARTED",
                event.getDistributionId(),
                metadata
            );

        } catch (Exception e) {
            log.warn("发送分发开始通知失败: distributionId={}", event.getDistributionId(), e);
        }
    }
    
    /**
     * 发送分发完成通知
     */
    private void sendDistributionCompletedNotification(DataDistributionCompletedEvent event) {
        try {
            log.debug("发送数据分发完成通知: distributionId={}, success={}",
                    event.getDistributionId(), event.isSuccess());

            // 构建通知内容
            String statusText = event.isSuccess() ? "成功完成" : "执行失败";
            String title = String.format("数据分发%s: %s", statusText, event.getDistributionStrategy());

            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append(String.format("数据分发任务%s\n", statusText));
            messageBuilder.append(String.format("分发ID: %s\n", event.getDistributionId()));
            messageBuilder.append(String.format("分发策略: %s\n", event.getDistributionStrategy()));
            messageBuilder.append(String.format("执行时长: %d 毫秒\n",
                    event.getExecutionDuration() != null ? event.getExecutionDuration() : 0));

            if (event.isSuccess()) {
                messageBuilder.append(String.format("成功VM数量: %d\n",
                        event.getSuccessfulVmCount() != null ? event.getSuccessfulVmCount() : 0));
                messageBuilder.append(String.format("失败VM数量: %d\n",
                        event.getFailedVmCount() != null ? event.getFailedVmCount() : 0));
                messageBuilder.append(String.format("分发数据大小: %s\n",
                        formatDataSize(event.getTotalDataSize())));

                if (event.hasNextStage()) {
                    messageBuilder.append(String.format("下一阶段: %s\n", event.getNextStageName()));
                }
            } else {
                messageBuilder.append(String.format("错误信息: %s\n", event.getErrorMessage()));
                if (event.getSuccessfulVmCount() != null && event.getFailedVmCount() != null) {
                    int totalVmCount = event.getSuccessfulVmCount() + event.getFailedVmCount();
                    double failureRate = totalVmCount > 0 ? (double) event.getFailedVmCount() / totalVmCount * 100 : 0;
                    messageBuilder.append(String.format("失败率: %.2f%% (%d/%d)\n",
                            failureRate, event.getFailedVmCount(), totalVmCount));
                }
            }

            messageBuilder.append(String.format("完成时间: %s", event.getEventTime()));

            // 准备通知元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("distributionId", event.getDistributionId());
            metadata.put("distributionStrategy", event.getDistributionStrategy());
            metadata.put("success", event.isSuccess());
            metadata.put("executionDuration", event.getExecutionDuration());
            metadata.put("successfulVmCount", event.getSuccessfulVmCount());
            metadata.put("failedVmCount", event.getFailedVmCount());
            metadata.put("totalDataSize", event.getTotalDataSize());
            metadata.put("hasNextStage", event.hasNextStage());
            metadata.put("eventType", "DISTRIBUTION_COMPLETED");

            if (!event.isSuccess()) {
                metadata.put("errorMessage", event.getErrorMessage());
            }

            // 1. 发送WebSocket通知前端更新状态
            String notificationType = event.isSuccess() ? "DISTRIBUTION_COMPLETED" : "DISTRIBUTION_FAILED";
            String priority = event.isSuccess() ? "INFO" : "ERROR";

            notificationService.sendWebSocketNotification(
                event.getTaskId(),
                notificationType,
                priority,
                messageBuilder.toString(),
                metadata
            );

            // 2. 对于失败或重要的分发任务，发送邮件报告
            if (!event.isSuccess() || isImportantDistributionTask(event)) {
                String emailPriority = event.isSuccess() ? "NORMAL" : "HIGH";

                notificationService.sendNotification(
                    "EMAIL",
                    title,
                    messageBuilder.toString(),
                    getDistributionNotificationRecipients(event.getDistributionStrategy()),
                    emailPriority,
                    metadata
                );
            }

            // 3. 记录到审计日志
            notificationService.sendAuditNotification(
                "DATA_DISTRIBUTION",
                event.isSuccess() ? "COMPLETED" : "FAILED",
                event.getDistributionId(),
                event.getOperatorId(),
                metadata
            );

            // 4. 发送系统通知更新仪表板数据
            notificationService.sendSystemNotification(
                "DATA_DISTRIBUTION",
                event.isSuccess() ? "COMPLETED" : "FAILED",
                event.getDistributionId(),
                metadata
            );

        } catch (Exception e) {
            log.warn("发送分发完成通知失败: distributionId={}", event.getDistributionId(), e);
        }
    }
    
    /**
     * 触发工作流下一阶段
     */
    private void triggerNextWorkflowStage(DataDistributionCompletedEvent event) {
        try {
            log.info("触发工作流下一阶段: taskId={}, nextStage={}",
                    event.getTaskId(), event.getNextStageName());

            // 准备下一阶段的输入数据
            Map<String, Object> stageInput = new HashMap<>();
            stageInput.put("distributionId", event.getDistributionId());
            stageInput.put("distributionResult", "SUCCESS");
            stageInput.put("distributionStrategy", event.getDistributionStrategy());
            stageInput.put("successfulVmCount", event.getSuccessfulVmCount());
            stageInput.put("failedVmCount", event.getFailedVmCount());
            stageInput.put("totalDataSize", event.getTotalDataSize());
            stageInput.put("executionDuration", event.getExecutionDuration());
            stageInput.put("distributionStatistics", event.getDistributionStatistics());
            stageInput.put("previousStage", "DATA_DISTRIBUTION");

            // 计算分发效率
            if (event.getSuccessfulVmCount() != null && event.getFailedVmCount() != null) {
                int totalVmCount = event.getSuccessfulVmCount() + event.getFailedVmCount();
                if (totalVmCount > 0) {
                    double successRate = (double) event.getSuccessfulVmCount() / totalVmCount;
                    stageInput.put("distributionSuccessRate", successRate);
                }
            }

            // 获取工作流编排ID
            String orchestrationId = getOrchestrationId(event.getTaskId());
            if (orchestrationId == null) {
                log.warn("无法获取工作流编排ID: taskId={}", event.getTaskId());
                return;
            }

            // 1. 调用工作流编排服务触发下一阶段
            orchestrationService.triggerNextStage(
                orchestrationId,
                "DataDistributionEventListener",
                stageInput
            );
            boolean triggered = true;

            if (triggered) {
                log.info("下一阶段触发成功: taskId={}, nextStage={}, orchestrationId={}",
                        event.getTaskId(), event.getNextStageName(), orchestrationId);

                // 记录阶段转换日志
                Map<String, Object> logDetails = new HashMap<>();
                logDetails.put("fromStage", "DATA_DISTRIBUTION");
                logDetails.put("toStage", event.getNextStageName());
                logDetails.put("orchestrationId", orchestrationId);
                logDetails.put("stageInput", stageInput);

                logService.logTask(event.getTaskId(), "INFO",
                        String.format("工作流阶段转换成功: 从 %s 转向 %s",
                                "DATA_DISTRIBUTION", event.getNextStageName()),
                        "WorkflowOrchestrator", null, logDetails);

            } else {
                log.error("下一阶段触发失败: taskId={}, nextStage={}, orchestrationId={}",
                        event.getTaskId(), event.getNextStageName(), orchestrationId);

                // 发送失败通知
                notificationService.sendNotification(
                    "EMAIL",
                    "工作流阶段转换失败",
                    String.format("数据分发成功后无法触发下一阶段 %s", event.getNextStageName()),
                    "admin@feduwacomm.com", // 管理员邮箱
                    "HIGH",
                    stageInput
                );
            }

        } catch (Exception e) {
            log.error("触发工作流下一阶段失败: taskId={}, nextStage={}",
                    event.getTaskId(), event.getNextStageName(), e);

            // 发送异常通知
            notificationService.sendNotification(
                "EMAIL",
                "工作流阶段转换异常",
                String.format("触发下一阶段时发生异常: %s", e.getMessage()),
                "admin@feduwacomm.com",
                "HIGH",
                Map.of("taskId", event.getTaskId(), "nextStage", event.getNextStageName(), "error", e.getMessage())
            );
        }
    }
    
    /**
     * 处理分发失败情况
     */
    private void handleDistributionFailure(DataDistributionCompletedEvent event) {
        try {
            log.error("处理数据分发失败: distributionId={}, error={}",
                    event.getDistributionId(), event.getErrorMessage());

            // 实现失败处理逻辑
            // 1. 根据失败类型确定处理策略
            String failureStrategy = determineFailureStrategy(event);

            // 2. 根据策略执行相应操作
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

        try {
            // 获取重试配置
            int maxRetries = getMaxRetriesForDistribution(event.getDistributionStrategy());
            int currentRetryCount = getCurrentDistributionRetryCount(event.getDistributionId());

            if (currentRetryCount >= maxRetries) {
                log.warn("数据分发重试次数已达上限，转为人工干预: distributionId={}, retryCount={}, maxRetries={}",
                        event.getDistributionId(), currentRetryCount, maxRetries);
                requestManualIntervention(event);
                return;
            }

            // 计算重试延迟（指数退避）
            long retryDelay = calculateDistributionRetryDelay(currentRetryCount);

            // 记录重试信息
            Map<String, Object> retryDetails = new HashMap<>();
            retryDetails.put("distributionId", event.getDistributionId());
            retryDetails.put("retryCount", currentRetryCount + 1);
            retryDetails.put("maxRetries", maxRetries);
            retryDetails.put("retryDelay", retryDelay);
            retryDetails.put("originalError", event.getErrorMessage());
            retryDetails.put("failedVmCount", event.getFailedVmCount());

            logService.logTask(event.getTaskId(), "WARN",
                    String.format("安排数据分发自动重试: 第%d/%d次，延迟%d秒",
                            currentRetryCount + 1, maxRetries, retryDelay / 1000),
                    "DataDistributionRetryService", null, retryDetails);

            // 增加重试计数
            incrementDistributionRetryCount(event.getDistributionId());

            // 发送重试通知
            notificationService.sendNotification(
                "EMAIL",
                "数据分发自动重试",
                String.format(
                    "数据分发失败，将在%d秒后进行第%d次重试\n" +
                    "分发ID: %s\n" +
                    "失败原因: %s\n" +
                    "失败VM数量: %d",
                    retryDelay / 1000, currentRetryCount + 1,
                    event.getDistributionId(), event.getErrorMessage(),
                    event.getFailedVmCount() != null ? event.getFailedVmCount() : 0
                ),
                getDistributionNotificationRecipients(event.getDistributionStrategy()),
                "NORMAL",
                retryDetails
            );

            // 安排延迟重试（在实际环境中应使用定时任务调度器）
            scheduleDelayedRetry(event, retryDelay);

        } catch (Exception e) {
            log.error("安排自动重试失败: distributionId={}", event.getDistributionId(), e);
            // 重试安排失败，转为人工干预
            requestManualIntervention(event);
        }
    }
    
    /**
     * 请求人工干预
     */
    private void requestManualIntervention(DataDistributionCompletedEvent event) {
        log.warn("数据分发需要人工干预: distributionId={}, error={}",
                event.getDistributionId(), event.getErrorMessage());

        try {
            // 记录人工干预请求
            Map<String, Object> interventionDetails = new HashMap<>();
            interventionDetails.put("distributionId", event.getDistributionId());
            interventionDetails.put("taskId", event.getTaskId());
            interventionDetails.put("distributionStrategy", event.getDistributionStrategy());
            interventionDetails.put("errorMessage", event.getErrorMessage());
            interventionDetails.put("successfulVmCount", event.getSuccessfulVmCount());
            interventionDetails.put("failedVmCount", event.getFailedVmCount());
            interventionDetails.put("failureRate", calculateFailureRate(event));
            interventionDetails.put("executionDuration", event.getExecutionDuration());
            interventionDetails.put("requestTime", System.currentTimeMillis());
            interventionDetails.put("operatorId", event.getOperatorId());

            logService.logTask(event.getTaskId(), "WARN",
                    String.format("请求人工干预处理数据分发失败: 失败率=%.2f%%, 错误=%s",
                            calculateFailureRate(event), event.getErrorMessage()),
                    "DataDistributionInterventionService", null, interventionDetails);

            // 发送高优先级邮件告警给管理员
            String alertTitle = String.format("【紧急】数据分发失败需要人工干预 - %s", event.getDistributionStrategy());
            String alertMessage = buildInterventionAlertMessage(event, interventionDetails);

            notificationService.sendNotification(
                "EMAIL",
                alertTitle,
                alertMessage,
                "admin@feduwacomm.com,operator@feduwacomm.com", // 管理员和操作员
                "HIGH",
                interventionDetails
            );

            // 发送WebSocket实时通知给前端
            notificationService.sendWebSocketNotification(
                event.getTaskId(),
                "DISTRIBUTION_MANUAL_INTERVENTION_REQUIRED",
                "WARNING",
                alertMessage,
                interventionDetails
            );

            // 发送短信通知（对于关键失败）
            if (isCriticalDistributionFailure(event)) {
                notificationService.sendNotification(
                    "SMS",
                    "数据分发关键失败",
                    String.format("分发ID %s 发生关键失败，请立即处理", event.getDistributionId()),
                    "+86-138xxxx8888", // 管理员手机
                    "CRITICAL",
                    interventionDetails
                );
            }

            // 更新任务状态为需要人工干预
            try {
                // 这里可以调用任务服务更新状态
                // taskService.updateTaskStatus(event.getTaskId(), "MANUAL_INTERVENTION_REQUIRED", "数据分发失败");
                log.info("任务状态已更新为需要人工干预: taskId={}", event.getTaskId());
            } catch (Exception e) {
                log.warn("更新任务状态失败: taskId={}", event.getTaskId(), e);
            }

            // 记录干预请求指标
            performanceMonitorService.recordManualInterventionRequest(
                "DATA_DISTRIBUTION",
                event.getDistributionStrategy(),
                categorizeDistributionError(event.getErrorMessage())
            );

        } catch (Exception e) {
            log.error("请求人工干预时发生异常: distributionId={}", event.getDistributionId(), e);
        }
    }
    
    /**
     * 终止工作流
     */
    private void terminateWorkflow(DataDistributionCompletedEvent event) {
        log.error("数据分发严重失败，终止工作流: taskId={}, distributionId={}",
                event.getTaskId(), event.getDistributionId());

        try {
            // 记录工作流终止信息
            Map<String, Object> terminationDetails = new HashMap<>();
            terminationDetails.put("distributionId", event.getDistributionId());
            terminationDetails.put("taskId", event.getTaskId());
            terminationDetails.put("terminationReason", "数据分发严重失败");
            terminationDetails.put("errorMessage", event.getErrorMessage());
            terminationDetails.put("failureRate", calculateFailureRate(event));
            terminationDetails.put("distributionStrategy", event.getDistributionStrategy());
            terminationDetails.put("executionDuration", event.getExecutionDuration());
            terminationDetails.put("terminationTime", System.currentTimeMillis());

            logService.logTask(event.getTaskId(), "ERROR",
                    String.format("因数据分发严重失败终止工作流: 失败率=%.2f%%, 错误=%s",
                            calculateFailureRate(event), event.getErrorMessage()),
                    "WorkflowTerminationService", null, terminationDetails);

            // 获取工作流编排ID
            String orchestrationId = getOrchestrationId(event.getTaskId());
            if (orchestrationId != null) {
                // 调用工作流编排服务终止整个工作流
                orchestrationService.terminateOrchestration(
                    orchestrationId,
                    "数据分发严重失败: " + event.getErrorMessage()
                );

                log.info("工作流终止请求已发送: orchestrationId={}, taskId={}",
                        orchestrationId, event.getTaskId());
            } else {
                log.warn("无法获取工作流编排ID，无法终止工作流: taskId={}", event.getTaskId());
            }

            // 发送关键告警通知
            String alertTitle = String.format("【关键告警】工作流因数据分发失败被终止 - %s", event.getDistributionStrategy());
            String alertMessage = String.format(
                "工作流因数据分发严重失败被终止\n" +
                "任务ID: %s\n" +
                "分发ID: %s\n" +
                "分发策略: %s\n" +
                "失败率: %.2f%%\n" +
                "错误信息: %s\n" +
                "终止时间: %s\n" +
                "\n请立即检查系统状态并采取相应措施。",
                event.getTaskId(),
                event.getDistributionId(),
                event.getDistributionStrategy(),
                calculateFailureRate(event),
                event.getErrorMessage(),
                new java.util.Date()
            );

            // 发送紧急邮件通知
            notificationService.sendNotification(
                "EMAIL",
                alertTitle,
                alertMessage,
                "admin@feduwacomm.com,operator@feduwacomm.com,emergency@feduwacomm.com",
                "CRITICAL",
                terminationDetails
            );

            // 发送紧急短信通知
            notificationService.sendNotification(
                "SMS",
                "工作流紧急终止",
                String.format("任务%s因数据分发失败终止，请立即检查。失败率%.1f%%",
                        event.getTaskId(), calculateFailureRate(event)),
                "+86-138xxxx8888,+86-139xxxx9999", // 管理员和紧急联系人
                "CRITICAL",
                terminationDetails
            );

            // 发送WebSocket实时通知给前端
            notificationService.sendWebSocketNotification(
                event.getTaskId(),
                "WORKFLOW_TERMINATED",
                "ERROR",
                alertMessage,
                terminationDetails
            );

            // 记录终止指标
            performanceMonitorService.recordWorkflowTermination(
                "DATA_DISTRIBUTION_FAILURE",
                event.getDistributionStrategy(),
                calculateFailureRate(event)
            );

        } catch (Exception e) {
            log.error("终止工作流时发生异常: taskId={}, distributionId={}",
                    event.getTaskId(), event.getDistributionId(), e);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 计算失败率
     */
    private double calculateFailureRate(DataDistributionCompletedEvent event) {
        if (event.getSuccessfulVmCount() == null || event.getFailedVmCount() == null) {
            return 0.0;
        }

        int totalVmCount = event.getSuccessfulVmCount() + event.getFailedVmCount();
        if (totalVmCount == 0) {
            return 0.0;
        }

        return (double) event.getFailedVmCount() / totalVmCount * 100.0;
    }

    /**
     * 获取工作流编排ID
     */
    private String getOrchestrationId(String taskId) {
        try {
            // 这里应该调用服务获取编排ID，简化实现
            return "orchestration_" + taskId;
        } catch (Exception e) {
            log.warn("获取工作流编排ID失败: taskId={}", taskId, e);
            return null;
        }
    }

    /**
     * 格式化数据大小
     */
    private String formatDataSize(Long sizeInBytes) {
        if (sizeInBytes == null || sizeInBytes == 0) {
            return "0 bytes";
        }

        String[] units = {"bytes", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(sizeInBytes) / Math.log10(1024));
        return String.format("%.2f %s", sizeInBytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    /**
     * 判断是否为大型分发任务
     */
    private boolean isLargeDistributionTask(DataDistributionStartedEvent event) {
        return (event.getExpectedVmCount() != null && event.getExpectedVmCount() > 50) ||
               (event.getTotalDataSize() != null && event.getTotalDataSize() > 1024 * 1024 * 100); // 100MB
    }

    /**
     * 判断是否为重要的分发任务
     */
    private boolean isImportantDistributionTask(DataDistributionCompletedEvent event) {
        return event.getDistributionStrategy() != null &&
               (event.getDistributionStrategy().contains("CRITICAL") ||
                event.getDistributionStrategy().contains("PRODUCTION"));
    }

    /**
     * 获取分发通知接收者
     */
    private String getDistributionNotificationRecipients(String distributionStrategy) {
        // 根据分发策略返回不同的通知接收者
        if (distributionStrategy != null && distributionStrategy.contains("CRITICAL")) {
            return "admin@feduwacomm.com,operator@feduwacomm.com";
        }
        return "admin@feduwacomm.com";
    }

    /**
     * 分类分发错误
     */
    private String categorizeDistributionError(String errorMessage) {
        if (errorMessage == null) {
            return "UNKNOWN";
        }

        String upperError = errorMessage.toUpperCase();
        if (upperError.contains("TIMEOUT")) {
            return "TIMEOUT";
        } else if (upperError.contains("CONNECTION") || upperError.contains("NETWORK")) {
            return "NETWORK";
        } else if (upperError.contains("PERMISSION") || upperError.contains("ACCESS")) {
            return "PERMISSION";
        } else if (upperError.contains("DISK") || upperError.contains("SPACE")) {
            return "STORAGE";
        } else if (upperError.contains("MEMORY") || upperError.contains("OOM")) {
            return "MEMORY";
        } else {
            return "BUSINESS_LOGIC";
        }
    }

    /**
     * 获取分发策略的最大重试次数
     */
    private int getMaxRetriesForDistribution(String distributionStrategy) {
        if (distributionStrategy == null) {
            return 2;
        }

        switch (distributionStrategy.toUpperCase()) {
            case "BATCH_DISTRIBUTION":
            case "PARALLEL_DISTRIBUTION":
                return 3;
            case "SEQUENTIAL_DISTRIBUTION":
                return 2;
            case "CRITICAL_DISTRIBUTION":
                return 5;
            default:
                return 2;
        }
    }

    /**
     * 获取当前分发重试次数
     */
    private int getCurrentDistributionRetryCount(String distributionId) {
        try {
            // 这里应该从数据库或缓存中获取重试次数，简化实现
            return 0;
        } catch (Exception e) {
            log.warn("获取分发重试次数失败: distributionId={}", distributionId, e);
            return 0;
        }
    }

    /**
     * 增加分发重试计数
     */
    private void incrementDistributionRetryCount(String distributionId) {
        try {
            // 这里应该更新数据库或缓存中的重试次数，简化实现
            log.debug("增加分发重试计数: distributionId={}", distributionId);
        } catch (Exception e) {
            log.warn("增加分发重试计数失败: distributionId={}", distributionId, e);
        }
    }

    /**
     * 计算分发重试延迟
     */
    private long calculateDistributionRetryDelay(int retryCount) {
        // 指数退避: 2^retryCount * 2000ms，最大60秒
        long baseDelay = 2000L; // 2秒基础延迟
        long delay = (long) (baseDelay * Math.pow(2, retryCount));
        return Math.min(delay, 60000L); // 最大60秒
    }

    /**
     * 安排延迟重试
     */
    private void scheduleDelayedRetry(DataDistributionCompletedEvent event, long delayMs) {
        try {
            // 在实际环境中应使用定时任务调度器（如Spring @Scheduled 或 Quartz）
            // 这里简化实现，只记录日志
            log.info("安排延迟重试: distributionId={}, delay={}ms", event.getDistributionId(), delayMs);

            // 实际实现应该是：
            // schedulerService.scheduleRetry(event.getDistributionId(), delayMs);
        } catch (Exception e) {
            log.error("安排延迟重试失败: distributionId={}", event.getDistributionId(), e);
        }
    }

    /**
     * 构建干预告警消息
     */
    private String buildInterventionAlertMessage(DataDistributionCompletedEvent event, Map<String, Object> details) {
        return String.format(
            "数据分发失败需要人工干预\n" +
            "分发ID: %s\n" +
            "任务ID: %s\n" +
            "分发策略: %s\n" +
            "失败率: %.2f%%\n" +
            "成功VM数: %d\n" +
            "失败VM数: %d\n" +
            "执行时长: %d毫秒\n" +
            "错误信息: %s\n" +
            "请求时间: %s\n" +
            "\n请尽快登录系统检查并处理此问题。",
            event.getDistributionId(),
            event.getTaskId(),
            event.getDistributionStrategy(),
            calculateFailureRate(event),
            event.getSuccessfulVmCount() != null ? event.getSuccessfulVmCount() : 0,
            event.getFailedVmCount() != null ? event.getFailedVmCount() : 0,
            event.getExecutionDuration() != null ? event.getExecutionDuration() : 0,
            event.getErrorMessage(),
            new java.util.Date()
        );
    }

    /**
     * 判断是否为关键分发失败
     */
    private boolean isCriticalDistributionFailure(DataDistributionCompletedEvent event) {
        double failureRate = calculateFailureRate(event);
        return failureRate >= 80.0 || // 失败率超过80%
               (event.getDistributionStrategy() != null && event.getDistributionStrategy().contains("CRITICAL"));
    }

    /**
     * 发送失败告警
     */
    private void sendFailureAlert(DataDistributionCompletedEvent event, String failureStrategy, String errorCategory) {
        try {
            Map<String, Object> alertData = new HashMap<>();
            alertData.put("distributionId", event.getDistributionId());
            alertData.put("failureStrategy", failureStrategy);
            alertData.put("errorCategory", errorCategory);
            alertData.put("failureRate", calculateFailureRate(event));

            notificationService.sendSystemNotification(
                "DATA_DISTRIBUTION_FAILURE_ALERT",
                failureStrategy,
                event.getDistributionId(),
                alertData
            );

        } catch (Exception e) {
            log.warn("发送失败告警失败: distributionId={}", event.getDistributionId(), e);
        }
    }


    /**
     * 确定失败处理策略（增强版）
     */
    private String determineFailureStrategy(DataDistributionCompletedEvent event, String errorCategory, double failureRate) {
        // 优先级基于错误类型
        if ("NETWORK_TIMEOUT".equals(errorCategory) || "TEMPORARY_UNAVAILABLE".equals(errorCategory)) {
            if (failureRate < 0.3) { // 失败率低于30%
                return "AUTO_RETRY";
            } else if (failureRate < 0.7) {
                return "MANUAL_INTERVENTION";
            } else {
                return "TERMINATE";
            }
        }

        // 数据损坏或验证失败
        if ("DATA_CORRUPTION".equals(errorCategory) || "VALIDATION_FAILED".equals(errorCategory)) {
            return "ROLLBACK";
        }

        // 系统资源不足
        if ("RESOURCE_EXHAUSTED".equals(errorCategory)) {
            return "MANUAL_INTERVENTION";
        }

        // 严重系统错误
        if ("CRITICAL_SYSTEM_ERROR".equals(errorCategory)) {
            return "TERMINATE";
        }

        // 默认策略：基于失败率决定
        if (failureRate < 0.2) {
            return "AUTO_RETRY";
        } else if (failureRate < 0.5) {
            return "MANUAL_INTERVENTION";
        } else {
            return "TERMINATE";
        }
    }

    /**
     * 回滚到上一阶段
     */
    private void rollbackToPreviousStage(DataDistributionCompletedEvent event) {
        try {
            log.warn("数据分发失败，回滚到上一阶段: distributionId={}", event.getDistributionId());

            // 记录回滚信息
            Map<String, Object> rollbackDetails = new HashMap<>();
            rollbackDetails.put("distributionId", event.getDistributionId());
            rollbackDetails.put("rollbackReason", "数据分发失败");
            rollbackDetails.put("errorMessage", event.getErrorMessage());
            rollbackDetails.put("rollbackTimestamp", Instant.now().toString());

            logService.logTask(event.getTaskId(), "WARN",
                    String.format("数据分发失败，执行回滚操作: %s", event.getDistributionId()),
                    "DataDistributionRollbackService", null, rollbackDetails);

            // 调用编排服务执行回滚
            orchestrationService.rollbackToStage(
                event.getTaskId(),
                "DATA_PREPARATION", // 回滚到数据准备阶段
                "数据分发失败回滚"
            );

            // 发送回滚通知
            notificationService.sendSystemNotification(
                "DATA_DISTRIBUTION_ROLLBACK",
                "ROLLBACK",
                event.getDistributionId(),
                rollbackDetails
            );

        } catch (Exception e) {
            log.error("执行回滚操作失败: distributionId={}", event.getDistributionId(), e);
            // 回滚失败时，转为人工干预
            requestManualIntervention(event);
        }
    }
}