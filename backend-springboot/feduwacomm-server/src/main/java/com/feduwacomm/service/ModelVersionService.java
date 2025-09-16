package com.feduwacomm.service;

import com.feduwacomm.dto.*;
import com.feduwacomm.vo.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 模型版本管理服务接口
 */
public interface ModelVersionService {

    // 模型上传相关
    /**
     * 模型文件上传
     */
    ModelUploadResponseVO uploadModel(String taskId, Integer roundNumber, String description, 
                                    String parameters, MultipartFile file);

    /**
     * 批量模型上传
     */
    ModelBatchUploadResponseVO batchUploadModels(ModelBatchUploadDTO batchUploadDTO);

    // 模型版本查询相关
    /**
     * 模型版本列表查询
     */
    PageResponseDTO<ModelVersionVO> getModelVersions(ModelQueryDTO queryDTO);

    /**
     * 模型版本详情查询
     */
    ModelVersionVO getModelVersionById(String modelId);

    /**
     * 任务模型版本查询
     */
    ModelTaskVersionsVO getModelVersionsByTaskId(String taskId, Integer roundNumber, 
                                               String status, String sort, String order);

    // 模型性能评估相关
    /**
     * 模型性能评估
     */
    ModelEvaluateResponseVO evaluateModel(ModelEvaluateDTO evaluateDTO);

    /**
     * 批量模型评估
     */
    List<ModelEvaluateResponseVO> batchEvaluateModels(ModelBatchEvaluateDTO batchEvaluateDTO);

    /**
     * 评估结果查询
     */
    PageResponseDTO<ModelEvaluateResponseVO> getEvaluationResults(String modelId, String taskId, 
                                                                String evaluationId, int page, int size);

    // 模型部署相关
    /**
     * 模型部署
     */
    ModelDeployResponseVO deployModel(ModelDeployDTO deployDTO);

    /**
     * 部署状态查询
     */
    Map<String, Object> getDeploymentStatus(String deploymentId);

    /**
     * 部署列表查询
     */
    PageResponseDTO<Map<String, Object>> getDeploymentList(String modelId, String status, 
                                                          int page, int size);

    // 模型回滚相关
    /**
     * 模型回滚
     */
    Map<String, Object> rollbackModel(ModelRollbackDTO rollbackDTO);

    /**
     * 回滚历史查询
     */
    PageResponseDTO<Map<String, Object>> getRollbackHistory(String deploymentId, int page, int size);

    // 模型下载相关
    /**
     * 模型文件下载
     */
    byte[] downloadModel(String modelId, String format, Boolean compressed);

    /**
     * 批量模型下载
     */
    byte[] batchDownloadModels(ModelBatchDownloadDTO batchDownloadDTO);

    // 模型删除相关
    /**
     * 模型版本删除
     */
    Map<String, Object> deleteModelVersion(String modelId, Boolean force, Boolean deleteFile);

    /**
     * 批量模型删除
     */
    Map<String, Object> batchDeleteModels(ModelBatchDeleteDTO batchDeleteDTO);

    // 模型统计相关
    /**
     * 模型统计信息
     */
    ModelStatisticsVO getModelStatistics(String taskId, String timeRange);

    /**
     * 任务模型统计
     */
    Map<String, Object> getTaskModelStatistics(String taskId);
}