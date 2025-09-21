package com.feduwacomm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static com.feduwacomm.constants.SystemConstants.*;

/**
 * 时间配置属性类
 *
 * 管理系统中所有时间相关的配置项，包括超时时间、延迟时间、过期时间等
 *
 * @author FedUWAComm Team
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "feduwacomm.time")
public class TimeProperties {

    /**
     * JWT过期秒数
     * 默认: 86400秒（24小时）
     */
    private int jwtExpireSeconds = 86400;

    /**
     * VM密钥过期天数
     * 默认: 30天
     */
    private int vmSecretExpireDays = 30;

    /**
     * 日志保留天数
     * 默认: 7天
     */
    private int logRetentionDays = DEFAULT_LOG_RETENTION_DAYS;

    /**
     * ISO日期时间格式
     * 默认: yyyy-MM-dd'T'HH:mm:ss
     */
    private String isoDatetimeFormat = ISO_DATETIME_FORMAT;

    /**
     * 默认用户锁定持续时间（秒）
     * 默认: 3600秒（1小时）
     */
    private int defaultUserLockDurationSeconds = 3600;

    /**
     * 默认任务超时时间（秒）
     * 默认: 3600秒（1小时）
     */
    private int defaultTaskTimeoutSeconds = 3600;

    /**
     * 最大任务超时时间（秒）
     * 默认: 86400秒（24小时）
     */
    private int maxTaskTimeoutSeconds = 86400;

    /**
     * 最小任务超时时间（秒）
     * 默认: 60秒（1分钟）
     */
    private int minTaskTimeoutSeconds = 60;

    /**
     * 基础处理延迟（毫秒）
     * 默认: 1000毫秒（1秒）
     */
    private long baseProcessingDelayMillis = 1000;

    /**
     * 短处理延迟（毫秒）
     * 默认: 5000毫秒（5秒）
     */
    private long shortProcessingDelayMillis = 5000;

    /**
     * 长处理延迟（毫秒）
     * 默认: 10000毫秒（10秒）
     */
    private long longProcessingDelayMillis = 10000;

    /**
     * 模型生成基础延迟（毫秒）
     * 默认: 2000毫秒（2秒）
     */
    private long modelGenerationBaseDelayMillis = 2000;

    /**
     * 模型生成随机范围（毫秒）
     * 默认: 5000毫秒（5秒）
     */
    private long modelGenerationRandomRangeMillis = 5000;

    /**
     * 性能计算窗口（秒）
     * 默认: 3600秒（1小时）
     */
    private int performanceCalculationWindowSeconds = 3600;

    /**
     * 获取默认用户锁定持续时间
     * @return Duration对象
     */
    public Duration getDefaultUserLockDuration() {
        return Duration.ofSeconds(defaultUserLockDurationSeconds);
    }

    /**
     * 获取默认任务超时时间
     * @return Duration对象
     */
    public Duration getDefaultTaskTimeout() {
        return Duration.ofSeconds(defaultTaskTimeoutSeconds);
    }

    /**
     * 获取最大任务超时时间
     * @return Duration对象
     */
    public Duration getMaxTaskTimeout() {
        return Duration.ofSeconds(maxTaskTimeoutSeconds);
    }

    /**
     * 获取最小任务超时时间
     * @return Duration对象
     */
    public Duration getMinTaskTimeout() {
        return Duration.ofSeconds(minTaskTimeoutSeconds);
    }

    /**
     * 获取基础处理延迟
     * @return Duration对象
     */
    public Duration getBaseProcessingDelay() {
        return Duration.ofMillis(baseProcessingDelayMillis);
    }

    /**
     * 获取短处理延迟
     * @return Duration对象
     */
    public Duration getShortProcessingDelay() {
        return Duration.ofMillis(shortProcessingDelayMillis);
    }

    /**
     * 获取长处理延迟
     * @return Duration对象
     */
    public Duration getLongProcessingDelay() {
        return Duration.ofMillis(longProcessingDelayMillis);
    }

    /**
     * 获取模型生成基础延迟
     * @return Duration对象
     */
    public Duration getModelGenerationBaseDelay() {
        return Duration.ofMillis(modelGenerationBaseDelayMillis);
    }

    /**
     * 获取模型生成随机范围
     * @return Duration对象
     */
    public Duration getModelGenerationRandomRange() {
        return Duration.ofMillis(modelGenerationRandomRangeMillis);
    }

    /**
     * 获取性能计算窗口
     * @return Duration对象
     */
    public Duration getPerformanceCalculationWindow() {
        return Duration.ofSeconds(performanceCalculationWindowSeconds);
    }

    /**
     * 验证任务超时时间是否有效
     * @param timeoutSeconds 超时时间（秒）
     * @return 是否有效
     */
    public boolean isValidTaskTimeout(int timeoutSeconds) {
        return timeoutSeconds >= minTaskTimeoutSeconds && timeoutSeconds <= maxTaskTimeoutSeconds;
    }

    /**
     * 获取任务超时时间验证错误消息
     * @return 错误消息
     */
    public String getTaskTimeoutValidationMessage() {
        return String.format("任务超时时间必须在%d秒到%d秒之间", minTaskTimeoutSeconds, maxTaskTimeoutSeconds);
    }
}