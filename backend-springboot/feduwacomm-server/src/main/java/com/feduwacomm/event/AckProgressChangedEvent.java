package com.feduwacomm.event;

import com.feduwacomm.entity.VmAckTracking;
import com.feduwacomm.service.cache.model.AckProgress;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * ACK进度变更事件
 * 当任务的ACK进度发生变化时发布此事件
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-29
 */
@Getter
public class AckProgressChangedEvent extends ApplicationEvent {

    private final String taskId;
    private final VmAckTracking.AckType ackType;
    private final AckProgress currentProgress;
    private final boolean allCompleted;
    private final boolean allSuccess;
    private final Integer roundNumber;

    public AckProgressChangedEvent(Object source, String taskId, VmAckTracking.AckType ackType,
                                  AckProgress currentProgress, Integer roundNumber) {
        super(source);
        this.taskId = taskId;
        this.ackType = ackType;
        this.currentProgress = currentProgress;
        this.allCompleted = currentProgress != null && currentProgress.isAllCompleted();
        this.allSuccess = currentProgress != null && currentProgress.isAllSuccess();
        this.roundNumber = roundNumber;
    }

    /**
     * 创建进度变更事件
     */
    public static AckProgressChangedEvent create(Object source, String taskId,
                                                VmAckTracking.AckType ackType, AckProgress progress,
                                                Integer roundNumber) {
        return new AckProgressChangedEvent(source, taskId, ackType, progress, roundNumber);
    }

    /**
     * 获取进度百分比
     */
    public double getProgressPercentage() {
        return currentProgress != null ? currentProgress.getProgressPercentage() : 0.0;
    }

    /**
     * 获取已确认VM数量
     */
    public int getAcknowledgedVms() {
        return currentProgress != null ? currentProgress.getAcknowledgedVms() : 0;
    }

    /**
     * 获取总VM数量
     */
    public int getTotalVms() {
        return currentProgress != null ? currentProgress.getTotalVms() : 0;
    }

    /**
     * 获取成功VM数量
     */
    public int getSuccessVms() {
        return currentProgress != null ? currentProgress.getSuccessVmCount() : 0;
    }

    /**
     * 获取失败VM数量
     */
    public int getFailedVms() {
        return currentProgress != null ? currentProgress.getFailedVmCount() : 0;
    }

    /**
     * 获取超时VM数量
     */
    public int getTimeoutVms() {
        return currentProgress != null ? currentProgress.getTimeoutVmCount() : 0;
    }

    /**
     * 检查是否有问题（失败或超时）
     */
    public boolean hasIssues() {
        return currentProgress != null && currentProgress.hasIssues();
    }

    /**
     * 获取问题VM数量
     */
    public int getIssueVmCount() {
        return currentProgress != null ? currentProgress.getIssueVmCount() : 0;
    }

    /**
     * 检查进度是否健康
     */
    public boolean isHealthy() {
        return currentProgress != null && currentProgress.isHealthy();
    }

    /**
     * 获取进度描述
     */
    public String getProgressDescription() {
        return currentProgress != null ? currentProgress.getProgressDescription() : "0/0 (0.0%)";
    }

    /**
     * 获取详细状态描述
     */
    public String getDetailedStatusDescription() {
        return currentProgress != null ? currentProgress.getDetailedStatusDescription() : "无进度信息";
    }

    /**
     * 获取事件摘要信息
     */
    public String getSummary() {
        String roundInfo = roundNumber != null ? " [轮次:" + roundNumber + "]" : "";
        String statusInfo = allCompleted ? (allSuccess ? " ✓ 全部成功" : " ⚠ 有失败") : " ⏳ 进行中";
        String issueInfo = hasIssues() ? " (" + getIssueVmCount() + "个问题)" : "";

        return String.format("ACK进度 - 任务:%s, 类型:%s, 进度:%s%s%s%s",
                taskId, ackType, getProgressDescription(), roundInfo, statusInfo, issueInfo);
    }

    @Override
    public String toString() {
        return "AckProgressChangedEvent{" +
                "taskId='" + taskId + '\'' +
                ", ackType=" + ackType +
                ", allCompleted=" + allCompleted +
                ", allSuccess=" + allSuccess +
                ", progressPercentage=" + getProgressPercentage() +
                ", acknowledgedVms=" + getAcknowledgedVms() +
                ", totalVms=" + getTotalVms() +
                ", roundNumber=" + roundNumber +
                '}';
    }
}