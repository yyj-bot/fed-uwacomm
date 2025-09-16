/**
 * Model Version Store Mock 数据
 * 用于测试模型版本管理相关功能的模拟数据
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import type { ModelVersion } from '@/types'
import type { 
  UploadModelRequest,
  EvaluationRequest,
  EvaluationResult,
  DeploymentRequest,
  RollbackRequest,
  DeleteModelRequest,
  DownloadRequest,
  TaskModelVersions,
  ModelStatistics,
  TaskStatistics
} from '@/services'

// ==================== 模拟模型版本数据 ====================

export const mockModelVersion: ModelVersion = {
  modelId: 'model-001',
  taskId: 'task-001',
  roundNumber: 10,
  accuracy: 92.5,
  loss: 0.275,
  status: 'DEPLOYED',
  description: 'MNIST CNN模型 - 第10轮训练结果',
  parameters: {
    layers: 6,
    totalParams: 431080,
    trainableParams: 431080,
    nonTrainableParams: 0,
    modelSize: '1.7MB',
    framework: 'TensorFlow'
  },
  createdAt: '2024-01-15T10:00:00Z'
}

export const mockModelVersionList: ModelVersion[] = [
  mockModelVersion,
  {
    modelId: 'model-002',
    taskId: 'task-001',
    roundNumber: 9,
    accuracy: 91.8,
    loss: 0.295,
    status: 'VALIDATED',
    description: 'MNIST CNN模型 - 第9轮训练结果',
    parameters: {
      layers: 6,
      totalParams: 431080,
      trainableParams: 431080,
      nonTrainableParams: 0,
      modelSize: '1.7MB',
      framework: 'TensorFlow'
    },
    createdAt: '2024-01-15T09:45:00Z'
  },
  {
    modelId: 'model-003',
    taskId: 'task-002',
    roundNumber: 5,
    accuracy: 85.2,
    loss: 0.425,
    status: 'UPLOADING',
    description: 'CIFAR-10模型 - 第5轮训练结果',
    parameters: {
      layers: 8,
      totalParams: 1250000,
      trainableParams: 1250000,
      nonTrainableParams: 0,
      modelSize: '4.8MB',
      framework: 'PyTorch'
    },
    createdAt: '2024-01-15T11:00:00Z'
  }
]

// ==================== 模拟任务模型版本 ====================

export const mockTaskModelVersions: TaskModelVersions = {
  taskId: 'task-001',
  taskName: 'MNIST分类任务',
  totalModels: 3,
  versions: [
    {
      modelId: 'model-001',
      roundNumber: 10,
      accuracy: 92.5,
      loss: 0.275,
      status: 'DEPLOYED',
      createdAt: '2024-01-15T10:00:00Z'
    },
    {
      modelId: 'model-002',
      roundNumber: 9,
      accuracy: 91.8,
      loss: 0.295,
      status: 'VALIDATED',
      createdAt: '2024-01-15T09:45:00Z'
    },
    {
      modelId: 'model-004',
      roundNumber: 8,
      accuracy: 90.5,
      loss: 0.325,
      status: 'DEPRECATED',
      createdAt: '2024-01-15T09:30:00Z'
    }
  ]
}

// ==================== 模拟评估结果 ====================

export const mockEvaluationResult: EvaluationResult = {
  evaluationId: 'eval-001',
  modelId: 'model-001',
  taskId: 'task-001',
  metrics: {
    accuracy: 92.5,
    precision: 91.8,
    recall: 92.1,
    f1Score: 91.9,
    loss: 0.275,
    auc: 0.985
  },
  evaluationTime: 120,
  testSamples: 10000,
  status: 'COMPLETED',
  createdAt: '2024-01-15T10:30:00Z'
}

// ==================== 模拟请求数据 ====================

export const mockUploadModelRequest: UploadModelRequest = {
  taskId: 'task-001',
  roundNumber: 11,
  description: '新上传的模型版本',
  parameters: {
    framework: 'TensorFlow',
    version: '2.12.0',
    pythonVersion: '3.9.0',
    trainingDuration: 3600,
    hyperparameters: {
      learningRate: 0.001,
      batchSize: 32,
      epochs: 10
    }
  },
  file: new File(['model data'], 'model.h5', { type: 'application/octet-stream' })
}

export const mockEvaluationRequest: EvaluationRequest = {
  modelId: 'model-001',
  testDataPath: '/data/test/mnist.csv',
  metrics: ['accuracy', 'precision', 'recall', 'f1Score', 'auc'],
  batchSize: 32,
  device: 'cpu'
}

export const mockDeploymentRequest: DeploymentRequest = {
  modelId: 'model-001',
  deploymentName: 'mnist-model-deployment',
  targetVms: ['vm-001', 'vm-002'],
  deploymentConfig: {
    replicas: 3,
    resources: {
      cpu: '1000m',
      memory: '2Gi'
    },
    environment: {
      'MODEL_PATH': '/models/mnist',
      'BATCH_SIZE': '32'
    }
  },
  description: '生产环境模型部署'
}

export const mockRollbackRequest: RollbackRequest = {
  deploymentId: 'deploy-001',
  targetModelId: 'model-002',
  rollbackReason: 'Performance degradation detected',
  force: true
}

export const mockDeleteModelRequest: DeleteModelRequest = {
  force: true,
  deleteFile: true
}

export const mockDownloadRequest: DownloadRequest = {
  format: 'original',
  compressed: true
}

// ==================== 模拟响应数据 ====================

export const mockUploadModelResponse = {
  modelId: 'model-new-001',
  uploadId: 'upload-model-001',
  status: 'UPLOADING',
  estimatedTime: 600
}

export const mockDeploymentResponse = {
  deploymentId: 'deploy-001',
  status: 'DEPLOYING',
  endpoint: 'https://api.example.com/models/model-001',
  estimatedTime: 300
}

export const mockRollbackResponse = {
  rollbackId: 'rollback-001',
  previousVersion: 'v1.0.0',
  status: 'ROLLING_BACK',
  estimatedTime: 180
}

// ==================== 模拟统计数据 ====================

export const mockModelStatistics: ModelStatistics = {
  totalModels: 150,
  averageAccuracy: 87.5,
  averageLoss: 0.325,
  uploadTrend: [
    { date: '2024-01-01', count: 5 },
    { date: '2024-01-02', count: 8 },
    { date: '2024-01-03', count: 12 },
    { date: '2024-01-04', count: 7 },
    { date: '2024-01-05', count: 15 }
  ],
  accuracyTrend: [
    { roundNumber: 1, accuracy: 75.2 },
    { roundNumber: 2, accuracy: 78.5 },
    { roundNumber: 3, accuracy: 82.1 },
    { roundNumber: 4, accuracy: 85.3 },
    { roundNumber: 5, accuracy: 87.5 }
  ]
}

export const mockTaskStatistics: TaskStatistics = {
  taskId: 'task-001',
  taskName: 'MNIST分类任务',
  totalRounds: 10,
  completedRounds: 10,
  performanceMetrics: {
    bestAccuracy: 92.5,
    bestRound: 10,
    averageAccuracy: 88.7,
    accuracyImprovement: 15.2
  }
}

// ==================== 模拟错误信息 ====================

export const mockModelVersionErrors = {
  FETCH_MODEL_LIST_ERROR: new Error('获取模型列表失败'),
  FETCH_MODEL_DETAIL_ERROR: new Error('获取模型详情失败'),
  UPLOAD_MODEL_ERROR: new Error('模型上传失败'),
  EVALUATE_MODEL_ERROR: new Error('模型评估失败'),
  DEPLOY_MODEL_ERROR: new Error('模型部署失败'),
  ROLLBACK_MODEL_ERROR: new Error('模型回滚失败'),
  DELETE_MODEL_ERROR: new Error('删除模型失败'),
  DOWNLOAD_MODEL_ERROR: new Error('下载模型失败'),
  FETCH_TASK_MODELS_ERROR: new Error('获取任务模型失败'),
  FETCH_MODEL_STATISTICS_ERROR: new Error('获取模型统计失败'),
  FETCH_TASK_STATISTICS_ERROR: new Error('获取任务统计失败'),
  NETWORK_ERROR: new Error('网络连接失败'),
  TIMEOUT_ERROR: new Error('请求超时'),
  SERVER_ERROR: new Error('服务器内部错误'),
  PERMISSION_ERROR: new Error('权限不足'),
  STORAGE_ERROR: new Error('存储空间不足'),
  MODEL_NOT_FOUND: new Error('模型不存在'),
  MODEL_ALREADY_DEPLOYED: new Error('模型已部署'),
  INVALID_MODEL_FORMAT: new Error('模型格式无效')
}

// ==================== 模拟初始状态 ====================

export const mockInitialModelVersionState = {
  modelVersionList: [],
  modelVersionListTotal: 0,
  modelVersionListLoading: false,
  modelVersionListError: null,
  currentModelVersion: null,
  currentModelVersionLoading: false,
  currentModelVersionError: null,
  taskModelVersions: {},
  taskModelVersionsLoading: {},
  evaluationResults: {},
  evaluationLoading: {},
  uploadLoading: {},
  uploadProgress: {},
  uploadError: {},
  operationLoading: {},
  operationError: {},
  modelStatistics: null,
  modelStatisticsLoading: false,
  taskStatistics: {},
  taskStatisticsLoading: {},
  pagination: {
    page: 1,
    size: 20,
    total: 0
  },
  queryParams: {},
  selectedModelIds: []
}

// ==================== 模拟工具函数 ====================

export const createMockModelVersion = (overrides: Partial<ModelVersion> = {}): ModelVersion => ({
  ...mockModelVersion,
  modelId: `model-${Date.now()}`,
  createdAt: new Date().toISOString(),
  ...overrides
})

export const createMockEvaluationResult = (overrides: Partial<EvaluationResult> = {}): EvaluationResult => ({
  ...mockEvaluationResult,
  evaluationId: `eval-${Date.now()}`,
  createdAt: new Date().toISOString(),
  ...overrides
})

export const createMockUploadModelRequest = (overrides: Partial<UploadModelRequest> = {}): UploadModelRequest => ({
  ...mockUploadModelRequest,
  ...overrides
})

export const createMockEvaluationRequest = (overrides: Partial<EvaluationRequest> = {}): EvaluationRequest => ({
  ...mockEvaluationRequest,
  ...overrides
})

export const createMockDeploymentRequest = (overrides: Partial<DeploymentRequest> = {}): DeploymentRequest => ({
  ...mockDeploymentRequest,
  ...overrides
})

export const createMockModelStatistics = (overrides: Partial<ModelStatistics> = {}): ModelStatistics => ({
  ...mockModelStatistics,
  ...overrides
})

export const createMockTaskStatistics = (overrides: Partial<TaskStatistics> = {}): TaskStatistics => ({
  ...mockTaskStatistics,
  ...overrides
})
