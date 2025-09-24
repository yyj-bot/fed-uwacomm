package com.feduwacomm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 联邦学习聚合配置类
 * 控制聚合触发策略、超时设置、算法参数等
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "federated.aggregation")
public class AggregationConfig {

    /**
     * 最少参与者数量（触发聚合的最低客户端数）
     */
    private int minParticipants = 2;

    /**
     * 最大等待时间（秒）
     * 超过此时间将强制触发聚合
     */
    private int maxWaitTimeSeconds = 300;

    /**
     * 参与率阈值（0.0-1.0）
     * 达到期望参与者的此比例时可触发聚合
     */
    private double participationRate = 0.8;

    /**
     * 是否启用超时聚合
     * 如果禁用，将只在所有期望参与者都上传后才聚合
     */
    private boolean enableTimeoutAggregation = true;

    /**
     * 定时检查间隔（秒）
     * 用于定时检查待聚合任务
     */
    private int checkIntervalSeconds = 30;

    /**
     * 聚合线程池大小
     */
    private int aggregationThreadPoolSize = 5;

    /**
     * 最大并发聚合任务数
     */
    private int maxConcurrentAggregations = 3;

    /**
     * 聚合重试次数
     */
    private int maxRetryAttempts = 3;

    /**
     * 聚合超时时间（秒）
     * 单次聚合操作的最大执行时间
     */
    private int aggregationTimeoutSeconds = 120;

    /**
     * 是否启用聚合结果缓存
     */
    private boolean enableResultCache = true;

    /**
     * 缓存过期时间（分钟）
     */
    private int cacheExpirationMinutes = 60;

    /**
     * FedAvg算法配置
     */
    private FedAvgConfig fedAvg = new FedAvgConfig();

    /**
     * FedProx算法配置
     */
    private FedProxConfig fedProx = new FedProxConfig();

    /**
     * FedNova算法配置
     */
    private FedNovaConfig fedNova = new FedNovaConfig();

    /**
     * 事件发布配置
     */
    private EventConfig event = new EventConfig();

    /**
     * 模型分发配置
     */
    private ModelDistributionConfig modelDistribution = new ModelDistributionConfig();

    @Data
    public static class FedAvgConfig {
        /**
         * 是否启用权重归一化
         */
        private boolean enableWeightNormalization = true;

        /**
         * 最小权重值（避免除零）
         */
        private double minWeight = 1e-8;

        /**
         * 数值精度控制
         */
        private int decimalScale = 8;
    }

    @Data
    public static class FedProxConfig {
        /**
         * 默认正则化系数μ
         */
        private double defaultMu = 0.01;

        /**
         * μ的取值范围
         */
        private double minMu = 0.0;
        private double maxMu = 1.0;

        /**
         * 是否启用自适应μ调整
         */
        private boolean enableAdaptiveMu = false;
    }

    @Data
    public static class FedNovaConfig {
        /**
         * 默认本地训练步数
         */
        private int defaultLocalSteps = 1;

        /**
         * 最大本地训练步数
         */
        private int maxLocalSteps = 100;

        /**
         * 是否启用步数归一化
         */
        private boolean enableStepNormalization = true;
    }

    @Data
    public static class EventConfig {
        /**
         * 是否启用异步事件处理
         */
        private boolean enableAsyncEventHandling = true;

        /**
         * 事件处理线程池大小
         */
        private int eventThreadPoolSize = 3;

        /**
         * 事件重试次数
         */
        private int eventRetryAttempts = 2;
    }

    @Data
    public static class ModelDistributionConfig {
        /**
         * 是否启用模型压缩
         */
        private boolean enableCompression = false;

        /**
         * 压缩算法（GZIP, LZ4等）
         */
        private String compressionAlgorithm = "GZIP";

        /**
         * 最大模型大小（MB）
         */
        private int maxModelSizeMB = 100;

        /**
         * 分发超时时间（秒）
         */
        private int distributionTimeoutSeconds = 60;

        /**
         * 并行分发客户端数量
         */
        private int parallelDistributionCount = 10;
    }

    /**
     * 验证配置参数的有效性
     */
    public void validateConfig() {
        if (minParticipants < 1) {
            throw new IllegalArgumentException("最少参与者数量必须大于0");
        }

        if (participationRate < 0.0 || participationRate > 1.0) {
            throw new IllegalArgumentException("参与率阈值必须在0.0-1.0之间");
        }

        if (maxWaitTimeSeconds <= 0) {
            throw new IllegalArgumentException("最大等待时间必须大于0");
        }

        if (checkIntervalSeconds <= 0) {
            throw new IllegalArgumentException("检查间隔必须大于0");
        }

        if (fedProx.getMinMu() < 0.0 || fedProx.getMaxMu() > 1.0 || fedProx.getMinMu() > fedProx.getMaxMu()) {
            throw new IllegalArgumentException("FedProx的μ参数范围设置无效");
        }
    }

    /**
     * 根据算法类型获取相应配置
     */
    public Object getAlgorithmConfig(String algorithm) {
        return switch (algorithm.toUpperCase()) {
            case "FEDERATED_AVERAGING" -> fedAvg;
            case "FEDERATED_PROXIMAL" -> fedProx;
            case "FEDERATED_NOVA" -> fedNova;
            default -> null;
        };
    }

    /**
     * 判断是否应该触发聚合
     */
    public boolean shouldTriggerAggregation(int currentParticipants, int expectedParticipants, long waitTimeSeconds) {
        // 达到所有预期参与者
        if (currentParticipants >= expectedParticipants) {
            return true;
        }

        // 达到最少参与者且满足参与率
        if (currentParticipants >= minParticipants) {
            double currentRate = (double) currentParticipants / expectedParticipants;
            if (currentRate >= participationRate) {
                return true;
            }
        }

        // 超时触发
        if (enableTimeoutAggregation && waitTimeSeconds >= maxWaitTimeSeconds) {
            return currentParticipants >= minParticipants;
        }

        return false;
    }
}