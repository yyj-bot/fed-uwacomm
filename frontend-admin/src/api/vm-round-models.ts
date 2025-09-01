import axios, { type AxiosResponse } from 'axios'
import type { 
  ApiResponse, 
  PaginatedResponse,
  PaginationParams
} from '@/types'

// 创建本地模型API实例
const vmRoundModelsApi = axios.create({
  baseURL: 'http://localhost:8080/api/model',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器
vmRoundModelsApi.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('access_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
vmRoundModelsApi.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<unknown>>) => {
    if (response.data.code !== 200) {
      throw new Error(response.data.message || '请求失败')
    }
    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('access_token')
      localStorage.removeItem('refresh_token')
      window.location.href = '/login'
    }
    console.error('VM Round Models API Error:', error)
    throw error
  }
)

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
    const response = await vmRoundModelsApi.get<ApiResponse<any>>('/vm-round-models', { params })
    return response.data.data
  },

  // ==================== 2.2 本地模型结果详情查询 ====================
  async getVMRoundModelDetail(vmRoundModelId: string): Promise<VMRoundModel> {
    const response = await vmRoundModelsApi.get<ApiResponse<VMRoundModel>>(`/vm-round-models/${vmRoundModelId}`)
    return response.data.data
  },

  // ==================== 2.3 本地模型训练指标趋势 ====================
  async getVMModelTrend(params: {
    taskId: string
    vmId: string
    metric: string
  }): Promise<VMModelTrend> {
    const response = await vmRoundModelsApi.get<ApiResponse<VMModelTrend>>('/vm-round-models/metrics/trend', { params })
    return response.data.data
  },

  // ==================== 2.4 本地模型最佳/离群查询 ====================
  async getVMModelBest(params: {
    taskId: string
    metric: string
    type: 'best' | 'outlier'
  }): Promise<VMModelBest> {
    const response = await vmRoundModelsApi.get<ApiResponse<VMModelBest>>('/vm-round-models/metrics/best', { params })
    return response.data.data
  },
} as const

export default vmRoundModels

