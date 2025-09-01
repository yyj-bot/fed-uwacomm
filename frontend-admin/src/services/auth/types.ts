/**
 * 认证服务相关类型定义
 */

import type { User } from '@/types'

// 认证状态接口
export interface AuthState {
  user: User | null
  isAuthenticated: boolean
  isLoading: boolean
  error: string | null
}

// Token信息接口
export interface TokenInfo {
  accessToken: string
  refreshToken: string
  expiresAt: number
  expiresIn: number
}

// 认证事件类型
export type AuthEventType = 'LOGIN' | 'LOGOUT' | 'TOKEN_REFRESH' | 'AUTH_ERROR'

// 认证事件接口
export interface AuthEvent {
  type: AuthEventType
  payload?: {
    user?: User
    error?: string
    timestamp?: string
  }
}

// 认证事件监听器
export type AuthEventListener = (event: AuthEvent) => void

// 权限检查结果
export interface PermissionCheckResult {
  hasPermission: boolean
  reason?: string
}

// 角色权限配置
export interface RolePermissions {
  [role: string]: {
    permissions: string[]
    description: string
  }
}
