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

// ==================== 水下机器人管理相关类型 ====================

/**
 * VM权限类型
 */
export type VmPermission = 'READ' | 'WRITE' | 'EXECUTE' | 'ADMIN'

/**
 * VM状态类型
 */
export type VmStatus = 'RUNNING' | 'STOPPED' | 'ERROR' | 'STARTING' | 'STOPPING' | 'OFFLINE'

/**
 * VM连接状态类型
 */
export type VmConnectionStatus = 'CONNECTED' | 'DISCONNECTED' | 'CONNECTING'

/**
 * VM控制操作类型
 */
export type VmControlAction = 'START' | 'STOP' | 'RESTART' | 'FORCE_STOP'

/**
 * 水下机器人分配信息
 */
export interface VmAssignment {
  /** 水下机器人ID */
  readonly vmId: string
  /** 用户ID */
  readonly userId: string
  /** 权限列表 */
  readonly permissions: VmPermission[]
  /** 分配时间 */
  readonly assignedAt: string
  /** 分配者ID */
  readonly assignedBy: string
  /** 备注信息 */
  readonly notes?: string
}

/**
 * 水下机器人分配详情（包含用户信息）
 */
export interface VmAssignmentDetail {
  /** 用户ID */
  readonly userId: string
  /** 用户名 */
  readonly username: string
  /** 邮箱 */
  readonly email: string
  /** 权限列表 */
  readonly permissions: VmPermission[]
  /** 分配时间 */
  readonly assignedAt: string
  /** 分配者ID */
  readonly assignedBy: string
}

/**
 * 水下机器人基本信息
 */
export interface VirtualMachine {
  /** 水下机器人ID */
  readonly vmId: string
  /** 水下机器人名称 */
  readonly name: string
  /** IP地址 */
  readonly ipAddress: string
  /** 端口 */
  readonly port?: number
  /** 水下机器人状态 */
  readonly status: VmStatus
  /** 连接状态 */
  readonly connectionStatus: VmConnectionStatus
  /** 电量百分比 */
  readonly batteryLevel?: number
  /** 操作系统类型 */
  readonly osType?: string
  /** CPU核心数 */
  readonly cpuCores?: number
  /** 内存大小(MB) */
  readonly memoryMb?: number
  /** 磁盘大小(GB) */
  readonly diskGb?: number
  /** 是否已分配 */
  readonly isAssigned?: boolean
  /** 已分配用户数量 */
  readonly assignedUserCount?: number
  /** 创建时间 */
  readonly createdAt: string
  /** 更新时间 */
  readonly updatedAt?: string
  /** 最后心跳时间 */
  readonly lastHeartbeat?: string
  /** 系统信息 */
  readonly systemInfo?: {
    os?: string
    kernel?: string
    python?: string
    gpu?: string
    cuda?: string
    cudnn?: string
  }
  /** 能力配置 */
  readonly capabilities?: {
    supportedAlgorithms?: string[]
    maxBatchSize?: number
    maxMemoryUsage?: number
    gpuMemory?: number
    networkSpeed?: number
  }
  /** 网络配置 */
  readonly networkConfig?: {
    uploadSpeed?: number
    downloadSpeed?: number
    latency?: number
    bandwidth?: number
  }
  /** 元数据 */
  readonly metadata?: {
    description?: string
    location?: string
    owner?: string
    department?: string
    tags?: string[]
  }
}

/**
 * 用户水下机器人信息
 */
export interface UserVm {
  /** 水下机器人ID */
  readonly vmId: string
  /** 水下机器人名称 */
  readonly vmName: string
  /** IP地址 */
  readonly ipAddress: string
  /** 水下机器人状态 */
  readonly status: VmStatus
  /** 权限列表 */
  readonly permissions: VmPermission[]
  /** 分配时间 */
  readonly assignedAt: string
  /** 电量百分比 */
  readonly batteryLevel?: number
}

/**
 * 批量操作结果
 */
export interface BatchOperationResult {
  /** 水下机器人ID */
  readonly vmId: string
  /** 操作状态 */
  readonly status: 'SUCCESS' | 'FAILED'
  /** 分配时间 */
  readonly assignedAt?: string
  /** 取消分配时间 */
  readonly unassignedAt?: string
  /** 错误信息 */
  readonly error?: string
}

/**
 * 水下机器人分配概况统计
 */
export interface VmAssignmentOverview {
  /** 概况统计 */
  readonly summary: {
    /** 总水下机器人数 */
    readonly totalVms: number
    /** 已分配水下机器人数 */
    readonly assignedVms: number
    /** 未分配水下机器人数 */
    readonly unassignedVms: number
    /** 总用户数 */
    readonly totalUsers: number
    /** 拥有水下机器人的用户数 */
    readonly usersWithVms: number
  }
  /** 状态分布 */
  readonly statusDistribution: Record<VmStatus, number>
  /** 热门水下机器人（分配最多的） */
  readonly topAssignedVms: Array<{
    readonly vmId: string
    readonly vmName: string
    readonly assignedUserCount: number
  }>
  /** 最近分配记录 */
  readonly recentAssignments: Array<{
    readonly vmId: string
    readonly vmName: string
    readonly userId: string
    readonly username: string
    readonly assignedAt: string
  }>
}

