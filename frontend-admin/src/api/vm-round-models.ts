import { createApiInstance } from './base'
import type { 
  ApiResponse, 
  PaginatedResponse,
  PaginationParams
} from '@/types'

// 创建虚拟机轮次模型API实例
const vmRoundModelsApiInstance = createApiInstance('MODEL')

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
}

// ==================== 水声联邦学习系统本地模型API ====================
export const vmRoundModels = {
  // ==================== 2.1 本地模型结果分页查询 ====================
  async getVMRoundModels(params: PaginationParams & {
    taskId?: string
    roundNumber?: number
    vmId?: string
  } = {}): Promise<any> {
    const response = await vmRoundModelsApiInstance.get<ApiResponse<any>>('/vm-round-models', { params })
    return response.data.data
  },

  // ==================== 2.2 本地模型结果详情查询 ====================
  async getVMRoundModelDetail(vmRoundModelId: string): Promise<VMRoundModel> {
    const response = await vmRoundModelsApiInstance.get<ApiResponse<VMRoundModel>>(`/vm-round-models/${vmRoundModelId}`)
    return response.data.data
  },

  // ==================== 2.3 本地模型训练指标趋势 ====================
  async getVMModelTrend(params: {
    taskId: string
    vmId: string
    metric: string
  }): Promise<VMModelTrend> {
    const response = await vmRoundModelsApiInstance.get<ApiResponse<VMModelTrend>>('/vm-round-models/metrics/trend', { params })
    return response.data.data
  },

  // ==================== 2.4 本地模型最佳/离群查询 ====================
  async getVMModelBest(params: {
    taskId: string
    metric: string
    type: 'best' | 'outlier'
  }): Promise<VMModelBest> {
    const response = await vmRoundModelsApiInstance.get<ApiResponse<VMModelBest>>('/vm-round-models/metrics/best', { params })
    return response.data.data
  },
} as const

// 使用命名导出以保持一致性

