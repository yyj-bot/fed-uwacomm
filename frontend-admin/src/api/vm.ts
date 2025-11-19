import { createApiInstance } from './base'
import type { 
  ApiResponse, 
  PaginationParams,
  UnderwaterRobotSpecs
} from '@/types'

// 创建水下机器人API实例
const vmApiInstance = createApiInstance('VM')
// 创建模型API实例（用于本地模型相关接口）
const modelApiInstance = createApiInstance('MODEL')

// ==================== 类型定义 ====================

// 水下机器人基础信息类型
type VMSpeed = number | string | {
  value: number
  unit?: string
}

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
  batteryLevel?: number
  speed?: VMSpeed
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
  specs?: UnderwaterRobotSpecs
}

// 水下机器人状态类型
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
  specs?: UnderwaterRobotSpecs
}

// 本地模型结果类型
interface VMRoundModel {
  vmRoundModelId: string
  taskId: string
  roundNumber: number
  vmId: string
  modelJson?: Record<string, unknown>
  metrics: {
    accuracy: number
    loss: number
    [key: string]: number
  }
  createdAt: string
  specs?: UnderwaterRobotSpecs
}

// 本地模型训练指标趋势类型
interface VMModelTrend {
  taskId: string
  vmId: string
  metric: string
  trend: Array<{
    roundNumber: number
    value: number
  }>
  specs?: UnderwaterRobotSpecs
}

// 本地模型最佳/离群查询结果类型
interface VMModelBest {
  taskId: string
  metric: string
  type: 'best' | 'outlier'
  result: {
    vmRoundModelId: string
    roundNumber: number
    vmId: string
    value: number
  }
  specs?: UnderwaterRobotSpecs
}

// ==================== 水下机器人管理API ====================
export const vmApi = {

  // ==================== 4.1 水下机器人列表查询接口 ====================
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
    }>>('/vm/list', { params })
    return response.data.data
  },

  // ==================== 4.2 水下机器人详情查询接口 ====================
  async getVMDetail(vmId: string): Promise<VirtualMachine> {
    const response = await vmApiInstance.get<ApiResponse<VirtualMachine>>(`/vm/${vmId}`)
    return response.data.data
  },

  // ==================== 4.3 水下机器人更新接口 ====================
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
    }>>(`/vm/${vmId}`, vmData)
    return response.data.data
  },

  // ==================== 4.4 水下机器人删除接口 ====================
  async deleteVM(vmId: string, force?: boolean): Promise<{
    vmId: string
    deletedAt: string
  }> {
    const params = force ? { force: true } : {}
    const response = await vmApiInstance.delete<ApiResponse<{
      vmId: string
      deletedAt: string
    }>>(`/vm/${vmId}`, { params })
    return response.data.data
  },

  // ==================== 5.1 水下机器人启动接口 ====================
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
    }>>(`/vm/${vmId}/start`, startData)
    return response.data.data
  },

  // ==================== 5.2 水下机器人停止接口 ====================
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
    }>>(`/vm/${vmId}/stop`, stopData)
    return response.data.data
  },

  // ==================== 5.3 水下机器人重启接口 ====================
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
    }>>(`/vm/${vmId}/restart`, restartData)
    return response.data.data
  },

  // ==================== 6.1 水下机器人状态查询接口 ====================
  async getVMStatus(vmId: string): Promise<VMStatus> {
    const response = await vmApiInstance.get<ApiResponse<VMStatus>>(`/vm/${vmId}/status`)
    return response.data.data
  },

  // ==================== 本地模型（VM Round Models）接口 ====================
  
  // ==================== 2.1 本地模型结果分页查询 ====================
  async getVMRoundModels(params: PaginationParams & {
    taskId?: string
    roundNumber?: number
    vmId?: string
  } = {}): Promise<{
    total: number
    pages: number
    current: number
    size: number
    records: VMRoundModel[]
  }> {
    const response = await modelApiInstance.get<ApiResponse<{
      total: number
      pages: number
      current: number
      size: number
      records: VMRoundModel[]
    }>>('/vm-round-models', { params })
    return response.data.data
  },

  // ==================== 2.2 本地模型结果详情查询 ====================
  async getVMRoundModelDetail(vmRoundModelId: string): Promise<VMRoundModel> {
    const response = await modelApiInstance.get<ApiResponse<VMRoundModel>>(`/vm-round-models/${vmRoundModelId}`)
    return response.data.data
  },

  // ==================== 2.3 本地模型训练指标趋势 ====================
  async getVMModelTrend(params: {
    taskId: string
    vmId: string
    metric: string
  }): Promise<VMModelTrend> {
    const response = await modelApiInstance.get<ApiResponse<VMModelTrend>>('/vm-round-models/metrics/trend', { params })
    return response.data.data
  },

  // ==================== 2.4 本地模型最佳/离群查询 ====================
  async getVMModelBest(params: {
    taskId: string
    metric: string
    type: 'best' | 'outlier'
  }): Promise<VMModelBest> {
    const response = await modelApiInstance.get<ApiResponse<VMModelBest>>('/vm-round-models/metrics/best', { params })
    return response.data.data
  },
} as const

// 使用命名导出以保持一致性

// 导出类型定义
export type {
  VirtualMachine,
  VMStatus,
  VMRoundModel,
  VMModelTrend,
  VMModelBest
}