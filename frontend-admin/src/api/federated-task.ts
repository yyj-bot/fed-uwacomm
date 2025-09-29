import { createApiInstance } from './base'
import type { 
  ApiResponse, 
  PaginatedResponse,
  PaginationParams,
  FederatedTaskDetails
} from '@/types'

// 创建联邦学习任务API实例
const federatedTaskApiInstance = createApiInstance('FEDERATED')

// 创建联邦学习流程编排API实例
const orchestrationApiInstance = createApiInstance('FEDERATED')

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

// 任务详情类型 - v1.3 增强版
// FederatedTaskDetails 类型定义已移至 types/index.ts

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

// ==================== v1.3 新增类型定义 ====================

// 虚拟机类型
interface AvailableVM {
  vmId: string
  name: string
  ipAddress: string
  status: 'RUNNING' | 'STOPPED' | 'PAUSED' | 'ERROR'
  connectionStatus: 'CONNECTED' | 'DISCONNECTED' | 'CONNECTING'
  osType: string
  resources: {
    cpuCores: number
    memoryMb: number
    diskGb: number
    gpuCount: number
    gpuMemoryMb: number
  }
  capabilities: string[]
  supportedAlgorithms: string[]
  currentUsage: {
    cpuUsage: number
    memoryUsage: number
    networkUsage: number
  }
  networkInfo: {
    bandwidth: number
    latency: number
    uploadSpeed: number
    downloadSpeed: number
  }
  lastHeartbeat: string
  reliability: {
    uptime: number
    avgResponseTime: number
    taskSuccessRate: number
  }
}

// 数据集类型
interface AvailableDataset {
  datasetId: string
  name: string
  description: string
  dataType: 'ACOUSTIC' | 'IMAGE' | 'TEXT' | 'NUMERICAL' | 'TIME_SERIES'
  status: 'READY' | 'PROCESSING' | 'ERROR' | 'UPLOADING'
  statistics: {
    totalRows: number
    totalColumns: number
    fileSize: number
    fileSizeFormatted: string
  }
  features: {
    featureColumns: string[]
    targetColumn: string
    numericFeatures: number
    categoricalFeatures: number
  }
  quality: {
    completeness: number
    consistency: number
    accuracy: number
    missingValues: number
    duplicates: number
    outliers: number
  }
  metadata: {
    source: string
    version: string
    sampleRate?: number
    frequency?: string
    environment?: string
  }
  tags: string[]
  uploadTime: string
  uploadedBy: string
}

// 角色配置类型 - v1.4 更新：只支持 PARTICIPANT 角色
interface RoleConfig {
  role: 'PARTICIPANT'
  name: string
  description: string  // v1.4 新增：详细描述字段，说明角色职责
  requirements: {
    minCpuCores: number
    minMemoryMb: number
    requiredCapabilities: string[]
  }
  compatibleAlgorithms: string[]
}

// 算法模板类型
interface AlgorithmTemplate {
  algorithm: string
  name: string
  description: string
  applicableTaskTypes: ('CLASSIFICATION' | 'REGRESSION' | 'CLUSTERING' | 'ANOMALY_DETECTION')[]
  defaultHyperparameters: {
    learningRate: number
    batchSize: number
    epochs: number
    rounds: number
    minParticipants: number
    aggregationMethod: string
  }
  parameterRanges: {
    learningRate: {
      min: number
      max: number
      recommended: number[]
    }
    batchSize: {
      min: number
      max: number
      recommended: number[]
    }
  }
}

// 数据分配预览类型
interface DistributionPreview {
  distributionResult: {
    participants: Array<{
      vmId: string
      vmName: string
      allocatedRatio: number
      allocatedRows: number
      estimatedTrainingTime: number
    }>
  }
  qualityMetrics: {
    iidScore: number
    balanceScore: number
  }
}

// 参与者验证类型
interface ParticipantValidation {
  overallValid: boolean
  participantValidations: Array<{
    vmId: string
    isValid: boolean
    validationResults: {
      connectivity: {
        status: 'PASS' | 'FAIL'
        message: string
      }
      resources: {
        status: 'PASS' | 'FAIL'
        message: string
      }
    }
  }>
}

