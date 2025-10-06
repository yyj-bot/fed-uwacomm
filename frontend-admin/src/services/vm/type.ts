/**
 * 虚拟机服务类型定义
 * 定义虚拟机管理相关的请求和响应类型
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import type { PaginationParams, VirtualMachine, VMStatus, DeepReadonly, PartialBy, RequiredBy } from '@/types'

// ==================== 查询相关类型 ====================

/**
 * 虚拟机列表查询参数
 */
export interface VMListParams extends PaginationParams {
  /** 状态过滤 */
  readonly status?: VirtualMachine['status']
  /** 操作系统类型过滤 */
  readonly osType?: string
  /** 关键词搜索（名称、IP地址） */
  readonly keyword?: string
}

/**
 * 虚拟机列表响应
 */
export interface VMListResponse {
  /** 总数 */
  readonly total: number
  /** 当前页 */
  readonly page: number
  /** 每页大小 */
  readonly size: number
  /** 总页数 */
  readonly pages: number
  /** 虚拟机列表 */
  readonly list: VirtualMachine[]
}



// ==================== 更新相关类型 ====================

/**
 * 虚拟机更新请求
 */
export interface VMUpdateRequest {
  /** 虚拟机名称 */
  readonly name?: string
  /** IP地址 */
  readonly ipAddress?: string
  /** 端口 */
  readonly port?: number
  /** 操作系统类型 */
  readonly osType?: string
  /** CPU核心数 */
  readonly cpuCores?: number
  /** 内存大小（MB） */
  readonly memoryMb?: number
  /** 磁盘大小（GB） */
  readonly diskGb?: number
  /** 系统信息 */
  readonly systemInfo?: {
    readonly os?: string
    readonly kernel?: string
    readonly python?: string
    readonly gpu?: string
    readonly cuda?: string
    readonly cudnn?: string
  }
  /** 能力信息 */
  readonly capabilities?: {
    readonly supportedAlgorithms?: string[]
    readonly maxBatchSize?: number
    readonly maxMemoryUsage?: number
    readonly gpuMemory?: number
    readonly networkSpeed?: number
  }
  /** 网络配置 */
  readonly networkConfig?: {
    readonly uploadSpeed?: number
    readonly downloadSpeed?: number
    readonly latency?: number
    readonly bandwidth?: number
  }
  /** 元数据 */
  readonly metadata?: {
    readonly description?: string
    readonly location?: string
    readonly owner?: string
    readonly department?: string
    readonly tags?: string[]
  }
}

/**
 * 虚拟机更新响应
 */
export interface VMUpdateResponse {
  /** 虚拟机ID */
  readonly vmId: string
  /** 虚拟机名称 */
  readonly name: string
  /** 更新时间 */
  readonly updatedAt: string
}

/**
 * 虚拟机删除响应
 */
export interface VMDeleteResponse {
  /** 虚拟机ID */
  readonly vmId: string
  /** 删除时间 */
  readonly deletedAt: string
}

// ==================== 控制相关类型 ====================

/**
 * 虚拟机启动请求
 */
export interface VMStartRequest {
  /** 超时时间（秒） */
  readonly timeout?: number
  /** 配置信息 */
  readonly config?: {
    readonly memory?: string
    readonly cpu?: string
    readonly disk?: string
    readonly network?: string
  }
  /** 环境变量 */
  readonly environment?: {
    readonly variables?: Record<string, string>
  }
}

/**
 * 虚拟机停止请求
 */
export interface VMStopRequest {
  /** 是否强制停止 */
  readonly force?: boolean
  /** 超时时间（秒） */
  readonly timeout?: number
  /** 是否保存状态 */
  readonly saveState?: boolean
}

/**
 * 虚拟机重启请求
 */
export interface VMRestartRequest {
  /** 超时时间（秒） */
  readonly timeout?: number
  /** 是否优雅重启 */
  readonly graceful?: boolean
  /** 配置信息 */
  readonly config?: {
    readonly memory?: string
    readonly cpu?: string
  }
}

/**
 * 虚拟机启动响应
 */
export interface VMStartResponse {
  /** 虚拟机ID */
  readonly vmId: string
  /** 虚拟机状态 */
  readonly status: VirtualMachine['status']
  /** 命令ID */
  readonly commandId: string
  /** 预计时间（秒） */
  readonly estimatedTime: number
}

