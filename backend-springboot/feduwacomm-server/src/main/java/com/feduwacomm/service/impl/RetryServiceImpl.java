package com.feduwacomm.service.impl;

import com.feduwacomm.orchestration.WorkflowStage;
import com.feduwacomm.service.RetryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 重试服务实现
 * 提供统一的重试逻辑和策略管理
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-28
 */
@Slf4j
@Service
public class RetryServiceImpl implements RetryService {

    private final TaskScheduler taskScheduler;
    private final InitialModelDistributionRetryExecutor initialModelDistributionRetryExecutor;

    // 重试次数跟踪
    private final Map<String, Integer> retryCountMap = new ConcurrentHashMap<>();

    // 重试任务跟踪
    private final Map<String, ScheduledFuture<?>> scheduledRetries = new ConcurrentHashMap<>();

    // 重试策略配置
    private static final int MAX_WORKFLOW_RETRIES = 3;
    private static final int MAX_DISTRIBUTION_RETRIES = 5;
    private static final int MAX_WEBSOCKET_RETRIES = 3;

    private static final long BASE_DELAY_MS = 1000; // 1秒基础延迟
    private static final double BACKOFF_MULTIPLIER = 2.0; // 指数退避倍数

    public RetryServiceImpl(@Qualifier("heartBeatScheduler") TaskScheduler taskScheduler,
                            InitialModelDistributionRetryExecutor initialModelDistributionRetryExecutor) {
        this.taskScheduler = taskScheduler;
        this.initialModelDistributionRetryExecutor = initialModelDistributionRetryExecutor;
    }

    @Override
    public RetryResult shouldRetryWorkflowStage(String orchestrationId, WorkflowStage stage,
                                              int currentAttempt, String errorMessage) {
        String key = "workflow:" + orchestrationId + ":" + stage.name();

        // 检查是否超过最大重试次数
        if (currentAttempt >= MAX_WORKFLOW_RETRIES) {
            log.warn("工作流阶段重试次数已达上限: orchestrationId={}, stage={}, attempts={}",
                    orchestrationId, stage, currentAttempt);
            return new RetryResult(false, 0, currentAttempt, "超过最大重试次数");
        }

        // 检查错误类型是否应该重试
        if (!isRetryableWorkflowError(errorMessage)) {
            log.warn("工作流阶段错误不适合重试: orchestrationId={}, stage={}, error={}",
                    orchestrationId, stage, errorMessage);
            return new RetryResult(false, 0, currentAttempt, "错误类型不可重试");
        }

        // 计算延迟时间（指数退避）
        long delayMillis = calculateBackoffDelay(currentAttempt);

        log.info("工作流阶段将重试: orchestrationId={}, stage={}, attempt={}, delay={}ms",
                orchestrationId, stage, currentAttempt + 1, delayMillis);

        return new RetryResult(true, delayMillis, currentAttempt + 1, "可重试错误");
    }

    @Override
    public RetryResult shouldRetryDistribution(String distributionId, int currentAttempt,
                                             String errorMessage) {
        // 检查是否超过最大重试次数
        if (currentAttempt >= MAX_DISTRIBUTION_RETRIES) {
            log.warn("分发操作重试次数已达上限: distributionId={}, attempts={}",
                    distributionId, currentAttempt);
            return new RetryResult(false, 0, currentAttempt, "超过最大重试次数");
        }

        // 检查错误类型
        if (!isRetryableDistributionError(errorMessage)) {
            log.warn("分发操作错误不适合重试: distributionId={}, error={}",
                    distributionId, errorMessage);
            return new RetryResult(false, 0, currentAttempt, "错误类型不可重试");
        }

        long delayMillis = calculateBackoffDelay(currentAttempt);

        log.info("分发操作将重试: distributionId={}, attempt={}, delay={}ms",
                distributionId, currentAttempt + 1, delayMillis);

        return new RetryResult(true, delayMillis, currentAttempt + 1, "网络或临时错误");
    }

    @Override
    public RetryResult shouldRetryWebSocketSend(String vmId, String messageType,
                                              int currentAttempt, String errorMessage) {
        // 检查是否超过最大重试次数
        if (currentAttempt >= MAX_WEBSOCKET_RETRIES) {
            log.warn("WebSocket消息发送重试次数已达上限: vmId={}, messageType={}, attempts={}",
                    vmId, messageType, currentAttempt);
            return new RetryResult(false, 0, currentAttempt, "超过最大重试次数");
        }

        // 检查错误类型
        if (!isRetryableWebSocketError(errorMessage)) {
            log.warn("WebSocket消息发送错误不适合重试: vmId={}, messageType={}, error={}",
                    vmId, messageType, errorMessage);
            return new RetryResult(false, 0, currentAttempt, "错误类型不可重试");
        }

        // WebSocket重试使用较短的延迟
        long delayMillis = Math.min(calculateBackoffDelay(currentAttempt), 5000); // 最大5秒

        log.info("WebSocket消息发送将重试: vmId={}, messageType={}, attempt={}, delay={}ms",
                vmId, messageType, currentAttempt + 1, delayMillis);

        return new RetryResult(true, delayMillis, currentAttempt + 1, "连接或网络错误");
    }

