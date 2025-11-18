/**
 * 水下机器人服务层 - 企业级规范实现
 * 提供水下机器人管理相关的业务逻辑处理
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { vmApi } from '@/api/vm'
import type { PaginationParams } from '@/types'
import type { 
  VirtualMachine,
  VMStatus,
  VMRoundModel,
  VMModelTrend,
  VMModelBest
} from '@/api/vm'
import type { 
  VMListParams,
  VMUpdateRequest,
  VMStartRequest,
  VMStopRequest,
  VMRestartRequest,
  VMListResponse,
  VMUpdateResponse,
  VMDeleteResponse,
  VMStartResponse,
  VMStopResponse,
  VMRestartResponse,
  VMRoundModelPaginatedResponse,
  VMRoundModelListParams,
  VMModelTrendParams,
  VMModelBestParams,
  MetricType
} from './type'

/**
 * 水下机器人服务类
 */
export class VMService {

  // ==================== 水下机器人查询管理 ====================

  /**
   * 获取水下机器人列表
   * @param params 查询参数
   * @returns 分页水下机器人列表
   */
  async getVMList(params: VMListParams = {}): Promise<VMListResponse> {
    try {
      this.validateVMListParams(params)
      
      const result = await vmApi.getVMList(params)
      
      return {
        total: result.total,
        page: result.page,
        size: result.size,
        pages: result.pages,
        list: result.list.map(vm => this.transformVirtualMachine(vm))
      }
    } catch (error) {
      throw this.handleServiceError(error, '获取水下机器人列表失败')
    }
  }

  /**
   * 获取水下机器人详情
   * @param vmId 水下机器人ID
   * @returns 水下机器人详细信息
   */
  async getVMDetail(vmId: string): Promise<VirtualMachine> {
    try {
      this.validateVMId(vmId)
      
      const vm = await vmApi.getVMDetail(vmId)
      return this.transformVirtualMachine(vm)
    } catch (error) {
      throw this.handleServiceError(error, `获取水下机器人详情失败 (ID: ${vmId})`)
    }
  }

  /**
   * 更新水下机器人信息
   * @param vmId 水下机器人ID
   * @param vmData 更新数据
   * @returns 更新响应信息
   */
  async updateVM(vmId: string, vmData: VMUpdateRequest): Promise<VMUpdateResponse> {
    try {
      this.validateVMId(vmId)
      this.validateVMUpdateRequest(vmData)
      
      const response = await vmApi.updateVM(vmId, vmData)
      
      return {
        vmId: response.vmId,
        name: response.name,
        updatedAt: response.updatedAt
      }
    } catch (error) {
      throw this.handleServiceError(error, `更新水下机器人失败 (ID: ${vmId})`)
    }
  }

  /**
   * 删除水下机器人
   * @param vmId 水下机器人ID
   * @param force 是否强制删除
   * @returns 删除响应信息
   */
  async deleteVM(vmId: string, force: boolean = false): Promise<VMDeleteResponse> {
    try {
      this.validateVMId(vmId)
      
      const response = await vmApi.deleteVM(vmId, force)
      
      return {
        vmId: response.vmId,
        deletedAt: response.deletedAt
      }
    } catch (error) {
      throw this.handleServiceError(error, `删除水下机器人失败 (ID: ${vmId})`)
    }
  }

  // ==================== 水下机器人控制管理 ====================

  /**
   * 启动水下机器人
   * @param vmId 水下机器人ID
   * @param startData 启动参数
   * @returns 启动响应信息
   */
  async startVM(vmId: string, startData?: VMStartRequest): Promise<VMStartResponse> {
    try {
      this.validateVMId(vmId)
      if (startData) {
        this.validateVMStartRequest(startData)
      }
      
      const response = await vmApi.startVM(vmId, startData)
      
      return {
        vmId: response.vmId,
        status: response.status as VirtualMachine['status'],
        commandId: response.commandId,
        estimatedTime: response.estimatedTime
      }
    } catch (error) {
      throw this.handleServiceError(error, `启动水下机器人失败 (ID: ${vmId})`)
    }
  }

