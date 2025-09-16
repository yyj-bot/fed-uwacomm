/**
 * 管理员服务类型定义
 * 定义管理员用户管理相关的请求和响应类型
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import type { User, PaginationParams, DeepReadonly, PartialBy, RequiredBy } from '@/types'

// ==================== 权限相关类型 ====================

/**
 * 权限信息
 */
export interface Permission {
  /** 权限ID */
  readonly permissionId: string
  /** 权限名称 */
  readonly permissionName: string
  /** 权限描述 */
  readonly description: string
  /** 授予时间 */
  readonly grantedAt: string
}

// ==================== 用户管理请求类型 ====================

/**
 * 用户列表查询参数
 */
export interface UserListParams extends PaginationParams {
  /** 用户名模糊查询 */
  readonly username?: string
  /** 邮箱模糊查询 */
  readonly email?: string
  /** 角色筛选 */
  readonly role?: User['role']
  /** 状态筛选 */
  readonly status?: User['status']
}

/**
 * 创建用户请求
 */
export interface CreateUserRequest {
  /** 用户名 */
  readonly username: string
  /** 邮箱地址 */
  readonly email: string
  /** 密码 */
  readonly password: string
  /** 用户角色 */
  readonly role: User['role']
  /** 用户状态，可选，默认ACTIVE */
  readonly status?: User['status']
}

/**
 * 更新用户请求
 */
export interface UpdateUserRequest {
  /** 用户名 */
  readonly username?: string
  /** 邮箱地址 */
  readonly email?: string
  /** 用户角色 */
  readonly role?: User['role']
  /** 用户状态 */
  readonly status?: User['status']
  /** 新密码，管理员可直接修改用户密码 */
  readonly password?: string
}

/**
 * 锁定用户请求
 */
export interface LockUserRequest {
  /** 锁定时长（秒），可选，默认1小时 */
  readonly duration?: number
}

/**
 * 重置密码请求
 */
export interface ResetPasswordRequest {
  /** 新密码 */
  readonly newPassword: string
}

/**
 * 授予权限请求
 */
export interface GrantPermissionRequest {
  /** 权限名称 */
  readonly permissionName: string
}

// ==================== 响应类型 ====================

/**
 * 锁定用户响应
 */
export interface LockUserResponse {
  /** 用户ID */
  readonly userId: string
  /** 锁定到期时间 */
  readonly lockedUntil: string
  /** 锁定时长（秒） */
  readonly duration?: number
}

/**
 * 解锁用户响应
 */
export interface UnlockUserResponse {
  /** 用户ID */
  readonly userId: string
  /** 用户状态 */
  readonly status: User['status']
}

// ==================== 服务错误类型 ====================

/**
 * 服务层错误类型
 */
export interface ServiceError {
  /** 错误码 */
  readonly code: string
  /** 错误消息 */
  readonly message: string
  /** 详细错误信息 */
  readonly details?: unknown
  /** 原始错误 */
  readonly originalError?: Error
}

/**
 * 验证错误类型
 */
export interface ValidationError extends ServiceError {
  /** 验证失败的字段 */
  readonly field: string
  /** 验证规则 */
  readonly rule: string
}

// ==================== 业务逻辑类型 ====================

/**
 * 用户操作类型
 */
export type UserOperation = 
  | 'CREATE'
  | 'UPDATE'
  | 'DELETE'
  | 'LOCK'
  | 'UNLOCK'
  | 'RESET_PASSWORD'
  | 'GRANT_PERMISSION'
  | 'REVOKE_PERMISSION'

/**
 * 用户操作日志
 */
export interface UserOperationLog {
  /** 操作ID */
  readonly operationId: string
  /** 操作类型 */
  readonly operation: UserOperation
  /** 目标用户ID */
  readonly targetUserId: string
  /** 操作者ID */
  readonly operatorId: string
  /** 操作时间 */
  readonly operatedAt: string
  /** 操作详情 */
  readonly details?: Record<string, unknown>
  /** 操作结果 */
  readonly success: boolean
  /** 错误信息（如果操作失败） */
  readonly errorMessage?: string
}

// ==================== 统计类型 ====================

/**
 * 用户统计信息
 */
export interface UserStatistics {
  /** 总用户数 */
  readonly totalUsers: number
  /** 活跃用户数 */
  readonly activeUsers: number
  /** 锁定用户数 */
  readonly lockedUsers: number
  /** 按角色分布 */
  readonly roleDistribution: Record<User['role'], number>
  /** 按状态分布 */
  readonly statusDistribution: Record<User['status'], number>
  /** 本月新增用户数 */
  readonly newUsersThisMonth: number
  /** 本月活跃用户数 */
  readonly activeUsersThisMonth: number
}

// ==================== 配置类型 ====================

/**
 * 管理员服务配置
 */
export interface AdminServiceConfig {
  /** API基础URL */
  readonly baseURL: string
  /** 请求超时时间（毫秒） */
  readonly timeout: number
  /** 是否启用请求日志 */
  readonly enableLogging: boolean
  /** 是否启用错误重试 */
  readonly enableRetry: boolean
  /** 最大重试次数 */
  readonly maxRetries: number
  /** 重试延迟（毫秒） */
  readonly retryDelay: number
}

// ==================== 重新导出基础类型 ====================
// 注：工具类型已从 @/types 统一导入，不再重复定义
