/**
 * 任务管理 Mock 数据
 * 严格按照 API 文档规范定义的数据结构
 * 
 * @author FedUWAComm Team
 * @version 1.4.0
 */

import { baseVmList } from './shared/vm-base'
import { baseTaskList, type TaskType, type TaskStatus } from './shared/task-base'

// ==================== 数据生成工具函数 ====================

/**
 * 生成32位十六进制任务ID
 */
export const createMockTaskId = (): string => {
  return Array.from({ length: 32 }, () => 
    Math.floor(Math.random() * 16).toString(16)
  ).join('')
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

// ==================== Mock 数据定义 ====================

/**
 * 任务列表数据
 * 数据来源：baseTaskList（单一数据源）
 */
export const mockFederatedTasks = baseTaskList

// ==================== 任务详情扩展数据 ====================
// 只存储任务的扩展信息，基础信息从 baseTaskList 继承

/**
 * 任务详情扩展信息（不包含基础字段）
 * 基础字段包括：taskId, taskName, taskType, status, createdAt, startedAt, completedAt, 
 *               participantCount, currentRound, totalRounds, progress, finalAccuracy
 */
const taskDetailsExtensions: Record<string, any> = {
  'c3d4e5f6789012345678901234567890': {
    algorithm: 'FEDERATED_AVERAGING',
    participants: [
      {
        vmId: 'a1b2c3d4e5f678901234567890123456',
        role: 'PARTICIPANT',
        status: 'TRAINING',
        lastHeartbeat: generateTimestamp(0, -1),
        currentEpoch: 45,
        loss: 0.234,
        accuracy: 0.876,
        dataRatio: 0.6,
        capabilities: ['GPU', 'HIGH_MEMORY'],
        constraints: {
          maxCpuUsage: 80,
          maxMemoryUsage: 75
        }
      },
      {
        vmId: 'b2c3d4e5f67890123456789012345678',
        role: 'PARTICIPANT',
        status: 'TRAINING',
        lastHeartbeat: generateTimestamp(0, -1),
        currentEpoch: 42,
        loss: 0.256,
        accuracy: 0.854,
        dataRatio: 0.4,
        capabilities: ['TRAINING'],
        constraints: {
          maxCpuUsage: 70,
          maxMemoryUsage: 80
        }
      }
    ],
    metrics: {
      globalLoss: 0.245,
      globalAccuracy: 0.865,
      communicationRounds: 5,
      dataProcessed: 15000,
      estimatedTimeRemaining: 1800
    },
    datasetConfig: {
      datasetId: 'e5f67890123456789012345678901234',
      distributionStrategy: 'BALANCED',
      totalRows: 10000,
      qualityMetrics: {
        iidScore: 0.85,
        balanceScore: 0.92
      }
    }
  },
  'd4e5f678901234567890123456789012': {
    algorithm: 'FEDERATED_AVERAGING',
    participants: [
      {
        vmId: 'a1b2c3d4e5f678901234567890123456',
        role: 'PARTICIPANT',
        status: 'COMPLETED',
        lastHeartbeat: generateTimestamp(0, -3),
        currentEpoch: 60,
        loss: 0.089,
        accuracy: 0.912,
        dataRatio: 0.4,
        capabilities: ['GPU', 'HIGH_MEMORY'],
        constraints: {
          maxCpuUsage: 80,
          maxMemoryUsage: 75
        }
      },
      {
        vmId: 'b2c3d4e5f67890123456789012345678',
        role: 'PARTICIPANT',
        status: 'COMPLETED',
        lastHeartbeat: generateTimestamp(0, -3),
        currentEpoch: 60,
        loss: 0.094,
        accuracy: 0.898,
        dataRatio: 0.35,
        capabilities: ['TRAINING'],
        constraints: {
          maxCpuUsage: 70,
          maxMemoryUsage: 80
        }
      },
      {
        vmId: 'c3d4e5f678901234567890123456789a',
        role: 'PARTICIPANT',
        status: 'COMPLETED',
        lastHeartbeat: generateTimestamp(0, -3),
        currentEpoch: 60,
        loss: 0.098,
        accuracy: 0.885,
        dataRatio: 0.25,
        capabilities: ['TRAINING'],
        constraints: {
          maxCpuUsage: 60,
          maxMemoryUsage: 70
        }
      }
    ],
    metrics: {
      globalLoss: 0.092,
      globalAccuracy: 0.892,
      communicationRounds: 12,
      dataProcessed: 25000,
      estimatedTimeRemaining: 0
    },
    datasetConfig: {
      datasetId: 'f6789012345678901234567890123456',
      distributionStrategy: 'BALANCED',
      totalRows: 15000,
      qualityMetrics: {
        iidScore: 0.78,
        balanceScore: 0.88
      }
    }
  },
  'e5f67890123456789012345678901234': {
    algorithm: 'FEDERATED_PROXIMAL',
    participants: [
      {
        vmId: 'a1b2c3d4e5f678901234567890123456',
        role: 'PARTICIPANT',
        status: 'PAUSED',
        lastHeartbeat: generateTimestamp(0, -2),
        currentEpoch: 18,
        loss: 0.345,
        accuracy: 0.789,
        dataRatio: 0.3,
        capabilities: ['GPU', 'HIGH_MEMORY'],
        constraints: {
          maxCpuUsage: 80,
          maxMemoryUsage: 75
        }
      },
      {
        vmId: 'b2c3d4e5f67890123456789012345678',
        role: 'PARTICIPANT',
        status: 'PAUSED',
        lastHeartbeat: generateTimestamp(0, -2),
        currentEpoch: 18,
        loss: 0.356,
        accuracy: 0.765,
        dataRatio: 0.25,
        capabilities: ['TRAINING'],
        constraints: {
          maxCpuUsage: 70,
          maxMemoryUsage: 80
        }
      },
      {
        vmId: 'c3d4e5f678901234567890123456789a',
        role: 'PARTICIPANT',
        status: 'PAUSED',
        lastHeartbeat: generateTimestamp(0, -2),
        currentEpoch: 18,
        loss: 0.367,
        accuracy: 0.756,
        dataRatio: 0.25,
        capabilities: ['TRAINING'],
        constraints: {
          maxCpuUsage: 60,
          maxMemoryUsage: 70
        }
      },
      {
        vmId: 'd4e5f678901234567890123456789012',
        role: 'PARTICIPANT',
        status: 'PAUSED',
        lastHeartbeat: generateTimestamp(0, -2),
        currentEpoch: 18,
        loss: 0.372,
        accuracy: 0.743,
        dataRatio: 0.2,
        capabilities: ['TRAINING'],
        constraints: {
          maxCpuUsage: 50,
          maxMemoryUsage: 60
        }
      }
    ],
    metrics: {
      globalLoss: 0.360,
      globalAccuracy: 0.763,
      communicationRounds: 3,
      dataProcessed: 8000,
      estimatedTimeRemaining: 2700
    },
    datasetConfig: {
      datasetId: 'e5f67890123456789012345678901234',
      distributionStrategy: 'IID_ADAPTIVE',
      totalRows: 12000,
      qualityMetrics: {
        iidScore: 0.65,
        balanceScore: 0.82
      }
    }
  },
  'a1b2c3d4e5f678901234567890123456': {
    algorithm: 'FEDERATED_AVERAGING',
    participants: [
      {
        vmId: 'a1b2c3d4e5f678901234567890123456',
        role: 'PARTICIPANT',
        status: 'PENDING',
        lastHeartbeat: generateTimestamp(0, -2),
        currentEpoch: 0,
        loss: 0,
        accuracy: 0,
        dataSize: 2500
      },
      {
        vmId: 'b2c3d4e5f67890123456789012345678',
        role: 'PARTICIPANT', 
        status: 'PENDING',
        lastHeartbeat: generateTimestamp(0, -2),
        currentEpoch: 0,
        loss: 0,
        accuracy: 0,
        dataSize: 2200
      },
      {
        vmId: 'c3d4e5f6789012345678901234567890',
        role: 'PARTICIPANT',
        status: 'PENDING',
        lastHeartbeat: generateTimestamp(0, -2),
        currentEpoch: 0,
        loss: 0,
        accuracy: 0,
        dataSize: 2800
      }
    ],
    hyperparameters: {
      learningRate: 0.001,
      batchSize: 32,
      epochs: 50,
      rounds: 10,
      minParticipants: 2,
      aggregationMethod: 'WEIGHTED_AVERAGE'
    },
    datasetConfig: {
      datasetId: 'f6789012345678901234567890123456',
      distributionStrategy: 'BALANCED',
      totalRows: 7500,
      qualityMetrics: {
        iidScore: 0.75,
        balanceScore: 0.88
      }
    }
  },
  'f6789012345678901234567890123456': {
    algorithm: 'FEDERATED_AVERAGING',
    participants: [
      {
        vmId: 'a1b2c3d4e5f678901234567890123456',
        role: 'PARTICIPANT',
        status: 'READY',
        lastHeartbeat: generateTimestamp(0, -1),
        currentEpoch: 0,
        loss: 0,
        accuracy: 0,
        dataRatio: 0.25,
        capabilities: ['GPU', 'HIGH_MEMORY'],
        constraints: {
          maxCpuUsage: 80,
          maxMemoryUsage: 75
        }
      },
      {
        vmId: 'b2c3d4e5f67890123456789012345678',
        role: 'PARTICIPANT',
        status: 'READY',
        lastHeartbeat: generateTimestamp(0, -1),
        currentEpoch: 0,
        loss: 0,
        accuracy: 0,
        dataRatio: 0.2,
        capabilities: ['TRAINING'],
        constraints: {
          maxCpuUsage: 70,
          maxMemoryUsage: 80
        }
      },
      {
        vmId: 'c3d4e5f678901234567890123456789a',
        role: 'PARTICIPANT',
        status: 'READY',
        lastHeartbeat: generateTimestamp(0, -1),
        currentEpoch: 0,
        loss: 0,
        accuracy: 0,
        dataRatio: 0.2,
        capabilities: ['TRAINING'],
        constraints: {
          maxCpuUsage: 60,
          maxMemoryUsage: 70
        }
      },
      {
        vmId: 'd4e5f678901234567890123456789012',
        role: 'PARTICIPANT',
        status: 'READY',
        lastHeartbeat: generateTimestamp(0, -1),
        currentEpoch: 0,
        loss: 0,
        accuracy: 0,
        dataRatio: 0.2,
        capabilities: ['TRAINING'],
        constraints: {
          maxCpuUsage: 50,
          maxMemoryUsage: 60
        }
      },
      {
        vmId: 'e5f67890123456789012345678901234',
        role: 'PARTICIPANT',
        status: 'READY',
        lastHeartbeat: generateTimestamp(0, -1),
        currentEpoch: 0,
        loss: 0,
        accuracy: 0,
        dataRatio: 0.15,
        capabilities: ['TRAINING'],
        constraints: {
          maxCpuUsage: 40,
          maxMemoryUsage: 50
        }
      }
    ],
    metrics: {
      globalLoss: 0,
      globalAccuracy: 0,
      communicationRounds: 0,
      dataProcessed: 0,
      estimatedTimeRemaining: 3600
    },
    datasetConfig: {
      datasetId: 'f6789012345678901234567890123456',
      distributionStrategy: 'CLUSTER_BALANCED',
      totalRows: 20000,
      qualityMetrics: {
        iidScore: 0.72,
        balanceScore: 0.95
      }
    }
  },
  'b2c3d4e5f67890123456789012345678': {
    algorithm: 'FEDERATED_AVERAGING',
    participants: [
      {
        vmId: 'a1b2c3d4e5f678901234567890123456',
        role: 'PARTICIPANT',
        status: 'FAILED',
        lastHeartbeat: generateTimestamp(0, -5),
        currentEpoch: 2,
        loss: 0.85,
        accuracy: 0.42,
        dataSize: 3200
      },
      {
        vmId: 'b2c3d4e5f67890123456789012345678',
        role: 'PARTICIPANT',
        status: 'FAILED',
        lastHeartbeat: generateTimestamp(0, -5),
        currentEpoch: 2,
        loss: 0.92,
        accuracy: 0.38,
        dataSize: 2800
      }
    ],
    hyperparameters: {
      learningRate: 0.01,
      batchSize: 64,
      epochs: 100,
      rounds: 15,
      minParticipants: 3,
      aggregationMethod: 'WEIGHTED_AVERAGE'
    },
    metrics: {
      globalLoss: 0.88,
      globalAccuracy: 0.40,
      communicationRounds: 2,
      dataProcessed: 6000,
      estimatedTimeRemaining: 0
    },
    datasetConfig: {
      datasetId: 'b2c3d4e5f67890123456789012345678',
      distributionStrategy: 'NON_IID',
      totalRows: 6000,
      qualityMetrics: {
        iidScore: 0.35,
        balanceScore: 0.62
      }
    }
  },
  'c4d5e6f7890123456789012345678901': {
    algorithm: 'FEDERATED_AVERAGING',
    participants: [
      {
        vmId: 'a1b2c3d4e5f678901234567890123456',
        role: 'PARTICIPANT',
        status: 'READY',
        lastHeartbeat: generateTimestamp(0, -1),
        currentEpoch: 0,
        loss: 0,
        accuracy: 0,
        dataSize: 4000
      },
      {
        vmId: 'b2c3d4e5f67890123456789012345678',
        role: 'PARTICIPANT',
        status: 'READY',
        lastHeartbeat: generateTimestamp(0, -1),
        currentEpoch: 0,
        loss: 0,
        accuracy: 0,
        dataSize: 3500
      }
    ],
    hyperparameters: {
      learningRate: 0.005,
      batchSize: 32,
      epochs: 80,
      rounds: 8,
      minParticipants: 2,
      aggregationMethod: 'SIMPLE_AVERAGE'
    },
    metrics: {
      globalLoss: 0,
      globalAccuracy: 0,
      communicationRounds: 0,
      dataProcessed: 0,
      estimatedTimeRemaining: 2400
    },
    datasetConfig: {
      datasetId: 'c4d5e6f7890123456789012345678901',
      distributionStrategy: 'BALANCED',
      totalRows: 7500,
      qualityMetrics: {
        iidScore: 0.85,
        balanceScore: 0.92
      }
    }
  }
}

/**
 * 任务详情数据
 * 通过合并 baseTaskList 和 taskDetailsExtensions 生成
 */
export const mockFederatedTaskDetails = baseTaskList.map(task => ({
  ...task,
  ...taskDetailsExtensions[task.taskId]
}))

// 任务结果数据
export const mockTaskResults = [
  {
    taskId: 'c3d4e5f6789012345678901234567890',
    taskName: '联邦水下声呐目标识别',
    status: 'RUNNING',
    finalResults: null, // 运行中的任务还没有最终结果
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
      },
      {
        round: 3,
        accuracy: 0.830,
        loss: 0.170,
        participants: ['a1b2c3d4e5f678901234567890123456', 'b2c3d4e5f67890123456789012345678']
      },
      {
        round: 4,
        accuracy: 0.855,
        loss: 0.145,
        participants: ['a1b2c3d4e5f678901234567890123456', 'b2c3d4e5f67890123456789012345678']
      },
      {
        round: 5,
        accuracy: 0.865,
        loss: 0.135,
        participants: ['a1b2c3d4e5f678901234567890123456', 'b2c3d4e5f67890123456789012345678']
      }
    ],
    participantResults: [
      {
        vmId: 'a1b2c3d4e5f678901234567890123456',
        finalAccuracy: 0.876,
        finalLoss: 0.124,
        trainingTime: 7200,
        dataSize: 1000,
        parameters: {
          artifact: {
            format: 'pickle',
            checksum: 'sha256:abc123running...'
          }
        }
      },
      {
        vmId: 'b2c3d4e5f67890123456789012345678',
        finalAccuracy: 0.854,
        finalLoss: 0.146,
        trainingTime: 7200,
        dataSize: 1000,
        parameters: {
          artifact: {
            format: 'pickle',
            checksum: 'sha256:def456running...'
          }
        }
      }
    ],
    modelInfo: null
  },
  {
    taskId: 'd4e5f678901234567890123456789012',
    taskName: '声学传播回归分析',
    status: 'COMPLETED',
    finalResults: {
      accuracy: 0.892,
      loss: 0.092,
      precision: 0.885,
      recall: 0.890,
      f1Score: 0.887,
      rmse: 0.145,
      mae: 0.098
    },
    roundResults: [
      {
        round: 1,
        accuracy: 0.720,
        loss: 0.280,
        participants: ['a1b2c3d4e5f678901234567890123456', 'b2c3d4e5f67890123456789012345678', 'c3d4e5f678901234567890123456789a']
      },
      {
        round: 6,
        accuracy: 0.845,
        loss: 0.155,
        participants: ['a1b2c3d4e5f678901234567890123456', 'b2c3d4e5f67890123456789012345678', 'c3d4e5f678901234567890123456789a']
      },
      {
        round: 12,
        accuracy: 0.892,
        loss: 0.092,
        participants: ['a1b2c3d4e5f678901234567890123456', 'b2c3d4e5f67890123456789012345678', 'c3d4e5f678901234567890123456789a']
      }
    ],
    participantResults: [
      {
        vmId: 'a1b2c3d4e5f678901234567890123456',
        finalAccuracy: 0.912,
        finalLoss: 0.089,
        trainingTime: 9000,
        dataSize: 1500,
        parameters: {
          artifact: {
            format: 'pickle',
            checksum: 'sha256:regression_a1b2...'
          }
        }
      },
      {
        vmId: 'b2c3d4e5f67890123456789012345678',
        finalAccuracy: 0.898,
        finalLoss: 0.094,
        trainingTime: 9000,
        dataSize: 1200,
        parameters: {
          artifact: {
            format: 'pickle',
            checksum: 'sha256:regression_b2c3...'
          }
        }
      },
      {
        vmId: 'c3d4e5f678901234567890123456789a',
        finalAccuracy: 0.885,
        finalLoss: 0.098,
        trainingTime: 9000,
        dataSize: 800,
        parameters: {
          artifact: {
            format: 'pickle',
            checksum: 'sha256:regression_c3d4...'
          }
        }
      }
    ],
    modelInfo: {
      parameters: {
        artifact: {
          format: 'pickle',
          checksum: 'sha256:regression_final...'
        },
        meta: {
          version: '1.0.0',
          modelType: 'LINEAR_REGRESSION'
        }
      }
    }
  },
  {
    taskId: 'e5f67890123456789012345678901234',
    taskName: '水下异常检测任务',
    status: 'PAUSED',
    finalResults: null,
    roundResults: [
      {
        round: 1,
        accuracy: 0.680,
        loss: 0.420,
        participants: ['a1b2c3d4e5f678901234567890123456', 'b2c3d4e5f67890123456789012345678', 'c3d4e5f678901234567890123456789a', 'd4e5f678901234567890123456789012']
      },
      {
        round: 2,
        accuracy: 0.735,
        loss: 0.385,
        participants: ['a1b2c3d4e5f678901234567890123456', 'b2c3d4e5f67890123456789012345678', 'c3d4e5f678901234567890123456789a', 'd4e5f678901234567890123456789012']
      },
      {
        round: 3,
        accuracy: 0.763,
        loss: 0.360,
        participants: ['a1b2c3d4e5f678901234567890123456', 'b2c3d4e5f67890123456789012345678', 'c3d4e5f678901234567890123456789a', 'd4e5f678901234567890123456789012']
      }
    ],
    participantResults: [
      {
        vmId: 'a1b2c3d4e5f678901234567890123456',
        finalAccuracy: 0.789,
        finalLoss: 0.345,
        trainingTime: 3600,
        dataSize: 900,
        parameters: {
          artifact: {
            format: 'pickle',
            checksum: 'sha256:anomaly_a1b2...'
          }
        }
      },
      {
        vmId: 'b2c3d4e5f67890123456789012345678',
        finalAccuracy: 0.765,
        finalLoss: 0.356,
        trainingTime: 3600,
        dataSize: 750,
        parameters: {
          artifact: {
            format: 'pickle',
            checksum: 'sha256:anomaly_b2c3...'
          }
        }
      },
      {
        vmId: 'c3d4e5f678901234567890123456789a',
        finalAccuracy: 0.756,
        finalLoss: 0.367,
        trainingTime: 3600,
        dataSize: 750,
        parameters: {
          artifact: {
            format: 'pickle',
            checksum: 'sha256:anomaly_c3d4...'
          }
        }
      },
      {
        vmId: 'd4e5f678901234567890123456789012',
        finalAccuracy: 0.743,
        finalLoss: 0.372,
        trainingTime: 3600,
        dataSize: 600,
        parameters: {
          artifact: {
            format: 'pickle',
            checksum: 'sha256:anomaly_d4e5...'
          }
        }
      }
    ],
    modelInfo: null
  },
  {
    taskId: 'f6789012345678901234567890123456',
    taskName: '海洋声学聚类分析',
    status: 'CONFIGURED',
    finalResults: null,
    roundResults: [],
    participantResults: [],
    modelInfo: null
  }
]