/**
 * VM控制命令响应
 */
export interface VmControlResponse {
  /** 水下机器人ID */
  readonly vmId: string
  /** 操作类型 */
  readonly action: VmControlAction
  /** 命令ID */
  readonly commandId: string
  /** 操作原因 */
  readonly reason?: string
  /** 执行者ID */
  readonly executedBy: string
  /** 执行时间 */
  readonly executedAt: string
  /** 预计完成时间（秒） */
  readonly estimatedTime?: number
}

/**
 * 分配水下机器人请求参数
 */
export interface AssignVmRequest {
  /** 水下机器人ID */
  readonly vmId: string
  /** 用户ID */
  readonly userId: string
  /** 权限列表 */
  readonly permissions?: VmPermission[]
  /** 备注信息 */
  readonly notes?: string
}

/**
 * 取消分配水下机器人响应
 */
export interface UnassignVmResponse {
  /** 水下机器人ID */
  readonly vmId: string
  /** 用户ID */
  readonly userId: string
  /** 取消分配时间 */
  readonly unassignedAt: string
}

/**
 * 水下机器人分配信息响应
 */
export interface VmAssignmentInfoResponse {
  /** 水下机器人ID */
  readonly vmId: string
  /** 水下机器人名称 */
  readonly vmName: string
  /** 分配列表 */
  readonly assignments: VmAssignmentDetail[]
  /** 总分配数 */
  readonly totalAssignments: number
}

/**
 * 更新权限响应
 */
export interface UpdateVmPermissionsResponse {
  /** 水下机器人ID */
  readonly vmId: string
  /** 用户ID */
  readonly userId: string
  /** 旧权限列表 */
  readonly oldPermissions: VmPermission[]
  /** 新权限列表 */
  readonly newPermissions: VmPermission[]
  /** 更新时间 */
  readonly updatedAt: string
}

/**
 * 用户水下机器人列表请求参数
 */
export interface UserVmListParams extends PaginationParams {
  /** 用户ID */
  readonly userId: string
  /** 状态筛选 */
  readonly status?: VmStatus
}

/**
 * 批量分配水下机器人请求
 */
export interface BatchAssignVmsRequest {
  /** 用户ID */
  readonly userId: string
  /** 水下机器人ID列表 */
  readonly vmIds: string[]
  /** 权限列表 */
  readonly permissions?: VmPermission[]
  /** 备注信息 */
  readonly notes?: string
}

/**
 * 批量分配水下机器人响应
 */
export interface BatchAssignVmsResponse {
  /** 用户ID */
  readonly userId: string
  /** 成功数量 */
  readonly successCount: number
  /** 失败数量 */
  readonly failedCount: number
  /** 操作结果列表 */
  readonly results: BatchOperationResult[]
}

/**
 * 批量移除水下机器人请求
 */
export interface BatchRemoveVmsRequest {
  /** 用户ID */
  readonly userId: string
  /** 水下机器人ID列表 */
  readonly vmIds: string[]
}

/**
 * 批量移除水下机器人响应
 */
export interface BatchRemoveVmsResponse {
  /** 用户ID */
  readonly userId: string
  /** 成功数量 */
  readonly successCount: number
  /** 失败数量 */
  readonly failedCount: number
  /** 操作结果列表 */
  readonly results: BatchOperationResult[]
}

/**
 * 管理员水下机器人列表请求参数
 */
export interface AdminVmListParams extends PaginationParams {
  /** 状态筛选 */
  readonly status?: VmStatus
  /** 是否已分配筛选 */
  readonly assigned?: boolean
  /** 关键词搜索（名称、IP） */
  readonly keyword?: string
}

/**
 * 未分配水下机器人列表请求参数
 */
export interface UnassignedVmListParams {
  /** 状态筛选 */
  readonly status?: VmStatus
}

/**
 * 未分配水下机器人列表响应
 */
export interface UnassignedVmListResponse {
  /** 未分配水下机器人列表 */
  readonly unassignedVms: VirtualMachine[]
  /** 总数 */
  readonly total: number
}

/**
 * 强制控制水下机器人请求
 */
export interface ForceControlVmRequest {
  /** 水下机器人ID */
  readonly vmId: string
  /** 操作类型 */
  readonly action: VmControlAction
  /** 操作原因（必填） */
  readonly reason: string
  /** 超时时间（秒） */
  readonly timeout?: number
}

// ==================== 重新导出基础类型 ====================
// 注：工具类型已从 @/types 统一导入，不再重复定义
