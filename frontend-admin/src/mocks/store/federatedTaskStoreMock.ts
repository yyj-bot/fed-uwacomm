/**
 * Federated Task Store Mock 数据
 * 用于测试联邦学习任务管理相关功能的模拟数据
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import type { 
  FederatedTask, 
  FederatedTaskDetails, 
  TaskResults, 
  TaskParticipant,
  TaskMetrics
} from '@/types'
import type { CreateTaskRequest } from '@/services'

// ==================== 模拟联邦学习任务数据 ====================

export const mockTask: FederatedTask = {
  taskId: 'task-001',
  taskName: 'MNIST联邦学习训练',
  taskType: 'CLASSIFICATION',
  status: 'COMPLETED',
  createdAt: '2024-01-15T08:00:00Z',
  startedAt: '2024-01-15T08:30:00Z',
  completedAt: '2024-01-15T10:00:00Z',
  currentRound: 10,
  totalRounds: 10,
  progress: 100,
  participantCount: 3,
  finalAccuracy: 92.5
}

export const mockTaskList: FederatedTask[] = [
  mockTask,
  {
    taskId: 'task-002',
    taskName: 'CIFAR-10图像分类',
    taskType: 'CLASSIFICATION',
    status: 'RUNNING',
    createdAt: '2024-01-15T09:00:00Z',
    startedAt: '2024-01-15T09:30:00Z',
    currentRound: 5,
    totalRounds: 15,
    progress: 33,
    participantCount: 5,
    finalAccuracy: 85.2
  },
  {
    taskId: 'task-003',
    taskName: '回归预测模型',
    taskType: 'REGRESSION',
    status: 'CREATED',
    createdAt: '2024-01-15T10:00:00Z',
    participantCount: 2
  }
]

// ==================== 模拟任务详情 ====================

export const mockTaskParticipants: TaskParticipant[] = [
  {
    vmId: 'vm-001',
    role: 'PARTICIPANT',
    status: 'CONNECTED',
    dataSource: 'local-dataset-001',
    lastHeartbeat: '2024-01-15T10:00:00Z',
    currentEpoch: 5,
    loss: 0.25,
    accuracy: 92.3
  },
  {
    vmId: 'vm-002',
    role: 'PARTICIPANT',
    status: 'TRAINING',
    dataSource: 'local-dataset-002',
    lastHeartbeat: '2024-01-15T10:00:30Z',
    currentEpoch: 5,
    loss: 0.28,
    accuracy: 91.8
  },
  {
    vmId: 'vm-003',
    role: 'PARTICIPANT',
    status: 'IDLE',
    dataSource: 'local-dataset-003',
    lastHeartbeat: '2024-01-15T09:58:00Z',
    currentEpoch: 5,
    loss: 0.30,
    accuracy: 90.5
  }
]

export const mockTaskMetrics: TaskMetrics = {
  globalLoss: 0.275,
  globalAccuracy: 91.5,
  communicationRounds: 10,
  dataProcessed: 60000,
  estimatedTimeRemaining: 0
}

export const mockTaskDetails: FederatedTaskDetails = {
  ...mockTask,
  algorithm: 'FedAvg',
  participants: mockTaskParticipants,
  metrics: mockTaskMetrics,
  hyperparameters: {
    learningRate: 0.001,
    batchSize: 32,
    epochs: 5,
    rounds: 10,
    minParticipants: 2
  },
  modelConfig: {
    modelType: 'CNN',
    featureColumns: ['pixel_0', 'pixel_1', 'pixel_2'],
    targetColumn: 'label',
    testSize: 0.2,
    randomState: 42
  }
}

// ==================== 模拟任务结果 ====================

export const mockTaskResults: TaskResults = {
  taskId: 'task-001',
  taskName: 'MNIST联邦学习训练',
  status: 'COMPLETED',
  finalResults: {
    accuracy: 92.5,
    loss: 0.275,
    precision: 91.8,
    recall: 92.1,
    f1Score: 91.9,
    confusionMatrix: [
      [950, 0, 2, 1, 1, 8, 5, 1, 10, 2],
      [0, 1128, 3, 2, 0, 1, 4, 1, 6, 0],
      [9, 8, 1004, 10, 8, 3, 4, 6, 12, 4],
      [4, 0, 17, 921, 0, 21, 0, 6, 18, 23],
      [1, 2, 5, 3, 921, 0, 6, 2, 4, 38],
      [13, 3, 1, 43, 8, 801, 17, 7, 14, 5],
      [9, 3, 0, 1, 3, 13, 929, 0, 0, 0],
      [2, 6, 22, 4, 4, 0, 0, 980, 3, 7],
      [6, 8, 3, 14, 3, 4, 7, 5, 918, 6],
      [9, 7, 0, 6, 14, 4, 1, 4, 7, 957]
    ]
  },
  roundResults: [
    {
      round: 1,
      accuracy: 85.2,
      loss: 0.45,
      participants: ['vm-001', 'vm-002', 'vm-003']
    },
    {
      round: 2,
      accuracy: 87.1,
      loss: 0.38,
      participants: ['vm-001', 'vm-002', 'vm-003']
    },
    {
      round: 10,
      accuracy: 92.5,
      loss: 0.275,
      participants: ['vm-001', 'vm-002', 'vm-003']
    }
  ],
  participantResults: [
    {
      vmId: 'vm-001',
      finalAccuracy: 92.3,
      finalLoss: 0.25,
      trainingTime: 3600,
      dataSize: 20000,
      parameters: { learningRate: 0.001, batchSize: 32 }
    },
    {
      vmId: 'vm-002',
      finalAccuracy: 91.8,
      finalLoss: 0.28,
      trainingTime: 3800,
      dataSize: 20000,
      parameters: { learningRate: 0.001, batchSize: 32 }
    },
    {
      vmId: 'vm-003',
      finalAccuracy: 90.5,
      finalLoss: 0.30,
      trainingTime: 3900,
      dataSize: 20000,
      parameters: { learningRate: 0.001, batchSize: 32 }
    }
  ],
  modelInfo: {
    parameters: {
      totalParams: 1234567,
      trainableParams: 1234567,
      layers: 8,
      modelSize: '4.7MB'
    }
  }
}

// ==================== 模拟任务日志 ====================

export const mockTaskLogs = [
  {
    timestamp: '2024-01-15T08:30:00Z',
    level: 'INFO',
    message: '任务开始执行',
    source: 'task-manager',
    details: { taskId: 'task-001', phase: 'initialization' }
  },
  {
    timestamp: '2024-01-15T08:35:00Z',
    level: 'INFO',
    message: '第1轮训练开始',
    source: 'federated-trainer',
    details: { round: 1, participants: 3 }
  },
  {
    timestamp: '2024-01-15T08:45:00Z',
    level: 'INFO',
    message: '第1轮训练完成',
    source: 'federated-trainer',
    details: { round: 1, accuracy: 85.2, loss: 0.45 }
  },
  {
    timestamp: '2024-01-15T10:00:00Z',
    level: 'INFO',
    message: '任务执行完成',
    source: 'task-manager',
    details: { taskId: 'task-001', finalAccuracy: 92.5 }
  }
]

// ==================== 模拟创建任务请求 ====================

export const mockCreateTaskRequest: CreateTaskRequest = {
  taskName: '新建MNIST分类任务',
  taskType: 'CLASSIFICATION',
  description: '测试联邦学习分类算法',
  algorithm: 'FedAvg',
  participants: [
    {
      vmId: 'vm-001',
      role: 'PARTICIPANT',
      dataSource: 'mnist-dataset-001'
    },
    {
      vmId: 'vm-002',
      role: 'PARTICIPANT',
      dataSource: 'mnist-dataset-002'
    }
  ],
  hyperparameters: {
    learningRate: 0.001,
    batchSize: 32,
    epochs: 5,
    rounds: 10,
    minParticipants: 2
  },
  modelConfig: {
    modelType: 'CNN',
    featureColumns: ['pixel_0', 'pixel_1', 'pixel_2'],
    targetColumn: 'label',
    testSize: 0.2,
    randomState: 42
  },
  schedule: {
    startTime: '2024-01-16T09:00:00Z',
    timeout: 7200
  }
}

// ==================== 模拟错误信息 ====================

export const mockFederatedTaskErrors = {
  TASK_NOT_FOUND: new Error('任务不存在'),
  TASK_ALREADY_EXISTS: new Error('任务已存在'),
  TASK_ALREADY_RUNNING: new Error('任务正在运行'),
  TASK_ALREADY_COMPLETED: new Error('任务已完成'),
  INSUFFICIENT_CLIENTS: new Error('参与者数量不足'),
  MODEL_NOT_READY: new Error('模型未就绪'),
  DATA_NOT_AVAILABLE: new Error('数据不可用'),
  TASK_CONFIG_ERROR: new Error('任务配置错误'),
  COMMUNICATION_ERROR: new Error('通信失败'),
  AGGREGATION_ERROR: new Error('聚合失败'),
  VALIDATION_ERROR: new Error('验证失败'),
  NETWORK_ERROR: new Error('网络连接失败'),
  TIMEOUT_ERROR: new Error('请求超时'),
  SERVER_ERROR: new Error('服务器内部错误')
}

// ==================== 模拟初始状态 ====================

export const mockInitialTaskState = {
  taskList: [],
  taskListTotal: 0,
  taskListLoading: false,
  taskListError: null,
  currentTask: null,
  currentTaskLoading: false,
  currentTaskError: null,
  taskResults: {},
  taskLogs: {},
  taskLogsLoading: {},
  operationLoading: {},
  operationError: {},
  createTaskLoading: false,
  createTaskError: null,
  pagination: {
    page: 1,
    size: 20,
    total: 0
  },
  queryParams: {},
  realtimeData: {}
}

// ==================== 模拟工具函数 ====================

export const createMockTask = (overrides: Partial<FederatedTask> = {}): FederatedTask => ({
  ...mockTask,
  taskId: `task-${Date.now()}`,
  createdAt: new Date().toISOString(),
  ...overrides
})

export const createMockTaskDetails = (overrides: Partial<FederatedTaskDetails> = {}): FederatedTaskDetails => ({
  ...mockTaskDetails,
  taskId: `task-${Date.now()}`,
  createdAt: new Date().toISOString(),
  ...overrides
})

export const createMockTaskParticipant = (overrides: Partial<TaskParticipant> = {}): TaskParticipant => ({
  vmId: `vm-${Date.now()}`,
  role: 'PARTICIPANT',
  status: 'CONNECTED',
  dataSource: 'local-dataset',
  lastHeartbeat: new Date().toISOString(),
  ...overrides
})

export const createMockTaskResults = (overrides: Partial<TaskResults> = {}): TaskResults => ({
  ...mockTaskResults,
  taskId: `task-${Date.now()}`,
  ...overrides
})

export const createMockTaskLog = (overrides: any = {}) => ({
  timestamp: new Date().toISOString(),
  level: 'INFO',
  message: '任务日志消息',
  source: 'task-manager',
  details: {},
  ...overrides
})
