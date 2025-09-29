package com.feduwacomm.service.cache.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 参与者度量指标缓存数据结构
 * 用于缓存联邦学习参与者的实时训练指标
 *
 * @author FedUWAComm Team
 * @version 1.4.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantMetrics {

    /**
     * 虚拟机ID
     */
    private String vmId;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 当前轮次
     */
    private Integer currentRound;

    /**
     * 训练精度
     */
    private Double accuracy;

    /**
     * 训练损失值
     */
    private Double loss;

    /**
     * 最后更新时间
     */
    private LocalDateTime lastUpdated;

    /**
     * 参与者状态
     */
    private String status;

    /**
     * 数据大小
     */
    private Integer dataSize;

    /**
     * 检查缓存数据是否有效
     *
     * @param maxAgeMinutes 最大有效期（分钟）
     * @return 是否有效
     */
    public boolean isValid(int maxAgeMinutes) {
        if (lastUpdated == null) {
            return false;
        }

        LocalDateTime expireTime = lastUpdated.plusMinutes(maxAgeMinutes);
        return LocalDateTime.now().isBefore(expireTime);
    }

    /**
     * 检查数据完整性
     *
     * @return 数据是否完整
     */
    public boolean isComplete() {
        return vmId != null &&
               taskId != null &&
               currentRound != null &&
               accuracy != null &&
               loss != null &&
               status != null;
    }

    /**
     * 创建缓存键
     *
     * @return 缓存键字符串
     */
    public String getCacheKey() {
        return taskId + ":" + vmId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParticipantMetrics that = (ParticipantMetrics) o;
        return Objects.equals(vmId, that.vmId) &&
               Objects.equals(taskId, that.taskId) &&
               Objects.equals(currentRound, that.currentRound);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vmId, taskId, currentRound);
    }

    @Override
    public String toString() {
        return "ParticipantMetrics{" +
                "vmId='" + vmId + '\'' +
                ", taskId='" + taskId + '\'' +
                ", currentRound=" + currentRound +
                ", accuracy=" + accuracy +
                ", loss=" + loss +
                ", status='" + status + '\'' +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}