package com.feduwacomm.service.impl;

import com.feduwacomm.entity.VmAckTracking;
import com.feduwacomm.event.AckProgressChangedEvent;
import com.feduwacomm.event.AckStatusChangedEvent;
import com.feduwacomm.mapper.TaskParticipantsMapper;
import com.feduwacomm.mapper.VmAckTrackingMapper;
import com.feduwacomm.service.AckCacheService;
import com.feduwacomm.service.CacheService;
import com.feduwacomm.service.cache.model.AckCacheEntry;
import com.feduwacomm.service.cache.model.AckProgress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ACK缓存服务实现
 * 基于CacheService的ACK状态缓存管理
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-29
 */
@Slf4j
@Service
public class AckCacheServiceImpl implements AckCacheService {

    // 缓存TTL配置
    private static final Duration ACK_STATUS_TTL = Duration.ofHours(2);
    private static final Duration ACK_PROGRESS_TTL = Duration.ofMinutes(10);
    private static final Duration TASK_PARTICIPANTS_TTL = Duration.ofHours(24);
    private static final Duration ACK_TIMEOUT_TTL = Duration.ofHours(1);

    @Autowired
    private CacheService cacheService;

    @Autowired
    private VmAckTrackingMapper vmAckTrackingMapper;

    @Autowired
    private TaskParticipantsMapper taskParticipantsMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    // ======================== ACK状态缓存操作 ========================

    @Override
    public void updateAckStatus(String taskId, String vmId, VmAckTracking.AckType ackType,
                               VmAckTracking.AckStatus status, String errorMessage) {
        if (taskId == null || vmId == null || ackType == null || status == null) {
            log.warn("更新ACK状态参数无效: taskId={}, vmId={}, ackType={}, status={}",
                    taskId, vmId, ackType, status);
            return;
        }

        String cacheKey = buildAckStatusKey(taskId, vmId, ackType);

        // 获取旧状态用于事件发布
        Optional<AckCacheEntry> oldEntry = cacheService.get(cacheKey, AckCacheEntry.class);
        VmAckTracking.AckStatus oldStatus = oldEntry.map(AckCacheEntry::getStatus).orElse(null);

        AckCacheEntry entry = AckCacheEntry.builder()
                .taskId(taskId)
                .vmId(vmId)
                .ackType(ackType)
                .status(status)
                .errorMessage(errorMessage)
                .acknowledgedAt(status == VmAckTracking.AckStatus.SUCCESS ? LocalDateTime.now() : null)
                .createdAt(LocalDateTime.now())
                .lastUpdated(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plus(ACK_STATUS_TTL))
                .retryCount(0)
                .maxRetries(3)
                .build();

        cacheService.put(cacheKey, entry, ACK_STATUS_TTL);

        // 失效进度缓存，强制重新计算
        invalidateAckProgress(taskId, ackType);

        // 发布ACK状态变更事件
        try {
            AckStatusChangedEvent statusEvent = oldStatus == null ?
                AckStatusChangedEvent.createNew(this, taskId, vmId, ackType, status, errorMessage, null) :
                AckStatusChangedEvent.createUpdate(this, taskId, vmId, ackType, oldStatus, status, errorMessage, null);

            eventPublisher.publishEvent(statusEvent);

            log.debug("发布ACK状态变更事件: taskId={}, vmId={}, ackType={}, oldStatus={}, newStatus={}",
                    taskId, vmId, ackType, oldStatus, status);

        } catch (Exception e) {
            log.warn("发布ACK状态变更事件失败: taskId={}, vmId={}, ackType={}, error={}",
                    taskId, vmId, ackType, e.getMessage());
        }

        log.debug("更新ACK状态缓存: taskId={}, vmId={}, ackType={}, status={}",
                taskId, vmId, ackType, status);
    }

    @Override
    public void updateAckStatus(String taskId, String vmId, VmAckTracking.AckType ackType,
                               VmAckTracking.AckStatus status) {
        updateAckStatus(taskId, vmId, ackType, status, null);
    }

    @Override
    public Optional<AckCacheEntry> getAckStatus(String taskId, String vmId, VmAckTracking.AckType ackType) {
        if (taskId == null || vmId == null || ackType == null) {
            return Optional.empty();
        }

        String cacheKey = buildAckStatusKey(taskId, vmId, ackType);
        return cacheService.get(cacheKey, AckCacheEntry.class);
    }

