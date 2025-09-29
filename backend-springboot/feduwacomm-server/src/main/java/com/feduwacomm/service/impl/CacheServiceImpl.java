package com.feduwacomm.service.impl;

import com.feduwacomm.service.CacheService;
import com.feduwacomm.service.PerformanceMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * 内存缓存服务实现
 * 基于ConcurrentHashMap的高性能内存缓存
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-28
 */
@Slf4j
@Service
public class CacheServiceImpl implements CacheService {

    // 默认配置
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);
    private static final long CLEANUP_INTERVAL_SECONDS = 60; // 清理间隔60秒
    private static final int MAX_CACHE_SIZE = 10000; // 最大缓存条目数

    // 缓存存储
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    // 统计信息
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong evictionCount = new AtomicLong(0);

    // 定时清理服务
    private ScheduledExecutorService cleanupExecutor;

    // 性能监控服务
    @Autowired(required = false)
    private PerformanceMonitorService performanceMonitorService;

    /**
     * 缓存条目
     */
    private static class CacheEntry {
        private final Object value;
        private final LocalDateTime createdAt;
        private final LocalDateTime expiresAt;
        private volatile LocalDateTime lastAccessedAt;

        public CacheEntry(Object value, Duration ttl) {
            this.value = value;
            this.createdAt = LocalDateTime.now();
            this.lastAccessedAt = this.createdAt;
            this.expiresAt = ttl != null ? this.createdAt.plus(ttl) : null;
        }

        public Object getValue() {
            this.lastAccessedAt = LocalDateTime.now();
            return value;
        }

        public boolean isExpired() {
            return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
        }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
    }

    @PostConstruct
    public void init() {
        // 启动定时清理任务
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cache-cleanup");
            t.setDaemon(true);
            return t;
        });

        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredEntries,
                CLEANUP_INTERVAL_SECONDS, CLEANUP_INTERVAL_SECONDS, TimeUnit.SECONDS);

        log.info("缓存服务已启动，清理间隔: {}秒, 最大缓存大小: {}", CLEANUP_INTERVAL_SECONDS, MAX_CACHE_SIZE);
    }

    @PreDestroy
    public void destroy() {
        if (cleanupExecutor != null && !cleanupExecutor.isShutdown()) {
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        cache.clear();
        log.info("缓存服务已关闭");
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> valueType) {
        long startTime = System.currentTimeMillis();
        boolean hit = false;

        try {
            if (key == null || valueType == null) {
                return Optional.empty();
            }

            CacheEntry entry = cache.get(key);
            if (entry == null) {
                missCount.incrementAndGet();
                log.debug("缓存未命中: key={}", key);
                return Optional.empty();
            }

            if (entry.isExpired()) {
                cache.remove(key);
                missCount.incrementAndGet();
                log.debug("缓存已过期: key={}", key);
                return Optional.empty();
            }

            try {
                Object value = entry.getValue();
                if (valueType.isInstance(value)) {
                    hitCount.incrementAndGet();
                    hit = true;
                    log.debug("缓存命中: key={}, type={}", key, valueType.getSimpleName());
                    return Optional.of(valueType.cast(value));
                } else {
                    // 类型不匹配，移除缓存
                    cache.remove(key);
                    missCount.incrementAndGet();
                    log.warn("缓存类型不匹配: key={}, expected={}, actual={}",
                            key, valueType.getSimpleName(), value.getClass().getSimpleName());
                    return Optional.empty();
                }
            } catch (Exception e) {
                cache.remove(key);
                missCount.incrementAndGet();
                log.error("获取缓存值失败: key={}, error={}", key, e.getMessage(), e);
                return Optional.empty();
            }
        } finally {
            // 记录缓存操作性能
            if (performanceMonitorService != null) {
                long executionTime = System.currentTimeMillis() - startTime;
                performanceMonitorService.recordCacheOperation("GET", key, hit, executionTime);
            }
        }
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        long startTime = System.currentTimeMillis();

        try {
            if (key == null || value == null) {
                log.warn("缓存键或值不能为空: key={}, value={}", key, value);
                return;
            }

            // 检查缓存大小限制
            if (cache.size() >= MAX_CACHE_SIZE) {
                evictLeastRecentlyUsed();
            }

            CacheEntry entry = new CacheEntry(value, ttl);
            cache.put(key, entry);

            log.debug("缓存已设置: key={}, ttl={}, size={}", key, ttl, cache.size());
        } finally {
            // 记录缓存操作性能
            if (performanceMonitorService != null) {
                long executionTime = System.currentTimeMillis() - startTime;
                performanceMonitorService.recordCacheOperation("PUT", key, false, executionTime);
            }
        }
    }

    @Override
    public void put(String key, Object value) {
        put(key, value, DEFAULT_TTL);
    }

    @Override
    public <T> T getOrSet(String key, Class<T> valueType, Supplier<T> supplier, Duration ttl) {
        if (key == null || valueType == null || supplier == null) {
            throw new IllegalArgumentException("参数不能为空");
        }

        Optional<T> cachedValue = get(key, valueType);
        if (cachedValue.isPresent()) {
            return cachedValue.get();
        }

        try {
            T value = supplier.get();
            if (value != null) {
                put(key, value, ttl);
                log.debug("缓存已设置 (通过supplier): key={}, type={}", key, valueType.getSimpleName());
            }
            return value;
        } catch (Exception e) {
            log.error("通过supplier获取值失败: key={}, error={}", key, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public <T> T getOrSet(String key, Class<T> valueType, Supplier<T> supplier) {
        return getOrSet(key, valueType, supplier, DEFAULT_TTL);
    }

    @Override
    public boolean evict(String key) {
        long startTime = System.currentTimeMillis();

        try {
            if (key == null) {
                return false;
            }

            boolean removed = cache.remove(key) != null;
            if (removed) {
                evictionCount.incrementAndGet();
                log.debug("缓存已删除: key={}", key);
            }
            return removed;
        } finally {
            // 记录缓存操作性能
            if (performanceMonitorService != null) {
                long executionTime = System.currentTimeMillis() - startTime;
                performanceMonitorService.recordCacheOperation("REMOVE", key, false, executionTime);
            }
        }
    }

    @Override
    public int evictByPattern(String pattern) {
        if (pattern == null) {
            return 0;
        }

        try {
            Pattern regex = Pattern.compile(pattern.replace("*", ".*"));
            int count = 0;

            var iterator = cache.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                if (regex.matcher(entry.getKey()).matches()) {
                    iterator.remove();
                    count++;
                }
            }

            evictionCount.addAndGet(count);
            log.debug("按模式删除缓存: pattern={}, count={}", pattern, count);
            return count;

        } catch (Exception e) {
            log.error("按模式删除缓存失败: pattern={}, error={}", pattern, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public int evictByPrefix(String prefix) {
        if (prefix == null) {
            return 0;
        }

        int count = 0;
        var iterator = cache.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getKey().startsWith(prefix)) {
                iterator.remove();
                count++;
            }
        }

        evictionCount.addAndGet(count);
        log.debug("按前缀删除缓存: prefix={}, count={}", prefix, count);
        return count;
    }

    @Override
    public void clear() {
        int size = cache.size();
        cache.clear();
        evictionCount.addAndGet(size);
        log.info("缓存已清空: 删除了 {} 个条目", size);
    }

    @Override
    public boolean exists(String key) {
        if (key == null) {
            return false;
        }

        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return false;
        }

        if (entry.isExpired()) {
            cache.remove(key);
            return false;
        }

        return true;
    }

    @Override
    public CacheStats getStats() {
        return new CacheStats(
                hitCount.get(),
                missCount.get(),
                evictionCount.get(),
                cache.size()
        );
    }

    /**
     * 清理过期条目
     */
    private void cleanupExpiredEntries() {
        try {
            int cleanedCount = 0;
            var iterator = cache.entrySet().iterator();

            while (iterator.hasNext()) {
                var entry = iterator.next();
                if (entry.getValue().isExpired()) {
                    iterator.remove();
                    cleanedCount++;
                }
            }

            if (cleanedCount > 0) {
                evictionCount.addAndGet(cleanedCount);
                log.debug("清理过期缓存条目: 删除了 {} 个过期条目，当前缓存大小: {}", cleanedCount, cache.size());
            }

        } catch (Exception e) {
            log.error("清理过期缓存条目失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 驱逐最近最少使用的条目
     */
    private void evictLeastRecentlyUsed() {
        if (cache.isEmpty()) {
            return;
        }

        try {
            // 找到最近最少使用的条目
            String lruKey = null;
            LocalDateTime oldestAccess = LocalDateTime.now();

            for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
                LocalDateTime lastAccessed = entry.getValue().getLastAccessedAt();
                if (lastAccessed.isBefore(oldestAccess)) {
                    oldestAccess = lastAccessed;
                    lruKey = entry.getKey();
                }
            }

            if (lruKey != null) {
                cache.remove(lruKey);
                evictionCount.incrementAndGet();
                log.debug("驱逐LRU缓存条目: key={}, lastAccessed={}", lruKey, oldestAccess);
            }

        } catch (Exception e) {
            log.error("驱逐LRU缓存条目失败: {}", e.getMessage(), e);
        }
    }
}