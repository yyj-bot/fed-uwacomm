/**
 * 训练数据服务层 - 企业级规范实现
 * 提供训练数据管理相关的业务逻辑处理
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { trainingData } from '@/api/training-data'
import type { PaginationParams } from '@/types'
import type { 
  DataStatistics,
  PreprocessTask,
  ValidationResult,
  BatchOperationResult,
  ExportTask,
  DatasetDetail
} from '@/api/training-data'
import type { 
  UploadFileRequest,
  UploadFileResponse,
  UploadTextRequest,
  UploadTextResponse,
  DataListParams,
  DataListResponse,
  PreprocessRequest,
  ValidationRequest,
  UpdateDataRequest,
  UpdateDataResponse,
  DeleteDataRequest,
  DeleteDataResponse,
  BatchOperationRequest,
  DataStatisticsParams,
  ExportDataRequest
} from './type'

/**
 * 训练数据服务类
 */
export class TrainingDataService {
  // ==================== 数据上传管理 ====================

  /**
   * 上传文件
   * @param formData 文件上传数据
   * @returns 上传响应信息
   */
  async uploadFile(formData: FormData): Promise<UploadFileResponse> {
    try {
      this.validateUploadFile(formData)
      
      const response = await trainingData.uploadFile(formData)

      const vmIdEntry = formData.get('vmId')
      if (typeof vmIdEntry !== 'string') {
        throw new Error('虚拟机ID格式不正确')
      }

      return {
        datasetId: response.datasetId,
        datasetDescription: response.datasetDescription,
        datasetType: response.datasetType,
        vmId: vmIdEntry,
        status: response.status,
        uploadTime: response.uploadTime,
        uploadedBy: response.uploadedBy,
        progress: response.progress
      }
    } catch (error) {
      throw this.handleServiceError(error, '文件上传失败')
    }
  }

  /**
   * 上传文本信息
   * @param textData 文本数据
   * @returns 上传响应信息
   */
  async uploadText(textData: UploadTextRequest): Promise<UploadTextResponse> {
    try {
      this.validateUploadText(textData)
      
      const response = await trainingData.uploadText(textData)

      return {
        datasetId: response.datasetId,
        datasetDescription: response.datasetDescription,
        datasetType: response.datasetType,
        vmId: textData.vmId,
        status: response.status,
        uploadTime: response.uploadTime,
        uploadedBy: response.uploadedBy
      }
    } catch (error) {
      throw this.handleServiceError(error, '文本信息上传失败')
    }
  }

  // ==================== 数据查询管理 ====================

  /**
   * 获取数据列表
   * @param params 查询参数
   * @returns 分页数据列表
   */
  async getDataList(params: DataListParams = {}): Promise<DataListResponse> {
    try {
      this.validateDataListParams(params)
      
      const result = await trainingData.getDataList(params)
      
      return {
        total: result.total,
        page: result.page,
        size: result.size,
        dataList: result.dataList.map(item => this.transformDataItem(item))
      }
    } catch (error) {
      throw this.handleServiceError(error, '获取训练数据列表失败')
    }
  }

  /**
   * 获取数据详情
   * @param datasetId 数据集ID
   * @returns 数据详细信息
   */
  async getDataDetail(datasetId: string): Promise<DatasetDetail> {
    try {
      this.validateDatasetId(datasetId)
      
      const detail = await trainingData.getDataDetail(datasetId)
      return this.transformDataDetail(detail)
    } catch (error) {
      throw this.handleServiceError(error, `获取训练数据详情失败 (ID: ${datasetId})`)
    }
  }

  /**
   * 下载数据
   * @param datasetId 数据集ID
   * @returns 文件数据流
   */
  async downloadData(datasetId: string): Promise<Blob> {
    try {
      this.validateDatasetId(datasetId)
      
      const blob = await trainingData.downloadData(datasetId)
      return blob
    } catch (error) {
      throw this.handleServiceError(error, `下载训练数据失败 (ID: ${datasetId})`)
    }
  }

  // ==================== 数据处理管理 ====================

  /**
   * 数据预处理
   * @param datasetId 数据集ID
   * @param preprocessData 预处理参数
   * @returns 预处理任务信息
   */
  async preprocessData(datasetId: string, preprocessData: PreprocessRequest): Promise<PreprocessTask> {
    try {
      this.validateDatasetId(datasetId)
      this.validatePreprocessRequest(preprocessData)
      
      const task = await trainingData.preprocessData(datasetId, preprocessData)
      return this.transformPreprocessTask(task)
    } catch (error) {
      throw this.handleServiceError(error, `数据预处理失败 (ID: ${datasetId})`)
    }
  }

