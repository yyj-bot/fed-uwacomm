/**
 * 模型版本管理服务类型定义
 * 定义模型版本管理相关的请求和响应类型
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import type { PaginationParams, SortParams, DeepReadonly, PartialBy, RequiredBy, PickBy, OmitBy } from '@/types'

// ==================== 基础类型定义 ====================

/**
 * 模型状态枚举
 */
export type ModelStatus = 'UPLOADING' | 'UPLOADED' | 'VALIDATING' | 'VALIDATED' | 'DEPLOYED' | 'DEPRECATED' | 'FAILED'

/**
 * 部署状态枚举
 */
export type DeploymentStatusType = 'PENDING' | 'DEPLOYING' | 'RUNNING' | 'STOPPED' | 'FAILED'

/**
 * 回滚状态枚举
 */
export type RollbackStatus = 'PENDING' | 'ROLLING_BACK' | 'COMPLETED' | 'FAILED'

/**
 * 评估状态枚举
 */
export type EvaluationStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED'

/**
 * 下载格式枚举
 */
export type DownloadFormat = 'original' | 'onnx'

/**
 * 时间范围枚举
 */
export type TimeRange = '7d' | '30d' | '90d'

// ==================== 上传相关类型 ====================

/**
 * 模型上传请求（FormData格式）
 */
export interface UploadModelRequest {
  /** 关联任务ID */
  readonly taskId: string
  /** 训练轮数 */
  readonly roundNumber: number
  /** 模型描述 */
  readonly description?: string
  /** 模型参数 */
  readonly parameters?: Record<string, unknown>
  /** 模型文件 */
  readonly file: File
}

/**
 * 模型上传响应
 */
export interface UploadModelResponse {
  /** 模型ID */
  readonly modelId: string
  /** 任务ID */
  readonly taskId: string
  /** 训练轮数 */
  readonly roundNumber: number
  /** 状态 */
  readonly status: string
  /** 模型描述 */
  readonly description?: string
  /** 模型参数 */
  readonly parameters: Record<string, unknown>
  /** 创建时间 */
  readonly createdAt: string
}

/**
 * 批量上传请求
 */
export interface BatchUploadRequest {
  /** 关联任务ID */
  readonly taskId: string
  /** 模型列表 */
  readonly models: Array<{
    readonly roundNumber: number
    readonly description?: string
    readonly parameters?: Record<string, unknown>
    readonly file: File
  }>
}

/**
 * 批量上传响应
 */
export interface BatchUploadResponse {
  /** 成功数量 */
  readonly successCount: number
  /** 失败数量 */
  readonly failedCount: number
  /** 模型结果列表 */
  readonly models: Array<{
    readonly modelId: string
    readonly status: string
    readonly message: string
  }>
}

// ==================== 查询相关类型 ====================

/**
 * 模型版本专用分页响应类型（符合 model-version-api-reference.md）
 */
export interface ModelVersionPaginatedResponse<T> {
  readonly total: number
  readonly current: number  // 使用 current 而不是 page
  readonly size: number
  readonly pages: number
  readonly records: T[]
}

/**
 * 模型版本列表查询参数
 */
export interface ModelVersionListParams extends PaginationParams, SortParams {
  /** 任务ID过滤 */
  readonly taskId?: string
  /** 训练轮数过滤 */
  readonly roundNumber?: number
  /** 状态过滤 */
  readonly status?: ModelStatus
}

/**
 * 模型版本详情
 */
export interface ModelVersionDetail {
  /** 模型ID */
  readonly modelId: string
  /** 任务ID */
  readonly taskId: string
  /** 训练轮数 */
  readonly roundNumber: number
  /** 聚合方法 */
  readonly aggregationMethod: string
  /** 客户端数量 */
  readonly clientCount: number
  /** 模型JSON数据 */
  readonly modelJson: Record<string, unknown>
  /** 评估指标 */
  readonly metrics: {
    readonly accuracy: number
    readonly loss: number
  }
  /** 创建时间 */
  readonly createdAt: string
  /** 聚合完成时间 */
  readonly aggregatedAt: string
  /** 状态 */
  readonly status: string
}

/**
 * 任务模型版本信息
 */