    @Override
    public Map<String, AckCacheEntry> getAllVmAckStatus(String taskId, VmAckTracking.AckType ackType) {
        if (taskId == null || ackType == null) {
            return new HashMap<>();
        }

        String keyPrefix = String.format("ack:status:%s:%s:", taskId, ackType.name());
        Set<String> matchingKeys = cacheService.getKeysByPrefix(keyPrefix);

        return cacheService.batchGet(matchingKeys, AckCacheEntry.class);
    }

    // ======================== ACK进度管理 ========================

    @Override
    public AckProgress getAckProgress(String taskId, VmAckTracking.AckType ackType) {
        if (taskId == null || ackType == null) {
            return null;
        }

        String progressKey = buildAckProgressKey(taskId, ackType);

        return cacheService.getOrSet(progressKey, AckProgress.class, () -> {
            return computeAckProgress(taskId, ackType);
        }, ACK_PROGRESS_TTL);
    }

    @Override
    public void updateAckProgress(String taskId, VmAckTracking.AckType ackType, Integer explicitRoundNumber) {
        if (taskId == null || ackType == null) {
            return;
        }

        // 失效缓存，下次查询时重新计算
        invalidateAckProgress(taskId, ackType);

        // 立即计算新的进度
        AckProgress newProgress = computeAckProgress(taskId, ackType);
        if (newProgress != null) {
            String progressKey = buildAckProgressKey(taskId, ackType);
            cacheService.put(progressKey, newProgress, ACK_PROGRESS_TTL);

            // 发布ACK进度变更事件
            try {
                Integer latestRound = explicitRoundNumber != null
                        ? explicitRoundNumber
                        : vmAckTrackingMapper.findLatestRoundNumber(taskId, ackType);
                AckProgressChangedEvent progressEvent = AckProgressChangedEvent.create(
                    this, taskId, ackType, newProgress, latestRound);

                eventPublisher.publishEvent(progressEvent);

                log.debug("发布ACK进度变更事件: taskId={}, ackType={}, progress={}",
                        taskId, ackType, newProgress.getProgressDescription());

            } catch (Exception e) {
                log.warn("发布ACK进度变更事件失败: taskId={}, ackType={}, error={}",
                        taskId, ackType, e.getMessage());
            }
        }
    }

    @Override
    public boolean isAllVmsAcked(String taskId, VmAckTracking.AckType ackType) {
        AckProgress progress = getAckProgress(taskId, ackType);
        return progress != null && progress.isAllSuccess();
    }

    @Override
    public Set<String> getPendingVms(String taskId, VmAckTracking.AckType ackType) {
        AckProgress progress = getAckProgress(taskId, ackType);
        return progress != null ? progress.getPendingVms() : new HashSet<>();
    }

    // ======================== 任务参与者管理 ========================

    @Override
    public void setTaskParticipants(String taskId, Set<String> vmIds) {
        if (taskId == null || vmIds == null) {
            return;
        }

        String cacheKey = buildTaskParticipantsKey(taskId);
        cacheService.put(cacheKey, new HashSet<>(vmIds), TASK_PARTICIPANTS_TTL);

        log.debug("设置任务参与者缓存: taskId={}, vmCount={}", taskId, vmIds.size());
    }

    @Override
    public Set<String> getTaskParticipants(String taskId) {
        if (taskId == null) {
            return new HashSet<>();
        }

        String cacheKey = buildTaskParticipantsKey(taskId);

        return cacheService.getOrSet(cacheKey, Set.class, () -> {
            // 从数据库查询任务参与者
            return loadTaskParticipantsFromDatabase(taskId);
        }, TASK_PARTICIPANTS_TTL);
    }

    @Override
    public void addTaskParticipant(String taskId, String vmId) {
        if (taskId == null || vmId == null) {
            return;
        }

        Set<String> participants = getTaskParticipants(taskId);
        participants.add(vmId);
        setTaskParticipants(taskId, participants);

        log.debug("添加任务参与者: taskId={}, vmId={}", taskId, vmId);
    }

    @Override
    public void removeTaskParticipant(String taskId, String vmId) {
        if (taskId == null || vmId == null) {
            return;
        }

        Set<String> participants = getTaskParticipants(taskId);
        participants.remove(vmId);
        setTaskParticipants(taskId, participants);

        log.debug("移除任务参与者: taskId={}, vmId={}", taskId, vmId);
    }

    // ======================== 超时管理 ========================