/**
 * 虚拟机停止响应
 */
export interface VMStopResponse {
  /** 虚拟机ID */
  readonly vmId: string
  /** 虚拟机状态 */
  readonly status: VirtualMachine['status']
  /** 命令ID */
  readonly commandId: string
  /** 预计时间（秒） */
  readonly estimatedTime: number
}

/**
 * 虚拟机重启响应
 */
export interface VMRestartResponse {
  /** 虚拟机ID */
  readonly vmId: string
  /** 虚拟机状态 */
  readonly status: VirtualMachine['status']
  /** 命令ID */
  readonly commandId: string
  /** 预计时间（秒） */
  readonly estimatedTime: number
}

// ==================== 业务逻辑类型 ====================

/**
 * 虚拟机操作类型
 */
export type VMOperation = 
  | 'UPDATE'
  | 'DELETE'
  | 'START'
  | 'STOP'
  | 'RESTART'
  | 'STATUS_CHECK'

/**
 * 虚拟机操作日志
 */
export interface VMOperationLog {
  /** 操作ID */
  readonly operationId: string
  /** 操作类型 */
  readonly operation: VMOperation
  /** 虚拟机ID */
  readonly vmId: string
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
  /** 命令ID（如果适用） */
  readonly commandId?: string
}

// ==================== 监控相关类型 ====================

/**
 * 虚拟机资源使用情况
 */
export interface VMResourceUsage {
  /** CPU使用率（百分比） */
  readonly cpu: number
  /** 内存使用率（百分比） */
  readonly memory: number
  /** 磁盘使用率（百分比） */
  readonly disk: number
  /** GPU使用率（百分比，可选） */
  readonly gpu?: number
}

/**
 * 虚拟机网络信息
 */
export interface VMNetworkInfo {
  /** IP地址 */
  readonly ipAddress: string
  /** MAC地址 */
  readonly macAddress?: string
  /** 端口 */
  readonly port: number
  /** 上传速度（KB/s） */
  readonly uploadSpeed?: number
  /** 下载速度（KB/s） */
  readonly downloadSpeed?: number
  /** 延迟（ms） */
  readonly latency?: number
}

/**
 * 虚拟机进程信息
 */
export interface VMProcessInfo {
  /** 总进程数 */
  readonly total: number
  /** 活跃进程数 */
  readonly active: number
  /** 系统进程数 */
  readonly system: number
  /** 用户进程数 */
  readonly user: number
}

// ==================== 统计类型 ====================

/**
 * 虚拟机统计信息
 */
export interface VMStatistics {
  /** 总虚拟机数 */
  readonly totalVMs: number
  /** 运行中的虚拟机数 */
  readonly runningVMs: number
  /** 已停止的虚拟机数 */
  readonly stoppedVMs: number
  /** 错误状态的虚拟机数 */
  readonly errorVMs: number
  /** 离线的虚拟机数 */
  readonly offlineVMs: number
  /** 按状态分布 */
  readonly statusDistribution: Record<VirtualMachine['status'], number>
  /** 按操作系统分布 */
  readonly osTypeDistribution: Record<string, number>
  /** 平均CPU核心数 */
  readonly averageCpuCores: number
  /** 平均内存大小（MB） */
  readonly averageMemoryMb: number
  /** 总CPU核心数 */
  readonly totalCpuCores: number
  /** 总内存大小（MB） */
  readonly totalMemoryMb: number
}

// ==================== 错误类型 ====================

/**
 * 虚拟机服务错误类型
 */
