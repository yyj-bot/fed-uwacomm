/**
 * 模型管理 Mock 数据
 * 严格按照 initial-model-api-reference.md 和 model-version-api-reference.md 规范定义
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

// ==================== 数据生成工具函数 ====================

/**
 * 生成32位十六进制模型ID
 */
export const createMockModelId = (): string => {
  return Array.from({ length: 32 }, () => 
    Math.floor(Math.random() * 16).toString(16)
  ).join('')
}

/**
 * 生成分发ID
 */
export const createMockDistributionId = (): string => {
  return `dist_${Array.from({ length: 8 }, () => 
    Math.floor(Math.random() * 16).toString(16)
  ).join('')}`
}

/**
 * 生成评估ID
 */
export const createMockEvaluationId = (): string => {
  return `eval_${Array.from({ length: 10 }, () => 
    Math.random().toString(36).charAt(0)
  ).join('')}`
}

/**
 * 生成回滚ID
 */
export const createMockRollbackId = (): string => {
  return `rollback_${Array.from({ length: 12 }, () => 
    Math.floor(Math.random() * 16).toString(16)
  ).join('')}`
}



/**
 * 生成随机时间戳
 */
const generateTimestamp = (daysAgo: number = 0, hoursOffset: number = 0): string => {
  const date = new Date()
  date.setDate(date.getDate() - daysAgo)
  date.setHours(date.getHours() + hoursOffset)
  return date.toISOString()
}

/**
 * 生成随机数值（带小数位）
 */
const generateRandomFloat = (min: number, max: number, decimals: number = 4): number => {
  const value = min + Math.random() * (max - min)
  return Math.round(value * Math.pow(10, decimals)) / Math.pow(10, decimals)
}

/**
 * 随机选择数组元素
 */
const randomChoice = <T>(array: T[]): T => {
  return array[Math.floor(Math.random() * array.length)]
}

/**
 * 生成校验和
 */
const generateChecksum = (): string => {
  const chars = '0123456789abcdef'
  return 'sha256:' + Array.from({ length: 64 }, () => 
    chars[Math.floor(Math.random() * chars.length)]
  ).join('')
}

// ============= 枚举定义 =============

// 初始模型状态枚举
export const InitialModelStatus = {
  GENERATING: 'GENERATING',
  READY: 'READY', 
  UPLOADED: 'UPLOADED',
  DISTRIBUTING: 'DISTRIBUTING',
  DISTRIBUTED: 'DISTRIBUTED',
  FAILED: 'FAILED',
  DELETED: 'DELETED'
} as const

// 分发状态枚举
export const DistributionStatus = {
  PENDING: 'PENDING',
  IN_PROGRESS: 'IN_PROGRESS', 
  COMPLETED: 'COMPLETED',
  FAILED: 'FAILED',
  CANCELLED: 'CANCELLED'
} as const

// 模型版本状态枚举
export const ModelVersionStatus = {
  UPLOADING: 'UPLOADING',
  UPLOADED: 'UPLOADED',
  VALIDATING: 'VALIDATING',
  VALIDATED: 'VALIDATED',
  DEPLOYED: 'DEPLOYED',
  DEPRECATED: 'DEPRECATED',
  FAILED: 'FAILED'
} as const

// 聚合方法枚举
export const AggregationMethod = {
  FEDAVG: 'FEDAVG',
  FEDPROX: 'FEDPROX',
  SCAFFOLD: 'SCAFFOLD',
  FEDNOVA: 'FEDNOVA'
} as const

// VM分发详情状态
export const VmDistributionStatus = {
  SUCCESS: 'SUCCESS',
  FAILED: 'FAILED',
  PENDING: 'PENDING',
  IN_PROGRESS: 'IN_PROGRESS'
} as const

// 验证状态
export const VerificationStatus = {
  VERIFIED: 'VERIFIED',
  FAILED: 'FAILED',
  PENDING: 'PENDING'
} as const

// 回滚状态枚举
export const RollbackStatus = {
  PENDING: 'PENDING',
  ROLLING_BACK: 'ROLLING_BACK',
  COMPLETED: 'COMPLETED',
  FAILED: 'FAILED'
} as const




// ============= 类型定义 =============

export interface ModelArchitecture {
  inputSize: number
  hiddenLayers: number[]
  outputSize: number
  activationFunction: string
  optimizer: string
  learningRate: number
}

