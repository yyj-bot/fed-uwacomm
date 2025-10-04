package com.feduwacomm.service;

import com.feduwacomm.entity.VmAckTracking;
import com.feduwacomm.service.cache.model.AckProgress;
import com.feduwacomm.service.cache.model.AckCacheEntry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * ACK缓存服务接口
 * 专门处理VM确认状态的缓存操作
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-29
 */
public interface AckCacheService {

    // ======================== ACK状态缓存操作 ========================

    /**
     * 更新VM的ACK状态
     *
     * @param taskId 任务ID
     * @param vmId VM ID
     * @param ackType 确认类型
     * @param status 确认状态
     * @param errorMessage 错误信息（可选）
     */
    void updateAckStatus(String taskId, String vmId, VmAckTracking.AckType ackType,
                        VmAckTracking.AckStatus status, String errorMessage);

    /**
     * 更新VM的ACK状态（无错误信息）
     *
     * @param taskId 任务ID
     * @param vmId VM ID
     * @param ackType 确认类型
     * @param status 确认状态
     */
    void updateAckStatus(String taskId, String vmId, VmAckTracking.AckType ackType,
                        VmAckTracking.AckStatus status);

    /**
     * 获取VM的ACK状态
     *
     * @param taskId 任务ID
     * @param vmId VM ID
     * @param ackType 确认类型
     * @return ACK缓存条目，如果不存在返回空
     */
    Optional<AckCacheEntry> getAckStatus(String taskId, String vmId, VmAckTracking.AckType ackType);

    /**
     * 批量获取任务的所有VM ACK状态
     *
     * @param taskId 任务ID
     * @param ackType 确认类型
     * @return VM ID -> ACK状态的映射
     */
    Map<String, AckCacheEntry> getAllVmAckStatus(String taskId, VmAckTracking.AckType ackType);

    // ======================== ACK进度管理 ========================

    /**
     * 获取ACK进度
     *
     * @param taskId 任务ID
     * @param ackType 确认类型
     * @return ACK进度信息
     */
    AckProgress getAckProgress(String taskId, VmAckTracking.AckType ackType);

    /**
     * 更新ACK进度（通常在收到新ACK时调用）
     *
     * @param taskId 任务ID
     * @param ackType 确认类型
     */
    void updateAckProgress(String taskId, VmAckTracking.AckType ackType);

    /**
     * 检查是否所有VM都已确认
     *
     * @param taskId 任务ID
     * @param ackType 确认类型
     * @return 是否所有VM都已确认
     */
    boolean isAllVmsAcked(String taskId, VmAckTracking.AckType ackType);

    /**
     * 获取未确认的VM列表
     *
     * @param taskId 任务ID
     * @param ackType 确认类型
     * @return 未确认的VM ID集合
     */
    Set<String> getPendingVms(String taskId, VmAckTracking.AckType ackType);

    // ======================== 任务参与者管理 ========================

    /**
     * 设置任务参与者列表
     *
     * @param taskId 任务ID
     * @param vmIds 参与者VM ID集合
     */
    void setTaskParticipants(String taskId, Set<String> vmIds);

    /**
     * 获取任务参与者列表
     *
     * @param taskId 任务ID
     * @return 参与者VM ID集合
     */
    Set<String> getTaskParticipants(String taskId);

    /**
     * 添加任务参与者
     *
     * @param taskId 任务ID
     * @param vmId 新参与者VM ID
     */
    void addTaskParticipant(String taskId, String vmId);

    /**
     * 移除任务参与者
     *
     * @param taskId 任务ID
     * @param vmId 要移除的VM ID
     */
    void removeTaskParticipant(String taskId, String vmId);

    // ======================== 超时管理 ========================

    /**
     * 设置ACK超时时间
     *
     * @param taskId 任务ID
     * @param vmId VM ID
     * @param ackType 确认类型
     * @param timeoutAt 超时时间
     */
    void setAckTimeout(String taskId, String vmId, VmAckTracking.AckType ackType, LocalDateTime timeoutAt);

    /**
     * 获取即将超时的ACK列表
     *
     * @param beforeTime 指定时间之前
     * @return 即将超时的ACK信息列表
     */
    List<String> getTimeoutAcks(LocalDateTime beforeTime);

    /**
     * 清理超时的ACK状态
     *
     * @param currentTime 当前时间
     * @return 清理的数量
     */
    int cleanupTimeoutAcks(LocalDateTime currentTime);

    // ======================== 缓存管理 ========================

    /**
     * 清理指定任务的所有ACK缓存
     *
     * @param taskId 任务ID
     */
    void clearTaskAckCache(String taskId);

    /**
     * 清理指定任务和确认类型的ACK缓存
     *
     * @param taskId 任务ID
     * @param ackType 确认类型
     */
    void clearAckTypeCache(String taskId, VmAckTracking.AckType ackType);

    /**
     * 失效ACK进度缓存（用于强制重新计算）
     *
     * @param taskId 任务ID
     * @param ackType 确认类型
     */
    void invalidateAckProgress(String taskId, VmAckTracking.AckType ackType);

    /**
     * 预热任务的ACK缓存
     *
     * @param taskId 任务ID
     * @param vmIds 参与者VM ID集合
     * @param supportedAckTypes 支持的确认类型集合
     */
    void warmupTaskAckCache(String taskId, Set<String> vmIds, Set<VmAckTracking.AckType> supportedAckTypes);

    // ======================== 缓存键工具方法 ========================

    /**
     * 构建ACK状态缓存键
     *
     * @param taskId 任务ID
     * @param vmId VM ID
     * @param ackType 确认类型
     * @return 缓存键
     */
    static String buildAckStatusKey(String taskId, String vmId, VmAckTracking.AckType ackType) {
        return String.format("ack:status:%s:%s:%s", taskId, ackType.name(), vmId);
    }

    /**
     * 构建ACK进度缓存键
     *
     * @param taskId 任务ID
     * @param ackType 确认类型
     * @return 缓存键
     */
    static String buildAckProgressKey(String taskId, VmAckTracking.AckType ackType) {
        return String.format("ack:progress:%s:%s", taskId, ackType.name());
    }

    /**
     * 构建任务参与者缓存键
     *
     * @param taskId 任务ID
     * @return 缓存键
     */
    static String buildTaskParticipantsKey(String taskId) {
        return String.format("task:participants:%s", taskId);
    }

    /**
     * 构建ACK超时缓存键
     *
     * @param taskId 任务ID
     * @param vmId VM ID
     * @param ackType 确认类型
     * @return 缓存键
     */
    static String buildAckTimeoutKey(String taskId, String vmId, VmAckTracking.AckType ackType) {
        return String.format("ack:timeout:%s:%s:%s", taskId, ackType.name(), vmId);
    }

    /**
     * 构建任务前缀（用于批量操作）
     *
     * @param taskId 任务ID
     * @return 缓存键前缀
     */
    static String buildTaskPrefix(String taskId) {
        return String.format("ack:%s:", taskId);
    }
}