import axios, { type AxiosResponse } from 'axios'
import type { 
  ApiResponse, 
  EnvironmentFeature, 
  ModelMetrics, 
  FederatedRound, 
  OFDMPerformance 
} from '@/types'

// 创建API实例，严格配置
const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 响应拦截器 - 统一错误处理
api.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<unknown>>) => response,
  (error) => {
    console.error('API Error:', error)
    throw error
  }
)

// 环境数据API - 高内聚，单一职责
export const environmentApi = {
  // 获取环境特征数据
  async getEnvironmentFeatures(): Promise<EnvironmentFeature[]> {
    const response = await api.get<ApiResponse<EnvironmentFeature[]>>('/environment/features')
    return response.data.data
  },

  // 获取声速剖面统计
  async getSoundSpeedStats(): Promise<Record<string, number>> {
    const response = await api.get<ApiResponse<Record<string, number>>>('/environment/sound-speed-stats')
    return response.data.data
  },
} as const

// 模型性能API
export const modelApi = {
  // 获取模型性能指标
  async getModelMetrics(modelId: string): Promise<ModelMetrics> {
    const response = await api.get<ApiResponse<ModelMetrics>>(`/models/${modelId}/metrics`)
    return response.data.data
  },

  // 获取所有模型列表
  async getAllModels(): Promise<ModelMetrics[]> {
    const response = await api.get<ApiResponse<ModelMetrics[]>>('/models')
    return response.data.data
  },

  // 获取特征重要性数据
  async getFeatureImportance(modelId: string): Promise<Array<{ feature: string; importance: number }>> {
    const response = await api.get<ApiResponse<Array<{ feature: string; importance: number }>>>(`/models/${modelId}/feature-importance`)
    return response.data.data
  },
} as const

// 联邦学习API
export const federatedApi = {
  // 获取联邦学习轮次数据
  async getFederatedRounds(sessionId: string): Promise<FederatedRound[]> {
    const response = await api.get<ApiResponse<FederatedRound[]>>(`/federated/${sessionId}/rounds`)
    return response.data.data
  },

  // 获取客户端状态
  async getClientStatus(): Promise<Array<{ clientId: string; status: string; lastUpdate: string }>> {
    const response = await api.get<ApiResponse<Array<{ clientId: string; status: string; lastUpdate: string }>>>('/federated/clients/status')
    return response.data.data
  },
} as const

// OFDM通信API
export const ofdmApi = {
  // 获取OFDM性能数据
  async getOFDMPerformance(): Promise<OFDMPerformance> {
    const response = await api.get<ApiResponse<OFDMPerformance>>('/ofdm/performance')
    return response.data.data
  },

  // 获取BER曲线数据
  async getBERCurve(): Promise<{ snr: number[]; ber: number[] }> {
    const response = await api.get<ApiResponse<{ snr: number[]; ber: number[] }>>('/ofdm/ber-curve')
    return response.data.data
  },
} as const

// 系统监控API
export const systemApi = {
  // 获取系统健康状态
  async getSystemHealth(): Promise<{ status: string; uptime: number; memory: number; cpu: number }> {
    const response = await api.get<ApiResponse<{ status: string; uptime: number; memory: number; cpu: number }>>('/health')
    return response.data.data
  },
} as const 