  /**
   * 数据验证
   * @param datasetId 数据集ID
   * @param validationRules 验证规则
   * @returns 验证结果
   */
  async validateData(datasetId: string, validationRules?: ValidationRequest): Promise<ValidationResult> {
    try {
      this.validateDatasetId(datasetId)
      if (validationRules) {
        this.validateValidationRequest(validationRules)
      }
      
      const result = await trainingData.validateData(datasetId, validationRules)
      return this.transformValidationResult(result)
    } catch (error) {
      throw this.handleServiceError(error, `数据验证失败 (ID: ${datasetId})`)
    }
  }

  // ==================== 数据修改管理 ====================

  /**
   * 更新数据信息
   * @param datasetId 数据集ID
   * @param updateData 更新数据
   * @returns 更新响应信息
   */
  async updateData(datasetId: string, updateData: UpdateDataRequest): Promise<UpdateDataResponse> {
    try {
      this.validateDatasetId(datasetId)
      this.validateUpdateDataRequest(updateData)
      
      const response = await trainingData.updateData(datasetId, updateData)
      
      return {
        datasetId: response.datasetId,
        updatedAt: response.updatedAt,
        updatedBy: response.updatedBy
      }
    } catch (error) {
      throw this.handleServiceError(error, `更新训练数据失败 (ID: ${datasetId})`)
    }
  }

  /**
   * 删除数据
   * @param datasetId 数据集ID
   * @param deleteParams 删除参数
   * @returns 删除响应信息
   */
  async deleteData(datasetId: string, deleteParams?: DeleteDataRequest): Promise<DeleteDataResponse> {
    try {
      this.validateDatasetId(datasetId)
      if (deleteParams) {
        this.validateDeleteDataRequest(deleteParams)
      }
      
      const response = await trainingData.deleteData(datasetId, deleteParams)
      
      return {
        datasetId: response.datasetId,
        deletedAt: response.deletedAt,
        deletedBy: response.deletedBy,
        fileDeleted: response.fileDeleted,
        metadataPreserved: response.metadataPreserved
      }
    } catch (error) {
      throw this.handleServiceError(error, `删除训练数据失败 (ID: ${datasetId})`)
    }
  }

  // ==================== 批量操作管理 ====================

  /**
   * 批量数据操作
   * @param batchData 批量操作数据
   * @returns 批量操作结果
   */
  async batchOperation(batchData: BatchOperationRequest): Promise<BatchOperationResult> {
    try {
      this.validateBatchOperationRequest(batchData)
      
      const result = await trainingData.batchOperation(batchData)
      return this.transformBatchOperationResult(result)
    } catch (error) {
      throw this.handleServiceError(error, '批量数据操作失败')
    }
  }

  // ==================== 数据统计管理 ====================

  /**
   * 获取数据统计
   * @param params 统计查询参数
   * @returns 数据统计信息
   */
  async getDataStatistics(params: DataStatisticsParams = {}): Promise<DataStatistics> {
    try {
      this.validateDataStatisticsParams(params)
      
      const statistics = await trainingData.getDataStatistics(params)
      return this.transformDataStatistics(statistics)
    } catch (error) {
      throw this.handleServiceError(error, '获取数据统计失败')
    }
  }

  /**
   * 导出数据
   * @param exportData 导出参数
   * @returns 导出任务信息
   */
  async exportData(exportData: ExportDataRequest): Promise<ExportTask> {
    try {
      this.validateExportDataRequest(exportData)
      
      const task = await trainingData.exportData(exportData)
      return this.transformExportTask(task)
    } catch (error) {
      throw this.handleServiceError(error, '数据导出失败')
    }
  }

  // ==================== 私有验证方法 ====================

  /**
   * 验证数据集ID
   */
  private validateDatasetId(datasetId: string): void {
    if (!datasetId || typeof datasetId !== 'string' || datasetId.trim().length === 0) {
      throw new Error('数据集ID不能为空')
    }
    
    // 验证UUID格式（32位十六进制字符）
    const uuidRegex = /^[a-f0-9]{32}$/i
    if (!uuidRegex.test(datasetId)) {
      throw new Error('数据集ID格式不正确，应为32位UUID格式')
    }
  }

