/**
 * 联邦学习任务API Mock数据
 * 提供与接口文档一致的Mock数据，用于开发和测试
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import type { ApiResponse } from '@/types'
import type { 
  FederatedTask,
  FederatedTaskDetails,
  TaskResults,
  TaskLog
} from '@/api/federated-task'

// ==================== Mock数据生成工具 ====================

/**
 * 生成随机任务ID（32位十六进制）
 */
const generateTaskId = (): string => {
  return Array.from({ length: 32 }, () => 
    Math.floor(Math.random() * 16).toString(16)
  ).join('')
}

/**
 * 生成随机用户ID（32位十六进制）
 */
const generateUserId = (): string => {
  return Array.from({ length: 32 }, () => 
    Math.floor(Math.random() * 16).toString(16)
  ).join('')
}

/**
 * 生成随机虚拟机ID（32位十六进制）
 */
const generateVMId = (): string => {
  return Array.from({ length: 32 }, () => 
    Math.floor(Math.random() * 16).toString(16)
  ).join('')
}

/**
 * 生成随机时间戳
 */
const generateTimestamp = (daysAgo: number = 0): string => {
  const date = new Date()
  date.setDate(date.getDate() - daysAgo)
  return date.toISOString()
}

/**
 * 生成随机进度百分比
 */
const generateProgress = (): number => {
  return Math.round(Math.random() * 100 * 100) / 100
}

/**
 * 生成随机准确率
 */
const generateAccuracy = (): number => {
  return Math.round((0.7 + Math.random() * 0.25) * 1000) / 1000
}

/**
 * 生成随机损失值
 */
const generateLoss = (): number => {
  return Math.round((0.05 + Math.random() * 0.3) * 1000) / 1000
}

// ==================== Mock联邦学习任务数据 ====================

/**
 * Mock联邦学习任务列表
 */
export const mockFederatedTasks: FederatedTask[] = [
  {
    taskId: 'c3d4e5f6789012345678901234567890',
    taskName: '水声传播特征分类任务',
    taskType: 'CLASSIFICATION',
    status: 'RUNNING',
    createdAt: '2024-01-01T09:00:00.000Z',
    startedAt: '2024-01-01T10:00:00.000Z',
    participantCount: 2,
    currentRound: 5,
    totalRounds: 15,
    progress: 33.33,
    finalAccuracy: undefined
  },
  {
    taskId: 'd4e5f678901234567890123456789012',
    taskName: '声学传播回归分析',
    taskType: 'REGRESSION',
    status: 'COMPLETED',
    createdAt: '2024-01-01T08:00:00.000Z',
    startedAt: '2024-01-01T08:30:00.000Z',
    completedAt: '2024-01-01T11:00:00.000Z',
    participantCount: 3,
    currentRound: 10,
    totalRounds: 10,
    progress: 100,
    finalAccuracy: 0.892
  },
  {
    taskId: 'e5f6789012345678901234567890abcd',
    taskName: '水声目标异常检测',
    taskType: 'ANOMALY_DETECTION',
    status: 'PAUSED',
    createdAt: '2024-01-02T09:00:00.000Z',
    startedAt: '2024-01-02T10:00:00.000Z',
    participantCount: 4,
    currentRound: 3,
    totalRounds: 8,
    progress: 37.5,
    finalAccuracy: undefined
  }
]

/**
 * Mock联邦学习任务详情数据
 */