export interface TaskModelVersions {
  /** 任务ID */
  readonly taskId: string
  /** 任务名称 */
  readonly taskName: string
  /** 模型总数 */
  readonly totalModels: number
  /** 版本列表 */
  readonly versions: Array<{
    readonly modelId: string
    readonly roundNumber: number
    readonly accuracy: number
    readonly loss: number
    readonly status: string
    readonly createdAt: string
  }>
}

// ==================== 评估相关类型 ====================

/**
 * 模型评估请求
 */
export interface EvaluationRequest {
  /** 模型ID */
  readonly modelId: string
  /** 测试数据路径 */
  readonly testDataPath: string
  /** 评估指标 */
  readonly metrics?: string[]
  /** 批次大小 */
  readonly batchSize?: number
  /** 计算设备 */
  readonly device?: string
}

/**
 * 模型评估响应
 */
export interface EvaluationResponse {
  /** 模型ID */
  readonly modelId: string
  /** 评估ID */
  readonly evaluationId: string
  /** 评估指标结果 */
  readonly metrics: Record<string, number>
  /** 评估时间（秒） */
  readonly evaluationTime: number
  /** 测试样本数 */
  readonly testSamples: number
  /** 状态 */
  readonly status: string
  /** 创建时间 */
  readonly createdAt: string
}

/**
 * 批量模型评估请求
 */
export interface BatchEvaluationRequest {
  /** 任务ID */
  readonly taskId: string
  /** 测试数据路径 */
  readonly testDataPath: string
  /** 评估轮数列表 */
  readonly roundNumbers?: number[]
  /** 评估指标 */
  readonly metrics?: string[]
  /** 批次大小 */
  readonly batchSize?: number
}

/**
 * 批量模型评估响应
 */
export interface BatchEvaluationResponse {
  /** 任务ID */
  readonly taskId: string
  /** 评估数量 */
  readonly evaluatedCount: number
  /** 评估结果列表 */
  readonly results: Array<{
    readonly modelId: string
    readonly roundNumber: number
    readonly accuracy: number
    readonly loss: number
    readonly status: string
  }>
}

// ==================== 部署相关类型 ====================

/**
 * 部署配置类型
 */
export interface DeploymentConfig {
  replicas?: number
  resources?: {
    cpu?: string
    memory?: string
  }
  environment?: Record<string, string>
}

/**
 * 模型部署请求
 */
export interface DeploymentRequest {
  /** 模型ID */
  readonly modelId: string
  /** 部署名称 */
  readonly deploymentName: string
  /** 目标虚拟机列表 */
  readonly targetVms?: string[]
  /** 部署配置 */
  readonly deploymentConfig?: DeploymentConfig
  /** 部署描述 */
  readonly description?: string
}

/**
 * 模型部署响应
 */
export interface DeploymentResponse {
  /** 部署ID */
  readonly deploymentId: string
  /** 模型ID */
  readonly modelId: string
  /** 部署名称 */
  readonly deploymentName: string
  /** 目标虚拟机列表 */
  readonly targetVms?: string[]
  /** 状态 */
  readonly status: string
  /** 部署配置 */
  readonly deploymentConfig?: DeploymentConfig
  /** 服务端点列表 */
  readonly endpoints?: string[]
  /** 创建时间 */
  readonly createdAt: string
}

/**
 * 部署状态信息
 */
export interface DeploymentStatus {
  /** 部署ID */
  readonly deploymentId: string
  /** 模型ID */
  readonly modelId: string
  /** 部署名称 */
  readonly deploymentName: string
  /** 状态 */
  readonly status: string
  /** 副本信息 */
  readonly replicas?: {
    readonly desired: number
    readonly available: number
    readonly ready: number
  }
  /** 服务端点列表 */
  readonly endpoints?: string[]
  /** 健康检查信息 */
  readonly healthCheck?: {
    readonly status: string
    readonly lastCheck: string
    readonly responseTime: number
  }
  /** 创建时间 */
  readonly createdAt: string
  /** 更新时间 */
  readonly updatedAt: string
}

// ==================== 回滚相关类型 ====================

/**
 * 模型回滚请求
 */
export interface RollbackRequest {
  /** 部署ID */
  readonly deploymentId: string
  /** 目标模型ID */
  readonly targetModelId: string
  /** 回滚原因 */
  readonly rollbackReason?: string
  /** 强制回滚 */
  readonly force?: boolean
}

/**
 * 模型回滚响应
 */