export interface VMServiceError {
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
 * 虚拟机操作错误类型
 */
export interface VMOperationError extends VMServiceError {
  /** 虚拟机ID */
  readonly vmId: string
  /** 操作类型 */
  readonly operation: VMOperation
  /** 失败原因 */
  readonly failureReason: 
    | 'INVALID_PARAMETER'
    | 'VM_NOT_FOUND'
    | 'VM_ALREADY_EXISTS'
    | 'CONNECTION_FAILED'
    | 'OPERATION_TIMEOUT'
    | 'INSUFFICIENT_RESOURCES'
    | 'SECURITY_ERROR'
    | 'UNKNOWN'
}

/**
 * 验证错误类型
 */
export interface VMValidationError extends VMServiceError {
  /** 验证失败的字段 */
  readonly field: string
  /** 验证规则 */
  readonly rule: string
  /** 期望值 */
  readonly expected?: unknown
  /** 实际值 */
  readonly actual?: unknown
}

// ==================== 配置类型 ====================

/**
 * 虚拟机服务配置
 */
export interface VMServiceConfig {
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
  /** WebSocket连接超时时间（毫秒） */
  readonly websocketTimeout: number
  /** 心跳检测间隔（毫秒） */
  readonly heartbeatInterval: number
}

/**
 * 虚拟机默认配置
 */
export interface VMDefaultConfig {
  /** 默认CPU核心数 */
  readonly defaultCpuCores: number
  /** 默认内存大小（MB） */
  readonly defaultMemoryMb: number
  /** 默认磁盘大小（GB） */
  readonly defaultDiskGb: number
  /** 默认操作超时时间（秒） */
  readonly defaultOperationTimeout: number
  /** 支持的操作系统类型 */
  readonly supportedOsTypes: string[]
  /** 支持的算法类型 */
  readonly supportedAlgorithms: string[]
}

// ==================== VM本地模型相关类型 ====================

/**
 * VM本地模型专用分页响应类型（符合 vm-round-models-api-reference.md）
 */
export interface VMRoundModelPaginatedResponse<T> {
  readonly total: number
  readonly current: number  // 使用 current 而不是 page
  readonly size: number
  readonly pages: number
  readonly records: T[]
}

/**
 * VM本地模型结果类型
 */
export interface VMRoundModel {
  /** VM本地模型ID */
  readonly vmRoundModelId: string
  /** 任务ID */
  readonly taskId: string
  /** 训练轮数 */
  readonly roundNumber: number
  /** 虚拟机ID */
  readonly vmId: string
  /** 模型JSON数据（可选） */
  readonly modelJson?: Record<string, unknown>
  /** 评估指标 */
  readonly metrics: {
    readonly accuracy: number
    readonly loss: number
    readonly [key: string]: number
  }
  /** 创建时间 */
  readonly createdAt: string
}

/**
 * VM本地模型训练指标趋势类型
 */
export interface VMModelTrend {
  /** 任务ID */
  readonly taskId: string
  /** 虚拟机ID */
  readonly vmId: string
  /** 指标名称 */
  readonly metric: string
  /** 趋势数据 */
  readonly trend: Array<{
    readonly roundNumber: number
    readonly value: number
  }>
}

/**
 * VM本地模型最佳/离群查询结果类型
 */
export interface VMModelBest {
  /** 任务ID */
  readonly taskId: string
  /** 指标名称 */
  readonly metric: string
  /** 查询类型 */
  readonly type: 'best' | 'outlier'
  /** 查询结果 */
  readonly result: {
    readonly vmRoundModelId: string
    readonly roundNumber: number
    readonly vmId: string
    readonly value: number
  }
}

/**
 * VM本地模型列表查询参数
 */
export interface VMRoundModelListParams extends PaginationParams {
  /** 任务ID过滤 */
  readonly taskId?: string
  /** 训练轮数过滤 */
  readonly roundNumber?: number
  /** 虚拟机ID过滤 */
  readonly vmId?: string
}

/**
 * VM模型趋势查询参数
 */
export interface VMModelTrendParams {
  /** 任务ID */
  readonly taskId: string
  /** 虚拟机ID */
  readonly vmId: string
  /** 指标名称 */
  readonly metric: string
}

/**
 * VM模型最佳/离群查询参数
 */
export interface VMModelBestParams {
  /** 任务ID */
  readonly taskId: string
  /** 指标名称 */
  readonly metric: string
  /** 查询类型 */
  readonly type: 'best' | 'outlier'
}

/**
 * 支持的指标类型
 */
export type MetricType = 'accuracy' | 'loss' | 'precision' | 'recall' | 'f1'

/**
 * 查询类型
 */
export type QueryType = 'best' | 'outlier'

/**
 * 支持的指标常量
 */
export const SUPPORTED_METRICS = {
  ACCURACY: 'accuracy',
  LOSS: 'loss',
  PRECISION: 'precision',
  RECALL: 'recall',
  F1: 'f1'
} as const

/**
 * 查询类型常量
 */
export const QUERY_TYPES = {
  BEST: 'best',
  OUTLIER: 'outlier'
} as const

// ==================== 重新导出基础类型 ====================
// 注：工具类型已从 @/types 统一导入，不再重复定义
