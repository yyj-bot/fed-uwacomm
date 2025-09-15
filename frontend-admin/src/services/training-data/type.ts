/**
 * 训练数据服务类型定义
 * 定义训练数据管理相关的请求和响应类型
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import type { 
  PaginationParams,
  DataStatistics,
  DeepReadonly,
  PartialBy,
  RequiredBy,
  PickBy,
  OmitBy
} from '@/types'

// ==================== 基础类型定义 ====================

/**
 * 数据类型枚举
 */
export type DataType = 'ACOUSTIC' | 'ENVIRONMENT' | 'MODEL' | 'FEATURE' | 'OTHER'

/**
 * 数据状态枚举
 */
export type DataStatus = 'UPLOADING' | 'PROCESSING' | 'VALIDATING' | 'READY' | 'ERROR' | 'DELETED'

/**
 * 批量操作类型枚举
 */
export type BatchOperationType = 'DELETE' | 'UPDATE' | 'VALIDATE'

/**
 * 导出类型枚举
 */
export type ExportType = 'CSV' | 'JSON' | 'EXCEL'

/**
 * 导出格式枚举
 */
export type ExportFormat = 'ZIP' | 'TAR'

// ==================== 上传相关类型 ====================

/**
 * 文件上传请求（FormData格式）
 */
export interface UploadFileRequest {
  /** 虚拟机ID */
  readonly vmId: string
  /** 数据类型 */
  readonly dataType: DataType
  /** 数据描述 */
  readonly description?: string
  /** 数据标签 */
  readonly tags?: string[]
  /** 元数据 */
  readonly metadata?: Record<string, unknown>
  /** 上传文件 */
  readonly file: File
}

/**
 * 文件上传响应
 */
export interface UploadFileResponse {
  /** 数据集ID */
  readonly datasetId: string
  /** 数据集描述 */
  readonly datasetDescription: string
  /** 数据集类型 */
  readonly datasetType: string
  /** 虚拟机ID */
  readonly vmId: string
  /** 状态 */
  readonly status: string
  /** 上传时间 */
  readonly uploadTime: string
  /** 上传者ID */
  readonly uploadedBy: string
  /** 上传进度 */
  readonly progress: number
}

/**
 * 文本上传请求
 */
export interface UploadTextRequest {
  /** 虚拟机ID */
  readonly vmId: string
  /** 数据类型 */
  readonly dataType: DataType
  /** 标题 */
  readonly title: string
  /** 内容 */
  readonly content: string
  /** 描述 */
  readonly description?: string
  /** 标签 */
  readonly tags?: string[]
  /** 元数据 */
  readonly metadata?: Record<string, unknown>
}

/**
 * 文本上传响应
 */
export interface UploadTextResponse {
  /** 数据集ID */
  readonly datasetId: string
  /** 数据集描述 */
  readonly datasetDescription: string
  /** 数据集类型 */
  readonly datasetType: string
  /** 虚拟机ID */
  readonly vmId: string
  /** 状态 */
  readonly status: string
  /** 上传时间 */
  readonly uploadTime: string
  /** 上传者ID */
  readonly uploadedBy: string
}

// ==================== 查询相关类型 ====================

/**
 * 数据列表查询参数
 */
export interface DataListParams extends PaginationParams {
  /** 虚拟机ID过滤 */
  readonly vmId?: string
  /** 数据类型过滤 */
  readonly dataType?: DataType
  /** 状态过滤 */
  readonly status?: DataStatus
  /** 关键词搜索 */
  readonly keyword?: string
  /** 开始日期 */
  readonly startDate?: string
  /** 结束日期 */
  readonly endDate?: string
  /** 标签过滤 */
  readonly tags?: string
}

/**
 * 数据列表响应
 */