export interface RollbackResponse {
  /** 回滚ID */
  readonly rollbackId: string
  /** 部署ID */
  readonly deploymentId: string
  /** 源模型ID */
  readonly fromModelId: string
  /** 目标模型ID */
  readonly toModelId: string
  /** 状态 */
  readonly status: string
  /** 回滚原因 */
  readonly rollbackReason?: string
  /** 回滚时间（秒） */
  readonly rollbackTime: number
  /** 创建时间 */
  readonly createdAt: string
}

// ==================== 下载相关类型 ====================

/**
 * 模型下载请求
 */
export interface DownloadRequest {
  /** 下载格式 */
  readonly format?: DownloadFormat
  /** 是否压缩 */
  readonly compressed?: boolean
}

/**
 * 批量下载请求
 */
export interface BatchDownloadRequest {
  /** 模型ID列表 */
  readonly modelIds: string[]
  /** 下载格式 */
  readonly format?: DownloadFormat
  /** 是否压缩 */
  readonly compressed?: boolean
}

// ==================== 删除相关类型 ====================

/**
 * 删除模型请求
 */
export interface DeleteModelRequest {
  /** 强制删除 */
  readonly force?: boolean
  /** 是否删除文件 */
  readonly deleteFile?: boolean
}

/**
 * 删除模型响应
 */
export interface DeleteModelResponse {
  /** 模型ID */
  readonly modelId: string
  /** 删除时间 */
  readonly deletedAt: string
}

/**
 * 批量删除请求
 */
export interface BatchDeleteRequest {
  /** 模型ID列表 */
  readonly modelIds: string[]
  /** 强制删除 */
  readonly force?: boolean
  /** 是否删除文件 */
  readonly deleteFile?: boolean
}

/**
 * 批量删除响应
 */
export interface BatchDeleteResponse {
  /** 成功数量 */
  readonly successCount: number
  /** 失败数量 */
  readonly failedCount: number
  /** 删除结果列表 */
  readonly results: Array<{
    readonly modelId: string
    readonly status: string
    readonly message: string
  }>
}

// ==================== 统计相关类型 ====================

/**
 * 统计查询参数
 */
export interface StatisticsParams {
  /** 任务ID过滤 */
  readonly taskId?: string
  /** 时间范围 */
  readonly timeRange?: TimeRange
}

/**
 * 任务模型统计信息
 */
export interface TaskStatistics {
  /** 任务ID */
  readonly taskId: string
  /** 任务名称 */
  readonly taskName: string
  /** 总轮数 */
  readonly totalRounds: number
  /** 已完成轮数 */
  readonly completedRounds: number
  /** 性能指标 */
  readonly performanceMetrics: {
    readonly bestAccuracy: number
    readonly bestRound: number
    readonly averageAccuracy: number
    readonly accuracyImprovement: number
  }
}

// ==================== 业务逻辑类型 ====================

/**
 * 模型操作类型
 */
export type ModelOperation = 
  | 'UPLOAD'
  | 'BATCH_UPLOAD'
  | 'EVALUATE'
  | 'BATCH_EVALUATE'
  | 'DEPLOY'
  | 'ROLLBACK'
  | 'DOWNLOAD'
  | 'BATCH_DOWNLOAD'
  | 'DELETE'
  | 'BATCH_DELETE'
  | 'QUERY'
  | 'STATISTICS'

/**
 * 模型操作日志
 */