  /**
   * 停止水下机器人
   * @param vmId 水下机器人ID
   * @param stopData 停止参数
   * @returns 停止响应信息
   */
  async stopVM(vmId: string, stopData?: VMStopRequest): Promise<VMStopResponse> {
    try {
      this.validateVMId(vmId)
      if (stopData) {
        this.validateVMStopRequest(stopData)
      }
      
      const response = await vmApi.stopVM(vmId, stopData)
      
      return {
        vmId: response.vmId,
        status: response.status as VirtualMachine['status'],
        commandId: response.commandId,
        estimatedTime: response.estimatedTime
      }
    } catch (error) {
      throw this.handleServiceError(error, `停止水下机器人失败 (ID: ${vmId})`)
    }
  }

  /**
   * 重启水下机器人
   * @param vmId 水下机器人ID
   * @param restartData 重启参数
   * @returns 重启响应信息
   */
  async restartVM(vmId: string, restartData?: VMRestartRequest): Promise<VMRestartResponse> {
    try {
      this.validateVMId(vmId)
      if (restartData) {
        this.validateVMRestartRequest(restartData)
      }
      
      const response = await vmApi.restartVM(vmId, restartData)
      
      return {
        vmId: response.vmId,
        status: response.status as VirtualMachine['status'],
        commandId: response.commandId,
        estimatedTime: response.estimatedTime
      }
    } catch (error) {
      throw this.handleServiceError(error, `重启水下机器人失败 (ID: ${vmId})`)
    }
  }

  // ==================== 水下机器人状态管理 ====================

  /**
   * 获取水下机器人状态
   * @param vmId 水下机器人ID
   * @returns 水下机器人状态信息
   */
  async getVMStatus(vmId: string): Promise<VMStatus> {
    try {
      this.validateVMId(vmId)
      
      const status = await vmApi.getVMStatus(vmId)
      return this.transformVMStatus(status)
    } catch (error) {
      throw this.handleServiceError(error, `获取水下机器人状态失败 (ID: ${vmId})`)
    }
  }

  // ==================== VM本地模型管理 ====================

  /**
   * 获取VM本地模型列表
   * @param params 查询参数
   * @returns 分页VM本地模型列表
   */
  async getVMRoundModels(params: VMRoundModelListParams = {}): Promise<VMRoundModelPaginatedResponse<VMRoundModel>> {
    try {
      this.validateVMRoundModelListParams(params)
      
      const result = await vmApi.getVMRoundModels(params)
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
      
      const detail = await vmApi.getVMRoundModelDetail(vmRoundModelId)
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
      
      const trend = await vmApi.getVMModelTrend(params)
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
      
      const best = await vmApi.getVMModelBest(params)
      return this.transformVMModelBest(best)
    } catch (error) {
      throw this.handleServiceError(error, `获取VM模型${params.type === 'best' ? '最佳' : '离群'}结果失败 (TaskID: ${params.taskId})`)
    }
  }

  // ==================== 私有方法 ====================

  /**
   * 验证水下机器人ID
   */
  private validateVMId(vmId: string): void {
    if (!vmId || typeof vmId !== 'string' || vmId.trim().length === 0) {
      throw new Error('水下机器人ID不能为空')
    }
    
    // 验证UUID格式（32位十六进制字符）
    const uuidRegex = /^[a-f0-9]{32}$/i
    if (!uuidRegex.test(vmId)) {
      throw new Error('水下机器人ID格式不正确，应为32位UUID格式')
    }
  }

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
   * 验证水下机器人列表查询参数
   */
  private validateVMListParams(params: VMListParams): void {
    if (params.page !== undefined && (params.page < 1)) {
      throw new Error('页码必须大于0')
    }
    
    if (params.size !== undefined && (params.size < 1 || params.size > 100)) {
      throw new Error('每页大小必须在1-100范围内')
    }
    
    if (params.status !== undefined) {
      const validStatuses = ['RUNNING', 'STOPPED', 'STARTING', 'STOPPING', 'ERROR', 'OFFLINE']
      if (!validStatuses.includes(params.status)) {
        throw new Error('状态参数无效')
      }
    }
  }

