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
  }
}

// ==================== 导出Mock数据 ====================

export default mockFederatedTaskApi
