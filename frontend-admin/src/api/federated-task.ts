import { createApiInstance } from './base'
import type { 
  ApiResponse, 
  PaginatedResponse,
  PaginationParams
} from '@/types'

// 创建联邦学习任务API实例
const federatedTaskApiInstance = createApiInstance('http://localhost:8080/api/federated')

// 联邦学习任务基础类型
interface FederatedTask {
  taskId: string
  taskName: string
  taskType: 'CLASSIFICATION' | 'REGRESSION' | 'CLUSTERING' | 'ANOMALY_DETECTION'
  status: 'CREATED' | 'CONFIGURED' | 'RUNNING' | 'PAUSED' | 'STOPPED' | 'COMPLETED' | 'FAILED' | 'CANCELLED'
  createdAt: string
  startedAt?: string
  completedAt?: string
  participantCount: number
  currentRound?: number
  totalRounds?: number
  progress?: number
  finalAccuracy?: number
}

// 任务详情类型
interface FederatedTaskDetails extends FederatedTask {
  algorithm: string
  participants: Array<{
    vmId: string
    role: string
    status: string
    lastHeartbeat?: string
    currentEpoch?: number
    loss?: number
    accuracy?: number
    dataSource?: string
  }>
  metrics?: {
    globalLoss: number
    globalAccuracy: number
    communicationRounds: number
    dataProcessed: number
    estimatedTimeRemaining: number
  }
}

// 任务结果类型
interface TaskResults {
  taskId: string
  taskName: string
  status: string
  finalResults: {
    accuracy: number
    loss: number
    precision: number
    recall: number
    f1Score: number
    confusionMatrix: number[][]
  }
  roundResults: Array<{
    round: number
    accuracy: number
    loss: number
    participants: string[]
  }>
  participantResults: Array<{
    vmId: string
    finalAccuracy: number
    finalLoss: number
    trainingTime: number
    dataSize: number
    parameters: {
      artifact: {
        format: string
        checksum: string
      }
    }
  }>
  modelInfo: {
    parameters: {
      artifact: {
        format: string
        checksum: string
      }
      meta: {
        version: string
        modelType: string
      }
    }
  }
}

// 任务日志类型
interface TaskLog {
  timestamp: string
  level: string
  message: string
  source: string
  details?: Record<string, unknown>
}

