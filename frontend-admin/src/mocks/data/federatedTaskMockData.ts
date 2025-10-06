/**
 * 联邦学习任务管理 Mock 数据
 * 严格按照 API 文档规范定义的数据结构
 * 
 * @author FedUWAComm Team
 * @version 1.4.0
 */

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
 * 生成编排任务ID
 */
export const createMockOrchestrationId = (): string => {
  return `orch_${Array.from({ length: 8 }, () => 
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

// 联邦学习任务列表数据
export const mockFederatedTasks = [
  {
    taskId: 'c3d4e5f6789012345678901234567890',
    taskName: '水声传播特征分类任务',
    taskType: 'CLASSIFICATION' as const,
    status: 'RUNNING' as const,
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
    taskType: 'REGRESSION' as const,
    status: 'COMPLETED' as const,
    createdAt: '2024-01-01T08:00:00.000Z',
    startedAt: '2024-01-01T08:30:00.000Z',
    completedAt: '2024-01-01T11:00:00.000Z',
    participantCount: 3,
    currentRound: undefined,
    totalRounds: 12,
    progress: undefined,
    finalAccuracy: 0.892
  },
  {
    taskId: 'e5f67890123456789012345678901234',
    taskName: '水下异常检测任务',
    taskType: 'ANOMALY_DETECTION' as const,
    status: 'PAUSED' as const,
    createdAt: '2024-01-01T07:00:00.000Z',
    startedAt: '2024-01-01T07:30:00.000Z',
    participantCount: 4,
    currentRound: 3,
    totalRounds: 12,
    progress: 25.0,
    finalAccuracy: undefined
  },
  {
    taskId: 'f6789012345678901234567890123456',
    taskName: '海洋声学聚类分析',
    taskType: 'CLUSTERING' as const,
    status: 'CONFIGURED' as const,
    createdAt: '2024-01-01T06:00:00.000Z',
    participantCount: 5,
    currentRound: undefined,
    totalRounds: 20,
    progress: undefined,
    finalAccuracy: undefined
  },
  {
    taskId: 'a1b2c3d4e5f678901234567890123456',
    taskName: '深海声学模式识别',
    taskType: 'CLASSIFICATION' as const,
    status: 'CREATED' as const,
    createdAt: '2024-01-01T05:00:00.000Z',
    participantCount: 3,
    currentRound: undefined,
    totalRounds: 10,
    progress: undefined,
    finalAccuracy: undefined
  },
  {
    taskId: 'b2c3d4e5f67890123456789012345678',
    taskName: '海底地形声学分析',
    taskType: 'REGRESSION' as const,
    status: 'FAILED' as const,
    createdAt: '2024-01-01T04:00:00.000Z',
    startedAt: '2024-01-01T04:30:00.000Z',
    participantCount: 4,
    currentRound: 2,
    totalRounds: 15,
    progress: 13.33,
    finalAccuracy: undefined
  },
  {
    taskId: 'c4d5e6f7890123456789012345678901',
    taskName: '水声通信优化',
    taskType: 'CLASSIFICATION' as const,
    status: 'CONFIGURED' as const,
    createdAt: '2024-01-01T03:00:00.000Z',
    participantCount: 2,
    currentRound: undefined,
    totalRounds: 8,
    progress: undefined,
    finalAccuracy: undefined
  }
]

// 联邦学习任务详情数据
export const mockFederatedTaskDetails = [
  {
    taskId: 'c3d4e5f6789012345678901234567890',
    taskName: '水声传播特征分类任务',
    taskType: 'CLASSIFICATION' as const,
    status: 'RUNNING' as const,
    algorithm: 'FEDERATED_AVERAGING',
    createdAt: '2024-01-01T09:00:00.000Z',
    startedAt: '2024-01-01T10:00:00.000Z',
    currentRound: 5,
    totalRounds: 15,
    progress: 33.33,
    participantCount: 2,
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
  {
    taskId: 'd4e5f678901234567890123456789012',
    taskName: '声学传播回归分析',
    taskType: 'REGRESSION' as const,
    status: 'COMPLETED' as const,
    algorithm: 'FEDERATED_AVERAGING',
    createdAt: '2024-01-01T08:00:00.000Z',
    startedAt: '2024-01-01T08:30:00.000Z',
    completedAt: '2024-01-01T11:00:00.000Z',
    currentRound: 12,
    totalRounds: 12,
    progress: 100,
    participantCount: 3,
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
        vmId: 'c3d4e5f67890123456789012345678901',
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
  {
    taskId: 'e5f67890123456789012345678901234',
    taskName: '水下异常检测任务',
    taskType: 'ANOMALY_DETECTION' as const,
    status: 'PAUSED' as const,
    algorithm: 'FEDERATED_PROXIMAL',
    createdAt: '2024-01-01T07:00:00.000Z',
    startedAt: '2024-01-01T07:30:00.000Z',
    currentRound: 3,
    totalRounds: 12,
    progress: 25.0,
    participantCount: 4,
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
        vmId: 'c3d4e5f67890123456789012345678901',
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
  {
    taskId: 'a1b2c3d4e5f678901234567890123456',
    taskName: '深海声学模式识别',
    taskType: 'CLASSIFICATION' as const,
    status: 'CREATED' as const,
    algorithm: 'FEDERATED_AVERAGING',
    createdAt: '2024-01-01T05:00:00.000Z',
    currentRound: 0,
    totalRounds: 10,
    progress: 0,
    participantCount: 3,
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
  {
    taskId: 'f6789012345678901234567890123456',
    taskName: '海洋声学聚类分析',
    taskType: 'CLUSTERING' as const,
    status: 'CONFIGURED' as const,
    algorithm: 'FEDERATED_AVERAGING',
    createdAt: '2024-01-01T06:00:00.000Z',
    currentRound: 0,
    totalRounds: 20,
    progress: 0,
    participantCount: 5,
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
        vmId: 'c3d4e5f67890123456789012345678901',
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
  {
    taskId: 'b2c3d4e5f67890123456789012345678',
    taskName: '海底地形声学分析',
    taskType: 'REGRESSION' as const,
    status: 'FAILED' as const,
    algorithm: 'FEDERATED_AVERAGING',
    createdAt: '2024-01-01T04:00:00.000Z',
    startedAt: '2024-01-01T04:30:00.000Z',
    currentRound: 2,
    totalRounds: 15,
    progress: 13.33,
    participantCount: 4,
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
  {
    taskId: 'c4d5e6f7890123456789012345678901',
    taskName: '水声通信优化',
    taskType: 'CLASSIFICATION' as const,
    status: 'CONFIGURED' as const,
    algorithm: 'FEDERATED_AVERAGING',
    createdAt: '2024-01-01T03:00:00.000Z',
    currentRound: 0,
    totalRounds: 8,
    progress: 0,
    participantCount: 2,
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
]

// 任务结果数据
export const mockTaskResults = [
  {
    taskId: 'c3d4e5f6789012345678901234567890',
    taskName: '水声传播特征分类任务',
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
        participants: ['a1b2c3d4e5f678901234567890123456', 'b2c3d4e5f67890123456789012345678', 'c3d4e5f67890123456789012345678901']
      },
      {
        round: 6,
        accuracy: 0.845,
        loss: 0.155,
        participants: ['a1b2c3d4e5f678901234567890123456', 'b2c3d4e5f67890123456789012345678', 'c3d4e5f67890123456789012345678901']
      },
      {
        round: 12,
        accuracy: 0.892,
        loss: 0.092,
        participants: ['a1b2c3d4e5f678901234567890123456', 'b2c3d4e5f67890123456789012345678', 'c3d4e5f67890123456789012345678901']
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
        vmId: 'c3d4e5f67890123456789012345678901',
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
        participants: ['a1b2c3d4e5f678901234567890123456', 'b2c3d4e5f67890123456789012345678', 'c3d4e5f67890123456789012345678901', 'd4e5f678901234567890123456789012']
      },
      {
        round: 2,
        accuracy: 0.735,
        loss: 0.385,
        participants: ['a1b2c3d4e5f678901234567890123456', 'b2c3d4e5f67890123456789012345678', 'c3d4e5f67890123456789012345678901', 'd4e5f678901234567890123456789012']
      },
      {
        round: 3,
        accuracy: 0.763,
        loss: 0.360,
        participants: ['a1b2c3d4e5f678901234567890123456', 'b2c3d4e5f67890123456789012345678', 'c3d4e5f67890123456789012345678901', 'd4e5f678901234567890123456789012']
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
        vmId: 'c3d4e5f67890123456789012345678901',
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
      message: '水声传播特征分类任务启动成功',
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
        participants: ['a1b2c3d4e5f678901234567890123456', 'b2c3d4e5f67890123456789012345678', 'c3d4e5f67890123456789012345678901']
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
      timestamp: '2024-01-01T07:30:00.000Z',
      level: 'INFO',
      message: '水下异常检测任务启动成功',
      source: 'TASK_MANAGER',
      details: {
        participants: ['a1b2c3d4e5f678901234567890123456', 'b2c3d4e5f67890123456789012345678', 'c3d4e5f67890123456789012345678901', 'd4e5f678901234567890123456789012']
      }
    },
    {
      timestamp: '2024-01-01T07:35:00.000Z',
      level: 'INFO',
      message: '开始第1轮联邦训练',
      source: 'TRAINING_COORDINATOR',
      details: {
        round: 1,
        participants: 4
      }
    },
    {
      timestamp: '2024-01-01T08:35:00.000Z',
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
      timestamp: '2024-01-01T08:40:00.000Z',
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
      timestamp: '2024-01-01T06:00:00.000Z',
      level: 'INFO',
      message: '海洋声学聚类分析任务创建成功',
      source: 'TASK_MANAGER',
      details: {
        taskType: 'CLUSTERING',
        algorithm: 'FEDERATED_AVERAGING'
      }
    },
    {
      timestamp: '2024-01-01T06:05:00.000Z',
      level: 'INFO',
      message: '任务配置完成，等待启动',
      source: 'TASK_MANAGER',
      details: {
        participants: ['a1b2c3d4e5f678901234567890123456', 'b2c3d4e5f67890123456789012345678', 'c3d4e5f67890123456789012345678901', 'd4e5f678901234567890123456789012', 'e5f67890123456789012345678901234'],
        totalRounds: 20
      }
    }
  ]
}

// 可用虚拟机列表
export const mockAvailableVMs = [
  {
    vmId: 'a1b2c3d4e5f678901234567890123456',
    name: '水声联邦学习节点-001',
    ipAddress: '192.168.1.100',
    status: 'RUNNING' as const,
    connectionStatus: 'CONNECTED' as const,
    osType: 'Ubuntu 20.04',
    resources: {
      cpuCores: 8,
      memoryMb: 16384,
      diskGb: 500,
      gpuCount: 1,
      gpuMemoryMb: 8192
    },
    capabilities: ['GPU', 'HIGH_MEMORY'],
    supportedAlgorithms: ['FEDERATED_AVERAGING', 'FEDERATED_PROXIMAL'],
    currentUsage: {
      cpuUsage: 45.2,
      memoryUsage: 60.8,
      networkUsage: 15.3
    },
    networkInfo: {
      bandwidth: 1000,
      latency: 12,
      uploadSpeed: 100,
      downloadSpeed: 100
    },
    lastHeartbeat: generateTimestamp(),
    reliability: {
      uptime: 99.5,
      avgResponseTime: 150,
      taskSuccessRate: 0.98
    }
  },
  {
    vmId: 'b2c3d4e5f67890123456789012345678',
    name: '水声联邦学习节点-002',
    ipAddress: '192.168.1.101',
    status: 'RUNNING' as const,
    connectionStatus: 'CONNECTED' as const,
    osType: 'Ubuntu 20.04',
    resources: {
      cpuCores: 6,
      memoryMb: 12288,
      diskGb: 300,
      gpuCount: 0,
      gpuMemoryMb: 0
    },
    capabilities: ['TRAINING'],
    supportedAlgorithms: ['FEDERATED_AVERAGING'],
    currentUsage: {
      cpuUsage: 32.1,
      memoryUsage: 48.5,
      networkUsage: 8.7
    },
    networkInfo: {
      bandwidth: 1000,
      latency: 18,
      uploadSpeed: 80,
      downloadSpeed: 80
    },
    lastHeartbeat: generateTimestamp(),
    reliability: {
      uptime: 97.8,
      avgResponseTime: 180,
      taskSuccessRate: 0.95
    }
  },
  {
    vmId: 'c3d4e5f67890123456789012345678901',
    name: '水声联邦学习节点-003',
    ipAddress: '192.168.1.102',
    status: 'STOPPED' as const,
    connectionStatus: 'DISCONNECTED' as const,
    osType: 'Ubuntu 20.04',
    resources: {
      cpuCores: 4,
      memoryMb: 8192,
      diskGb: 200,
      gpuCount: 0,
      gpuMemoryMb: 0
    },
    capabilities: ['TRAINING'],
    supportedAlgorithms: ['FEDERATED_AVERAGING'],
    currentUsage: {
      cpuUsage: 0,
      memoryUsage: 0,
      networkUsage: 0
    },
    networkInfo: {
      bandwidth: 100,
      latency: 0,
      uploadSpeed: 0,
      downloadSpeed: 0
    },
    lastHeartbeat: generateTimestamp(0, -2),
    reliability: {
      uptime: 85.2,
      avgResponseTime: 200,
      taskSuccessRate: 0.92
    }
  }
]

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

// ==================== 流程编排相关 Mock 数据 ====================

// 编排工作流数据
export const mockOrchestrationWorkflows = [
  {
    orchestrationId: 'orch_001',
    taskId: 'task_001',
    status: 'IN_PROGRESS' as const,
    startedAt: '2024-09-10T10:00:00Z',
    lastUpdated: '2024-09-10T10:45:30Z',
    currentStage: 'FEDERATED_TRAINING',
    progress: {
      overallProgress: 65.0,
      stageProgress: 50.0,
      currentRound: 5,
      totalRounds: 10
    },
    stageDetails: [
      {
        name: 'INITIAL_MODEL_GENERATION' as const,
        status: 'COMPLETED' as const,
        startedAt: '2024-09-10T10:00:00Z',
        completedAt: '2024-09-10T10:03:15Z',
        duration: '00:03:15',
        result: {
          modelId: 'initial_model_001',
          modelSize: 1048576,
          checksum: 'sha256:abc123...'
        }
      },
      {
        name: 'DATA_DISTRIBUTION' as const,
        status: 'COMPLETED' as const,
        startedAt: '2024-09-10T10:03:15Z',
        completedAt: '2024-09-10T10:18:45Z',
        duration: '00:15:30',
        result: {
          distributionId: 'dist_data_001',
          distributedVms: 3,
          totalDataSize: 419430400,
          verificationPassed: true
        }
      },
      {
        name: 'FEDERATED_TRAINING' as const,
        status: 'IN_PROGRESS' as const,
        startedAt: '2024-09-10T10:25:00Z',
        estimatedDuration: '01:20:00'
      }
    ],
    workflowPlan: {
      totalStages: 5,
      estimatedDuration: '02:00:00',
      stages: [
        { name: 'INITIAL_MODEL_GENERATION', status: 'COMPLETED', estimatedDuration: '00:05:00' },
        { name: 'DATA_DISTRIBUTION', status: 'COMPLETED', estimatedDuration: '00:15:00' },
        { name: 'MODEL_DISTRIBUTION', status: 'COMPLETED', estimatedDuration: '00:10:00' },
        { name: 'FEDERATED_TRAINING', status: 'IN_PROGRESS', estimatedDuration: '01:20:00' },
        { name: 'FINAL_AGGREGATION', status: 'PENDING', estimatedDuration: '00:10:00' }
      ]
    },
    resourceAllocation: {
      allocatedMemory: '4GB',
      allocatedCpuCores: 8,
      allocatedBandwidth: '100MB/s',
      participatingVms: ['vm_001', 'vm_002', 'vm_003']
    }
  }
]

// 工作流时间线数据
export const mockWorkflowTimelines = [
  {
    orchestrationId: 'orch_001',
    taskId: 'task_001',
    timeline: {
      startTime: '2024-09-10T10:00:00Z',
      endTime: null,
      totalDuration: '01:23:45',
      events: [
        {
          eventId: 'event_001',
          timestamp: '2024-09-10T10:00:00Z',
          eventType: 'WORKFLOW_STARTED' as const,
          stage: 'INITIALIZATION',
          level: 'MAJOR' as const,
          message: '联邦学习流程启动',
          details: {
            taskId: 'task_001',
            participantCount: 3,
            estimatedDuration: '02:00:00'
          }
        },
        {
          eventId: 'event_002',
          timestamp: '2024-09-10T10:00:15Z',
          eventType: 'STAGE_STARTED' as const,
          stage: 'INITIAL_MODEL_GENERATION',
          level: 'MAJOR' as const,
          message: '开始生成初始模型',
          details: {
            modelType: 'neural_network',
            architecture: 'custom'
          }
        },
        {
          eventId: 'event_003',
          timestamp: '2024-09-10T10:03:15Z',
          eventType: 'STAGE_COMPLETED' as const,
          stage: 'INITIAL_MODEL_GENERATION',
          level: 'MAJOR' as const,
          message: '初始模型生成完成',
          duration: '00:03:00',
          details: {
            modelId: 'initial_model_001',
            modelSize: 1048576,
            status: 'SUCCESS'
          }
        }
      ]
    },
    stagesSummary: {
      'INITIAL_MODEL_GENERATION': {
        status: 'COMPLETED',
        duration: '00:03:00',
        events: 2
      },
      'DATA_DISTRIBUTION': {
        status: 'COMPLETED',
        duration: '00:15:30',
        events: 6
      },
      'FEDERATED_TRAINING': {
        status: 'IN_PROGRESS',
        duration: '00:58:45',
        events: 18,
        completedRounds: 4,
        currentRound: 5
      }
    }
  }
]

// 工作流性能分析数据
export const mockWorkflowAnalytics = [
  {
    orchestrationId: 'orch_001',
    analysisTimestamp: '2024-09-10T11:23:45Z',
    overallPerformance: {
      score: 85.5,
      grade: 'B+',
      efficiency: 87.2,
      reliability: 94.1,
      scalability: 76.8
    },
    stagePerformance: {
      'INITIAL_MODEL_GENERATION': {
        duration: '00:03:00',
        efficiency: 95.0,
        resourceUtilization: 45.2
      },
      'DATA_DISTRIBUTION': {
        duration: '00:15:30',
        efficiency: 82.5,
        transferSpeed: '18.5MB/s',
        resourceUtilization: 67.3
      },
      'FEDERATED_TRAINING': {
        duration: '00:58:45',
        convergenceRate: 0.12,
        participationRate: 100.0,
        resourceUtilization: 78.9
      }
    },
    resourceMetrics: {
      cpu: {
        averageUtilization: 72.5,
        peakUtilization: 89.2,
        efficiency: 81.3
      },
      memory: {
        averageUtilization: 65.8,
        peakUtilization: 82.4,
        efficiency: 79.9
      },
      network: {
        averageUtilization: 42.3,
        peakBandwidth: '85.2MB/s',
        efficiency: 88.7
      }
    },
    qualityMetrics: {
      modelAccuracy: {
        initial: 0.45,
        final: 0.85,
        improvement: 0.40,
        convergenceRounds: 6
      },
      trainingStability: 92.1,
      aggregationQuality: 94.8
    },
    recommendations: [
      {
        category: 'PERFORMANCE' as const,
        priority: 'HIGH' as const,
        title: '增加训练轮次并行度',
        description: '当前虚拟机资源利用率较低，建议增加并行处理的训练任务',
        expectedImprovement: '15-20%性能提升',
        implementation: {
          parameter: 'parallelTraining',
          currentValue: 1,
          recommendedValue: 2
        }
      },
      {
        category: 'EFFICIENCY' as const,
        priority: 'MEDIUM' as const,
        title: '优化数据分发策略',
        description: '数据分发阶段耗时较长，建议使用更高效的分发算法',
        expectedImprovement: '25%分发时间减少',
        implementation: {
          parameter: 'distributionStrategy',
          currentValue: 'BALANCED',
          recommendedValue: 'ADAPTIVE'
        }
      }
    ],
    comparisons: {
      similarTasks: {
        averageDuration: '01:45:30',
        performanceRanking: 'TOP_25%',
        efficiencyRanking: 'TOP_30%'
      },
      historicalTrends: {
        improvementRate: 8.5,
        consistencyScore: 89.2
      }
    }
  }
]

