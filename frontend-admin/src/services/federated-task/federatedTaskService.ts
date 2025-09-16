/**
 * 联邦学习任务服务层 - 企业级规范实现
 * 提供联邦学习任务管理相关的业务逻辑处理
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { federatedTask } from '@/api/federated-task'
import type { PaginationParams, FederatedTask, FederatedTaskDetails, TaskResults } from '@/types'
import type { 
  CreateTaskRequest,
  ConfigTaskRequest,
  StartTaskResponse,
  PauseTaskResponse,
  ResumeTaskResponse,
  StopTaskRequest,
  StopTaskResponse,
  CancelTaskRequest,
  CancelTaskResponse,
  TaskListParams,
  TaskListResponse,
  DeleteTaskRequest,
  DeleteTaskResponse,
  TaskLogsParams,
  TaskLogsResponse
} from './type'

/**
 * 联邦学习任务服务类
 */
export class FederatedTaskService {
  // ==================== 任务创建管理 ====================

  /**
   * 创建联邦学习任务
   * @param taskData 任务创建数据
   * @returns 创建响应信息
   */
  async createTask(taskData: CreateTaskRequest): Promise<{
    taskId: string
    taskName: string
    status: string
    createdAt: string
    createdBy: string
    participantCount: number
    estimatedDuration: number
  }> {
    try {
      this.validateCreateTaskRequest(taskData)
      
      const response = await federatedTask.createTask(taskData)
      
      return {
        taskId: response.taskId,
        taskName: response.taskName,
        status: response.status,
        createdAt: response.createdAt,
        createdBy: response.createdBy,
        participantCount: response.participantCount,
        estimatedDuration: response.estimatedDuration
      }
    } catch (error) {
      throw this.handleServiceError(error, '创建联邦学习任务失败')
    }
  }

  /**
   * 配置联邦学习任务
   * @param taskId 任务ID
   * @param config 配置数据
   * @returns 配置响应信息
   */
  async configTask(taskId: string, config: ConfigTaskRequest): Promise<{
    taskId: string
    status: string
    updatedAt: string
    configVersion: string
  }> {
    try {
      this.validateTaskId(taskId)
      this.validateConfigTaskRequest(config)
      
      const response = await federatedTask.configTask(taskId, config)
      
      return {
        taskId: response.taskId,
        status: response.status,
        updatedAt: response.updatedAt,
        configVersion: response.configVersion
      }
    } catch (error) {
      throw this.handleServiceError(error, `配置联邦学习任务失败 (ID: ${taskId})`)
    }
  }

  // ==================== 任务控制管理 ====================

  /**
   * 启动联邦学习任务
   * @param taskId 任务ID
   * @returns 启动响应信息
   */
  async startTask(taskId: string): Promise<StartTaskResponse> {
    try {
      this.validateTaskId(taskId)
      
      const response = await federatedTask.startTask(taskId)
      
      return {
        taskId: response.taskId,
        status: response.status as FederatedTask['status'],
        startedAt: response.startedAt,
        currentRound: response.currentRound,
        participants: response.participants
      }
    } catch (error) {
      throw this.handleServiceError(error, `启动联邦学习任务失败 (ID: ${taskId})`)
    }
  }

  /**
   * 暂停联邦学习任务
   * @param taskId 任务ID
   * @returns 暂停响应信息
   */
  async pauseTask(taskId: string): Promise<PauseTaskResponse> {
    try {
      this.validateTaskId(taskId)
      
      const response = await federatedTask.pauseTask(taskId)
      
      return {
        taskId: response.taskId,
        status: response.status as FederatedTask['status'],
        pausedAt: response.pausedAt,
        currentRound: response.currentRound,
        resumePoint: response.resumePoint
      }
    } catch (error) {
      throw this.handleServiceError(error, `暂停联邦学习任务失败 (ID: ${taskId})`)
    }
  }

  /**
   * 恢复联邦学习任务
   * @param taskId 任务ID
   * @returns 恢复响应信息
   */
  async resumeTask(taskId: string): Promise<ResumeTaskResponse> {
    try {
      this.validateTaskId(taskId)
      
      const response = await federatedTask.resumeTask(taskId)
      
      return {
        taskId: response.taskId,
        status: response.status as FederatedTask['status'],
        resumedAt: response.resumedAt,
        currentRound: response.currentRound
      }
    } catch (error) {
      throw this.handleServiceError(error, `恢复联邦学习任务失败 (ID: ${taskId})`)
    }
  }

