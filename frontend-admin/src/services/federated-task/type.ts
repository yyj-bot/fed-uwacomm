/**
 * 联邦学习任务服务类型定义
 * 定义联邦学习任务管理相关的请求和响应类型
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import type { PaginationParams, FederatedTask, FederatedTaskDetails, TaskResults, DeepReadonly, PartialBy, RequiredBy } from '@/types'

// ==================== 创建任务相关类型 ====================

/**
 * 创建任务请求 - v1.3 增强版
 */
export interface CreateTaskRequest {
  /** 任务名称 */
  readonly taskName: string
  /** 任务类型 */
  readonly taskType: 'CLASSIFICATION' | 'REGRESSION' | 'CLUSTERING' | 'ANOMALY_DETECTION'
  /** 任务描述 */
  readonly description?: string
  /** 算法名称 */
  readonly algorithm: string
  
  // 🆕 v1.3 新增：智能数据集配置
  readonly datasetConfig?: {
    readonly datasetId: string
    readonly distributionStrategy: 'BALANCED' | 'RANDOM' | 'CUSTOM'
    readonly distributionRatios: Record<string, number>
    readonly validationSplit: number
    readonly testSplit: number
  }
  
  // 🆕 v1.3 新增：智能参与者配置
  readonly participantConfig?: {
    readonly selectionMode: 'MANUAL' | 'AUTOMATIC'
    readonly requirements?: {
      readonly minParticipants: number
      readonly maxParticipants: number
      readonly minCpuCores: number
      readonly minMemoryMb: number
    }
    readonly participants: Array<{
      readonly vmId: string
      readonly role: 'PARTICIPANT'
      readonly dataRatio: number
      readonly capabilities?: string[]
      readonly constraints?: {
        readonly maxCpuUsage?: number
        readonly maxMemoryUsage?: number
      }
    }>
  }
  
  // 兼容旧格式 - 将在 v2.0 中删除
  /** @deprecated 使用 participantConfig 代替 */
  readonly participants?: Array<{
    readonly vmId: string
    readonly role: 'PARTICIPANT'
    readonly dataSource: string
  }>
  
  /** 超参数配置 */
  readonly hyperparameters: {
    readonly learningRate: number
    readonly batchSize: number
    readonly epochs: number
    readonly rounds: number
    readonly minParticipants: number
  }
  /** 模型配置 */
  readonly modelConfig: {
    readonly modelType: string
    readonly featureColumns: string[]
    readonly targetColumn: string
    readonly testSize: number
    readonly randomState: number
  }
  /** 调度配置 */
  readonly schedule?: {
    readonly startTime?: string
    readonly endTime?: string
    readonly timeout?: number
  }
}

/**
 * 创建任务响应 - v1.3 增强版
 */
export interface CreateTaskResponse {
  /** 任务ID */
  readonly taskId: string
  /** 任务名称 */
  readonly taskName: string
  /** 任务状态 */
  readonly status: string
  /** 创建时间 */
  readonly createdAt: string
  /** 创建者ID */
  readonly createdBy: string
  /** 参与者数量 */
  readonly participantCount: number
  /** 预估执行时间（秒） */
  readonly estimatedDuration: number
  
  // 🆕 v1.3 新增：配置摘要
  readonly configSummary?: {
    readonly dataset?: {
      readonly datasetId: string
      readonly totalRows: number
      readonly distributionStrategy: string
    }
    readonly participants: Array<{
      readonly vmId: string
      readonly vmName: string
      readonly role: string
      readonly dataRatio: number
    }>
  }
  
  // 🆕 v1.3 新增：预计性能指标
  readonly performanceEstimation?: {
    readonly expectedAccuracy: number
    readonly convergenceRounds: number
    readonly networkTraffic: string
  }
}

// ==================== 配置任务相关类型 ====================

/**
 * 配置任务请求
 */