  /**
   * 验证文件上传
   */
  private validateUploadFile(formData: FormData): void {
    if (!formData || !(formData instanceof FormData)) {
      throw new Error('上传数据格式不正确')
    }
    
    const vmId = formData.get('vmId')
    if (!vmId || typeof vmId !== 'string' || vmId.trim().length === 0) {
      throw new Error('水下机器人ID不能为空')
    }
    
    // 验证vmId格式
    const uuidRegex = /^[a-f0-9]{32}$/i
    if (!uuidRegex.test(vmId)) {
      throw new Error('水下机器人ID格式不正确')
    }
    
    const dataType = formData.get('dataType')
    if (!dataType || typeof dataType !== 'string') {
      throw new Error('数据类型不能为空')
    }
    
    const validDataTypes = ['ACOUSTIC', 'ENVIRONMENT', 'MODEL', 'FEATURE', 'OTHER']
    if (!validDataTypes.includes(dataType)) {
      throw new Error('数据类型无效')
    }
    
    const file = formData.get('file')
    if (!file || !(file instanceof File)) {
      throw new Error('上传文件不能为空')
    }
    
    // 验证文件大小（100MB限制）
    const maxSize = 100 * 1024 * 1024
    if (file.size > maxSize) {
      throw new Error('文件大小不能超过100MB')
    }
  }

  /**
   * 验证文本上传
   */
  private validateUploadText(textData: UploadTextRequest): void {
    if (!textData.vmId || typeof textData.vmId !== 'string' || textData.vmId.trim().length === 0) {
      throw new Error('水下机器人ID不能为空')
    }
    
    // 验证vmId格式
    const uuidRegex = /^[a-f0-9]{32}$/i
    if (!uuidRegex.test(textData.vmId)) {
      throw new Error('水下机器人ID格式不正确')
    }
    
    if (!textData.dataType || typeof textData.dataType !== 'string') {
      throw new Error('数据类型不能为空')
    }
    
    const validDataTypes = ['ACOUSTIC', 'ENVIRONMENT', 'MODEL', 'FEATURE', 'OTHER']
    if (!validDataTypes.includes(textData.dataType)) {
      throw new Error('数据类型无效')
    }
    
    if (!textData.title || typeof textData.title !== 'string' || textData.title.trim().length === 0) {
      throw new Error('标题不能为空')
    }
    
    if (textData.title.length > 200) {
      throw new Error('标题不能超过200个字符')
    }
    
    if (!textData.content || typeof textData.content !== 'string' || textData.content.trim().length === 0) {
      throw new Error('内容不能为空')
    }
    
    if (textData.content.length > 1000000) { // 1MB文本限制
      throw new Error('内容不能超过1MB')
    }
    
    if (textData.description && textData.description.length > 1000) {
      throw new Error('描述不能超过1000个字符')
    }
    
    if (textData.tags && (!Array.isArray(textData.tags) || textData.tags.length > 20)) {
      throw new Error('标签数量不能超过20个')
    }
  }

