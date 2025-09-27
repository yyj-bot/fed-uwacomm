package com.feduwacomm.service;

import com.feduwacomm.enums.FederatedTaskStatus;
import com.feduwacomm.service.cache.MetricsCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 缓存生命周期管理器
 * 负责缓存的生命周期管理、定期清理和事件响应
 *
 * @author FedUWAComm Team
 * @version 1.4.0
 */
@Component
public class CacheLifecycleManager {

    private static final Logger log = LoggerFactory.getLogger(CacheLifecycleManager.class);

    @Autowired
    private MetricsCacheService metricsCacheService;

    /**
     * 任务状态变更事件处理
     * 当任务状态发生变化时，相应地管理缓存
     *
     * @param event 任务状态变更事件
     */
    @EventListener
    public void handleTaskStateChange(TaskStateChangeEvent event) {
        String taskId = event.getTaskId();
        FederatedTaskStatus newStatus = event.getNewStatus();
        FederatedTaskStatus oldStatus = event.getOldStatus();

        log.info("处理任务状态变更事件: taskId={}, {} -> {}", taskId, oldStatus, newStatus);

        try {
            switch (newStatus) {
                case COMPLETED:
                case CANCELLED:
                case FAILED:
                    // 任务结束时保留缓存一段时间用于查询，然后清理
                    log.info("任务{}已结束，状态={}，保留缓存用于历史查询", taskId, newStatus);
                    // 不立即清理，留给定期清理任务处理
                    break;

                case PAUSED:
                    // 暂停时保留缓存
                    log.debug("任务{}已暂停，保留缓存", taskId);
                    break;

                case RUNNING:
                    // 运行中时确保缓存一致性
                    if (oldStatus == FederatedTaskStatus.PAUSED) {
                        log.info("任务{}从暂停状态恢复运行，检查缓存一致性", taskId);
                        checkAndRepairCacheConsistency(taskId);
                    } else {
                        log.info("任务{}开始运行，初始化缓存监控", taskId);
                    }
                    break;

                case CREATED:
                case CONFIGURED:
                    // 创建或配置阶段无需特殊处理
                    log.debug("任务{}处于{}状态，无需缓存操作", taskId, newStatus);
                    break;

                default:
                    log.warn("未知的任务状态: taskId={}, status={}", taskId, newStatus);
                    break;
            }

        } catch (Exception e) {
            log.error("处理任务状态变更事件失败: taskId={}, status={}, 错误={}",
                    taskId, newStatus, e.getMessage(), e);
        }
    }

    /**
     * 定期清理过期缓存
     * 每5分钟执行一次缓存清理
     */
    @Scheduled(fixedRate = 300000) // 300秒 = 5分钟
    public void cleanupExpiredCache() {
        try {
            log.debug("开始定期清理过期缓存");

            // 获取清理前的统计信息
            MetricsCacheService.CacheStatistics beforeStats = metricsCacheService.getCacheStatistics();

            // 执行清理
            metricsCacheService.invalidateExpiredCache();

            // 获取清理后的统计信息
            MetricsCacheService.CacheStatistics afterStats = metricsCacheService.getCacheStatistics();

            // 记录清理结果
            int participantCacheDiff = beforeStats.getParticipantCacheSize() - afterStats.getParticipantCacheSize();
            int globalCacheDiff = beforeStats.getGlobalCacheSize() - afterStats.getGlobalCacheSize();

            if (participantCacheDiff > 0 || globalCacheDiff > 0) {
                log.info("定期缓存清理完成: 清理参与者缓存{}个, 全局缓存{}个",
                        participantCacheDiff, globalCacheDiff);
            } else {
                log.debug("定期缓存清理完成: 无过期缓存");
            }

        } catch (Exception e) {
            log.error("定期清理缓存失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 缓存健康检查
     * 每小时执行一次缓存健康检查
     */
    @Scheduled(fixedRate = 3600000) // 3600秒 = 1小时
    public void cacheHealthCheck() {
        try {
            log.debug("开始缓存健康检查");

            MetricsCacheService.CacheStatistics stats = metricsCacheService.getCacheStatistics();

            log.info("缓存健康检查报告: {}", stats);

            // 检查缓存命中率
            if (stats.getHitRate() < 0.5 && (stats.getHitCount() + stats.getMissCount()) > 100) {
                log.warn("缓存命中率较低: {}%, 建议检查缓存策略", stats.getHitRate() * 100);
            }

            // 检查缓存大小
            if (stats.getParticipantCacheSize() > 10000) {
                log.warn("参与者缓存大小过大: {}, 可能存在内存泄漏", stats.getParticipantCacheSize());
            }

            if (stats.getGlobalCacheSize() > 1000) {
                log.warn("全局缓存大小过大: {}, 可能存在内存泄漏", stats.getGlobalCacheSize());
            }

        } catch (Exception e) {
            log.error("缓存健康检查失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 检查并修复缓存一致性
     *
     * @param taskId 任务ID
     */
    private void checkAndRepairCacheConsistency(String taskId) {
        try {
            boolean isConsistent = metricsCacheService.checkCacheConsistency(taskId);

            if (!isConsistent) {
                log.warn("发现缓存不一致，清理任务缓存: taskId={}", taskId);
                metricsCacheService.clearTaskCache(taskId);
            } else {
                log.debug("缓存一致性检查通过: taskId={}", taskId);
            }

        } catch (Exception e) {
            log.error("缓存一致性检查失败: taskId={}, 错误={}", taskId, e.getMessage(), e);
        }
    }

    /**
     * 应用关闭时的缓存清理
     */
    public void shutdown() {
        try {
            log.info("应用关闭，开始清理所有缓存");

            MetricsCacheService.CacheStatistics stats = metricsCacheService.getCacheStatistics();
            log.info("清理前缓存统计: {}", stats);

            metricsCacheService.clearAllCache();
            log.info("所有缓存清理完成");

        } catch (Exception e) {
            log.error("应用关闭时清理缓存失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 任务状态变更事件类
     */
    public static class TaskStateChangeEvent {
        private final String taskId;
        private final FederatedTaskStatus oldStatus;
        private final FederatedTaskStatus newStatus;
        private final String reason;

        public TaskStateChangeEvent(String taskId, FederatedTaskStatus oldStatus,
                                   FederatedTaskStatus newStatus, String reason) {
            this.taskId = taskId;
            this.oldStatus = oldStatus;
            this.newStatus = newStatus;
            this.reason = reason;
        }

        public String getTaskId() { return taskId; }
        public FederatedTaskStatus getOldStatus() { return oldStatus; }
        public FederatedTaskStatus getNewStatus() { return newStatus; }
        public String getReason() { return reason; }

        @Override
        public String toString() {
            return String.format("TaskStateChangeEvent{taskId='%s', %s -> %s, reason='%s'}",
                    taskId, oldStatus, newStatus, reason);
        }
    }
}