export interface InitialModelInfo {
  modelId: string
  taskId: string
  modelType: string
  modelSize: number
  parametersCount?: number
  architecture: ModelArchitecture
  generatedAt?: string
  uploadedAt?: string
  createdAt: string
  status: string
  checksum: string
  description?: string
  fileName?: string
  metadata?: Record<string, any>
  distributionStatus?: {
    totalVms: number
    distributedVms: number
    failedVms: number
    distributedAt?: string
  }
}

export interface VmDistributionDetail {
  vmId: string
  status: string
  distributedAt?: string
  verificationStatus: string
  checksum?: string
  errorMessage?: string
}

export interface DistributionProgress {
  distributionId: string
  taskId: string
  modelId: string
  targetVms: string[]
  distributionMode: string
  status: string
  startedAt: string
  completedAt?: string
  estimatedCompletion?: string
  timeout?: number
  retryAttempts?: number
  verifyChecksum?: boolean
  notifyOnCompletion?: boolean
  progress: {
    total: number
    completed: number
    failed: number
    inProgress: number
  }
  vmDetails: VmDistributionDetail[]
}

export interface ModelVersionInfo {
  modelId: string
  taskId: string
  roundNumber: number
  aggregationMethod: string
  clientCount: number
  modelJson: Record<string, any>
  metrics: {
    accuracy: number
    loss: number
  }
  status: string
  createdAt: string
  aggregatedAt: string
}

export interface EvaluationResult {
  evaluationId: string
  modelId: string
  taskId?: string
  metrics: {
    accuracy: number
    loss: number
    precision?: number
    recall?: number
    f1?: number
  }
  evaluationTime: number
  testSamples: number
  status: string
  createdAt: string
  testDataPath?: string
  batchSize?: number
  device?: string
}



export interface ModelStatistics {
  totalModels: number
  averageAccuracy: number
  averageLoss: number
  uploadTrend: Array<{
    date: string
    count: number
  }>
  accuracyTrend: Array<{
    roundNumber: number
    accuracy: number
  }>
}

export interface TaskModelStatistics {
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
}

// 回滚记录接口
export interface RollbackRecord {
  rollbackId: string
  deploymentId: string
  fromModelId: string
  toModelId: string
  status: keyof typeof RollbackStatus
  rollbackReason?: string
  rollbackTime?: number
  createdAt: string
}

// ============= 常量数据 =============

// 任务ID列表（与federatedTask共享）
export const taskIds = [
  'c3d4e5f6789012345678901234567890', // 水声传播特征分类任务
  'd4e5f678901234567890123456789012', // 声学传播回归分析
  'e5f67890123456789012345678901234', // 水下异常检测任务
  'f6789012345678901234567890123456', // 水声信号聚类分析
  'a1b2c3d4e5f678901234567890123456'  // 额外任务ID
]

// VM ID列表 - 32位字符串格式（与federatedTask保持一致）
export const vmIds = [
  'a1b2c3d4e5f678901234567890123456', // 水声联邦学习节点-001
  'b2c3d4e5f67890123456789012345678', // 水声联邦学习节点-002  
  'c3d4e5f67890123456789012345678901', // 水声联邦学习节点-003
  'd4e5f678901234567890123456789012', // 水声联邦学习节点-004
  'e5f67890123456789012345678901234', // 水声联邦学习节点-005
  'f6789012345678901234567890123456', // 水声联邦学习节点-006
  'a2b3c4d5e6f789012345678901234567', // 水声联邦学习节点-007
  'b3c4d5e6f78901234567890123456789', // 水声联邦学习节点-008
  'c4d5e6f7890123456789012345678901', // 水声联邦学习节点-009
  'd5e6f789012345678901234567890123'  // 水声联邦学习节点-010
]

// 模型类型选项
export const modelTypes = ['neural_network', 'cnn', 'rnn', 'transformer', 'resnet']

// 激活函数选项
export const activationFunctions = ['relu', 'sigmoid', 'tanh', 'leaky_relu', 'softmax']

// 优化器选项
export const optimizers = ['adam', 'sgd', 'rmsprop', 'adagrad', 'adamw']

// 框架选项
export const frameworks = ['pytorch', 'tensorflow', 'onnx', 'sklearn']

// 设备选项
export const devices = ['cpu', 'cuda', 'mps']

// 回滚原因选项
export const rollbackReasons = [
  '模型性能下降',
  '系统故障',
  '配置错误',
  '安全漏洞',
  '用户请求',
  '维护需要',
  '版本兼容性问题'
]

// ============= Mock数据生成函数 =============

