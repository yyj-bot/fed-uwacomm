package com.feduwacomm.service;

import com.feduwacomm.dto.*;
import com.feduwacomm.entity.FederatedTask;
import com.feduwacomm.entity.TaskParticipant;
import com.feduwacomm.vo.*;

import java.util.List;

/**
 * 联邦学习任务服务接口
 * 提供任务生命周期管理功能
 */
public interface FederatedTaskService {

    // 任务生命周期管理
    /**
     * 创建联邦学习任务
     */
    TaskOperationVO createTask(TaskCreateDTO createDTO, String createdBy);

    /**
     * 配置任务参数
     */
    TaskOperationVO configureTask(String taskId, TaskConfigDTO configDTO, String updatedBy);

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
    boolean isValidTaskConfig(TaskCreateDTO createDTO);

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
    String generateConfigJson(TaskCreateDTO createDTO);

    /**
     * 生成任务配置JSON
     */
    String generateConfigJson(TaskConfigDTO configDTO);

    /**
     * 估算任务执行时间
     */
    int estimateTaskDuration(TaskCreateDTO createDTO);
}