export interface ConfigTaskRequest {
  /** 算法名称 */
  readonly algorithm?: string
  /** 超参数配置 */
  readonly hyperparameters?: {
    readonly learningRate?: number
    readonly batchSize?: number
    readonly epochs?: number
    readonly rounds?: number
    readonly minParticipants?: number
    readonly aggregationMethod?: string
  }
  /** 模型配置 */
  readonly modelConfig?: {
    readonly modelType?: string
    readonly nEstimators?: number
    readonly maxDepth?: number
    readonly minSamplesSplit?: number
    readonly minSamplesLeaf?: number
  }
  /** 数据配置 */
  readonly dataConfig?: {
    readonly preprocessing?: {
      readonly normalization?: string
      readonly featureSelection?: string
      readonly outlierRemoval?: boolean
    }
    readonly validation?: {
      readonly crossValidation?: string
      readonly kFolds?: number
      readonly stratified?: boolean
    }
  }
  /** 安全配置 */
  readonly securityConfig?: {
    readonly encryption?: string
    readonly differentialPrivacy?: {
      readonly enabled?: boolean
      readonly epsilon?: number
      readonly delta?: number
    }
    readonly secureAggregation?: boolean
  }
}

/**
 * 配置任务响应
 */
export interface ConfigTaskResponse {
  /** 任务ID */
  readonly taskId: string
  /** 任务状态 */
  readonly status: string
  /** 更新时间 */
  readonly updatedAt: string
  /** 配置版本 */
  readonly configVersion: string
}

// ==================== 任务控制相关类型 ====================

/**
 * 启动任务响应
 */
export interface StartTaskResponse {
  /** 任务ID */
  readonly taskId: string
  /** 任务状态 */
  readonly status: FederatedTask['status']
  /** 启动时间 */
  readonly startedAt: string
  /** 当前轮次 */
  readonly currentRound: number
  /** 参与者列表 */
  readonly participants: Array<{
    readonly vmId: string
    readonly status: string
    readonly dataSource: string
  }>
}

/**
 * 暂停任务响应
 */
export interface PauseTaskResponse {
  /** 任务ID */
  readonly taskId: string
  /** 任务状态 */
  readonly status: FederatedTask['status']
  /** 暂停时间 */
  readonly pausedAt: string
  /** 当前轮次 */
  readonly currentRound: number
  /** 恢复点信息 */
  readonly resumePoint: {
    readonly round: number
    readonly step: string
  }
}

/**
 * 恢复任务响应
 */
export interface ResumeTaskResponse {
  /** 任务ID */
  readonly taskId: string
  /** 任务状态 */
  readonly status: FederatedTask['status']
  /** 恢复时间 */
  readonly resumedAt: string
  /** 当前轮次 */
  readonly currentRound: number
}

/**
 * 停止任务请求
 */
export interface StopTaskRequest {
  /** 停止原因 */
  readonly reason?: string
  /** 是否保存检查点 */
  readonly saveCheckpoint?: boolean
}

/**
 * 停止任务响应
 */
export interface StopTaskResponse {
  /** 任务ID */
  readonly taskId: string
  /** 任务状态 */
  readonly status: FederatedTask['status']
  /** 停止时间 */
  readonly stoppedAt: string
  /** 最终轮次 */
  readonly finalRound: number
  /** 是否保存了检查点 */
  readonly checkpointSaved: boolean
  /** 检查点路径 */
  readonly checkpointPath?: string
}

/**
 * 取消任务请求
 */
export interface CancelTaskRequest {
  /** 取消原因 */
  readonly reason?: string
}

/**
 * 取消任务响应
 */
export interface CancelTaskResponse {
  /** 任务ID */
  readonly taskId: string
  /** 任务状态 */
  readonly status: FederatedTask['status']
  /** 取消时间 */
  readonly cancelledAt: string
  /** 取消原因 */
  readonly reason?: string
}

// ==================== 查询相关类型 ====================

/**
 * 任务列表查询参数
 */
export interface TaskListParams extends PaginationParams {
  /** 状态过滤 */
  readonly status?: FederatedTask['status']
  /** 类型过滤 */
  readonly type?: FederatedTask['taskType']
  /** 开始日期 */
  readonly startDate?: string
  /** 结束日期 */
  readonly endDate?: string
  /** 关键词搜索 */
  readonly keyword?: string
}

/**
 * 任务列表响应
 */