// 生成模型架构
const generateModelArchitecture = (): ModelArchitecture => ({
  inputSize: 64 + Math.floor(Math.random() * 449), // 64-512
  hiddenLayers: [
    32 + Math.floor(Math.random() * 225), // 32-256
    16 + Math.floor(Math.random() * 113), // 16-128
    8 + Math.floor(Math.random() * 57)    // 8-64
  ],
  outputSize: 2 + Math.floor(Math.random() * 19), // 2-20
  activationFunction: randomChoice(activationFunctions),
  optimizer: randomChoice(optimizers),
  learningRate: generateRandomFloat(0.0001, 0.01, 4)
})

// 生成VM分发详情
const generateVmDistributionDetails = (vmList: string[]): VmDistributionDetail[] =>
  vmList.map(vmId => ({
    vmId,
    status: randomChoice(Object.values(VmDistributionStatus)),
    distributedAt: generateTimestamp(1),
    verificationStatus: randomChoice(Object.values(VerificationStatus)),
    checksum: generateChecksum()
  }))

// ============= 初始模型Mock数据 =============

// 生成初始模型信息
export const generateInitialModels = (count: number = 20): InitialModelInfo[] => {
  const models: InitialModelInfo[] = []
  
  // 为每个任务ID生成一个初始模型（符合API文档设计）
  taskIds.forEach((taskId, index) => {
    const modelId = `initial_model_${String(index + 1).padStart(3, '0')}`
    
    // 优化状态分配：确保有足够的可操作状态用于测试
    let status: keyof typeof InitialModelStatus
    if (index === 0) {
      status = InitialModelStatus.READY  // 第一个设为READY，可测试分发
    } else if (index === 1) {
      status = InitialModelStatus.UPLOADED  // 第二个设为UPLOADED，也可测试分发
    } else if (index === 2) {
      status = InitialModelStatus.DISTRIBUTING  // 第三个设为DISTRIBUTING，测试不可删除
    } else if (index === 3) {
      status = InitialModelStatus.DISTRIBUTED  // 第四个设为DISTRIBUTED，测试不可删除
    } else {
      // 其余的随机分配，但偏向可操作状态
      const operableStatuses = [
        InitialModelStatus.READY,
        InitialModelStatus.UPLOADED,
        InitialModelStatus.GENERATING,
        InitialModelStatus.FAILED
      ]
      const allStatuses = Object.values(InitialModelStatus)
      // 70%概率选择可操作状态，30%概率选择所有状态
      status = Math.random() < 0.7 ? 
        randomChoice(operableStatuses) : 
        randomChoice(allStatuses)
    }
    
    const isUploaded = status === InitialModelStatus.UPLOADED
    
    models.push({
      modelId,
      taskId,
      modelType: randomChoice(modelTypes),
      modelSize: 512000 + Math.floor(Math.random() * 9973760), // 0.5MB - 10MB
      parametersCount: 1000 + Math.floor(Math.random() * 99000),
      architecture: generateModelArchitecture(),
      ...(isUploaded ? {
        uploadedAt: generateTimestamp(7),
        fileName: `${modelId}.pth`,
        metadata: {
          framework: randomChoice(frameworks),
          version: `${Math.floor(Math.random() * 3) + 1}.${Math.floor(Math.random() * 10)}.${Math.floor(Math.random() * 10)}`
        }
      } : {
        generatedAt: generateTimestamp(7)
      }),
      createdAt: generateTimestamp(30),
      status,
      checksum: generateChecksum(),
      description: isUploaded ? 
        `${taskId}的上传初始模型` : 
        `${taskId}的随机生成初始模型`,
      ...(status === InitialModelStatus.DISTRIBUTED ? {
        distributionStatus: {
          totalVms: 5,
          distributedVms: 3 + Math.floor(Math.random() * 3),
          failedVms: Math.floor(Math.random() * 3),
          distributedAt: generateTimestamp(1)
        }
      } : {})
    })
  })
  
  // 生成剩余的随机模型（为不同任务）
  const remainingCount = Math.max(0, count - taskIds.length)
  for (let index = 0; index < remainingCount; index++) {
    const taskId = randomChoice(taskIds)
    const modelId = `initial_model_extra_${String(index + 1).padStart(3, '0')}`
    
    // 剩余模型也偏向可操作状态
    const operableStatuses = [
      InitialModelStatus.READY,
      InitialModelStatus.UPLOADED,
      InitialModelStatus.GENERATING,
      InitialModelStatus.FAILED
    ]
    const allStatuses = Object.values(InitialModelStatus)
    const status = Math.random() < 0.6 ? 
      randomChoice(operableStatuses) : 
      randomChoice(allStatuses)
      
    const isUploaded = status === InitialModelStatus.UPLOADED
    
    models.push({
      modelId,
      taskId,
      modelType: randomChoice(modelTypes),
      modelSize: 512000 + Math.floor(Math.random() * 9973760), // 0.5MB - 10MB
      parametersCount: 1000 + Math.floor(Math.random() * 99000),
      architecture: generateModelArchitecture(),
      ...(isUploaded ? {
        uploadedAt: generateTimestamp(7),
        fileName: `${modelId}.pth`,
        metadata: {
          framework: randomChoice(frameworks),
          version: `${Math.floor(Math.random() * 3) + 1}.${Math.floor(Math.random() * 10)}.${Math.floor(Math.random() * 10)}`
        }
      } : {
        generatedAt: generateTimestamp(7)
      }),
      createdAt: generateTimestamp(30),
      status,
      checksum: generateChecksum(),
      description: `${taskId}的额外初始模型`,
      ...(status === InitialModelStatus.DISTRIBUTED ? {
        distributionStatus: {
          totalVms: 5,
          distributedVms: 3 + Math.floor(Math.random() * 3),
          failedVms: Math.floor(Math.random() * 3),
          distributedAt: generateTimestamp(1)
        }
      } : {})
    })
  }
  
  return models
}