  /**
   * 停止联邦学习任务
   * @param taskId 任务ID
   * @param stopData 停止参数
   * @returns 停止响应信息
   */
  async stopTask(taskId: string, stopData?: StopTaskRequest): Promise<StopTaskResponse> {
    try {
      this.validateTaskId(taskId)
      if (stopData) {
        this.validateStopTaskRequest(stopData)
      }
      
      const response = await federatedTask.stopTask(taskId, stopData)
      
      return {
        taskId: response.taskId,
        status: response.status as FederatedTask['status'],
        stoppedAt: response.stoppedAt,
        finalRound: response.finalRound,
        checkpointSaved: response.checkpointSaved,
        checkpointPath: response.checkpointPath
      }
    } catch (error) {
      throw this.handleServiceError(error, `停止联邦学习任务失败 (ID: ${taskId})`)
    }
  }

  /**
   * 取消联邦学习任务
   * @param taskId 任务ID
   * @param cancelData 取消参数
   * @returns 取消响应信息
   */
  async cancelTask(taskId: string, cancelData?: CancelTaskRequest): Promise<CancelTaskResponse> {
    try {
      this.validateTaskId(taskId)
      if (cancelData) {
        this.validateCancelTaskRequest(cancelData)
      }
      
      const response = await federatedTask.cancelTask(taskId, cancelData)
      
      return {
        taskId: response.taskId,
        status: response.status as FederatedTask['status'],
        cancelledAt: response.cancelledAt,
        reason: response.reason
      }
    } catch (error) {
      throw this.handleServiceError(error, `取消联邦学习任务失败 (ID: ${taskId})`)
    }
  }

  // ==================== 任务查询管理 ====================

  /**
   * 获取任务详情
   * @param taskId 任务ID
   * @returns 任务详细信息
   */
  async getTaskDetail(taskId: string): Promise<FederatedTaskDetails> {
    try {
      this.validateTaskId(taskId)
      
      const task = await federatedTask.getTaskDetail(taskId)
      return this.transformFederatedTaskDetails(task)
    } catch (error) {
      throw this.handleServiceError(error, `获取联邦学习任务详情失败 (ID: ${taskId})`)
    }
  }

  /**
   * 获取任务列表
   * @param params 查询参数
   * @returns 分页任务列表
   */
  async getTaskList(params: TaskListParams = {}): Promise<TaskListResponse> {
    try {
      this.validateTaskListParams(params)
      
      const result = await federatedTask.getTaskList(params)
      
      return {
        total: result.total,
        page: result.page,
        size: result.size,
        tasks: result.tasks.map(task => this.transformFederatedTask(task))
      }
    } catch (error) {
      throw this.handleServiceError(error, '获取联邦学习任务列表失败')
    }
  }

  /**
   * 获取任务结果
   * @param taskId 任务ID
   * @returns 任务结果信息
   */
  async getTaskResults(taskId: string): Promise<TaskResults> {
    try {
      this.validateTaskId(taskId)
      
      const results = await federatedTask.getTaskResults(taskId)
      return this.transformTaskResults(results)
    } catch (error) {
      throw this.handleServiceError(error, `获取联邦学习任务结果失败 (ID: ${taskId})`)
    }
  }

  /**
   * 获取任务日志
   * @param taskId 任务ID
   * @param params 日志查询参数
   * @returns 分页任务日志
   */
  async getTaskLogs(taskId: string, params: TaskLogsParams = {}): Promise<TaskLogsResponse> {
    try {
      this.validateTaskId(taskId)
      this.validateTaskLogsParams(params)
      
      const result = await federatedTask.getTaskLogs(taskId, params)
      
      return {
        taskId: result.taskId,
        total: result.total,
        page: result.page,
        size: result.size,
        logs: result.logs.map(log => this.transformTaskLog(log))
      }
    } catch (error) {
      throw this.handleServiceError(error, `获取联邦学习任务日志失败 (ID: ${taskId})`)
    }
  }

