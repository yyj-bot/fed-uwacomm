package com.feduwacomm.service.cache.impl;

import com.feduwacomm.service.cache.MetricsCacheService;
import com.feduwacomm.service.cache.exception.CacheValidationException;
import com.feduwacomm.service.cache.model.GlobalMetrics;
import com.feduwacomm.service.cache.model.ParticipantMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 度量指标缓存服务实现类
 * 使用内存缓存提供高性能的度量指标存储和检索
 *
 * @author FedUWAComm Team
 * @version 1.5.0
 */
@Service
public class MetricsCacheServiceImpl implements MetricsCacheService {

    private static final Logger log = LoggerFactory.getLogger(MetricsCacheServiceImpl.class);

    /**
     * 缓存有效期（分钟）
     */
    private static final int CACHE_VALIDITY_MINUTES = 10;

    /**
     * 参与者度量指标缓存
     * 结构: taskId -> (vmId -> ParticipantMetrics)
     */
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, ParticipantMetrics>> participantMetricsCache
            = new ConcurrentHashMap<>();

    /**
     * 全局度量指标缓存
     * 结构: taskId -> GlobalMetrics
     */
    private final ConcurrentHashMap<String, GlobalMetrics> globalMetricsCache = new ConcurrentHashMap<>();

    /**
     * 缓存命中统计
     */
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);

    // ======================== 参与者指标操作 ========================

    @Override
    public void updateParticipantMetrics(String taskId, String vmId, ParticipantMetrics metrics) {
        if (taskId == null || vmId == null || metrics == null) {
            throw CacheValidationException.nullData("ParticipantMetrics", taskId + ":" + vmId);
        }

        if (!metrics.isComplete()) {
            throw CacheValidationException.incompleteData("ParticipantMetrics", taskId + ":" + vmId);
        }

        // 确保指标中的taskId和vmId一致
        metrics.setTaskId(taskId);
        metrics.setVmId(vmId);
        metrics.setLastUpdated(LocalDateTime.now());

        // 获取或创建任务的参与者缓存
        ConcurrentHashMap<String, ParticipantMetrics> taskParticipants =
                participantMetricsCache.computeIfAbsent(taskId, k -> new ConcurrentHashMap<>());

        // 更新参与者指标
        taskParticipants.put(vmId, metrics);

        log.debug("更新参与者度量指标缓存: taskId={}, vmId={}, accuracy={}, loss={}",
                taskId, vmId, metrics.getAccuracy(), metrics.getLoss());
    }

    @Override
    public Optional<ParticipantMetrics> getParticipantMetrics(String taskId, String vmId) {
        if (taskId == null || vmId == null) {
            missCount.incrementAndGet();
            return Optional.empty();
        }

        ConcurrentHashMap<String, ParticipantMetrics> taskParticipants = participantMetricsCache.get(taskId);
        if (taskParticipants == null) {
            missCount.incrementAndGet();
            return Optional.empty();
        }

        ParticipantMetrics metrics = taskParticipants.get(vmId);
        if (metrics == null) {
            missCount.incrementAndGet();
            return Optional.empty();
        }

        // 检查数据有效性
        if (!metrics.isValid(CACHE_VALIDITY_MINUTES)) {
            log.warn("参与者度量指标缓存已过期: taskId={}, vmId={}, lastUpdated={}",
                    taskId, vmId, metrics.getLastUpdated());
            taskParticipants.remove(vmId);
            missCount.incrementAndGet();
            return Optional.empty();
        }

        hitCount.incrementAndGet();
        return Optional.of(metrics);
    }

    @Override
    public Map<String, ParticipantMetrics> getAllParticipantMetrics(String taskId) {
        if (taskId == null) {
            return Map.of();
        }

        ConcurrentHashMap<String, ParticipantMetrics> taskParticipants = participantMetricsCache.get(taskId);
        if (taskParticipants == null) {
            return Map.of();
        }

        // 过滤有效的指标
        Map<String, ParticipantMetrics> validMetrics = taskParticipants.entrySet().stream()
                .filter(entry -> entry.getValue().isValid(CACHE_VALIDITY_MINUTES))
                .collect(ConcurrentHashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                        ConcurrentHashMap::putAll);

        // 清理过期数据
        if (validMetrics.size() != taskParticipants.size()) {
            taskParticipants.clear();
            taskParticipants.putAll(validMetrics);
            log.info("清理任务{}的过期参与者度量指标缓存，保留{}个有效记录", taskId, validMetrics.size());
        }

        return validMetrics;
    }

    @Override
    public ParticipantMetrics validateAndGetParticipantMetrics(String taskId, String vmId) {
        Optional<ParticipantMetrics> metrics = getParticipantMetrics(taskId, vmId);
        if (metrics.isEmpty()) {
            throw CacheValidationException.notFound("ParticipantMetrics", taskId + ":" + vmId);
        }

        ParticipantMetrics participantMetrics = metrics.get();
        if (!participantMetrics.isComplete()) {
            throw CacheValidationException.incompleteData("ParticipantMetrics", taskId + ":" + vmId);
        }

        return participantMetrics;
    }

    // ======================== 全局指标操作 ========================

    @Override
    public void updateGlobalMetrics(String taskId, GlobalMetrics metrics) {
        if (taskId == null || metrics == null) {
            throw CacheValidationException.nullData("GlobalMetrics", taskId);
        }

        if (!metrics.isComplete()) {
            throw CacheValidationException.incompleteData("GlobalMetrics", taskId);
        }

        // 确保指标中的taskId一致
        metrics.setTaskId(taskId);
        metrics.setLastUpdated(LocalDateTime.now());

        globalMetricsCache.put(taskId, metrics);

        log.debug("更新全局度量指标缓存: taskId={}, globalAccuracy={}, globalLoss={}, rounds={}",
                taskId, metrics.getGlobalAccuracy(), metrics.getGlobalLoss(), metrics.getCommunicationRounds());
    }

    @Override
    public Optional<GlobalMetrics> getGlobalMetrics(String taskId) {
        if (taskId == null) {
            missCount.incrementAndGet();
            return Optional.empty();
        }

        GlobalMetrics metrics = globalMetricsCache.get(taskId);
        if (metrics == null) {
            missCount.incrementAndGet();
            return Optional.empty();
        }

        // 检查数据有效性
        if (!metrics.isValid(CACHE_VALIDITY_MINUTES)) {
            log.warn("全局度量指标缓存已过期: taskId={}, lastUpdated={}", taskId, metrics.getLastUpdated());
            globalMetricsCache.remove(taskId);
            missCount.incrementAndGet();
            return Optional.empty();
        }

        hitCount.incrementAndGet();
        return Optional.of(metrics);
    }

    @Override
    public GlobalMetrics validateAndGetGlobalMetrics(String taskId) {
        Optional<GlobalMetrics> metrics = getGlobalMetrics(taskId);
        if (metrics.isEmpty()) {
            throw CacheValidationException.notFound("GlobalMetrics", taskId);
        }

        GlobalMetrics globalMetrics = metrics.get();
        if (!globalMetrics.isComplete()) {
            throw CacheValidationException.incompleteData("GlobalMetrics", taskId);
        }

        return globalMetrics;
    }

    @Override
    public GlobalMetrics computeAndUpdateGlobalMetrics(String taskId, Integer totalRounds) {
        if (taskId == null) {
            throw new IllegalArgumentException("任务ID不能为null");
        }

        // 获取所有参与者指标
        Map<String, ParticipantMetrics> participantMetrics = getAllParticipantMetrics(taskId);

        // 计算全局指标
        GlobalMetrics globalMetrics = GlobalMetrics.computeFromParticipants(
                taskId, participantMetrics.values(), totalRounds);

        // 更新缓存
        updateGlobalMetrics(taskId, globalMetrics);

        log.info("重新计算并更新全局度量指标: taskId={}, 参与者数量={}, globalAccuracy={}, globalLoss={}",
                taskId, participantMetrics.size(), globalMetrics.getGlobalAccuracy(), globalMetrics.getGlobalLoss());

        return globalMetrics;
    }

    // ======================== 缓存管理 ========================

    @Override
    public void clearTaskCache(String taskId) {
        if (taskId == null) {
            return;
        }

        participantMetricsCache.remove(taskId);
        globalMetricsCache.remove(taskId);

        log.info("清理任务{}的所有度量指标缓存", taskId);
    }

    @Override
    public void invalidateExpiredCache() {
        LocalDateTime now = LocalDateTime.now();
        int removedParticipants = 0;
        int removedGlobal = 0;

        // 清理过期的参与者指标
        for (Map.Entry<String, ConcurrentHashMap<String, ParticipantMetrics>> taskEntry : participantMetricsCache.entrySet()) {
            String taskId = taskEntry.getKey();
            ConcurrentHashMap<String, ParticipantMetrics> participants = taskEntry.getValue();

            participants.entrySet().removeIf(entry -> {
                if (!entry.getValue().isValid(CACHE_VALIDITY_MINUTES)) {
                    log.debug("移除过期参与者度量指标: taskId={}, vmId={}", taskId, entry.getKey());
                    return true;
                }
                return false;
            });

            if (participants.isEmpty()) {
                participantMetricsCache.remove(taskId);
                removedParticipants++;
            }
        }

        // 清理过期的全局指标
        globalMetricsCache.entrySet().removeIf(entry -> {
            if (!entry.getValue().isValid(CACHE_VALIDITY_MINUTES)) {
                log.debug("移除过期全局度量指标: taskId={}", entry.getKey());
                return true;
            }
            return false;
        });
        removedGlobal = globalMetricsCache.size();

        if (removedParticipants > 0 || removedGlobal > 0) {
            log.info("清理过期缓存完成: 参与者指标任务数={}, 全局指标数={}", removedParticipants, removedGlobal);
        }
    }

    @Override
    public void clearAllCache() {
        int participantTasks = participantMetricsCache.size();
        int globalTasks = globalMetricsCache.size();

        participantMetricsCache.clear();
        globalMetricsCache.clear();
        hitCount.set(0);
        missCount.set(0);

        log.info("清理所有度量指标缓存: 参与者指标任务数={}, 全局指标任务数={}", participantTasks, globalTasks);
    }

    @Override
    public CacheStatistics getCacheStatistics() {
        int participantCacheSize = participantMetricsCache.values().stream()
                .mapToInt(Map::size)
                .sum();
        int globalCacheSize = globalMetricsCache.size();

        return new CacheStatistics(participantCacheSize, globalCacheSize,
                hitCount.get(), missCount.get());
    }

    @Override
    public boolean checkCacheConsistency(String taskId) {
        if (taskId == null) {
            return false;
        }

        // 检查参与者缓存一致性
        Map<String, ParticipantMetrics> participants = getAllParticipantMetrics(taskId);
        for (ParticipantMetrics metrics : participants.values()) {
            if (!metrics.isComplete() || !metrics.isValid(CACHE_VALIDITY_MINUTES)) {
                log.warn("发现不一致的参与者度量指标: taskId={}, vmId={}", taskId, metrics.getVmId());
                return false;
            }
        }

        // 检查全局缓存一致性
        Optional<GlobalMetrics> globalMetrics = getGlobalMetrics(taskId);
        if (globalMetrics.isPresent()) {
            GlobalMetrics metrics = globalMetrics.get();
            if (!metrics.isComplete() || !metrics.isValid(CACHE_VALIDITY_MINUTES)) {
                log.warn("发现不一致的全局度量指标: taskId={}", taskId);
                return false;
            }
        }

        return true;
    }
}