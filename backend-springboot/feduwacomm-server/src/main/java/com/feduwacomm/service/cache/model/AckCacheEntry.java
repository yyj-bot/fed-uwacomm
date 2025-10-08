package com.feduwacomm.service.cache.model;

import com.feduwacomm.entity.VmAckTracking;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ACK缓存条目
 * 用于缓存VM确认状态的详细信息
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AckCacheEntry {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * VM ID
     */
    private String vmId;

    /**
     * 确认类型
     */
    private VmAckTracking.AckType ackType;

    /**
     * 确认状态
     */
    private VmAckTracking.AckStatus status;

    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 确认数据（JSON格式）
     */
    private String ackData;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 确认时间
     */
    private LocalDateTime acknowledgedAt;

    /**
     * 超时时间
     */
    private LocalDateTime timeoutAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 最后更新时间
     */
    private LocalDateTime lastUpdated;

    /**
     * 缓存过期时间
     */
    private LocalDateTime expiresAt;

    /**
     * 重试次数
     */
    private int retryCount;

    /**
     * 最大重试次数
     */
    private int maxRetries;

    /**
     * 检查是否已确认
     *
     * @return 是否已确认
     */
    public boolean isAcknowledged() {
        return status == VmAckTracking.AckStatus.SUCCESS;
    }

    /**
     * 检查是否失败
     *
     * @return 是否失败
     */
    public boolean isFailed() {
        return status == VmAckTracking.AckStatus.FAILED;
    }

    /**
     * 检查是否超时
     *
     * @return 是否超时
     */
    public boolean isTimeout() {
        return status == VmAckTracking.AckStatus.TIMEOUT ||
                (timeoutAt != null && LocalDateTime.now().isAfter(timeoutAt));
    }

    /**
     * 检查是否等待中
     *
     * @return 是否等待中
     */
    public boolean isPending() {
        return status == VmAckTracking.AckStatus.PENDING && !isTimeout();
    }

    /**
     * 检查是否已完成（成功、失败或超时）
     *
     * @return 是否已完成
     */
    public boolean isCompleted() {
        return status == VmAckTracking.AckStatus.SUCCESS ||
               status == VmAckTracking.AckStatus.FAILED ||
               status == VmAckTracking.AckStatus.TIMEOUT ||
               isTimeout();
    }

    /**
     * 检查是否可以重试
     *
     * @return 是否可以重试
     */
    public boolean canRetry() {
        return isFailed() && retryCount < maxRetries;
    }

    /**
     * 获取状态描述
     *
     * @return 状态描述
     */
    public String getStatusDescription() {
        if (isTimeout()) {
            return "超时";
        }

        switch (status) {
            case PENDING:
                return "等待中";
            case SUCCESS:
                return "成功";
            case FAILED:
                return "失败";
            case TIMEOUT:
                return "超时";
            default:
                return "未知";
        }
    }

    /**
     * 获取剩余超时时间
     *
     * @return 剩余超时时间（秒），如果已超时或无超时设置返回-1
     */
    public long getRemainingTimeoutSeconds() {
        if (timeoutAt == null) {
            return -1;
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(timeoutAt)) {
            return -1; // 已超时
        }

        return java.time.Duration.between(now, timeoutAt).getSeconds();
    }

    /**
     * 获取等待时间
     *
     * @return 等待时间（秒）
     */
    public long getWaitingTimeSeconds() {
        if (createdAt == null) {
            return 0;
        }

        LocalDateTime endTime = acknowledgedAt != null ? acknowledgedAt : LocalDateTime.now();
        return java.time.Duration.between(createdAt, endTime).getSeconds();
    }

    /**
     * 创建一个新的重试条目
     *
     * @return 新的重试条目
     */
    public AckCacheEntry createRetryEntry() {
        return AckCacheEntry.builder()
                .taskId(this.taskId)
                .vmId(this.vmId)
                .ackType(this.ackType)
                .status(VmAckTracking.AckStatus.PENDING)
                .messageId(this.messageId)
                .ackData(this.ackData)
                .errorMessage(null)
                .acknowledgedAt(null)
                .timeoutAt(this.timeoutAt)
                .createdAt(LocalDateTime.now())
                .lastUpdated(LocalDateTime.now())
                .expiresAt(this.expiresAt)
                .retryCount(this.retryCount + 1)
                .maxRetries(this.maxRetries)
                .build();
    }

    /**
     * 转换为VmAckTracking实体
     *
     * @return VmAckTracking实体
     */
    public VmAckTracking toVmAckTracking() {
        return VmAckTracking.builder()
                .taskId(this.taskId)
                .vmId(this.vmId)
                .ackType(this.ackType)
                .status(this.status)
                .messageId(this.messageId)
                .ackData(this.ackData)
                .errorMessage(this.errorMessage)
                .acknowledgedAt(this.acknowledgedAt)
                .timeoutAt(this.timeoutAt)
                .createdAt(this.createdAt)
                .updatedAt(this.lastUpdated)
                .build();
    }

    /**
     * 从VmAckTracking实体创建缓存条目
     *
     * @param vmAckTracking VmAckTracking实体
     * @param expiresAt 缓存过期时间
     * @return 缓存条目
     */
    public static AckCacheEntry fromVmAckTracking(VmAckTracking vmAckTracking, LocalDateTime expiresAt) {
        return AckCacheEntry.builder()
                .taskId(vmAckTracking.getTaskId())
                .vmId(vmAckTracking.getVmId())
                .ackType(vmAckTracking.getAckType())
                .status(vmAckTracking.getStatus())
                .messageId(vmAckTracking.getMessageId())
                .ackData(vmAckTracking.getAckData())
                .errorMessage(vmAckTracking.getErrorMessage())
                .acknowledgedAt(vmAckTracking.getAcknowledgedAt())
                .timeoutAt(vmAckTracking.getTimeoutAt())
                .createdAt(vmAckTracking.getCreatedAt())
                .lastUpdated(vmAckTracking.getUpdatedAt())
                .expiresAt(expiresAt)
                .retryCount(0)
                .maxRetries(3) // 默认最大重试3次
                .build();
    }
}