// 任务日志数据 - 使用映射存储不同任务的日志
export const mockTaskLogs: Record<string, any[]> = {
  'c3d4e5f6789012345678901234567890': [
    {
      timestamp: '2024-01-01T10:00:00.000Z',
      level: 'INFO',
      message: '联邦水下声呐目标识别任务启动成功',
      source: 'TASK_MANAGER',
      details: {
        participants: ['a1b2c3d4e5f678901234567890123456', 'b2c3d4e5f67890123456789012345678']
      }
    },
    {
      timestamp: '2024-01-01T10:05:00.000Z',
      level: 'INFO',
      message: '开始第1轮联邦训练',
      source: 'TRAINING_COORDINATOR',
      details: {
        round: 1,
        participants: 2
      }
    },
    {
      timestamp: '2024-01-01T10:35:00.000Z',
      level: 'INFO',
      message: '第1轮训练完成',
      source: 'TRAINING_COORDINATOR',
      details: {
        round: 1,
        accuracy: 0.750,
        loss: 0.250
      }
    },
    {
      timestamp: '2024-01-01T11:35:00.000Z',
      level: 'INFO',
      message: '第5轮训练完成，当前进度33.33%',
      source: 'TRAINING_COORDINATOR',
      details: {
        round: 5,
        accuracy: 0.865,
        loss: 0.135
      }
    }
  ],
  'd4e5f678901234567890123456789012': [
    {
      timestamp: '2024-01-01T08:30:00.000Z',
      level: 'INFO',
      message: '声学传播回归分析任务启动成功',
      source: 'TASK_MANAGER',
      details: {
        participants: ['a1b2c3d4e5f678901234567890123456', 'b2c3d4e5f67890123456789012345678', 'c3d4e5f678901234567890123456789a']
      }
    },
    {
      timestamp: '2024-01-01T08:35:00.000Z',
      level: 'INFO',
      message: '开始第1轮联邦训练',
      source: 'TRAINING_COORDINATOR',
      details: {
        round: 1,
        participants: 3
      }
    },
    {
      timestamp: '2024-01-01T10:30:00.000Z',
      level: 'INFO',
      message: '第12轮训练完成',
      source: 'TRAINING_COORDINATOR',
      details: {
        round: 12,
        accuracy: 0.892,
        loss: 0.092
      }
    },
    {
      timestamp: '2024-01-01T11:00:00.000Z',
      level: 'INFO',
      message: '任务执行完成，最终准确率: 89.2%',
      source: 'TASK_MANAGER',
      details: {
        finalAccuracy: 0.892,
        totalRounds: 12,
        duration: '02:30:00'
      }
    }
  ],
  'e5f67890123456789012345678901234': [
    {
      timestamp: '2024-01-02T07:30:00.000Z',
      level: 'INFO',
      message: '水下异常检测任务启动成功',
      source: 'TASK_MANAGER',
      details: {
        participants: ['a1b2c3d4e5f678901234567890123456', 'b2c3d4e5f67890123456789012345678', 'c3d4e5f678901234567890123456789a', 'd4e5f678901234567890123456789012']
      }
    },
    {
      timestamp: '2024-01-02T07:35:00.000Z',
      level: 'INFO',
      message: '开始第1轮联邦训练',
      source: 'TRAINING_COORDINATOR',
      details: {
        round: 1,
        participants: 4
      }
    },
    {
      timestamp: '2024-01-02T08:35:00.000Z',
      level: 'INFO',
      message: '第3轮训练完成',
      source: 'TRAINING_COORDINATOR',
      details: {
        round: 3,
        accuracy: 0.763,
        loss: 0.360
      }
    },
    {
      timestamp: '2024-01-02T08:40:00.000Z',
      level: 'WARN',
      message: '用户主动暂停任务',
      source: 'TASK_MANAGER',
      details: {
        reason: '需要调整超参数',
        currentRound: 3,
        totalRounds: 12
      }
    }
  ],
  'f6789012345678901234567890123456': [
    {
      timestamp: '2024-01-03T06:00:00.000Z',
      level: 'INFO',
      message: '海洋声学聚类分析任务创建成功',
      source: 'TASK_MANAGER',
      details: {
        taskType: 'CLUSTERING',
        algorithm: 'FEDERATED_AVERAGING'
      }
    },
    {
      timestamp: '2024-01-03T06:05:00.000Z',
      level: 'INFO',
      message: '任务配置完成，等待启动',
      source: 'TASK_MANAGER',
      details: {
        participants: ['a1b2c3d4e5f678901234567890123456', 'b2c3d4e5f67890123456789012345678', 'c3d4e5f678901234567890123456789a', 'd4e5f678901234567890123456789012', 'e5f67890123456789012345678901234'],
        totalRounds: 20
      }
    }
  ]
}