  /**
   * 删除联邦学习任务
   * @param taskId 任务ID
   * @param deleteOptions 删除选项
   * @returns 删除响应信息
   */
  async deleteTask(taskId: string, deleteOptions?: DeleteTaskRequest): Promise<DeleteTaskResponse> {
    try {
      this.validateTaskId(taskId)
      if (deleteOptions) {
        this.validateDeleteTaskRequest(deleteOptions)
      }
      
      const response = await federatedTask.deleteTask(taskId, deleteOptions)
      
      return {
        taskId: response.taskId,
        deletedAt: response.deletedAt,
        dataDeleted: response.dataDeleted,
        modelPreserved: response.modelPreserved
      }
    } catch (error) {
      throw this.handleServiceError(error, `删除联邦学习任务失败 (ID: ${taskId})`)
    }
  }

  // ==================== 私有方法 ====================

  /**
   * 验证任务ID
   */
  private validateTaskId(taskId: string): void {
    if (!taskId || typeof taskId !== 'string' || taskId.trim().length === 0) {
      throw new Error('任务ID不能为空')
    }
    
    // 验证UUID格式（32位十六进制字符）
    const uuidRegex = /^[a-f0-9]{32}$/i
    if (!uuidRegex.test(taskId)) {
      throw new Error('任务ID格式不正确，应为32位UUID格式')
    }
  }

  /**
   * 验证创建任务请求
   */
  private validateCreateTaskRequest(data: CreateTaskRequest): void {
    if (!data.taskName || data.taskName.trim().length === 0) {
      throw new Error('任务名称不能为空')
    }
    
    if (data.taskName.length > 100) {
      throw new Error('任务名称不能超过100个字符')
    }
    
    if (!data.taskType) {
      throw new Error('任务类型不能为空')
    }
    
    const validTaskTypes = ['CLASSIFICATION', 'REGRESSION', 'CLUSTERING', 'ANOMALY_DETECTION']
    if (!validTaskTypes.includes(data.taskType)) {
      throw new Error('任务类型无效')
    }
    
    if (!data.algorithm || data.algorithm.trim().length === 0) {
      throw new Error('算法不能为空')
    }
    
    if (!data.participants || !Array.isArray(data.participants) || data.participants.length === 0) {
      throw new Error('参与者列表不能为空')
    }
    
    if (data.participants.length < 2) {
      throw new Error('参与者数量至少为2个')
    }
    
    // 验证参与者
    data.participants.forEach((participant, index) => {
      if (!participant.vmId || typeof participant.vmId !== 'string') {
        throw new Error(`参与者${index + 1}的虚拟机ID无效`)
      }
      
      if (!participant.role || typeof participant.role !== 'string') {
        throw new Error(`参与者${index + 1}的角色无效`)
      }
      
      if (!participant.dataSource || typeof participant.dataSource !== 'string') {
        throw new Error(`参与者${index + 1}的数据源无效`)
      }
    })
    
    // 验证超参数
    if (!data.hyperparameters) {
      throw new Error('超参数配置不能为空')
    }
    
    const { hyperparameters } = data
    if (typeof hyperparameters.learningRate !== 'number' || hyperparameters.learningRate <= 0) {
      throw new Error('学习率必须为正数')
    }
    
    if (!Number.isInteger(hyperparameters.batchSize) || hyperparameters.batchSize <= 0) {
      throw new Error('批次大小必须为正整数')
    }
    
    if (!Number.isInteger(hyperparameters.epochs) || hyperparameters.epochs <= 0) {
      throw new Error('训练轮次必须为正整数')
    }
    
    if (!Number.isInteger(hyperparameters.rounds) || hyperparameters.rounds <= 0) {
      throw new Error('联邦轮次必须为正整数')
    }
    
    if (!Number.isInteger(hyperparameters.minParticipants) || hyperparameters.minParticipants <= 0) {
      throw new Error('最小参与者数量必须为正整数')
    }
    
    // 验证模型配置
    if (!data.modelConfig) {
      throw new Error('模型配置不能为空')
    }
    
    const { modelConfig } = data
    if (!modelConfig.modelType || typeof modelConfig.modelType !== 'string') {
      throw new Error('模型类型不能为空')
    }
    
    if (!Array.isArray(modelConfig.featureColumns) || modelConfig.featureColumns.length === 0) {
      throw new Error('特征列不能为空')
    }
    
    if (!modelConfig.targetColumn || typeof modelConfig.targetColumn !== 'string') {
      throw new Error('目标列不能为空')
    }
    
    if (typeof modelConfig.testSize !== 'number' || modelConfig.testSize <= 0 || modelConfig.testSize >= 1) {
      throw new Error('测试集比例必须在0-1之间')
    }
  }