export const mockFederatedTaskDetails: FederatedTaskDetails = {
  taskId: 'c3d4e5f6789012345678901234567890',
  taskName: '水声传播特征分类任务',
  taskType: 'CLASSIFICATION',
  status: 'RUNNING',
  createdAt: '2024-01-01T09:00:00.000Z',
  startedAt: '2024-01-01T10:00:00.000Z',
  participantCount: 2,
  currentRound: 5,
  totalRounds: 15,
  progress: 33.33,
  finalAccuracy: undefined,
  algorithm: 'FEDERATED_AVERAGING',
  participants: [
    {
      vmId: 'a1b2c3d4e5f678901234567890123456',
      role: 'PARTICIPANT',
      status: 'TRAINING',
      lastHeartbeat: '2024-01-01T12:30:00.000Z',
      currentEpoch: 45,
      loss: 0.234,
      accuracy: 0.876,
      dataSource: 'bellhop_features_001.csv'
    },
    {
      vmId: 'b2c3d4e5f67890123456789012345678',
      role: 'PARTICIPANT',
      status: 'TRAINING',
      lastHeartbeat: '2024-01-01T12:30:00.000Z',
      currentEpoch: 42,
      loss: 0.256,
      accuracy: 0.854,
      dataSource: 'bellhop_features_002.csv'
    }
  ],
  metrics: {
    globalLoss: 0.245,
    globalAccuracy: 0.865,
    communicationRounds: 5,
    dataProcessed: 15000,
    estimatedTimeRemaining: 1800
  }
}

/**
 * Mock任务结果数据
 */
export const mockTaskResults: TaskResults = {
  taskId: 'c3d4e5f6789012345678901234567890',
  taskName: '水声传播特征分类任务',
  status: 'COMPLETED',
  finalResults: {
    accuracy: 0.892,
    loss: 0.098,
    precision: 0.885,
    recall: 0.890,
    f1Score: 0.887,
    confusionMatrix: [[45, 5], [8, 42]]
  },
  roundResults: [
    {
      round: 1,
      accuracy: 0.750,
      loss: 0.250,
      participants: ['a1b2c3d4e5f678901234567890123456', 'b2c3d4e5f67890123456789012345678']
    },
    {
      round: 2,
      accuracy: 0.800,
      loss: 0.200,
      participants: ['a1b2c3d4e5f678901234567890123456', 'b2c3d4e5f67890123456789012345678']
    }
  ],
  participantResults: [
    {
      vmId: 'a1b2c3d4e5f678901234567890123456',
      finalAccuracy: 0.889,
      finalLoss: 0.102,
      trainingTime: 14400,
      dataSize: 1000,
      parameters: {
        artifact: {
          format: 'pickle',
          checksum: 'sha256:...'
        }
      }
    },
    {
      vmId: 'b2c3d4e5f67890123456789012345678',
      finalAccuracy: 0.895,
      finalLoss: 0.094,
      trainingTime: 14400,
      dataSize: 1000,
      parameters: {
        artifact: {
          format: 'pickle',
          checksum: 'sha256:...'
        }
      }
    }
  ],
  modelInfo: {
    parameters: {
      artifact: {
        format: 'pickle',
        checksum: 'sha256:...'
      },
      meta: {
        version: '1.0.0',
        modelType: 'RANDOM_FOREST'
      }
    }
  }
}

/**
 * Mock任务日志数据
 */
export const mockTaskLogs: TaskLog[] = [
  {
    timestamp: '2024-01-01T10:00:00.000Z',
    level: 'INFO',
    message: '任务启动成功',
    source: 'TASK_MANAGER',
    details: {
      participants: ['a1b2c3d4e5f678901234567890123456', 'b2c3d4e5f67890123456789012345678']
    }
  },
  {
    timestamp: '2024-01-01T10:30:00.000Z',
    level: 'WARN',
    message: '虚拟机a1b2c3d4e5f678901234567890123456响应超时',
    source: 'TASK_MANAGER',
    details: {
      vmId: 'a1b2c3d4e5f678901234567890123456',
      timeout: 300
    }
  }
]

// ==================== Mock API响应 ====================

/**
 * 创建成功响应
 */
export const createSuccessResponse = <T>(data: T, message: string = '操作成功'): ApiResponse<T> => ({
  code: 200,
  message,
  data
})

/**
 * 创建错误响应
 */
export const createErrorResponse = <T = null>(code: number, message: string, data?: T): ApiResponse<T> => ({
  code,
  message,
  data: data || null as T
})

// ==================== Mock服务方法 ====================

