package com.feduwacomm.service;

import com.feduwacomm.controller.FederatedTaskController;
import com.feduwacomm.dto.*;
import com.feduwacomm.entity.FederatedTask;
import com.feduwacomm.entity.TaskParticipant;
import com.feduwacomm.model.dto.federated.FederatedTaskConfigDTO;
import com.feduwacomm.model.dto.federated.FederatedTaskCreateRequest;
import com.feduwacomm.vo.*;

import java.util.List;

/**
 * 联邦学习任务服务接口
 * 提供任务生命周期管理功能
 */
public interface FederatedTaskService {

    // 任务生命周期管理
    /**
     * 创建联邦学习任务（统一入口）
     */
    TaskOperationVO createTask(FederatedTaskCreateRequest createDTO, String createdBy);

    /**
     * 配置任务参数
     */
    TaskOperationVO configureTask(String taskId, FederatedTaskConfigDTO configDTO, String updatedBy);

    /**
     * 启动任务
     */
    TaskOperationVO startTask(String taskId, String operatorId);

    /**
     * 暂停任务
     */
    TaskOperationVO pauseTask(String taskId, String operatorId);

    /**
     * 恢复任务
     */
    TaskOperationVO resumeTask(String taskId, String operatorId);

    /**
     * 停止任务
     */
    TaskOperationVO stopTask(String taskId, TaskStopDTO stopDTO, String operatorId);

    /**
     * 取消任务
     */
    TaskOperationVO cancelTask(String taskId, TaskCancelDTO cancelDTO, String operatorId);

    /**
     * 删除任务
     */
    TaskOperationVO deleteTask(String taskId, TaskDeleteDTO deleteDTO, String operatorId);

    // 任务查询和监控
    /**
     * 获取任务详细状态
     */
    TaskDetailVO getTaskDetail(String taskId);

    /**
     * 查询任务列表
     */
    TaskListVO getTaskList(TaskQueryDTO queryDTO);

    /**
     * 获取任务结果
     */
    TaskResultVO getTaskResult(String taskId);

    /**
     * 查询任务日志
     */
    TaskLogVO getTaskLogs(String taskId, TaskLogQueryDTO queryDTO);

    // 任务参与者管理
    /**
     * 添加任务参与者
     */
    void addParticipant(String taskId, TaskParticipant participant);

    /**
     * 更新参与者状态
     */
    void updateParticipantStatus(String taskId, String vmId, String status);

    /**
     * 移除任务参与者
     */
    void removeParticipant(String taskId, String vmId);

    /**
     * 获取任务参与者列表
     */
    List<TaskParticipant> getTaskParticipants(String taskId);

    // 任务日志管理
    /**
     * 记录任务日志
     */
    void logTask(String taskId, String level, String message, String source, String vmId, Object details);

    // 任务验证和权限检查
    /**
     * 验证任务是否存在
     */
    boolean taskExists(String taskId);

    /**
     * 检查用户是否有任务操作权限
     */
    boolean hasTaskPermission(String userId, String taskId, String operation);

    /**
     * 验证任务状态转换是否合法
     */
    boolean isValidStatusTransition(String currentStatus, String targetStatus);

    /**
     * 验证任务配置是否有效
     */
    boolean isValidTaskConfig(FederatedTaskCreateRequest createDTO);

    // 内部方法
    /**
     * 根据ID获取任务
     */
    FederatedTask getTaskById(String taskId);

    /**
     * 计算任务进度
     */
    double calculateTaskProgress(String taskId);

    /**
     * 生成任务配置JSON
     */
    String generateConfigJson(FederatedTaskCreateRequest createDTO);

    /**
     * 生成任务配置JSON
     */
    String generateConfigJson(FederatedTaskConfigDTO configDTO);

    /**
     * 估算任务执行时间
     */
    int estimateTaskDuration(FederatedTaskCreateRequest createDTO);

    // ========== v1.3 图形化配置接口方法 ==========

    /**
     * 获取可用虚拟机列表 (v1.3)
     */
    ConfigPreviewVO.AvailableVmsVO getAvailableVms(String algorithm, Integer minCpuCores,
                                                   Integer minMemoryMb, String status, String capabilities);

    /**
     * 获取可用数据集列表 (v1.3)
     */
    ConfigPreviewVO.AvailableDatasetsVO getAvailableDatasets(String dataType, String status,
                                                            Long minSize, Long maxSize, String keyword);

    /**
     * 获取角色配置选项 (v1.3)
     */
    ConfigPreviewVO.RoleConfigVO getRoleConfig();

    /**
     * 获取算法配置模板 (v1.3)
     */
    ConfigPreviewVO.AlgorithmTemplatesVO getAlgorithmTemplates();

    /**
     * 数据分配预览 (v1.3)
     */
    ConfigPreviewVO.DistributionPreviewVO previewDistribution(
        FederatedTaskController.DistributionPreviewRequestDTO requestDTO);

    /**
     * 参与者验证 (v1.3)
     */
    ConfigPreviewVO.ParticipantValidationVO validateParticipants(
        FederatedTaskController.ParticipantValidationRequestDTO requestDTO);

    /**
     * 获取任务配置状态 (v1.3)
     */
    TaskConfigStatusVO getTaskConfigStatus(String taskId);

    /**
     * 获取任务资源使用情况 (v1.3)
     */
    TaskResourceUsageVO getTaskResourceUsage(String taskId);

    /**
     * 创建智能任务 (v1.3 标准方法)
     * 支持新的datasetConfig和participantConfig格式
     */
    TaskOperationVO createSmartTask(FederatedTaskCreateRequest createDTO, String createdBy);

    /**
     * 获取任务全局模型列表
     */
    GlobalModelsVO getTaskGlobalModels(String taskId);

    /**
     * 获取任务统计信息
     */
    TaskStatisticsVO getTaskStatistics();

    /**
     * 批量操作任务
     */
    TaskBatchOperationResultVO batchOperateTask(TaskBatchOperationDTO batchDTO, String operatorId);

    // ========== v1.5标准联邦学习接口 ==========

    /**
     * 标准联邦学习任务创建流程 (v1.5)
     * 实现13步标准化流程的步骤6-12
     * 🎯 实现目标：完整的标准任务创建链路，包含数据集分配
     */
    TaskOperationVO createStandardFederatedTask(FederatedTaskCreateRequest createDTO, String createdBy);

    /**
     * 数据集分配和assignedDatasetId生成 (v1.5)
     * 实现步骤8：通过算法将数据集进行分配
     * 🎯 实现目标：为每个VM生成唯一的assignedDatasetId
     */
    com.feduwacomm.dto.DatasetAllocationResult allocateDatasets(String taskId, List<String> participantVmIds,
                                                                String originalDatasetPath);

    /**
     * 查询数据集状态并验证 (v1.5)
     * 实现步骤9：向虚拟机查询数据集数据
     * 🎯 实现目标：确保所有VM的数据集就绪
     */
    com.feduwacomm.dto.DatasetValidationResult validateParticipantDatasets(String taskId);

    /**
     * 启动联邦学习流程 (v1.5)
     * 实现步骤12-13：发送任务消息并开始联邦学习
     * 🎯 实现目标：启动完整的联邦学习流程
     */
    TaskOperationVO startFederatedLearningFlow(String taskId);
}
