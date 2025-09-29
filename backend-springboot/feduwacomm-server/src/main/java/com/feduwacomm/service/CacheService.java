package com.feduwacomm.service;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 内存缓存服务接口
 * 提供统一的缓存操作
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-28
 */
public interface CacheService {

    /**
     * 获取缓存值
     *
     * @param key 缓存键
     * @param valueType 值类型
     * @return 缓存值，如果不存在返回空
     */
    <T> Optional<T> get(String key, Class<T> valueType);

    /**
     * 设置缓存值
     *
     * @param key 缓存键
     * @param value 缓存值
     * @param ttl 生存时间
     */
    void put(String key, Object value, Duration ttl);

    /**
     * 设置缓存值（使用默认TTL）
     *
     * @param key 缓存键
     * @param value 缓存值
     */
    void put(String key, Object value);

    /**
     * 获取或设置缓存值
     * 如果缓存不存在，则调用supplier获取值并缓存
     *
     * @param key 缓存键
     * @param valueType 值类型
     * @param supplier 值提供者
     * @param ttl 生存时间
     * @return 缓存值
     */
    <T> T getOrSet(String key, Class<T> valueType, Supplier<T> supplier, Duration ttl);

    /**
     * 获取或设置缓存值（使用默认TTL）
     *
     * @param key 缓存键
     * @param valueType 值类型
     * @param supplier 值提供者
     * @return 缓存值
     */
    <T> T getOrSet(String key, Class<T> valueType, Supplier<T> supplier);

    /**
     * 删除缓存
     *
     * @param key 缓存键
     * @return 是否删除成功
     */
    boolean evict(String key);

    /**
     * 批量删除缓存
     *
     * @param pattern 键模式（支持通配符）
     * @return 删除的键数量
     */
    int evictByPattern(String pattern);

    /**
     * 清空指定前缀的缓存
     *
     * @param prefix 键前缀
     * @return 删除的键数量
     */
    int evictByPrefix(String prefix);

    /**
     * 清空所有缓存
     */
    void clear();

    /**
     * 检查缓存是否存在
     *
     * @param key 缓存键
     * @return 是否存在
     */
    boolean exists(String key);

    /**
     * 获取缓存统计信息
     *
     * @return 缓存统计信息
     */
    CacheStats getStats();

    /**
     * 缓存统计信息
     */
    class CacheStats {
        private final long hitCount;
        private final long missCount;
        private final long evictionCount;
        private final long size;
        private final double hitRate;

        public CacheStats(long hitCount, long missCount, long evictionCount, long size) {
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.evictionCount = evictionCount;
            this.size = size;
            this.hitRate = (hitCount + missCount) > 0 ? (double) hitCount / (hitCount + missCount) : 0.0;
        }

        public long getHitCount() { return hitCount; }
        public long getMissCount() { return missCount; }
        public long getEvictionCount() { return evictionCount; }
        public long getSize() { return size; }
        public double getHitRate() { return hitRate; }

        @Override
        public String toString() {
            return String.format("CacheStats{hits=%d, misses=%d, evictions=%d, size=%d, hitRate=%.2f%%}",
                    hitCount, missCount, evictionCount, size, hitRate * 100);
        }
    }
}