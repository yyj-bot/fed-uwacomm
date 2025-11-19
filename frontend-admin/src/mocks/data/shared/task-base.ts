/**
 * 任务基础数据 - 单一数据源
 * 
 * 本文件作为所有任务相关mock数据的单一可信数据源
 * 其他文件应该从这里导入任务基础数据
 * 
 * @file shared/task-base.ts
 * @description 任务基础数据定义
 */

import { generateTimestamp } from '../../utils'

/**
 * 任务类型
 */
export type TaskType = 'CLASSIFICATION' | 'REGRESSION' | 'ANOMALY_DETECTION' | 'CLUSTERING'

/**
 * 任务状态
 */
export type TaskStatus = 'CREATED' | 'CONFIGURED' | 'RUNNING' | 'PAUSED' | 'COMPLETED' | 'FAILED' | 'CANCELLED'

/**
 * 基础任务接口
 */
export interface BaseTask {
  taskId: string
  taskName: string
  taskType: TaskType
  status: TaskStatus
  createdAt: string
  startedAt?: string
  completedAt?: string
  participantCount: number
  currentRound?: number
  totalRounds: number
  progress?: number
  finalAccuracy?: number
}

/**
 * 任务基础数据列表
 * 
 * 这是所有任务数据的单一数据源
 * 任何需要任务数据的mock文件都应该从这里导入
 */
export const baseTaskList: BaseTask[] = [
  {
    taskId: 'c3d4e5f6789012345678901234567890',
    taskName: '联邦水下声呐目标识别',
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
    currentRound: undefined,
    totalRounds: 12,
    progress: undefined,
    finalAccuracy: 0.892
  },
  {
    taskId: 'e5f67890123456789012345678901234',
    taskName: '水下异常检测任务',
    taskType: 'ANOMALY_DETECTION',
    status: 'PAUSED',
    createdAt: '2024-01-02T07:00:00.000Z',
    startedAt: '2024-01-02T07:30:00.000Z',
    participantCount: 4,
    currentRound: 3,
    totalRounds: 12,
    progress: 25.0,
    finalAccuracy: undefined
  },
  {
    taskId: 'f6789012345678901234567890123456',
    taskName: '海洋声学聚类分析',
    taskType: 'CLUSTERING',
    status: 'CONFIGURED',
    createdAt: '2024-01-03T06:00:00.000Z',
    participantCount: 5,
    currentRound: undefined,
    totalRounds: 20,
    progress: undefined,
    finalAccuracy: undefined
  },
  {
    taskId: 'a1b2c3d4e5f678901234567890123456',
    taskName: '深海声学模式识别',
    taskType: 'CLASSIFICATION',
    status: 'CREATED',
    createdAt: '2024-01-04T05:00:00.000Z',
    participantCount: 3,
    currentRound: undefined,
    totalRounds: 10,
    progress: undefined,
    finalAccuracy: undefined
  },
  {
    taskId: 'b2c3d4e5f67890123456789012345678',
    taskName: '海底地形声学分析',
    taskType: 'REGRESSION',
    status: 'FAILED',
    createdAt: '2024-01-05T04:00:00.000Z',
    startedAt: '2024-01-05T04:30:00.000Z',
    participantCount: 4,
    currentRound: 2,
    totalRounds: 15,
    progress: 13.33,
    finalAccuracy: undefined
  },
]

/**
 * 工具函数：根据任务ID查找任务
 */
export const getTaskById = (taskId: string): BaseTask | undefined => {
  return baseTaskList.find(task => task.taskId === taskId)
}

/**
 * 工具函数：根据任务类型筛选任务
 */
export const getTasksByType = (taskType: TaskType): BaseTask[] => {
  return baseTaskList.filter(task => task.taskType === taskType)
}

/**
 * 工具函数：根据任务状态筛选任务
 */
export const getTasksByStatus = (status: TaskStatus): BaseTask[] => {
  return baseTaskList.filter(task => task.status === status)
}

/**
 * 工具函数：获取运行中的任务
 */
export const getRunningTasks = (): BaseTask[] => {
  return getTasksByStatus('RUNNING')
}

/**
 * 工具函数：获取已完成的任务
 */
export const getCompletedTasks = (): BaseTask[] => {
  return getTasksByStatus('COMPLETED')
}

/**
 * 工具函数：获取失败的任务
 */
export const getFailedTasks = (): BaseTask[] => {
  return getTasksByStatus('FAILED')
}




