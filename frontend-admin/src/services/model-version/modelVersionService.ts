/**
 * 模型版本管理服务层 - 企业级规范实现
 * 提供全局模型版本管理相关的业务逻辑处理
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { model, initialModel } from '@/api/model-version'
import type { 
  ModelVersionListParams,
  ModelVersionDetail,
  TaskModelVersions,
  EvaluationRequest,
  EvaluationResponse,
  BatchEvaluationRequest,
  BatchEvaluationResponse,
  RollbackRequest,
  RollbackResponse,
  RollbackInfo,
  DownloadRequest,
  DeleteModelRequest,
  DeleteModelResponse,
  BatchDeleteRequest,
  BatchDeleteResponse,
  StatisticsParams,
  TaskStatistics,
  ModelVersionPaginatedResponse,
  EvaluationResult,
  ModelStatistics,
  // 初始模型管理相关类型
  InitialModelGenerationRequest,
  InitialModelGenerationResponse,
  InitialModelUploadRequest,
  InitialModelUploadResponse,
  InitialModelInfo,
  ModelDistributionRequest,
  ModelDistributionResponse,
  DistributionStatusDetail,
  InitialModelDeleteRequest,
  InitialModelDeleteResponse
} from './type'

/**
 * 模型版本管理服务类
 */
export class ModelVersionService {
  // ==================== 初始模型管理 ====================

  /**
   * 生成随机初始模型
   * @param generationData 模型生成数据
   * @returns 生成响应信息
   */
  async generateInitialModel(generationData: InitialModelGenerationRequest): Promise<InitialModelGenerationResponse> {
    try {
      this.validateInitialModelGenerationRequest(generationData)
      
      const response = await initialModel.generateInitialModel(generationData)
      
      return {
        modelId: response.modelId,
        taskId: response.taskId,
        modelType: response.modelType,
        modelSize: response.modelSize,
        parametersCount: response.parametersCount,
        architecture: response.architecture,
        generatedAt: response.generatedAt,
        status: response.status,
        checksum: response.checksum
      }
    } catch (error) {
      throw this.handleServiceError(error, '随机初始模型生成失败')
    }
  }

  /**
   * 上传自定义初始模型
   * @param uploadData 上传数据
   * @returns 上传响应信息
   */
  async uploadCustomInitialModel(uploadData: InitialModelUploadRequest): Promise<InitialModelUploadResponse> {
    try {
      this.validateInitialModelUploadRequest(uploadData)
      
      // 构建FormData
      const formData = new FormData()
      formData.append('taskId', uploadData.taskId)
      formData.append('modelType', uploadData.modelType)
      if (uploadData.description) {
        formData.append('description', uploadData.description)
      }
      formData.append('file', uploadData.file)
      if (uploadData.metadata) {
        formData.append('metadata', JSON.stringify(uploadData.metadata))
      }
      
      const response = await initialModel.uploadCustomInitialModel(formData)
      
      return {
        modelId: response.modelId,
        taskId: response.taskId,
        modelType: response.modelType,
        fileName: response.fileName,
        modelSize: response.modelSize,
        uploadedAt: response.uploadedAt,
        status: response.status,
        checksum: response.checksum,
        metadata: response.metadata
      }
    } catch (error) {
      throw this.handleServiceError(error, '自定义初始模型上传失败')
    }
  }

  /**
   * 获取任务初始模型信息
   * @param taskId 任务ID
   * @param params 查询参数
   * @returns 初始模型信息
   */
  async getTaskInitialModel(taskId: string, params: {
    includeParameters?: boolean
    format?: 'json' | 'binary'
  } = {}): Promise<InitialModelInfo> {
    try {
      this.validateTaskId(taskId)
      this.validateInitialModelQueryParams(params)
      
      const response = await initialModel.getTaskInitialModel(taskId, params)
      
      return {
        modelId: response.modelId,
        taskId: response.taskId,
        modelType: response.modelType,
        modelSize: response.modelSize,
        createdAt: response.createdAt,
        status: response.status as any,
        architecture: response.architecture,
        distributionStatus: response.distributionStatus,
        checksum: response.checksum
      }
    } catch (error) {
      throw this.handleServiceError(error, `获取任务初始模型失败 (TaskID: ${taskId})`)
    }
  }