export interface TaskListResponse {
  /** 总数 */
  readonly total: number
  /** 当前页 */
  readonly page: number
  /** 每页大小 */
  readonly size: number
  /** 任务列表 */
  readonly tasks: FederatedTask[]
}

/**
 * 任务日志查询参数
 */
export interface TaskLogsParams {
  /** 日志级别 */
  readonly level?: string
  /** 开始时间 */
  readonly startTime?: string
  /** 结束时间 */
  readonly endTime?: string
  /** 关键词搜索 */
  readonly keyword?: string
  /** 页码 */
  readonly page?: number
  /** 每页大小 */
  readonly size?: number
}

/**
 * 任务日志响应
 */
export interface TaskLogsResponse {
  /** 任务ID */
  readonly taskId: string
  /** 总数 */
  readonly total: number
  /** 当前页 */
  readonly page: number
  /** 每页大小 */
  readonly size: number
  /** 日志列表 */
  readonly logs: Array<{
    readonly timestamp: string
    readonly level: string
    readonly message: string
    readonly source: string
    readonly details?: Record<string, unknown>
  }>
}

// ==================== 删除相关类型 ====================

/**
 * 删除任务请求
 */
export interface DeleteTaskRequest {
  /** 是否删除数据 */
  readonly deleteData?: boolean
  /** 是否删除模型 */
  readonly deleteModel?: boolean
}

/**
 * 删除任务响应
 */
export interface DeleteTaskResponse {
  /** 任务ID */
  readonly taskId: string
  /** 删除时间 */
  readonly deletedAt: string
  /** 数据是否已删除 */
  readonly dataDeleted: boolean
  /** 模型是否保留 */
  readonly modelPreserved: boolean
}

// ==================== 业务逻辑类型 ====================

/**
 * 任务操作类型
 */
export type TaskOperation = 
  | 'CREATE'
  | 'CONFIG'
  | 'START'
  | 'PAUSE'
  | 'RESUME'
  | 'STOP'
  | 'CANCEL'
  | 'DELETE'
  | 'QUERY'
  | 'RESULTS'
  | 'LOGS'

/**
 * 任务操作日志
 */
export interface TaskOperationLog {
  /** 操作ID */
  readonly operationId: string
  /** 操作类型 */
  readonly operation: TaskOperation
  /** 任务ID */
  readonly taskId: string
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

// ==================== 监控相关类型 ====================

/**
 * 任务监控指标
 */
export interface TaskMetrics {
  /** 全局损失 */
  readonly globalLoss: number
  /** 全局准确率 */
  readonly globalAccuracy: number
  /** 通信轮次 */
  readonly communicationRounds: number
  /** 处理的数据量 */
  readonly dataProcessed: number
  /** 预计剩余时间（秒） */
  readonly estimatedTimeRemaining: number
}

/**
 * 参与者状态
 */
export interface ParticipantStatus {
  /** 虚拟机ID */
  readonly vmId: string
  /** 参与者角色 */
  readonly role: string
  /** 参与者状态 */
  readonly status: string
  /** 最后心跳时间 */
  readonly lastHeartbeat?: string
  /** 当前训练轮次 */
  readonly currentEpoch?: number
  /** 当前损失 */
  readonly loss?: number
  /** 当前准确率 */
  readonly accuracy?: number
  /** 数据源 */
  readonly dataSource?: string
}

/**
 * 任务进度信息
 */
export interface TaskProgress {
  /** 任务ID */
  readonly taskId: string
  /** 当前轮次 */
  readonly currentRound: number
  /** 总轮次 */
  readonly totalRounds: number
  /** 进度百分比 */
  readonly progress: number
  /** 开始时间 */
  readonly startedAt: string
  /** 预计完成时间 */
  readonly estimatedCompletionTime?: string
  /** 参与者状态列表 */
  readonly participants: ParticipantStatus[]
  /** 监控指标 */
  readonly metrics?: TaskMetrics
}

// ==================== 统计类型 ====================

/**
 * 任务统计信息
 */
export interface TaskStatistics {
  /** 总任务数 */
  readonly totalTasks: number
  /** 运行中的任务数 */
  readonly runningTasks: number
  /** 已完成的任务数 */
  readonly completedTasks: number
  /** 失败的任务数 */
  readonly failedTasks: number
  /** 按状态分布 */
  readonly statusDistribution: Record<FederatedTask['status'], number>
  /** 按类型分布 */
  readonly typeDistribution: Record<FederatedTask['taskType'], number>
  /** 平均执行时间（秒） */
  readonly averageExecutionTime: number
  /** 平均参与者数量 */
  readonly averageParticipantCount: number
  /** 总数据处理量 */
  readonly totalDataProcessed: number
  /** 平均准确率 */
  readonly averageAccuracy: number
}

// ==================== 错误类型 ====================

/**
 * 联邦学习任务服务错误类型 - v1.3 增强版
 */
export interface FederatedTaskServiceError {
  /** 错误码 */
  readonly code: string
  /** 错误消息 */
  readonly message: string
  /** 详细错误信息 */
  readonly details?: unknown
  /** 原始错误 */
  readonly originalError?: Error
  