// ==================== 联邦学习任务管理API ====================
export const federatedTask = {
  // ==================== 3.1 任务创建接口 ====================
  async createTask(taskData: {
    taskName: string
    taskType: 'CLASSIFICATION' | 'REGRESSION' | 'CLUSTERING' | 'ANOMALY_DETECTION'
    description?: string
    algorithm: string
    participants: Array<{
      vmId: string
      role: string
      dataSource: string
    }>
    hyperparameters: {
      learningRate: number
      batchSize: number
      epochs: number
      rounds: number
      minParticipants: number
    }
    modelConfig: {
      modelType: string
      featureColumns: string[]
      targetColumn: string
      testSize: number
      randomState: number
    }
    schedule?: {
      startTime?: string
      endTime?: string
      timeout?: number
    }
  }): Promise<{
    taskId: string
    taskName: string
    status: string
    createdAt: string
    createdBy: string
    participantCount: number
    estimatedDuration: number
  }> {
    const response = await federatedTaskApiInstance.post<ApiResponse<{
      taskId: string
      taskName: string
      status: string
      createdAt: string
      createdBy: string
      participantCount: number
      estimatedDuration: number
    }>>('/tasks', taskData)
    return response.data.data
  },

  // ==================== 3.2 任务配置接口 ====================
  async configTask(taskId: string, config: {
    algorithm?: string
    hyperparameters?: {
      learningRate?: number
      batchSize?: number
      epochs?: number
      rounds?: number
      minParticipants?: number
      aggregationMethod?: string
    }
    modelConfig?: {
      modelType?: string
      nEstimators?: number
      maxDepth?: number
      minSamplesSplit?: number
      minSamplesLeaf?: number
    }
    dataConfig?: {
      preprocessing?: {
        normalization?: string
        featureSelection?: string
        outlierRemoval?: boolean
      }
      validation?: {
        crossValidation?: string
        kFolds?: number
        stratified?: boolean
      }
    }
    securityConfig?: {
      encryption?: string
      differentialPrivacy?: {
        enabled?: boolean
        epsilon?: number
        delta?: number
      }
      secureAggregation?: boolean
    }
  }): Promise<{
    taskId: string
    status: string
    updatedAt: string
    configVersion: string
  }> {
    const response = await federatedTaskApiInstance.put<ApiResponse<{
      taskId: string
      status: string
      updatedAt: string
      configVersion: string
    }>>(`/tasks/${taskId}/config`, config)
    return response.data.data
  },

  // ==================== 3.3 任务启动接口 ====================
  async startTask(taskId: string): Promise<{
    taskId: string
    status: string
    startedAt: string
    currentRound: number
    participants: Array<{
      vmId: string
      status: string
      dataSource: string
    }>
  }> {
    const response = await federatedTaskApiInstance.post<ApiResponse<{
      taskId: string
      status: string
      startedAt: string
      currentRound: number
      participants: Array<{
        vmId: string
        status: string
        dataSource: string
      }>
    }>>(`/tasks/${taskId}/start`)
    return response.data.data
  },

  // ==================== 3.4 任务暂停接口 ====================
  async pauseTask(taskId: string): Promise<{
    taskId: string
    status: string
    pausedAt: string
    currentRound: number
    resumePoint: {
      round: number
      step: string
    }
  }> {
    const response = await federatedTaskApiInstance.post<ApiResponse<{
      taskId: string
      status: string
      pausedAt: string
      currentRound: number
      resumePoint: {
        round: number
        step: string
      }
    }>>(`/tasks/${taskId}/pause`)
    return response.data.data
  },

  // ==================== 3.5 任务恢复接口 ====================
  async resumeTask(taskId: string): Promise<{
    taskId: string
    status: string
    resumedAt: string
    currentRound: number
  }> {
    const response = await federatedTaskApiInstance.post<ApiResponse<{
      taskId: string
      status: string
      resumedAt: string
      currentRound: number
    }>>(`/tasks/${taskId}/resume`)
    return response.data.data
  },

  // ==================== 3.6 任务停止接口 ====================
  async stopTask(taskId: string, stopData?: {
    reason?: string
    saveCheckpoint?: boolean
  }): Promise<{
    taskId: string
    status: string
    stoppedAt: string
    finalRound: number
    checkpointSaved: boolean
    checkpointPath?: string
  }> {
    const response = await federatedTaskApiInstance.post<ApiResponse<{
      taskId: string
      status: string
      stoppedAt: string
      finalRound: number
      checkpointSaved: boolean
      checkpointPath?: string
    }>>(`/tasks/${taskId}/stop`, stopData)
    return response.data.data
  },

  // ==================== 3.7 任务取消接口 ====================
  async cancelTask(taskId: string, cancelData?: {
    reason?: string
  }): Promise<{
    taskId: string
    status: string
    cancelledAt: string
    reason?: string
  }> {
    const response = await federatedTaskApiInstance.post<ApiResponse<{
      taskId: string
      status: string
      cancelledAt: string
      reason?: string
    }>>(`/tasks/${taskId}/cancel`, cancelData)
    return response.data.data
  },

  // ==================== 3.8 任务状态查询接口 ====================
  async getTaskDetail(taskId: string): Promise<FederatedTaskDetails> {
    const response = await federatedTaskApiInstance.get<ApiResponse<FederatedTaskDetails>>(`/tasks/${taskId}`)
    return response.data.data
  },

  // ==================== 3.9 任务列表查询接口 ====================
  async getTaskList(params: PaginationParams & {
    status?: string
    type?: string
    startDate?: string
    endDate?: string
    keyword?: string
  } = {}): Promise<{
    total: number
    page: number
    size: number
    tasks: FederatedTask[]
  }> {
    const response = await federatedTaskApiInstance.get<ApiResponse<{
      total: number
      page: number
      size: number
      tasks: FederatedTask[]
    }>>('/tasks', { params })
    return response.data.data
  },

  // ==================== 3.10 任务结果查询接口 ====================
  async getTaskResults(taskId: string): Promise<TaskResults> {
    const response = await federatedTaskApiInstance.get<ApiResponse<TaskResults>>(`/tasks/${taskId}/results`)
    return response.data.data
  },

  // ==================== 3.11 任务日志查询接口 ====================
  async getTaskLogs(taskId: string, params: {
    level?: string
    startTime?: string
    endTime?: string
    keyword?: string
    page?: number
    size?: number
  } = {}): Promise<{
    taskId: string
    total: number
    page: number
    size: number
    logs: TaskLog[]
  }> {
    const response = await federatedTaskApiInstance.get<ApiResponse<{
      taskId: string
      total: number
      page: number
      size: number
      logs: TaskLog[]
    }>>(`/tasks/${taskId}/logs`, { params })
    return response.data.data
  },

  // ==================== 3.12 任务删除接口 ====================
  async deleteTask(taskId: string, deleteOptions?: {
    deleteData?: boolean
    deleteModel?: boolean
  }): Promise<{
    taskId: string
    deletedAt: string
    dataDeleted: boolean
    modelPreserved: boolean
  }> {
    const config = deleteOptions ? { data: deleteOptions } : undefined
    const response = await federatedTaskApiInstance.delete<ApiResponse<{
      taskId: string
      deletedAt: string
      dataDeleted: boolean
      modelPreserved: boolean
    }>>(`/tasks/${taskId}`, config)
    return response.data.data
  },
} as const

// 使用命名导出以保持一致性 

// 导出类型定义
export type {
  FederatedTask,
  FederatedTaskDetails,
  TaskResults,
  TaskLog
}