  /**
   * 验证配置任务请求
   */
  private validateConfigTaskRequest(data: ConfigTaskRequest): void {
    if (data.hyperparameters) {
      const { hyperparameters } = data
      
      if (hyperparameters.learningRate !== undefined) {
        if (typeof hyperparameters.learningRate !== 'number' || hyperparameters.learningRate <= 0) {
          throw new Error('学习率必须为正数')
        }
      }
      
      if (hyperparameters.batchSize !== undefined) {
        if (!Number.isInteger(hyperparameters.batchSize) || hyperparameters.batchSize <= 0) {
          throw new Error('批次大小必须为正整数')
        }
      }
      
      if (hyperparameters.epochs !== undefined) {
        if (!Number.isInteger(hyperparameters.epochs) || hyperparameters.epochs <= 0) {
          throw new Error('训练轮次必须为正整数')
        }
      }
      
      if (hyperparameters.rounds !== undefined) {
        if (!Number.isInteger(hyperparameters.rounds) || hyperparameters.rounds <= 0) {
          throw new Error('联邦轮次必须为正整数')
        }
      }
      
      if (hyperparameters.minParticipants !== undefined) {
        if (!Number.isInteger(hyperparameters.minParticipants) || hyperparameters.minParticipants <= 0) {
          throw new Error('最小参与者数量必须为正整数')
        }
      }
    }
    
    if (data.securityConfig?.differentialPrivacy) {
      const { differentialPrivacy } = data.securityConfig
      
      if (differentialPrivacy.epsilon !== undefined) {
        if (typeof differentialPrivacy.epsilon !== 'number' || differentialPrivacy.epsilon <= 0) {
          throw new Error('差分隐私epsilon参数必须为正数')
        }
      }
      
      if (differentialPrivacy.delta !== undefined) {
        if (typeof differentialPrivacy.delta !== 'number' || differentialPrivacy.delta < 0 || differentialPrivacy.delta >= 1) {
          throw new Error('差分隐私delta参数必须在0-1之间')
        }
      }
    }
  }

  /**
   * 验证任务列表查询参数
   */
  private validateTaskListParams(params: TaskListParams): void {
    if (params.page !== undefined && params.page < 1) {
      throw new Error('页码必须大于0')
    }
    
    if (params.size !== undefined && (params.size < 1 || params.size > 100)) {
      throw new Error('每页大小必须在1-100范围内')
    }
    
    if (params.status !== undefined) {
      const validStatuses = ['CREATED', 'CONFIGURED', 'RUNNING', 'PAUSED', 'STOPPED', 'COMPLETED', 'FAILED', 'CANCELLED']
      if (!validStatuses.includes(params.status)) {
        throw new Error('任务状态参数无效')
      }
    }
    
    if (params.type !== undefined) {
      const validTypes = ['CLASSIFICATION', 'REGRESSION', 'CLUSTERING', 'ANOMALY_DETECTION']
      if (!validTypes.includes(params.type)) {
        throw new Error('任务类型参数无效')
      }
    }
    
    if (params.startDate !== undefined && params.endDate !== undefined) {
      const startDate = new Date(params.startDate)
      const endDate = new Date(params.endDate)
      
      if (startDate > endDate) {
        throw new Error('开始日期不能晚于结束日期')
      }
    }
  }