// 可用水下机器人列表
// 数据来源：baseVmList（单一数据源）
// 为联邦任务添加特有字段：resources, capabilities, currentUsage, networkInfo, reliability
export const mockAvailableVMs = baseVmList.map(vm => {
  // 根据vmId确定任务特定配置
  const taskConfigs: Record<string, any> = {
    'a1b2c3d4e5f678901234567890123456': {
      gpuCount: 1,
      gpuMemoryMb: 8192,
      capabilities: ['GPU', 'HIGH_MEMORY'],
      supportedAlgorithms: ['FEDERATED_AVERAGING', 'FEDERATED_PROXIMAL'],
      currentUsage: { cpuUsage: 45.2, memoryUsage: 60.8, networkUsage: 15.3 },
      reliability: { uptime: 99.5, avgResponseTime: 150, taskSuccessRate: 0.98 }
    },
    'b2c3d4e5f67890123456789012345678': {
      gpuCount: 0,
      gpuMemoryMb: 0,
      capabilities: ['TRAINING'],
      supportedAlgorithms: ['FEDERATED_AVERAGING'],
      currentUsage: { cpuUsage: 32.1, memoryUsage: 48.5, networkUsage: 8.7 },
      reliability: { uptime: 97.8, avgResponseTime: 180, taskSuccessRate: 0.95 }
    },
    'c3d4e5f678901234567890123456789a': {
      gpuCount: 0,
      gpuMemoryMb: 0,
      capabilities: ['TRAINING'],
      supportedAlgorithms: ['FEDERATED_AVERAGING'],
      currentUsage: { cpuUsage: 0, memoryUsage: 0, networkUsage: 0 },
      reliability: { uptime: 85.2, avgResponseTime: 200, taskSuccessRate: 0.92 }
    }
  }
  
  const config = taskConfigs[vm.vmId] || taskConfigs['c3d4e5f678901234567890123456789a']
  
  return {
    vmId: vm.vmId,
    name: vm.name,
    ipAddress: vm.ipAddress,
    status: vm.status,
    connectionStatus: vm.connectionStatus,
    osType: vm.osType,
    resources: {
      cpuCores: vm.cpuCores,
      memoryMb: vm.memoryMb,
      diskGb: vm.diskGb,
      gpuCount: config.gpuCount,
      gpuMemoryMb: config.gpuMemoryMb
    },
    capabilities: config.capabilities,
    supportedAlgorithms: config.supportedAlgorithms,
    currentUsage: config.currentUsage,
    networkInfo: {
      bandwidth: vm.networkConfig?.bandwidth || 1000,
      latency: vm.networkConfig?.latency || 12,
      uploadSpeed: vm.networkConfig?.uploadSpeed / 10 || 100,
      downloadSpeed: vm.networkConfig?.downloadSpeed / 10 || 100
    },
    lastHeartbeat: vm.status === 'RUNNING' ? generateTimestamp() : generateTimestamp(0, -2),
    reliability: config.reliability
  }
})

