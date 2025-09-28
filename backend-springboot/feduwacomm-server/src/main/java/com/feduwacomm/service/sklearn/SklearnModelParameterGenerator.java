package com.feduwacomm.service.sklearn;

import java.util.Map;

/**
 * Sklearn模型参数生成器接口
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
public interface SklearnModelParameterGenerator {

    /**
     * 生成符合sklearn标准的模型参数
     *
     * @param modelId 模型ID
     * @param taskId 任务ID
     * @param architectureParams 架构参数
     * @param generationMethod 生成方式
     * @param createdBy 创建者
     * @return 标准化的模型参数JSON对象
     */
    Map<String, Object> generateParameters(String modelId, String taskId,
                                         Map<String, Object> architectureParams,
                                         String generationMethod, String createdBy);

    /**
     * 验证生成的参数是否符合规范
     *
     * @param parameters 参数对象
     * @return 验证结果
     */
    boolean validateParameters(Map<String, Object> parameters);

    /**
     * 获取支持的模型类型
     *
     * @return 模型类型标识
     */
    String getSupportedModelType();
}