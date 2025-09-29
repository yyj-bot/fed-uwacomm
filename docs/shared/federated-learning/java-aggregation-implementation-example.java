/**
 * 联邦学习梯度聚合算法 Java 实现示例
 *
 * 本文件展示了在 FedUWAComm Spring Boot 系统中实现 RandomForest 联邦学习聚合算法的具体代码。
 * 这些算法专门针对 Scikit-learn 框架的树模型进行了优化。
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-25
 */
package com.feduwacomm.service.federated;

import com.feduwacomm.common.result.Result;
import com.feduwacomm.pojo.dto.ClientUpdateDTO;
import com.feduwacomm.pojo.dto.GlobalModelDTO;
import com.feduwacomm.pojo.dto.ModelParametersDTO;
import com.feduwacomm.pojo.dto.TrainingMetadataDTO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 聚合方法枚举
 */
enum AggregationMethod {
    FEDAVG_RF("FedAvg-RF"),
    FEDPROX_RF("FedProx-RF"),
    ADAPTIVE_RF("Adaptive-RF");

    private final String value;

    AggregationMethod(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

/**
 * 聚合结果数据传输对象
 */
@Data
class AggregationResultDTO {
    private List<Double> featureImportances;
    private Integer nEstimators;
    private String aggregationMethod;
    private Integer participants;
    private List<Double> weightsUsed;
    private Double regularizationMu;
    private List<Double> adaptiveWeights;
    private List<Double> uncertaintyScores;
    private List<Double> performanceScores;
}

/**
 * RandomForest 联邦学习聚合器
 */
@Slf4j
@Service
class RandomForestFederatedAggregator {

    private final int minParticipants;

    public RandomForestFederatedAggregator() {
        this.minParticipants = 3;
    }

    /**
     * FedAvg 聚合算法实现
     *
     * @param clientUpdates 客户端更新列表
     * @return 聚合结果
     */
    public AggregationResultDTO fedAvgAggregate(List<ClientUpdateDTO> clientUpdates) {
        if (clientUpdates.size() < minParticipants) {
            throw new IllegalArgumentException(
                String.format("参与者数量不足，需要至少 %d 个", minParticipants));
        }

        // 1. 计算权重（基于样本数量）
        long totalSamples = clientUpdates.stream()
            .mapToLong(update -> update.getTrainingMetadata().getSamplesCount())
            .sum();

        List<Double> weights = clientUpdates.stream()
            .map(update -> (double) update.getTrainingMetadata().getSamplesCount() / totalSamples)
            .collect(Collectors.toList());

        // 2. 获取特征数量并验证一致性
        int nFeatures = clientUpdates.get(0).getModelParameters().getFeatureImportances().size();
        for (ClientUpdateDTO update : clientUpdates) {
            if (update.getModelParameters().getFeatureImportances().size() != nFeatures) {
                throw new IllegalArgumentException(
                    String.format("客户端 %s 的特征数量不匹配", update.getClientId()));
            }
        }

        // 3. 加权平均聚合特征重要性
        List<Double> globalImportances = new ArrayList<>(Collections.nCopies(nFeatures, 0.0));

        for (int i = 0; i < clientUpdates.size(); i++) {
            List<Double> importanceVector = clientUpdates.get(i).getModelParameters().getFeatureImportances();
            double weight = weights.get(i);

            for (int j = 0; j < nFeatures; j++) {
                globalImportances.set(j, globalImportances.get(j) + weight * importanceVector.get(j));
            }
        }

        // 4. 归一化特征重要性
        double totalImportance = globalImportances.stream().mapToDouble(Double::doubleValue).sum();
        if (totalImportance > 0) {
            globalImportances = globalImportances.stream()
                .map(imp -> imp / totalImportance)
                .collect(Collectors.toList());
        } else {
            // 如果所有重要性都为0，设置为均匀分布
            double uniformValue = 1.0 / nFeatures;
            globalImportances = Collections.nCopies(nFeatures, uniformValue);
        }

        log.info("FedAvg聚合完成，参与者: {}", clientUpdates.size());

        AggregationResultDTO result = new AggregationResultDTO();
        result.setFeatureImportances(globalImportances);
        result.setNEstimators(clientUpdates.get(0).getModelParameters().getNEstimators());
        result.setAggregationMethod(AggregationMethod.FEDAVG_RF.getValue());
        result.setParticipants(clientUpdates.size());
        result.setWeightsUsed(weights);

        return result;
    }

    /**
     * FedProx 聚合算法实现
     *
     * @param clientUpdates 客户端更新列表
     * @param globalModel 上一轮的全局模型
     * @param mu 正则化系数
     * @return 聚合结果
     */
    public AggregationResultDTO fedProxAggregate(List<ClientUpdateDTO> clientUpdates,
                                               GlobalModelDTO globalModel,
                                               double mu) {
        // 1. 首先执行 FedAvg 聚合
        AggregationResultDTO fedavgResult = fedAvgAggregate(clientUpdates);

        // 2. 如果有上一轮的全局模型，应用 FedProx 正则化
        if (globalModel != null && globalModel.getParameters() != null) {
            List<Double> globalImportances = globalModel.getParameters().getFeatureImportances();
            List<Double> fedavgImportances = fedavgResult.getFeatureImportances();

            if (globalImportances != null && globalImportances.size() == fedavgImportances.size()) {
                // 3. FedProx 公式：θ_new = (1-μ) * θ_fedavg + μ * θ_global
                List<Double> regularizedImportances = new ArrayList<>();
                for (int i = 0; i < fedavgImportances.size(); i++) {
                    double regularizedValue = (1 - mu) * fedavgImportances.get(i) +
                                            mu * globalImportances.get(i);
                    regularizedImportances.add(regularizedValue);
                }

                // 4. 重新归一化
                double totalImportance = regularizedImportances.stream()
                    .mapToDouble(Double::doubleValue).sum();
                if (totalImportance > 0) {
                    regularizedImportances = regularizedImportances.stream()
                        .map(imp -> imp / totalImportance)
                        .collect(Collectors.toList());
                }

                fedavgResult.setFeatureImportances(regularizedImportances);
                fedavgResult.setAggregationMethod(AggregationMethod.FEDPROX_RF.getValue());
                fedavgResult.setRegularizationMu(mu);
            }
        }

        log.info("FedProx聚合完成，正则化系数: {}", mu);
        return fedavgResult;
    }

    /**
     * 自适应聚合算法实现
     *
     * @param clientUpdates 客户端更新列表
     * @return 聚合结果
     */
    public AggregationResultDTO adaptiveAggregate(List<ClientUpdateDTO> clientUpdates) {
        // 1. 计算每个客户端的模型不确定性和性能权重
        List<Double> uncertainties = new ArrayList<>();
        List<Double> performanceWeights = new ArrayList<>();

        for (ClientUpdateDTO update : clientUpdates) {
            // 特征重要性的方差作为不确定性度量
            List<Double> importances = update.getModelParameters().getFeatureImportances();
            double mean = importances.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double variance = importances.stream()
                .mapToDouble(imp -> Math.pow(imp - mean, 2))
                .average()
                .orElse(0.0);

            double uncertainty = 1.0 / (1.0 + variance); // 方差越小，权重越大
            uncertainties.add(uncertainty);

            // 考虑本地准确率
            double accuracyWeight = update.getTrainingMetadata().getLocalAccuracy();
            performanceWeights.add(accuracyWeight);
        }

        // 2. 组合不确定性权重和性能权重
        double alpha = 0.6, beta = 0.4; // 权衡参数
        List<Double> combinedWeights = new ArrayList<>();
        for (int i = 0; i < clientUpdates.size(); i++) {
            double combinedWeight = alpha * uncertainties.get(i) + beta * performanceWeights.get(i);
            combinedWeights.add(combinedWeight);
        }

        // 3. 归一化权重
        double totalWeight = combinedWeights.stream().mapToDouble(Double::doubleValue).sum();
        List<Double> adaptiveWeights = combinedWeights.stream()
            .map(w -> w / totalWeight)
            .collect(Collectors.toList());

        // 4. 使用自适应权重进行聚合
        int nFeatures = clientUpdates.get(0).getModelParameters().getFeatureImportances().size();
        List<Double> globalImportances = new ArrayList<>(Collections.nCopies(nFeatures, 0.0));

        for (int i = 0; i < clientUpdates.size(); i++) {
            List<Double> importanceVector = clientUpdates.get(i).getModelParameters().getFeatureImportances();
            double weight = adaptiveWeights.get(i);

            for (int j = 0; j < nFeatures; j++) {
                globalImportances.set(j, globalImportances.get(j) + weight * importanceVector.get(j));
            }
        }

        // 5. 归一化
        double totalImportance = globalImportances.stream().mapToDouble(Double::doubleValue).sum();
        if (totalImportance > 0) {
            globalImportances = globalImportances.stream()
                .map(imp -> imp / totalImportance)
                .collect(Collectors.toList());
        } else {
            double uniformValue = 1.0 / nFeatures;
            globalImportances = Collections.nCopies(nFeatures, uniformValue);
        }

        log.info("自适应聚合完成，权重分布: {}", adaptiveWeights);

        AggregationResultDTO result = new AggregationResultDTO();
        result.setFeatureImportances(globalImportances);
        result.setNEstimators(clientUpdates.get(0).getModelParameters().getNEstimators());
        result.setAggregationMethod(AggregationMethod.ADAPTIVE_RF.getValue());
        result.setParticipants(clientUpdates.size());
        result.setAdaptiveWeights(adaptiveWeights);
        result.setUncertaintyScores(uncertainties);
        result.setPerformanceScores(performanceWeights);

        return result;
    }
}

/**
 * 联邦学习聚合服务
 */
@Slf4j
@Service
public class FederatedAggregationService {

    private final RandomForestFederatedAggregator aggregator;
    private final Map<String, List<ClientUpdateDTO>> taskUpdates;
    private final Map<String, GlobalModelDTO> globalModels;

    private AggregationMethod aggregationMethod;
    private int currentRound;

    public FederatedAggregationService(RandomForestFederatedAggregator aggregator) {
        this.aggregator = aggregator;
        this.taskUpdates = new ConcurrentHashMap<>();
        this.globalModels = new ConcurrentHashMap<>();
        this.aggregationMethod = AggregationMethod.FEDAVG_RF;
        this.currentRound = 0;
    }

    /**
     * 添加客户端更新
     *
     * @param taskId 任务ID
     * @param updateData 客户端更新数据
     * @return 是否添加成功
     */
    public boolean addClientUpdate(String taskId, Map<String, Object> updateData) {
        try {
            ClientUpdateDTO clientUpdate = parseClientUpdate(updateData);

            if (validateClientUpdate(clientUpdate)) {
                taskUpdates.computeIfAbsent(taskId, k -> new CopyOnWriteArrayList<>())
                          .add(clientUpdate);
                log.info("收到客户端更新: {} for task: {}", clientUpdate.getClientId(), taskId);
                return true;
            } else {
                log.warn("客户端更新验证失败: {} for task: {}",
                        clientUpdate.getClientId(), taskId);
                return false;
            }
        } catch (Exception e) {
            log.error("处理客户端更新时出错: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 聚合任务的所有客户端更新
     *
     * @param taskId 任务ID
     * @return 聚合结果
     */
    public Result<GlobalModelDTO> aggregateUpdates(String taskId) {
        List<ClientUpdateDTO> clientUpdates = taskUpdates.get(taskId);

        if (clientUpdates == null || clientUpdates.size() < 3) {
            return Result.error(String.format("任务 %s 的参与者数量不足: %d",
                               taskId, clientUpdates != null ? clientUpdates.size() : 0));
        }

        try {
            long startTime = System.currentTimeMillis();

            // 根据聚合方法执行聚合
            AggregationResultDTO aggregatedParams;
            GlobalModelDTO previousGlobalModel = globalModels.get(taskId);

            switch (aggregationMethod) {
                case FEDAVG_RF:
                    aggregatedParams = aggregator.fedAvgAggregate(clientUpdates);
                    break;
                case FEDPROX_RF:
                    aggregatedParams = aggregator.fedProxAggregate(
                        clientUpdates, previousGlobalModel, 0.1);
                    break;
                case ADAPTIVE_RF:
                    aggregatedParams = aggregator.adaptiveAggregate(clientUpdates);
                    break;
                default:
                    return Result.error("未知的聚合方法: " + aggregationMethod);
            }

            // 创建全局模型
            currentRound++;
            GlobalModelDTO globalModel = createGlobalModel(taskId, aggregatedParams);

            if (validateGlobalModel(globalModel)) {
                globalModels.put(taskId, globalModel);
                long aggregationTime = System.currentTimeMillis() - startTime;

                log.info("聚合完成 - 任务: {}, 轮次: {}, 参与者: {}, 用时: {}ms",
                        taskId, currentRound, clientUpdates.size(), aggregationTime);

                // 清空客户端更新缓存
                taskUpdates.remove(taskId);

                return Result.success(globalModel);
            } else {
                return Result.error("生成的全局模型验证失败");
            }

        } catch (Exception e) {
            log.error("聚合过程出错: {}", e.getMessage(), e);
            return Result.error("聚合过程出错: " + e.getMessage());
        }
    }

    /**
     * 获取用于WebSocket广播的全局模型格式
     *
     * @param taskId 任务ID
     * @return 广播消息格式
     */
    public Map<String, Object> getGlobalModelForBroadcast(String taskId) {
        GlobalModelDTO globalModel = globalModels.get(taskId);
        if (globalModel == null) {
            return null;
        }

        Map<String, Object> broadcast = new HashMap<>();
        broadcast.put("message_type", "GLOBAL_MODEL_UPDATE");
        broadcast.put("task_id", taskId);
        broadcast.put("round", globalModel.getRound());

        // 全局模型数据
        Map<String, Object> globalModelData = new HashMap<>();

        // 模型元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("model_id", globalModel.getModelId());
        metadata.put("model_type", "sklearn");
        metadata.put("algorithm", "RandomForest");
        metadata.put("created_at", globalModel.getCreatedAt());
        metadata.put("version", "round_" + globalModel.getRound());

        // 模型参数
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("feature_importances_", globalModel.getParameters().getFeatureImportances());
        parameters.put("n_estimators", globalModel.getParameters().getNEstimators());

        globalModelData.put("model_metadata", metadata);
        globalModelData.put("parameters", parameters);

        // 聚合信息
        Map<String, Object> aggregationInfo = new HashMap<>();
        aggregationInfo.put("method", globalModel.getAggregationMethod());
        aggregationInfo.put("participants", globalModel.getParticipants());

        Map<String, Object> convergenceMetrics = new HashMap<>();
        convergenceMetrics.put("parameter_change", calculateParameterChange(taskId));
        convergenceMetrics.put("improvement", calculateImprovement(taskId));
        aggregationInfo.put("convergence_metrics", convergenceMetrics);

        // 下一轮配置
        Map<String, Object> nextRoundConfig = new HashMap<>();
        nextRoundConfig.put("local_epochs", 5);
        nextRoundConfig.put("target_accuracy", 0.90);
        nextRoundConfig.put("max_training_time", 300);

        broadcast.put("global_model", globalModelData);
        broadcast.put("aggregation_info", aggregationInfo);
        broadcast.put("next_round_config", nextRoundConfig);

        return broadcast;
    }

    /**
     * 解析客户端更新数据
     */
    private ClientUpdateDTO parseClientUpdate(Map<String, Object> updateData) {
        ClientUpdateDTO clientUpdate = new ClientUpdateDTO();

        clientUpdate.setClientId((String) updateData.get("client_id"));
        clientUpdate.setTaskId((String) updateData.get("task_id"));
        clientUpdate.setRound((Integer) updateData.get("round"));
        clientUpdate.setTimestamp((String) updateData.get("timestamp"));

        // 解析训练结果
        @SuppressWarnings("unchecked")
        Map<String, Object> trainingResult = (Map<String, Object>) updateData.get("training_result");

        // 解析模型参数
        @SuppressWarnings("unchecked")
        Map<String, Object> modelParams = (Map<String, Object>) trainingResult.get("model_parameters");
        ModelParametersDTO parameters = new ModelParametersDTO();

        @SuppressWarnings("unchecked")
        List<Double> importances = (List<Double>) modelParams.get("feature_importances_");
        parameters.setFeatureImportances(importances);
        parameters.setNEstimators((Integer) modelParams.get("n_estimators"));

        clientUpdate.setModelParameters(parameters);

        // 解析训练元数据
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) trainingResult.get("training_metadata");
        TrainingMetadataDTO trainingMetadata = new TrainingMetadataDTO();

        trainingMetadata.setSamplesCount(((Number) metadata.get("samples_count")).longValue());
        trainingMetadata.setTrainingTime(((Number) metadata.get("training_time")).doubleValue());
        trainingMetadata.setLocalAccuracy(((Number) metadata.get("local_accuracy")).doubleValue());
        trainingMetadata.setConvergenceStatus((String) metadata.get("convergence_status"));

        clientUpdate.setTrainingMetadata(trainingMetadata);

        return clientUpdate;
    }

    /**
     * 验证客户端更新的有效性
     */
    private boolean validateClientUpdate(ClientUpdateDTO update) {
        try {
            // 检查特征重要性
            List<Double> importances = update.getModelParameters().getFeatureImportances();
            if (importances.stream().anyMatch(imp -> imp < 0)) {
                return false;
            }

            // 检查特征重要性总和
            double total = importances.stream().mapToDouble(Double::doubleValue).sum();
            if (Math.abs(total - 1.0) > 0.1) { // 允许10%的误差
                return false;
            }

            // 检查其他参数
            if (update.getModelParameters().getNEstimators() <= 0) {
                return false;
            }

            if (update.getTrainingMetadata().getSamplesCount() <= 0) {
                return false;
            }

            double accuracy = update.getTrainingMetadata().getLocalAccuracy();
            if (accuracy < 0 || accuracy > 1) {
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("验证客户端更新时出错: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 创建全局模型
     */
    private GlobalModelDTO createGlobalModel(String taskId, AggregationResultDTO aggregatedParams) {
        GlobalModelDTO globalModel = new GlobalModelDTO();

        globalModel.setModelId(String.format("global_model_%s_round_%d", taskId, currentRound));
        globalModel.setTaskId(taskId);
        globalModel.setRound(currentRound);
        globalModel.setAggregationMethod(aggregatedParams.getAggregationMethod());
        globalModel.setParticipants(aggregatedParams.getParticipants());
        globalModel.setCreatedAt(LocalDateTime.now());

        ModelParametersDTO parameters = new ModelParametersDTO();
        parameters.setFeatureImportances(aggregatedParams.getFeatureImportances());
        parameters.setNEstimators(aggregatedParams.getNEstimators());

        globalModel.setParameters(parameters);

        return globalModel;
    }

    /**
     * 验证全局模型的有效性
     */
    private boolean validateGlobalModel(GlobalModelDTO model) {
        try {
            // 检查特征重要性
            List<Double> importances = model.getParameters().getFeatureImportances();
            if (importances.stream().anyMatch(imp -> imp < 0)) {
                return false;
            }

            // 检查归一化
            double total = importances.stream().mapToDouble(Double::doubleValue).sum();
            if (Math.abs(total - 1.0) > 0.01) {
                return false;
            }

            // 检查模型结构
            if (model.getParameters().getNEstimators() <= 0) {
                return false;
            }

            if (model.getParticipants() < 1) {
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("验证全局模型时出错: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 计算参数变化程度
     */
    private double calculateParameterChange(String taskId) {
        // 实际实现中应该与上一轮模型比较
        // 简化实现，返回固定值
        return 0.023;
    }

    /**
     * 计算模型改进程度
     */
    private double calculateImprovement(String taskId) {
        // 实际实现中应该基于验证集性能计算
        // 简化实现，返回固定值
        return 0.012;
    }

    // Getter 和 Setter 方法
    public AggregationMethod getAggregationMethod() {
        return aggregationMethod;
    }

    public void setAggregationMethod(AggregationMethod aggregationMethod) {
        this.aggregationMethod = aggregationMethod;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public Map<String, GlobalModelDTO> getGlobalModels() {
        return new HashMap<>(globalModels);
    }
}