  /**
   * 验证水下机器人更新请求
   */
  private validateVMUpdateRequest(data: VMUpdateRequest): void {
    if (data.name !== undefined) {
      if (!data.name || data.name.trim().length === 0) {
        throw new Error('水下机器人名称不能为空')
      }
      if (data.name.length > 100) {
        throw new Error('水下机器人名称不能超过100个字符')
      }
    }
    
    if (data.ipAddress !== undefined) {
      if (!data.ipAddress || !this.isValidIPAddress(data.ipAddress)) {
        throw new Error('IP地址格式不正确')
      }
    }
    
    if (data.port !== undefined) {
      if (data.port < 1 || data.port > 65535) {
        throw new Error('端口必须在1-65535范围内')
      }
    }
    
    if (data.cpuCores !== undefined && data.cpuCores < 1) {
      throw new Error('CPU核心数必须大于0')
    }
    
    if (data.memoryMb !== undefined && data.memoryMb < 1024) {
      throw new Error('内存必须大于等于1024MB')
    }
    
    if (data.diskGb !== undefined && data.diskGb < 20) {
      throw new Error('磁盘必须大于等于20GB')
    }
  }

  /**
   * 验证水下机器人启动请求
   */
  private validateVMStartRequest(data: VMStartRequest): void {
    if (data.timeout !== undefined && data.timeout < 0) {
      throw new Error('超时时间不能为负数')
    }
  }

  /**
   * 验证水下机器人停止请求
   */
  private validateVMStopRequest(data: VMStopRequest): void {
    if (data.timeout !== undefined && data.timeout < 0) {
      throw new Error('超时时间不能为负数')
    }
  }

  /**
   * 验证水下机器人重启请求
   */
  private validateVMRestartRequest(data: VMRestartRequest): void {
    if (data.timeout !== undefined && data.timeout < 0) {
      throw new Error('超时时间不能为负数')
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

  /**
   * 验证IP地址格式
   */
  private isValidIPAddress(ip: string): boolean {
    // IPv4格式验证
    const ipv4Regex = /^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/
    // IPv6格式验证（简化版）
    const ipv6Regex = /^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$/
    
    return ipv4Regex.test(ip) || ipv6Regex.test(ip)
  }

  /**
   * 转换水下机器人数据
   */
  private transformVirtualMachine(vm: any): VirtualMachine {
    return {
      vmId: vm.vmId,
      name: vm.name,
      ipAddress: vm.ipAddress,
      port: vm.port,
      status: vm.status,
      osType: vm.osType,
      cpuCores: vm.cpuCores,
      memoryMb: vm.memoryMb,
      diskGb: vm.diskGb,
      connectionStatus: vm.connectionStatus,
      lastHeartbeat: vm.lastHeartbeat,
      wsSessionId: vm.wsSessionId,
      createdAt: vm.createdAt,
      updatedAt: vm.updatedAt,
      systemInfo: vm.systemInfo,
      capabilities: vm.capabilities,
      networkConfig: vm.networkConfig,
      metadata: vm.metadata
    }
  }

  /**
   * 转换水下机器人状态数据
   */
  private transformVMStatus(status: any): VMStatus {
    return {
      vmId: status.vmId,
      status: status.status,
      connectionStatus: status.connectionStatus,
      uptime: status.uptime,
      resourceUsage: status.resourceUsage,
      network: status.network,
      processes: status.processes,
      lastHeartbeat: status.lastHeartbeat,
      wsSessionId: status.wsSessionId
    }
  }

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
    console.error(`[VMService] ${message}:`, error)
    
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
export const vmService = new VMService()

// 导出默认实例
export default vmService
