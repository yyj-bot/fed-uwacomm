package com.feduwacomm.service;

import com.feduwacomm.algorithm.FederatedAlgorithms;
import com.feduwacomm.entity.FederatedTask;
import com.feduwacomm.entity.GlobalModel;
import com.feduwacomm.entity.VmRoundModel;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模型聚合引擎
 * 负责调度不同的联邦学习聚合算法，处理模型参数和指标聚合
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelAggregatorEngine {

    private final ObjectMapper objectMapper;

    /**
     * 执行模型聚合
     *
     * @param algorithm    聚合算法类型
     * @param localModels  本地模型列表
     * @param task         任务信息
     * @param globalModel  全局模型记录（用于存储聚合结果）
     * @return 聚合结果
     */
    public AggregationResult aggregate(String algorithm, 
                                       List<VmRoundModel> localModels, 
                                       FederatedTask task,
                                       GlobalModel globalModel) {

        if (localModels == null || localModels.isEmpty()) {
            throw new IllegalArgumentException("本地模型列表不能为空");
        }

        log.info("开始模型聚合: 算法={}, 模型数量={}, 任务ID={}", 
                algorithm, localModels.size(), task.getId());

        long startTime = System.currentTimeMillis();
        
        try {
            // 解析模型参数和指标
            AggregationInput input = prepareAggregationInput(localModels);
            
            // 执行对应的聚合算法
            Map<String, Object> aggregatedParams = executeAggregationAlgorithm(
                    algorithm, input, task);
            
            // 计算全局指标
            Map<String, BigDecimal> globalMetrics = FederatedAlgorithms.calculateGlobalMetrics(
                    input.getClientMetrics(), input.getWeights());
            
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("模型聚合完成: 算法={}, 耗时={}ms, 参数数量={}, 指标数量={}", 
                    algorithm, duration, aggregatedParams.size(), globalMetrics.size());

            return AggregationResult.builder()
                    .success(true)
                    .globalParameters(aggregatedParams)
                    .globalMetrics(globalMetrics)
                    .participantCount(localModels.size())
                    .aggregationDuration(duration)
                    .algorithm(algorithm)
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("模型聚合失败: 算法={}, 错误={}, 耗时={}ms", 
                    algorithm, e.getMessage(), duration, e);
            
            return AggregationResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .participantCount(localModels.size())
                    .aggregationDuration(duration)
                    .algorithm(algorithm)
                    .build();
        }
    }

    /**
     * 准备聚合输入数据
     */
    private AggregationInput prepareAggregationInput(List<VmRoundModel> localModels) {
        List<Map<String, Object>> clientParameters = new ArrayList<>();
        List<Map<String, Object>> clientMetrics = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        List<String> vmIds = new ArrayList<>();

        for (VmRoundModel model : localModels) {
            try {
                // 解析模型参数JSON
                Map<String, Object> modelData = parseModelParameters(model.getParameters());
                
                // 提取参数和指标
                Map<String, Object> parameters = (Map<String, Object>) modelData.get("parameters");
                Map<String, Object> metrics = (Map<String, Object>) modelData.get("metrics");
                
                if (parameters != null) {
                    clientParameters.add(parameters);
                }
                
                if (metrics != null) {
                    clientMetrics.add(metrics);
                }
                
                // 使用样本数量作为权重，如果没有则使用默认权重1.0
                Double sampleCount = extractSampleCount(metrics);
                weights.add(sampleCount != null ? sampleCount : 1.0);
                
                vmIds.add(model.getVmId());
                
            } catch (Exception e) {
                log.warn("解析模型参数失败，跳过该模型: vmId={}, error={}", 
                        model.getVmId(), e.getMessage());
            }
        }

        if (clientParameters.isEmpty()) {
            throw new IllegalArgumentException("没有有效的客户端参数可用于聚合");
        }

        return AggregationInput.builder()
                .clientParameters(clientParameters)
                .clientMetrics(clientMetrics)
                .weights(weights)
                .vmIds(vmIds)
                .build();
    }

    /**
     * 执行具体的聚合算法
     */
    private Map<String, Object> executeAggregationAlgorithm(String algorithm, 
                                                           AggregationInput input, 
                                                           FederatedTask task) {
        
        switch (algorithm.toUpperCase()) {
            case "FEDAVG":
                return FederatedAlgorithms.fedAvgAggregate(
                        input.getClientParameters(), 
                        input.getWeights());
                
            case "FEDPROX":
                // 获取服务器端参数（上一轮的全局模型）
                Map<String, Object> serverParameters = getServerParameters(task);
                double mu = extractMuParameter(task);
                return FederatedAlgorithms.fedProxAggregate(
                        input.getClientParameters(),
                        serverParameters,
                        input.getWeights(),
                        mu);
                
            case "FEDNOVA":
                List<Integer> localSteps = extractLocalSteps(input.getClientMetrics());
                return FederatedAlgorithms.fedNovaAggregate(
                        input.getClientParameters(),
                        localSteps,
                        input.getWeights());
                
            default:
                log.warn("不支持的聚合算法: {}, 使用默认FedAvg", algorithm);
                return FederatedAlgorithms.fedAvgAggregate(
                        input.getClientParameters(), 
                        input.getWeights());
        }
    }

    /**
     * 解析模型参数JSON
     */
    private Map<String, Object> parseModelParameters(String parametersJson) {
        try {
            if (parametersJson == null || parametersJson.trim().isEmpty()) {
                return new HashMap<>();
            }
            
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
            return objectMapper.readValue(parametersJson, typeRef);
            
        } catch (Exception e) {
            log.warn("解析模型参数JSON失败: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * 提取样本数量（用作权重）
     */
    private Double extractSampleCount(Map<String, Object> metrics) {
        if (metrics == null) {
            return null;
        }
        
        // 尝试多种可能的键名
        String[] sampleCountKeys = {"sample_count", "sampleCount", "samples", "data_size", "dataSize"};
        
        for (String key : sampleCountKeys) {
            Object value = metrics.get(key);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
        }
        
        return null;
    }

    /**
     * 获取服务器端参数（FedProx使用）
     */
    private Map<String, Object> getServerParameters(FederatedTask task) {
        // 这里应该获取上一轮的全局模型参数
        // 暂时返回空Map，实际实现中需要查询GlobalModel表
        return new HashMap<>();
    }

    /**
     * 提取FedProx的μ参数
     */
    private double extractMuParameter(FederatedTask task) {
        // 从任务配置中提取μ参数，默认值0.01
        try {
            String config = task.getConfig();
            if (config != null) {
                Map<String, Object> configMap = objectMapper.readValue(config, 
                        new TypeReference<Map<String, Object>>() {});
                Object mu = configMap.get("mu");
                if (mu instanceof Number) {
                    return ((Number) mu).doubleValue();
                }
            }
        } catch (Exception e) {
            log.warn("提取μ参数失败，使用默认值: {}", e.getMessage());
        }
        
        return 0.01; // 默认值
    }

    /**
     * 提取本地训练步数（FedNova使用）
     */
    private List<Integer> extractLocalSteps(List<Map<String, Object>> clientMetrics) {
        List<Integer> localSteps = new ArrayList<>();
        
        for (Map<String, Object> metrics : clientMetrics) {
            Integer steps = extractLocalStepsFromMetrics(metrics);
            localSteps.add(steps != null ? steps : 1); // 默认步数为1
        }
        
        return localSteps;
    }

    /**
     * 从指标中提取本地训练步数
     */
    private Integer extractLocalStepsFromMetrics(Map<String, Object> metrics) {
        if (metrics == null) {
            return null;
        }
        
        String[] stepKeys = {"local_steps", "localSteps", "epochs", "iterations"};
        
        for (String key : stepKeys) {
            Object value = metrics.get(key);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        }
        
        return null;
    }

    /**
     * 聚合输入数据结构
     */
    public static class AggregationInput {
        private final List<Map<String, Object>> clientParameters;
        private final List<Map<String, Object>> clientMetrics;
        private final List<Double> weights;
        private final List<String> vmIds;

        private AggregationInput(Builder builder) {
            this.clientParameters = builder.clientParameters;
            this.clientMetrics = builder.clientMetrics;
            this.weights = builder.weights;
            this.vmIds = builder.vmIds;
        }

        public static Builder builder() {
            return new Builder();
        }

        public List<Map<String, Object>> getClientParameters() { return clientParameters; }
        public List<Map<String, Object>> getClientMetrics() { return clientMetrics; }
        public List<Double> getWeights() { return weights; }
        public List<String> getVmIds() { return vmIds; }

        public static class Builder {
            private List<Map<String, Object>> clientParameters;
            private List<Map<String, Object>> clientMetrics;
            private List<Double> weights;
            private List<String> vmIds;

            public Builder clientParameters(List<Map<String, Object>> clientParameters) {
                this.clientParameters = clientParameters;
                return this;
            }

            public Builder clientMetrics(List<Map<String, Object>> clientMetrics) {
                this.clientMetrics = clientMetrics;
                return this;
            }

            public Builder weights(List<Double> weights) {
                this.weights = weights;
                return this;
            }

            public Builder vmIds(List<String> vmIds) {
                this.vmIds = vmIds;
                return this;
            }

            public AggregationInput build() {
                return new AggregationInput(this);
            }
        }
    }

    /**
     * 聚合结果数据结构
     */
    public static class AggregationResult {
        private final boolean success;
        private final String errorMessage;
        private final Map<String, Object> globalParameters;
        private final Map<String, BigDecimal> globalMetrics;
        private final int participantCount;
        private final long aggregationDuration;
        private final String algorithm;

        private AggregationResult(Builder builder) {
            this.success = builder.success;
            this.errorMessage = builder.errorMessage;
            this.globalParameters = builder.globalParameters;
            this.globalMetrics = builder.globalMetrics;
            this.participantCount = builder.participantCount;
            this.aggregationDuration = builder.aggregationDuration;
            this.algorithm = builder.algorithm;
        }

        public static Builder builder() {
            return new Builder();
        }

        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public Map<String, Object> getGlobalParameters() { return globalParameters; }
        public Map<String, BigDecimal> getGlobalMetrics() { return globalMetrics; }
        public int getParticipantCount() { return participantCount; }
        public long getAggregationDuration() { return aggregationDuration; }
        public String getAlgorithm() { return algorithm; }

        public static class Builder {
            private boolean success;
            private String errorMessage;
            private Map<String, Object> globalParameters;
            private Map<String, BigDecimal> globalMetrics;
            private int participantCount;
            private long aggregationDuration;
            private String algorithm;

            public Builder success(boolean success) {
                this.success = success;
                return this;
            }

            public Builder errorMessage(String errorMessage) {
                this.errorMessage = errorMessage;
                return this;
            }

            public Builder globalParameters(Map<String, Object> globalParameters) {
                this.globalParameters = globalParameters;
                return this;
            }

            public Builder globalMetrics(Map<String, BigDecimal> globalMetrics) {
                this.globalMetrics = globalMetrics;
                return this;
            }

            public Builder participantCount(int participantCount) {
                this.participantCount = participantCount;
                return this;
            }

            public Builder aggregationDuration(long aggregationDuration) {
                this.aggregationDuration = aggregationDuration;
                return this;
            }

            public Builder algorithm(String algorithm) {
                this.algorithm = algorithm;
                return this;
            }

            public AggregationResult build() {
                return new AggregationResult(this);
            }
        }
    }
}