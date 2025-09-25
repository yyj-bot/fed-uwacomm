package com.feduwacomm.strategy;

import com.feduwacomm.config.AggregationConfig;
import com.feduwacomm.entity.FederatedTask;
import com.feduwacomm.entity.GlobalModel;
import com.feduwacomm.entity.VmRoundModel;
import com.feduwacomm.enums.AggregationMethod;
import com.feduwacomm.enums.FederatedTaskStatus;
import com.feduwacomm.enums.GlobalModelStatus;
import com.feduwacomm.service.ModelAggregatorEngine;
import com.feduwacomm.utils.UuidUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * FedAvg聚合策略实现
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FedAvgStrategy implements AggregationStrategy {

    private final AggregationConfig aggregationConfig;
    private final UuidUtil uuidUtil;

    @Override
    public String getStrategyName() {
        return "FedAvg";
    }

    @Override
    public boolean supports(String algorithm) {
        return AggregationMethod.FEDERATED_AVERAGING.getCode().equalsIgnoreCase(algorithm);
    }

    @Override
    public ModelAggregatorEngine.AggregationResult aggregate(List<VmRoundModel> localModels, 
                                                              FederatedTask task,
                                                              ModelAggregatorEngine engine) {
        log.info("执行FedAvg聚合: 任务ID={}, 模型数量={}", task.getId(), localModels.size());

        // 创建全局模型记录
        GlobalModel globalModel = GlobalModel.builder()
                .id(uuidUtil.generateUuid())
                .taskId(task.getId())
                .roundNumber(task.getCurrentRound())
                .aggregationMethod(AggregationMethod.FEDERATED_AVERAGING)
                .status(GlobalModelStatus.AGGREGATING)
                .participantCount(localModels.size())
                .startedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        // 执行聚合
        return engine.aggregate(AggregationMethod.FEDERATED_AVERAGING.getCode(), localModels, task, globalModel);
    }

    @Override
    public ValidationResult validatePreconditions(List<VmRoundModel> localModels, FederatedTask task) {
        if (localModels == null || localModels.isEmpty()) {
            return ValidationResult.invalid("本地模型列表不能为空");
        }

        if (localModels.size() < aggregationConfig.getMinParticipants()) {
            return ValidationResult.invalid(
                    String.format("参与者数量不足，当前: %d, 最少需要: %d", 
                            localModels.size(), aggregationConfig.getMinParticipants()));
        }

        // 验证所有模型都有参数
        for (VmRoundModel model : localModels) {
            if (model.getParameters() == null || model.getParameters().trim().isEmpty()) {
                return ValidationResult.invalid(
                        String.format("客户端 %s 的模型参数为空", model.getVmId()));
            }
        }

        // 验证任务状态
        if (task == null || !FederatedTaskStatus.RUNNING.equals(task.getStatus())) {
            return ValidationResult.invalid("任务状态无效，无法进行聚合");
        }

        return ValidationResult.valid();
    }

    @Override
    public AlgorithmConfigSuggestion getConfigSuggestion(FederatedTask task, int participantCount) {
        Map<String, Object> suggestedParams = new HashMap<>();
        
        // FedAvg的配置建议
        AggregationConfig.FedAvgConfig fedAvgConfig = aggregationConfig.getFedAvg();
        suggestedParams.put("enableWeightNormalization", fedAvgConfig.isEnableWeightNormalization());
        suggestedParams.put("minWeight", fedAvgConfig.getMinWeight());
        suggestedParams.put("decimalScale", fedAvgConfig.getDecimalScale());

        // 根据参与者数量调整配置
        if (participantCount > 10) {
            suggestedParams.put("enableParallelProcessing", true);
            suggestedParams.put("batchSize", Math.max(5, participantCount / 4));
        }

        String reasoning = String.format(
                "基于%d个参与者的FedAvg配置建议。启用权重归一化以确保数值稳定性，" +
                "设置最小权重避免除零错误。", participantCount);

        if (participantCount > 10) {
            reasoning += "由于参与者较多，建议启用并行处理以提升性能。";
        }

        return new AlgorithmConfigSuggestion(AggregationMethod.FEDERATED_AVERAGING.getCode(), suggestedParams, reasoning);
    }
}