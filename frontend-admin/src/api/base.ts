/**
 * API基础配置和拦截器
 * 统一管理所有API实例的通用配置，消除重复代码
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import axios, { type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import type { ApiResponse } from '@/types'

// ==================== API 配置常量 ====================
/**
 * API基础配置
 */
export const API_CONFIG = {
  // 后端服务基础URL
  BASE_URL: 'http://localhost:8080',
  
  // API路径前缀
  API_PREFIX: '/api',
  
  // 各模块的API路径
  ENDPOINTS: {
    USER: '/user',
    ADMIN: '/admin', 
    VM: '',  // vm接口直接在/api下
    FEDERATED: '/federated',
    MODEL: '/model',
    TRAINING_DATA: '/training-data',
    LOG: '/log'
  },
  
  // 请求超时时间
  TIMEOUT: 30000
} as const

/**
 * 获取完整的API端点URL
 * @param endpoint 端点路径
 * @returns 完整的API URL
 */
export function getApiUrl(endpoint: keyof typeof API_CONFIG.ENDPOINTS): string {
  return `${API_CONFIG.BASE_URL}${API_CONFIG.API_PREFIX}${API_CONFIG.ENDPOINTS[endpoint]}`
}

/**
 * 创建标准化的API实例
 * @param endpoint API端点类型或自定义URL
 * @param timeout 请求超时时间（毫秒）
 * @returns 配置好的Axios实例
 */
export function createApiInstance(
  endpoint: keyof typeof API_CONFIG.ENDPOINTS | string, 
  timeout: number = API_CONFIG.TIMEOUT
): AxiosInstance {
  // 如果是预定义的端点，使用getApiUrl获取URL；否则直接使用传入的URL
  const baseURL = typeof endpoint === 'string' && endpoint.startsWith('http') 
    ? endpoint 
    : getApiUrl(endpoint as keyof typeof API_CONFIG.ENDPOINTS)
    
  const instance = axios.create({
    baseURL,
    timeout,
    headers: {
      'Content-Type': 'application/json',
    },
  })

  // 添加统一的请求拦截器
  setupRequestInterceptor(instance)
  
  // 添加统一的响应拦截器
  setupResponseInterceptor(instance)

  return instance
}

/**
 * 设置请求拦截器
 * @param instance Axios实例
 */
function setupRequestInterceptor(instance: AxiosInstance): void {
  instance.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
      // 添加认证头
      const token = localStorage.getItem('access_token')
      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`
      }
      
      return config
    },
    (error) => Promise.reject(error)
  )
}

/**
 * 设置响应拦截器
 * @param instance Axios实例
 */
function setupResponseInterceptor(instance: AxiosInstance): void {
  instance.interceptors.response.use(
    (response: AxiosResponse<ApiResponse<unknown>>) => {
      // 检查业务状态码
      if (response.data && response.data.code !== 200) {
        throw new Error(response.data.message || '请求失败')
      }
      
      return response
    },
    (error) => {
      // 统一错误处理
      handleApiError(error)
      return Promise.reject(error)
    }
  )
}

/**
 * 统一API错误处理
 * @param error 错误对象
 */
function handleApiError(error: any): void {
  // 处理认证失败
  if (error.response?.status === 401) {
    localStorage.removeItem('access_token')
    localStorage.removeItem('refresh_token')
    if (typeof window !== 'undefined') {
      window.location.href = '/login'
    }
    return
  }

  // 记录其他错误
  console.error('API Error:', error)
}