// 配置状态类型
interface ConfigStatus {
  taskId: string
  configStatus: 'PENDING' | 'READY' | 'ERROR'
  configurationSteps: Array<{
    step: 'DATASET_DISTRIBUTION' | 'PARTICIPANT_VALIDATION' | 'MODEL_INITIALIZATION'
    status: 'PENDING' | 'COMPLETED' | 'FAILED'
    completedAt?: string
  }>
  participantStatuses: Array<{
    vmId: string
    configStatus: 'PENDING' | 'READY' | 'ERROR'
    dataDistributed: boolean
    modelInitialized: boolean
  }>
}

// 资源使用监控类型
interface ResourceUsage {
  taskId: string
  participantMetrics: Array<{
    vmId: string
    currentUsage: {
      cpu: number
      memory: number
      network: {
        inbound: number
        outbound: number
      }
    }
    averageUsage: {
      cpu: number
      memory: number
    }
  }>
  aggregatedMetrics: {
    totalCpuUsage: number
    totalMemoryUsage: number
    taskProgress: number
  }
}

// ==================== v1.4 新增：联邦学习流程编排类型定义 ====================

// 工作流配置类型
interface WorkflowConfig {
  autoStart: boolean
  stages: {
    initialModelGeneration: {
      enabled: boolean
      strategy: 'RANDOM_GENERATION' | 'PRETRAINED_MODEL' | 'CUSTOM'
      parameters: {
        modelType: string
        architecture: {
          inputSize: number
          hiddenLayers: number[]
          outputSize: number
        }
      }
    }
    dataDistribution: {
      enabled: boolean
      strategy: 'BALANCED' | 'RANDOM' | 'CUSTOM'
      verificationLevel: 'NONE' | 'BASIC' | 'FULL'
    }
    modelDistribution: {
      enabled: boolean
      timeout: number
      retryAttempts: number
    }
    federatedTraining: {
      maxRounds: number
      convergenceThreshold: number
      participantThreshold: number
      roundTimeout: number
    }
    modelAggregation: {
      algorithm: 'FedAvg' | 'FedProx' | 'FedNova' | 'SCAFFOLD'
      aggregationTimeout: number
      qualityThreshold: number
    }
  }
  errorHandling: {
    autoRetry: boolean
    maxRetries: number
    retryDelay: number
    fallbackStrategy: 'ABORT' | 'PARTIAL_CONTINUE' | 'RETRY_ALL'
  }
  notifications: {
    stageCompletion: boolean
    errors: boolean
    finalResult: boolean
    webhookUrl?: string
  }
}

// 调度选项类型
interface SchedulingOptions {
  priority: 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'
  maxExecutionTime: number
  resourceLimits: {
    maxMemory: string
    maxCpuCores: number
    maxBandwidth: string
  }
}

// 工作流阶段类型
interface WorkflowStage {
  name: 'INITIAL_MODEL_GENERATION' | 'DATA_DISTRIBUTION' | 'MODEL_DISTRIBUTION' | 'FEDERATED_TRAINING' | 'FINAL_AGGREGATION'
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED' | 'SKIPPED'
  startedAt?: string
  completedAt?: string
  duration?: string
  estimatedDuration?: string
  result?: {
    modelId?: string
    modelSize?: number
    checksum?: string
    distributionId?: string
    distributedVms?: number
    totalDataSize?: number
    verificationPassed?: boolean
  }
  errorInfo?: {
    errorCode: string
    errorMessage: string
    details?: Record<string, unknown>
  }
}

// 工作流进度类型
interface WorkflowProgress {
  overallProgress: number
  stageProgress: number
  currentRound?: number
  totalRounds?: number
}

// 训练轮次详情类型
interface TrainingRound {
  roundNumber: number
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED'
  startedAt?: string
  completedAt?: string
  duration?: string
  expectedCompletion?: string
  accuracy?: number
  loss?: number
  participatingVms?: number
  submittedModels?: number
  aggregationProgress?: number
}