export interface DataListResponse {
  /** 总数 */
  readonly total: number
  /** 当前页 */
  readonly page: number
  /** 每页大小 */
  readonly size: number
  /** 数据列表 */
  readonly dataList: Array<{
    readonly datasetId: string
    readonly datasetDescription: string
    readonly datasetType: string
    readonly vmId: string
    readonly status: string
    readonly tags?: string[]
  }>
}

// ==================== 处理相关类型 ====================

/**
 * 预处理请求
 */
export interface PreprocessRequest {
  /** 预处理方法 */
  readonly methods: string[]
  /** 预处理参数 */
  readonly parameters: Record<string, unknown>
  /** 输出格式 */
  readonly outputFormat?: string
}

/**
 * 验证请求
 */
export interface ValidationRequest {
  /** 数据类型 */
  readonly dataType?: DataType
  /** 必需列 */
  readonly requiredColumns?: string[]
  /** 数据类型映射 */
  readonly dataTypes?: Record<string, string>
  /** 约束条件 */
  readonly constraints?: Record<string, {
    readonly min?: number
    readonly max?: number
    readonly notNull?: boolean
  }>
  /** 质量检查项 */
  readonly qualityChecks?: string[]
}

// ==================== 修改相关类型 ====================

/**
 * 更新数据请求
 */
export interface UpdateDataRequest {
  /** 数据集描述 */
  readonly datasetDescription?: string
  /** 标签 */
  readonly tags?: string[]
  /** 元数据 */
  readonly metadata?: Record<string, unknown>
}

/**
 * 更新数据响应
 */
export interface UpdateDataResponse {
  /** 数据集ID */
  readonly datasetId: string
  /** 更新时间 */
  readonly updatedAt: string
  /** 更新者ID */
  readonly updatedBy: string
}

/**
 * 删除数据请求
 */
export interface DeleteDataRequest {
  /** 删除原因 */
  readonly reason?: string
  /** 是否删除文件 */
  readonly deleteFile?: boolean
  /** 是否删除元数据 */
  readonly deleteMetadata?: boolean
}

/**
 * 删除数据响应
 */
export interface DeleteDataResponse {
  /** 数据集ID */
  readonly datasetId: string
  /** 删除时间 */
  readonly deletedAt: string
  /** 删除者ID */
  readonly deletedBy: string
  /** 文件是否已删除 */
  readonly fileDeleted: boolean
  /** 元数据是否保留 */
  readonly metadataPreserved: boolean
}

// ==================== 批量操作相关类型 ====================

/**
 * 批量操作请求
 */
export interface BatchOperationRequest {
  /** 操作类型 */
  readonly operation: BatchOperationType
  /** 数据集ID列表 */
  readonly datasetIds: string[]
  /** 操作参数 */
  readonly parameters?: Record<string, unknown>
}

/**
 * 批量操作结果项
 */
export interface BatchOperationResultItem {
  /** 数据集ID */
  readonly datasetId: string
  /** 操作状态 */
  readonly status: string
  /** 操作消息 */
  readonly message: string
}

// ==================== 统计相关类型 ====================

/**
 * 数据统计查询参数
 */
export interface DataStatisticsParams {
  /** 虚拟机ID过滤 */
  readonly vmId?: string
  /** 数据类型过滤 */
  readonly dataType?: DataType
  /** 开始日期 */
  readonly startDate?: string
  /** 结束日期 */
  readonly endDate?: string
}

/**
 * 虚拟机分布信息
 */
export interface VMDistribution {
  /** 数据数量 */
  readonly count: number
  /** 数据大小（字节） */
  readonly size: number
}

/**
 * 顶级数据类型信息
 */
export interface TopDataType {
  /** 数据类型 */
  readonly dataType: string
  /** 数量 */
  readonly count: number
  /** 百分比 */
  readonly percentage: number
}

/**
 * 上传趋势信息
 */
export interface UploadTrend {
  /** 最近7天数据 */
  readonly last7Days: number[]
  /** 最近30天数据 */
  readonly last30Days: number[]
}

// ==================== 导出相关类型 ====================

/**
 * 导出数据请求
 */
