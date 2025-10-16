package com.feduwacomm.service.cache;

import com.feduwacomm.service.cache.exception.CacheValidationException;
import com.feduwacomm.service.cache.model.GlobalMetrics;
import com.feduwacomm.service.cache.model.ParticipantMetrics;

import java.util.Map;
import java.util.Optional;

/**
 * 度量指标缓存服务接口
 * 提供参与者和全局度量指标的缓存操作
 *
 * @author FedUWAComm Team
 * @version 1.5.0
 */
public interface MetricsCacheService {

    // ======================== 参与者指标操作 ========================

    /**
     * 更新参与者度量指标
     *
     * @param taskId 任务ID
     * @param vmId 虚拟机ID
     * @param metrics 度量指标
     * @throws CacheValidationException 当数据无效时抛出
     */
    void updateParticipantMetrics(String taskId, String vmId, ParticipantMetrics metrics);

    /**
     * 获取参与者度量指标
     *
     * @param taskId 任务ID
     * @param vmId 虚拟机ID
     * @return 度量指标，如果不存在则返回empty
     */
    Optional<ParticipantMetrics> getParticipantMetrics(String taskId, String vmId);

    /**
     * 获取任务的所有参与者度量指标
     *
     * @param taskId 任务ID
     * @return 参与者度量指标映射，键为vmId
     */
    Map<String, ParticipantMetrics> getAllParticipantMetrics(String taskId);

    /**
     * 验证参与者度量指标数据
     *
     * @param taskId 任务ID
     * @param vmId 虚拟机ID
     * @return 验证通过的度量指标
     * @throws CacheValidationException 当数据无效时抛出
     */
    ParticipantMetrics validateAndGetParticipantMetrics(String taskId, String vmId);

    // ======================== 全局指标操作 ========================

    /**
     * 更新全局度量指标
     *
     * @param taskId 任务ID
     * @param metrics 全局指标
     * @throws CacheValidationException 当数据无效时抛出
     */
    void updateGlobalMetrics(String taskId, GlobalMetrics metrics);

    /**
     * 获取全局度量指标
     *
     * @param taskId 任务ID
     * @return 全局指标，如果不存在则返回empty
     */
    Optional<GlobalMetrics> getGlobalMetrics(String taskId);

    /**
     * 验证全局度量指标数据
     *
     * @param taskId 任务ID
     * @return 验证通过的全局指标
     * @throws CacheValidationException 当数据无效时抛出
     */
    GlobalMetrics validateAndGetGlobalMetrics(String taskId);

    /**
     * 从参与者指标计算并更新全局指标
     *
     * @param taskId 任务ID
     * @param totalRounds 总轮次数
     * @return 计算得到的全局指标
     */
    GlobalMetrics computeAndUpdateGlobalMetrics(String taskId, Integer totalRounds);

    // ======================== 缓存管理 ========================

    /**
     * 清理指定任务的所有缓存
     *
     * @param taskId 任务ID
     */
    void clearTaskCache(String taskId);

    /**
     * 清理过期的缓存数据
     */
    void invalidateExpiredCache();

    /**
     * 清理所有缓存
     */
    void clearAllCache();

    /**
     * 获取缓存统计信息
     *
     * @return 缓存统计
     */
    CacheStatistics getCacheStatistics();

    // ======================== 缓存一致性检查 ========================

    /**
     * 检查缓存一致性
     *
     * @param taskId 任务ID
     * @return 一致性检查结果
     */
    boolean checkCacheConsistency(String taskId);

    /**
     * 缓存统计信息内部类
     */
    class CacheStatistics {
        private final int participantCacheSize;
        private final int globalCacheSize;
        private final long hitCount;
        private final long missCount;
        private final double hitRate;

        public CacheStatistics(int participantCacheSize, int globalCacheSize, long hitCount, long missCount) {
            this.participantCacheSize = participantCacheSize;
            this.globalCacheSize = globalCacheSize;
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.hitRate = (hitCount + missCount) > 0 ? (double) hitCount / (hitCount + missCount) : 0.0;
        }

        public int getParticipantCacheSize() { return participantCacheSize; }
        public int getGlobalCacheSize() { return globalCacheSize; }
        public long getHitCount() { return hitCount; }
        public long getMissCount() { return missCount; }
        public double getHitRate() { return hitRate; }

        @Override
        public String toString() {
            return String.format("CacheStats{participantCache=%d, globalCache=%d, hits=%d, misses=%d, hitRate=%.2f%%}",
                    participantCacheSize, globalCacheSize, hitCount, missCount, hitRate * 100);
        }
    }
}