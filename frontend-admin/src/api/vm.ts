import { createApiInstance } from './base'
import type { 
  ApiResponse, 
  PaginationParams
} from '@/types'

// 创建虚拟机API实例
const vmApiInstance = createApiInstance('http://localhost:8080/api')

// ==================== 类型定义 ====================

// 虚拟机基础信息类型
interface VirtualMachine {
  vmId: string
  name: string
  ipAddress: string
  port: number
  status: 'RUNNING' | 'STOPPED' | 'STARTING' | 'STOPPING' | 'ERROR' | 'OFFLINE'
  osType: string
  cpuCores: number
  memoryMb: number
  diskGb: number
  connectionStatus: 'CONNECTED' | 'DISCONNECTED'
  lastHeartbeat?: string
  wsSessionId?: string
  createdAt: string
  updatedAt: string
  systemInfo?: {
    os?: string
    kernel?: string
    python?: string
    gpu?: string
    cuda?: string
    cudnn?: string
  }
  capabilities?: {
    supportedAlgorithms?: string[]
    maxBatchSize?: number
    maxMemoryUsage?: number
    gpuMemory?: number
    networkSpeed?: number
  }
  networkConfig?: {
    uploadSpeed?: number
    downloadSpeed?: number
    latency?: number
    bandwidth?: number
  }
  metadata?: {
    description?: string
    location?: string
    owner?: string
    department?: string
    tags?: string[]
  }
}

// 虚拟机状态类型
interface VMStatus {
  vmId: string
  status: 'RUNNING' | 'STOPPED' | 'STARTING' | 'STOPPING' | 'ERROR' | 'OFFLINE'
  connectionStatus: 'CONNECTED' | 'DISCONNECTED'
  uptime?: number
  resourceUsage?: {
    cpu: number
    memory: number
    disk: number
    gpu?: number
  }
  network?: {
    ipAddress: string
    macAddress?: string
    port: number
    uploadSpeed?: number
    downloadSpeed?: number
    latency?: number
  }
  processes?: {
    total: number
    active: number
    system: number
    user: number
  }
  lastHeartbeat?: string
  wsSessionId?: string
}



// ==================== 虚拟机管理API ====================
export const vmApi = {

  // ==================== 4.1 虚拟机列表查询接口 ====================
  async getVMList(params: PaginationParams & {
    status?: string
    osType?: string
    keyword?: string
  } = {}): Promise<{
    total: number
    page: number
    size: number
    pages: number
    list: VirtualMachine[]
  }> {
    const response = await vmApiInstance.get<ApiResponse<{
      total: number
      page: number
      size: number
      pages: number
      list: VirtualMachine[]
    }>>('/v1/vm/list', { params })
    return response.data.data
  },

  // ==================== 4.2 虚拟机详情查询接口 ====================
  async getVMDetail(vmId: string): Promise<VirtualMachine> {
    const response = await vmApiInstance.get<ApiResponse<VirtualMachine>>(`/v1/vm/${vmId}`)
    return response.data.data
  },

  // ==================== 4.3 虚拟机更新接口 ====================
  async updateVM(vmId: string, vmData: Partial<{
    name: string
    ipAddress: string
    port: number
    osType: string
    cpuCores: number
    memoryMb: number
    diskGb: number
    systemInfo: {
      os?: string
      kernel?: string
      python?: string
      gpu?: string
      cuda?: string
      cudnn?: string
    }
    capabilities: {
      supportedAlgorithms?: string[]
      maxBatchSize?: number
      maxMemoryUsage?: number
      gpuMemory?: number
      networkSpeed?: number
    }
    networkConfig: {
      uploadSpeed?: number
      downloadSpeed?: number
      latency?: number
      bandwidth?: number
    }
    metadata: {
      description?: string
      location?: string
      owner?: string
      department?: string
      tags?: string[]
    }
  }>): Promise<{
    vmId: string
    name: string
    updatedAt: string
  }> {
    const response = await vmApiInstance.put<ApiResponse<{
      vmId: string
      name: string
      updatedAt: string
    }>>(`/v1/vm/${vmId}`, vmData)
    return response.data.data
  },

  // ==================== 4.4 虚拟机删除接口 ====================
  async deleteVM(vmId: string, force?: boolean): Promise<{
    vmId: string
    deletedAt: string
  }> {
    const params = force ? { force: true } : {}
    const response = await vmApiInstance.delete<ApiResponse<{
      vmId: string
      deletedAt: string
    }>>(`/v1/vm/${vmId}`, { params })
    return response.data.data
  },

  // ==================== 5.1 虚拟机启动接口 ====================
  async startVM(vmId: string, startData?: {
    timeout?: number
    config?: {
      memory?: string
      cpu?: string
      disk?: string
      network?: string
    }
    environment?: {
      variables?: Record<string, string>
    }
  }): Promise<{
    vmId: string
    status: string
    commandId: string
    estimatedTime: number
  }> {
    const response = await vmApiInstance.post<ApiResponse<{
      vmId: string
      status: string
      commandId: string
      estimatedTime: number
    }>>(`/v1/vm/${vmId}/start`, startData)
    return response.data.data
  },

  // ==================== 5.2 虚拟机停止接口 ====================
  async stopVM(vmId: string, stopData?: {
    force?: boolean
    timeout?: number
    saveState?: boolean
  }): Promise<{
    vmId: string
    status: string
    commandId: string
    estimatedTime: number
  }> {
    const response = await vmApiInstance.post<ApiResponse<{
      vmId: string
      status: string
      commandId: string
      estimatedTime: number
    }>>(`/v1/vm/${vmId}/stop`, stopData)
    return response.data.data
  },

  // ==================== 5.3 虚拟机重启接口 ====================
  async restartVM(vmId: string, restartData?: {
    timeout?: number
    graceful?: boolean
    config?: {
      memory?: string
      cpu?: string
    }
  }): Promise<{
    vmId: string
    status: string
    commandId: string
    estimatedTime: number
  }> {
    const response = await vmApiInstance.post<ApiResponse<{
      vmId: string
      status: string
      commandId: string
      estimatedTime: number
    }>>(`/v1/vm/${vmId}/restart`, restartData)
    return response.data.data
  },

  // ==================== 6.1 虚拟机状态查询接口 ====================
  async getVMStatus(vmId: string): Promise<VMStatus> {
    const response = await vmApiInstance.get<ApiResponse<VMStatus>>(`/v1/vm/${vmId}/status`)
    return response.data.data
  },
} as const

// 使用命名导出以保持一致性

// 导出类型定义
export type {
  VirtualMachine,
  VMStatus
}