export interface ExportDataRequest {
  /** 导出类型 */
  readonly exportType: ExportType
  /** 过滤条件 */
  readonly filters?: {
    readonly dataType?: DataType
    readonly vmId?: string
    readonly status?: DataStatus
    readonly startTime?: string
    readonly endTime?: string
  }
  /** 导出字段 */
  readonly fields?: string[]
  /** 导出格式 */
  readonly format?: ExportFormat
}

// ==================== 业务逻辑类型 ====================

/**
 * 数据操作类型
 */
export type DataOperation = 
  | 'UPLOAD_FILE'
  | 'UPLOAD_TEXT'
  | 'DOWNLOAD'
  | 'PREPROCESS'
  | 'VALIDATE'
  | 'UPDATE'
  | 'DELETE'
  | 'BATCH_OPERATION'
  | 'EXPORT'
  | 'STATISTICS'

/**
 * 数据操作日志
 */
export interface DataOperationLog {
  /** 操作ID */
  readonly operationId: string
  /** 操作类型 */
  readonly operation: DataOperation
  /** 数据集ID */
  readonly datasetId: string
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
 * 文件信息
 */
export interface FileInfo {
  /** 文件名 */
  readonly fileName: string
  /** 文件大小（字节） */
  readonly fileSize: number
  /** 文件类型 */
  readonly fileType: string
  /** 文件扩展名 */
  readonly fileExtension: string
  /** 文件MD5校验值 */
  readonly fileMD5: string
  /** 文件上传时间 */
  readonly uploadTime: string
  /** 文件存储路径 */
  readonly filePath: string
}

/**
 * 数据质量信息
 */
export interface DataQuality {
  /** 数据完整性得分 */
  readonly completenessScore: number
  /** 数据一致性得分 */
  readonly consistencyScore: number
  /** 数据准确性得分 */
  readonly accuracyScore: number
  /** 数据及时性得分 */
  readonly timelinessScore: number
  /** 总体质量得分 */
  readonly overallScore: number
  /** 质量评估时间 */
  readonly assessmentTime: string
}

/**
 * 数据血缘信息
 */
export interface DataLineage {
  /** 源数据集ID列表 */
  readonly sourceDatasets: string[]
  /** 派生数据集ID列表 */
  readonly derivedDatasets: string[]
  /** 处理步骤 */
  readonly processingSteps: Array<{
    readonly stepId: string
    readonly stepName: string
    readonly stepType: string
    readonly parameters: Record<string, unknown>
    readonly timestamp: string
  }>
  /** 血缘关系图 */
  readonly lineageGraph: Record<string, unknown>
}

// ==================== 监控相关类型 ====================

/**
 * 数据处理监控信息
 */
export interface ProcessingMonitor {
  /** 任务ID */
  readonly taskId: string
  /** 数据集ID */
  readonly datasetId: string
  /** 处理类型 */
  readonly processingType: string
  /** 当前状态 */
  readonly currentStatus: string
  /** 开始时间 */
  readonly startTime: string
  /** 预计完成时间 */
  readonly estimatedEndTime?: string
  /** 进度百分比 */
  readonly progress: number
  /** 处理日志 */
  readonly logs: Array<{
    readonly timestamp: string
    readonly level: string
    readonly message: string
  }>
  /** 资源使用情况 */
  readonly resourceUsage: {
    readonly cpuUsage: number
    readonly memoryUsage: number
    readonly diskUsage: number
  }
}

/**
 * 存储监控信息
 */
export interface StorageMonitor {
  /** 总存储容量（字节） */
  readonly totalCapacity: number
  /** 已使用存储（字节） */
  readonly usedStorage: number
  /** 可用存储（字节） */
  readonly availableStorage: number
  /** 存储使用率 */
  readonly usagePercentage: number
  /** 按数据类型分组的存储使用 */
  readonly storageByType: Record<DataType, number>
  /** 按虚拟机分组的存储使用 */
  readonly storageByVM: Record<string, number>
  /** 存储趋势 */
  readonly storageTrend: {
    readonly daily: number[]
    readonly weekly: number[]
    readonly monthly: number[]
  }
}

// ==================== 错误类型 ====================

/**
 * 训练数据服务错误类型
 */
export interface TrainingDataServiceError {
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
 * 数据操作错误类型
 */
export interface DataOperationError extends TrainingDataServiceError {
  /** 数据集ID */
  readonly datasetId: string
  /** 操作类型 */
  readonly operation: DataOperation
  /** 失败原因 */
  readonly failureReason: 
    | 'INVALID_PARAMETER'
    | 'DATA_NOT_FOUND'
    | 'DATA_ALREADY_EXISTS'
    | 'FILE_TOO_LARGE'
    | 'FILE_TYPE_NOT_SUPPORTED'
    | 'FILE_CORRUPTED'
    | 'INSUFFICIENT_STORAGE'
    | 'QUOTA_EXCEEDED'
    | 'ACCESS_DENIED'
    | 'PROCESSING_FAILED'
    | 'VALIDATION_FAILED'
    | 'UPLOAD_TIMEOUT'
    | 'DOWNLOAD_FAILED'
    | 'UNKNOWN'
}

/**
 * 文件验证错误类型
 */
export interface FileValidationError extends TrainingDataServiceError {
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
 * 训练数据服务配置
 */
export interface TrainingDataServiceConfig {
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
 * 文件上传配置
 */
export interface UploadConfig {
  /** 单个文件最大大小（字节） */
  readonly maxFileSize: number
  /** 支持的文件类型 */
  readonly supportedFileTypes: Record<DataType, string[]>
  /** 是否启用断点续传 */
  readonly enableResumeUpload: boolean
  /** 是否启用分片上传 */
  readonly enableChunkUpload: boolean
  /** 上传分片大小（字节） */
  readonly chunkSize: number
  /** 并发上传分片数 */
  readonly concurrentChunks: number
}

/**
 * 数据处理配置
 */
export interface ProcessingConfig {
  /** 默认预处理方法 */
  readonly defaultPreprocessMethods: string[]
  /** 支持的预处理方法 */
  readonly supportedPreprocessMethods: string[]
  /** 默认验证规则 */
  readonly defaultValidationRules: Record<DataType, ValidationRequest>
  /** 处理超时时间（毫秒） */
  readonly processingTimeout: number
  /** 最大并发处理任务数 */
  readonly maxConcurrentProcessing: number
}

// ==================== 工具类型 ====================
// 注：工具类型已从 @/types 统一导入，不再重复定义

// ==================== 常量类型 ====================

/**
 * 数据类型常量
 */
export const DATA_TYPES = {
  ACOUSTIC: 'ACOUSTIC',
  ENVIRONMENT: 'ENVIRONMENT',
  MODEL: 'MODEL',
  FEATURE: 'FEATURE',
  OTHER: 'OTHER'
} as const

/**
 * 数据状态常量
 */
export const DATA_STATUSES = {
  UPLOADING: 'UPLOADING',
  PROCESSING: 'PROCESSING',
  VALIDATING: 'VALIDATING',
  READY: 'READY',
  ERROR: 'ERROR',
  DELETED: 'DELETED'
} as const

/**
 * 批量操作类型常量
 */
export const BATCH_OPERATIONS = {
  DELETE: 'DELETE',
  UPDATE: 'UPDATE',
  VALIDATE: 'VALIDATE'
} as const

/**
 * 导出类型常量
 */
export const EXPORT_TYPES = {
  CSV: 'CSV',
  JSON: 'JSON',
  EXCEL: 'EXCEL'
} as const

/**
 * 导出格式常量
 */
export const EXPORT_FORMATS = {
  ZIP: 'ZIP',
  TAR: 'TAR'
} as const

// ==================== 重新导出基础类型 ====================
// 注：基础类型已从 @/types 统一导入
