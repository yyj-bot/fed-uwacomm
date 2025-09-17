/**
 * API基础配置和拦截器
 * 统一管理所有API实例的通用配置，消除重复代码
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import axios, { type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import type { ApiResponse } from '@/types'

// ==================== 环境检测 ====================
/**
 * 检测是否为开发环境
 */
const isDevelopment = (() => {
  // 优先使用Node环境变量
  if (typeof process !== 'undefined' && process.env?.NODE_ENV) {
    return process.env.NODE_ENV === 'development'
  }
  
  // 如果在浏览器环境，通过hostname判断
  if (typeof window !== 'undefined' && window.location) {
    const hostname = window.location.hostname
    return hostname === 'localhost' || hostname === '127.0.0.1' || hostname === '0.0.0.0'
  }
  
  // 默认为生产环境
  return false
})()

// ==================== API 配置常量 ====================
/**
 * API基础配置
 */
export const API_CONFIG = {
  // 后端服务基础URL - 开发环境使用代理，生产环境使用完整URL
  BASE_URL: isDevelopment ? '' : 'http://localhost:8080',
  
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
      // 开发环境下打印请求信息
      if (isDevelopment) {
        console.log(`🚀 API Request: ${config.method?.toUpperCase()} ${config.baseURL}${config.url}`)
        if (config.data) {
          console.log(`📤 Request Data:`, config.data)
        }
        if (config.params) {
          console.log(`📋 Request Params:`, config.params)
        }
      }
      
      // 添加认证头
      const token = localStorage.getItem('access_token')
      if (token && config.headers) {
        // 清理token中可能的空格和换行符
        const cleanToken = token.trim().replace(/\s+/g, '')
        config.headers.Authorization = `Bearer ${cleanToken}`
        
        // 调试token处理
        if (isDevelopment && config.url && !config.url.includes('/login')) {
          console.log('🔑 API请求添加token:', {
            url: config.url,
            hasToken: !!token,
            tokenPreview: token ? '***' + token.slice(-10) : 'null',
            cleanTokenPreview: cleanToken ? '***' + cleanToken.slice(-10) : 'null',
            tokenLength: token.length,
            cleanTokenLength: cleanToken.length,
            tokenChanged: token !== cleanToken,
            authHeader: `Bearer ${cleanToken.substring(0, 20)}...`
          })
        }
      }
      
      return config
    },
    (error) => {
      console.error('❌ Request Error:', error)
      return Promise.reject(error)
    }
  )
}

/**
 * 设置响应拦截器
 * @param instance Axios实例
 */
function setupResponseInterceptor(instance: AxiosInstance): void {
  instance.interceptors.response.use(
    (response: AxiosResponse<ApiResponse<unknown>>) => {
      // 开发环境下打印响应信息
      if (isDevelopment) {
        console.log(`✅ API Response: ${response.status} ${response.config.url}`)
        console.log(`📥 Response Data:`, response.data)
      }
      
      // 检查业务状态码
      if (response.data && response.data.code !== 200) {
        // 特殊处理认证失败
        if (response.data.code === 401) {
          console.warn('🚨 API返回401认证失败:', {
            url: response.config.url,
            message: response.data.message,
            willClearToken: !response.config.url?.includes('/login')
          })
          
          // 暂时禁用自动token清理，避免后端认证问题导致循环
          console.warn('⚠️ 检测到401但暂时不清理token，API:', response.config.url)
          console.warn('💡 如果这是后端问题，请检查后端token验证逻辑')
          
          // 只对特定API清理token（避免误杀）
          const criticalApis = ['/user/profile', '/auth/verify']
          const shouldClearToken = criticalApis.some(api => response.config.url?.includes(api))
          
          if (shouldClearToken && !response.config.url?.includes('/login')) {
            console.warn('🧹 关键API失败，清理token:', response.config.url)
            localStorage.removeItem('access_token')
            localStorage.removeItem('refresh_token')
            
            if (typeof window !== 'undefined' && !window.location.pathname.includes('/login')) {
              console.log('🔄 重定向到登录页面')
              window.location.href = '/login'
            }
          }
        }
        throw new Error(response.data.message || '请求失败')
      }
      
      return response
    },
    (error) => {
      // 开发环境下打印错误信息
      if (isDevelopment) {
        console.error(`❌ API Error: ${error.response?.status || 'Network Error'} ${error.config?.url || 'Unknown URL'}`)
        if (error.response?.data) {
          console.error(`📥 Error Response:`, error.response.data)
        }
      }
      
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
    console.warn('🚨 handleApiError检测到401错误:', {
      url: error.config?.url,
      status: error.response?.status,
      data: error.response?.data
    })
    
    // 暂时禁用自动清理，避免后端问题导致循环
    console.warn('⚠️ handleApiError检测到401但暂时不清理token')
    console.warn('💡 请检查后端API认证逻辑')
    
    // 只对关键认证API清理token
    const criticalApis = ['/user/profile', '/auth/verify', '/user/login']
    const shouldClearToken = criticalApis.some(api => error.config?.url?.includes(api))
    
    if (shouldClearToken) {
      console.warn('🧹 handleApiError关键API失败，清理token')
      localStorage.removeItem('access_token')
      localStorage.removeItem('refresh_token')
      
      if (typeof window !== 'undefined') {
        console.log('🔄 handleApiError重定向到登录页面')
        window.location.href = '/login'
      }
    }
    return
  }

  // 记录其他错误
  console.error('API Error:', error)
}