// 编排工作流类型
interface OrchestrationWorkflow {
  orchestrationId: string
  taskId: string
  status: 'CREATED' | 'STARTED' | 'IN_PROGRESS' | 'PAUSED' | 'COMPLETED' | 'FAILED' | 'TERMINATED'
  startedAt: string
  completedAt?: string
  lastUpdated: string
  currentStage: string
  progress: WorkflowProgress
  stageDetails: WorkflowStage[]
  estimatedCompletion?: string
  workflowPlan?: {
    totalStages: number
    estimatedDuration: string
    stages: Array<{
      name: string
      status: string
      estimatedDuration: string
    }>
  }
  resourceAllocation?: {
    allocatedMemory: string
    allocatedCpuCores: number
    allocatedBandwidth: string
    participatingVms: string[]
  }
  performanceMetrics?: {
    averageRoundDuration: string
    dataTransferSpeed: string
    aggregationEfficiency: number
    resourceUtilization: {
      cpu: number
      memory: number
      network: number
    }
  }
}

// 工作流时间线事件类型
interface WorkflowTimelineEvent {
  eventId: string
  timestamp: string
  eventType: 'WORKFLOW_STARTED' | 'STAGE_STARTED' | 'STAGE_COMPLETED' | 'STAGE_FAILED' | 'TRAINING_ROUND_STARTED' | 'TRAINING_ROUND_COMPLETED' | 'ERROR_OCCURRED' | 'WORKFLOW_COMPLETED'
  stage: string
  level: 'MAJOR' | 'MINOR' | 'ERROR' | 'INFO'
  message: string
  duration?: string
  details?: Record<string, unknown>
}

// 工作流时间线类型
interface WorkflowTimeline {
  orchestrationId: string
  taskId: string
  timeline: {
    startTime: string
    endTime?: string
    totalDuration: string
    events: WorkflowTimelineEvent[]
  }
  stagesSummary: Record<string, {
    status: string
    duration: string
    events: number
    completedRounds?: number
    currentRound?: number
  }>
}

// 性能分析类型
interface WorkflowPerformanceAnalysis {
  orchestrationId: string
  analysisTimestamp: string
  overallPerformance: {
    score: number
    grade: string
    efficiency: number
    reliability: number
    scalability: number
  }
  stagePerformance: Record<string, {
    duration: string
    efficiency: number
    resourceUtilization: number
    transferSpeed?: string
    convergenceRate?: number
    participationRate?: number
  }>
  resourceMetrics: {
    cpu: {
      averageUtilization: number
      peakUtilization: number
      efficiency: number
    }
    memory: {
      averageUtilization: number
      peakUtilization: number
      efficiency: number
    }
    network: {
      averageUtilization: number
      peakBandwidth: string
      efficiency: number
    }
  }
  qualityMetrics: {
    modelAccuracy: {
      initial: number
      final: number
      improvement: number
      convergenceRounds: number
    }
    trainingStability: number
    aggregationQuality: number
  }
  recommendations: Array<{
    category: 'PERFORMANCE' | 'EFFICIENCY' | 'QUALITY' | 'SECURITY'
    priority: 'HIGH' | 'MEDIUM' | 'LOW'
    title: string
    description: string
    expectedImprovement: string
    implementation: {
      parameter: string
      currentValue: unknown
      recommendedValue: unknown
    }
  }>
  comparisons: {
    similarTasks: {
      averageDuration: string
      performanceRanking: string
      efficiencyRanking: string
    }
    historicalTrends: {
      improvementRate: number
      consistencyScore: number
    }
  }
}

// 状态快照类型
interface StateSnapshot {
  savedAt: string
  snapshotId: string
  preservedData: {
    currentModels: boolean
    trainingState: boolean
    aggregationProgress: boolean
  }
}

