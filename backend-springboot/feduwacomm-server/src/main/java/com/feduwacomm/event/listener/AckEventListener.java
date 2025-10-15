package com.feduwacomm.event.listener;

import com.feduwacomm.entity.FederatedTask;
import com.feduwacomm.event.AckProgressChangedEvent;
import com.feduwacomm.event.AckStatusChangedEvent;
import com.feduwacomm.mapper.FederatedTasksMapper;
import com.feduwacomm.mapper.TaskParticipantsMapper;
import com.feduwacomm.service.FederatedLearningOrchestrator;
import com.feduwacomm.service.WebSocketService;
import com.feduwacomm.entity.VmAckTracking;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ACK事件监听器
 * 监听ACK状态和进度变更事件，并通过WebSocket推送给客户端
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-29
 */
@Slf4j
@Component
public class AckEventListener {

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private FederatedLearningOrchestrator learningOrchestrator;

    @Autowired
    private TaskParticipantsMapper taskParticipantsMapper;

    @Autowired
    private FederatedTasksMapper federatedTasksMapper;

    /**
     * 监听ACK状态变更事件
     */
    @Async("ackEventExecutor")
    @EventListener
    public void handleAckStatusChanged(AckStatusChangedEvent event) {
        try {
            log.debug("处理ACK状态变更事件: {}", event.getSummary());

            // 构建WebSocket消息
            Map<String, Object> message = new HashMap<>();
            message.put("type", "ACK_STATUS_CHANGED");
            message.put("taskId", event.getTaskId());
            message.put("vmId", event.getVmId());
            message.put("ackType", event.getAckType().name());
            message.put("newStatus", event.getNewStatus().name());
            message.put("timestamp", System.currentTimeMillis());

            if (event.getOldStatus() != null) {
                message.put("oldStatus", event.getOldStatus().name());
            }

            if (event.getErrorMessage() != null) {
                message.put("errorMessage", event.getErrorMessage());
            }

            if (event.getRoundNumber() != null) {
                message.put("roundNumber", event.getRoundNumber());
            }

            message.put("isNewRecord", event.isNewRecord());
            message.put("isStatusUpdate", event.isStatusUpdate());
            message.put("isSuccess", event.isSuccess());
            message.put("isFailed", event.isFailed());
            message.put("isTimeout", event.isTimeout());

            // 推送给相关的WebSocket客户端
            // 1. 推送给管理员
            webSocketService.sendToAdmins(message);

            // 2. 推送给任务相关的用户
            webSocketService.sendToTaskSubscribers(event.getTaskId(), message);

            // 3. 如果是重要状态变更，推送给所有在线用户
            if (event.isFailed() || event.isTimeout()) {
                webSocketService.sendToAllUsers(message);
            }

            log.debug("ACK状态变更事件WebSocket推送完成: taskId={}, vmId={}, ackType={}, status={}",
                    event.getTaskId(), event.getVmId(), event.getAckType(), event.getNewStatus());

        } catch (Exception e) {
            log.error("处理ACK状态变更事件失败: {}", event.getSummary(), e);
        }
    }

