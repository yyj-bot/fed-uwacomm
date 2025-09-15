import { createApiInstance } from './base'
import type { 
  ApiResponse, 
  User,
  LoginRequest,
  LoginResponse,
  PaginatedResponse,
  PaginationParams
} from '@/types'

// 创建用户API实例
const userApiInstance = createApiInstance('http://localhost:8080/api/user')

// ==================== 用户认证API ====================
export const userApi = {
  // 用户注册
  async register(userData: {
    username: string
    email: string
    password: string
    confirmPassword: string
  }): Promise<User> {
    const response = await userApiInstance.post<ApiResponse<User>>('/register', userData)
    return response.data.data
  },

  // 用户登录
  async login(loginData: LoginRequest): Promise<LoginResponse> {
    const response = await userApiInstance.post<ApiResponse<LoginResponse>>('/login', loginData)
    return response.data.data
  },

  // 刷新Token
  async refreshToken(): Promise<{ token: string; refreshToken: string; expiresIn: number }> {
    const refreshToken = localStorage.getItem('refresh_token')
    const response = await userApiInstance.post<ApiResponse<{ token: string; refreshToken: string; expiresIn: number }>>(
      '/refresh',
      {},
      { headers: { Authorization: `Bearer ${refreshToken}` } }
    )
    return response.data.data
  },

  // 用户登出
  async logout(): Promise<void> {
    await userApiInstance.post<ApiResponse<null>>('/logout')
  },

  // 获取当前用户信息
  async getProfile(): Promise<User> {
    const response = await userApiInstance.get<ApiResponse<User>>('/profile')
    return response.data.data
  },

  // 更新用户信息
  async updateProfile(userData: { username?: string; email?: string }): Promise<User> {
    const response = await userApiInstance.put<ApiResponse<User>>('/profile', userData)
    return response.data.data
  },

  // 修改密码
  async changePassword(passwordData: {
    oldPassword: string
    newPassword: string
    confirmPassword: string
  }): Promise<void> {
    await userApiInstance.put<ApiResponse<null>>('/password', passwordData)
  },
} as const

 