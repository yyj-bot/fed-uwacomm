package com.feduwacomm.strategy;

import com.feduwacomm.entity.FederatedTask;
import com.feduwacomm.entity.VmRoundModel;
import com.feduwacomm.service.ModelAggregatorEngine;

import java.util.List;

/**
 * 聚合策略接口
 * 定义不同聚合算法的统一接口
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
public interface AggregationStrategy {

    /**
     * 获取策略名称
     */
    String getStrategyName();

    /**
     * 检查是否支持该算法
     */
    boolean supports(String algorithm);

    /**
     * 执行聚合
     *
     * @param localModels 本地模型列表
     * @param task        任务信息
     * @param engine      聚合引擎
     * @return 聚合结果
     */
    ModelAggregatorEngine.AggregationResult aggregate(List<VmRoundModel> localModels, 
                                                       FederatedTask task,
                                                       ModelAggregatorEngine engine);

    /**
     * 验证聚合前置条件
     *
     * @param localModels 本地模型列表
     * @param task        任务信息
     * @return 验证结果
     */
    ValidationResult validatePreconditions(List<VmRoundModel> localModels, FederatedTask task);

    /**
     * 获取算法配置建议
     */
    AlgorithmConfigSuggestion getConfigSuggestion(FederatedTask task, int participantCount);

    /**
     * 验证结果类
     */
    class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        public ValidationResult(boolean valid, String errorMessage) {
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

    /**
     * 算法配置建议类
     */
    class AlgorithmConfigSuggestion {
        private final String algorithm;
        private final java.util.Map<String, Object> suggestedParams;
        private final String reasoning;

        public AlgorithmConfigSuggestion(String algorithm, java.util.Map<String, Object> suggestedParams, String reasoning) {
            this.algorithm = algorithm;
            this.suggestedParams = suggestedParams;
            this.reasoning = reasoning;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public java.util.Map<String, Object> getSuggestedParams() {
            return suggestedParams;
        }

        public String getReasoning() {
            return reasoning;
        }
    }
}