  /**
   * 分发初始模型到虚拟机
   * @param taskId 任务ID
   * @param distributionData 分发数据
   * @returns 分发响应信息
   */
  async distributeInitialModel(taskId: string, distributionData: ModelDistributionRequest): Promise<ModelDistributionResponse> {
    try {
      this.validateTaskId(taskId)
      this.validateModelDistributionRequest(distributionData)
      
      const response = await initialModel.distributeInitialModel(taskId, distributionData)
      
      return {
        distributionId: response.distributionId,
        taskId: response.taskId,
        modelId: response.modelId,
        targetVms: response.targetVms,
        distributionMode: response.distributionMode,
        status: response.status,
        startedAt: response.startedAt,
        estimatedCompletion: response.estimatedCompletion,
        progress: response.progress
      }
    } catch (error) {
      throw this.handleServiceError(error, `初始模型分发失败 (TaskID: ${taskId})`)
    }
  }

  /**
   * 获取分发状态
   * @param distributionId 分发ID
   * @returns 分发状态详情
   */
  async getDistributionStatus(distributionId: string): Promise<DistributionStatusDetail> {
    try {
      this.validateDistributionId(distributionId)
      
      const response = await initialModel.getDistributionStatus(distributionId)
      
      return {
        distributionId: response.distributionId,
        taskId: response.taskId,
        modelId: response.modelId,
        status: response.status as any,
        startedAt: response.startedAt,
        completedAt: response.completedAt,
        progress: response.progress,
        vmDetails: response.vmDetails
      }
    } catch (error) {
      throw this.handleServiceError(error, `获取分发状态失败 (DistributionID: ${distributionId})`)
    }
  }

  /**
   * 下载初始模型
   * @param taskId 任务ID
   * @param params 下载参数
   * @returns 文件数据流
   */
  async downloadInitialModel(taskId: string, params: {
    format?: 'binary' | 'json'
    modelId?: string
  } = {}): Promise<Blob> {
    try {
      this.validateTaskId(taskId)
      this.validateInitialModelDownloadParams(params)
      
      const blob = await initialModel.downloadInitialModel(taskId, params)
      return blob
    } catch (error) {
      throw this.handleServiceError(error, `下载初始模型失败 (TaskID: ${taskId})`)
    }
  }

  /**
   * 删除初始模型
   * @param taskId 任务ID
   * @param deleteData 删除参数
   * @returns 删除响应信息
   */
  async deleteInitialModel(taskId: string, deleteData: InitialModelDeleteRequest = {}): Promise<InitialModelDeleteResponse> {
    try {
      this.validateTaskId(taskId)
      this.validateInitialModelDeleteRequest(deleteData)
      
      const response = await initialModel.deleteInitialModel(taskId, deleteData)
      
      return {
        taskId: response.taskId,
        modelId: response.modelId,
        deletedAt: response.deletedAt,
        cleanupStatus: response.cleanupStatus
      }
    } catch (error) {
      throw this.handleServiceError(error, `删除初始模型失败 (TaskID: ${taskId})`)
    }
  }


  // ==================== 模型版本查询管理 ====================

  /**
   * 获取模型版本列表
   * @param params 查询参数
   * @returns 分页模型版本列表
   */
  async getModelVersions(params: ModelVersionListParams = {}): Promise<ModelVersionPaginatedResponse<any>> {
    try {
      this.validateModelVersionListParams(params)
      
      const result = await model.getModelVersions(params)
      return this.transformModelVersionList(result)
    } catch (error) {
      throw this.handleServiceError(error, '获取模型版本列表失败')
    }
  }

  /**
   * 获取模型版本详情
   * @param modelId 模型ID
   * @returns 模型版本详细信息
   */
  async getModelVersionDetail(modelId: string): Promise<ModelVersionDetail> {
    try {
      this.validateModelId(modelId)
      
      const detail = await model.getModelVersionDetail(modelId)
      return this.transformModelVersionDetail(detail)
    } catch (error) {
      throw this.handleServiceError(error, `获取模型版本详情失败 (ID: ${modelId})`)
    }
  }