// 可用数据集列表
export const mockAvailableDatasets = [
  {
    datasetId: 'e5f67890123456789012345678901234',
    name: '水声传播特征数据集_v1.0',
    description: '基于Bellhop模型的水声传播特征数据集，包含多种海洋环境参数',
    dataType: 'ACOUSTIC' as const,
    status: 'READY' as const,
    statistics: {
      totalRows: 10000,
      totalColumns: 128,
      fileSize: 45678901,
      fileSizeFormatted: '43.5MB'
    },
    features: {
      featureColumns: ['frequency', 'depth', 'range', 'sound_speed', 'temperature', 'salinity'],
      targetColumn: 'transmission_loss',
      numericFeatures: 120,
      categoricalFeatures: 8
    },
    quality: {
      completeness: 98.5,
      consistency: 96.2,
      accuracy: 94.8,
      missingValues: 150,
      duplicates: 23,
      outliers: 87
    },
    metadata: {
      source: 'BELLHOP_SIMULATION',
      version: '1.0.0',
      sampleRate: 44100,
      frequency: '1-10kHz',
      environment: 'SHALLOW_WATER'
    },
    tags: ['acoustic', 'bellhop', 'shallow_water', 'transmission_loss'],
    uploadTime: '2024-01-01T00:00:00.000Z',
    uploadedBy: 'admin'
  },
  {
    datasetId: 'f6789012345678901234567890123456',
    name: '深海声学数据集_v2.1',
    description: '深海环境下的声学传播数据，适用于长距离传播建模',
    dataType: 'ACOUSTIC' as const,
    status: 'READY' as const,
    statistics: {
      totalRows: 25000,
      totalColumns: 96,
      fileSize: 98765432,
      fileSizeFormatted: '94.2MB'
    },
    features: {
      featureColumns: ['frequency', 'depth', 'range', 'sound_speed', 'temperature'],
      targetColumn: 'arrival_time',
      numericFeatures: 88,
      categoricalFeatures: 8
    },
    quality: {
      completeness: 99.2,
      consistency: 97.8,
      accuracy: 96.1,
      missingValues: 200,
      duplicates: 45,
      outliers: 123
    },
    metadata: {
      source: 'FIELD_MEASUREMENT',
      version: '2.1.0',
      sampleRate: 48000,
      frequency: '0.5-5kHz',
      environment: 'DEEP_WATER'
    },
    tags: ['acoustic', 'deep_water', 'field_data', 'arrival_time'],
    uploadTime: '2024-01-02T00:00:00.000Z',
    uploadedBy: 'researcher'
  }
]

