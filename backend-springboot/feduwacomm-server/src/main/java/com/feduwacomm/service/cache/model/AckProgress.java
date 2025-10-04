package com.feduwacomm.service.cache.model;

import com.feduwacomm.entity.VmAckTracking;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * ACK进度信息
 * 用于缓存任务的确认进度状态
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AckProgress {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 确认类型
     */
    private VmAckTracking.AckType ackType;

    /**
     * 总参与VM数量
     */
    private int totalVms;

    /**
     * 已确认VM数量
     */
    private int acknowledgedVms;

    /**
     * 成功确认VM数量
     */
    private int successVmCount;

    /**
     * 失败确认VM数量
     */
    private int failedVmCount;

    /**
     * 超时确认VM数量
     */
    private int timeoutVmCount;

    /**
     * 等待确认的VM ID集合
     */
    private Set<String> pendingVms;

    /**
     * 成功确认的VM ID集合
     */
    private Set<String> successVms;

    /**
     * 失败确认的VM ID集合
     */
    private Set<String> failedVms;

    /**
     * 超时确认的VM ID集合
     */
    private Set<String> timeoutVms;

    /**
     * 是否所有VM都已完成确认（成功、失败或超时）
     */
    private boolean allCompleted;

    /**
     * 是否所有VM都成功确认
     */
    private boolean allSuccess;

    /**
     * 进度百分比（0-100）
     */
    private double progressPercentage;

    /**
     * 最后更新时间
     */
    private LocalDateTime lastUpdated;

    /**
     * 预计完成时间（基于当前进度估算）
     */
    private LocalDateTime estimatedCompletion;

    /**
     * 获取确认进度描述
     *
     * @return 进度描述字符串
     */
    public String getProgressDescription() {
        return String.format("%d/%d (%.1f%%)", acknowledgedVms, totalVms, progressPercentage);
    }

    /**
     * 获取详细状态描述
     *
     * @return 详细状态描述
     */
    public String getDetailedStatusDescription() {
        return String.format("总计:%d, 成功:%d, 失败:%d, 超时:%d, 等待:%d",
                totalVms, successVmCount, failedVmCount, timeoutVmCount, pendingVms.size());
    }

    /**
     * 计算剩余等待时间（如果有预计完成时间）
     *
     * @return 剩余等待时间描述
     */
    public String getRemainingTimeDescription() {
        if (estimatedCompletion == null) {
            return "未知";
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(estimatedCompletion)) {
            return "已超时";
        }

        long seconds = java.time.Duration.between(now, estimatedCompletion).getSeconds();
        if (seconds < 60) {
            return seconds + "秒";
        } else if (seconds < 3600) {
            return (seconds / 60) + "分钟";
        } else {
            return (seconds / 3600) + "小时";
        }
    }

    /**
     * 检查是否有异常情况（失败或超时）
     *
     * @return 是否有异常
     */
    public boolean hasIssues() {
        return failedVmCount > 0 || timeoutVmCount > 0;
    }

    /**
     * 获取问题VM数量
     *
     * @return 问题VM数量
     */
    public int getIssueVmCount() {
        return failedVmCount + timeoutVmCount;
    }

    /**
     * 检查进度是否正常（无异常且在合理时间内）
     *
     * @return 是否正常
     */
    public boolean isHealthy() {
        return !hasIssues() && (estimatedCompletion == null ||
                LocalDateTime.now().isBefore(estimatedCompletion.plusMinutes(5)));
    }
}