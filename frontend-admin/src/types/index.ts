// 水声联邦学习系统类型定义 - 严格按照API文档设计

// WebSocket相关类型
export * from './websocket'

// 工具类型
export * from './utils'

// ==================== 通用响应类型 ====================
export interface ApiResponse<T> {
  readonly code: number
  readonly message: string
  readonly data: T
}

// ==================== 用户管理类型 ====================
export interface User {
  readonly userId: string
  readonly username: string
  readonly email: string
  readonly role: 'ADMIN' | 'RESEARCHER' | 'OPERATOR' | 'VIEWER'
  readonly status: 'ACTIVE' | 'INACTIVE' | 'LOCKED' | 'DELETED'
  readonly lastLoginTime?: string
  readonly lastLoginIp?: string
  readonly createdAt: string
  readonly updatedAt: string
}

export interface LoginRequest {
  readonly loginIdentifier: string
  readonly password: string
  readonly captcha?: string
  readonly captchaKey?: string
  readonly rememberMe?: boolean
}

export interface LoginResponse {
  readonly token: string
  readonly refreshToken: string
  readonly expiresIn: number
  readonly user: User
}

// ==================== 虚拟机管理类型 ====================
export interface VirtualMachine {
  readonly vmId: string
  readonly name: string
  readonly ipAddress: string
  readonly port: number
  readonly status: 'RUNNING' | 'STOPPED' | 'STARTING' | 'STOPPING' | 'ERROR' | 'OFFLINE'
  readonly osType: string
  readonly cpuCores: number
  readonly memoryMb: number
  readonly diskGb: number
  readonly connectionStatus: 'CONNECTED' | 'DISCONNECTED'
  readonly lastHeartbeat?: string
  readonly createdAt: string
  readonly updatedAt: string
  readonly systemInfo?: {
    readonly os?: string
    readonly kernel?: string
    readonly python?: string
    readonly gpu?: string
    readonly cuda?: string
    readonly cudnn?: string
  }
  readonly capabilities?: {
    readonly supportedAlgorithms?: string[]
    readonly maxBatchSize?: number
    readonly maxMemoryUsage?: number
    readonly gpuMemory?: number
    readonly networkSpeed?: number
  }
}

export interface VMResourceUsage {
  readonly cpu: number
  readonly memory: number
  readonly disk: number
  readonly gpu?: number
}

export interface VMStatus {
  readonly vmId: string
  readonly status: string
  readonly connectionStatus: string
  readonly uptime: number
  readonly resourceUsage: VMResourceUsage
  readonly network: {
    readonly ipAddress: string
    readonly macAddress: string
    readonly port: number
    readonly uploadSpeed: number
    readonly downloadSpeed: number
    readonly latency: number
  }
  readonly processes: {
    readonly total: number
    readonly active: number
    readonly system: number
    readonly user: number
  }
  readonly lastHeartbeat: string
  readonly wsSessionId?: string
}

// ==================== 联邦学习任务类型 ====================
export interface FederatedTask {
  readonly taskId: string
  readonly taskName: string
  readonly taskType: 'CLASSIFICATION' | 'REGRESSION' | 'CLUSTERING' | 'ANOMALY_DETECTION'
  readonly status: 'CREATED' | 'CONFIGURED' | 'RUNNING' | 'PAUSED' | 'STOPPED' | 'COMPLETED' | 'FAILED' | 'CANCELLED'
  readonly createdAt: string
  readonly startedAt?: string
  readonly completedAt?: string
  readonly currentRound?: number
  readonly totalRounds?: number
  readonly progress?: number
  readonly participantCount: number
  readonly finalAccuracy?: number
}

export interface TaskParticipant {
  readonly vmId: string
  readonly role: 'PARTICIPANT'
  readonly status: 'CONNECTED' | 'TRAINING' | 'IDLE' | 'ERROR'
  readonly dataSource: string
  readonly lastHeartbeat?: string
  readonly currentEpoch?: number
  readonly loss?: number
  readonly accuracy?: number
}

export interface TaskMetrics {
  readonly globalLoss: number
  readonly globalAccuracy: number
  readonly communicationRounds: number
  readonly dataProcessed: number
  readonly estimatedTimeRemaining: number
}

export interface FederatedTaskDetails extends FederatedTask {
  readonly algorithm: string
  readonly participants: TaskParticipant[]
  readonly metrics?: TaskMetrics
  readonly hyperparameters?: Record<string, unknown>
  readonly modelConfig?: Record<string, unknown>
}