  /**
   * 获取任务模型版本
   * @param taskId 任务ID
   * @param params 查询参数
   * @returns 任务模型版本信息
   */
  async getTaskModelVersions(taskId: string, params: {
    roundNumber?: number
    status?: string
    sort?: string
    order?: 'asc' | 'desc'
  } = {}): Promise<TaskModelVersions> {
    try {
      this.validateTaskId(taskId)
      this.validateTaskModelVersionParams(params)
      
      const result = await model.getTaskModelVersions(taskId, params)
      return this.transformTaskModelVersions(result)
    } catch (error) {
      throw this.handleServiceError(error, `获取任务模型版本失败 (TaskID: ${taskId})`)
    }
  }

  // ==================== 模型性能评估管理 ====================

  /**
   * 模型性能评估
   * @param evaluationData 评估数据
   * @returns 评估响应信息
   */
  async evaluateModel(evaluationData: EvaluationRequest): Promise<EvaluationResponse> {
    try {
      this.validateEvaluationRequest(evaluationData)
      
      const response = await model.evaluateModel(evaluationData)
      
      return {
        modelId: response.modelId,
        evaluationId: response.evaluationId,
        metrics: response.metrics,
        evaluationTime: response.evaluationTime,
        testSamples: response.testSamples,
        status: response.status,
        createdAt: response.createdAt
      }
    } catch (error) {
      throw this.handleServiceError(error, `模型性能评估失败 (ModelID: ${evaluationData.modelId})`)
    }
  }

  /**
   * 批量模型评估
   * @param evaluationData 批量评估数据
   * @returns 批量评估响应
   */
  async evaluateModelBatch(evaluationData: BatchEvaluationRequest): Promise<BatchEvaluationResponse> {
    try {
      this.validateBatchEvaluationRequest(evaluationData)
      
      const response = await model.evaluateModelBatch(evaluationData)
      
      return {
        taskId: response.taskId,
        evaluatedCount: response.evaluatedCount,
        results: response.results
      }
    } catch (error) {
      throw this.handleServiceError(error, `批量模型评估失败 (TaskID: ${evaluationData.taskId})`)
    }
  }

  /**
   * 获取评估结果
   * @param params 查询参数
   * @returns 分页评估结果
   */
  async getEvaluationResults(params: {
    modelId?: string
    taskId?: string
    evaluationId?: string
    page?: number
    size?: number
  } = {}): Promise<ModelVersionPaginatedResponse<EvaluationResult>> {
    try {
      this.validateEvaluationResultParams(params)
      
      const result = await model.getEvaluationResults(params)
      return this.transformEvaluationResults(result)
    } catch (error) {
      throw this.handleServiceError(error, '获取评估结果失败')
    }
  }


  // ==================== 模型回滚管理 ====================

  /**
   * 模型回滚
   * @param rollbackData 回滚数据
   * @returns 回滚响应信息
   */
  async rollbackModel(rollbackData: RollbackRequest): Promise<RollbackResponse> {
    try {
      this.validateRollbackRequest(rollbackData)
      
      const response = await model.rollbackModel(rollbackData)
      
      return {
        rollbackId: response.rollbackId,
        deploymentId: response.deploymentId,
        fromModelId: response.fromModelId,
        toModelId: response.toModelId,
        status: response.status as any,
        rollbackReason: response.rollbackReason,
        rollbackTime: response.rollbackTime,
        createdAt: response.createdAt
      }
    } catch (error) {
      throw this.handleServiceError(error, `模型回滚失败 (DeploymentID: ${rollbackData.deploymentId})`)
    }
  }

  /**
   * 获取回滚历史
   * @param params 查询参数
   * @returns 分页回滚历史
   */
  async getRollbackHistory(params: {
    deploymentId?: string
    page?: number
    size?: number
  } = {}): Promise<ModelVersionPaginatedResponse<RollbackInfo>> {
    try {
      this.validateRollbackHistoryParams(params)
      
      const result = await model.getRollbackHistory(params)
      return this.transformRollbackHistory(result)
    } catch (error) {
      throw this.handleServiceError(error, '获取回滚历史失败')
    }
  }

  // ==================== 模型下载管理 ====================

  /**
   * 下载模型文件
   * @param modelId 模型ID
   * @param params 下载参数
   * @returns 文件数据流
   */
  async downloadModel(modelId: string, params: DownloadRequest = {}): Promise<Blob> {
    try {
      this.validateModelId(modelId)
      this.validateDownloadRequest(params)
      
      const blob = await model.downloadModel(modelId, params)
      return blob
    } catch (error) {
      throw this.handleServiceError(error, `下载模型文件失败 (ModelID: ${modelId})`)
    }
  }