  /**
   * 验证数据列表查询参数
   */
  private validateDataListParams(params: DataListParams): void {
    if (params.page !== undefined && params.page < 1) {
      throw new Error('页码必须大于0')
    }
    
    if (params.size !== undefined && (params.size < 1 || params.size > 100)) {
      throw new Error('每页大小必须在1-100范围内')
    }
    
    if (params.vmId !== undefined && typeof params.vmId === 'string') {
      const uuidRegex = /^[a-f0-9]{32}$/i
      if (!uuidRegex.test(params.vmId)) {
        throw new Error('水下机器人ID格式不正确')
      }
    }
    
    if (params.dataType !== undefined) {
      const validDataTypes = ['ACOUSTIC', 'ENVIRONMENT', 'MODEL', 'FEATURE', 'OTHER']
      if (!validDataTypes.includes(params.dataType)) {
        throw new Error('数据类型参数无效')
      }
    }
    
    if (params.status !== undefined) {
      const validStatuses = ['UPLOADING', 'PROCESSING', 'VALIDATING', 'READY', 'ERROR', 'DELETED']
      if (!validStatuses.includes(params.status)) {
        throw new Error('状态参数无效')
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
   * 验证预处理请求
   */
  private validatePreprocessRequest(data: PreprocessRequest): void {
    if (!data.methods || !Array.isArray(data.methods) || data.methods.length === 0) {
      throw new Error('预处理方法不能为空')
    }
    
    const validMethods = ['normalization', 'feature_selection', 'outlier_removal', 'data_cleaning', 'dimensionality_reduction']
    data.methods.forEach(method => {
      if (!validMethods.includes(method)) {
        throw new Error(`预处理方法 ${method} 无效`)
      }
    })
    
    if (!data.parameters || typeof data.parameters !== 'object') {
      throw new Error('预处理参数不能为空')
    }
    
    if (data.outputFormat && !['csv', 'json', 'parquet', 'hdf5'].includes(data.outputFormat)) {
      throw new Error('输出格式无效')
    }
  }

  /**
   * 验证验证请求
   */
  private validateValidationRequest(data: ValidationRequest): void {
    if (data.dataType !== undefined) {
      const validDataTypes = ['ACOUSTIC', 'ENVIRONMENT', 'MODEL', 'FEATURE', 'OTHER']
      if (!validDataTypes.includes(data.dataType)) {
        throw new Error('数据类型无效')
      }
    }
    
    if (data.requiredColumns && !Array.isArray(data.requiredColumns)) {
      throw new Error('必需列必须为数组格式')
    }
    
    if (data.dataTypes && typeof data.dataTypes !== 'object') {
      throw new Error('数据类型映射必须为对象格式')
    }
    
    if (data.constraints && typeof data.constraints !== 'object') {
      throw new Error('约束条件必须为对象格式')
    }
    
    if (data.qualityChecks && !Array.isArray(data.qualityChecks)) {
      throw new Error('质量检查项必须为数组格式')
    }
  }

  /**
   * 验证更新数据请求
   */
  private validateUpdateDataRequest(data: UpdateDataRequest): void {
    if (data.datasetDescription && typeof data.datasetDescription !== 'string') {
      throw new Error('数据集描述必须为字符串')
    }
    
    if (data.datasetDescription && data.datasetDescription.length > 1000) {
      throw new Error('数据集描述不能超过1000个字符')
    }
    
    if (data.tags && (!Array.isArray(data.tags) || data.tags.length > 20)) {
      throw new Error('标签数量不能超过20个')
    }
    
    if (data.metadata && typeof data.metadata !== 'object') {
      throw new Error('元数据必须为对象格式')
    }
  }

  /**
   * 验证删除数据请求
   */
  private validateDeleteDataRequest(data: DeleteDataRequest): void {
    if (data.reason && typeof data.reason !== 'string') {
      throw new Error('删除原因必须为字符串')
    }
    
    if (data.deleteFile !== undefined && typeof data.deleteFile !== 'boolean') {
      throw new Error('删除文件标志必须为布尔值')
    }
    
    if (data.deleteMetadata !== undefined && typeof data.deleteMetadata !== 'boolean') {
      throw new Error('删除元数据标志必须为布尔值')
    }
  }

  /**
   * 验证批量操作请求
   */
  private validateBatchOperationRequest(data: BatchOperationRequest): void {
    if (!data.operation || typeof data.operation !== 'string') {
      throw new Error('操作类型不能为空')
    }
    
    const validOperations = ['DELETE', 'UPDATE', 'VALIDATE']
    if (!validOperations.includes(data.operation)) {
      throw new Error('操作类型无效')
    }
    
    if (!data.datasetIds || !Array.isArray(data.datasetIds) || data.datasetIds.length === 0) {
      throw new Error('数据集ID列表不能为空')
    }
    
    if (data.datasetIds.length > 100) {
      throw new Error('批量操作数据集数量不能超过100个')
    }
    
    // 验证每个数据集ID格式
    const uuidRegex = /^[a-f0-9]{32}$/i
    data.datasetIds.forEach(id => {
      if (!uuidRegex.test(id)) {
        throw new Error(`数据集ID ${id} 格式不正确`)
      }
    })
  }

  /**
   * 验证数据统计参数
   */
  private validateDataStatisticsParams(params: DataStatisticsParams): void {
    if (params.vmId !== undefined && typeof params.vmId === 'string') {
      const uuidRegex = /^[a-f0-9]{32}$/i
      if (!uuidRegex.test(params.vmId)) {
        throw new Error('水下机器人ID格式不正确')
      }
    }
    
    if (params.dataType !== undefined) {
      const validDataTypes = ['ACOUSTIC', 'ENVIRONMENT', 'MODEL', 'FEATURE', 'OTHER']
      if (!validDataTypes.includes(params.dataType)) {
        throw new Error('数据类型参数无效')
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
   * 验证导出数据请求
   */
  private validateExportDataRequest(data: ExportDataRequest): void {
    if (!data.exportType || typeof data.exportType !== 'string') {
      throw new Error('导出类型不能为空')
    }
    
    const validExportTypes = ['CSV', 'JSON', 'EXCEL']
    if (!validExportTypes.includes(data.exportType)) {
      throw new Error('导出类型无效')
    }
    
    if (data.format && !['ZIP', 'TAR'].includes(data.format)) {
      throw new Error('导出格式无效')
    }
    
    if (data.fields && !Array.isArray(data.fields)) {
      throw new Error('导出字段必须为数组格式')
    }
    
    if (data.filters) {
      if (data.filters.dataType && !['ACOUSTIC', 'ENVIRONMENT', 'MODEL', 'FEATURE', 'OTHER'].includes(data.filters.dataType)) {
        throw new Error('过滤数据类型无效')
      }
      
      if (data.filters.status && !['UPLOADING', 'PROCESSING', 'VALIDATING', 'READY', 'ERROR', 'DELETED'].includes(data.filters.status)) {
        throw new Error('过滤状态无效')
      }
      
      if (data.filters.vmId && typeof data.filters.vmId === 'string') {
        const uuidRegex = /^[a-f0-9]{32}$/i
        if (!uuidRegex.test(data.filters.vmId)) {
          throw new Error('过滤水下机器人ID格式不正确')
        }
      }
    }
  }

  // ==================== 私有转换方法 ====================

  /**
   * 转换数据项
   */
  private transformDataItem(item: any): any {
    return {
      datasetId: item.datasetId,
      datasetDescription: item.datasetDescription,
      datasetType: item.datasetType,
      vmId: item.vmId,
      status: item.status,
      tags: item.tags || []
    }
  }

  /**
   * 转换数据详情
   */
  private transformDataDetail(detail: any): DatasetDetail {
    return {
      ...detail,
      metadata: detail.metadata || {},
      validation: detail.validation || {
        isValid: false,
        validationTime: '',
        errors: [],
        warnings: []
      },
      preprocessing: detail.preprocessing || {
        isProcessed: false,
        processTime: '',
        methods: [],
        parameters: {}
      }
    }
  }

  /**
   * 转换预处理任务
   */
  private transformPreprocessTask(task: any): PreprocessTask {
    return {
      datasetId: task.datasetId,
      taskId: task.taskId,
      status: task.status,
      methods: task.methods,
      startedAt: task.startedAt,
      estimatedTime: task.estimatedTime
    }
  }

  /**
   * 转换验证结果
   */
  private transformValidationResult(result: any): ValidationResult {
    return {
      datasetId: result.datasetId,
      isValid: result.isValid,
      validationTime: result.validationTime,
      results: result.results,
      errors: result.errors || [],
      warnings: result.warnings || []
    }
  }

  /**
   * 转换批量操作结果
   */
  private transformBatchOperationResult(result: any): BatchOperationResult {
    return {
      operation: result.operation,
      total: result.total,
      success: result.success,
      failed: result.failed,
      results: result.results
    }
  }

  /**
   * 转换数据统计
   */
  private transformDataStatistics(statistics: any): DataStatistics {
    return {
      totalCount: statistics.totalCount,
      totalSize: statistics.totalSize,
      dataTypeDistribution: statistics.dataTypeDistribution,
      statusDistribution: statistics.statusDistribution,
      vmDistribution: statistics.vmDistribution,
      uploadTrend: statistics.uploadTrend,
      topDataTypes: statistics.topDataTypes
    }
  }

  /**
   * 转换导出任务
   */
  private transformExportTask(task: any): ExportTask {
    return {
      taskId: task.taskId,
      status: task.status,
      format: task.format,
      startedAt: task.startedAt,
      estimatedTime: task.estimatedTime,
      downloadUrl: task.downloadUrl
    }
  }

  /**
   * 统一错误处理
   */
  private handleServiceError(error: any, message: string): Error {
    console.error(`[TrainingDataService] ${message}:`, error)
    
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
export const trainingDataService = new TrainingDataService()

// 导出默认实例
export default trainingDataService