// ==================== 联邦学习任务管理API ====================
export const federatedTask = {
  // ==================== 3.1 任务创建接口 - v1.3 增强版 ====================
  async createTask(taskData: {
    taskName: string
    taskType: 'CLASSIFICATION' | 'REGRESSION' | 'CLUSTERING' | 'ANOMALY_DETECTION'
    description?: string
    algorithm: string
    
    // 🆕 v1.3 新增：智能数据集配置
    datasetConfig?: {
      datasetId: string
      distributionStrategy: 'BALANCED' | 'RANDOM' | 'CUSTOM'
      distributionRatios: Record<string, number>
      validationSplit: number
      testSplit: number
    }
    
    // 🆕 v1.3 新增：智能参与者配置
    participantConfig?: {
      selectionMode: 'MANUAL' | 'AUTOMATIC'
      requirements?: {
        minParticipants: number
        maxParticipants: number
        minCpuCores: number
        minMemoryMb: number
      }
      participants: Array<{
        vmId: string
        role: 'PARTICIPANT'
        dataRatio: number
        capabilities?: string[]
        constraints?: {
          maxCpuUsage?: number
          maxMemoryUsage?: number
        }
      }>
    }
    
    // 兼容旧格式 - 将在 v2.0 中删除
    /** @deprecated 使用 participantConfig 代替 */
    participants?: Array<{
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
    
    // 🆕 v1.3 新增：配置摘要
    configSummary?: {
      dataset?: {
        datasetId: string
        totalRows: number
        distributionStrategy: string
      }
      participants: Array<{
        vmId: string
        vmName: string
        role: string
        dataRatio: number
      }>
    }
    
    // 🆕 v1.3 新增：预计性能指标
    performanceEstimation?: {
      expectedAccuracy: number
      convergenceRounds: number
      networkTraffic: string
    }
  }> {
    const response = await federatedTaskApiInstance.post<ApiResponse<{
      taskId: string
      taskName: string
      status: string
      createdAt: string
      createdBy: string
      participantCount: number
      estimatedDuration: number
      configSummary?: {
        dataset?: {
          datasetId: string
          totalRows: number
          distributionStrategy: string
        }
        participants: Array<{
          vmId: string
          vmName: string
          role: string
          dataRatio: number
        }>
      }
      performanceEstimation?: {
        expectedAccuracy: number
        convergenceRounds: number
        networkTraffic: string
      }
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

  // ==================== v1.3 新增接口组 ====================
  
  // ==================== 预配置接口组 ====================
  
  /**
   * 获取可用虚拟机列表
   */
  async getAvailableVMs(params: {
    algorithm?: string
    minCpuCores?: number
    minMemoryMb?: number
    status?: string
    capabilities?: string
  } = {}): Promise<{
    total: number
    availableVms: AvailableVM[]
  }> {
    const response = await federatedTaskApiInstance.get<ApiResponse<{
      total: number
      availableVms: AvailableVM[]
    }>>('/config/available-vms', { params })
    return response.data.data
  },

  /**
   * 获取可用数据集列表
   */
  async getAvailableDatasets(params: {
    dataType?: string
    status?: string
    minSize?: number
    maxSize?: number
    keyword?: string
  } = {}): Promise<{
    total: number
    availableDatasets: AvailableDataset[]
  }> {
    const response = await federatedTaskApiInstance.get<ApiResponse<{
      total: number
      availableDatasets: AvailableDataset[]
    }>>('/config/available-datasets', { params })
    return response.data.data
  },

  /**
   * 获取角色配置选项
   */
  async getRoleConfigs(): Promise<{
    roles: RoleConfig[]
  }> {
    const response = await federatedTaskApiInstance.get<ApiResponse<{
      roles: RoleConfig[]
    }>>('/config/roles')
    return response.data.data
  },

  /**
   * 获取算法配置模板
   */
  async getAlgorithmTemplates(): Promise<{
    templates: AlgorithmTemplate[]
  }> {
    const response = await federatedTaskApiInstance.get<ApiResponse<{
      templates: AlgorithmTemplate[]
    }>>('/config/algorithm-templates')
    return response.data.data
  },

  // ==================== 智能配置接口组 ====================
  
  /**
   * 数据分配预览
   */
  async previewDataDistribution(data: {
    datasetId: string
    distributionStrategy: 'BALANCED' | 'RANDOM' | 'CUSTOM'
    participants: Array<{
      vmId: string
      requestedRatio: number
    }>
  }): Promise<DistributionPreview> {
    const response = await federatedTaskApiInstance.post<ApiResponse<DistributionPreview>>('/tasks/preview-distribution', data)
    return response.data.data
  },

  /**
   * 参与者验证
   */
  async validateParticipants(data: {
    algorithm: string
    taskType: 'CLASSIFICATION' | 'REGRESSION' | 'CLUSTERING' | 'ANOMALY_DETECTION'
    participants: Array<{
      vmId: string
      role: 'PARTICIPANT'
    }>
  }): Promise<ParticipantValidation> {
    const response = await federatedTaskApiInstance.post<ApiResponse<ParticipantValidation>>('/tasks/validate-participants', data)
    return response.data.data
  },

  // ==================== 增强监控接口组 ====================
  
  /**
   * 获取配置状态监控
   */
  async getConfigStatus(taskId: string): Promise<ConfigStatus> {
    const response = await federatedTaskApiInstance.get<ApiResponse<ConfigStatus>>(`/tasks/${taskId}/config-status`)
    return response.data.data
  },

  /**
   * 获取资源使用监控
   */
  async getResourceUsage(taskId: string): Promise<ResourceUsage> {
    const response = await federatedTaskApiInstance.get<ApiResponse<ResourceUsage>>(`/tasks/${taskId}/resource-usage`)
    return response.data.data
  },

  // ==================== v1.4 新增：联邦学习流程编排接口组 ====================
  
  /**
   * 2.1 启动联邦学习流程
   * 启动完整的端到端联邦学习工作流
   */
  async startOrchestration(orchestrationData: {
    taskId: string
    workflowConfig: WorkflowConfig
    schedulingOptions?: SchedulingOptions
  }): Promise<{
    orchestrationId: string
    taskId: string
    status: string
    startedAt: string
    estimatedCompletion: string
    currentStage: string
    workflowPlan: {
      totalStages: number
      estimatedDuration: string
      stages: Array<{
        name: string
        status: string
        estimatedDuration: string
      }>
    }
    resourceAllocation: {
      allocatedMemory: string
      allocatedCpuCores: number
      allocatedBandwidth: string
      participatingVms: string[]
    }
  }> {
    const response = await orchestrationApiInstance.post<ApiResponse<{
      orchestrationId: string
      taskId: string
      status: string
      startedAt: string
      estimatedCompletion: string
      currentStage: string
      workflowPlan: {
        totalStages: number
        estimatedDuration: string
        stages: Array<{
          name: string
          status: string
          estimatedDuration: string
        }>
      }
      resourceAllocation: {
        allocatedMemory: string
        allocatedCpuCores: number
        allocatedBandwidth: string
        participatingVms: string[]
      }
    }>>('/orchestration/start', orchestrationData)
    return response.data.data
  },

  /**
   * 2.2 查询流程状态
   * 查询联邦学习流程的详细执行状态
   */
  async getOrchestrationStatus(orchestrationId: string, params: {
    includeDetails?: boolean
    includeMetrics?: boolean
    refresh?: boolean
  } = {}): Promise<OrchestrationWorkflow> {
    const response = await orchestrationApiInstance.get<ApiResponse<OrchestrationWorkflow>>(`/orchestration/${orchestrationId}/status`, { params })
    return response.data.data
  },

  /**
   * 2.3 暂停流程执行
   * 暂停正在执行的联邦学习流程
   */
  async pauseOrchestration(orchestrationId: string, pauseData?: {
    reason?: string
    pauseMode?: 'GRACEFUL' | 'IMMEDIATE'
    waitForCurrentRound?: boolean
    preserveState?: boolean
    notifyParticipants?: boolean
  }): Promise<{
    orchestrationId: string
    status: string
    pausedAt: string
    pausedStage: string
    pausedRound?: number
    reason?: string
    canResume: boolean
    stateSnapshot: StateSnapshot
  }> {
    const response = await orchestrationApiInstance.post<ApiResponse<{
      orchestrationId: string
      status: string
      pausedAt: string
      pausedStage: string
      pausedRound?: number
      reason?: string
      canResume: boolean
      stateSnapshot: StateSnapshot
    }>>(`/orchestration/${orchestrationId}/pause`, pauseData)
    return response.data.data
  },

  /**
   * 2.4 恢复流程执行
   * 恢复已暂停的联邦学习流程
   */
  async resumeOrchestration(orchestrationId: string, resumeData?: {
    resumeFromSnapshot?: boolean
    snapshotId?: string
    validateState?: boolean
    notifyParticipants?: boolean
  }): Promise<{
    orchestrationId: string
    status: string
    resumedAt: string
    resumedStage: string
    resumedRound?: number
    stateValidation: {
      passed: boolean
      modelsVerified: number
      stateConsistent: boolean
    }
    estimatedRemainingTime: string
  }> {
    const response = await orchestrationApiInstance.post<ApiResponse<{
      orchestrationId: string
      status: string
      resumedAt: string
      resumedStage: string
      resumedRound?: number
      stateValidation: {
        passed: boolean
        modelsVerified: number
        stateConsistent: boolean
      }
      estimatedRemainingTime: string
    }>>(`/orchestration/${orchestrationId}/resume`, resumeData)
    return response.data.data
  },

  /**
   * 2.5 终止流程执行
   * 终止正在执行的联邦学习流程
   */
  async terminateOrchestration(orchestrationId: string, params: {
    force?: boolean
    cleanup?: boolean
    saveResults?: boolean
  } = {}): Promise<{
    orchestrationId: string
    status: string
    terminatedAt: string
    terminatedStage: string
    terminatedRound?: number
    completedRounds: number
    partialResults: {
      bestModel: {
        roundNumber: number
        accuracy: number
        modelId: string
      }
      savedModels: number
      trainingMetrics: string
    }
    cleanup: {
      resourcesReleased: boolean
      temporaryDataCleared: boolean
      participantsNotified: boolean
    }
  }> {
    const response = await orchestrationApiInstance.delete<ApiResponse<{
      orchestrationId: string
      status: string
      terminatedAt: string
      terminatedStage: string
      terminatedRound?: number
      completedRounds: number
      partialResults: {
        bestModel: {
          roundNumber: number
          accuracy: number
          modelId: string
        }
        savedModels: number
        trainingMetrics: string
      }
      cleanup: {
        resourcesReleased: boolean
        temporaryDataCleared: boolean
        participantsNotified: boolean
      }
    }>>(`/orchestration/${orchestrationId}`, { params })
    return response.data.data
  },

  /**
   * 2.6 获取流程时间线
   * 获取联邦学习流程的详细执行时间线
   */
  async getOrchestrationTimeline(orchestrationId: string, params: {
    includeEvents?: boolean
    eventLevel?: 'ALL' | 'MAJOR' | 'ERROR'
    timeRange?: string
  } = {}): Promise<WorkflowTimeline> {
    const response = await orchestrationApiInstance.get<ApiResponse<WorkflowTimeline>>(`/workflow/${orchestrationId}/timeline`, { params })
    return response.data.data
  },

  /**
   * 2.7 获取流程列表
   * 获取用户的联邦学习流程执行历史
   */
  async getOrchestrationList(params: {
    taskId?: string
    status?: string
    page?: number
    size?: number
    sortBy?: string
    sortOrder?: 'asc' | 'desc'
  } = {}): Promise<{
    total: number
    page: number
    size: number
    items: Array<{
      orchestrationId: string
      taskId: string
      status: string
      startedAt: string
      completedAt?: string
      currentStage: string
      progress: number
      duration: string
      participatingVms: number
      completedRounds: number
      totalRounds: number
      finalAccuracy?: number
      success?: boolean
    }>
  }> {
    const response = await orchestrationApiInstance.get<ApiResponse<{
      total: number
      page: number
      size: number
      items: Array<{
        orchestrationId: string
        taskId: string
        status: string
        startedAt: string
        completedAt?: string
        currentStage: string
        progress: number
        duration: string
        participatingVms: number
        completedRounds: number
        totalRounds: number
        finalAccuracy?: number
        success?: boolean
      }>
    }>>('/orchestration', { params })
    return response.data.data
  },

  /**
   * 2.8 获取流程性能分析
   * 获取联邦学习流程的性能分析报告
   */
  async getOrchestrationAnalytics(orchestrationId: string, params: {
    includeRecommendations?: boolean
    metricsLevel?: 'BASIC' | 'DETAILED' | 'FULL'
  } = {}): Promise<WorkflowPerformanceAnalysis> {
    const response = await orchestrationApiInstance.get<ApiResponse<WorkflowPerformanceAnalysis>>(`/orchestration/${orchestrationId}/analytics`, { params })
    return response.data.data
  },

  // ==================== v1.4 新增：聚合引擎监控接口组 ====================
  
  /**
   * 3.1 聚合引擎状态查询
   * 查询UniversalAggregationEngine的运行状态
   */
  async getAggregationEngineStatus(): Promise<{
    engineStatus: 'RUNNING' | 'STOPPED' | 'ERROR' | 'MAINTENANCE'
    currentTasks: Array<{
      taskId: string
      status: 'AGGREGATING' | 'WAITING' | 'COMPLETED' | 'FAILED'
      currentRound: number
      algorithm: string
      participantCount: number
    }>
    systemMetrics: {
      cpuUsage: number
      memoryUsage: number
      diskUsage: number
    }
    aggregationMetrics: {
      totalAggregations: number
      successRate: number
      averageAggregationTime: number
    }
    supportedAlgorithms: string[]
  }> {
    const response = await federatedTaskApiInstance.get<ApiResponse<{
      engineStatus: 'RUNNING' | 'STOPPED' | 'ERROR' | 'MAINTENANCE'
      currentTasks: Array<{
        taskId: string
        status: 'AGGREGATING' | 'WAITING' | 'COMPLETED' | 'FAILED'
        currentRound: number
        algorithm: string
        participantCount: number
      }>
      systemMetrics: {
        cpuUsage: number
        memoryUsage: number
        diskUsage: number
      }
      aggregationMetrics: {
        totalAggregations: number
        successRate: number
        averageAggregationTime: number
      }
      supportedAlgorithms: string[]
    }>>('/engine/status')
    return response.data.data
  },

  /**
   * 3.2 可用聚合策略查询
   * 查询系统支持的所有聚合策略
   */
  async getAvailableStrategies(): Promise<{
    total: number
    strategies: Array<{
      algorithm: string
      name: string
      description: string
      category: 'AVERAGING' | 'PROXIMAL' | 'SCAFFOLD' | 'NOVA' | 'CUSTOM'
      supportedModelTypes: string[]
      parameters: Array<{
        name: string
        type: 'DOUBLE' | 'INTEGER' | 'BOOLEAN' | 'STRING'
        description: string
        defaultValue: unknown
        range?: {
          min: number
          max: number
        }
      }>
      requirements: {
        minParticipants: number
        maxParticipants: number
        recommendedParticipants: number
      }
    }>
  }> {
    const response = await federatedTaskApiInstance.get<ApiResponse<{
      total: number
      strategies: Array<{
        algorithm: string
        name: string
        description: string
        category: 'AVERAGING' | 'PROXIMAL' | 'SCAFFOLD' | 'NOVA' | 'CUSTOM'
        supportedModelTypes: string[]
        parameters: Array<{
          name: string
          type: 'DOUBLE' | 'INTEGER' | 'BOOLEAN' | 'STRING'
          description: string
          defaultValue: unknown
          range?: {
            min: number
            max: number
          }
        }>
        requirements: {
          minParticipants: number
          maxParticipants: number
          recommendedParticipants: number
        }
      }>
    }>>('/strategies/available')
    return response.data.data
  },
} as const

// 使用命名导出以保持一致性 

// 导出类型定义
export type {
  FederatedTask,
  FederatedTaskDetails,
  TaskResults,
  TaskLog,
  // v1.3 新增类型
  AvailableVM,
  AvailableDataset,
  RoleConfig,
  AlgorithmTemplate,
  DistributionPreview,
  ParticipantValidation,
  ConfigStatus,
  ResourceUsage,
  // v1.4 新增：联邦学习流程编排类型
  WorkflowConfig,
  SchedulingOptions,
  WorkflowStage,
  WorkflowProgress,
  TrainingRound,
  OrchestrationWorkflow,
  WorkflowTimelineEvent,
  WorkflowTimeline,
  WorkflowPerformanceAnalysis,
  StateSnapshot
}