  /**
   * 批量下载模型
   * @param downloadData 批量下载数据
   * @returns 压缩文件数据流
   */
  async downloadModelBatch(downloadData: {
    modelIds: string[]
    format?: 'original' | 'onnx'
    compressed?: boolean
  }): Promise<Blob> {
    try {
      this.validateBatchDownloadRequest(downloadData)
      
      const blob = await model.downloadModelBatch(downloadData)
      return blob
    } catch (error) {
      throw this.handleServiceError(error, '批量下载模型失败')
    }
  }

  // ==================== 模型删除管理 ====================

  /**
   * 删除模型版本
   * @param modelId 模型ID
   * @param deleteData 删除参数
   * @returns 删除响应信息
   */
  async deleteModel(modelId: string, deleteData: DeleteModelRequest = {}): Promise<DeleteModelResponse> {
    try {
      this.validateModelId(modelId)
      this.validateDeleteModelRequest(deleteData)
      
      const response = await model.deleteModel(modelId, deleteData)
      
      return {
        modelId: response.modelId,
        deletedAt: response.deletedAt
      }
    } catch (error) {
      throw this.handleServiceError(error, `删除模型版本失败 (ModelID: ${modelId})`)
    }
  }

  /**
   * 批量删除模型
   * @param deleteData 批量删除数据
   * @returns 批量删除响应
   */
  async deleteModelBatch(deleteData: BatchDeleteRequest): Promise<BatchDeleteResponse> {
    try {
      this.validateBatchDeleteRequest(deleteData)
      
      const response = await model.deleteModelBatch(deleteData)
      
      return {
        successCount: response.successCount,
        failedCount: response.failedCount,
        results: response.results
      }
    } catch (error) {
      throw this.handleServiceError(error, '批量删除模型失败')
    }
  }

  // ==================== 模型统计管理 ====================

  /**
   * 获取模型统计信息
   * @param params 统计参数
   * @returns 模型统计信息
   */
  async getModelStatistics(params: StatisticsParams = {}): Promise<ModelStatistics> {
    try {
      this.validateStatisticsParams(params)
      
      const statistics = await model.getModelStatistics(params)
      return this.transformModelStatistics(statistics)
    } catch (error) {
      throw this.handleServiceError(error, '获取模型统计信息失败')
    }
  }

  /**
   * 获取任务模型统计
   * @param taskId 任务ID
   * @returns 任务模型统计信息
   */
  async getTaskModelStatistics(taskId: string): Promise<TaskStatistics> {
    try {
      this.validateTaskId(taskId)
      
      const statistics = await model.getTaskModelStatistics(taskId)
      return this.transformTaskStatistics(statistics)
    } catch (error) {
      throw this.handleServiceError(error, `获取任务模型统计失败 (TaskID: ${taskId})`)
    }
  }

  // ==================== 私有验证方法 ====================

  /**
   * 验证初始模型生成请求
   */
  private validateInitialModelGenerationRequest(data: InitialModelGenerationRequest): void {
    this.validateTaskId(data.taskId)
    
    if (!data.modelType || typeof data.modelType !== 'string') {
      throw new Error('模型类型不能为空')
    }
    
    const validModelTypes = ['neural_network', 'random_forest', 'svm', 'linear_regression', 'logistic_regression']
    if (!validModelTypes.includes(data.modelType)) {
      throw new Error('模型类型无效')
    }
    
    if (!data.architecture || typeof data.architecture !== 'object') {
      throw new Error('模型架构参数不能为空')
    }
    
    const { architecture } = data
    if (!Number.isInteger(architecture.inputSize) || architecture.inputSize <= 0) {
      throw new Error('输入层大小必须为正整数')
    }
    
    if (!Array.isArray(architecture.hiddenLayers) || architecture.hiddenLayers.length === 0) {
      throw new Error('隐藏层配置不能为空')
    }
    
    architecture.hiddenLayers.forEach((size, index) => {
      if (!Number.isInteger(size) || size <= 0) {
        throw new Error(`隐藏层${index + 1}大小必须为正整数`)
      }
    })
    
    if (!Number.isInteger(architecture.outputSize) || architecture.outputSize <= 0) {
      throw new Error('输出层大小必须为正整数')
    }
    
    if (!architecture.activationFunction || typeof architecture.activationFunction !== 'string') {
      throw new Error('激活函数不能为空')
    }
    
    if (!architecture.optimizer || typeof architecture.optimizer !== 'string') {
      throw new Error('优化器不能为空')
    }
    
    if (typeof architecture.learningRate !== 'number' || architecture.learningRate <= 0) {
      throw new Error('学习率必须为正数')
    }
    
    if (data.randomSeed !== undefined && (!Number.isInteger(data.randomSeed) || data.randomSeed < 0)) {
      throw new Error('随机种子必须为非负整数')
    }
  }

