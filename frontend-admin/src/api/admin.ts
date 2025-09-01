import axios, { type AxiosResponse } from 'axios'
import type { 
  ApiResponse, 
  User,
  PaginatedResponse,
  PaginationParams
} from '@/types'

// 创建管理员API实例
const adminApi = axios.create({
  baseURL: 'http://localhost:8080/api/admin',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器 - 添加认证头
adminApi.interceptors.request.use(
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
adminApi.interceptors.response.use(
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
    console.error('Admin API Error:', error)
    throw error
  }
)

// 权限信息类型定义
interface Permission {
  permissionId: string
  permissionName: string
  description: string
  grantedAt: string
}

// ==================== 管理员用户管理API ====================
export const admin = {
  // 1.1 获取用户列表
  async getUserList(params: PaginationParams & {
    username?: string
    email?: string
    role?: string
    status?: string
  } = {}): Promise<PaginatedResponse<User>> {
    const response = await adminApi.get<ApiResponse<PaginatedResponse<User>>>('/user/list', { params })
    return response.data.data
  },

  // 1.2 获取用户详情
  async getUserDetail(userId: string): Promise<User> {
    const response = await adminApi.get<ApiResponse<User>>(`/user/${userId}`)
    return response.data.data
  },

  // 1.3 创建用户
  async createUser(userData: {
    username: string
    email: string
    password: string
    role: string
    status?: string
  }): Promise<User> {
    const response = await adminApi.post<ApiResponse<User>>('/user/create', userData)
    return response.data.data
  },

  // 1.4 更新用户
  async updateUser(userId: string, userData: {
    username?: string
    email?: string
    role?: string
    status?: string
    password?: string
  }): Promise<User> {
    const response = await adminApi.put<ApiResponse<User>>(`/user/${userId}`, userData)
    return response.data.data
  },

  // 1.5 删除用户
  async deleteUser(userId: string): Promise<void> {
    await adminApi.delete<ApiResponse<null>>(`/user/${userId}`)
  },

  // 1.6 锁定用户
  async lockUser(userId: string, duration?: number): Promise<{ userId: string; lockedUntil: string }> {
    const response = await adminApi.post<ApiResponse<{ userId: string; lockedUntil: string }>>(
      `/user/${userId}/lock`,
      { duration }
    )
    return response.data.data
  },

  // 1.7 解锁用户
  async unlockUser(userId: string): Promise<{ userId: string; status: string }> {
    const response = await adminApi.post<ApiResponse<{ userId: string; status: string }>>(
      `/user/${userId}/unlock`
    )
    return response.data.data
  },

  // 1.8 重置用户密码
  async resetUserPassword(userId: string, newPassword: string): Promise<void> {
    await adminApi.post<ApiResponse<null>>(`/user/${userId}/reset-password`, { newPassword })
  },

  // 1.9 获取用户权限
  async getUserPermissions(userId: string): Promise<Permission[]> {
    const response = await adminApi.get<ApiResponse<Permission[]>>(`/user/${userId}/permissions`)
    return response.data.data
  },

  // 1.10 授予用户权限
  async grantUserPermission(userId: string, permissionName: string): Promise<Permission> {
    const response = await adminApi.post<ApiResponse<Permission>>(
      `/user/${userId}/permissions`,
      { permissionName }
    )
    return response.data.data
  },

  // 1.11 撤销用户权限
  async revokeUserPermission(userId: string, permissionId: string): Promise<void> {
    await adminApi.delete<ApiResponse<null>>(`/user/${userId}/permissions/${permissionId}`)
  },
} as const

export default admin