  // 🆕 v1.3 新增：废弃警告信息
  readonly deprecationWarning?: {
    readonly deprecationVersion: string
    readonly removalVersion: string
    readonly migrationGuide: string
  }
}

/**
 * 任务操作错误类型 - v1.3 增强版
 */
export interface TaskOperationError extends FederatedTaskServiceError {
  /** 任务ID */
  readonly taskId: string
  /** 操作类型 */
  readonly operation: TaskOperation
  /** 失败原因 */
  readonly failureReason: 
    | 'INVALID_PARAMETER'
    | 'TASK_NOT_FOUND'
    | 'TASK_ALREADY_EXISTS'
    | 'TASK_INVALID_STATUS'
    | 'TASK_CONFIG_ERROR'
    | 'DATASET_CONFIG_ERROR'  // 🆕 v1.3 新增
    | 'INSUFFICIENT_PARTICIPANTS'
    | 'PARTICIPANT_OFFLINE'
    | 'MODEL_ERROR'
    | 'DATA_ERROR'
    | 'SECURITY_ERROR'
    | 'UNKNOWN'
}

/**
 * 验证错误类型
 */
export interface TaskValidationError extends FederatedTaskServiceError {
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
 * 联邦学习任务服务配置
 */
export interface FederatedTaskServiceConfig {
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
  /** 任务状态轮询间隔（毫秒） */
  readonly statusPollingInterval: number
  /** 最大并发任务数 */
  readonly maxConcurrentTasks: number
}

/**
 * 联邦学习默认配置
 */
export interface FederatedLearningDefaults {
  /** 默认学习率 */
  readonly defaultLearningRate: number
  /** 默认批次大小 */
  readonly defaultBatchSize: number
  /** 默认训练轮次 */
  readonly defaultEpochs: number
  /** 默认联邦轮次 */
  readonly defaultRounds: number
  /** 默认最小参与者数量 */
  readonly defaultMinParticipants: number
  /** 默认测试集比例 */
  readonly defaultTestSize: number
  /** 支持的任务类型 */
  readonly supportedTaskTypes: FederatedTask['taskType'][]
  /** 支持的算法类型 */
  readonly supportedAlgorithms: string[]
  /** 支持的模型类型 */
  readonly supportedModelTypes: string[]
}

/**
 * 联邦学习任务状态联合类型
 */
export type TaskStatus = FederatedTask['status']

/**
 * 联邦学习任务类型联合类型
 */
export type TaskType = FederatedTask['taskType']

// ==================== v1.4 新增：联邦学习流程编排类型 ====================

/**
 * 启动编排请求
 */
export interface StartOrchestrationRequest {
  /** 关联的任务ID */
  readonly taskId: string
  /** 工作流配置 */
  readonly workflowConfig: {
    readonly autoStart: boolean
    readonly stages: {
      readonly initialModelGeneration: {
        readonly enabled: boolean
        readonly strategy: 'RANDOM_GENERATION' | 'PRETRAINED_MODEL' | 'CUSTOM'
        readonly parameters: {
          readonly modelType: string
          readonly architecture: {
            readonly inputSize: number
            readonly hiddenLayers: number[]
            readonly outputSize: number
          }
        }
      }
      readonly dataDistribution: {
        readonly enabled: boolean
        readonly strategy: 'BALANCED' | 'RANDOM' | 'CUSTOM'
        readonly verificationLevel: 'NONE' | 'BASIC' | 'FULL'
      }
      readonly modelDistribution: {
        readonly enabled: boolean
        readonly timeout: number
        readonly retryAttempts: number
      }
      readonly federatedTraining: {
        readonly maxRounds: number
        readonly convergenceThreshold: number
        readonly participantThreshold: number
        readonly earlyStoppingPatience?: number
        readonly dynamicParticipantSelection?: boolean
      }
      readonly modelAggregation: {
        readonly method: 'FEDAVG' | 'FEDPROX' | 'CUSTOM'
        readonly weightingStrategy: 'EQUAL' | 'DATA_SIZE' | 'ACCURACY'
        readonly qualityGating: {
          readonly enabled: boolean
          readonly minAccuracy?: number
          readonly maxLoss?: number
        }
      }
      readonly evaluation: {
        readonly enabled: boolean
        readonly metrics: string[]
        readonly testDataRatio: number
      }
    }
    readonly errorHandling: {
      readonly maxRetries: number
      readonly retryDelay: number
      readonly failureThreshold: number
    }
    readonly monitoring: {
      readonly enableRealTimeMetrics: boolean
      readonly metricsCollectionInterval: number
      readonly performanceAlerts: boolean
    }
  }
  /** 调度选项 */
  readonly schedulingOptions?: {
    readonly priority: 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'
    readonly maxExecutionTime: number
    readonly resourceLimits: {
      readonly maxCpuCores: number
      readonly maxMemoryMb: number
      readonly maxNetworkBandwidth: number
    }
    readonly scheduling: {
      readonly startTime?: string
      readonly timezone?: string
      readonly dependencies?: string[]
    }
  }
}

/**
 * 启动编排响应
 */
export interface StartOrchestrationResponse {
  /** 编排任务ID */
  readonly orchestrationId: string
  /** 关联的任务ID */
  readonly taskId: string
  /** 当前状态 */
  readonly status: string
  /** 启动时间 */
  readonly startedAt: string
  /** 预计完成时间 */
  readonly estimatedCompletion: string
  /** 当前阶段 */
  readonly currentStage: string
  /** 工作流计划 */
  readonly workflowPlan: {
    readonly totalStages: number
    readonly estimatedDuration: string
    readonly stages: Array<{
      readonly name: string
      readonly status: string
      readonly estimatedDuration: string
    }>
  }
  /** 资源分配 */
  readonly resourceAllocation: {
    readonly allocatedMemory: string
    readonly allocatedCpuCores: number
    readonly allocatedBandwidth: string
    readonly participatingVms: string[]
  }
}

/**
 * 编排状态查询请求参数
 */
export interface OrchestrationStatusParams {
  /** 是否包含详细信息 */
  readonly includeDetails?: boolean
  /** 是否包含指标数据 */
  readonly includeMetrics?: boolean
  /** 是否强制刷新 */
  readonly refresh?: boolean
}

/**
 * 暂停编排请求
 */
export interface PauseOrchestrationRequest {
  /** 暂停原因 */
  readonly reason?: string
  /** 暂停模式 */
  readonly pauseMode?: 'GRACEFUL' | 'IMMEDIATE'
  /** 是否等待当前轮次完成 */
  readonly waitForCurrentRound?: boolean
  /** 是否保留状态 */
  readonly preserveState?: boolean
  /** 是否通知参与者 */
  readonly notifyParticipants?: boolean
}

/**
 * 暂停编排响应
 */
export interface PauseOrchestrationResponse {
  /** 编排任务ID */
  readonly orchestrationId: string
  /** 当前状态 */
  readonly status: string
  /** 暂停时间 */
  readonly pausedAt: string
  /** 暂停阶段 */
  readonly pausedStage: string
  /** 暂停轮次 */
  readonly pausedRound?: number
  /** 暂停原因 */
  readonly reason?: string
  /** 是否可以恢复 */
  readonly canResume: boolean
  /** 状态快照 */
  readonly stateSnapshot: {
    readonly snapshotId: string
    readonly createdAt: string
    readonly modelVersions: Record<string, string>
    readonly trainingProgress: number
    readonly participantStates: Record<string, any>
  }
}

/**
 * 恢复编排请求
 */
export interface ResumeOrchestrationRequest {
  /** 是否从快照恢复 */
  readonly resumeFromSnapshot?: boolean
  /** 快照ID */
  readonly snapshotId?: string
  /** 是否验证状态 */
  readonly validateState?: boolean
  /** 是否通知参与者 */
  readonly notifyParticipants?: boolean
}

/**
 * 恢复编排响应
 */
export interface ResumeOrchestrationResponse {
  /** 编排任务ID */
  readonly orchestrationId: string
  /** 当前状态 */
  readonly status: string
  /** 恢复时间 */
  readonly resumedAt: string
  /** 恢复阶段 */
  readonly resumedStage: string
  /** 恢复轮次 */
  readonly resumedRound?: number
  /** 状态验证结果 */
  readonly stateValidation: {
    readonly passed: boolean
    readonly modelsVerified: number
    readonly stateConsistent: boolean
  }
  /** 预计剩余时间 */
  readonly estimatedRemainingTime: string
}

/**
 * 终止编排请求参数
 */
export interface TerminateOrchestrationParams {
  /** 是否强制终止 */
  readonly force?: boolean
  /** 是否清理资源 */
  readonly cleanup?: boolean
  /** 是否保存结果 */
  readonly saveResults?: boolean
}

/**
 * 终止编排响应
 */
export interface TerminateOrchestrationResponse {
  /** 编排任务ID */
  readonly orchestrationId: string
  /** 当前状态 */
  readonly status: string
  /** 终止时间 */
  readonly terminatedAt: string
  /** 终止阶段 */
  readonly terminatedStage: string
  /** 终止轮次 */
  readonly terminatedRound?: number
  /** 已完成轮次 */
  readonly completedRounds: number
  /** 部分结果 */
  readonly partialResults: {
    readonly bestModel: {
      readonly roundNumber: number
      readonly accuracy: number
      readonly modelId: string
    }
    readonly savedModels: number
    readonly trainingMetrics: string
  }
  /** 清理状态 */
  readonly cleanup: {
    readonly resourcesReleased: boolean
    readonly temporaryDataCleared: boolean
    readonly participantsNotified: boolean
  }
}

/**
 * 编排时间线查询参数
 */
export interface OrchestrationTimelineParams {
  /** 是否包含事件 */
  readonly includeEvents?: boolean
  /** 事件级别 */
  readonly eventLevel?: 'ALL' | 'MAJOR' | 'ERROR'
  /** 时间范围 */
  readonly timeRange?: string
}

/**
 * 编排列表查询参数
 */
export interface OrchestrationListParams extends PaginationParams {
  /** 关联的任务ID */
  readonly taskId?: string
  /** 编排状态 */
  readonly status?: string
  /** 排序字段 */
  readonly sortBy?: string
  /** 排序方向 */
  readonly sortOrder?: 'asc' | 'desc'
}

/**
 * 编排列表响应
 */
export interface OrchestrationListResponse {
  /** 总数 */
  readonly total: number
  /** 当前页 */
  readonly page: number
  /** 每页大小 */
  readonly size: number
  /** 编排列表 */
  readonly items: Array<{
    readonly orchestrationId: string
    readonly taskId: string
    readonly status: string
    readonly startedAt: string
    readonly completedAt?: string
    readonly currentStage: string
    readonly progress: number
    readonly duration: string
    readonly participatingVms: number
    readonly completedRounds: number
    readonly totalRounds: number
    readonly finalAccuracy?: number
    readonly success?: boolean
  }>
}

/**
 * 编排性能分析查询参数
 */
export interface OrchestrationAnalyticsParams {
  /** 是否包含建议 */
  readonly includeRecommendations?: boolean
  /** 指标级别 */
  readonly metricsLevel?: 'BASIC' | 'DETAILED' | 'FULL'
}

// ==================== 重新导出基础类型 ====================
// 注：工具类型已从 @/types 统一导入，不再重复定义
