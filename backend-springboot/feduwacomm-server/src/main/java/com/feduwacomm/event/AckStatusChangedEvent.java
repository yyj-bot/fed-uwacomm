package com.feduwacomm.event;

import com.feduwacomm.entity.VmAckTracking;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * ACK状态变更事件
 * 当VM的确认状态发生变化时发布此事件
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-29
 */
@Getter
public class AckStatusChangedEvent extends ApplicationEvent {

    private final String taskId;
    private final String vmId;
    private final VmAckTracking.AckType ackType;
    private final VmAckTracking.AckStatus oldStatus;
    private final VmAckTracking.AckStatus newStatus;
    private final String errorMessage;
    private final Integer roundNumber;

    public AckStatusChangedEvent(Object source, String taskId, String vmId,
                                VmAckTracking.AckType ackType, VmAckTracking.AckStatus oldStatus,
                                VmAckTracking.AckStatus newStatus, String errorMessage, Integer roundNumber) {
        super(source);
        this.taskId = taskId;
        this.vmId = vmId;
        this.ackType = ackType;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.errorMessage = errorMessage;
        this.roundNumber = roundNumber;
    }

    /**
     * 创建新ACK记录事件（旧状态为null）
     */
    public static AckStatusChangedEvent createNew(Object source, String taskId, String vmId,
                                                  VmAckTracking.AckType ackType, VmAckTracking.AckStatus newStatus,
                                                  String errorMessage, Integer roundNumber) {
        return new AckStatusChangedEvent(source, taskId, vmId, ackType, null, newStatus, errorMessage, roundNumber);
    }

    /**
     * 创建状态更新事件
     */
    public static AckStatusChangedEvent createUpdate(Object source, String taskId, String vmId,
                                                     VmAckTracking.AckType ackType, VmAckTracking.AckStatus oldStatus,
                                                     VmAckTracking.AckStatus newStatus, String errorMessage, Integer roundNumber) {
        return new AckStatusChangedEvent(source, taskId, vmId, ackType, oldStatus, newStatus, errorMessage, roundNumber);
    }

    /**
     * 检查是否为新增ACK记录
     */
    public boolean isNewRecord() {
        return oldStatus == null;
    }

    /**
     * 检查是否为状态更新
     */
    public boolean isStatusUpdate() {
        return oldStatus != null && oldStatus != newStatus;
    }

    /**
     * 检查是否为成功状态
     */
    public boolean isSuccess() {
        return newStatus == VmAckTracking.AckStatus.SUCCESS;
    }

    /**
     * 检查是否为失败状态
     */
    public boolean isFailed() {
        return newStatus == VmAckTracking.AckStatus.FAILED;
    }

    /**
     * 检查是否为超时状态
     */
    public boolean isTimeout() {
        return newStatus == VmAckTracking.AckStatus.TIMEOUT;
    }

    /**
     * 获取事件摘要信息
     */
    public String getSummary() {
        String changeType = isNewRecord() ? "新增" : "更新";
        String roundInfo = roundNumber != null ? " [轮次:" + roundNumber + "]" : "";
        String errorInfo = errorMessage != null ? " 错误:" + errorMessage : "";

        return String.format("%s ACK - 任务:%s, VM:%s, 类型:%s, 状态:%s%s%s",
                changeType, taskId, vmId, ackType, newStatus, roundInfo, errorInfo);
    }

    @Override
    public String toString() {
        return "AckStatusChangedEvent{" +
                "taskId='" + taskId + '\'' +
                ", vmId='" + vmId + '\'' +
                ", ackType=" + ackType +
                ", oldStatus=" + oldStatus +
                ", newStatus=" + newStatus +
                ", errorMessage='" + errorMessage + '\'' +
                ", roundNumber=" + roundNumber +
                '}';
    }
}