// 角色配置选项 (v1.4 只支持 PARTICIPANT 角色)
export const mockRoleConfigs = [
  {
    role: 'PARTICIPANT' as const,
    name: '参与者',
    description: '参与联邦学习训练的节点，负责本地模型训练和参数更新',
    requirements: {
      minCpuCores: 2,
      minMemoryMb: 4096,
      requiredCapabilities: ['TRAINING']
    },
    compatibleAlgorithms: ['FEDERATED_AVERAGING', 'FEDERATED_PROXIMAL', 'FEDERATED_NOVA', 'FEDERATED_SCAFFOLD']
  }
]

// 算法配置模板
export const mockAlgorithmTemplates = [
  {
    algorithm: 'FEDERATED_AVERAGING',
    name: '联邦平均算法',
    description: '经典的FedAvg算法，适用于大多数联邦学习场景',
    applicableTaskTypes: ['CLASSIFICATION', 'REGRESSION', 'CLUSTERING', 'ANOMALY_DETECTION'] as const,
    defaultHyperparameters: {
      learningRate: 0.01,
      batchSize: 32,
      epochs: 10,
      rounds: 10,
      minParticipants: 2,
      aggregationMethod: 'WEIGHTED_AVERAGE'
    },
    parameterRanges: {
      learningRate: {
        min: 0.0001,
        max: 1.0,
        recommended: [0.001, 0.01, 0.1]
      },
      batchSize: {
        min: 1,
        max: 1024,
        recommended: [16, 32, 64, 128]
      }
    }
  },
  {
    algorithm: 'FEDERATED_PROXIMAL',
    name: '联邦近端算法',
    description: 'FedProx算法，适用于非独立同分布数据的联邦学习',
    applicableTaskTypes: ['CLASSIFICATION', 'REGRESSION'] as const,
    defaultHyperparameters: {
      learningRate: 0.01,
      batchSize: 32,
      epochs: 10,
      rounds: 15,
      minParticipants: 3,
      aggregationMethod: 'PROXIMAL'
    },
    parameterRanges: {
      learningRate: {
        min: 0.0001,
        max: 0.1,
        recommended: [0.001, 0.01, 0.05]
      },
      batchSize: {
        min: 8,
        max: 256,
        recommended: [16, 32, 64]
      }
    }
  }
]

