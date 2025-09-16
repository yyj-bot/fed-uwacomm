/**
 * VM本地模型服务层 - 企业级规范实现
 * 提供VM本地模型管理相关的业务逻辑处理
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { vmRoundModels } from '@/api/vm-round-models'
import type { 
  VMRoundModel,
  VMModelTrend,
  VMModelBest,
  VMRoundModelListParams,
  VMModelTrendParams,
  VMModelBestParams,
  VMRoundModelPaginatedResponse,
  MetricType,
  QueryType
} from './type'

/**
 * VM本地模型服务类
 */
export class VMRoundModelsService {
  // ==================== VM本地模型查询管理 ====================

  /**
   * 获取VM本地模型列表
   * @param params 查询参数
   * @returns 分页VM本地模型列表
   */
  async getVMRoundModels(params: VMRoundModelListParams = {}): Promise<VMRoundModelPaginatedResponse<VMRoundModel>> {
    try {
      this.validateVMRoundModelListParams(params)
      
      const result = await vmRoundModels.getVMRoundModels(params)
      return this.transformVMRoundModelList(result)
    } catch (error) {
      throw this.handleServiceError(error, '获取VM本地模型列表失败')
    }
  }

  /**
   * 获取VM本地模型详情
   * @param vmRoundModelId VM本地模型ID
   * @returns VM本地模型详细信息
   */
  async getVMRoundModelDetail(vmRoundModelId: string): Promise<VMRoundModel> {
    try {
      this.validateVMRoundModelId(vmRoundModelId)
      
      const detail = await vmRoundModels.getVMRoundModelDetail(vmRoundModelId)
      return this.transformVMRoundModel(detail)
    } catch (error) {
      throw this.handleServiceError(error, `获取VM本地模型详情失败 (ID: ${vmRoundModelId})`)
    }
  }

  /**
   * 获取VM模型训练指标趋势
   * @param params 趋势查询参数
   * @returns VM模型训练指标趋势
   */
  async getVMModelTrend(params: VMModelTrendParams): Promise<VMModelTrend> {
    try {
      this.validateVMModelTrendParams(params)
      
      const trend = await vmRoundModels.getVMModelTrend(params)
      return this.transformVMModelTrend(trend)
    } catch (error) {
      throw this.handleServiceError(error, `获取VM模型训练指标趋势失败 (TaskID: ${params.taskId}, VMID: ${params.vmId})`)
    }
  }

  /**
   * 获取VM模型最佳/离群结果
   * @param params 最佳/离群查询参数
   * @returns VM模型最佳/离群结果
   */
  async getVMModelBest(params: VMModelBestParams): Promise<VMModelBest> {
    try {
      this.validateVMModelBestParams(params)
      
      const best = await vmRoundModels.getVMModelBest(params)
      return this.transformVMModelBest(best)
    } catch (error) {
      throw this.handleServiceError(error, `获取VM模型${params.type === 'best' ? '最佳' : '离群'}结果失败 (TaskID: ${params.taskId})`)
    }
  }

  // ==================== 私有验证方法 ====================

  /**
   * 验证VM本地模型ID
   */
  private validateVMRoundModelId(vmRoundModelId: string): void {
    if (!vmRoundModelId || typeof vmRoundModelId !== 'string' || vmRoundModelId.trim().length === 0) {
      throw new Error('VM本地模型ID不能为空')
    }
    
    // 验证ID格式（以vmrm-开头）
    if (!vmRoundModelId.startsWith('vmrm-')) {
      throw new Error('VM本地模型ID格式不正确，应以vmrm-开头')
    }
  }

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
   * 验证虚拟机ID
   */
  private validateVMId(vmId: string): void {
    if (!vmId || typeof vmId !== 'string' || vmId.trim().length === 0) {
      throw new Error('虚拟机ID不能为空')
    }
    
    // 验证UUID格式（32位十六进制字符）
    const uuidRegex = /^[a-f0-9]{32}$/i
    if (!uuidRegex.test(vmId)) {
      throw new Error('虚拟机ID格式不正确，应为32位UUID格式')
    }
  }