    /**
     * 监听ACK进度变更事件
     */
    @Async("ackEventExecutor")
    @EventListener
    public void handleAckProgressChanged(AckProgressChangedEvent event) {
        try {
            log.debug("处理ACK进度变更事件: {}", event.getSummary());

            // 构建WebSocket消息
            Map<String, Object> message = new HashMap<>();
            message.put("type", "ACK_PROGRESS_CHANGED");
            message.put("taskId", event.getTaskId());
            message.put("ackType", event.getAckType().name());
            message.put("timestamp", System.currentTimeMillis());

            // 进度信息
            message.put("progressPercentage", event.getProgressPercentage());
            message.put("acknowledgedVms", event.getAcknowledgedVms());
            message.put("totalVms", event.getTotalVms());
            message.put("successVms", event.getSuccessVms());
            message.put("failedVms", event.getFailedVms());
            message.put("timeoutVms", event.getTimeoutVms());

            // 状态标志
            message.put("allCompleted", event.isAllCompleted());
            message.put("allSuccess", event.isAllSuccess());
            message.put("hasIssues", event.hasIssues());
            message.put("isHealthy", event.isHealthy());

            // 描述信息
            message.put("progressDescription", event.getProgressDescription());
            message.put("detailedDescription", event.getDetailedStatusDescription());

            if (event.getRoundNumber() != null) {
                message.put("roundNumber", event.getRoundNumber());
            }

            // 推送给相关的WebSocket客户端
            // 1. 推送给管理员
            webSocketService.sendToAdmins(message);

            // 2. 推送给任务相关的用户
            webSocketService.sendToTaskSubscribers(event.getTaskId(), message);

            // 3. 如果任务完成或有问题，推送给所有在线用户
            if (event.isAllCompleted() || event.hasIssues()) {
                webSocketService.sendToAllUsers(message);
            }

            log.debug("ACK进度变更事件WebSocket推送完成: taskId={}, ackType={}, progress={}",
                    event.getTaskId(), event.getAckType(), event.getProgressDescription());

        } catch (Exception e) {
            log.error("处理ACK进度变更事件失败: {}", event.getSummary(), e);
        }
    }

    /**
     * 监听重要的ACK事件（同步处理）
     * 用于关键业务逻辑，如任务状态更新等
     */
    @EventListener
    public void handleCriticalAckEvents(AckProgressChangedEvent event) {
        try {
            // 如果所有VM都确认完成，可能需要触发下一阶段的业务逻辑
            if (event.isAllSuccess()) {
            log.info("任务ACK全部成功完成: taskId={}, ackType={}, 可能需要触发下一阶段",
                    event.getTaskId(), event.getAckType());

            if (event.getAckType() == VmAckTracking.AckType.GLOBAL_MODEL_BROADCAST) {
                triggerNextRoundIfNeeded(event);
            }
        }

        // 如果有失败或超时，可能需要特殊处理
        if (event.hasIssues()) {
            log.warn("任务ACK存在问题: taskId={}, ackType={}, 问题VM数={}, 详情={}",
                        event.getTaskId(), event.getAckType(), event.getIssueVmCount(),
                        event.getDetailedStatusDescription());

                // 这里可以添加问题处理逻辑，如：
                // - 标记有问题的VM
                // - 发送告警通知
                // - 自动重试机制
                // - 等等
            }

        } catch (Exception e) {
            log.error("处理关键ACK事件失败: {}", event.getSummary(), e);
        }
    }

    private void triggerNextRoundIfNeeded(AckProgressChangedEvent event) {
        FederatedTask task = federatedTasksMapper.selectTaskById(event.getTaskId());
        if (task == null) {
            log.warn("无法触发下一轮次: 任务不存在, taskId={}", event.getTaskId());
            return;
        }

        Integer currentRound = event.getRoundNumber();
        if (currentRound == null) {
            currentRound = task.getCurrentRound();
            if (currentRound == null) {
                log.warn("无法触发下一轮次: roundNumber为空, taskId={}", event.getTaskId());
                return;
            }
        }

        int nextRound = currentRound + 1;
        Integer totalRounds = task.getTotalRounds();
        if (totalRounds == null || nextRound > totalRounds) {
            log.info("任务已完成全部轮次，无需触发下一轮: taskId={}, currentRound={}, totalRounds={}",
                    event.getTaskId(), currentRound, totalRounds);
            return;
        }

        var vmIds = taskParticipantsMapper.selectVmIdsByTaskId(event.getTaskId());
        if (vmIds == null || vmIds.isEmpty()) {
            log.warn("无法触发下一轮次: 未找到参与的VM, taskId={}", event.getTaskId());
            return;
        }

        try {
            learningOrchestrator.startFederatedRound(event.getTaskId(), nextRound, List.copyOf(vmIds));
            log.info("已触发下一轮联邦训练: taskId={}, nextRound={}, participantCount={}",
                    event.getTaskId(), nextRound, vmIds.size());
        } catch (Exception ex) {
            log.error("触发下一轮联邦训练失败: taskId={}, nextRound={}, error={}",
                    event.getTaskId(), nextRound, ex.getMessage(), ex);
        }
    }
}