  /**
   * 验证初始模型上传请求
   */
  private validateInitialModelUploadRequest(data: InitialModelUploadRequest): void {
    this.validateTaskId(data.taskId)
    
    if (!data.modelType || typeof data.modelType !== 'string') {
      throw new Error('模型类型不能为空')
    }
    
    const validModelTypes = ['neural_network', 'random_forest', 'svm', 'linear_regression', 'logistic_regression']
    if (!validModelTypes.includes(data.modelType)) {
      throw new Error('模型类型无效')
    }
    
    if (!data.file || !(data.file instanceof File)) {
      throw new Error('模型文件不能为空')
    }
    
    // 验证文件大小（100MB限制）
    const maxSize = 100 * 1024 * 1024
    if (data.file.size > maxSize) {
      throw new Error('模型文件大小不能超过100MB')
    }
    
    // 验证文件格式
    const supportedFormats = ['.pth', '.pt', '.h5', '.pb', '.onnx', '.pkl', '.pickle', '.joblib']
    const fileName = data.file.name.toLowerCase()
    const isValidFormat = supportedFormats.some(format => fileName.endsWith(format))
    if (!isValidFormat) {
      throw new Error('不支持的模型文件格式')
    }
  }

  /**
   * 验证初始模型查询参数
   */
  private validateInitialModelQueryParams(params: any): void {
    if (params.includeParameters !== undefined && typeof params.includeParameters !== 'boolean') {
      throw new Error('includeParameters参数必须为布尔值')
    }
    
    if (params.format !== undefined && !['json', 'binary'].includes(params.format)) {
      throw new Error('format参数必须为json或binary')
    }
  }

  /**
   * 验证模型分发请求
   */
  private validateModelDistributionRequest(data: ModelDistributionRequest): void {
    if (!data.vmIds || !Array.isArray(data.vmIds) || data.vmIds.length === 0) {
      throw new Error('目标虚拟机列表不能为空')
    }
    
    if (data.vmIds.length > 50) {
      throw new Error('目标虚拟机数量不能超过50个')
    }
    
    data.vmIds.forEach(vmId => {
      if (!vmId || typeof vmId !== 'string' || vmId.trim().length === 0) {
        throw new Error('虚拟机ID不能为空')
      }
    })
    
    if (!data.distributionMode || !['ASYNC', 'SYNC'].includes(data.distributionMode)) {
      throw new Error('分发模式必须为ASYNC或SYNC')
    }
    
    if (data.timeout !== undefined && (!Number.isInteger(data.timeout) || data.timeout <= 0)) {
      throw new Error('超时时间必须为正整数')
    }
    
    if (data.retryAttempts !== undefined && (!Number.isInteger(data.retryAttempts) || data.retryAttempts < 0)) {
      throw new Error('重试次数必须为非负整数')
    }
    
    if (data.verifyChecksum !== undefined && typeof data.verifyChecksum !== 'boolean') {
      throw new Error('verifyChecksum参数必须为布尔值')
    }
    
    if (data.notifyOnCompletion !== undefined && typeof data.notifyOnCompletion !== 'boolean') {
      throw new Error('notifyOnCompletion参数必须为布尔值')
    }
  }

  /**
   * 验证分发ID
   */
  private validateDistributionId(distributionId: string): void {
    if (!distributionId || typeof distributionId !== 'string' || distributionId.trim().length === 0) {
      throw new Error('分发ID不能为空')
    }
  }

  /**
   * 验证初始模型下载参数
   */
  private validateInitialModelDownloadParams(params: any): void {
    if (params.format !== undefined && !['binary', 'json'].includes(params.format)) {
      throw new Error('下载格式必须为binary或json')
    }
  }