  /**
   * 验证指标名称
   */
  private validateMetric(metric: string): void {
    if (!metric || typeof metric !== 'string' || metric.trim().length === 0) {
      throw new Error('指标名称不能为空')
    }
    
    const validMetrics = ['accuracy', 'loss', 'precision', 'recall', 'f1']
    if (!validMetrics.includes(metric)) {
      throw new Error('不支持的指标类型')
    }
  }

  /**
   * 验证VM本地模型列表参数
   */
  private validateVMRoundModelListParams(params: VMRoundModelListParams): void {
    if (params.page !== undefined && params.page < 1) {
      throw new Error('页码必须大于0')
    }
    
    if (params.size !== undefined && (params.size < 1 || params.size > 100)) {
      throw new Error('每页大小必须在1-100范围内')
    }
    
    if (params.taskId !== undefined && typeof params.taskId === 'string') {
      this.validateTaskId(params.taskId)
    }
    
    if (params.vmId !== undefined && typeof params.vmId === 'string') {
      this.validateVMId(params.vmId)
    }
    
    if (params.roundNumber !== undefined && (!Number.isInteger(params.roundNumber) || params.roundNumber <= 0)) {
      throw new Error('训练轮数必须为正整数')
    }
  }

  /**
   * 验证VM模型趋势参数
   */
  private validateVMModelTrendParams(params: VMModelTrendParams): void {
    this.validateTaskId(params.taskId)
    this.validateVMId(params.vmId)
    this.validateMetric(params.metric)
  }

  /**
   * 验证VM模型最佳/离群参数
   */
  private validateVMModelBestParams(params: VMModelBestParams): void {
    this.validateTaskId(params.taskId)
    this.validateMetric(params.metric)
    
    if (!['best', 'outlier'].includes(params.type)) {
      throw new Error('查询类型必须为best或outlier')
    }
  }

  // ==================== 私有转换方法 ====================

  /**
   * 转换VM本地模型列表
   */
  private transformVMRoundModelList(result: any): VMRoundModelPaginatedResponse<VMRoundModel> {
    return {
      total: result.total,
      pages: result.pages,
      current: result.current,
      size: result.size,
      records: result.records.map((item: any) => this.transformVMRoundModel(item))
    }
  }

  /**
   * 转换VM本地模型项
   */
  private transformVMRoundModel(item: any): VMRoundModel {
    return {
      vmRoundModelId: item.vmRoundModelId,
      taskId: item.taskId,
      roundNumber: item.roundNumber,
      vmId: item.vmId,
      modelJson: item.modelJson,
      metrics: {
        accuracy: item.metrics.accuracy,
        loss: item.metrics.loss,
        ...item.metrics
      },
      createdAt: item.createdAt
    }
  }

  /**
   * 转换VM模型趋势
   */
  private transformVMModelTrend(trend: any): VMModelTrend {
    return {
      taskId: trend.taskId,
      vmId: trend.vmId,
      metric: trend.metric,
      trend: trend.trend.map((item: any) => ({
        roundNumber: item.roundNumber,
        value: item.value
      }))
    }
  }

  /**
   * 转换VM模型最佳/离群结果
   */
  private transformVMModelBest(best: any): VMModelBest {
    return {
      taskId: best.taskId,
      metric: best.metric,
      type: best.type,
      result: {
        vmRoundModelId: best.result.vmRoundModelId,
        roundNumber: best.result.roundNumber,
        vmId: best.result.vmId,
        value: best.result.value
      }
    }
  }

  /**
   * 统一错误处理
   */
  private handleServiceError(error: any, message: string): Error {
    console.error(`[VMRoundModelsService] ${message}:`, error)
    
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
export const vmRoundModelsService = new VMRoundModelsService()

// 导出默认实例
export default vmRoundModelsService
