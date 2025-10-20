package com.feduwacomm.controller;

import com.feduwacomm.common.Result;
import com.feduwacomm.service.CacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓存管理控制器
 * 提供缓存统计、清理等管理功能
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-28
 */
@Slf4j
@RestController
@RequestMapping("/api/cache")
@Tag(name = "缓存管理", description = "缓存统计和管理相关接口")
@PreAuthorize("hasRole('ADMIN')")
public class CacheController {

    @Autowired
    private CacheService cacheService;

    /**
     * 获取缓存统计信息
     */
    @GetMapping("/stats")
    @Operation(summary = "获取缓存统计信息", description = "获取缓存的命中率、大小等统计信息")
    public Result<Map<String, Object>> getCacheStats() {
        try {
            CacheService.CacheStats stats = cacheService.getStats();

            Map<String, Object> result = new HashMap<>();
            result.put("hitCount", stats.getHitCount());
            result.put("missCount", stats.getMissCount());
            result.put("evictionCount", stats.getEvictionCount());
            result.put("size", stats.getSize());
            result.put("hitRate", stats.getHitRate());
            result.put("hitRatePercent", String.format("%.2f%%", stats.getHitRate() * 100));

            log.info("获取缓存统计信息: {}", stats);
            return Result.success(result);

        } catch (Exception e) {
            log.error("获取缓存统计信息失败: {}", e.getMessage(), e);
            return Result.error("获取缓存统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 检查缓存键是否存在
     */
    @GetMapping("/exists")
    @Operation(summary = "检查缓存键是否存在", description = "检查指定的缓存键是否存在且未过期")
    public Result<Boolean> existsCache(
            @Parameter(description = "缓存键", required = true)
            @RequestParam String key) {
        try {
            if (key == null || key.trim().isEmpty()) {
                return Result.error("缓存键不能为空");
            }

            boolean exists = cacheService.exists(key);
            log.debug("检查缓存键存在性: key={}, exists={}", key, exists);

            return Result.success(exists);

        } catch (Exception e) {
            log.error("检查缓存键存在性失败: key={}, error={}", key, e.getMessage(), e);
            return Result.error("检查缓存键存在性失败: " + e.getMessage());
        }
    }

    /**
     * 删除指定缓存
     */
    @DeleteMapping("/evict")
    @Operation(summary = "删除指定缓存", description = "删除指定键的缓存条目")
    public Result<Boolean> evictCache(
            @Parameter(description = "缓存键", required = true)
            @RequestParam String key) {
        try {
            if (key == null || key.trim().isEmpty()) {
                return Result.error("缓存键不能为空");
            }

            boolean success = cacheService.evict(key);
            log.info("删除缓存: key={}, success={}", key, success);

            return Result.success(success);

        } catch (Exception e) {
            log.error("删除缓存失败: key={}, error={}", key, e.getMessage(), e);
            return Result.error("删除缓存失败: " + e.getMessage());
        }
    }

    /**
     * 按模式删除缓存
     */
    @DeleteMapping("/evict-pattern")
    @Operation(summary = "按模式删除缓存", description = "根据模式（支持通配符*）删除匹配的缓存条目")
    public Result<Integer> evictCacheByPattern(
            @Parameter(description = "缓存键模式（支持通配符*）", required = true)
            @RequestParam String pattern) {
        try {
            if (pattern == null || pattern.trim().isEmpty()) {
                return Result.error("缓存键模式不能为空");
            }

            int count = cacheService.evictByPattern(pattern);
            log.info("按模式删除缓存: pattern={}, deletedCount={}", pattern, count);

            return Result.success(count);

        } catch (Exception e) {
            log.error("按模式删除缓存失败: pattern={}, error={}", pattern, e.getMessage(), e);
            return Result.error("按模式删除缓存失败: " + e.getMessage());
        }
    }

    /**
     * 按前缀删除缓存
     */
    @DeleteMapping("/evict-prefix")
    @Operation(summary = "按前缀删除缓存", description = "删除指定前缀的所有缓存条目")
    public Result<Integer> evictCacheByPrefix(
            @Parameter(description = "缓存键前缀", required = true)
            @RequestParam String prefix) {
        try {
            if (prefix == null || prefix.trim().isEmpty()) {
                return Result.error("缓存键前缀不能为空");
            }

            int count = cacheService.evictByPrefix(prefix);
            log.info("按前缀删除缓存: prefix={}, deletedCount={}", prefix, count);

            return Result.success(count);

        } catch (Exception e) {
            log.error("按前缀删除缓存失败: prefix={}, error={}", prefix, e.getMessage(), e);
            return Result.error("按前缀删除缓存失败: " + e.getMessage());
        }
    }

    /**
     * 清空所有缓存
     */
    @DeleteMapping("/clear")
    @Operation(summary = "清空所有缓存", description = "清空所有缓存条目，请谨慎操作")
    public Result<String> clearCache() {
        try {
            CacheService.CacheStats statsBefore = cacheService.getStats();
            cacheService.clear();

            String message = String.format("缓存已清空，删除了 %d 个条目", statsBefore.getSize());
            log.warn("清空所有缓存: deletedCount={}", statsBefore.getSize());

            return Result.success(message);

        } catch (Exception e) {
            log.error("清空缓存失败: {}", e.getMessage(), e);
            return Result.error("清空缓存失败: " + e.getMessage());
        }
    }

    /**
     * 预热常用缓存
     */
    @PostMapping("/warmup")
    @Operation(summary = "预热常用缓存", description = "预热系统中的常用缓存数据")
    public Result<String> warmupCache() {
        try {
            log.info("开始预热缓存...");

            // 实现具体的缓存预热逻辑
            int preheatedCount = 0;

            // 1. 预热系统配置和常量
            warmupSystemConfigs();
            preheatedCount += 10; // 预计系统配置数量

            // 2. 预热常用算法配置
            warmupAlgorithmConfigs();
            preheatedCount += 5;

            // 3. 预热联邦学习模板数据
            warmupFederatedLearningTemplates();
            preheatedCount += 8;

            // 4. 预热网络配置和资源限制
            warmupNetworkAndResourceConfigs();
            preheatedCount += 6;

            // 5. 预热示例数据和统计信息
            warmupExampleData();
            preheatedCount += 4;

            log.info("缓存预热完成，共预热 {} 个缓存条目", preheatedCount);

            String message = "缓存预热完成";
            log.info("缓存预热完成");

            return Result.success(message);

        } catch (Exception e) {
            log.error("缓存预热失败: {}", e.getMessage(), e);
            return Result.error("缓存预热失败: " + e.getMessage());
        }
    }

    /**
     * 获取缓存健康状态
     */
    @GetMapping("/health")
    @Operation(summary = "获取缓存健康状态", description = "获取缓存系统的健康状态信息")
    public Result<Map<String, Object>> getCacheHealth() {
        try {
            CacheService.CacheStats stats = cacheService.getStats();

            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("hitRate", stats.getHitRate());
            health.put("size", stats.getSize());

            // 健康状态评估
            String healthStatus = "HEALTHY";
            if (stats.getHitRate() < 0.5) {
                healthStatus = "WARNING"; // 命中率低于50%
            }
            if (stats.getSize() > 8000) {
                healthStatus = "WARNING"; // 缓存条目过多
            }

            health.put("healthStatus", healthStatus);
            health.put("details", stats.toString());

            log.debug("获取缓存健康状态: {}", healthStatus);
            return Result.success(health);

        } catch (Exception e) {
            log.error("获取缓存健康状态失败: {}", e.getMessage(), e);

            Map<String, Object> errorHealth = new HashMap<>();
            errorHealth.put("status", "DOWN");
            errorHealth.put("healthStatus", "ERROR");
            errorHealth.put("error", e.getMessage());

            return Result.success(errorHealth);
        }
    }

    /**
     * 预热系统配置
     */
    private void warmupSystemConfigs() {
        try {
            cacheService.put("system:config:version", "1.5.0");
            cacheService.put("system:config:name", "FedUWAComm");
            cacheService.put("system:config:protocol_version", "1.5");
            cacheService.put("system:config:environment", "production");
            cacheService.put("system:config:startup_time", System.currentTimeMillis());
            cacheService.put("system:config:admin_email", "admin@feduwacomm.com");
            cacheService.put("system:config:max_concurrent_tasks", 10);
            cacheService.put("system:config:default_timeout", 300000);
            cacheService.put("system:config:heartbeat_interval", 30000);
            cacheService.put("system:config:websocket_enabled", true);
            log.debug("系统配置缓存预热完成");
        } catch (Exception e) {
            log.warn("预热系统配置失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 预热算法配置
     */
    private void warmupAlgorithmConfigs() {
        try {
            cacheService.put("algorithm:federated_averaging:enabled", true);
            cacheService.put("algorithm:federated_proximal:enabled", true);
            cacheService.put("algorithm:federated_nova:enabled", true);
            cacheService.put("algorithm:federated_scaffold:enabled", false);
            cacheService.put("algorithm:default", "FEDERATED_AVERAGING");
            log.debug("算法配置缓存预热完成");
        } catch (Exception e) {
            log.warn("预热算法配置失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 预热联邦学习模板
     */
    private void warmupFederatedLearningTemplates() {
        try {
            cacheService.put("template:classification:default_epochs", 100);
            cacheService.put("template:classification:default_learning_rate", 0.01);
            cacheService.put("template:classification:default_batch_size", 32);
            cacheService.put("template:regression:default_epochs", 150);
            cacheService.put("template:regression:default_learning_rate", 0.001);
            cacheService.put("template:regression:default_batch_size", 64);
            cacheService.put("template:acoustic:default_sample_rate", 48000);
            cacheService.put("template:acoustic:default_frequency_range", "1kHz-10kHz");
            log.debug("联邦学习模板缓存预热完成");
        } catch (Exception e) {
            log.warn("预热联邦学习模板失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 预热网络和资源配置
     */
    private void warmupNetworkAndResourceConfigs() {
        try {
            cacheService.put("limits:max_file_size", 100 * 1024 * 1024); // 100MB
            cacheService.put("limits:max_model_size", 500 * 1024 * 1024); // 500MB
            cacheService.put("limits:session_timeout", 30 * 60); // 30分钟
            cacheService.put("limits:max_participants", 50);
            cacheService.put("network:default_port", 8080);
            cacheService.put("network:websocket_port", 8081);
            log.debug("网络和资源配置缓存预热完成");
        } catch (Exception e) {
            log.warn("预热网络和资源配置失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 预热示例数据
     */
    private void warmupExampleData() {
        try {
            // 预热统计信息和示例数据
            cacheService.put("stats:total_tasks_created", 0);
            cacheService.put("stats:total_models_uploaded", 0);
            cacheService.put("stats:total_data_processed_gb", 0.0);
            cacheService.put("example:sample_accuracy", 0.85);
            log.debug("示例缓存数据预热完成");
        } catch (Exception e) {
            log.warn("预热示例数据失败: {}", e.getMessage(), e);
        }
    }
}