// 生成分发进度信息
export const generateDistributionProgress = (count: number = 10): DistributionProgress[] =>
  Array.from({ length: count }, (_, index) => {
    const distributionId = createMockDistributionId()
    const taskId = randomChoice(taskIds)
    const modelId = `initial_model_${String(Math.floor(Math.random() * 20) + 1).padStart(3, '0')}`
    const targetVms = vmIds.slice(0, 3 + Math.floor(Math.random() * 6)) // 3-8个VM
    const status = randomChoice(Object.values(DistributionStatus))
    const total = targetVms.length
    const completed = status === DistributionStatus.COMPLETED ? total : Math.floor(Math.random() * (total + 1))
    const failed = status === DistributionStatus.FAILED ? Math.floor(Math.random() * (total - completed + 1)) : 0
    const inProgress = total - completed - failed

    return {
      distributionId,
      taskId,
      modelId,
      targetVms,
      distributionMode: randomChoice(['SYNC', 'ASYNC']),
      status,
      startedAt: generateTimestamp(2),
      ...(status === DistributionStatus.COMPLETED ? {
        completedAt: generateTimestamp(1)
      } : {
        estimatedCompletion: generateTimestamp(0, 1)
      }),
      timeout: 60 + Math.floor(Math.random() * 541), // 60-600
      retryAttempts: 1 + Math.floor(Math.random() * 5),
      verifyChecksum: Math.random() > 0.5,
      notifyOnCompletion: Math.random() > 0.5,
      progress: {
        total,
        completed,
        failed,
        inProgress
      },
      vmDetails: generateVmDistributionDetails(targetVms)
    }
  })

// ============= 模型版本Mock数据 =============

// 生成模型版本信息（严格按照接口文档规范）
export const generateModelVersions = (count: number = 50): ModelVersionInfo[] =>
  Array.from({ length: count }, (_, index) => {
    const modelId = createMockModelId()
    const taskId = randomChoice(taskIds)
    const roundNumber = 1 + Math.floor(Math.random() * 100)
    const status = randomChoice(Object.values(ModelVersionStatus))
    
    return {
      modelId,
      taskId,
      roundNumber,
      aggregationMethod: randomChoice(Object.values(AggregationMethod)),
      clientCount: 3 + Math.floor(Math.random() * 18),
      modelJson: {
        layers: 3 + Math.floor(Math.random() * 8),
        parameters: 1000 + Math.floor(Math.random() * 99000),
        weights: `compressed_weights_${modelId.slice(0, 8)}`,
        architecture: {
          inputSize: 64 + Math.floor(Math.random() * 449),
          hiddenLayers: [128, 64, 32],
          outputSize: 2 + Math.floor(Math.random() * 19)
        }
      },
      metrics: {
        accuracy: generateRandomFloat(0.6, 0.99, 4),
        loss: generateRandomFloat(0.01, 0.5, 6)
      },
      status,
      createdAt: generateTimestamp(30),
      aggregatedAt: generateTimestamp(30)
    }
  })

