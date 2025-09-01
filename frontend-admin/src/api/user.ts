import axios, { type AxiosResponse } from 'axios'
import type { 
  ApiResponse, 
  User,
  LoginRequest,
  LoginResponse,
  PaginatedResponse,
  PaginationParams
} from '@/types'

// 创建API实例
const api = axios.create({
  baseURL: 'http://localhost:8080/api/user',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器 - 添加认证头
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('access_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器 - 统一错误处理
api.interceptors.response.use(
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
    console.error('API Error:', error)
    throw error
  }
)

// ==================== 用户认证API ====================
export const userApi = {
  // 用户注册
  async register(userData: {
    username: string
    email: string
    password: string
    confirmPassword: string
  }): Promise<User> {
    const response = await api.post<ApiResponse<User>>('/register', userData)
    return response.data.data
  },

  // 用户登录
  async login(loginData: LoginRequest): Promise<LoginResponse> {
    const response = await api.post<ApiResponse<LoginResponse>>('/login', loginData)
    return response.data.data
  },

  // 刷新Token
  async refreshToken(): Promise<{ token: string; refreshToken: string; expiresIn: number }> {
    const refreshToken = localStorage.getItem('refresh_token')
    const response = await api.post<ApiResponse<{ token: string; refreshToken: string; expiresIn: number }>>(
      '/refresh',
      {},
      { headers: { Authorization: `Bearer ${refreshToken}` } }
    )
    return response.data.data
  },

  // 用户登出
  async logout(): Promise<void> {
    await api.post<ApiResponse<null>>('/logout')
  },

  // 获取当前用户信息
  async getProfile(): Promise<User> {
    const response = await api.get<ApiResponse<User>>('/profile')
    return response.data.data
  },

  // 更新用户信息
  async updateProfile(userData: { username?: string; email?: string }): Promise<User> {
    const response = await api.put<ApiResponse<User>>('/profile', userData)
    return response.data.data
  },

  // 修改密码
  async changePassword(passwordData: {
    oldPassword: string
    newPassword: string
    confirmPassword: string
  }): Promise<void> {
    await api.put<ApiResponse<null>>('/password', passwordData)
  },
} as const

 