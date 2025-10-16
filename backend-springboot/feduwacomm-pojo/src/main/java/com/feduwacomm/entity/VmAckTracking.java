package com.feduwacomm.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * VM确认跟踪实体类
 * 对应数据库表: vm_ack_tracking
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmAckTracking {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 轮次号，NULL表示任务级别确认
     */
    private Integer roundNumber;

    /**
     * 虚拟机ID
     */
    private String vmId;

    /**
     * 确认类型
     */
    private AckType ackType;

    /**
     * 确认状态
     */
    private AckStatus status;

    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 确认数据（JSON格式）
     */
    private String ackData;

    /**
     * 确认时间（别名）
     */
    public LocalDateTime getAckTime() {
        return this.acknowledgedAt;
    }

    public void setAckTime(LocalDateTime ackTime) {
        this.acknowledgedAt = ackTime;
    }

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
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 确认类型枚举
     */
    public enum AckType {
        TASK_START("任务启动"),
        TASK_STOP("任务停止"),
        TASK_RESUME("任务恢复"),
        TASK_DELETE("任务删除"),
        TASK_INIT("任务初始化"),
        TASK_REINIT("任务重新初始化"),
        ROUND_START("轮次开始"),
        GRADIENT_UPLOAD("梯度上传"),
        GRADIENT_READY("梯度就绪"),
        GLOBAL_MODEL_BROADCAST("全局模型广播"),
        ROUND_COMPLETE("轮次完成"),
        // v1.5 新增数据集相关确认类型
        DATASET_LIST_QUERY("数据集列表查询"),
        DATASET_CREATE("数据集创建"),
        DATASET_STATUS_QUERY("数据集状态查询"),
        DATASET_COMPLETE("数据集分发完成");

        private final String description;

        AckType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 确认状态枚举
     */
    public enum AckStatus {
        PENDING("等待中"),
        SUCCESS("成功"),
        FAILED("失败"),
        TIMEOUT("超时");

        private final String description;

        AckStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 判断是否为任务级别的确认
     */
    public boolean isTaskLevelAck() {
        return this.roundNumber == null;
    }

    /**
     * 判断是否为轮次级别的确认
     */
    public boolean isRoundLevelAck() {
        return this.roundNumber != null;
    }

    /**
     * 判断是否已超时
     */
    public boolean isTimedOut() {
        return this.status == AckStatus.TIMEOUT ||
                (this.timeoutAt != null && LocalDateTime.now().isAfter(this.timeoutAt));
    }

    /**
     * 判断是否已完成（成功、失败或超时）
     */
    public boolean isCompleted() {
        return this.status == AckStatus.SUCCESS ||
                this.status == AckStatus.FAILED ||
                this.status == AckStatus.TIMEOUT;
    }
}