// 生成评估结果
export const generateEvaluationResults = (count: number = 30): EvaluationResult[] =>
  Array.from({ length: count }, (_, index) => {
    const evaluationId = createMockEvaluationId()
    const modelId = createMockModelId()
    const taskId = randomChoice(taskIds)
    
    return {
      evaluationId,
      modelId,
      taskId,
      metrics: {
        accuracy: generateRandomFloat(0.6, 0.99, 4),
        loss: generateRandomFloat(0.01, 0.5, 6),
        precision: generateRandomFloat(0.6, 0.99, 4),
        recall: generateRandomFloat(0.6, 0.99, 4),
        f1: generateRandomFloat(0.6, 0.99, 4)
      },
      evaluationTime: generateRandomFloat(5.0, 300.0, 1),
      testSamples: 500 + Math.floor(Math.random() * 4501),
      status: randomChoice(['COMPLETED', 'FAILED', 'IN_PROGRESS']),
      createdAt: generateTimestamp(15),
      testDataPath: `/data/test_datasets/dataset_${1 + Math.floor(Math.random() * 10)}`,
      batchSize: randomChoice([16, 32, 64, 128]),
      device: randomChoice(devices)
    }
  })



// 生成统计信息
export const generateModelStatistics = (): ModelStatistics => {
  const days = 30
  return {
    totalModels: 50 + Math.floor(Math.random() * 151),
    averageAccuracy: generateRandomFloat(0.7, 0.95, 4),
    averageLoss: generateRandomFloat(0.05, 0.3, 6),
    uploadTrend: Array.from({ length: days }, (_, i) => ({
      date: new Date(Date.now() - (days - i - 1) * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
      count: Math.floor(Math.random() * 11)
    })),
    accuracyTrend: Array.from({ length: 20 }, (_, i) => ({
      roundNumber: i + 1,
      accuracy: generateRandomFloat(0.6 + i * 0.015, 0.65 + i * 0.015, 4)
    }))
  }
}

// 生成任务模型统计
export const generateTaskModelStatistics = (taskId: string): TaskModelStatistics => {
  const totalRounds = 50 + Math.floor(Math.random() * 151)
  const completedRounds = 30 + Math.floor(Math.random() * (totalRounds - 29))
  const bestRound = 20 + Math.floor(Math.random() * (completedRounds - 19))
  
  const taskNames = ['水声分类任务', '海洋检测任务', '声学识别任务', '水下定位任务', '环境监测任务']
  
  return {
    taskId,
    taskName: randomChoice(taskNames),
    totalRounds,
    completedRounds,
    performanceMetrics: {
      bestAccuracy: generateRandomFloat(0.85, 0.99, 4),
      bestRound,
      averageAccuracy: generateRandomFloat(0.75, 0.90, 4),
      accuracyImprovement: generateRandomFloat(0.01, 0.15, 4)
    }
  }
}

// 生成回滚记录
export const generateRollbackRecords = (count: number = 15): RollbackRecord[] =>
  Array.from({ length: count }, (_, index) => {
    const rollbackId = createMockRollbackId()
    const deploymentId = `deployment_${String(Math.floor(Math.random() * 20) + 1).padStart(3, '0')}`
    const fromModelId = `model_${String(Math.floor(Math.random() * 50) + 1).padStart(3, '0')}`
    const toModelId = `model_${String(Math.floor(Math.random() * 50) + 1).padStart(3, '0')}`
    const status = randomChoice(Object.values(RollbackStatus))
    const hasReason = Math.random() > 0.3
    const hasRollbackTime = status === RollbackStatus.COMPLETED
    
    return {
      rollbackId,
      deploymentId,
      fromModelId,
      toModelId,
      status,
      rollbackReason: hasReason ? randomChoice(rollbackReasons) : undefined,
      rollbackTime: hasRollbackTime ? Math.floor(Math.random() * 300000) + 10000 : undefined, // 10s-5min
      createdAt: generateTimestamp(Math.floor(Math.random() * 30))
    }
  })

// ============= 导出Mock数据实例 =============

export const mockInitialModels = generateInitialModels(20)
export const mockDistributionProgress = generateDistributionProgress(10)
export const mockModelVersions = generateModelVersions(50)
export const mockEvaluationResults = generateEvaluationResults(30)
export const mockRollbackRecords = generateRollbackRecords(15)
export const mockModelStatistics = generateModelStatistics()
export const mockTaskModelStatistics = taskIds.map(taskId => generateTaskModelStatistics(taskId))

// 根据任务ID获取相关数据的工具函数
export const getModelsByTaskId = (taskId: string): {
  initialModels: InitialModelInfo[]
  modelVersions: ModelVersionInfo[]
  evaluationResults: EvaluationResult[]
} => ({
  initialModels: mockInitialModels.filter(model => model.taskId === taskId),
  modelVersions: mockModelVersions.filter(model => model.taskId === taskId),
  evaluationResults: mockEvaluationResults.filter(result => result.taskId === taskId)
})

// 根据模型ID获取相关数据
export const getDataByModelId = (modelId: string): {
  modelVersion?: ModelVersionInfo
  evaluationResults: EvaluationResult[]
} => ({
  modelVersion: mockModelVersions.find(model => model.modelId === modelId),
  evaluationResults: mockEvaluationResults.filter(result => result.modelId === modelId)
})

// 分页工具函数
export const paginate = <T>(data: T[], page: number = 1, size: number = 10): {
  total: number
  pages: number
  current: number
  size: number
  records: T[]
} => {
  const total = data.length
  const pages = Math.ceil(total / size)
  const start = (page - 1) * size
  const end = start + size
  const records = data.slice(start, end)

  return {
    total,
    pages,
    current: page,
    size,
    records
  }
}

// 错误码定义
export const ErrorCodes = {
  // 初始模型相关错误码
  INITIAL_MODEL_NOT_FOUND: 'INITIAL_MODEL_NOT_FOUND',
  INITIAL_MODEL_GENERATION_FAILED: 'INITIAL_MODEL_GENERATION_FAILED',
  INITIAL_MODEL_UPLOAD_FAILED: 'INITIAL_MODEL_UPLOAD_FAILED',
  INITIAL_MODEL_INVALID_FORMAT: 'INITIAL_MODEL_INVALID_FORMAT',
  INITIAL_MODEL_ALREADY_EXISTS: 'INITIAL_MODEL_ALREADY_EXISTS',
  INITIAL_MODEL_DISTRIBUTION_FAILED: 'INITIAL_MODEL_DISTRIBUTION_FAILED',
  INITIAL_MODEL_CHECKSUM_MISMATCH: 'INITIAL_MODEL_CHECKSUM_MISMATCH',
  INITIAL_MODEL_SIZE_EXCEEDED: 'INITIAL_MODEL_SIZE_EXCEEDED',
  
  // 模型版本管理错误码
  MODEL_NOT_FOUND: 'MODEL_NOT_FOUND',
  MODEL_UPLOAD_FAILED: 'MODEL_UPLOAD_FAILED',
  MODEL_FORMAT_UNSUPPORTED: 'MODEL_FORMAT_UNSUPPORTED',
  MODEL_SIZE_EXCEEDED: 'MODEL_SIZE_EXCEEDED',
  MODEL_VALIDATION_FAILED: 'MODEL_VALIDATION_FAILED',
  MODEL_DEPLOYMENT_FAILED: 'MODEL_DEPLOYMENT_FAILED',
  MODEL_ROLLBACK_FAILED: 'MODEL_ROLLBACK_FAILED',
  MODEL_DELETE_FAILED: 'MODEL_DELETE_FAILED',
  EVALUATION_FAILED: 'EVALUATION_FAILED',
  DEPLOYMENT_NOT_FOUND: 'DEPLOYMENT_NOT_FOUND',
  DEPLOYMENT_IN_USE: 'DEPLOYMENT_IN_USE',
  
  // 权限相关错误码
  INSUFFICIENT_PERMISSION: 'INSUFFICIENT_PERMISSION',
  TASK_ACCESS_DENIED: 'TASK_ACCESS_DENIED'
} as const

export default {
  InitialModelStatus,
  DistributionStatus,
  ModelVersionStatus,
  AggregationMethod,
  VmDistributionStatus,
  VerificationStatus,
  RollbackStatus,
  ErrorCodes,
  mockInitialModels,
  mockDistributionProgress,
  mockModelVersions,
  mockEvaluationResults,
  mockRollbackRecords,
  mockModelStatistics,
  mockTaskModelStatistics,
  generateInitialModels,
  generateDistributionProgress,
  generateModelVersions,
  generateEvaluationResults,
  generateRollbackRecords,
  generateModelStatistics,
  generateTaskModelStatistics,
  getModelsByTaskId,
  getDataByModelId,
  paginate
}