// 聚合引擎状态数据 (v1.4 新增)
export const mockAggregationEngineStatus = {
  engineStatus: 'RUNNING' as const,
  currentTasks: [
    {
      taskId: 'c3d4e5f6789012345678901234567890',
      status: 'AGGREGATING' as const,
      currentRound: 5,
      algorithm: 'FEDERATED_AVERAGING',
      participantCount: 3
    },
    {
      taskId: 'd4e5f678901234567890123456789012',
      status: 'WAITING' as const,
      currentRound: 0,
      algorithm: 'FEDERATED_PROXIMAL',
      participantCount: 2
    }
  ],
  systemMetrics: {
    cpuUsage: 45.2,
    memoryUsage: 68.5,
    diskUsage: 23.8
  },
  aggregationMetrics: {
    totalAggregations: 125,
    successRate: 0.98,
    averageAggregationTime: 2.3
  },
  supportedAlgorithms: [
    'FEDERATED_AVERAGING',
    'FEDERATED_PROXIMAL',
    'FEDERATED_NOVA',
    'FEDERATED_SCAFFOLD'
  ]
}

// 可用聚合策略数据 (v1.4 新增)
export const mockAggregationStrategies = {
  strategies: [
    {
      algorithm: 'FEDERATED_AVERAGING',
      name: '联邦平均算法',
      description: '经典的FedAvg算法，适用于大多数联邦学习场景',
      category: 'AVERAGING' as const,
      supportedModelTypes: ['RANDOM_FOREST', 'NEURAL_NETWORK'],
      parameters: [
        {
          name: 'learningRate',
          type: 'DOUBLE' as const,
          description: '学习率',
          defaultValue: 0.01,
          range: { min: 0.0001, max: 1.0 }
        },
        {
          name: 'momentum',
          type: 'DOUBLE' as const,
          description: '动量参数',
          defaultValue: 0.9,
          range: { min: 0.0, max: 1.0 }
        },
        {
          name: 'batchSize',
          type: 'INTEGER' as const,
          description: '批处理大小',
          defaultValue: 32,
          range: { min: 1, max: 1024 }
        }
      ],
      requirements: {
        minParticipants: 2,
        maxParticipants: 100,
        recommendedParticipants: 5
      }
    },
    {
      algorithm: 'FEDERATED_PROXIMAL',
      name: '联邦近端算法',
      description: 'FedProx算法，适用于非独立同分布数据的联邦学习',
      category: 'PROXIMAL' as const,
      supportedModelTypes: ['NEURAL_NETWORK'],
      parameters: [
        {
          name: 'learningRate',
          type: 'DOUBLE' as const,
          description: '学习率',
          defaultValue: 0.01,
          range: { min: 0.0001, max: 1.0 }
        },
        {
          name: 'proximalMu',
          type: 'DOUBLE' as const,
          description: '近端参数μ',
          defaultValue: 0.1,
          range: { min: 0.0, max: 10.0 }
        }
      ],
      requirements: {
        minParticipants: 3,
        maxParticipants: 50,
        recommendedParticipants: 8
      }
    }
  ],
  categories: [
    {
      category: 'AVERAGING',
      name: '平均类算法',
      description: '基于模型参数平均的联邦学习算法'
    },
    {
      category: 'PROXIMAL',
      name: '近端类算法', 
      description: '使用近端项处理数据异构性的算法'
    }
  ]
}