/**
 * Mock联邦学习任务API服务
 */
export const mockFederatedTaskApi = {
  /**
   * Mock创建任务
   */
  createTask: (taskData: any): ApiResponse<{
    taskId: string
    taskName: string
    status: string
    createdAt: string
    createdBy: string
    participantCount: number
    estimatedDuration: number
  }> => {
    // 参数验证
    if (!taskData.taskName || taskData.taskName.trim().length === 0) {
      return createErrorResponse(400, '任务名称不能为空', {
        errors: [{
          field: 'taskName',
          message: '任务名称不能为空'
        }]
      } as any)
    }

    if (!taskData.taskType) {
      return createErrorResponse(400, '任务类型不能为空', {
        errors: [{
          field: 'taskType',
          message: '任务类型不能为空'
        }]
      } as any)
    }

    const validTaskTypes = ['CLASSIFICATION', 'REGRESSION', 'CLUSTERING', 'ANOMALY_DETECTION']
    if (!validTaskTypes.includes(taskData.taskType)) {
      return createErrorResponse(400, '任务类型无效', {
        errors: [{
          field: 'taskType',
          message: '任务类型无效'
        }]
      } as any)
    }

    if (!taskData.participants || !Array.isArray(taskData.participants) || taskData.participants.length < 2) {
      return createErrorResponse(400, '参与者数量至少为2个', {
        errors: [{
          field: 'participants',
          message: '参与者数量至少为2个'
        }]
      } as any)
    }

    const newTaskId = generateTaskId()
    const createdBy = generateUserId()

    return createSuccessResponse({
      taskId: newTaskId,
      taskName: taskData.taskName,
      status: 'CREATED',
      createdAt: generateTimestamp(),
      createdBy,
      participantCount: taskData.participants.length,
      estimatedDuration: 3600
    }, '任务创建成功')
  },

  /**
   * Mock配置任务
   */
  configTask: (taskId: string, config: any): ApiResponse<{
    taskId: string
    status: string
    updatedAt: string
    configVersion: string
  }> => {
    if (!mockFederatedTasks.some(task => task.taskId === taskId)) {
      return createErrorResponse(404, '任务不存在', null as any)
    }

    return createSuccessResponse({
      taskId,
      status: 'CONFIGURED',
      updatedAt: generateTimestamp(),
      configVersion: 'v1.1'
    }, '任务配置成功')
  },

  /**
   * Mock启动任务
   */
  startTask: (taskId: string): ApiResponse<{
    taskId: string
    status: string
    startedAt: string
    currentRound: number
    participants: Array<{
      vmId: string
      status: string
      dataSource: string
    }>
  }> => {
    if (!mockFederatedTasks.some(task => task.taskId === taskId)) {
      return createErrorResponse(404, '任务不存在', null as any)
    }

    return createSuccessResponse({
      taskId,
      status: 'RUNNING',
      startedAt: generateTimestamp(),
      currentRound: 0,
      participants: [
        {
          vmId: 'a1b2c3d4e5f678901234567890123456',
          status: 'CONNECTED',
          dataSource: 'bellhop_features_001.csv'
        },
        {
          vmId: 'b2c3d4e5f67890123456789012345678',
          status: 'CONNECTED',
          dataSource: 'bellhop_features_002.csv'
        }
      ]
    }, '任务启动成功')
  },

  /**
   * Mock暂停任务
   */
  pauseTask: (taskId: string): ApiResponse<{
    taskId: string
    status: string
    pausedAt: string
    currentRound: number
    resumePoint: {
      round: number
      step: string
    }
  }> => {
    if (!mockFederatedTasks.some(task => task.taskId === taskId)) {
      return createErrorResponse(404, '任务不存在', null as any)
    }

    return createSuccessResponse({
      taskId,
      status: 'PAUSED',
      pausedAt: generateTimestamp(),
      currentRound: 5,
      resumePoint: {
        round: 5,
        step: 'AGGREGATION'
      }
    }, '任务暂停成功')
  },

  /**
   * Mock恢复任务
   */
  resumeTask: (taskId: string): ApiResponse<{
    taskId: string
    status: string
    resumedAt: string
    currentRound: number
  }> => {
    if (!mockFederatedTasks.some(task => task.taskId === taskId)) {
      return createErrorResponse(404, '任务不存在', null as any)
    }

    return createSuccessResponse({
      taskId,
      status: 'RUNNING',
      resumedAt: generateTimestamp(),
      currentRound: 5
    }, '任务恢复成功')
  },

  /**
   * Mock停止任务
   */
  stopTask: (taskId: string, stopData?: any): ApiResponse<{
    taskId: string
    status: string
    stoppedAt: string
    finalRound: number
    checkpointSaved: boolean
    checkpointPath?: string
  }> => {
    if (!mockFederatedTasks.some(task => task.taskId === taskId)) {
      return createErrorResponse(404, '任务不存在', null as any)
    }

    const saveCheckpoint = stopData?.saveCheckpoint !== false
    
    return createSuccessResponse({
      taskId,
      status: 'STOPPED',
      stoppedAt: generateTimestamp(),
      finalRound: 8,
      checkpointSaved: saveCheckpoint,
      checkpointPath: saveCheckpoint ? `/checkpoints/task_${taskId}_round_8.pkl` : undefined
    }, '任务停止成功')
  },

  /**
   * Mock取消任务
   */
  cancelTask: (taskId: string, cancelData?: any): ApiResponse<{
    taskId: string
    status: string
    cancelledAt: string
    reason?: string
  }> => {
    if (!mockFederatedTasks.some(task => task.taskId === taskId)) {
      return createErrorResponse(404, '任务不存在', null as any)
    }

    return createSuccessResponse({
      taskId,
      status: 'CANCELLED',
      cancelledAt: generateTimestamp(),
      reason: cancelData?.reason || '用户取消'
    }, '任务取消成功')
  },

  /**
   * Mock获取任务详情
   */
  getTaskDetail: (taskId: string): ApiResponse<FederatedTaskDetails> => {
    if (!mockFederatedTasks.some(task => task.taskId === taskId)) {
      return createErrorResponse<FederatedTaskDetails>(404, '任务不存在')
    }

    return createSuccessResponse(mockFederatedTaskDetails, '查询成功')
  },

  /**
   * Mock获取任务列表
   */
  getTaskList: (params: {
    page?: number
    size?: number
    status?: string
    type?: string
    startDate?: string
    endDate?: string
    keyword?: string
  } = {}): ApiResponse<{
    total: number
    page: number
    size: number
    tasks: FederatedTask[]
  }> => {
    const { page = 1, size = 20, status, type, keyword } = params
    
    let filteredTasks = [...mockFederatedTasks]
    
    // 应用过滤条件
    if (status) {
      filteredTasks = filteredTasks.filter(task => task.status === status)
    }
    if (type) {
      filteredTasks = filteredTasks.filter(task => task.taskType === type)
    }
    if (keyword) {
      filteredTasks = filteredTasks.filter(task => 
        task.taskName.toLowerCase().includes(keyword.toLowerCase())
      )
    }
    
    // 分页处理
    const total = filteredTasks.length
    const startIndex = (page - 1) * size
    const endIndex = startIndex + size
    const tasks = filteredTasks.slice(startIndex, endIndex)
    
    return createSuccessResponse({
      total,
      page,
      size,
      tasks
    }, '查询成功')
  },

  /**
   * Mock获取任务结果
   */
  getTaskResults: (taskId: string): ApiResponse<TaskResults> => {
    if (!mockFederatedTasks.some(task => task.taskId === taskId)) {
      return createErrorResponse<TaskResults>(404, '任务不存在')
    }

    return createSuccessResponse(mockTaskResults, '查询成功')
  },

  /**
   * Mock获取任务日志
   */
  getTaskLogs: (taskId: string, params: {
    level?: string
    startTime?: string
    endTime?: string
    keyword?: string
    page?: number
    size?: number
  } = {}): ApiResponse<{
    taskId: string
    total: number
    page: number
    size: number
    logs: TaskLog[]
  }> => {
    if (!mockFederatedTasks.some(task => task.taskId === taskId)) {
      return createErrorResponse(404, '任务不存在', null as any)
    }

    const { page = 1, size = 10, level, keyword } = params
    
    let filteredLogs = [...mockTaskLogs]
    
    // 应用过滤条件
    if (level) {
      filteredLogs = filteredLogs.filter(log => log.level === level)
    }
    if (keyword) {
      filteredLogs = filteredLogs.filter(log => 
        log.message.toLowerCase().includes(keyword.toLowerCase())
      )
    }
    
    // 分页处理
    const total = filteredLogs.length
    const startIndex = (page - 1) * size
    const endIndex = startIndex + size
    const logs = filteredLogs.slice(startIndex, endIndex)
    
    return createSuccessResponse({
      taskId,
      total,
      page,
      size,
      logs
    }, '查询成功')
  },

  /**
   * Mock删除任务
   */
  deleteTask: (taskId: string, deleteOptions?: any): ApiResponse<{
    taskId: string
    deletedAt: string
    dataDeleted: boolean
    modelPreserved: boolean
  }> => {
    if (!mockFederatedTasks.some(task => task.taskId === taskId)) {
      return createErrorResponse(404, '任务不存在', null as any)
    }

    const deleteData = deleteOptions?.deleteData !== false
    const deleteModel = deleteOptions?.deleteModel === true

    return createSuccessResponse({
      taskId,
      deletedAt: generateTimestamp(),
      dataDeleted: deleteData,
      modelPreserved: !deleteModel
    }, '任务删除成功')
  },

  /**
   * Mock获取角色配置选项 - v1.4版本：只支持PARTICIPANT角色
   */
  getRoleConfigs: (): ApiResponse<{
    roles: Array<{
      role: 'PARTICIPANT'
      name: string
      description: string
      requirements: {
        minCpuCores: number
        minMemoryMb: number
        requiredCapabilities: string[]
      }
      compatibleAlgorithms: string[]
    }>
  }> => {
    return createSuccessResponse({
      roles: [
        {
          role: 'PARTICIPANT',
          name: '参与者',
          description: '参与联邦学习训练的客户端节点，所有虚拟机均为参与者角色，聚合由后端服务统一处理',
          requirements: {
            minCpuCores: 2,
            minMemoryMb: 4096,
            requiredCapabilities: ['PYTHON', 'MACHINE_LEARNING']
          },
          compatibleAlgorithms: ['FEDERATED_AVERAGING', 'FEDERATED_SGD', 'FEDPROX']
        }
      ]
    }, '查询角色配置成功')
  },

  // ==================== v1.4 新增：联邦学习流程编排Mock接口 ====================

  /**
   * 启动联邦学习流程Mock
   */
  startOrchestration: (orchestrationData: {
    taskId: string
    workflowConfig: any
    schedulingOptions?: any
  }): ApiResponse<{
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
  }> => {
    const orchestrationId = generateTaskId()
    const estimatedHours = Math.floor(Math.random() * 5) + 1
    
    return createSuccessResponse({
      orchestrationId,
      taskId: orchestrationData.taskId,
      status: 'INITIALIZING',
      startedAt: generateTimestamp(),
      estimatedCompletion: generateTimestamp(-estimatedHours),
      currentStage: '初始模型生成',
      workflowPlan: {
        totalStages: 5,
        estimatedDuration: `${estimatedHours}小时`,
        stages: [
          { name: '初始模型生成', status: 'IN_PROGRESS', estimatedDuration: '30分钟' },
          { name: '数据分发', status: 'PENDING', estimatedDuration: '15分钟' },
          { name: '模型分发', status: 'PENDING', estimatedDuration: '10分钟' },
          { name: '联邦训练', status: 'PENDING', estimatedDuration: `${estimatedHours - 1}小时` },
          { name: '模型聚合与评估', status: 'PENDING', estimatedDuration: '15分钟' }
        ]
      },
      resourceAllocation: {
        allocatedMemory: '16GB',
        allocatedCpuCores: 8,
        allocatedBandwidth: '1Gbps',
        participatingVms: Array.from({ length: 3 }, () => generateVMId())
      }
    }, '联邦学习流程启动成功')
  },

  /**
   * 查询流程状态Mock
   */
  getOrchestrationStatus: (orchestrationId: string, params?: {
    includeDetails?: boolean
    includeMetrics?: boolean
    refresh?: boolean
  }): ApiResponse<any> => {
    const statuses = ['INITIALIZING', 'RUNNING', 'PAUSED', 'COMPLETED', 'FAILED']
    const stages = ['初始模型生成', '数据分发', '模型分发', '联邦训练', '模型聚合与评估']
    const currentStage = stages[Math.floor(Math.random() * stages.length)]
    const status = statuses[Math.floor(Math.random() * statuses.length)]
    const progress = status === 'COMPLETED' ? 100 : Math.floor(Math.random() * 90) + 10
    
    return createSuccessResponse({
      orchestrationId,
      taskId: generateTaskId(),
      status,
      startedAt: generateTimestamp(1),
      completedAt: status === 'COMPLETED' ? generateTimestamp() : undefined,
      currentStage,
      progress,
      workflow: {
        currentRound: Math.floor(Math.random() * 10) + 1,
        totalRounds: 20,
        participatingVms: 3,
        avgAccuracy: status === 'COMPLETED' ? 0.95 : 0.78 + Math.random() * 0.15
      },
      stages: [
        { 
          name: '初始模型生成', 
          status: 'COMPLETED', 
          startedAt: generateTimestamp(1), 
          completedAt: generateTimestamp(1),
          duration: '25分钟'
        },
        { 
          name: '数据分发', 
          status: progress > 20 ? 'COMPLETED' : 'IN_PROGRESS', 
          startedAt: generateTimestamp(1), 
          completedAt: progress > 20 ? generateTimestamp(1) : undefined,
          duration: progress > 20 ? '12分钟' : undefined
        },
        { 
          name: '模型分发', 
          status: progress > 40 ? 'COMPLETED' : progress > 20 ? 'IN_PROGRESS' : 'PENDING', 
          startedAt: progress > 20 ? generateTimestamp(1) : undefined, 
          completedAt: progress > 40 ? generateTimestamp(1) : undefined,
          duration: progress > 40 ? '8分钟' : undefined
        },
        { 
          name: '联邦训练', 
          status: progress > 80 ? 'COMPLETED' : progress > 40 ? 'IN_PROGRESS' : 'PENDING', 
          startedAt: progress > 40 ? generateTimestamp(1) : undefined, 
          completedAt: progress > 80 ? generateTimestamp() : undefined,
          duration: progress > 80 ? '2小时35分钟' : undefined
        },
        { 
          name: '模型聚合与评估', 
          status: status === 'COMPLETED' ? 'COMPLETED' : progress > 80 ? 'IN_PROGRESS' : 'PENDING', 
          startedAt: progress > 80 ? generateTimestamp() : undefined, 
          completedAt: status === 'COMPLETED' ? generateTimestamp() : undefined,
          duration: status === 'COMPLETED' ? '18分钟' : undefined
        }
      ],
      metrics: params?.includeMetrics ? {
        resourceUtilization: {
          avgCpuUsage: Math.floor(Math.random() * 30) + 60,
          avgMemoryUsage: Math.floor(Math.random() * 20) + 70,
          networkThroughput: `${Math.floor(Math.random() * 300) + 200}MB/s`
        },
        performance: {
          trainingAccuracy: 0.78 + Math.random() * 0.17,
          validationAccuracy: 0.75 + Math.random() * 0.18,
          loss: Math.random() * 0.5 + 0.1,
          convergenceRate: Math.random() * 0.1 + 0.02
        }
      } : undefined
    }, '查询流程状态成功')
  },

  /**
   * 暂停流程执行Mock
   */
  pauseOrchestration: (orchestrationId: string, pauseData?: any): ApiResponse<any> => {
    return createSuccessResponse({
      orchestrationId,
      status: 'PAUSED',
      pausedAt: generateTimestamp(),
      pausedStage: '联邦训练',
      pausedRound: Math.floor(Math.random() * 15) + 5,
      reason: pauseData?.reason || '用户主动暂停',
      canResume: true,
      stateSnapshot: {
        snapshotId: generateTaskId(),
        createdAt: generateTimestamp(),
        modelVersions: {
          globalModel: 'v1.5.3',
          participantModels: ['v1.5.1', 'v1.5.2', 'v1.5.3']
        },
        trainingProgress: Math.floor(Math.random() * 40) + 40,
        participantStates: {
          vm1: { status: 'READY', lastUpdate: generateTimestamp() },
          vm2: { status: 'READY', lastUpdate: generateTimestamp() },
          vm3: { status: 'READY', lastUpdate: generateTimestamp() }
        }
      }
    }, '流程暂停成功')
  },

  /**
   * 恢复流程执行Mock
   */
  resumeOrchestration: (orchestrationId: string, resumeData?: any): ApiResponse<any> => {
    return createSuccessResponse({
      orchestrationId,
      status: 'RUNNING',
      resumedAt: generateTimestamp(),
      resumedStage: '联邦训练',
      resumedRound: Math.floor(Math.random() * 15) + 5,
      stateValidation: {
        passed: true,
        modelsVerified: 3,
        stateConsistent: true
      },
      estimatedRemainingTime: `${Math.floor(Math.random() * 2) + 1}小时${Math.floor(Math.random() * 60)}分钟`
    }, '流程恢复成功')
  },

  /**
   * 终止流程执行Mock
   */
  terminateOrchestration: (orchestrationId: string, params?: any): ApiResponse<any> => {
    const completedRounds = Math.floor(Math.random() * 15) + 5
    
    return createSuccessResponse({
      orchestrationId,
      status: 'TERMINATED',
      terminatedAt: generateTimestamp(),
      terminatedStage: '联邦训练',
      terminatedRound: completedRounds,
      completedRounds,
      partialResults: {
        bestModel: {
          roundNumber: completedRounds - 1,
          accuracy: 0.75 + Math.random() * 0.15,
          modelId: generateTaskId()
        },
        savedModels: completedRounds,
        trainingMetrics: `metrics_${orchestrationId}.json`
      },
      cleanup: {
        resourcesReleased: true,
        temporaryDataCleared: params?.cleanup !== false,
        participantsNotified: true
      }
    }, '流程终止成功')
  },

  /**
   * 获取流程时间线Mock
   */
  getOrchestrationTimeline: (orchestrationId: string, params?: any): ApiResponse<any> => {
    const events = [
      { type: 'STAGE_START', stage: '初始模型生成', timestamp: generateTimestamp(1), level: 'MAJOR' },
      { type: 'MODEL_GENERATED', stage: '初始模型生成', timestamp: generateTimestamp(1), level: 'MAJOR' },
      { type: 'STAGE_COMPLETE', stage: '初始模型生成', timestamp: generateTimestamp(1), level: 'MAJOR' },
      { type: 'STAGE_START', stage: '数据分发', timestamp: generateTimestamp(1), level: 'MAJOR' },
      { type: 'DATA_DISTRIBUTED', stage: '数据分发', timestamp: generateTimestamp(1), level: 'MAJOR' },
      { type: 'STAGE_COMPLETE', stage: '数据分发', timestamp: generateTimestamp(1), level: 'MAJOR' },
      { type: 'STAGE_START', stage: '联邦训练', timestamp: generateTimestamp(), level: 'MAJOR' },
      { type: 'ROUND_START', stage: '联邦训练', round: 1, timestamp: generateTimestamp(), level: 'ALL' },
      { type: 'ROUND_COMPLETE', stage: '联邦训练', round: 1, accuracy: 0.65, timestamp: generateTimestamp(), level: 'ALL' }
    ]

    return createSuccessResponse({
      orchestrationId,
      timeline: {
        totalEvents: events.length,
        events: params?.includeEvents ? events : [],
        summary: {
          duration: '2小时15分钟',
          completedStages: 3,
          totalStages: 5,
          completedRounds: Math.floor(Math.random() * 10) + 5,
          currentAccuracy: 0.78 + Math.random() * 0.12
        }
      }
    }, '获取流程时间线成功')
  },

  /**
   * 获取流程列表Mock
   */
  getOrchestrationList: (params?: any): ApiResponse<any> => {
    const page = params?.page || 1
    const size = params?.size || 20
    const total = 45
    
    const orchestrations = Array.from({ length: Math.min(size, total) }, (_, i) => ({
      orchestrationId: generateTaskId(),
      taskId: generateTaskId(),
      status: ['RUNNING', 'COMPLETED', 'PAUSED', 'FAILED'][Math.floor(Math.random() * 4)],
      startedAt: generateTimestamp(Math.floor(Math.random() * 7)),
      completedAt: Math.random() > 0.5 ? generateTimestamp() : undefined,
      currentStage: ['初始模型生成', '数据分发', '联邦训练', '模型聚合'][Math.floor(Math.random() * 4)],
      progress: Math.floor(Math.random() * 100),
      duration: `${Math.floor(Math.random() * 4) + 1}小时${Math.floor(Math.random() * 60)}分钟`,
      participatingVms: Math.floor(Math.random() * 5) + 2,
      completedRounds: Math.floor(Math.random() * 20) + 1,
      totalRounds: 20,
      finalAccuracy: Math.random() > 0.5 ? 0.75 + Math.random() * 0.2 : undefined,
      success: Math.random() > 0.3
    }))

    return createSuccessResponse({
      total,
      page,
      size,
      items: orchestrations
    }, '获取流程列表成功')
  },

  /**
   * 获取流程性能分析Mock
   */
  getOrchestrationAnalytics: (orchestrationId: string, params?: any): ApiResponse<any> => {
    return createSuccessResponse({
      orchestrationId,
      performanceMetrics: {
        overall: {
          totalDuration: '3小时25分钟',
          efficiency: 0.85,
          resourceUtilization: 0.78,
          cost: 125.50
        },
        stages: [
          { 
            name: '初始模型生成', 
            duration: '25分钟', 
            efficiency: 0.92, 
            resourceUsage: { cpu: 65, memory: 70, network: 20 } 
          },
          { 
            name: '数据分发', 
            duration: '12分钟', 
            efficiency: 0.88, 
            resourceUsage: { cpu: 30, memory: 45, network: 85 } 
          },
          { 
            name: '联邦训练', 
            duration: '2小时35分钟', 
            efficiency: 0.82, 
            resourceUsage: { cpu: 85, memory: 80, network: 65 } 
          }
        ],
        training: {
          convergenceRate: 0.075,
          finalAccuracy: 0.91,
          averageRoundTime: '8分钟',
          participantStability: 0.95
        }
      },
      bottlenecks: [
        { stage: '联邦训练', issue: '网络延迟', impact: 'MEDIUM', suggestion: '考虑增加带宽或优化数据传输' },
        { stage: '模型聚合', issue: 'CPU利用率', impact: 'LOW', suggestion: '可以适当增加CPU核心数' }
      ],
      recommendations: params?.includeRecommendations ? [
        '建议在下次训练中增加参与者数量以提高模型精度',
        '考虑使用更高效的聚合算法来减少训练时间',
        '建议优化数据预处理流程以提高整体效率'
      ] : undefined
    }, '获取流程性能分析成功')
  }
}

// ==================== 导出Mock数据 ====================

export default mockFederatedTaskApi
