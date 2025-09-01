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
const modelApiInstance = createApiInstance('http://localhost:8080/api/model')

// 部署配置类型
interface DeploymentConfig {
  replicas?: number
  resources?: {
    cpu?: string
    memory?: string
  }
  environment?: Record<string, string>
}

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

// 部署信息类型
interface DeploymentInfo {
  deploymentId: string
  modelId: string
  deploymentName: string
  targetVms?: string[]
  status: string
  deploymentConfig?: DeploymentConfig
  endpoints?: string[]
  createdAt: string
}

// 回滚信息类型
interface RollbackInfo {
  rollbackId: string
  deploymentId: string
  fromModelId: string
  toModelId: string
  status: string
  rollbackReason?: string
  rollbackTime?: number
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

// ==================== 全局模型版本管理API ====================
export const model = {
  // ==================== 3. 模型上传接口 ====================
  
  // 3.1 模型文件上传
  async uploadModel(formData: FormData): Promise<{
    modelId: string
    taskId: string
    roundNumber: number
    status: string
    description?: string
    parameters: Record<string, unknown>
    createdAt: string
  }> {
    const response = await modelApiInstance.post<ApiResponse<{
      modelId: string
      taskId: string
      roundNumber: number
      status: string
      description?: string
      parameters: Record<string, unknown>
      createdAt: string
    }>>('/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    return response.data.data
  },

  // 3.2 批量模型上传
  async uploadModelBatch(batchData: {
    taskId: string
    models: Array<{
      roundNumber: number
      description?: string
      parameters?: Record<string, unknown>
      file: File
    }>
  }): Promise<{
    successCount: number
    failedCount: number
    models: Array<{
      modelId: string
      status: string
      message: string
    }>
  }> {
    const formData = new FormData()
    formData.append('taskId', batchData.taskId)
    
    batchData.models.forEach((model, index) => {
      formData.append(`models[${index}].roundNumber`, model.roundNumber.toString())
      if (model.description) {
        formData.append(`models[${index}].description`, model.description)
      }
      if (model.parameters) {
        formData.append(`models[${index}].parameters`, JSON.stringify(model.parameters))
      }
      formData.append(`models[${index}].file`, model.file)
    })

    const response = await modelApiInstance.post<ApiResponse<{
      successCount: number
      failedCount: number
      models: Array<{
        modelId: string
        status: string
        message: string
      }>
    }>>('/upload/batch', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    return response.data.data
  },

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

  // ==================== 6. 模型部署接口 ====================
  
  // 6.1 模型部署
  async deployModel(deploymentData: {
    modelId: string
    deploymentName: string
    targetVms?: string[]
    deploymentConfig?: DeploymentConfig
    description?: string
  }): Promise<{
    deploymentId: string
    modelId: string
    deploymentName: string
    targetVms?: string[]
    status: string
    deploymentConfig?: DeploymentConfig
    endpoints?: string[]
    createdAt: string
  }> {
    const response = await modelApiInstance.post<ApiResponse<{
      deploymentId: string
      modelId: string
      deploymentName: string
      targetVms?: string[]
      status: string
      deploymentConfig?: DeploymentConfig
      endpoints?: string[]
      createdAt: string
    }>>('/deploy', deploymentData)
    return response.data.data
  },

  // 6.2 部署状态查询
  async getDeploymentStatus(deploymentId: string): Promise<{
    deploymentId: string
    modelId: string
    deploymentName: string
    status: string
    replicas?: {
      desired: number
      available: number
      ready: number
    }
    endpoints?: string[]
    healthCheck?: {
      status: string
      lastCheck: string
      responseTime: number
    }
    createdAt: string
    updatedAt: string
  }> {
    const response = await modelApiInstance.get<ApiResponse<{
      deploymentId: string
      modelId: string
      deploymentName: string
      status: string
      replicas?: {
        desired: number
        available: number
        ready: number
      }
      endpoints?: string[]
      healthCheck?: {
        status: string
        lastCheck: string
        responseTime: number
      }
      createdAt: string
      updatedAt: string
    }>>(`/deploy/status/${deploymentId}`)
    return response.data.data
  },

  // 6.3 部署列表查询
  async getDeploymentList(params: {
    modelId?: string
    status?: string
    page?: number
    size?: number
  } = {}): Promise<ModelVersionPaginatedResponse<{
    deploymentId: string
    modelId: string
    deploymentName: string
    status: string
    replicas?: {
      desired: number
      available: number
    }
    createdAt: string
  }>> {
    const response = await modelApiInstance.get<ApiResponse<ModelVersionPaginatedResponse<{
      deploymentId: string
      modelId: string
      deploymentName: string
      status: string
      replicas?: {
        desired: number
        available: number
      }
      createdAt: string
    }>>>('/deploy/list', { params })
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
  } = {}): Promise<ModelVersionPaginatedResponse<RollbackInfo>> {
    const response = await modelApiInstance.get<ApiResponse<ModelVersionPaginatedResponse<RollbackInfo>>>('/rollback/history', { params })
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