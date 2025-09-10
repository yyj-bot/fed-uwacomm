package com.feduwacomm.service;

import com.feduwacomm.common.PageResult;
import com.feduwacomm.dto.InitialModelGenerationDTO;
import com.feduwacomm.vo.InitialModelInfoVO;

import java.util.List;

/**
 * 初始模型生成服务接口
 * 负责联邦学习初始模型的生成、管理和分发
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
public interface InitialModelGenerationService {

    /**
     * 生成初始模型
     * 支持随机生成和自定义上传两种方式
     *
     * @param generationDTO 模型生成请求
     * @param createdBy 创建者ID
     * @return 生成的模型信息
     */
    InitialModelInfoVO generateInitialModel(InitialModelGenerationDTO generationDTO, String createdBy);

    /**
     * 上传自定义初始模型
     *
     * @param taskId 任务ID
     * @param modelType 模型类型
     * @param filePath 上传文件路径
     * @param architectureParams 架构参数
     * @param createdBy 创建者ID
     * @return 上传的模型信息
     */
    InitialModelInfoVO uploadCustomModel(String taskId, String modelType, String filePath, 
                                       String architectureParams, String createdBy);

    /**
     * 获取模型详细信息
     *
     * @param modelId 模型ID
     * @return 模型详细信息
     */
    InitialModelInfoVO getModelInfo(String modelId);

    /**
     * 获取任务的所有初始模型
     *
     * @param taskId 任务ID
     * @return 模型列表
     */
    List<InitialModelInfoVO> getTaskModels(String taskId);

    /**
     * 获取任务的分页模型列表
     *
     * @param taskId 任务ID
     * @param status 状态过滤（可选）
     * @param page 页码
     * @param size 每页大小
     * @return 分页模型列表
     */
    PageResult<InitialModelInfoVO> getTaskModelsPaged(String taskId, String status, Integer page, Integer size);

    /**
     * 重新生成失败的模型
     *
     * @param modelId 模型ID
     * @param regeneratedBy 重新生成者ID
     * @return 重新生成的模型信息
     */
    InitialModelInfoVO regenerateFailedModel(String modelId, String regeneratedBy);

    /**
     * 删除初始模型
     *
     * @param modelId 模型ID
     * @param deletedBy 删除者ID
     * @return 是否删除成功
     */
    boolean deleteModel(String modelId, String deletedBy);

    /**
     * 批量删除任务的所有模型
     *
     * @param taskId 任务ID
     * @param deletedBy 删除者ID
     * @return 删除的模型数量
     */
    int deleteTaskModels(String taskId, String deletedBy);

    /**
     * 验证模型文件完整性
     *
     * @param modelId 模型ID
     * @return 验证结果
     */
    boolean validateModelIntegrity(String modelId);

    /**
     * 更新模型状态
     *
     * @param modelId 模型ID
     * @param status 新状态
     * @return 是否更新成功
     */
    boolean updateModelStatus(String modelId, String status);

    /**
     * 获取模型生成进度
     *
     * @param modelId 模型ID
     * @return 进度信息（0-100）
     */
    Integer getGenerationProgress(String modelId);

    /**
     * 取消正在生成的模型
     *
     * @param modelId 模型ID
     * @param cancelledBy 取消者ID
     * @return 是否取消成功
     */
    boolean cancelGeneration(String modelId, String cancelledBy);

    /**
     * 清理过期的失败模型
     *
     * @param daysOld 清理多少天前的失败模型
     * @return 清理的模型数量
     */
    int cleanupFailedModels(int daysOld);

    /**
     * 获取模型生成统计信息
     *
     * @param taskId 任务ID（可选）
     * @return 统计信息
     */
    InitialModelInfoVO.ModelGenerationStats getGenerationStats(String taskId);
}