  /**
   * 验证初始模型删除请求
   */
  private validateInitialModelDeleteRequest(data: InitialModelDeleteRequest): void {
    if (data.force !== undefined && typeof data.force !== 'boolean') {
      throw new Error('强制删除标志必须为布尔值')
    }
  }

  /**
   * 验证模型ID
   */
  private validateModelId(modelId: string): void {
    if (!modelId || typeof modelId !== 'string' || modelId.trim().length === 0) {
      throw new Error('模型ID不能为空')
    }
    
    // 验证UUID格式（32位十六进制字符）
    const uuidRegex = /^[a-f0-9]{32}$/i
    if (!uuidRegex.test(modelId)) {
      throw new Error('模型ID格式不正确，应为32位UUID格式')
    }
  }

  /**
   * 验证任务ID
   */
  private validateTaskId(taskId: string): void {
    if (!taskId || typeof taskId !== 'string' || taskId.trim().length === 0) {
      throw new Error('任务ID不能为空')
    }
    
    const uuidRegex = /^[a-f0-9]{32}$/i
    if (!uuidRegex.test(taskId)) {
      throw new Error('任务ID格式不正确，应为32位UUID格式')
    }
  }

  /**
   * 验证部署ID
   */
  private validateDeploymentId(deploymentId: string): void {
    if (!deploymentId || typeof deploymentId !== 'string' || deploymentId.trim().length === 0) {
      throw new Error('部署ID不能为空')
    }
  }

  /**
   * 验证模型版本列表参数
   */
  private validateModelVersionListParams(params: ModelVersionListParams): void {
    if (params.page !== undefined && params.page < 1) {
      throw new Error('页码必须大于0')
    }
    
    if (params.size !== undefined && (params.size < 1 || params.size > 100)) {
      throw new Error('每页大小必须在1-100范围内')
    }
    
    if (params.taskId !== undefined && typeof params.taskId === 'string') {
      const uuidRegex = /^[a-f0-9]{32}$/i
      if (!uuidRegex.test(params.taskId)) {
        throw new Error('任务ID格式不正确')
      }
    }
    
    if (params.roundNumber !== undefined && (!Number.isInteger(params.roundNumber) || params.roundNumber <= 0)) {
      throw new Error('训练轮数必须为正整数')
    }
    
    if (params.status !== undefined) {
      const validStatuses = ['UPLOADING', 'UPLOADED', 'VALIDATING', 'VALIDATED', 'DEPLOYED', 'DEPRECATED', 'FAILED']
      if (!validStatuses.includes(params.status)) {
        throw new Error('状态参数无效')
      }
    }
    
    if (params.sort !== undefined) {
      const validSortFields = ['createdAt', 'roundNumber', 'accuracy', 'loss']
      if (!validSortFields.includes(params.sort)) {
        throw new Error('排序字段无效')
      }
    }
    
    if (params.order !== undefined && !['asc', 'desc'].includes(params.order)) {
      throw new Error('排序方向必须为asc或desc')
    }
  }

  /**
   * 验证任务模型版本参数
   */
  private validateTaskModelVersionParams(params: any): void {
    if (params.roundNumber !== undefined && (!Number.isInteger(params.roundNumber) || params.roundNumber <= 0)) {
      throw new Error('训练轮数必须为正整数')
    }
    
    if (params.status !== undefined) {
      const validStatuses = ['UPLOADING', 'UPLOADED', 'VALIDATING', 'VALIDATED', 'DEPLOYED', 'DEPRECATED', 'FAILED']
      if (!validStatuses.includes(params.status)) {
        throw new Error('状态参数无效')
      }
    }
    
    if (params.sort !== undefined) {
      const validSortFields = ['roundNumber', 'createdAt', 'accuracy', 'loss']
      if (!validSortFields.includes(params.sort)) {
        throw new Error('排序字段无效')
      }
    }
    
    if (params.order !== undefined && !['asc', 'desc'].includes(params.order)) {
      throw new Error('排序方向必须为asc或desc')
    }
  }

