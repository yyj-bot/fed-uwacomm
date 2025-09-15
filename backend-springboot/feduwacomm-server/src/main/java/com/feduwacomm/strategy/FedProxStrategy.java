package com.feduwacomm.strategy;

import com.feduwacomm.config.AggregationConfig;
import com.feduwacomm.entity.FederatedTask;
import com.feduwacomm.entity.GlobalModel;
import com.feduwacomm.entity.VmRoundModel;
import com.feduwacomm.service.ModelAggregatorEngine;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * FedProx聚合策略实现
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FedProxStrategy implements AggregationStrategy {

    private final AggregationConfig aggregationConfig;
    private final ObjectMapper objectMapper;

    @Override
    public String getStrategyName() {
        return "FedProx";
    }

    @Override
    public boolean supports(String algorithm) {
        return "FEDPROX".equalsIgnoreCase(algorithm);
    }

    @Override
    public ModelAggregatorEngine.AggregationResult aggregate(List<VmRoundModel> localModels, 
                                                              FederatedTask task,
                                                              ModelAggregatorEngine engine) {
        log.info("执行FedProx聚合: 任务ID={}, 模型数量={}", task.getId(), localModels.size());

        // 创建全局模型记录
        GlobalModel globalModel = GlobalModel.builder()
                .id(UUID.randomUUID().toString().replace("-", ""))
                .taskId(task.getId())
                .roundNumber(task.getCurrentRound())
                .aggregationMethod("FEDPROX")
                .status("AGGREGATING")
                .participantCount(localModels.size())
                .startedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        // 执行聚合
        return engine.aggregate("FEDPROX", localModels, task, globalModel);
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

        // 验证μ参数
        double mu = extractMuParameter(task);
        AggregationConfig.FedProxConfig fedProxConfig = aggregationConfig.getFedProx();
        
        if (mu < fedProxConfig.getMinMu() || mu > fedProxConfig.getMaxMu()) {
            return ValidationResult.invalid(
                    String.format("FedProx的μ参数超出范围: %.4f, 有效范围: [%.4f, %.4f]", 
                            mu, fedProxConfig.getMinMu(), fedProxConfig.getMaxMu()));
        }

        // 验证所有模型都有参数
        for (VmRoundModel model : localModels) {
            if (model.getParameters() == null || model.getParameters().trim().isEmpty()) {
                return ValidationResult.invalid(
                        String.format("客户端 %s 的模型参数为空", model.getVmId()));
            }
        }

        // 对于FedProx，建议有服务器端参数（虽然不是必需的）
        if (task.getCurrentRound() > 0) {
            log.info("FedProx在轮次{}中执行，将使用正则化项", task.getCurrentRound());
        }

        return ValidationResult.valid();
    }

    @Override
    public AlgorithmConfigSuggestion getConfigSuggestion(FederatedTask task, int participantCount) {
        Map<String, Object> suggestedParams = new HashMap<>();
        AggregationConfig.FedProxConfig fedProxConfig = aggregationConfig.getFedProx();
        
        // 基于参与者数量和任务特性建议μ值
        double suggestedMu = calculateSuggestedMu(task, participantCount);
        suggestedParams.put("mu", suggestedMu);
        suggestedParams.put("enableAdaptiveMu", fedProxConfig.isEnableAdaptiveMu());
        
        // 继承FedAvg的基础配置
        AggregationConfig.FedAvgConfig fedAvgConfig = aggregationConfig.getFedAvg();
        suggestedParams.put("enableWeightNormalization", fedAvgConfig.isEnableWeightNormalization());
        suggestedParams.put("decimalScale", fedAvgConfig.getDecimalScale());

        // 并行处理建议
        if (participantCount > 8) {
            suggestedParams.put("enableParallelProcessing", true);
        }

        String reasoning = String.format(
                "基于%d个参与者的FedProx配置建议。推荐μ=%.4f，该值在数据异构性和收敛速度间取得平衡。", 
                participantCount, suggestedMu);

        if (fedProxConfig.isEnableAdaptiveMu()) {
            reasoning += "启用自适应μ调整以动态优化正则化强度。";
        }

        return new AlgorithmConfigSuggestion("FEDPROX", suggestedParams, reasoning);
    }

    /**
     * 提取任务配置中的μ参数
     */
    private double extractMuParameter(FederatedTask task) {
        try {
            if (task.getConfig() != null) {
                Map<String, Object> config = objectMapper.readValue(task.getConfig(), 
                        new TypeReference<Map<String, Object>>() {});
                Object muValue = config.get("mu");
                if (muValue instanceof Number) {
                    return ((Number) muValue).doubleValue();
                }
            }
        } catch (Exception e) {
            log.debug("无法从任务配置中提取μ参数，使用默认值: {}", e.getMessage());
        }
        
        return aggregationConfig.getFedProx().getDefaultMu();
    }

    /**
     * 根据任务特性和参与者数量计算建议的μ值
     */
    private double calculateSuggestedMu(FederatedTask task, int participantCount) {
        AggregationConfig.FedProxConfig config = aggregationConfig.getFedProx();
        double baseMu = config.getDefaultMu();

        // 参与者越多，建议μ稍小（减少正则化强度）
        if (participantCount > 20) {
            baseMu *= 0.8;
        } else if (participantCount > 10) {
            baseMu *= 0.9;
        }

        // 根据训练轮数调整（初期轮次μ可以稍大）
        if (task.getCurrentRound() != null && task.getCurrentRound() < 5) {
            baseMu *= 1.2;
        }

        // 确保在有效范围内
        return Math.max(config.getMinMu(), Math.min(config.getMaxMu(), baseMu));
    }
}