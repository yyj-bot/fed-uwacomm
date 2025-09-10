package com.feduwacomm.service.cache;

import com.feduwacomm.common.PageResult;
import com.feduwacomm.dto.LogQueryDTO;
import com.feduwacomm.vo.LogListVO;
import com.feduwacomm.vo.LogStatisticsVO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 日志缓存服务
 * 提供日志查询结果的缓存功能，提升查询性能
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Service
public class LogCacheService {

    private static final Logger logger = LogManager.getLogger(LogCacheService.class);
    
    // 缓存过期时间（分钟）
    private static final int CACHE_EXPIRE_MINUTES = 5;
    
    // 最大缓存条目数
    private static final int MAX_CACHE_SIZE = 1000;
    
    // 查询结果缓存
    private final Map<String, CacheEntry<PageResult<LogListVO>>> queryCache = new ConcurrentHashMap<>();
    
    // 统计数据缓存
    private final Map<String, CacheEntry<LogStatisticsVO>> statisticsCache = new ConcurrentHashMap<>();
    
    // 监控数据缓存
    private final Map<String, CacheEntry<Object>> monitorCache = new ConcurrentHashMap<>();
    
    // 缓存清理任务调度器
    private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor();
    
    /**
     * 缓存条目
     */
    private static class CacheEntry<T> {
        private final T data;
        private final LocalDateTime createTime;
        private final int expireMinutes;
        
        public CacheEntry(T data, int expireMinutes) {
            this.data = data;
            this.createTime = LocalDateTime.now();
            this.expireMinutes = expireMinutes;
        }
        
        public T getData() {
            return data;
        }
        
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(createTime.plusMinutes(expireMinutes));
        }
        