    @Override
    public void scheduleRetry(String retryType, String entityId, long delayMillis,
                            int nextAttempt, Map<String, Object> context) {
        String retryKey = retryType + ":" + entityId;

        // 取消之前的重试任务
        cancelRetry(retryType, entityId);

        // 更新重试次数
        retryCountMap.put(retryKey, nextAttempt);

        // 安排新的重试任务
        Instant retryTime = Instant.now().plus(Duration.ofMillis(delayMillis));
        ScheduledFuture<?> future = taskScheduler.schedule(() -> {
            try {
                executeRetry(retryType, entityId, nextAttempt, context);
            } catch (Exception e) {
                log.error("执行重试任务失败: retryType={}, entityId={}, attempt={}",
                        retryType, entityId, nextAttempt, e);
            } finally {
                // 清理已完成的任务
                scheduledRetries.remove(retryKey);
            }
        }, retryTime);

        scheduledRetries.put(retryKey, future);

        log.info("已安排重试任务: retryType={}, entityId={}, attempt={}, delay={}ms",
                retryType, entityId, nextAttempt, delayMillis);
    }

    @Override
    public void cancelRetry(String retryType, String entityId) {
        String retryKey = retryType + ":" + entityId;

        ScheduledFuture<?> future = scheduledRetries.remove(retryKey);
        if (future != null && !future.isDone()) {
            future.cancel(false);
            log.debug("已取消重试任务: retryType={}, entityId={}", retryType, entityId);
        }
    }

    @Override
    public int getRetryCount(String retryType, String entityId) {
        String retryKey = retryType + ":" + entityId;
        return retryCountMap.getOrDefault(retryKey, 0);
    }

    @Override
    public void resetRetryCount(String retryType, String entityId) {
        String retryKey = retryType + ":" + entityId;
        retryCountMap.remove(retryKey);
        cancelRetry(retryType, entityId);

        log.debug("已重置重试计数: retryType={}, entityId={}", retryType, entityId);
    }

    /**
     * 检查工作流错误是否可重试
     */
    private boolean isRetryableWorkflowError(String errorMessage) {
        if (errorMessage == null) {
            return false;
        }

        String errorLower = errorMessage.toLowerCase();

        // 不可重试的错误类型
        if (errorLower.contains("权限") || errorLower.contains("认证") ||
            errorLower.contains("参数错误") || errorLower.contains("配置错误") ||
            errorLower.contains("validation")) {
            return false;
        }

        // 可重试的错误类型
        return errorLower.contains("连接") || errorLower.contains("网络") ||
               errorLower.contains("超时") || errorLower.contains("临时") ||
               errorLower.contains("timeout") || errorLower.contains("connection");
    }

    /**
     * 检查分发错误是否可重试
     */
    private boolean isRetryableDistributionError(String errorMessage) {
        if (errorMessage == null) {
            return false;
        }

        String errorLower = errorMessage.toLowerCase();

        // 可重试的分发错误
        return errorLower.contains("网络") || errorLower.contains("连接") ||
               errorLower.contains("超时") || errorLower.contains("io") ||
               errorLower.contains("socket") || errorLower.contains("临时不可用");
    }

    /**
     * 检查WebSocket错误是否可重试
     */
    private boolean isRetryableWebSocketError(String errorMessage) {
        if (errorMessage == null) {
            return false;
        }

        String errorLower = errorMessage.toLowerCase();

        // 可重试的WebSocket错误
        return errorLower.contains("连接") || errorLower.contains("断开") ||
               errorLower.contains("网络") || errorLower.contains("超时") ||
               errorLower.contains("websocket") || errorLower.contains("session");
    }

    /**
     * 计算指数退避延迟
     */
    private long calculateBackoffDelay(int attempt) {
        return (long) (BASE_DELAY_MS * Math.pow(BACKOFF_MULTIPLIER, attempt));
    }

    /**
     * 执行具体的重试逻辑
     */
    private void executeRetry(String retryType, String entityId, int attempt,
                            Map<String, Object> context) {
        log.info("开始执行重试: retryType={}, entityId={}, attempt={}", retryType, entityId, attempt);

        switch (retryType) {
            case "workflow":
                // 工作流重试逻辑 - 由具体的编排服务处理
                log.info("工作流重试将由编排服务处理: entityId={}", entityId);
                break;

            case "distribution":
                // 分发重试逻辑 - 由分发服务处理
                log.info("分发重试将由分发服务处理: entityId={}", entityId);
                break;

            case "initial_model_distribution":
                initialModelDistributionRetryExecutor.retry(entityId, attempt, context);
                break;

            case "websocket":
                // WebSocket重试逻辑 - 由WebSocket服务处理
                log.info("WebSocket重试将由WebSocket服务处理: entityId={}", entityId);
                break;

            default:
                log.warn("未知的重试类型: retryType={}, entityId={}", retryType, entityId);
        }
    }
}
