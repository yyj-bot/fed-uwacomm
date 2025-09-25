package com.feduwacomm.service.sklearn;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.IntStream;

/**
 * RandomForest模型参数生成器
 * 生成符合sklearn-initial-model-guide.md规范的RandomForest参数
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class RandomForestParameterGenerator implements SklearnModelParameterGenerator {

    private static final Random RANDOM = new SecureRandom();
    private static final String MODEL_TYPE = "RANDOM_FOREST";

    // RandomForest默认参数范围
    private static final int DEFAULT_N_ESTIMATORS = 100;
    private static final int MIN_N_ESTIMATORS = 10;
    private static final int MAX_N_ESTIMATORS = 500;
    private static final int DEFAULT_N_FEATURES = 5;

    @Override
    public Map<String, Object> generateParameters(String modelId, String taskId,
                                                Map<String, Object> architectureParams,
                                                String generationMethod, String createdBy) {

        log.info("生成RandomForest模型参数: modelId={}, taskId={}", modelId, taskId);

        try {
            // 解析架构参数
            int nEstimators = parseNEstimators(architectureParams);
            int nFeatures = parseNFeatures(architectureParams);
            String taskType = parseTaskType(architectureParams);

            // 构建标准JSON格式
            Map<String, Object> result = new HashMap<>();

            // 1. model_metadata
            result.put("model_metadata", createModelMetadata(modelId, taskType));

            // 2. parameters (符合ModelWrapper.get_parameters()格式)
            result.put("parameters", createParameters(nEstimators, nFeatures));

            // 3. training_config
            result.put("training_config", createTrainingConfig(nFeatures));

            // 4. metadata
            result.put("metadata", createGenerationMetadata(generationMethod));

            log.info("RandomForest参数生成完成: modelId={}, n_estimators={}, n_features={}",
                    modelId, nEstimators, nFeatures);

            return result;

        } catch (Exception e) {
            log.error("RandomForest参数生成失败: modelId={}, error={}", modelId, e.getMessage(), e);
            throw new RuntimeException("模型参数生成失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean validateParameters(Map<String, Object> parameters) {
        log.debug("验证RandomForest参数格式");

        try {
            // 验证必需字段存在
            if (!parameters.containsKey("model_metadata") ||
                !parameters.containsKey("parameters") ||
                !parameters.containsKey("training_config")) {
                log.warn("缺少必需字段");
                return false;
            }

            // 验证model_metadata
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) parameters.get("model_metadata");
            if (!"sklearn".equals(metadata.get("model_type")) ||
                !"RandomForest".equals(metadata.get("algorithm"))) {
                log.warn("model_metadata格式不正确");
                return false;
            }

            // 验证parameters
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) parameters.get("parameters");
            if (!params.containsKey("feature_importances_") ||
                !params.containsKey("n_estimators")) {
                log.warn("缺少核心参数");
                return false;
            }

            // 验证feature_importances_
            @SuppressWarnings("unchecked")
            List<Double> importances = (List<Double>) params.get("feature_importances_");
            if (importances == null || importances.isEmpty()) {
                log.warn("feature_importances_为空");
                return false;
            }

            // 验证重要性总和接近1.0
            double sum = importances.stream().mapToDouble(Double::doubleValue).sum();
            if (Math.abs(sum - 1.0) > 0.01) {
                log.warn("feature_importances_总和不接近1.0: {}", sum);
                return false;
            }

            // 验证n_estimators
            Object nEstimators = params.get("n_estimators");
            if (!(nEstimators instanceof Integer) || (Integer) nEstimators <= 0) {
                log.warn("n_estimators格式不正确");
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("参数验证异常: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getSupportedModelType() {
        return MODEL_TYPE;
    }

    /**
     * 创建model_metadata
     */
    private Map<String, Object> createModelMetadata(String modelId, String taskType) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("model_id", modelId);
        metadata.put("model_type", "sklearn");
        metadata.put("algorithm", "RandomForest");
        metadata.put("task_type", taskType);
        metadata.put("created_at", LocalDateTime.now()
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_INSTANT));
        metadata.put("version", "1.0.0");
        return metadata;
    }

    /**
     * 创建parameters (符合ModelWrapper规范)
     */
    private Map<String, Object> createParameters(int nEstimators, int nFeatures) {
        Map<String, Object> parameters = new HashMap<>();

        // 生成特征重要性 (总和接近1.0)
        List<Double> featureImportances = generateFeatureImportances(nFeatures);
        parameters.put("feature_importances_", featureImportances);

        // 设置树的数量
        parameters.put("n_estimators", nEstimators);

        return parameters;
    }

    /**
     * 创建training_config
     */
    private Map<String, Object> createTrainingConfig(int nFeatures) {
        Map<String, Object> config = new HashMap<>();

        // 生成特征名称
        List<String> featureNames = IntStream.range(0, nFeatures)
                .mapToObj(i -> "feature_" + i)
                .toList();

        config.put("feature_names", featureNames);
        config.put("n_features", nFeatures);

        return config;
    }

    /**
     * 创建generation metadata
     */
    private Map<String, Object> createGenerationMetadata(String generationMethod) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("generation_method", generationMethod);
        metadata.put("generation_timestamp", System.currentTimeMillis());
        return metadata;
    }

    /**
     * 生成特征重要性数组 (总和接近1.0)
     */
    private List<Double> generateFeatureImportances(int nFeatures) {
        // 生成随机权重
        double[] weights = new double[nFeatures];
        for (int i = 0; i < nFeatures; i++) {
            weights[i] = RANDOM.nextDouble();
        }

        // 归一化使总和为1.0
        double sum = Arrays.stream(weights).sum();
        List<Double> importances = new ArrayList<>();
        for (double weight : weights) {
            importances.add(weight / sum);
        }

        return importances;
    }

    /**
     * 解析n_estimators参数
     */
    private int parseNEstimators(Map<String, Object> params) {
        if (params == null) {
            return DEFAULT_N_ESTIMATORS;
        }

        Object value = params.get("n_estimators");
        if (value instanceof Integer) {
            int nEstimators = (Integer) value;
            return Math.max(MIN_N_ESTIMATORS, Math.min(MAX_N_ESTIMATORS, nEstimators));
        }

        return DEFAULT_N_ESTIMATORS;
    }

    /**
     * 解析特征数量
     */
    private int parseNFeatures(Map<String, Object> params) {
        if (params == null) {
            return DEFAULT_N_FEATURES;
        }

        Object value = params.get("n_features");
        if (value instanceof Integer) {
            return Math.max(1, (Integer) value);
        }

        return DEFAULT_N_FEATURES;
    }

    /**
     * 解析任务类型
     */
    private String parseTaskType(Map<String, Object> params) {
        if (params == null) {
            return "regression";
        }

        Object value = params.get("task_type");
        if (value instanceof String) {
            String taskType = ((String) value).toLowerCase();
            if ("classification".equals(taskType) || "regression".equals(taskType)) {
                return taskType;
            }
        }

        return "regression"; // 默认回归任务
    }
}