    @Override
    public void setAckTimeout(String taskId, String vmId, VmAckTracking.AckType ackType, LocalDateTime timeoutAt) {
        if (taskId == null || vmId == null || ackType == null || timeoutAt == null) {
            return;
        }

        String cacheKey = buildAckTimeoutKey(taskId, vmId, ackType);
        cacheService.put(cacheKey, timeoutAt, ACK_TIMEOUT_TTL);

        log.debug("设置ACK超时: taskId={}, vmId={}, ackType={}, timeoutAt={}",
                taskId, vmId, ackType, timeoutAt);
    }

    @Override
    public List<String> getTimeoutAcks(LocalDateTime beforeTime) {
        if (beforeTime == null) {
            return new ArrayList<>();
        }

        Set<String> timeoutKeys = cacheService.getKeysByPrefix("ack:timeout:");
        List<String> timeoutAcks = new ArrayList<>();

        for (String key : timeoutKeys) {
            Optional<LocalDateTime> timeoutAt = cacheService.get(key, LocalDateTime.class);
            if (timeoutAt.isPresent() && timeoutAt.get().isBefore(beforeTime)) {
                timeoutAcks.add(key);
            }
        }

        return timeoutAcks;
    }

    @Override
    public int cleanupTimeoutAcks(LocalDateTime currentTime) {
        if (currentTime == null) {
            return 0;
        }

        List<String> timeoutAcks = getTimeoutAcks(currentTime);
        int cleanedCount = 0;

        for (String timeoutKey : timeoutAcks) {
            // 解析键获取任务信息
            String[] keyParts = timeoutKey.split(":");
            if (keyParts.length >= 5) {
                String taskId = keyParts[2];
                String ackTypeStr = keyParts[3];
                String vmId = keyParts[4];

                try {
                    VmAckTracking.AckType ackType = VmAckTracking.AckType.valueOf(ackTypeStr);

                    // 更新ACK状态为超时
                    updateAckStatus(taskId, vmId, ackType, VmAckTracking.AckStatus.TIMEOUT, "超时");

                    // 删除超时记录
                    cacheService.evict(timeoutKey);
                    cleanedCount++;

                } catch (IllegalArgumentException e) {
                    log.warn("无效的ACK类型: {}", ackTypeStr);
                }
            }
        }

        if (cleanedCount > 0) {
            log.info("清理超时ACK: 处理了 {} 个超时记录", cleanedCount);
        }

        return cleanedCount;
    }

    // ======================== 缓存管理 ========================

    @Override
    public void clearTaskAckCache(String taskId) {
        if (taskId == null) {
            return;
        }

        String taskPrefix = buildTaskPrefix(taskId);
        int removedCount = cacheService.evictByPrefix(taskPrefix);

        log.debug("清理任务ACK缓存: taskId={}, removedCount={}", taskId, removedCount);
    }

    @Override
    public void clearAckTypeCache(String taskId, VmAckTracking.AckType ackType) {
        if (taskId == null || ackType == null) {
            return;
        }

        String statusPrefix = String.format("ack:status:%s:%s:", taskId, ackType.name());
        String progressKey = buildAckProgressKey(taskId, ackType);
        String timeoutPrefix = String.format("ack:timeout:%s:%s:", taskId, ackType.name());

        int removedCount = 0;
        removedCount += cacheService.evictByPrefix(statusPrefix);
        removedCount += cacheService.evict(progressKey) ? 1 : 0;
        removedCount += cacheService.evictByPrefix(timeoutPrefix);

        log.debug("清理ACK类型缓存: taskId={}, ackType={}, removedCount={}",
                taskId, ackType, removedCount);
    }

    @Override
    public void invalidateAckProgress(String taskId, VmAckTracking.AckType ackType) {
        if (taskId == null || ackType == null) {
            return;
        }

        String progressKey = buildAckProgressKey(taskId, ackType);
        cacheService.evict(progressKey);

        log.debug("失效ACK进度缓存: taskId={}, ackType={}", taskId, ackType);
    }

    @Override
    public void warmupTaskAckCache(String taskId, Set<String> vmIds, Set<VmAckTracking.AckType> supportedAckTypes) {
        if (taskId == null || vmIds == null || supportedAckTypes == null) {
            return;
        }

        // 设置任务参与者
        setTaskParticipants(taskId, vmIds);

        // 从数据库加载现有的ACK状态
        for (VmAckTracking.AckType ackType : supportedAckTypes) {
            loadAckStatusFromDatabase(taskId, ackType);
        }

        log.info("预热任务ACK缓存完成: taskId={}, vmCount={}, ackTypeCount={}",
                taskId, vmIds.size(), supportedAckTypes.size());
    }

    // ======================== 私有辅助方法 ========================

