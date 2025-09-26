package com.feduwacomm.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Objects;

/**
 * 任务全局指标缓存数据结构
 * 用于缓存联邦学习任务的全局度量指标
 *
 * @author FedUWAComm Team
 * @version 1.4.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalMetrics {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 全局训练精度
     */
    private Double globalAccuracy;

    /**
     * 全局训练损失值
     */
    private Double globalLoss;

    /**
     * 通信轮次
     */
    private Integer communicationRounds;

    /**
     * 已处理的数据量
     */
    private Integer dataProcessed;

    /**
     * 预估剩余时间（秒）
     */
    private Integer estimatedTimeRemaining;

    /**
     * 最后更新时间
     */
    private LocalDateTime lastUpdated;

    /**
     * 参与的VM数量
     */
    private Integer participantCount;

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
        return taskId != null &&
               globalAccuracy != null &&
               globalLoss != null &&
               communicationRounds != null &&
               dataProcessed != null;
    }

    /**
     * 从参与者指标集合计算全局指标
     *
     * @param participantMetrics 参与者指标集合
     * @param totalRounds 总轮次数
     * @return 全局指标
     */
    public static GlobalMetrics computeFromParticipants(String taskId,
                                                       Collection<ParticipantMetrics> participantMetrics,
                                                       Integer totalRounds) {
        if (participantMetrics == null || participantMetrics.isEmpty()) {
            return createEmpty(taskId);
        }

        // 过滤有效的参与者指标
        Collection<ParticipantMetrics> validMetrics = participantMetrics.stream()
                .filter(Objects::nonNull)
                .filter(ParticipantMetrics::isComplete)
                .toList();

        if (validMetrics.isEmpty()) {
            return createEmpty(taskId);
        }

        // 计算全局精度（平均值）
        double globalAccuracy = validMetrics.stream()
                .mapToDouble(ParticipantMetrics::getAccuracy)
                .average()
                .orElse(0.0);

        // 计算全局损失值（平均值）
        double globalLoss = validMetrics.stream()
                .mapToDouble(ParticipantMetrics::getLoss)
                .average()
                .orElse(0.0);

        // 获取当前轮次（假设所有参与者同步）
        int currentRound = validMetrics.stream()
                .mapToInt(ParticipantMetrics::getCurrentRound)
                .max()
                .orElse(0);

        // 计算已处理数据量
        int dataProcessed = validMetrics.stream()
                .filter(p -> p.getDataSize() != null)
                .mapToInt(ParticipantMetrics::getDataSize)
                .sum();

        // 估算剩余时间
        int estimatedTimeRemaining = 0;
        if (totalRounds != null && totalRounds > currentRound) {
            int remainingRounds = totalRounds - currentRound;
            estimatedTimeRemaining = remainingRounds * 180; // 假设每轮3分钟
        }

        return GlobalMetrics.builder()
                .taskId(taskId)
                .globalAccuracy(globalAccuracy)
                .globalLoss(globalLoss)
                .communicationRounds(currentRound)
                .dataProcessed(dataProcessed)
                .estimatedTimeRemaining(estimatedTimeRemaining)
                .participantCount(validMetrics.size())
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    /**
     * 创建空的全局指标
     *
     * @param taskId 任务ID
     * @return 空的全局指标
     */
    public static GlobalMetrics createEmpty(String taskId) {
        return GlobalMetrics.builder()
                .taskId(taskId)
                .globalAccuracy(0.0)
                .globalLoss(0.0)
                .communicationRounds(0)
                .dataProcessed(0)
                .estimatedTimeRemaining(0)
                .participantCount(0)
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GlobalMetrics that = (GlobalMetrics) o;
        return Objects.equals(taskId, that.taskId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId);
    }

    @Override
    public String toString() {
        return "GlobalMetrics{" +
                "taskId='" + taskId + '\'' +
                ", globalAccuracy=" + globalAccuracy +
                ", globalLoss=" + globalLoss +
                ", communicationRounds=" + communicationRounds +
                ", dataProcessed=" + dataProcessed +
                ", estimatedTimeRemaining=" + estimatedTimeRemaining +
                ", participantCount=" + participantCount +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}