export interface ModelOperationLog {
  /** 操作ID */
  readonly operationId: string
  /** 操作类型 */
  readonly operation: ModelOperation
  /** 模型ID */
  readonly modelId: string
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

/**
 * 模型版本信息
 */
export interface ModelVersionInfo {
  /** 模型ID */
  readonly modelId: string
  /** 任务ID */
  readonly taskId: string
  /** 版本号 */
  readonly version: string
  /** 训练轮数 */
  readonly roundNumber: number
  /** 模型大小（字节） */
  readonly modelSize: number
  /** 模型格式 */
  readonly modelFormat: string
  /** 性能指标 */
  readonly metrics: Record<string, number>
  /** 创建时间 */
  readonly createdAt: string
  /** 更新时间 */
  readonly updatedAt: string
  /** 状态 */
  readonly status: ModelStatus
}

/**
 * 模型比较结果
 */
export interface ModelComparisonResult {
  /** 基线模型ID */
  readonly baselineModelId: string
  /** 比较模型ID */
  readonly comparisonModelId: string
  /** 性能差异 */
  readonly performanceDiff: Record<string, number>
  /** 模型大小差异 */
  readonly sizeDiff: number
  /** 推荐建议 */
  readonly recommendation: string
  /** 比较时间 */
  readonly comparedAt: string
}

// ==================== 监控相关类型 ====================

/**
 * 模型性能监控
 */
export interface ModelPerformanceMonitor {
  /** 模型ID */
  readonly modelId: string
  /** 部署ID */
  readonly deploymentId: string
  /** 实时指标 */
  readonly realTimeMetrics: {
    readonly requestsPerSecond: number
    readonly averageLatency: number
    readonly errorRate: number
    readonly cpuUsage: number
    readonly memoryUsage: number
  }
  /** 历史趋势 */
  readonly historicalTrend: Array<{
    readonly timestamp: string
    readonly metrics: Record<string, number>
  }>
  /** 监控时间 */
  readonly monitoredAt: string
}

/**
 * 部署健康状态
 */
export interface DeploymentHealthStatus {
  /** 部署ID */
  readonly deploymentId: string
  /** 整体健康状态 */
  readonly overallStatus: 'HEALTHY' | 'DEGRADED' | 'UNHEALTHY'
  /** 服务实例状态 */
  readonly instanceStatuses: Array<{
    readonly instanceId: string
    readonly status: 'RUNNING' | 'STOPPED' | 'ERROR'
    readonly lastHeartbeat: string
    readonly metrics: Record<string, number>
  }>
  /** 负载均衡状态 */
  readonly loadBalancerStatus: {
    readonly status: 'ACTIVE' | 'INACTIVE'
    readonly activeConnections: number
    readonly requestDistribution: Record<string, number>
  }
  /** 检查时间 */
  readonly checkedAt: string
}

// ==================== 错误类型 ====================

/**
 * 模型版本服务错误类型
 */
export interface ModelVersionServiceError {
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
 * 模型操作错误类型
 */
export interface ModelOperationError extends ModelVersionServiceError {
  /** 模型ID */
  readonly modelId: string
  /** 操作类型 */
  readonly operation: ModelOperation
  /** 失败原因 */
  readonly failureReason: 
    | 'INVALID_PARAMETER'
    | 'MODEL_NOT_FOUND'
    | 'MODEL_FORMAT_UNSUPPORTED'
    | 'MODEL_SIZE_EXCEEDED'
    | 'MODEL_VALIDATION_FAILED'
    | 'MODEL_DEPLOYMENT_FAILED'
    | 'MODEL_ROLLBACK_FAILED'
    | 'MODEL_DELETE_FAILED'
    | 'EVALUATION_FAILED'
    | 'DEPLOYMENT_NOT_FOUND'
    | 'DEPLOYMENT_IN_USE'
    | 'INSUFFICIENT_RESOURCES'
    | 'UNKNOWN'
}

/**
 * 文件验证错误类型
 */
export interface ModelFileValidationError extends ModelVersionServiceError {
  /** 文件名 */
  readonly fileName: string
  /** 验证失败的规则 */
  readonly failedRule: string
  /** 期望值 */
  readonly expected?: unknown
  /** 实际值 */
  readonly actual?: unknown
}

// ==================== 配置类型 ====================

/**
 * 模型版本服务配置
 */
export interface ModelVersionServiceConfig {
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
  /** 文件上传块大小（字节） */
  readonly uploadChunkSize: number
  /** 最大并发上传数 */
  readonly maxConcurrentUploads: number
}

/**
 * 模型上传配置
 */
export interface ModelUploadConfig {
  /** 单个模型文件最大大小（字节） */
  readonly maxFileSize: number
  /** 支持的模型格式 */
  readonly supportedFormats: string[]
  /** 是否启用断点续传 */
  readonly enableResumeUpload: boolean
  /** 是否启用分片上传 */
  readonly enableChunkUpload: boolean
  /** 上传分片大小（字节） */
  readonly chunkSize: number
  /** 并发上传分片数 */
  readonly concurrentChunks: number
  /** 模型验证规则 */
  readonly validationRules: {
    readonly checkFormat: boolean
    readonly checkSize: boolean
    readonly checkIntegrity: boolean
    readonly scanMalware: boolean
  }
}

/**
 * 模型部署配置
 */
export interface ModelDeploymentConfig {
  /** 默认副本数 */
  readonly defaultReplicas: number
  /** 最大副本数 */
  readonly maxReplicas: number
  /** 默认资源配置 */
  readonly defaultResources: {
    readonly cpu: string
    readonly memory: string
  }
  /** 健康检查配置 */
  readonly healthCheck: {
    readonly enabled: boolean
    readonly interval: number
    readonly timeout: number
    readonly retries: number
    readonly path: string
  }
  /** 负载均衡配置 */
  readonly loadBalancer: {
    readonly algorithm: 'ROUND_ROBIN' | 'LEAST_CONNECTIONS' | 'IP_HASH'
    readonly sessionAffinity: boolean
    readonly healthyThreshold: number
  }
}

// ==================== 工具类型 ====================

// ==================== 常量类型 ====================
// 注：工具类型已从 @/types 统一导入，不再重复定义

/**
 * 模型状态常量
 */
export const MODEL_STATUSES = {
  UPLOADING: 'UPLOADING',
  UPLOADED: 'UPLOADED',
  VALIDATING: 'VALIDATING',
  VALIDATED: 'VALIDATED',
  DEPLOYED: 'DEPLOYED',
  DEPRECATED: 'DEPRECATED',
  FAILED: 'FAILED'
} as const

/**
 * 部署状态常量
 */
export const DEPLOYMENT_STATUSES = {
  PENDING: 'PENDING',
  DEPLOYING: 'DEPLOYING',
  RUNNING: 'RUNNING',
  STOPPED: 'STOPPED',
  FAILED: 'FAILED'
} as const

/**
 * 回滚状态常量
 */
export const ROLLBACK_STATUSES = {
  PENDING: 'PENDING',
  ROLLING_BACK: 'ROLLING_BACK',
  COMPLETED: 'COMPLETED',
  FAILED: 'FAILED'
} as const

/**
 * 评估状态常量
 */
export const EVALUATION_STATUSES = {
  PENDING: 'PENDING',
  RUNNING: 'RUNNING',
  COMPLETED: 'COMPLETED',
  FAILED: 'FAILED'
} as const

/**
 * 下载格式常量
 */
export const DOWNLOAD_FORMATS = {
  ORIGINAL: 'original',
  ONNX: 'onnx'
} as const

/**
 * 时间范围常量
 */
export const TIME_RANGES = {
  SEVEN_DAYS: '7d',
  THIRTY_DAYS: '30d',
  NINETY_DAYS: '90d'
} as const

/**
 * 模型操作常量
 */
export const MODEL_OPERATIONS = {
  UPLOAD: 'UPLOAD',
  BATCH_UPLOAD: 'BATCH_UPLOAD',
  EVALUATE: 'EVALUATE',
  BATCH_EVALUATE: 'BATCH_EVALUATE',
  DEPLOY: 'DEPLOY',
  ROLLBACK: 'ROLLBACK',
  DOWNLOAD: 'DOWNLOAD',
  BATCH_DOWNLOAD: 'BATCH_DOWNLOAD',
  DELETE: 'DELETE',
  BATCH_DELETE: 'BATCH_DELETE',
  QUERY: 'QUERY',
  STATISTICS: 'STATISTICS'
} as const

/**
 * 支持的模型格式常量
 */
export const SUPPORTED_MODEL_FORMATS = {
  PYTORCH: ['.pth', '.pt'],
  TENSORFLOW: ['.h5', '.pb', '.savedmodel'],
  ONNX: ['.onnx'],
  PICKLE: ['.pkl', '.pickle'],
  JOBLIB: ['.joblib']
} as const

// ==================== 其他需要的类型定义 ====================

/**
 * 评估结果类型
 */
export interface EvaluationResult {
  evaluationId: string
  modelId: string
  taskId: string
  metrics: Record<string, number>
  evaluationTime: number
  testSamples: number
  status: string
  createdAt: string
}

/**
 * 回滚信息类型
 */
export interface RollbackInfo {
  rollbackId: string
  deploymentId: string
  fromModelId: string
  toModelId: string
  status: string
  rollbackReason?: string
  rollbackTime?: number
  createdAt: string
}

/**
 * 统计信息类型
 */
export interface ModelStatistics {
  totalModels: number
  averageAccuracy: number
  averageLoss: number
  uploadTrend: Array<{ date: string; count: number }>
  accuracyTrend: Array<{ roundNumber: number; accuracy: number }>
}