  /**
   * 验证评估请求
   */
  private validateEvaluationRequest(data: EvaluationRequest): void {
    this.validateModelId(data.modelId)
    
    if (!data.testDataPath || typeof data.testDataPath !== 'string' || data.testDataPath.trim().length === 0) {
      throw new Error('测试数据路径不能为空')
    }
    
    if (data.metrics && !Array.isArray(data.metrics)) {
      throw new Error('评估指标必须为数组格式')
    }
    
    if (data.batchSize !== undefined && (!Number.isInteger(data.batchSize) || data.batchSize <= 0)) {
      throw new Error('批次大小必须为正整数')
    }
    
    if (data.device !== undefined && !['cpu', 'gpu', 'cuda'].includes(data.device)) {
      throw new Error('计算设备参数无效')
    }
  }

  /**
   * 验证批量评估请求
   */
  private validateBatchEvaluationRequest(data: BatchEvaluationRequest): void {
    this.validateTaskId(data.taskId)
    
    if (!data.testDataPath || typeof data.testDataPath !== 'string' || data.testDataPath.trim().length === 0) {
      throw new Error('测试数据路径不能为空')
    }
    
    if (data.roundNumbers && !Array.isArray(data.roundNumbers)) {
      throw new Error('评估轮数必须为数组格式')
    }
    
    if (data.roundNumbers) {
      data.roundNumbers.forEach(roundNumber => {
        if (!Number.isInteger(roundNumber) || roundNumber <= 0) {
          throw new Error('评估轮数必须为正整数')
        }
      })
    }
    
    if (data.metrics && !Array.isArray(data.metrics)) {
      throw new Error('评估指标必须为数组格式')
    }
    
    if (data.batchSize !== undefined && (!Number.isInteger(data.batchSize) || data.batchSize <= 0)) {
      throw new Error('批次大小必须为正整数')
    }
  }

  /**
   * 验证评估结果参数
   */
  private validateEvaluationResultParams(params: any): void {
    if (params.modelId !== undefined && typeof params.modelId === 'string') {
      this.validateModelId(params.modelId)
    }
    
    if (params.taskId !== undefined && typeof params.taskId === 'string') {
      this.validateTaskId(params.taskId)
    }
    
    if (params.page !== undefined && params.page < 1) {
      throw new Error('页码必须大于0')
    }
    
    if (params.size !== undefined && (params.size < 1 || params.size > 100)) {
      throw new Error('每页大小必须在1-100范围内')
    }
  }


  /**
   * 验证回滚请求
   */
  private validateRollbackRequest(data: RollbackRequest): void {
    this.validateDeploymentId(data.deploymentId)
    this.validateModelId(data.targetModelId)
    
    if (data.force !== undefined && typeof data.force !== 'boolean') {
      throw new Error('强制回滚标志必须为布尔值')
    }
  }

  /**
   * 验证回滚历史参数
   */
  private validateRollbackHistoryParams(params: any): void {
    if (params.page !== undefined && params.page < 1) {
      throw new Error('页码必须大于0')
    }
    
    if (params.size !== undefined && (params.size < 1 || params.size > 100)) {
      throw new Error('每页大小必须在1-100范围内')
    }
  }

  /**
   * 验证下载请求
   */
  private validateDownloadRequest(params: DownloadRequest): void {
    if (params.format !== undefined && !['original', 'onnx'].includes(params.format)) {
      throw new Error('下载格式无效')
    }
    
    if (params.compressed !== undefined && typeof params.compressed !== 'boolean') {
      throw new Error('压缩标志必须为布尔值')
    }
  }

  /**
   * 验证批量下载请求
   */
  private validateBatchDownloadRequest(data: any): void {
    if (!data.modelIds || !Array.isArray(data.modelIds) || data.modelIds.length === 0) {
      throw new Error('模型ID列表不能为空')
    }
    
    if (data.modelIds.length > 50) {
      throw new Error('批量下载模型数量不能超过50个')
    }
    
    data.modelIds.forEach((id: string) => {
      this.validateModelId(id)
    })
    
    if (data.format !== undefined && !['original', 'onnx'].includes(data.format)) {
      throw new Error('下载格式无效')
    }
    
    if (data.compressed !== undefined && typeof data.compressed !== 'boolean') {
      throw new Error('压缩标志必须为布尔值')
    }
  }

  /**
   * 验证删除模型请求
   */
  private validateDeleteModelRequest(data: DeleteModelRequest): void {
    if (data.force !== undefined && typeof data.force !== 'boolean') {
      throw new Error('强制删除标志必须为布尔值')
    }
    
    if (data.deleteFile !== undefined && typeof data.deleteFile !== 'boolean') {
      throw new Error('删除文件标志必须为布尔值')
    }
  }

