/**
 * API基础配置和拦截器
 * 统一管理所有API实例的通用配置，消除重复代码
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import axios, { type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import type { ApiResponse } from '@/types'

/**
 * 创建标准化的API实例
 * @param baseURL API基础URL
 * @param timeout 请求超时时间（毫秒）
 * @returns 配置好的Axios实例
 */
export function createApiInstance(baseURL: string, timeout: number = 30000): AxiosInstance {
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
