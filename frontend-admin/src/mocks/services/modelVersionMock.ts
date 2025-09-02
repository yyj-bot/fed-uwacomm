/**
 * 模型版本管理 Mock 数据
 * 基于接口文档提供一致的模拟数据
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import type { ApiResponse, PaginatedResponse } from '@/types'

// ==================== Mock 数据定义 ====================

// 模拟模型版本数据
const mockModelVersions = [
  {
    modelId: 'c3d4e5f6789012345678901234567890',
    taskId: 'a1b2c3d4e5f678901234567890123456',
    roundNumber: 1,
    accuracy: 0.8500,
    loss: 0.123456,
    status: 'UPLOADED',
    description: '第1轮模型',
    parameters: {
      learning_rate: 0.001,
      batch_size: 32
    },
    createdAt: '2024-01-01T10:00:00'
  },
  {
    modelId: 'd4e5f678901234567890123456789012',
    taskId: 'a1b2c3d4e5f678901234567890123456',
    roundNumber: 2,
    accuracy: 0.8700,
    loss: 0.098765,
    status: 'UPLOADED',
    description: '第2轮模型',
    parameters: {
      learning_rate: 0.001,
      batch_size: 32
    },
    createdAt: '2024-01-01T11:00:00'
  },
  {
    modelId: 'e5f678901234567890123456789012ab',
    taskId: 'b2c3d4e5f678901234567890123456cd',
    roundNumber: 5,
    accuracy: 0.9000,
    loss: 0.065432,
    status: 'DEPLOYED',
    description: '第5轮模型',
    parameters: {
      learning_rate: 0.0008,
      batch_size: 64
    },
    createdAt: '2024-01-01T15:00:00'
  },
  {
    modelId: 'c3d4e5f6789012345678901234567891',
    taskId: 'a1b2c3d4e5f678901234567890123456',
    roundNumber: 3,
    accuracy: 0.8800,
    loss: 0.087654,
    status: 'UPLOADED',
    description: '第3轮模型',
    parameters: {
      learning_rate: 0.001,
      batch_size: 32
    },
    createdAt: '2024-01-01T12:00:00'
  }
]

// 模拟任务数据
const mockTasks = [
  {
    taskId: 'a1b2c3d4e5f678901234567890123456',
    taskName: '水声分类任务',
    totalModels: 50,
    completedRounds: 25
  },
  {
    taskId: 'b2c3d4e5f678901234567890123456cd',
    taskName: '环境声学任务',
    totalModels: 30,
    completedRounds: 15
  }
]

// 模拟部署数据
const mockDeployments = [
  {
    deploymentId: 'deploy_1234567890',
    modelId: 'c3d4e5f6789012345678901234567890',
    deploymentName: '水声分类模型_v1.0',
    status: 'RUNNING',
    targetVms: ['vm_1', 'vm_2'],
    replicas: {
      desired: 2,
      available: 2,
      ready: 2
    },
    endpoints: [
      'http://vm_1:8080/predict',
      'http://vm_2:8080/predict'
    ],
    healthCheck: {
      status: 'HEALTHY',
      lastCheck: '2024-01-01T10:00:00',
      responseTime: 50
    },
    createdAt: '2024-01-01T10:00:00',
    updatedAt: '2024-01-01T10:00:00'
  }
]

// 模拟评估结果数据
const mockEvaluationResults = [
  {
    evaluationId: 'eval_1234567890',
    modelId: 'c3d4e5f6789012345678901234567890',
    taskId: 'a1b2c3d4e5f678901234567890123456',
    metrics: {
      accuracy: 0.8500,
      loss: 0.123456,
      precision: 0.8200,
      recall: 0.8300,
      f1: 0.8250
    },
    evaluationTime: 15.5,
    testSamples: 1000,
    status: 'COMPLETED',
    createdAt: '2024-01-01T10:00:00'
  }
]

// 模拟回滚历史数据
const mockRollbackHistory = [
  {
    rollbackId: 'rollback_1234567890',
    deploymentId: 'deploy_1234567890',
    fromModelId: 'c3d4e5f6789012345678901234567890',
    toModelId: 'c3d4e5f6789012345678901234567891',
    status: 'COMPLETED',
    rollbackReason: '性能下降',
    rollbackTime: 30.5,
    createdAt: '2024-01-01T10:00:00'
  }
]

// ==================== Mock 函数实现 ====================

export const modelVersionMock = {
  // ==================== 3. 模型上传接口 ====================

  // 3.1 模型文件上传
  uploadModel: (formData: FormData): ApiResponse<{
    modelId: string
    taskId: string
    roundNumber: number
    status: string
    description?: string
    parameters: Record<string, unknown>
    createdAt: string
  }> => {
    const taskId = formData.get('taskId') as string
    const roundNumber = parseInt(formData.get('roundNumber') as string, 10)
    const description = formData.get('description') as string
    const file = formData.get('file') as File

    // 验证文件大小
    if (file && file.size > 100 * 1024 * 1024) {
      return {
        code: 400,
        message: '模型文件过大',
        data: {
          field: 'file',
          error: '文件大小不能超过100MB'
        }
      } as any
    }

    // 验证文件格式
    if (file) {
      const supportedFormats = ['.pth', '.pt', '.h5', '.pb', '.onnx', '.pkl', '.pickle', '.joblib']
      const fileName = file.name.toLowerCase()
      const isValidFormat = supportedFormats.some(format => fileName.endsWith(format))
      if (!isValidFormat) {
        return {
          code: 400,
          message: '模型上传失败',
          data: {
            field: 'file',
            error: '文件格式不支持'
          }
        } as any
      }
    }

    const newModelId = 'f' + Math.random().toString(16).slice(2, 33)
    return {
      code: 200,
      message: '模型上传成功',
      data: {
        modelId: newModelId,
        taskId: taskId,
        roundNumber: roundNumber,
        status: 'UPLOADED',
        description: description || `第${roundNumber}轮模型`,
        parameters: {
          learning_rate: 0.001,
          batch_size: 32
        },
        createdAt: new Date().toISOString()
      }
    }
  },

  // 3.2 批量模型上传
  uploadModelBatch: (batchData: {
    taskId: string
    models: Array<{
      roundNumber: number
      description?: string
      parameters?: Record<string, unknown>
      file: File
    }>
  }): ApiResponse<{
    successCount: number
    failedCount: number
    models: Array<{
      modelId: string
      status: string
      message: string
    }>
  }> => {
    const results = batchData.models.map((model, index) => {
      // 模拟一些失败的情况
      if (model.file.size > 100 * 1024 * 1024) {
        return {
          modelId: '',
          status: 'FAILED',
          message: '文件过大'
        }
      }

      const modelId = 'batch_' + Math.random().toString(16).slice(2, 33)
      return {
        modelId: modelId,
        status: 'UPLOADED',
        message: '上传成功'
      }
    })

    const successCount = results.filter(r => r.status === 'UPLOADED').length
    const failedCount = results.length - successCount

    return {
      code: 200,
      message: '批量上传完成',
      data: {
        successCount,
        failedCount,
        models: results
      }
    }
  },

  // ==================== 4. 模型版本查询接口 ====================

  // 4.1 模型版本列表查询
  getModelVersions: (params: any = {}): ApiResponse<any> => {
    let filteredVersions = [...mockModelVersions]

    // 应用过滤条件
    if (params.taskId) {
      filteredVersions = filteredVersions.filter(v => v.taskId === params.taskId)
    }
    if (params.roundNumber) {
      filteredVersions = filteredVersions.filter(v => v.roundNumber === params.roundNumber)
    }
    if (params.status) {
      filteredVersions = filteredVersions.filter(v => v.status === params.status)
    }

    // 应用排序
    if (params.sort) {
      filteredVersions.sort((a, b) => {
        const aVal = (a as any)[params.sort]
        const bVal = (b as any)[params.sort]
        if (params.order === 'desc') {
          return bVal > aVal ? 1 : -1
        }
        return aVal > bVal ? 1 : -1
      })
    }

    // 应用分页
    const page = params.page || 1
    const size = params.size || 10
    const start = (page - 1) * size
    const end = start + size
    const paginatedVersions = filteredVersions.slice(start, end)

    return {
      code: 200,
      message: '查询成功',
      data: {
        total: filteredVersions.length,
        pages: Math.ceil(filteredVersions.length / size),
        current: page,
        size: size,
        records: paginatedVersions
      }
    }
  },

  // 4.2 模型版本详情查询
  getModelVersionDetail: (modelId: string): ApiResponse<{
    modelId: string
    taskId: string
    roundNumber: number
    aggregationMethod: string
    clientCount: number
    modelJson: Record<string, unknown>
    metrics: { accuracy: number; loss: number }
    createdAt: string
    aggregatedAt: string
    status: string
  }> => {
    const model = mockModelVersions.find(m => m.modelId === modelId)
    
    if (!model) {
      return {
        code: 404,
        message: '模型不存在',
        data: null
      } as any
    }

    return {
      code: 200,
      message: '查询成功',
      data: {
        modelId: model.modelId,
        taskId: model.taskId,
        roundNumber: model.roundNumber,
        aggregationMethod: 'FEDAVG',
        clientCount: 8,
        modelJson: { weights: [1, 2, 3], biases: [0.1, 0.2] },
        metrics: {
          accuracy: model.accuracy,
          loss: model.loss
        },
        createdAt: model.createdAt,
        aggregatedAt: model.createdAt,
        status: model.status
      }
    }
  },

  // 4.3 任务模型版本查询
  getTaskModelVersions: (taskId: string, params: any = {}): ApiResponse<{
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
  }> => {
    const task = mockTasks.find(t => t.taskId === taskId)
    
    if (!task) {
      return {
        code: 404,
        message: '任务不存在',
        data: null
      } as any
    }

    let versions = mockModelVersions.filter(m => m.taskId === taskId)

    // 应用过滤和排序
    if (params.roundNumber) {
      versions = versions.filter(v => v.roundNumber === params.roundNumber)
    }
    if (params.status) {
      versions = versions.filter(v => v.status === params.status)
    }

    if (params.sort) {
      versions.sort((a, b) => {
        const aVal = (a as any)[params.sort]
        const bVal = (b as any)[params.sort]
        if (params.order === 'desc') {
          return bVal > aVal ? 1 : -1
        }
        return aVal > bVal ? 1 : -1
      })
    }

    return {
      code: 200,
      message: '查询成功',
      data: {
        taskId: task.taskId,
        taskName: task.taskName,
        totalModels: task.totalModels,
        versions: versions.map(v => ({
          modelId: v.modelId,
          roundNumber: v.roundNumber,
          accuracy: v.accuracy,
          loss: v.loss,
          status: v.status,
          createdAt: v.createdAt
        }))
      }
    }
  },

  // ==================== 5. 模型性能评估接口 ====================

  // 5.1 模型性能评估
  evaluateModel: (evaluationData: {
    modelId: string
    testDataPath: string
    metrics?: string[]
    batchSize?: number
    device?: string
  }): ApiResponse<{
    modelId: string
    evaluationId: string
    metrics: Record<string, number>
    evaluationTime: number
    testSamples: number
    status: string
    createdAt: string
  }> => {
    const model = mockModelVersions.find(m => m.modelId === evaluationData.modelId)
    
    if (!model) {
      return {
        code: 404,
        message: '模型不存在',
        data: null
      } as any
    }

    return {
      code: 200,
      message: '评估完成',
      data: {
        modelId: evaluationData.modelId,
        evaluationId: 'eval_' + Math.random().toString(16).slice(2, 12),
        metrics: {
          accuracy: model.accuracy,
          loss: model.loss,
          precision: 0.8200,
          recall: 0.8300,
          f1: 0.8250
        },
        evaluationTime: 15.5,
        testSamples: 1000,
        status: 'COMPLETED',
        createdAt: new Date().toISOString()
      }
    }
  },

  // 5.2 批量模型评估
  evaluateModelBatch: (evaluationData: {
    taskId: string
    testDataPath: string
    roundNumbers?: number[]
    metrics?: string[]
    batchSize?: number
  }): ApiResponse<{
    taskId: string
    evaluatedCount: number
    results: Array<{
      modelId: string
      roundNumber: number
      accuracy: number
      loss: number
      status: string
    }>
  }> => {
    const task = mockTasks.find(t => t.taskId === evaluationData.taskId)
    
    if (!task) {
      return {
        code: 404,
        message: '任务不存在',
        data: null
      } as any
    }

    let modelsToEvaluate = mockModelVersions.filter(m => m.taskId === evaluationData.taskId)
    
    if (evaluationData.roundNumbers && evaluationData.roundNumbers.length > 0) {
      modelsToEvaluate = modelsToEvaluate.filter(m => 
        evaluationData.roundNumbers!.includes(m.roundNumber)
      )
    }

    const results = modelsToEvaluate.map(model => ({
      modelId: model.modelId,
      roundNumber: model.roundNumber,
      accuracy: model.accuracy,
      loss: model.loss,
      status: 'COMPLETED'
    }))

    return {
      code: 200,
      message: '批量评估完成',
      data: {
        taskId: evaluationData.taskId,
        evaluatedCount: results.length,
        results
      }
    }
  },

  // 5.3 评估结果查询
  getEvaluationResults: (params: any = {}): ApiResponse<any> => {
    let filteredResults = [...mockEvaluationResults]

    // 应用过滤条件
    if (params.modelId) {
      filteredResults = filteredResults.filter(r => r.modelId === params.modelId)
    }
    if (params.taskId) {
      filteredResults = filteredResults.filter(r => r.taskId === params.taskId)
    }
    if (params.evaluationId) {
      filteredResults = filteredResults.filter(r => r.evaluationId === params.evaluationId)
    }

    // 应用分页
    const page = params.page || 1
    const size = params.size || 10
    const start = (page - 1) * size
    const end = start + size
    const paginatedResults = filteredResults.slice(start, end)

    return {
      code: 200,
      message: '查询成功',
      data: {
        total: filteredResults.length,
        pages: Math.ceil(filteredResults.length / size),
        current: page,
        size: size,
        records: paginatedResults
      }
    }
  },

  // ==================== 6. 模型部署接口 ====================

  // 6.1 模型部署
  deployModel: (deploymentData: {
    modelId: string
    deploymentName: string
    targetVms?: string[]
    deploymentConfig?: any
    description?: string
  }): ApiResponse<{
    deploymentId: string
    modelId: string
    deploymentName: string
    targetVms?: string[]
    status: string
    deploymentConfig?: any
    endpoints?: string[]
    createdAt: string
  }> => {
    const model = mockModelVersions.find(m => m.modelId === deploymentData.modelId)
    
    if (!model) {
      return {
        code: 404,
        message: '模型不存在',
        data: null
      } as any
    }

    const deploymentId = 'deploy_' + Math.random().toString(16).slice(2, 12)

    return {
      code: 200,
      message: '部署成功',
      data: {
        deploymentId: deploymentId,
        modelId: deploymentData.modelId,
        deploymentName: deploymentData.deploymentName,
        targetVms: deploymentData.targetVms || ['vm_1', 'vm_2'],
        status: 'DEPLOYED',
        deploymentConfig: deploymentData.deploymentConfig || {
          replicas: 2,
          resources: {
            cpu: '1',
            memory: '2Gi'
          }
        },
        endpoints: [
          'http://vm_1:8080/predict',
          'http://vm_2:8080/predict'
        ],
        createdAt: new Date().toISOString()
      }
    }
  },

  // 6.2 部署状态查询
  getDeploymentStatus: (deploymentId: string): ApiResponse<{
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
  }> => {
    const deployment = mockDeployments.find(d => d.deploymentId === deploymentId)
    
    if (!deployment) {
      return {
        code: 404,
        message: '部署不存在',
        data: null
      } as any
    }

    return {
      code: 200,
      message: '查询成功',
      data: deployment
    }
  },

  // 6.3 部署列表查询
  getDeploymentList: (params: any = {}): ApiResponse<any> => {
    let filteredDeployments = [...mockDeployments]

    // 应用过滤条件
    if (params.modelId) {
      filteredDeployments = filteredDeployments.filter(d => d.modelId === params.modelId)
    }
    if (params.status) {
      filteredDeployments = filteredDeployments.filter(d => d.status === params.status)
    }

    // 应用分页
    const page = params.page || 1
    const size = params.size || 10
    const start = (page - 1) * size
    const end = start + size
    const paginatedDeployments = filteredDeployments.slice(start, end)

    return {
      code: 200,
      message: '查询成功',
      data: {
        total: filteredDeployments.length,
        pages: Math.ceil(filteredDeployments.length / size),
        current: page,
        size: size,
        records: paginatedDeployments.map(d => ({
          deploymentId: d.deploymentId,
          modelId: d.modelId,
          deploymentName: d.deploymentName,
          status: d.status,
          replicas: {
            desired: d.replicas.desired,
            available: d.replicas.available
          },
          createdAt: d.createdAt
        }))
      }
    }
  },

  // ==================== 7. 模型回滚接口 ====================

  // 7.1 模型回滚
  rollbackModel: (rollbackData: {
    deploymentId: string
    targetModelId: string
    rollbackReason?: string
    force?: boolean
  }): ApiResponse<{
    rollbackId: string
    deploymentId: string
    fromModelId: string
    toModelId: string
    status: string
    rollbackReason?: string
    rollbackTime: number
    createdAt: string
  }> => {
    const deployment = mockDeployments.find(d => d.deploymentId === rollbackData.deploymentId)
    
    if (!deployment) {
      return {
        code: 404,
        message: '部署不存在',
        data: null
      } as any
    }

    const targetModel = mockModelVersions.find(m => m.modelId === rollbackData.targetModelId)
    
    if (!targetModel) {
      return {
        code: 404,
        message: '目标模型不存在',
        data: null
      } as any
    }

    return {
      code: 200,
      message: '回滚成功',
      data: {
        rollbackId: 'rollback_' + Math.random().toString(16).slice(2, 12),
        deploymentId: rollbackData.deploymentId,
        fromModelId: deployment.modelId,
        toModelId: rollbackData.targetModelId,
        status: 'COMPLETED',
        rollbackReason: rollbackData.rollbackReason || '性能优化',
        rollbackTime: 30.5,
        createdAt: new Date().toISOString()
      }
    }
  },

  // 7.2 回滚历史查询
  getRollbackHistory: (params: any = {}): ApiResponse<any> => {
    let filteredHistory = [...mockRollbackHistory]

    // 应用过滤条件
    if (params.deploymentId) {
      filteredHistory = filteredHistory.filter(h => h.deploymentId === params.deploymentId)
    }

    // 应用分页
    const page = params.page || 1
    const size = params.size || 10
    const start = (page - 1) * size
    const end = start + size
    const paginatedHistory = filteredHistory.slice(start, end)

    return {
      code: 200,
      message: '查询成功',
      data: {
        total: filteredHistory.length,
        pages: Math.ceil(filteredHistory.length / size),
        current: page,
        size: size,
        records: paginatedHistory
      }
    }
  },

  // ==================== 8. 模型下载接口 ====================

  // 8.1 模型文件下载
  downloadModel: (modelId: string, params: any = {}): Blob => {
    const model = mockModelVersions.find(m => m.modelId === modelId)
    
    if (!model) {
      throw new Error('模型不存在')
    }

    // 创建模拟的二进制数据
    const mockData = new Uint8Array([1, 2, 3, 4, 5])
    return new Blob([mockData], { type: 'application/octet-stream' })
  },

  // 8.2 批量模型下载
  downloadModelBatch: (downloadData: {
    modelIds: string[]
    format?: string
    compressed?: boolean
  }): Blob => {
    // 验证模型是否存在
    const existingModels = downloadData.modelIds.filter(id => 
      mockModelVersions.some(m => m.modelId === id)
    )

    if (existingModels.length === 0) {
      throw new Error('没有找到有效的模型')
    }

    // 创建模拟的ZIP文件数据
    const mockZipData = new Uint8Array([80, 75, 3, 4]) // ZIP文件头
    return new Blob([mockZipData], { type: 'application/zip' })
  },

  // ==================== 9. 模型删除接口 ====================

  // 9.1 模型版本删除
  deleteModel: (modelId: string, deleteData: any = {}): ApiResponse<{
    modelId: string
    deletedAt: string
  }> => {
    const modelIndex = mockModelVersions.findIndex(m => m.modelId === modelId)
    
    if (modelIndex === -1) {
      return {
        code: 404,
        message: '模型不存在',
        data: null
      } as any
    }

    // 检查模型是否正在使用
    const isDeployed = mockDeployments.some(d => d.modelId === modelId && d.status === 'RUNNING')
    if (isDeployed && !deleteData.force) {
      return {
        code: 409,
        message: '模型正在使用中，无法删除',
        data: null
      } as any
    }

    // 模拟删除操作
    mockModelVersions.splice(modelIndex, 1)

    return {
      code: 200,
      message: '删除成功',
      data: {
        modelId: modelId,
        deletedAt: new Date().toISOString()
      }
    }
  },

  // 9.2 批量模型删除
  deleteModelBatch: (deleteData: {
    modelIds: string[]
    force?: boolean
    deleteFile?: boolean
  }): ApiResponse<{
    successCount: number
    failedCount: number
    results: Array<{
      modelId: string
      status: string
      message: string
    }>
  }> => {
    const results = deleteData.modelIds.map(modelId => {
      const modelExists = mockModelVersions.some(m => m.modelId === modelId)
      
      if (!modelExists) {
        return {
          modelId: modelId,
          status: 'FAILED',
          message: '模型不存在'
        }
      }

      const isDeployed = mockDeployments.some(d => d.modelId === modelId && d.status === 'RUNNING')
      if (isDeployed && !deleteData.force) {
        return {
          modelId: modelId,
          status: 'FAILED',
          message: '模型正在使用中'
        }
      }

      return {
        modelId: modelId,
        status: 'DELETED',
        message: '删除成功'
      }
    })

    const successCount = results.filter(r => r.status === 'DELETED').length
    const failedCount = results.length - successCount

    return {
      code: 200,
      message: '批量删除完成',
      data: {
        successCount,
        failedCount,
        results
      }
    }
  },

  // ==================== 10. 模型统计接口 ====================

  // 10.1 模型统计信息
  getModelStatistics: (params: any = {}): ApiResponse<{
    totalModels: number
    averageAccuracy: number
    averageLoss: number
    uploadTrend: Array<{ date: string; count: number }>
    accuracyTrend: Array<{ roundNumber: number; accuracy: number }>
  }> => {
    let models = [...mockModelVersions]
    
    if (params.taskId) {
      models = models.filter(m => m.taskId === params.taskId)
    }

    const totalModels = models.length
    const averageAccuracy = models.reduce((sum, m) => sum + m.accuracy, 0) / totalModels
    const averageLoss = models.reduce((sum, m) => sum + m.loss, 0) / totalModels

    return {
      code: 200,
      message: '查询成功',
      data: {
        totalModels,
        averageAccuracy,
        averageLoss,
        uploadTrend: [
          { date: '2024-01-01', count: 5 },
          { date: '2024-01-02', count: 3 },
          { date: '2024-01-03', count: 7 }
        ],
        accuracyTrend: models.map(m => ({
          roundNumber: m.roundNumber,
          accuracy: m.accuracy
        }))
      }
    }
  },

  // 10.2 任务模型统计
  getTaskModelStatistics: (taskId: string): ApiResponse<{
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
  }> => {
    const task = mockTasks.find(t => t.taskId === taskId)
    
    if (!task) {
      return {
        code: 404,
        message: '任务不存在',
        data: null
      } as any
    }

    const taskModels = mockModelVersions.filter(m => m.taskId === taskId)
    const bestModel = taskModels.reduce((best, current) => 
      current.accuracy > best.accuracy ? current : best
    )
    const averageAccuracy = taskModels.reduce((sum, m) => sum + m.accuracy, 0) / taskModels.length

    return {
      code: 200,
      message: '查询成功',
      data: {
        taskId: task.taskId,
        taskName: task.taskName,
        totalRounds: 100,
        completedRounds: task.completedRounds,
        performanceMetrics: {
          bestAccuracy: bestModel.accuracy,
          bestRound: bestModel.roundNumber,
          averageAccuracy: averageAccuracy,
          accuracyImprovement: 0.0500
        }
      }
    }
  }
} as const

export default modelVersionMock