  /**
   * 验证任务日志查询参数
   */
  private validateTaskLogsParams(params: TaskLogsParams): void {
    if (params.page !== undefined && params.page < 1) {
      throw new Error('页码必须大于0')
    }
    
    if (params.size !== undefined && (params.size < 1 || params.size > 1000)) {
      throw new Error('每页大小必须在1-1000范围内')
    }
    
    if (params.level !== undefined) {
      const validLevels = ['DEBUG', 'INFO', 'WARN', 'ERROR']
      if (!validLevels.includes(params.level)) {
        throw new Error('日志级别参数无效')
      }
    }
    
    if (params.startTime !== undefined && params.endTime !== undefined) {
      const startTime = new Date(params.startTime)
      const endTime = new Date(params.endTime)
      
      if (startTime > endTime) {
        throw new Error('开始时间不能晚于结束时间')
      }
    }
  }

  /**
   * 验证停止任务请求
   */
  private validateStopTaskRequest(data: StopTaskRequest): void {
    if (data.reason !== undefined && typeof data.reason !== 'string') {
      throw new Error('停止原因必须为字符串')
    }
    
    if (data.saveCheckpoint !== undefined && typeof data.saveCheckpoint !== 'boolean') {
      throw new Error('保存检查点标志必须为布尔值')
    }
  }

  /**
   * 验证取消任务请求
   */
  private validateCancelTaskRequest(data: CancelTaskRequest): void {
    if (data.reason !== undefined && typeof data.reason !== 'string') {
      throw new Error('取消原因必须为字符串')
    }
  }

  /**
   * 验证删除任务请求
   */
  private validateDeleteTaskRequest(data: DeleteTaskRequest): void {
    if (data.deleteData !== undefined && typeof data.deleteData !== 'boolean') {
      throw new Error('删除数据标志必须为布尔值')
    }
    
    if (data.deleteModel !== undefined && typeof data.deleteModel !== 'boolean') {
      throw new Error('删除模型标志必须为布尔值')
    }
  }

  /**
   * 转换联邦学习任务数据
   */
  private transformFederatedTask(task: any): FederatedTask {
    return {
      taskId: task.taskId,
      taskName: task.taskName,
      taskType: task.taskType,
      status: task.status,
      createdAt: task.createdAt,
      startedAt: task.startedAt,
      completedAt: task.completedAt,
      participantCount: task.participantCount,
      currentRound: task.currentRound,
      totalRounds: task.totalRounds,
      progress: task.progress,
      finalAccuracy: task.finalAccuracy
    }
  }

  /**
   * 转换联邦学习任务详情数据
   */
  private transformFederatedTaskDetails(task: any): FederatedTaskDetails {
    return {
      taskId: task.taskId,
      taskName: task.taskName,
      taskType: task.taskType,
      status: task.status,
      createdAt: task.createdAt,
      startedAt: task.startedAt,
      completedAt: task.completedAt,
      participantCount: task.participantCount,
      currentRound: task.currentRound,
      totalRounds: task.totalRounds,
      progress: task.progress,
      finalAccuracy: task.finalAccuracy,
      algorithm: task.algorithm,
      participants: task.participants,
      metrics: task.metrics
    }
  }

  /**
   * 转换任务结果数据
   */
  private transformTaskResults(results: any): TaskResults {
    return {
      taskId: results.taskId,
      taskName: results.taskName,
      status: results.status,
      finalResults: results.finalResults,
      roundResults: results.roundResults,
      participantResults: results.participantResults,
      modelInfo: results.modelInfo
    }
  }

  /**
   * 转换任务日志数据
   */
  private transformTaskLog(log: any): {
    readonly timestamp: string
    readonly level: string
    readonly message: string
    readonly source: string
    readonly details?: Record<string, unknown>
  } {
    return {
      timestamp: log.timestamp,
      level: log.level,
      message: log.message,
      source: log.source,
      details: log.details
    }
  }

  /**
   * 统一错误处理
   */
  private handleServiceError(error: any, message: string): Error {
    console.error(`[FederatedTaskService] ${message}:`, error)
    
    if (error.response?.data?.message) {
      return new Error(`${message}: ${error.response.data.message}`)
    }
    
    if (error.message) {
      return new Error(`${message}: ${error.message}`)
    }
    
    return new Error(message)
  }
}

// 导出服务实例
export const federatedTaskService = new FederatedTaskService()

// 导出默认实例
export default federatedTaskService
