/**
 * 虚拟机服务层 - 企业级规范实现
 * 提供虚拟机管理相关的业务逻辑处理
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { vmApi } from '@/api/vm'
import type { PaginationParams } from '@/types'
import type { 
  VirtualMachine,
  VMStatus
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
  VMRestartResponse
} from './type'

/**
 * 虚拟机服务类
 */
export class VMService {

  // ==================== 虚拟机查询管理 ====================

  /**
   * 获取虚拟机列表
   * @param params 查询参数
   * @returns 分页虚拟机列表
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
      throw this.handleServiceError(error, '获取虚拟机列表失败')
    }
  }

  /**
   * 获取虚拟机详情
   * @param vmId 虚拟机ID
   * @returns 虚拟机详细信息
   */
  async getVMDetail(vmId: string): Promise<VirtualMachine> {
    try {
      this.validateVMId(vmId)
      
      const vm = await vmApi.getVMDetail(vmId)
      return this.transformVirtualMachine(vm)
    } catch (error) {
      throw this.handleServiceError(error, `获取虚拟机详情失败 (ID: ${vmId})`)
    }
  }

  /**
   * 更新虚拟机信息
   * @param vmId 虚拟机ID
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
      throw this.handleServiceError(error, `更新虚拟机失败 (ID: ${vmId})`)
    }
  }

  /**
   * 删除虚拟机
   * @param vmId 虚拟机ID
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
      throw this.handleServiceError(error, `删除虚拟机失败 (ID: ${vmId})`)
    }
  }

  // ==================== 虚拟机控制管理 ====================

  /**
   * 启动虚拟机
   * @param vmId 虚拟机ID
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
      throw this.handleServiceError(error, `启动虚拟机失败 (ID: ${vmId})`)
    }
  }

  /**
   * 停止虚拟机
   * @param vmId 虚拟机ID
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
      throw this.handleServiceError(error, `停止虚拟机失败 (ID: ${vmId})`)
    }
  }

  /**
   * 重启虚拟机
   * @param vmId 虚拟机ID
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
      throw this.handleServiceError(error, `重启虚拟机失败 (ID: ${vmId})`)
    }
  }

  // ==================== 虚拟机状态管理 ====================

  /**
   * 获取虚拟机状态
   * @param vmId 虚拟机ID
   * @returns 虚拟机状态信息
   */
  async getVMStatus(vmId: string): Promise<VMStatus> {
    try {
      this.validateVMId(vmId)
      
      const status = await vmApi.getVMStatus(vmId)
      return this.transformVMStatus(status)
    } catch (error) {
      throw this.handleServiceError(error, `获取虚拟机状态失败 (ID: ${vmId})`)
    }
  }

  // ==================== 私有方法 ====================

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
   * 验证虚拟机列表查询参数
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
   * 验证虚拟机更新请求
   */
  private validateVMUpdateRequest(data: VMUpdateRequest): void {
    if (data.name !== undefined) {
      if (!data.name || data.name.trim().length === 0) {
        throw new Error('虚拟机名称不能为空')
      }
      if (data.name.length > 100) {
        throw new Error('虚拟机名称不能超过100个字符')
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
   * 验证虚拟机启动请求
   */
  private validateVMStartRequest(data: VMStartRequest): void {
    if (data.timeout !== undefined && data.timeout < 0) {
      throw new Error('超时时间不能为负数')
    }
  }

  /**
   * 验证虚拟机停止请求
   */
  private validateVMStopRequest(data: VMStopRequest): void {
    if (data.timeout !== undefined && data.timeout < 0) {
      throw new Error('超时时间不能为负数')
    }
  }

  /**
   * 验证虚拟机重启请求
   */
  private validateVMRestartRequest(data: VMRestartRequest): void {
    if (data.timeout !== undefined && data.timeout < 0) {
      throw new Error('超时时间不能为负数')
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
   * 转换虚拟机数据
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
   * 转换虚拟机状态数据
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
