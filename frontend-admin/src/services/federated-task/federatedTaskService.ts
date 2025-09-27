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
import type {
  AvailableVM,
  AvailableDataset,
  RoleConfig,
  AlgorithmTemplate,
  DistributionPreview,
  ParticipantValidation,
  ConfigStatus,
  ResourceUsage,
  // v1.4 新增：联邦学习流程编排类型
  WorkflowConfig,
  SchedulingOptions,
  OrchestrationWorkflow,
  WorkflowTimeline,
  WorkflowPerformanceAnalysis,
  StateSnapshot
} from '@/api/federated-task'

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

  // ==================== v1.3 新增接口组 ====================
  
  // ==================== 预配置接口组 ====================
  
  /**
   * 获取可用虚拟机列表
   * @param params 查询参数
   * @returns 虚拟机列表
   */
  async getAvailableVMs(params: {
    algorithm?: string
    minCpuCores?: number
    minMemoryMb?: number
    status?: string
    capabilities?: string
  } = {}): Promise<{
    total: number
    availableVms: AvailableVM[]
  }> {
    try {
      this.validateAvailableVMsParams(params)
      
      const response = await federatedTask.getAvailableVMs(params)
      
      return {
        total: response.total,
        availableVms: response.availableVms
      }
    } catch (error) {
      throw this.handleServiceError(error, '获取可用虚拟机列表失败')
    }
  }

  /**
   * 获取可用数据集列表
   * @param params 查询参数
   * @returns 数据集列表
   */
  async getAvailableDatasets(params: {
    dataType?: string
    status?: string
    minSize?: number
    maxSize?: number
    keyword?: string
  } = {}): Promise<{
    total: number
    availableDatasets: AvailableDataset[]
  }> {
    try {
      this.validateAvailableDatasetsParams(params)
      
      const response = await federatedTask.getAvailableDatasets(params)
      
      return {
        total: response.total,
        availableDatasets: response.availableDatasets
      }
    } catch (error) {
      throw this.handleServiceError(error, '获取可用数据集列表失败')
    }
  }

  /**
   * 获取角色配置选项
   * @returns 角色配置列表
   */
  async getRoleConfigs(): Promise<{
    roles: RoleConfig[]
  }> {
    try {
      const response = await federatedTask.getRoleConfigs()
      
      return {
        roles: response.roles
      }
    } catch (error) {
      throw this.handleServiceError(error, '获取角色配置选项失败')
    }
  }

  /**
   * 获取算法配置模板
   * @returns 算法模板列表
   */
  async getAlgorithmTemplates(): Promise<{
    templates: AlgorithmTemplate[]
  }> {
    try {
      const response = await federatedTask.getAlgorithmTemplates()
      
      return {
        templates: response.templates
      }
    } catch (error) {
      throw this.handleServiceError(error, '获取算法配置模板失败')
    }
  }

  // ==================== 智能配置接口组 ====================
  
  /**
   * 数据分配预览
   * @param data 分配参数
   * @returns 分配预览结果
   */
  async previewDataDistribution(data: {
    datasetId: string
    distributionStrategy: 'BALANCED' | 'RANDOM' | 'CUSTOM'
    participants: Array<{
      vmId: string
      requestedRatio: number
    }>
  }): Promise<DistributionPreview> {
    try {
      this.validateDistributionPreviewRequest(data)
      
      const response = await federatedTask.previewDataDistribution(data)
      
      return response
    } catch (error) {
      throw this.handleServiceError(error, '数据分配预览失败')
    }
  }

  /**
   * 参与者验证
   * @param data 验证参数
   * @returns 验证结果
   */
  async validateParticipants(data: {
    algorithm: string
    taskType: 'CLASSIFICATION' | 'REGRESSION' | 'CLUSTERING' | 'ANOMALY_DETECTION'
    participants: Array<{
      vmId: string
      role: 'PARTICIPANT'
    }>
  }): Promise<ParticipantValidation> {
    try {
      this.validateParticipantsRequest(data)
      
      const response = await federatedTask.validateParticipants(data)
      
      return response
    } catch (error) {
      throw this.handleServiceError(error, '参与者验证失败')
    }
  }

  // ==================== 增强监控接口组 ====================
  
  /**
   * 获取配置状态监控
   * @param taskId 任务ID
   * @returns 配置状态信息
   */
  async getConfigStatus(taskId: string): Promise<ConfigStatus> {
    try {
      this.validateTaskId(taskId)
      
      const response = await federatedTask.getConfigStatus(taskId)
      
      return response
    } catch (error) {
      throw this.handleServiceError(error, `获取配置状态监控失败 (ID: ${taskId})`)
    }
  }

  /**
   * 获取资源使用监控
   * @param taskId 任务ID
   * @returns 资源使用信息
   */
  async getResourceUsage(taskId: string): Promise<ResourceUsage> {
    try {
      this.validateTaskId(taskId)
      
      const response = await federatedTask.getResourceUsage(taskId)
      
      return response
    } catch (error) {
      throw this.handleServiceError(error, `获取资源使用监控失败 (ID: ${taskId})`)
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
        throw new Error(`参与者${index + 1}的角色无效，只支持PARTICIPANT角色`)
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

  // ==================== v1.3 新增验证方法 ====================
  
  /**
   * 验证可用虚拟机查询参数
   */
  private validateAvailableVMsParams(params: {
    algorithm?: string
    minCpuCores?: number
    minMemoryMb?: number
    status?: string
    capabilities?: string
  }): void {
    if (params.minCpuCores !== undefined && (typeof params.minCpuCores !== 'number' || params.minCpuCores <= 0)) {
      throw new Error('最小CPU核心数必须为正数')
    }
    
    if (params.minMemoryMb !== undefined && (typeof params.minMemoryMb !== 'number' || params.minMemoryMb <= 0)) {
      throw new Error('最小内存必须为正数')
    }
  }

  /**
   * 验证可用数据集查询参数
   */
  private validateAvailableDatasetsParams(params: {
    dataType?: string
    status?: string
    minSize?: number
    maxSize?: number
    keyword?: string
  }): void {
    if (params.minSize !== undefined && (typeof params.minSize !== 'number' || params.minSize < 0)) {
      throw new Error('最小数据大小必须为非负数')
    }
    
    if (params.maxSize !== undefined && (typeof params.maxSize !== 'number' || params.maxSize < 0)) {
      throw new Error('最大数据大小必须为非负数')
    }
    
    if (params.minSize !== undefined && params.maxSize !== undefined && params.minSize > params.maxSize) {
      throw new Error('最小数据大小不能大于最大数据大小')
    }
  }

  /**
   * 验证数据分配预览请求
   */
  private validateDistributionPreviewRequest(data: {
    datasetId: string
    distributionStrategy: 'BALANCED' | 'RANDOM' | 'CUSTOM'
    participants: Array<{
      vmId: string
      requestedRatio: number
    }>
  }): void {
    if (!data.datasetId || typeof data.datasetId !== 'string') {
      throw new Error('数据集ID不能为空')
    }
    
    if (!data.distributionStrategy) {
      throw new Error('分配策略不能为空')
    }
    
    const validStrategies = ['BALANCED', 'RANDOM', 'CUSTOM']
    if (!validStrategies.includes(data.distributionStrategy)) {
      throw new Error('分配策略无效')
    }
    
    if (!Array.isArray(data.participants) || data.participants.length === 0) {
      throw new Error('参与者列表不能为空')
    }
    
    let totalRatio = 0
    data.participants.forEach((participant, index) => {
      if (!participant.vmId || typeof participant.vmId !== 'string') {
        throw new Error(`参与者${index + 1}的虚拟机ID无效`)
      }
      
      if (typeof participant.requestedRatio !== 'number' || participant.requestedRatio <= 0 || participant.requestedRatio > 1) {
        throw new Error(`参与者${index + 1}的请求比例必须在0-1之间`)
      }
      
      totalRatio += participant.requestedRatio
    })
    
    if (Math.abs(totalRatio - 1) > 0.01) {
      throw new Error('参与者请求比例总和必须等于1')
    }
  }

  /**
   * 验证参与者验证请求
   */
  private validateParticipantsRequest(data: {
    algorithm: string
    taskType: 'CLASSIFICATION' | 'REGRESSION' | 'CLUSTERING' | 'ANOMALY_DETECTION'
    participants: Array<{
      vmId: string
      role: 'PARTICIPANT'
    }>
  }): void {
    if (!data.algorithm || typeof data.algorithm !== 'string') {
      throw new Error('算法不能为空')
    }
    
    if (!data.taskType) {
      throw new Error('任务类型不能为空')
    }
    
    const validTaskTypes = ['CLASSIFICATION', 'REGRESSION', 'CLUSTERING', 'ANOMALY_DETECTION']
    if (!validTaskTypes.includes(data.taskType)) {
      throw new Error('任务类型无效')
    }
    
    if (!Array.isArray(data.participants) || data.participants.length === 0) {
      throw new Error('参与者列表不能为空')
    }
    
    data.participants.forEach((participant, index) => {
      if (!participant.vmId || typeof participant.vmId !== 'string') {
        throw new Error(`参与者${index + 1}的虚拟机ID无效`)
      }
      
      if (!participant.role) {
        throw new Error(`参与者${index + 1}的角色不能为空`)
      }
      
      const validRoles = ['PARTICIPANT']
      if (!validRoles.includes(participant.role)) {
        throw new Error(`参与者${index + 1}的角色无效，只支持PARTICIPANT角色`)
      }
    })
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
   * 验证新的参与者配置格式
   */
  private validateParticipantConfig(config: {
    selectionMode: 'MANUAL' | 'AUTOMATIC'
    requirements?: {
      minParticipants: number
      maxParticipants: number
      minCpuCores: number
      minMemoryMb: number
    }
    participants: Array<{
      vmId: string
      role: 'PARTICIPANT'
      dataRatio: number
      capabilities?: string[]
      constraints?: {
        maxCpuUsage?: number
        maxMemoryUsage?: number
      }
    }>
  }): void {
    if (!config.selectionMode) {
      throw new Error('参与者选择模式不能为空')
    }
    
    const validSelectionModes = ['MANUAL', 'AUTOMATIC']
    if (!validSelectionModes.includes(config.selectionMode)) {
      throw new Error('参与者选择模式无效')
    }
    
    if (!Array.isArray(config.participants) || config.participants.length === 0) {
      throw new Error('参与者列表不能为空')
    }
    
    if (config.participants.length < 2) {
      throw new Error('参与者数量至少为2个')
    }
    
    let totalRatio = 0
    config.participants.forEach((participant, index) => {
      if (!participant.vmId || typeof participant.vmId !== 'string') {
        throw new Error(`参与者${index + 1}的虚拟机ID无效`)
      }
      
      if (!participant.role) {
        throw new Error(`参与者${index + 1}的角色不能为空`)
      }
      
      const validRoles = ['PARTICIPANT']
      if (!validRoles.includes(participant.role)) {
        throw new Error(`参与者${index + 1}的角色无效，只支持PARTICIPANT角色`)
      }
      
      if (typeof participant.dataRatio !== 'number' || participant.dataRatio <= 0 || participant.dataRatio > 1) {
        throw new Error(`参与者${index + 1}的数据比例必须在0-1之间`)
      }
      
      totalRatio += participant.dataRatio
      
      // 验证约束条件
      if (participant.constraints) {
        if (participant.constraints.maxCpuUsage !== undefined) {
          if (typeof participant.constraints.maxCpuUsage !== 'number' || 
              participant.constraints.maxCpuUsage < 0 || 
              participant.constraints.maxCpuUsage > 100) {
            throw new Error(`参与者${index + 1}的最大CPU使用率必须在0-100之间`)
          }
        }
        
        if (participant.constraints.maxMemoryUsage !== undefined) {
          if (typeof participant.constraints.maxMemoryUsage !== 'number' || 
              participant.constraints.maxMemoryUsage < 0 || 
              participant.constraints.maxMemoryUsage > 100) {
            throw new Error(`参与者${index + 1}的最大内存使用率必须在0-100之间`)
          }
        }
      }
    })
    
    if (Math.abs(totalRatio - 1) > 0.01) {
      throw new Error('参与者数据比例总和必须等于1')
    }
    
    // 验证需求配置
    if (config.requirements) {
      const { requirements } = config
      
      if (requirements.minParticipants !== undefined && 
          (!Number.isInteger(requirements.minParticipants) || requirements.minParticipants <= 0)) {
        throw new Error('最小参与者数量必须为正整数')
      }
      
      if (requirements.maxParticipants !== undefined && 
          (!Number.isInteger(requirements.maxParticipants) || requirements.maxParticipants <= 0)) {
        throw new Error('最大参与者数量必须为正整数')
      }
      
      if (requirements.minParticipants !== undefined && 
          requirements.maxParticipants !== undefined && 
          requirements.minParticipants > requirements.maxParticipants) {
        throw new Error('最小参与者数量不能大于最大参与者数量')
      }
      
      if (requirements.minCpuCores !== undefined && 
          (typeof requirements.minCpuCores !== 'number' || requirements.minCpuCores <= 0)) {
        throw new Error('最小CPU核心数必须为正数')
      }
      
      if (requirements.minMemoryMb !== undefined && 
          (typeof requirements.minMemoryMb !== 'number' || requirements.minMemoryMb <= 0)) {
        throw new Error('最小内存必须为正数')
      }
    }
  }
  
  /**
   * 验证旧的参与者配置格式（废弃）
   */
  private validateLegacyParticipants(participants: Array<{
    vmId: string
    role: string
    dataSource: string
  }>): void {
    if (!Array.isArray(participants) || participants.length === 0) {
      throw new Error('参与者列表不能为空')
    }
    
    if (participants.length < 2) {
      throw new Error('参与者数量至少为2个')
    }
    
    participants.forEach((participant, index) => {
      if (!participant.vmId || typeof participant.vmId !== 'string') {
        throw new Error(`参与者${index + 1}的虚拟机ID无效`)
      }
      
      if (!participant.role || typeof participant.role !== 'string') {
        throw new Error(`参与者${index + 1}的角色无效，只支持PARTICIPANT角色`)
      }
      
      if (!participant.dataSource || typeof participant.dataSource !== 'string') {
        console.warn(`🚨 废弃警告：参与者${index + 1}使用了废弃的 dataSource 字段，建议使用 datasetConfig 和 dataRatio 代替`)
        throw new Error(`参与者${index + 1}的数据源无效`)
      }
    })
  }
  
  /**
   * 验证数据集配置
   */
  private validateDatasetConfig(config: {
    datasetId: string
    distributionStrategy: 'BALANCED' | 'RANDOM' | 'CUSTOM'
    distributionRatios: Record<string, number>
    validationSplit: number
    testSplit: number
  }): void {
    if (!config.datasetId || typeof config.datasetId !== 'string') {
      throw new Error('数据集ID不能为空')
    }
    
    if (!config.distributionStrategy) {
      throw new Error('分配策略不能为空')
    }
    
    const validStrategies = ['BALANCED', 'RANDOM', 'CUSTOM']
    if (!validStrategies.includes(config.distributionStrategy)) {
      throw new Error('分配策略无效')
    }
    
    if (!config.distributionRatios || typeof config.distributionRatios !== 'object') {
      throw new Error('分配比例配置不能为空')
    }
    
    const ratioSum = Object.values(config.distributionRatios).reduce((sum, ratio) => {
      if (typeof ratio !== 'number' || ratio <= 0 || ratio > 1) {
        throw new Error('分配比例必须在0-1之间')
      }
      return sum + ratio
    }, 0)
    
    if (Math.abs(ratioSum - 1) > 0.01) {
      throw new Error('分配比例总和必须等于1')
    }
    
    if (typeof config.validationSplit !== 'number' || config.validationSplit < 0 || config.validationSplit >= 1) {
      throw new Error('验证集比例必须在0-1之间')
    }
    
    if (typeof config.testSplit !== 'number' || config.testSplit < 0 || config.testSplit >= 1) {
      throw new Error('测试集比例必须在0-1之间')
    }
    
    if (config.validationSplit + config.testSplit >= 1) {
      throw new Error('验证集和测试集比例总和不能大于等于1')
    }
  }

  /**
   * 统一错误处理
   */
  private handleServiceError(error: any, message: string): Error {
    console.error(`[FederatedTaskService] ${message}:`, error)
    
    // 🆕 v1.3 新增：检查是否有废弃警告
    if (error.response?.data?.code === 'SIMPLE_PARTICIPANT_CONFIG_DEPRECATED') {
      console.warn('🚨 废弃警告：', error.response.data.message)
      console.warn('迁移指南：', error.response.data.details?.migrationGuide)
    }
    
    if (error.response?.data?.message) {
      return new Error(`${message}: ${error.response.data.message}`)
    }
    
    if (error.message) {
      return new Error(`${message}: ${error.message}`)
    }
    
    return new Error(message)
  }

  // ==================== v1.4 新增：联邦学习流程编排管理 ====================

  /**
   * 启动联邦学习流程
   * @param orchestrationData 流程启动数据
   * @returns 启动响应信息
   */
  async startOrchestration(orchestrationData: {
    taskId: string
    workflowConfig: WorkflowConfig
    schedulingOptions?: SchedulingOptions
  }): Promise<{
    orchestrationId: string
    taskId: string
    status: string
    startedAt: string
    estimatedCompletion: string
    currentStage: string
    workflowPlan: {
      totalStages: number
      estimatedDuration: string
      stages: Array<{
        name: string
        status: string
        estimatedDuration: string
      }>
    }
    resourceAllocation: {
      allocatedMemory: string
      allocatedCpuCores: number
      allocatedBandwidth: string
      participatingVms: string[]
    }
  }> {
    try {
      this.validateOrchestrationData(orchestrationData)
      
      const response = await federatedTask.startOrchestration(orchestrationData)
      
      return response
    } catch (error) {
      throw this.handleServiceError(error, '启动联邦学习流程失败')
    }
  }

  /**
   * 查询流程状态
   * @param orchestrationId 编排任务ID
   * @param params 查询参数
   * @returns 流程状态信息
   */
  async getOrchestrationStatus(orchestrationId: string, params: {
    includeDetails?: boolean
    includeMetrics?: boolean
    refresh?: boolean
  } = {}): Promise<OrchestrationWorkflow> {
    try {
      this.validateOrchestrationId(orchestrationId)
      
      const response = await federatedTask.getOrchestrationStatus(orchestrationId, params)
      
      return response
    } catch (error) {
      throw this.handleServiceError(error, `查询流程状态失败 (ID: ${orchestrationId})`)
    }
  }

  /**
   * 暂停流程执行
   * @param orchestrationId 编排任务ID
   * @param pauseData 暂停参数
   * @returns 暂停响应信息
   */
  async pauseOrchestration(orchestrationId: string, pauseData?: {
    reason?: string
    pauseMode?: 'GRACEFUL' | 'IMMEDIATE'
    waitForCurrentRound?: boolean
    preserveState?: boolean
    notifyParticipants?: boolean
  }): Promise<{
    orchestrationId: string
    status: string
    pausedAt: string
    pausedStage: string
    pausedRound?: number
    reason?: string
    canResume: boolean
    stateSnapshot: StateSnapshot
  }> {
    try {
      this.validateOrchestrationId(orchestrationId)
      if (pauseData) {
        this.validatePauseOrchestrationRequest(pauseData)
      }
      
      const response = await federatedTask.pauseOrchestration(orchestrationId, pauseData)
      
      return response
    } catch (error) {
      throw this.handleServiceError(error, `暂停流程执行失败 (ID: ${orchestrationId})`)
    }
  }

  /**
   * 恢复流程执行
   * @param orchestrationId 编排任务ID
   * @param resumeData 恢复参数
   * @returns 恢复响应信息
   */
  async resumeOrchestration(orchestrationId: string, resumeData?: {
    resumeFromSnapshot?: boolean
    snapshotId?: string
    validateState?: boolean
    notifyParticipants?: boolean
  }): Promise<{
    orchestrationId: string
    status: string
    resumedAt: string
    resumedStage: string
    resumedRound?: number
    stateValidation: {
      passed: boolean
      modelsVerified: number
      stateConsistent: boolean
    }
    estimatedRemainingTime: string
  }> {
    try {
      this.validateOrchestrationId(orchestrationId)
      if (resumeData) {
        this.validateResumeOrchestrationRequest(resumeData)
      }
      
      const response = await federatedTask.resumeOrchestration(orchestrationId, resumeData)
      
      return response
    } catch (error) {
      throw this.handleServiceError(error, `恢复流程执行失败 (ID: ${orchestrationId})`)
    }
  }

  /**
   * 终止流程执行
   * @param orchestrationId 编排任务ID
   * @param params 终止参数
   * @returns 终止响应信息
   */
  async terminateOrchestration(orchestrationId: string, params: {
    force?: boolean
    cleanup?: boolean
    saveResults?: boolean
  } = {}): Promise<{
    orchestrationId: string
    status: string
    terminatedAt: string
    terminatedStage: string
    terminatedRound?: number
    completedRounds: number
    partialResults: {
      bestModel: {
        roundNumber: number
        accuracy: number
        modelId: string
      }
      savedModels: number
      trainingMetrics: string
    }
    cleanup: {
      resourcesReleased: boolean
      temporaryDataCleared: boolean
      participantsNotified: boolean
    }
  }> {
    try {
      this.validateOrchestrationId(orchestrationId)
      
      const response = await federatedTask.terminateOrchestration(orchestrationId, params)
      
      return response
    } catch (error) {
      throw this.handleServiceError(error, `终止流程执行失败 (ID: ${orchestrationId})`)
    }
  }

  /**
   * 获取流程时间线
   * @param orchestrationId 编排任务ID
   * @param params 查询参数
   * @returns 流程时间线信息
   */
  async getOrchestrationTimeline(orchestrationId: string, params: {
    includeEvents?: boolean
    eventLevel?: 'ALL' | 'MAJOR' | 'ERROR'
    timeRange?: string
  } = {}): Promise<WorkflowTimeline> {
    try {
      this.validateOrchestrationId(orchestrationId)
      
      const response = await federatedTask.getOrchestrationTimeline(orchestrationId, params)
      
      return response
    } catch (error) {
      throw this.handleServiceError(error, `获取流程时间线失败 (ID: ${orchestrationId})`)
    }
  }

  /**
   * 获取流程列表
   * @param params 查询参数
   * @returns 流程列表信息
   */
  async getOrchestrationList(params: {
    taskId?: string
    status?: string
    page?: number
    size?: number
    sortBy?: string
    sortOrder?: 'asc' | 'desc'
  } = {}): Promise<{
    total: number
    page: number
    size: number
    items: Array<{
      orchestrationId: string
      taskId: string
      status: string
      startedAt: string
      completedAt?: string
      currentStage: string
      progress: number
      duration: string
      participatingVms: number
      completedRounds: number
      totalRounds: number
      finalAccuracy?: number
      success?: boolean
    }>
  }> {
    try {
      this.validateOrchestrationListParams(params)
      
      const response = await federatedTask.getOrchestrationList(params)
      
      return response
    } catch (error) {
      throw this.handleServiceError(error, '获取流程列表失败')
    }
  }

  /**
   * 获取流程性能分析
   * @param orchestrationId 编排任务ID
   * @param params 分析参数
   * @returns 性能分析信息
   */
  async getOrchestrationAnalytics(orchestrationId: string, params: {
    includeRecommendations?: boolean
    metricsLevel?: 'BASIC' | 'DETAILED' | 'FULL'
  } = {}): Promise<WorkflowPerformanceAnalysis> {
    try {
      this.validateOrchestrationId(orchestrationId)
      
      const response = await federatedTask.getOrchestrationAnalytics(orchestrationId, params)
      
      return response
    } catch (error) {
      throw this.handleServiceError(error, `获取流程性能分析失败 (ID: ${orchestrationId})`)
    }
  }

  // ==================== v1.4 新增：验证方法 ====================

  /**
   * 验证编排任务ID
   */
  private validateOrchestrationId(orchestrationId: string): void {
    if (!orchestrationId || typeof orchestrationId !== 'string' || orchestrationId.trim().length === 0) {
      throw new Error('编排任务ID不能为空')
    }
    
    // 验证格式
    if (orchestrationId.length < 5) {
      throw new Error('编排任务ID格式不正确')
    }
  }

  /**
   * 验证编排数据
   */
  private validateOrchestrationData(data: {
    taskId: string
    workflowConfig: WorkflowConfig
    schedulingOptions?: SchedulingOptions
  }): void {
    if (!data.taskId) {
      throw new Error('任务ID不能为空')
    }
    
    if (!data.workflowConfig) {
      throw new Error('工作流配置不能为空')
    }
    
    this.validateWorkflowConfig(data.workflowConfig)
    
    if (data.schedulingOptions) {
      this.validateSchedulingOptions(data.schedulingOptions)
    }
  }

  /**
   * 验证工作流配置
   */
  private validateWorkflowConfig(config: WorkflowConfig): void {
    if (typeof config.autoStart !== 'boolean') {
      throw new Error('autoStart必须为布尔值')
    }
    
    if (!config.stages) {
      throw new Error('工作流阶段配置不能为空')
    }
    
    // 验证各个阶段配置
    if (config.stages.federatedTraining) {
      const training = config.stages.federatedTraining
      if (training.maxRounds <= 0) {
        throw new Error('最大训练轮次必须大于0')
      }
      if (training.convergenceThreshold <= 0 || training.convergenceThreshold >= 1) {
        throw new Error('收敛阈值必须在0-1之间')
      }
      if (training.participantThreshold <= 0 || training.participantThreshold > 1) {
        throw new Error('参与者阈值必须在0-1之间')
      }
    }
    
    if (config.errorHandling) {
      const errorHandling = config.errorHandling
      if (errorHandling.maxRetries < 0) {
        throw new Error('最大重试次数不能为负数')
      }
      if (errorHandling.retryDelay < 0) {
        throw new Error('重试延迟不能为负数')
      }
    }
  }

  /**
   * 验证调度选项
   */
  private validateSchedulingOptions(options: SchedulingOptions): void {
    const validPriorities = ['LOW', 'NORMAL', 'HIGH', 'URGENT']
    if (!validPriorities.includes(options.priority)) {
      throw new Error('优先级无效')
    }
    
    if (options.maxExecutionTime <= 0) {
      throw new Error('最大执行时间必须大于0')
    }
    
    if (options.resourceLimits) {
      if (options.resourceLimits.maxCpuCores <= 0) {
        throw new Error('最大CPU核心数必须大于0')
      }
    }
  }

  /**
   * 验证暂停编排请求
   */
  private validatePauseOrchestrationRequest(data: {
    reason?: string
    pauseMode?: 'GRACEFUL' | 'IMMEDIATE'
    waitForCurrentRound?: boolean
    preserveState?: boolean
    notifyParticipants?: boolean
  }): void {
    if (data.pauseMode && !['GRACEFUL', 'IMMEDIATE'].includes(data.pauseMode)) {
      throw new Error('暂停模式无效')
    }
    
    if (data.reason && typeof data.reason !== 'string') {
      throw new Error('暂停原因必须为字符串')
    }
  }

  /**
   * 验证恢复编排请求
   */
  private validateResumeOrchestrationRequest(data: {
    resumeFromSnapshot?: boolean
    snapshotId?: string
    validateState?: boolean
    notifyParticipants?: boolean
  }): void {
    if (data.resumeFromSnapshot && !data.snapshotId) {
      throw new Error('使用快照恢复时必须提供快照ID')
    }
    
    if (data.snapshotId && typeof data.snapshotId !== 'string') {
      throw new Error('快照ID必须为字符串')
    }
  }

  /**
   * 验证编排列表查询参数
   */
  private validateOrchestrationListParams(params: {
    taskId?: string
    status?: string
    page?: number
    size?: number
    sortBy?: string
    sortOrder?: 'asc' | 'desc'
  }): void {
    if (params.page !== undefined && params.page < 1) {
      throw new Error('页码必须大于0')
    }
    
    if (params.size !== undefined && (params.size < 1 || params.size > 100)) {
      throw new Error('每页大小必须在1-100范围内')
    }
    
    if (params.sortOrder && !['asc', 'desc'].includes(params.sortOrder)) {
      throw new Error('排序方向必须为asc或desc')
    }
  }
}

// 导出服务实例
export const federatedTaskService = new FederatedTaskService()

// 导出默认实例
export default federatedTaskService
