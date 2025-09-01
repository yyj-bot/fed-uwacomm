/**
 * 用户服务类型定义
 * 定义用户认证和管理相关的请求和响应类型
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import type { User, DeepReadonly, PartialBy, RequiredBy } from '@/types'

// ==================== 认证相关类型 ====================

/**
 * 用户注册请求
 */
export interface RegisterUserRequest {
  /** 用户名，长度3-50字符 */
  readonly username: string
  /** 邮箱地址，符合邮箱格式，唯一 */
  readonly email: string
  /** 密码，长度6-20字符 */
  readonly password: string
  /** 确认密码，必须与密码一致 */
  readonly confirmPassword: string
}

/**
 * 刷新令牌响应
 */
export interface RefreshTokenResponse {
  /** 新的访问令牌 */
  readonly token: string
  /** 新的刷新令牌 */
  readonly refreshToken: string
  /** 令牌过期时间（秒） */
  readonly expiresIn: number
}

// ==================== 用户管理相关类型 ====================

/**
 * 更新个人信息请求
 */
export interface UpdateProfileRequest {
  /** 用户名，可选 */
  readonly username?: string
  /** 邮箱地址，可选 */
  readonly email?: string
}

/**
 * 修改密码请求
 */
export interface ChangePasswordRequest {
  /** 旧密码 */
  readonly oldPassword: string
  /** 新密码，长度6-20字符 */
  readonly newPassword: string
  /** 确认新密码，必须与新密码一致 */
  readonly confirmPassword: string
}

// ==================== 业务逻辑类型 ====================

/**
 * 用户操作类型
 */
export type UserOperation = 
  | 'REGISTER'
  | 'LOGIN'
  | 'LOGOUT'
  | 'UPDATE_PROFILE'
  | 'CHANGE_PASSWORD'
  | 'REFRESH_TOKEN'

/**
 * 用户操作日志
 */
export interface UserOperationLog {
  /** 操作ID */
  readonly operationId: string
  /** 操作类型 */
  readonly operation: UserOperation
  /** 用户ID */
  readonly userId: string
  /** 操作时间 */
  readonly operatedAt: string
  /** 操作详情 */
  readonly details?: Record<string, unknown>
  /** 操作结果 */
  readonly success: boolean
  /** 错误信息（如果操作失败） */
  readonly errorMessage?: string
  /** 客户端IP */
  readonly clientIp?: string
  /** 用户代理 */
  readonly userAgent?: string
}

// ==================== 认证状态类型 ====================

/**
 * 认证状态
 */
export interface AuthState {
  /** 是否已认证 */
  readonly isAuthenticated: boolean
  /** 当前用户信息 */
  readonly user: User | null
  /** 访问令牌 */
  readonly accessToken: string | null
  /** 刷新令牌 */
  readonly refreshToken: string | null
  /** 令牌过期时间 */
  readonly expiresAt: Date | null
}

/**
 * 登录状态
 */
export type LoginStatus = 
  | 'IDLE'
  | 'LOGGING_IN'
  | 'LOGGED_IN'
  | 'LOGGING_OUT'
  | 'LOGIN_FAILED'
  | 'TOKEN_EXPIRED'

// ==================== 错误类型 ====================

/**
 * 用户服务错误类型
 */
export interface UserServiceError {
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
 * 认证错误类型
 */
export interface AuthError extends UserServiceError {
  /** 认证失败类型 */
  readonly authFailureType: 
    | 'INVALID_CREDENTIALS'
    | 'TOKEN_EXPIRED'
    | 'TOKEN_INVALID'
    | 'ACCOUNT_LOCKED'
    | 'PERMISSION_DENIED'
    | 'UNKNOWN'
}

/**
 * 验证错误类型
 */
export interface ValidationError extends UserServiceError {
  /** 验证失败的字段 */
  readonly field: string
  /** 验证规则 */
  readonly rule: string
  /** 期望值 */
  readonly expected?: unknown
  /** 实际值 */
  readonly actual?: unknown
}

// ==================== 安全相关类型 ====================

/**
 * 密码强度等级
 */
export type PasswordStrength = 'WEAK' | 'MEDIUM' | 'STRONG' | 'VERY_STRONG'

/**
 * 密码策略
 */
export interface PasswordPolicy {
  /** 最小长度 */
  readonly minLength: number
  /** 最大长度 */
  readonly maxLength: number
  /** 是否需要大写字母 */
  readonly requireUppercase: boolean
  /** 是否需要小写字母 */
  readonly requireLowercase: boolean
  /** 是否需要数字 */
  readonly requireDigits: boolean
  /** 是否需要特殊字符 */
  readonly requireSpecialChars: boolean
  /** 禁止的字符 */
  readonly forbiddenChars?: string[]
  /** 不能与最近几次密码重复 */
  readonly historyCount?: number
}

/**
 * 登录安全策略
 */
export interface LoginSecurityPolicy {
  /** 最大失败尝试次数 */
  readonly maxFailedAttempts: number
  /** 锁定时间（分钟） */
  readonly lockoutDuration: number
  /** 是否启用验证码 */
  readonly enableCaptcha: boolean
  /** 验证码触发的失败次数 */
  readonly captchaThreshold: number
  /** 会话超时时间（分钟） */
  readonly sessionTimeout: number
}

// ==================== 统计类型 ====================

/**
 * 用户活动统计
 */
export interface UserActivityStats {
  /** 总登录次数 */
  readonly totalLogins: number
  /** 最后登录时间 */
  readonly lastLoginAt: string | null
  /** 最后登录IP */
  readonly lastLoginIp: string | null
  /** 连续登录天数 */
  readonly consecutiveLoginDays: number
  /** 本月登录次数 */
  readonly monthlyLogins: number
  /** 平均会话时长（分钟） */
  readonly averageSessionDuration: number
}

/**
 * 用户偏好设置
 */
export interface UserPreferences {
  /** 语言设置 */
  readonly language: string
  /** 时区设置 */
  readonly timezone: string
  /** 主题设置 */
  readonly theme: 'light' | 'dark' | 'auto'
  /** 通知设置 */
  readonly notifications: {
    readonly email: boolean
    readonly push: boolean
    readonly sms: boolean
  }
  /** 隐私设置 */
  readonly privacy: {
    readonly profileVisible: boolean
    readonly activityVisible: boolean
    readonly onlineStatus: boolean
  }
}

// ==================== 配置类型 ====================

/**
 * 用户服务配置
 */
export interface UserServiceConfig {
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
  /** 令牌刷新阈值（秒） */
  readonly tokenRefreshThreshold: number
  /** 是否自动刷新令牌 */
  readonly autoRefreshToken: boolean
}

// ==================== 重新导出基础类型 ====================
// 注：工具类型已从 @/types 统一导入，不再重复定义
