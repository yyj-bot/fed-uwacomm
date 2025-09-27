package com.feduwacomm.aggregation;

import com.feduwacomm.entity.VmRoundModel;

import java.util.List;
import java.util.Map;

/**
 * 聚合策略接口
 *
 * 定义联邦学习聚合算法的统一接口，支持多种聚合策略：
 * - FedAvg: 联邦平均算法
 * - FedProx: 联邦近端算法
 * - FedNova: Nova算法
 * - Scaffold: Scaffold算法
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
public interface AggregationStrategy {

    /**
     * 执行聚合操作
     *
     * @param models 参与聚合的本地模型列表
     * @param taskConfig 任务配置参数
     * @return 聚合后的全局模型参数
     * @throws AggregationException 聚合过程中发生异常
     */
    Map<String, Object> aggregate(List<VmRoundModel> models, Map<String, Object> taskConfig)
            throws AggregationException;

    /**
     * 验证聚合前置条件
     *
     * @param models 本地模型列表
     * @param taskConfig 任务配置
     * @return 验证结果
     */
    default ValidationResult validatePreconditions(List<VmRoundModel> models,
                                                  Map<String, Object> taskConfig) {
        if (models == null || models.isEmpty()) {
            return ValidationResult.invalid("模型列表不能为空");
        }

        if (models.size() < getMinimumParticipants()) {
            return ValidationResult.invalid("参与者数量不足，最少需要 " + getMinimumParticipants() + " 个");
        }

        return ValidationResult.valid();
    }

    /**
     * 获取最小参与者数量要求
     *
     * @return 最小参与者数量
     */
    default int getMinimumParticipants() {
        return 2;
    }

    /**
     * 获取策略名称
     *
     * @return 策略名称
     */
    String getStrategyName();

    /**
     * 获取支持的模型类型
     *
     * @return 支持的模型类型列表
     */
    default List<String> getSupportedModelTypes() {
        return List.of("RandomForest", "NeuralNetwork");
    }

    /**
     * 验证结果封装类
     */
    class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}