    /**
     * 计算ACK进度
     */
    private AckProgress computeAckProgress(String taskId, VmAckTracking.AckType ackType) {
        Set<String> allVmIds = getTaskParticipants(taskId);
        if (allVmIds.isEmpty()) {
            return null;
        }

        Map<String, AckCacheEntry> ackStatusMap = getAllVmAckStatus(taskId, ackType);

        Set<String> pendingVms = new HashSet<>();
        Set<String> successVms = new HashSet<>();
        Set<String> failedVms = new HashSet<>();
        Set<String> timeoutVms = new HashSet<>();

        for (String vmId : allVmIds) {
            AckCacheEntry entry = ackStatusMap.get(buildAckStatusKey(taskId, vmId, ackType));

            if (entry == null || entry.isPending()) {
                pendingVms.add(vmId);
            } else if (entry.isAcknowledged()) {
                successVms.add(vmId);
            } else if (entry.isTimeout()) {
                timeoutVms.add(vmId);
            } else if (entry.isFailed()) {
                failedVms.add(vmId);
            }
        }

        int totalVms = allVmIds.size();
        int acknowledgedVms = successVms.size() + failedVms.size() + timeoutVms.size();
        double progressPercentage = totalVms > 0 ? (double) acknowledgedVms / totalVms * 100 : 0;

        return AckProgress.builder()
                .taskId(taskId)
                .ackType(ackType)
                .totalVms(totalVms)
                .acknowledgedVms(acknowledgedVms)
                .successVmCount(successVms.size())
                .failedVmCount(failedVms.size())
                .timeoutVmCount(timeoutVms.size())
                .pendingVms(pendingVms)
                .successVms(successVms)
                .failedVms(failedVms)
                .timeoutVms(timeoutVms)
                .allCompleted(pendingVms.isEmpty())
                .allSuccess(pendingVms.isEmpty() && failedVms.isEmpty() && timeoutVms.isEmpty())
                .progressPercentage(progressPercentage)
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    /**
     * 从数据库加载任务参与者
     */
    private Set<String> loadTaskParticipantsFromDatabase(String taskId) {
        try {
            return taskParticipantsMapper.selectVmIdsByTaskId(taskId);
        } catch (Exception e) {
            log.error("从数据库加载任务参与者失败: taskId={}, error={}", taskId, e.getMessage(), e);
            return new HashSet<>();
        }
    }

    /**
     * 从数据库加载ACK状态
     */
    private void loadAckStatusFromDatabase(String taskId, VmAckTracking.AckType ackType) {
        try {
            List<VmAckTracking> ackTrackings = vmAckTrackingMapper.findByTaskIdAndAckType(taskId, ackType.name());

            for (VmAckTracking tracking : ackTrackings) {
                AckCacheEntry entry = AckCacheEntry.fromVmAckTracking(tracking,
                        LocalDateTime.now().plus(ACK_STATUS_TTL));

                String cacheKey = buildAckStatusKey(taskId, tracking.getVmId(), ackType);
                cacheService.put(cacheKey, entry, ACK_STATUS_TTL);
            }

            log.debug("从数据库加载ACK状态: taskId={}, ackType={}, count={}",
                    taskId, ackType, ackTrackings.size());

        } catch (Exception e) {
            log.error("从数据库加载ACK状态失败: taskId={}, ackType={}, error={}",
                    taskId, ackType, e.getMessage(), e);
        }
    }

    // ======================== 缓存键构建工具方法 ========================

    /**
     * 构建ACK状态缓存键
     */
    private String buildAckStatusKey(String taskId, String vmId, VmAckTracking.AckType ackType) {
        return AckCacheService.buildAckStatusKey(taskId, vmId, ackType);
    }

    /**
     * 构建ACK进度缓存键
     */
    private String buildAckProgressKey(String taskId, VmAckTracking.AckType ackType) {
        return AckCacheService.buildAckProgressKey(taskId, ackType);
    }

    /**
     * 构建任务参与者缓存键
     */
    private String buildTaskParticipantsKey(String taskId) {
        return AckCacheService.buildTaskParticipantsKey(taskId);
    }

    /**
     * 构建ACK超时缓存键
     */
    private String buildAckTimeoutKey(String taskId, String vmId, VmAckTracking.AckType ackType) {
        return AckCacheService.buildAckTimeoutKey(taskId, vmId, ackType);
    }

    /**
     * 构建任务前缀（用于批量操作）
     */
    private String buildTaskPrefix(String taskId) {
        return AckCacheService.buildTaskPrefix(taskId);
    }
}