        public LocalDateTime getCreateTime() {
            return createTime;
        }
    }
    
    public LogCacheService() {
        // 启动定期缓存清理任务
        cleanupScheduler.scheduleAtFixedRate(this::cleanupExpiredCache, 1, 1, TimeUnit.MINUTES);
        logger.info("日志缓存服务已启动，缓存过期时间: {} 分钟", CACHE_EXPIRE_MINUTES);
    }
    
    /**
     * 获取查询结果缓存
     */
    public PageResult<LogListVO> getQueryCache(LogQueryDTO queryDTO) {
        try {
            String cacheKey = generateQueryCacheKey(queryDTO);
            CacheEntry<PageResult<LogListVO>> entry = queryCache.get(cacheKey);
            
            if (entry != null && !entry.isExpired()) {
                logger.debug("命中查询缓存: {}", cacheKey);
                return entry.getData();
            } else if (entry != null) {
                // 缓存过期，移除
                queryCache.remove(cacheKey);
                logger.debug("查询缓存已过期: {}", cacheKey);
            }
            
            return null;
        } catch (Exception e) {
            logger.warn("获取查询缓存失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 设置查询结果缓存
     */
    public void putQueryCache(LogQueryDTO queryDTO, PageResult<LogListVO> result) {
        try {
            if (result == null || result.getRecords() == null) {
                return;
            }
            
            String cacheKey = generateQueryCacheKey(queryDTO);
            
            // 检查缓存大小限制
            if (queryCache.size() >= MAX_CACHE_SIZE) {
                // 移除最旧的缓存条目
                removeOldestCacheEntry(queryCache);
            }
            
            queryCache.put(cacheKey, new CacheEntry<>(result, CACHE_EXPIRE_MINUTES));
            logger.debug("设置查询缓存: {}, 结果数量: {}", cacheKey, result.getRecords().size());
            
        } catch (Exception e) {
            logger.warn("设置查询缓存失败: {}", e.getMessage());
        }
    }
    
    /**
     * 获取统计数据缓存
     */
    public LogStatisticsVO getStatisticsCache(LogQueryDTO queryDTO) {
        try {
            String cacheKey = generateStatisticsCacheKey(queryDTO);
            CacheEntry<LogStatisticsVO> entry = statisticsCache.get(cacheKey);
            
            if (entry != null && !entry.isExpired()) {
                logger.debug("命中统计缓存: {}", cacheKey);
                return entry.getData();
            } else if (entry != null) {
                statisticsCache.remove(cacheKey);
                logger.debug("统计缓存已过期: {}", cacheKey);
            }
            
            return null;
        } catch (Exception e) {
            logger.warn("获取统计缓存失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 设置统计数据缓存
     */
    public void putStatisticsCache(LogQueryDTO queryDTO, LogStatisticsVO statistics) {
        try {
            if (statistics == null) {
                return;
            }
            
            String cacheKey = generateStatisticsCacheKey(queryDTO);
            
            if (statisticsCache.size() >= MAX_CACHE_SIZE) {
                removeOldestCacheEntry(statisticsCache);
            }
            
            statisticsCache.put(cacheKey, new CacheEntry<>(statistics, CACHE_EXPIRE_MINUTES));
            logger.debug("设置统计缓存: {}", cacheKey);
            
        } catch (Exception e) {
            logger.warn("设置统计缓存失败: {}", e.getMessage());
        }
    }
    
    /**
     * 获取监控数据缓存
     */
    @SuppressWarnings("unchecked")
    public <T> T getMonitorCache(String cacheKey, Class<T> type) {
        try {
            CacheEntry<Object> entry = monitorCache.get(cacheKey);
            
            if (entry != null && !entry.isExpired()) {
                logger.debug("命中监控缓存: {}", cacheKey);
                return (T) entry.getData();
            } else if (entry != null) {
                monitorCache.remove(cacheKey);
                logger.debug("监控缓存已过期: {}", cacheKey);
            }
            
            return null;
        } catch (Exception e) {
            logger.warn("获取监控缓存失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 设置监控数据缓存
     */
    public void putMonitorCache(String cacheKey, Object data, int expireMinutes) {
        try {
            if (data == null) {
                return;
            }
            
            if (monitorCache.size() >= MAX_CACHE_SIZE) {
                removeOldestCacheEntry(monitorCache);
            }
            
            monitorCache.put(cacheKey, new CacheEntry<>(data, expireMinutes));
            logger.debug("设置监控缓存: {}, 过期时间: {} 分钟", cacheKey, expireMinutes);
            
        } catch (Exception e) {
            logger.warn("设置监控缓存失败: {}", e.getMessage());
        }
    }
    
    /**
     * 设置监控数据缓存（使用默认过期时间）
     */
    public void putMonitorCache(String cacheKey, Object data) {
        putMonitorCache(cacheKey, data, CACHE_EXPIRE_MINUTES);
    }
    
    /**
     * 清除指定模式的缓存
     */
    public void evictCacheByPattern(String pattern) {
        try {
            // 清除查询缓存
            queryCache.keySet().removeIf(key -> key.contains(pattern));
            
            // 清除统计缓存
            statisticsCache.keySet().removeIf(key -> key.contains(pattern));
            
            // 清除监控缓存
            monitorCache.keySet().removeIf(key -> key.contains(pattern));
            
            logger.info("已清除匹配模式 '{}' 的缓存", pattern);
        } catch (Exception e) {
            logger.warn("清除缓存失败: {}", e.getMessage());
        }
    }
    
    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        try {
            queryCache.clear();
            statisticsCache.clear();
            monitorCache.clear();
            logger.info("已清除所有缓存");
        } catch (Exception e) {
            logger.warn("清除所有缓存失败: {}", e.getMessage());
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("queryCacheSize", queryCache.size());
        stats.put("statisticsCacheSize", statisticsCache.size());
        stats.put("monitorCacheSize", monitorCache.size());
        stats.put("totalCacheSize", queryCache.size() + statisticsCache.size() + monitorCache.size());
        stats.put("maxCacheSize", MAX_CACHE_SIZE);
        stats.put("cacheExpireMinutes", CACHE_EXPIRE_MINUTES);
        
        // 计算命中率（这里需要添加计数器来实现）
        stats.put("hitRate", "N/A"); // TODO: 实现命中率统计
        
        return stats;
    }
    
    /**
     * 预热缓存
     */
    public void warmupCache() {
        try {
            logger.info("开始预热日志缓存...");
            
            // 预热常用查询
            List<LogQueryDTO> commonQueries = getCommonQueries();
            for (LogQueryDTO query : commonQueries) {
                // 这里需要调用实际的查询方法来填充缓存
                // 由于这是缓存层，我们不直接调用数据库，而是标记需要预热
                String cacheKey = generateQueryCacheKey(query);
                logger.debug("标记预热查询: {}", cacheKey);
            }
            
            logger.info("缓存预热完成");
        } catch (Exception e) {
            logger.warn("缓存预热失败: {}", e.getMessage());
        }
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 生成查询缓存键
     */
    private String generateQueryCacheKey(LogQueryDTO queryDTO) {
        StringBuilder key = new StringBuilder("query:");
        
        if (queryDTO.getLevel() != null) {
            key.append("level=").append(queryDTO.getLevel()).append(";");
        }
        if (queryDTO.getCategory() != null) {
            key.append("category=").append(queryDTO.getCategory()).append(";");
        }
        if (queryDTO.getVmId() != null) {
            key.append("vmId=").append(queryDTO.getVmId()).append(";");
        }
        if (queryDTO.getTaskId() != null) {
            key.append("taskId=").append(queryDTO.getTaskId()).append(";");
        }
        if (queryDTO.getStartTime() != null) {
            key.append("startTime=").append(queryDTO.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append(";");
        }
        if (queryDTO.getEndTime() != null) {
            key.append("endTime=").append(queryDTO.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append(";");
        }
        if (queryDTO.getKeyword() != null) {
            key.append("keyword=").append(queryDTO.getKeyword()).append(";");
        }
        
        key.append("page=").append(queryDTO.getPage());
        key.append("size=").append(queryDTO.getSize());
        key.append("sort=").append(queryDTO.getSort());
        key.append("order=").append(queryDTO.getOrder());
        
        return key.toString();
    }
    
    /**
     * 生成统计缓存键
     */
    private String generateStatisticsCacheKey(LogQueryDTO queryDTO) {
        StringBuilder key = new StringBuilder("stats:");
        
        if (queryDTO.getLevel() != null) {
            key.append("level=").append(queryDTO.getLevel()).append(";");
        }
        if (queryDTO.getCategory() != null) {
            key.append("category=").append(queryDTO.getCategory()).append(";");
        }
        if (queryDTO.getVmId() != null) {
            key.append("vmId=").append(queryDTO.getVmId()).append(";");
        }
        if (queryDTO.getTaskId() != null) {
            key.append("taskId=").append(queryDTO.getTaskId()).append(";");
        }
        if (queryDTO.getStartTime() != null) {
            key.append("startTime=").append(queryDTO.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append(";");
        }
        if (queryDTO.getEndTime() != null) {
            key.append("endTime=").append(queryDTO.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append(";");
        }
        
        return key.toString();
    }
    
    /**
     * 移除最旧的缓存条目
     */
    private <T> void removeOldestCacheEntry(Map<String, CacheEntry<T>> cache) {
        String oldestKey = null;
        LocalDateTime oldestTime = LocalDateTime.now();
        
        for (Map.Entry<String, CacheEntry<T>> entry : cache.entrySet()) {
            if (entry.getValue().getCreateTime().isBefore(oldestTime)) {
                oldestTime = entry.getValue().getCreateTime();
                oldestKey = entry.getKey();
            }
        }
        
        if (oldestKey != null) {
            cache.remove(oldestKey);
            logger.debug("移除最旧的缓存条目: {}", oldestKey);
        }
    }
    
    /**
     * 清理过期缓存
     */
    private void cleanupExpiredCache() {
        try {
            int cleanedCount = 0;
            
            // 清理查询缓存
            cleanedCount += cleanExpiredEntries(queryCache);
            
            // 清理统计缓存
            cleanedCount += cleanExpiredEntries(statisticsCache);
            
            // 清理监控缓存
            cleanedCount += cleanExpiredEntries(monitorCache);
            
            if (cleanedCount > 0) {
                logger.debug("清理过期缓存条目: {} 个", cleanedCount);
            }
        } catch (Exception e) {
            logger.warn("清理过期缓存失败: {}", e.getMessage());
        }
    }
    
    /**
     * 清理过期条目
     */
    private <T> int cleanExpiredEntries(Map<String, CacheEntry<T>> cache) {
        List<String> expiredKeys = new ArrayList<>();
        
        for (Map.Entry<String, CacheEntry<T>> entry : cache.entrySet()) {
            if (entry.getValue().isExpired()) {
                expiredKeys.add(entry.getKey());
            }
        }
        
        for (String key : expiredKeys) {
            cache.remove(key);
        }
        
        return expiredKeys.size();
    }
    
    /**
     * 获取常用查询列表
     */
    private List<LogQueryDTO> getCommonQueries() {
        List<LogQueryDTO> queries = new ArrayList<>();
        
        // 最近1小时的错误日志
        queries.add(LogQueryDTO.builder()
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now())
                .page(1)
                .size(10)
                .build());
        
        // 最近24小时的所有日志
        queries.add(LogQueryDTO.builder()
                .startTime(LocalDateTime.now().minusHours(24))
                .endTime(LocalDateTime.now())
                .page(1)
                .size(20)
                .build());
        
        return queries;
    }
    
    /**
     * 销毁方法
     */
    public void destroy() {
        try {
            cleanupScheduler.shutdown();
            if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupScheduler.shutdownNow();
            }
            clearAllCache();
            logger.info("日志缓存服务已关闭");
        } catch (InterruptedException e) {
            cleanupScheduler.shutdownNow();
            Thread.currentThread().interrupt();
            logger.warn("关闭缓存服务时被中断");
        }
    }
}