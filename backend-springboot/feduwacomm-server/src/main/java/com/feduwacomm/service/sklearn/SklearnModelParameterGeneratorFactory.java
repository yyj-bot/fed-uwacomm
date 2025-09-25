package com.feduwacomm.service.sklearn;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Sklearn模型参数生成器工厂
 * 根据模型类型返回对应的参数生成器
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SklearnModelParameterGeneratorFactory {

    private final List<SklearnModelParameterGenerator> generators;
    private Map<String, SklearnModelParameterGenerator> generatorMap;

    /**
     * 初始化生成器映射
     */
    private void initializeGeneratorMap() {
        if (generatorMap == null) {
            generatorMap = generators.stream()
                    .collect(Collectors.toMap(
                            SklearnModelParameterGenerator::getSupportedModelType,
                            Function.identity()
                    ));
            log.info("初始化sklearn参数生成器: {}", generatorMap.keySet());
        }
    }

    /**
     * 获取指定模型类型的参数生成器
     *
     * @param modelType 模型类型
     * @return 参数生成器，如果不支持则返回null
     */
    public SklearnModelParameterGenerator getGenerator(String modelType) {
        initializeGeneratorMap();

        SklearnModelParameterGenerator generator = generatorMap.get(modelType);
        if (generator == null) {
            log.warn("不支持的模型类型: {}, 支持的类型: {}", modelType, generatorMap.keySet());
        }

        return generator;
    }

    /**
     * 检查是否支持指定的模型类型
     *
     * @param modelType 模型类型
     * @return 是否支持
     */
    public boolean isSupported(String modelType) {
        initializeGeneratorMap();
        return generatorMap.containsKey(modelType);
    }

    /**
     * 获取所有支持的模型类型
     *
     * @return 支持的模型类型列表
     */
    public List<String> getSupportedModelTypes() {
        initializeGeneratorMap();
        return List.copyOf(generatorMap.keySet());
    }

    /**
     * 生成模型参数 (便捷方法)
     *
     * @param modelType 模型类型
     * @param modelId 模型ID
     * @param taskId 任务ID
     * @param architectureParams 架构参数
     * @param generationMethod 生成方式
     * @param createdBy 创建者
     * @return 生成的参数，如果不支持模型类型则返回null
     */
    public Map<String, Object> generateParameters(String modelType, String modelId, String taskId,
                                                 Map<String, Object> architectureParams,
                                                 String generationMethod, String createdBy) {
        SklearnModelParameterGenerator generator = getGenerator(modelType);
        if (generator == null) {
            return null;
        }

        return generator.generateParameters(modelId, taskId, architectureParams, generationMethod, createdBy);
    }

    /**
     * 验证模型参数 (便捷方法)
     *
     * @param modelType 模型类型
     * @param parameters 参数
     * @return 验证结果
     */
    public boolean validateParameters(String modelType, Map<String, Object> parameters) {
        SklearnModelParameterGenerator generator = getGenerator(modelType);
        if (generator == null) {
            return false;
        }

        return generator.validateParameters(parameters);
    }
}