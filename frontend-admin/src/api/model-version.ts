import { createApiInstance } from './base'
import type { 
  ApiResponse, 
  ModelVersion,
  PaginationParams,
  SortParams
} from '@/types'

// 模型版本专用分页响应类型（符合 model-version-api-reference.md）
interface ModelVersionPaginatedResponse<T> {
  readonly total: number
  readonly current: number  // 使用 current 而不是 page
  readonly size: number
  readonly pages: number
  readonly records: T[]
}

// 创建模型API实例
const modelApiInstance = createApiInstance('MODEL')


// 评估结果类型
interface EvaluationResult {
  evaluationId: string
  modelId: string
  taskId: string
  metrics: Record<string, number>
  evaluationTime: number
  testSamples: number
  status: string
  createdAt: string
}


// 统计信息类型
interface ModelStatistics {
  totalModels: number
  averageAccuracy: number
  averageLoss: number
  uploadTrend: Array<{ date: string; count: number }>
  accuracyTrend: Array<{ roundNumber: number; accuracy: number }>
}

// ==================== 初始模型管理API ====================
export const initialModel = {
  // ==================== 2.1 随机生成初始模型 ====================
  
  async generateInitialModel(generationData: {
    taskId: string
    modelType: string
    architecture: {
      inputSize: number
      hiddenLayers: number[]
      outputSize: number
      activationFunction: string
      optimizer: string
      learningRate: number
    }
    randomSeed?: number
    description?: string
  }): Promise<{
    modelId: string
    taskId: string
    modelType: string
    modelSize: number
    parametersCount: number
    architecture: {
      inputSize: number
      hiddenLayers: number[]
      outputSize: number
      activationFunction: string
      optimizer: string
      learningRate: number
    }
    generatedAt: string
    status: string
    checksum: string
  }> {
    const response = await modelApiInstance.post<ApiResponse<{
      modelId: string
      taskId: string
      modelType: string
      modelSize: number
      parametersCount: number
      architecture: {
        inputSize: number
        hiddenLayers: number[]
        outputSize: number
        activationFunction: string
        optimizer: string
        learningRate: number
      }
      generatedAt: string
      status: string
      checksum: string
    }>>('/initial/generate', generationData)
    return response.data.data
  },

  // ==================== 2.2 上传自定义初始模型 ====================
  
  async uploadCustomInitialModel(formData: FormData): Promise<{
    modelId: string
    taskId: string
    modelType: string
    fileName: string
    modelSize: number
    uploadedAt: string
    status: string
    checksum: string
    metadata?: {
      architecture?: {
        inputSize: number
        outputSize: number
      }
      framework?: string
      version?: string
    }
  }> {
    const response = await modelApiInstance.post<ApiResponse<{
      modelId: string
      taskId: string
      modelType: string
      fileName: string
      modelSize: number
      uploadedAt: string
      status: string
      checksum: string
      metadata?: {
        architecture?: {
          inputSize: number
          outputSize: number
        }
        framework?: string
        version?: string
      }
    }>>('/initial/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    return response.data.data
  },

  // ==================== 2.3 获取任务初始模型 ====================
  
  async getTaskInitialModel(taskId: string, params: {
    includeParameters?: boolean
    format?: 'json' | 'binary'
  } = {}): Promise<{
    modelId: string
    taskId: string
    modelType: string
    modelSize: number
    createdAt: string
    status: string
    architecture: {
      inputSize: number
      hiddenLayers: number[]
      outputSize: number
      activationFunction: string
      optimizer: string
      learningRate: number
    }
    distributionStatus: {
      totalVms: number
      distributedVms: number
      failedVms: number
      distributedAt?: string
    }
    checksum: string
  }> {
    const response = await modelApiInstance.get<ApiResponse<{
      modelId: string
      taskId: string
      modelType: string
      modelSize: number
      createdAt: string
      status: string
      architecture: {
        inputSize: number
        hiddenLayers: number[]
        outputSize: number
        activationFunction: string
        optimizer: string
        learningRate: number
      }
      distributionStatus: {
        totalVms: number
        distributedVms: number
        failedVms: number
        distributedAt?: string
      }
      checksum: string
    }>>(`/initial/${taskId}`, { params })
    return response.data.data
  },

  // ==================== 2.4 分发初始模型 ====================
  
  async distributeInitialModel(taskId: string, distributionData: {
    vmIds: string[]
    distributionMode: 'ASYNC' | 'SYNC'
    timeout?: number
    retryAttempts?: number
    verifyChecksum?: boolean
    notifyOnCompletion?: boolean
  }): Promise<{
    distributionId: string
    taskId: string
    modelId: string
    targetVms: string[]
    distributionMode: string
    status: string
    startedAt: string
    estimatedCompletion?: string
    progress: {
      total: number
      completed: number
      failed: number
      inProgress: number
    }
  }> {
    const response = await modelApiInstance.post<ApiResponse<{
      distributionId: string
      taskId: string
      modelId: string
      targetVms: string[]
      distributionMode: string
      status: string
      startedAt: string
      estimatedCompletion?: string
      progress: {
        total: number
        completed: number
        failed: number
        inProgress: number
      }
    }>>(`/initial/${taskId}/distribute`, distributionData)
    return response.data.data
  },

  // ==================== 2.5 查询分发状态 ====================
  
  async getDistributionStatus(distributionId: string): Promise<{
    distributionId: string
    taskId: string
    modelId: string
    status: string
    startedAt: string
    completedAt?: string
    progress: {
      total: number
      completed: number
      failed: number
      inProgress: number
    }
    vmDetails: Array<{
      vmId: string
      status: string
      distributedAt?: string
      verificationStatus: string
      checksum?: string
    }>
  }> {
    const response = await modelApiInstance.get<ApiResponse<{
      distributionId: string
      taskId: string
      modelId: string
      status: string
      startedAt: string
      completedAt?: string
      progress: {
        total: number
        completed: number
        failed: number
        inProgress: number
      }
      vmDetails: Array<{
        vmId: string
        status: string
        distributedAt?: string
        verificationStatus: string
        checksum?: string
      }>
    }>>(`/initial/distribution/${distributionId}`)
    return response.data.data
  },

  // ==================== 2.6 下载初始模型 ====================
  
  async downloadInitialModel(taskId: string, params: {
    format?: 'binary' | 'json'
    modelId?: string
  } = {}): Promise<Blob> {
    const response = await modelApiInstance.get(`/initial/${taskId}/download`, { 
      params,
      responseType: 'blob'
    })
    return response.data
  },

  // ==================== 2.7 删除初始模型 ====================
  
  async deleteInitialModel(taskId: string, deleteData: {
    force?: boolean
  } = {}): Promise<{
    taskId: string
    modelId: string
    deletedAt: string
    cleanupStatus: {
      modelFileDeleted: boolean
      distributionRecordsCleared: boolean
      vmCachesCleared: number
    }
  }> {
    const response = await modelApiInstance.delete<ApiResponse<{
      taskId: string
      modelId: string
      deletedAt: string
      cleanupStatus: {
        modelFileDeleted: boolean
        distributionRecordsCleared: boolean
        vmCachesCleared: number
      }
    }>>(`/initial/${taskId}`, { data: deleteData })
    return response.data.data
  }
} as const

// ==================== 全局模型版本管理API ====================
export const model = {

  // ==================== 4. 模型版本查询接口 ====================
  
  // 4.1 模型版本列表查询
  async getModelVersions(params: PaginationParams & SortParams & {
    taskId?: string
    roundNumber?: number
    status?: string
  } = {}): Promise<ModelVersionPaginatedResponse<ModelVersion>> {
    const response = await modelApiInstance.get<ApiResponse<ModelVersionPaginatedResponse<ModelVersion>>>('/versions', { params })
    return response.data.data
  },

  // 4.2 模型版本详情查询
  async getModelVersionDetail(modelId: string): Promise<{
    modelId: string
    taskId: string
    roundNumber: number
    aggregationMethod: string
    clientCount: number
    modelJson: Record<string, unknown>
    metrics: {
      accuracy: number
      loss: number
    }
    createdAt: string
    aggregatedAt: string
    status: string
  }> {
    const response = await modelApiInstance.get<ApiResponse<{
      modelId: string
      taskId: string
      roundNumber: number
      aggregationMethod: string
      clientCount: number
      modelJson: Record<string, unknown>
      metrics: {
        accuracy: number
        loss: number
      }
      createdAt: string
      aggregatedAt: string
      status: string
    }>>(`/versions/${modelId}`)
    return response.data.data
  },

  // 4.3 任务模型版本查询
  async getTaskModelVersions(taskId: string, params: {
    roundNumber?: number
    status?: string
    sort?: string
    order?: 'asc' | 'desc'
  } = {}): Promise<{
    taskId: string
    taskName: string
    totalModels: number
    versions: Array<{
      modelId: string
      roundNumber: number
      accuracy: number
      loss: number
      status: string
      createdAt: string
    }>
  }> {
    const response = await modelApiInstance.get<ApiResponse<{
      taskId: string
      taskName: string
      totalModels: number
      versions: Array<{
        modelId: string
        roundNumber: number
        accuracy: number
        loss: number
        status: string
        createdAt: string
      }>
    }>>(`/versions/task/${taskId}`, { params })
    return response.data.data
  },

  // ==================== 5. 模型性能评估接口 ====================
  
  // 5.1 模型性能评估
  async evaluateModel(evaluationData: {
    modelId: string
    testDataPath: string
    metrics?: string[]
    batchSize?: number
    device?: string
  }): Promise<{
    modelId: string
    evaluationId: string
    metrics: Record<string, number>
    evaluationTime: number
    testSamples: number
    status: string
    createdAt: string
  }> {
    const response = await modelApiInstance.post<ApiResponse<{
      modelId: string
      evaluationId: string
      metrics: Record<string, number>
      evaluationTime: number
      testSamples: number
      status: string
      createdAt: string
    }>>('/evaluate', evaluationData)
    return response.data.data
  },

  // 5.2 批量模型评估
  async evaluateModelBatch(evaluationData: {
    taskId: string
    testDataPath: string
    roundNumbers?: number[]
    metrics?: string[]
    batchSize?: number
  }): Promise<{
    taskId: string
    evaluatedCount: number
    results: Array<{
      modelId: string
      roundNumber: number
      accuracy: number
      loss: number
      status: string
    }>
  }> {
    const response = await modelApiInstance.post<ApiResponse<{
      taskId: string
      evaluatedCount: number
      results: Array<{
        modelId: string
        roundNumber: number
        accuracy: number
        loss: number
        status: string
      }>
    }>>('/evaluate/batch', evaluationData)
    return response.data.data
  },

  // 5.3 评估结果查询
  async getEvaluationResults(params: {
    modelId?: string
    taskId?: string
    evaluationId?: string
    page?: number
    size?: number
  } = {}): Promise<ModelVersionPaginatedResponse<EvaluationResult>> {
    const response = await modelApiInstance.get<ApiResponse<ModelVersionPaginatedResponse<EvaluationResult>>>('/evaluate/results', { params })
    return response.data.data
  },

  // ==================== 7. 模型回滚接口 ====================
  
  // 7.1 模型回滚
  async rollbackModel(rollbackData: {
    deploymentId: string
    targetModelId: string
    rollbackReason?: string
    force?: boolean
  }): Promise<{
    rollbackId: string
    deploymentId: string
    fromModelId: string
    toModelId: string
    status: string
    rollbackReason?: string
    rollbackTime: number
    createdAt: string
  }> {
    const response = await modelApiInstance.post<ApiResponse<{
      rollbackId: string
      deploymentId: string
      fromModelId: string
      toModelId: string
      status: string
      rollbackReason?: string
      rollbackTime: number
      createdAt: string
    }>>('/rollback', rollbackData)
    return response.data.data
  },

  // 7.2 回滚历史查询
  async getRollbackHistory(params: {
    deploymentId?: string
    page?: number
    size?: number
  } = {}): Promise<ModelVersionPaginatedResponse<{
    rollbackId: string
    deploymentId: string
    fromModelId: string
    toModelId: string
    status: string
    rollbackReason?: string
    rollbackTime?: number
    createdAt: string
  }>> {
    const response = await modelApiInstance.get<ApiResponse<ModelVersionPaginatedResponse<{
      rollbackId: string
      deploymentId: string
      fromModelId: string
      toModelId: string
      status: string
      rollbackReason?: string
      rollbackTime?: number
      createdAt: string
    }>>>('/rollback/history', { params })
    return response.data.data
  },

  // ==================== 8. 模型下载接口 ====================
  
  // 8.1 模型文件下载
  async downloadModel(modelId: string, params: {
    format?: 'original' | 'onnx'
    compressed?: boolean
  } = {}): Promise<Blob> {
    const response = await modelApiInstance.get(`/download/${modelId}`, { 
      params,
      responseType: 'blob'
    })
    return response.data
  },

  // 8.2 批量模型下载
  async downloadModelBatch(downloadData: {
    modelIds: string[]
    format?: 'original' | 'onnx'
    compressed?: boolean
  }): Promise<Blob> {
    const response = await modelApiInstance.post('/download/batch', downloadData, {
      responseType: 'blob'
    })
    return response.data
  },

  // ==================== 9. 模型删除接口 ====================
  
  // 9.1 模型版本删除
  async deleteModel(modelId: string, deleteData: {
    force?: boolean
    deleteFile?: boolean
  } = {}): Promise<{
    modelId: string
    deletedAt: string
  }> {
    const response = await modelApiInstance.delete<ApiResponse<{
      modelId: string
      deletedAt: string
    }>>(`/versions/${modelId}`, { data: deleteData })
    return response.data.data
  },

  // 9.2 批量模型删除
  async deleteModelBatch(deleteData: {
    modelIds: string[]
    force?: boolean
    deleteFile?: boolean
  }): Promise<{
    successCount: number
    failedCount: number
    results: Array<{
      modelId: string
      status: string
      message: string
    }>
  }> {
    const response = await modelApiInstance.delete<ApiResponse<{
      successCount: number
      failedCount: number
      results: Array<{
        modelId: string
        status: string
        message: string
      }>
    }>>('/versions/batch', { data: deleteData })
    return response.data.data
  },

  // ==================== 10. 模型统计接口 ====================
  
  // 10.1 模型统计信息
  async getModelStatistics(params: {
    taskId?: string
    timeRange?: '7d' | '30d' | '90d'
  } = {}): Promise<ModelStatistics> {
    const response = await modelApiInstance.get<ApiResponse<ModelStatistics>>('/statistics', { params })
    return response.data.data
  },

  // 10.2 任务模型统计
  async getTaskModelStatistics(taskId: string): Promise<{
    taskId: string
    taskName: string
    totalRounds: number
    completedRounds: number
    performanceMetrics: {
      bestAccuracy: number
      bestRound: number
      averageAccuracy: number
      accuracyImprovement: number
    }
  }> {
    const response = await modelApiInstance.get<ApiResponse<{
      taskId: string
      taskName: string
      totalRounds: number
      completedRounds: number
      performanceMetrics: {
        bestAccuracy: number
        bestRound: number
        averageAccuracy: number
        accuracyImprovement: number
      }
    }>>(`/statistics/task/${taskId}`)
    return response.data.data
  },
} as const

// 使用命名导出以保持一致性 