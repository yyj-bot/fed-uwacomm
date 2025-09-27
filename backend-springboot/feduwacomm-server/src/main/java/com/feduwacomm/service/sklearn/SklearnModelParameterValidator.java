package com.feduwacomm.service.sklearn;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Sklearn模型参数验证器
 * 提供统一的参数格式验证功能
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SklearnModelParameterValidator {

    private final SklearnModelParameterGeneratorFactory generatorFactory;

    /**
     * 验证模型参数的基本格式
     *
     * @param parameters 参数对象
     * @return 验证结果
     */
    public ValidationResult validateBasicFormat(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return ValidationResult.failure("参数对象为空");
        }

        // 验证顶级必需字段
        if (!parameters.containsKey("model_metadata")) {
            return ValidationResult.failure("缺少 model_metadata 字段");
        }

        if (!parameters.containsKey("parameters")) {
            return ValidationResult.failure("缺少 parameters 字段");
        }

        // 验证 model_metadata 结构
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) parameters.get("model_metadata");
        ValidationResult metadataResult = validateModelMetadata(metadata);
        if (!metadataResult.isValid()) {
            return metadataResult;
        }

        return ValidationResult.success();
    }

    /**
     * 验证特定模型类型的参数
     *
     * @param modelType 模型类型
     * @param parameters 参数对象
     * @return 验证结果
     */
    public ValidationResult validateModelSpecificParameters(String modelType, Map<String, Object> parameters) {
        // 首先验证基本格式
        ValidationResult basicResult = validateBasicFormat(parameters);
        if (!basicResult.isValid()) {
            return basicResult;
        }

        // 使用对应的生成器进行模型特定验证
        boolean isValid = generatorFactory.validateParameters(modelType, parameters);
        if (!isValid) {
            return ValidationResult.failure("模型特定参数验证失败: " + modelType);
        }

        return ValidationResult.success();
    }

    /**
     * 完整验证
     *
     * @param modelType 模型类型
     * @param parameters 参数对象
     * @return 验证结果
     */
    public ValidationResult validate(String modelType, Map<String, Object> parameters) {
        log.debug("开始验证sklearn模型参数: modelType={}", modelType);

        // 检查模型类型支持
        if (!generatorFactory.isSupported(modelType)) {
            return ValidationResult.failure("不支持的模型类型: " + modelType);
        }

        // 执行完整验证
        ValidationResult result = validateModelSpecificParameters(modelType, parameters);

        log.debug("sklearn模型参数验证结果: modelType={}, valid={}, message={}",
                modelType, result.isValid(), result.getMessage());

        return result;
    }

    /**
     * 验证model_metadata结构
     */
    private ValidationResult validateModelMetadata(Map<String, Object> metadata) {
        if (metadata == null) {
            return ValidationResult.failure("model_metadata不能为空");
        }

        // 验证必需字段
        String[] requiredFields = {"model_id", "model_type", "algorithm", "task_type", "created_at", "version"};
        for (String field : requiredFields) {
            if (!metadata.containsKey(field) || metadata.get(field) == null) {
                return ValidationResult.failure("model_metadata缺少必需字段: " + field);
            }
        }

        // 验证model_type必须是sklearn
        if (!"sklearn".equals(metadata.get("model_type"))) {
            return ValidationResult.failure("model_type必须为'sklearn'");
        }

        // 验证task_type
        String taskType = (String) metadata.get("task_type");
        if (!"regression".equals(taskType) && !"classification".equals(taskType)) {
            return ValidationResult.failure("task_type必须为'regression'或'classification'");
        }

        return ValidationResult.success();
    }

    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return valid ? "ValidationResult{valid=true}" :
                   "ValidationResult{valid=false, message='" + message + "'}";
        }
    }
}