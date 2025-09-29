package com.feduwacomm.service;

import com.feduwacomm.orchestration.WorkflowStage;

/**
 * 重试服务接口
 * 负责处理各种操作的重试逻辑
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-28
 */
public interface RetryService {

    /**
     * 重试策略结果
     */
    class RetryResult {
        private final boolean shouldRetry;
        private final long delayMillis;
        private final int nextAttempt;
        private final String reason;

        public RetryResult(boolean shouldRetry, long delayMillis, int nextAttempt, String reason) {
            this.shouldRetry = shouldRetry;
            this.delayMillis = delayMillis;
            this.nextAttempt = nextAttempt;
            this.reason = reason;
        }

        public boolean shouldRetry() { return shouldRetry; }
        public long getDelayMillis() { return delayMillis; }
        public int getNextAttempt() { return nextAttempt; }
        public String getReason() { return reason; }
    }

    /**
     * 检查工作流阶段是否应该重试
     *
     * @param orchestrationId 工作流ID
     * @param stage 当前阶段
     * @param currentAttempt 当前重试次数
     * @param errorMessage 错误信息
     * @return 重试结果
     */
    RetryResult shouldRetryWorkflowStage(String orchestrationId, WorkflowStage stage,
                                       int currentAttempt, String errorMessage);

    /**
     * 检查分发操作是否应该重试
     *
     * @param distributionId 分发ID
     * @param currentAttempt 当前重试次数
     * @param errorMessage 错误信息
     * @return 重试结果
     */
    RetryResult shouldRetryDistribution(String distributionId, int currentAttempt,
                                      String errorMessage);

    /**
     * 检查WebSocket消息发送是否应该重试
     *
     * @param vmId 虚拟机ID
     * @param messageType 消息类型
     * @param currentAttempt 当前重试次数
     * @param errorMessage 错误信息
     * @return 重试结果
     */
    RetryResult shouldRetryWebSocketSend(String vmId, String messageType,
                                       int currentAttempt, String errorMessage);

    /**
     * 安排重试任务
     *
     * @param retryType 重试类型
     * @param entityId 实体ID
     * @param delayMillis 延迟毫秒数
     * @param nextAttempt 下次重试次数
     * @param context 重试上下文
     */
    void scheduleRetry(String retryType, String entityId, long delayMillis,
                      int nextAttempt, java.util.Map<String, Object> context);

    /**
     * 取消重试任务
     *
     * @param retryType 重试类型
     * @param entityId 实体ID
     */
    void cancelRetry(String retryType, String entityId);

    /**
     * 获取实体的重试次数
     *
     * @param retryType 重试类型
     * @param entityId 实体ID
     * @return 重试次数
     */
    int getRetryCount(String retryType, String entityId);

    /**
     * 重置实体的重试次数
     *
     * @param retryType 重试类型
     * @param entityId 实体ID
     */
    void resetRetryCount(String retryType, String entityId);
}