  /**
   * 验证批量删除请求
   */
  private validateBatchDeleteRequest(data: BatchDeleteRequest): void {
    if (!data.modelIds || !Array.isArray(data.modelIds) || data.modelIds.length === 0) {
      throw new Error('模型ID列表不能为空')
    }
    
    if (data.modelIds.length > 50) {
      throw new Error('批量删除模型数量不能超过50个')
    }
    
    data.modelIds.forEach(id => {
      this.validateModelId(id)
    })
    
    if (data.force !== undefined && typeof data.force !== 'boolean') {
      throw new Error('强制删除标志必须为布尔值')
    }
    
    if (data.deleteFile !== undefined && typeof data.deleteFile !== 'boolean') {
      throw new Error('删除文件标志必须为布尔值')
    }
  }

  /**
   * 验证统计参数
   */
  private validateStatisticsParams(params: StatisticsParams): void {
    if (params.taskId !== undefined && typeof params.taskId === 'string') {
      this.validateTaskId(params.taskId)
    }
    
    if (params.timeRange !== undefined && !['7d', '30d', '90d'].includes(params.timeRange)) {
      throw new Error('时间范围参数无效')
    }
  }

  // ==================== 私有转换方法 ====================

  /**
   * 转换模型版本列表
   */
  private transformModelVersionList(result: any): ModelVersionPaginatedResponse<any> {
    return {
      total: result.total,
      pages: result.pages,
      current: result.current,
      size: result.size,
      records: result.records.map((item: any) => this.transformModelVersionItem(item))
    }
  }

  /**
   * 转换模型版本项
   */
  private transformModelVersionItem(item: any): any {
    return {
      modelId: item.modelId,
      taskId: item.taskId,
      roundNumber: item.roundNumber,
      accuracy: item.accuracy,
      loss: item.loss,
      status: item.status,
      description: item.description,
      parameters: item.parameters,
      createdAt: item.createdAt
    }
  }

  /**
   * 转换模型版本详情
   */
  private transformModelVersionDetail(detail: any): ModelVersionDetail {
    return {
      modelId: detail.modelId,
      taskId: detail.taskId,
      roundNumber: detail.roundNumber,
      aggregationMethod: detail.aggregationMethod,
      clientCount: detail.clientCount,
      modelJson: detail.modelJson,
      metrics: detail.metrics,
      createdAt: detail.createdAt,
      aggregatedAt: detail.aggregatedAt,
      status: detail.status
    }
  }

  /**
   * 转换任务模型版本
   */
  private transformTaskModelVersions(result: any): TaskModelVersions {
    return {
      taskId: result.taskId,
      taskName: result.taskName,
      totalModels: result.totalModels,
      versions: result.versions
    }
  }

  /**
   * 转换评估结果
   */
  private transformEvaluationResults(result: any): ModelVersionPaginatedResponse<EvaluationResult> {
    return {
      total: result.total,
      pages: result.pages,
      current: result.current,
      size: result.size,
      records: result.records
    }
  }


  /**
   * 转换回滚历史
   */
  private transformRollbackHistory(result: any): ModelVersionPaginatedResponse<RollbackInfo> {
    return {
      total: result.total,
      pages: result.pages,
      current: result.current,
      size: result.size,
      records: result.records
    }
  }

  /**
   * 转换模型统计
   */
  private transformModelStatistics(statistics: any): ModelStatistics {
    return {
      totalModels: statistics.totalModels,
      averageAccuracy: statistics.averageAccuracy,
      averageLoss: statistics.averageLoss,
      uploadTrend: statistics.uploadTrend,
      accuracyTrend: statistics.accuracyTrend
    }
  }

  /**
   * 转换任务统计
   */
  private transformTaskStatistics(statistics: any): TaskStatistics {
    return {
      taskId: statistics.taskId,
      taskName: statistics.taskName,
      totalRounds: statistics.totalRounds,
      completedRounds: statistics.completedRounds,
      performanceMetrics: statistics.performanceMetrics
    }
  }

  /**
   * 统一错误处理
   */
  private handleServiceError(error: any, message: string): Error {
    console.error(`[ModelVersionService] ${message}:`, error)
    
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
export const modelVersionService = new ModelVersionService()

// 导出默认实例
export default modelVersionService