export interface TaskResults {
  readonly taskId: string
  readonly taskName: string
  readonly status: string
  readonly finalResults: {
    readonly accuracy: number
    readonly loss: number
    readonly precision?: number
    readonly recall?: number
    readonly f1Score?: number
    readonly confusionMatrix?: number[][]
  }
  readonly roundResults: Array<{
    readonly round: number
    readonly accuracy: number
    readonly loss: number
    readonly participants: string[]
  }>
  readonly participantResults: Array<{
    readonly vmId: string
    readonly finalAccuracy: number
    readonly finalLoss: number
    readonly trainingTime: number
    readonly dataSize: number
    readonly parameters: Record<string, unknown>
  }>
  readonly modelInfo: {
    readonly parameters: Record<string, unknown>
  }
}

// ==================== 模型版本类型 ====================
export interface ModelVersion {
  readonly modelId: string
  readonly taskId: string
  readonly roundNumber: number
  readonly accuracy: number
  readonly loss: number
  readonly status: 'UPLOADING' | 'UPLOADED' | 'VALIDATING' | 'VALIDATED' | 'DEPLOYED' | 'DEPRECATED' | 'FAILED'
  readonly description?: string
  readonly parameters: Record<string, unknown>
  readonly createdAt: string
}

// ==================== 训练数据类型 ====================
export interface TrainingDataset {
  readonly datasetId: string
  readonly datasetDescription: string
  readonly datasetType: 'ACOUSTIC' | 'ENVIRONMENT' | 'MODEL' | 'FEATURE' | 'OTHER'
  readonly status: 'UPLOADING' | 'PROCESSING' | 'VALIDATING' | 'READY' | 'ERROR' | 'DELETED'
  readonly uploadTime: string
  readonly uploadedBy: string
  readonly tags?: string[]
  readonly metadata?: Record<string, unknown>
  readonly progress?: number
}

// ==================== 系统日志类型 ====================
export interface SystemLog {
  readonly logId: string
  readonly level: 'DEBUG' | 'INFO' | 'WARN' | 'ERROR'
  readonly category: 'SYSTEM' | 'USER' | 'VM' | 'TASK' | 'DATA' | 'MODEL' | 'SECURITY' | 'PERFORMANCE'
  readonly vmId?: string
  readonly taskId?: string
  readonly message: string
  readonly details?: Record<string, unknown>
  readonly createdAt: string
}

// ==================== 分页响应类型 ====================
export interface PaginatedResponse<T> {
  readonly total: number
  readonly page: number
  readonly size: number
  readonly pages?: number
  readonly records?: T[]
  readonly list?: T[]
}

// ==================== 查询参数类型 ====================
export interface PaginationParams {
  readonly page?: number
  readonly size?: number
}

export interface SortParams {
  readonly sort?: string
  readonly order?: 'asc' | 'desc'
}

export interface TimeRangeParams {
  readonly startTime?: string
  readonly endTime?: string
}

// ==================== WebSocket消息类型 ====================
export interface WebSocketMessage {
  readonly type: string
  readonly id: string
  readonly timestamp: string
  readonly vmId: string
  readonly data: unknown
  readonly signature?: string
}

// ==================== 图表配置类型 ====================
export interface ChartConfig {
  readonly title: string
  readonly width?: number
  readonly height?: number
  readonly responsive?: boolean
}

// ==================== 本地模型分析类型 ====================
export interface VMRoundModel {
  vmRoundModelId: string
  vmId: string
  roundNumber: number
  metrics: {
    accuracy: number
    loss: number
  }
  createdAt: string
}

export interface VMModelTrend {
  trend: Array<{
    roundNumber: number
    value: number
  }>
}

export interface VMModelBest {
  result: {
    vmId: string
    roundNumber: number
    value: number
  }
}

export interface DataStatistics {
  readonly totalDatasets: number
  readonly totalSize: number
  readonly datasetsByType: Record<string, number>
  readonly datasetsByStatus: Record<string, number>
  readonly uploadsThisMonth: number
}

export interface LogStatistics {
  readonly totalLogs: number
  readonly levelDistribution: Record<string, number>
  readonly categoryDistribution: Record<string, number>
  readonly timeDistribution?: Array<{
    readonly time: string
    readonly count: number
  }>
} 