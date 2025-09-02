/**
 * 联邦学习任务服务单元测试
 * 使用Vitest测试所有联邦学习任务服务接口方法
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { FederatedTaskService } from '@/services/federated-task/federatedTaskService'
import { federatedTask } from '@/api/federated-task'
import { mockFederatedTasks, mockFederatedTaskDetails, mockTaskResults, mockTaskLogs, mockFederatedTaskApi } from '../../mocks/services/federatedTaskMock'
import type { 
  FederatedTask,
  FederatedTaskDetails,
  TaskResults
} from '@/api/federated-task'
import type { 
  CreateTaskRequest,
  ConfigTaskRequest,
  TaskListParams,
  StopTaskRequest,
  CancelTaskRequest,
  DeleteTaskRequest,
  TaskLogsParams
} from '@/services/federated-task/type'

// Mock federatedTask API
vi.mock('@/api/federated-task', () => ({
  federatedTask: {
    createTask: vi.fn(),
    configTask: vi.fn(),
    startTask: vi.fn(),
    pauseTask: vi.fn(),
    resumeTask: vi.fn(),
    stopTask: vi.fn(),
    cancelTask: vi.fn(),
    getTaskDetail: vi.fn(),
    getTaskList: vi.fn(),
    getTaskResults: vi.fn(),
    getTaskLogs: vi.fn(),
    deleteTask: vi.fn()
  }
}))

describe('FederatedTaskService', () => {
  let federatedTaskService: FederatedTaskService
  
  beforeEach(() => {
    federatedTaskService = new FederatedTaskService()
    vi.clearAllMocks()
  })
  
  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('createTask', () => {
    it('应该成功创建联邦学习任务', async () => {
      // 准备测试数据
      const taskData: CreateTaskRequest = {
        taskName: '测试联邦学习任务',
        taskType: 'CLASSIFICATION',
        description: '测试用的分类任务',
        algorithm: 'FEDERATED_AVERAGING',
        participants: [
          {
            vmId: 'a1b2c3d4e5f678901234567890123456',
            role: 'PARTICIPANT',
            dataSource: 'test_data_001.csv'
          },
          {
            vmId: 'b2c3d4e5f67890123456789012345678',
            role: 'PARTICIPANT',
            dataSource: 'test_data_002.csv'
          }
        ],
        hyperparameters: {
          learningRate: 0.01,
          batchSize: 32,
          epochs: 100,
          rounds: 10,
          minParticipants: 2
        },
        modelConfig: {
          modelType: 'RANDOM_FOREST',
          featureColumns: ['feature_1', 'feature_2'],
          targetColumn: 'target',
          testSize: 0.2,
          randomState: 42
        }
      }
      
      const mockResponse = mockFederatedTaskApi.createTask(taskData).data
      
      // Mock API调用
      vi.mocked(federatedTask.createTask).mockResolvedValue(mockResponse)
      
      // 执行测试
      const result = await federatedTaskService.createTask(taskData)
      
      // 验证结果
      expect(federatedTask.createTask).toHaveBeenCalledWith(taskData)
      expect(result.taskId).toBeDefined()
      expect(result.taskName).toBe(taskData.taskName)
      expect(result.status).toBe('CREATED')
      expect(result.createdAt).toBeDefined()
      expect(result.createdBy).toBeDefined()
      expect(result.participantCount).toBe(2)
      expect(result.estimatedDuration).toBeGreaterThan(0)
    })

    it('应该验证任务名称', async () => {
      const invalidData: CreateTaskRequest = {
        taskName: '', // 空名称
        taskType: 'CLASSIFICATION',
        algorithm: 'FEDERATED_AVERAGING',
        participants: [
          { vmId: 'test1', role: 'PARTICIPANT', dataSource: 'data1.csv' },
          { vmId: 'test2', role: 'PARTICIPANT', dataSource: 'data2.csv' }
        ],
        hyperparameters: {
          learningRate: 0.01,
          batchSize: 32,
          epochs: 100,
          rounds: 10,
          minParticipants: 2
        },
        modelConfig: {
          modelType: 'RANDOM_FOREST',
          featureColumns: ['feature_1'],
          targetColumn: 'target',
          testSize: 0.2,
          randomState: 42
        }
      }
      
      await expect(federatedTaskService.createTask(invalidData)).rejects.toThrow('任务名称不能为空')
    })

    it('应该验证任务类型', async () => {
      const invalidData: CreateTaskRequest = {
        taskName: '测试任务',
        taskType: 'INVALID_TYPE' as any, // 无效类型
        algorithm: 'FEDERATED_AVERAGING',
        participants: [
          { vmId: 'test1', role: 'PARTICIPANT', dataSource: 'data1.csv' },
          { vmId: 'test2', role: 'PARTICIPANT', dataSource: 'data2.csv' }
        ],
        hyperparameters: {
          learningRate: 0.01,
          batchSize: 32,
          epochs: 100,
          rounds: 10,
          minParticipants: 2
        },
        modelConfig: {
          modelType: 'RANDOM_FOREST',
          featureColumns: ['feature_1'],
          targetColumn: 'target',
          testSize: 0.2,
          randomState: 42
        }
      }
      
      await expect(federatedTaskService.createTask(invalidData)).rejects.toThrow('任务类型无效')
    })

    it('应该验证参与者数量', async () => {
      const invalidData: CreateTaskRequest = {
        taskName: '测试任务',
        taskType: 'CLASSIFICATION',
        algorithm: 'FEDERATED_AVERAGING',
        participants: [
          { vmId: 'test1', role: 'PARTICIPANT', dataSource: 'data1.csv' }
        ], // 只有1个参与者
        hyperparameters: {
          learningRate: 0.01,
          batchSize: 32,
          epochs: 100,
          rounds: 10,
          minParticipants: 2
        },
        modelConfig: {
          modelType: 'RANDOM_FOREST',
          featureColumns: ['feature_1'],
          targetColumn: 'target',
          testSize: 0.2,
          randomState: 42
        }
      }
      
      await expect(federatedTaskService.createTask(invalidData)).rejects.toThrow('参与者数量至少为2个')
    })

    it('应该验证超参数配置', async () => {
      const invalidData: CreateTaskRequest = {
        taskName: '测试任务',
        taskType: 'CLASSIFICATION',
        algorithm: 'FEDERATED_AVERAGING',
        participants: [
          { vmId: 'test1', role: 'PARTICIPANT', dataSource: 'data1.csv' },
          { vmId: 'test2', role: 'PARTICIPANT', dataSource: 'data2.csv' }
        ],
        hyperparameters: {
          learningRate: -0.01, // 无效学习率
          batchSize: 32,
          epochs: 100,
          rounds: 10,
          minParticipants: 2
        },
        modelConfig: {
          modelType: 'RANDOM_FOREST',
          featureColumns: ['feature_1'],
          targetColumn: 'target',
          testSize: 0.2,
          randomState: 42
        }
      }
      
      await expect(federatedTaskService.createTask(invalidData)).rejects.toThrow('学习率必须为正数')
    })

    it('应该处理创建失败', async () => {
      const taskData: CreateTaskRequest = {
        taskName: '测试任务',
        taskType: 'CLASSIFICATION',
        algorithm: 'FEDERATED_AVERAGING',
        participants: [
          { vmId: 'test1', role: 'PARTICIPANT', dataSource: 'data1.csv' },
          { vmId: 'test2', role: 'PARTICIPANT', dataSource: 'data2.csv' }
        ],
        hyperparameters: {
          learningRate: 0.01,
          batchSize: 32,
          epochs: 100,
          rounds: 10,
          minParticipants: 2
        },
        modelConfig: {
          modelType: 'RANDOM_FOREST',
          featureColumns: ['feature_1'],
          targetColumn: 'target',
          testSize: 0.2,
          randomState: 42
        }
      }
      
      const error = new Error('创建失败')
      vi.mocked(federatedTask.createTask).mockRejectedValue(error)
      
      await expect(federatedTaskService.createTask(taskData)).rejects.toThrow('创建联邦学习任务失败')
    })
  })

  describe('configTask', () => {
    it('应该成功配置联邦学习任务', async () => {
      const taskId = mockFederatedTasks[0].taskId
      const config: ConfigTaskRequest = {
        hyperparameters: {
          learningRate: 0.02,
          batchSize: 64,
          epochs: 150
        },
        securityConfig: {
          differentialPrivacy: {
            enabled: true,
            epsilon: 1.0,
            delta: 0.0001
          }
        }
      }
      
      const mockResponse = mockFederatedTaskApi.configTask(taskId, config).data
      vi.mocked(federatedTask.configTask).mockResolvedValue(mockResponse)
      
      const result = await federatedTaskService.configTask(taskId, config)
      
      expect(federatedTask.configTask).toHaveBeenCalledWith(taskId, config)
      expect(result.taskId).toBe(taskId)
      expect(result.status).toBe('CONFIGURED')
      expect(result.updatedAt).toBeDefined()
      expect(result.configVersion).toBeDefined()
    })

    it('应该验证任务ID', async () => {
      const config: ConfigTaskRequest = {
        hyperparameters: {
          learningRate: 0.02
        }
      }
      
      await expect(federatedTaskService.configTask('', config)).rejects.toThrow('任务ID不能为空')
    })

    it('应该验证差分隐私参数', async () => {
      const taskId = mockFederatedTasks[0].taskId
      const config: ConfigTaskRequest = {
        securityConfig: {
          differentialPrivacy: {
            enabled: true,
            epsilon: -1.0 // 无效epsilon
          }
        }
      }
      
      await expect(federatedTaskService.configTask(taskId, config)).rejects.toThrow('差分隐私epsilon参数必须为正数')
    })
  })

  describe('startTask', () => {
    it('应该成功启动联邦学习任务', async () => {
      const taskId = mockFederatedTasks[0].taskId
      const mockResponse = mockFederatedTaskApi.startTask(taskId).data
      
      vi.mocked(federatedTask.startTask).mockResolvedValue(mockResponse)
      
      const result = await federatedTaskService.startTask(taskId)
      
      expect(federatedTask.startTask).toHaveBeenCalledWith(taskId)
      expect(result.taskId).toBe(taskId)
      expect(result.status).toBe('RUNNING')
      expect(result.startedAt).toBeDefined()
      expect(result.currentRound).toBe(0)
      expect(Array.isArray(result.participants)).toBe(true)
    })

    it('应该验证任务ID格式', async () => {
      await expect(federatedTaskService.startTask('invalid-id')).rejects.toThrow('任务ID格式不正确')
    })
  })

  describe('pauseTask', () => {
    it('应该成功暂停联邦学习任务', async () => {
      const taskId = mockFederatedTasks[0].taskId
      const mockResponse = mockFederatedTaskApi.pauseTask(taskId).data
      
      vi.mocked(federatedTask.pauseTask).mockResolvedValue(mockResponse)
      
      const result = await federatedTaskService.pauseTask(taskId)
      
      expect(federatedTask.pauseTask).toHaveBeenCalledWith(taskId)
      expect(result.taskId).toBe(taskId)
      expect(result.status).toBe('PAUSED')
      expect(result.pausedAt).toBeDefined()
      expect(result.currentRound).toBeGreaterThanOrEqual(0)
      expect(result.resumePoint).toBeDefined()
    })
  })

  describe('resumeTask', () => {
    it('应该成功恢复联邦学习任务', async () => {
      const taskId = mockFederatedTasks[0].taskId
      const mockResponse = mockFederatedTaskApi.resumeTask(taskId).data
      
      vi.mocked(federatedTask.resumeTask).mockResolvedValue(mockResponse)
      
      const result = await federatedTaskService.resumeTask(taskId)
      
      expect(federatedTask.resumeTask).toHaveBeenCalledWith(taskId)
      expect(result.taskId).toBe(taskId)
      expect(result.status).toBe('RUNNING')
      expect(result.resumedAt).toBeDefined()
      expect(result.currentRound).toBeGreaterThanOrEqual(0)
    })
  })

  describe('stopTask', () => {
    it('应该成功停止联邦学习任务', async () => {
      const taskId = mockFederatedTasks[0].taskId
      const stopData: StopTaskRequest = {
        reason: '用户主动停止',
        saveCheckpoint: true
      }
      
      const mockResponse = mockFederatedTaskApi.stopTask(taskId, stopData).data
      vi.mocked(federatedTask.stopTask).mockResolvedValue(mockResponse)
      
      const result = await federatedTaskService.stopTask(taskId, stopData)
      
      expect(federatedTask.stopTask).toHaveBeenCalledWith(taskId, stopData)
      expect(result.taskId).toBe(taskId)
      expect(result.status).toBe('STOPPED')
      expect(result.stoppedAt).toBeDefined()
      expect(result.finalRound).toBeGreaterThanOrEqual(0)
      expect(result.checkpointSaved).toBe(true)
      expect(result.checkpointPath).toBeDefined()
    })

    it('应该支持无参数停止', async () => {
      const taskId = mockFederatedTasks[0].taskId
      const mockResponse = mockFederatedTaskApi.stopTask(taskId).data
      
      vi.mocked(federatedTask.stopTask).mockResolvedValue(mockResponse)
      
      const result = await federatedTaskService.stopTask(taskId)
      
      expect(federatedTask.stopTask).toHaveBeenCalledWith(taskId, undefined)
      expect(result.taskId).toBe(taskId)
    })
  })

  describe('cancelTask', () => {
    it('应该成功取消联邦学习任务', async () => {
      const taskId = mockFederatedTasks[0].taskId
      const cancelData: CancelTaskRequest = {
        reason: '任务配置错误'
      }
      
      const mockResponse = mockFederatedTaskApi.cancelTask(taskId, cancelData).data
      vi.mocked(federatedTask.cancelTask).mockResolvedValue(mockResponse)
      
      const result = await federatedTaskService.cancelTask(taskId, cancelData)
      
      expect(federatedTask.cancelTask).toHaveBeenCalledWith(taskId, cancelData)
      expect(result.taskId).toBe(taskId)
      expect(result.status).toBe('CANCELLED')
      expect(result.cancelledAt).toBeDefined()
      expect(result.reason).toBe(cancelData.reason)
    })

    it('应该支持无原因取消', async () => {
      const taskId = mockFederatedTasks[0].taskId
      const mockResponse = mockFederatedTaskApi.cancelTask(taskId).data
      
      vi.mocked(federatedTask.cancelTask).mockResolvedValue(mockResponse)
      
      const result = await federatedTaskService.cancelTask(taskId)
      
      expect(federatedTask.cancelTask).toHaveBeenCalledWith(taskId, undefined)
      expect(result.taskId).toBe(taskId)
    })
  })

  describe('getTaskDetail', () => {
    it('应该成功获取任务详情', async () => {
      const taskId = mockFederatedTasks[0].taskId
      const mockDetail = mockFederatedTaskDetails
      
      vi.mocked(federatedTask.getTaskDetail).mockResolvedValue(mockDetail)
      
      const result = await federatedTaskService.getTaskDetail(taskId)
      
      expect(federatedTask.getTaskDetail).toHaveBeenCalledWith(taskId)
      expect(result.taskId).toBe(taskId)
      expect(result.taskName).toBeDefined()
      expect(result.taskType).toBeDefined()
      expect(result.status).toBeDefined()
      expect(result.algorithm).toBeDefined()
      expect(Array.isArray(result.participants)).toBe(true)
      expect(result.metrics).toBeDefined()
    })

    it('应该验证任务ID', async () => {
      await expect(federatedTaskService.getTaskDetail('')).rejects.toThrow('任务ID不能为空')
    })

    it('应该处理任务不存在', async () => {
      const error = new Error('任务不存在')
      vi.mocked(federatedTask.getTaskDetail).mockRejectedValue(error)
      
      await expect(federatedTaskService.getTaskDetail('nonexistent')).rejects.toThrow('获取联邦学习任务详情失败')
    })
  })

  describe('getTaskList', () => {
    it('应该成功获取任务列表', async () => {
      const params: TaskListParams = { page: 1, size: 10 }
      const mockResponse = mockFederatedTaskApi.getTaskList(params).data
      
      vi.mocked(federatedTask.getTaskList).mockResolvedValue(mockResponse)
      
      const result = await federatedTaskService.getTaskList(params)
      
      expect(federatedTask.getTaskList).toHaveBeenCalledWith(params)
      expect(result.total).toBeGreaterThanOrEqual(0)
      expect(result.page).toBe(1)
      expect(result.size).toBe(10)
      expect(Array.isArray(result.tasks)).toBe(true)
    })

    it('应该支持状态过滤', async () => {
      const params: TaskListParams = { status: 'RUNNING' }
      const mockResponse = mockFederatedTaskApi.getTaskList(params).data
      
      vi.mocked(federatedTask.getTaskList).mockResolvedValue(mockResponse)
      
      const result = await federatedTaskService.getTaskList(params)
      
      expect(federatedTask.getTaskList).toHaveBeenCalledWith(params)
      expect(result.tasks.every(task => task.status === 'RUNNING' || result.tasks.length === 0)).toBe(true)
    })

    it('应该支持类型过滤', async () => {
      const params: TaskListParams = { type: 'CLASSIFICATION' }
      const mockResponse = mockFederatedTaskApi.getTaskList(params).data
      
      vi.mocked(federatedTask.getTaskList).mockResolvedValue(mockResponse)
      
      const result = await federatedTaskService.getTaskList(params)
      
      expect(federatedTask.getTaskList).toHaveBeenCalledWith(params)
      expect(result.tasks.every(task => task.taskType === 'CLASSIFICATION' || result.tasks.length === 0)).toBe(true)
    })

    it('应该验证分页参数', async () => {
      const invalidParams: TaskListParams = { page: 0 } // 无效页码
      
      await expect(federatedTaskService.getTaskList(invalidParams)).rejects.toThrow('页码必须大于0')
    })

    it('应该验证每页大小', async () => {
      const invalidParams: TaskListParams = { size: 101 } // 超出限制
      
      await expect(federatedTaskService.getTaskList(invalidParams)).rejects.toThrow('每页大小必须在1-100范围内')
    })

    it('应该验证日期范围', async () => {
      const invalidParams: TaskListParams = {
        startDate: '2024-01-02',
        endDate: '2024-01-01' // 结束日期早于开始日期
      }
      
      await expect(federatedTaskService.getTaskList(invalidParams)).rejects.toThrow('开始日期不能晚于结束日期')
    })
  })

  describe('getTaskResults', () => {
    it('应该成功获取任务结果', async () => {
      const taskId = mockFederatedTasks[0].taskId
      const mockResults = mockTaskResults
      
      vi.mocked(federatedTask.getTaskResults).mockResolvedValue(mockResults)
      
      const result = await federatedTaskService.getTaskResults(taskId)
      
      expect(federatedTask.getTaskResults).toHaveBeenCalledWith(taskId)
      expect(result.taskId).toBe(taskId)
      expect(result.taskName).toBeDefined()
      expect(result.status).toBeDefined()
      expect(result.finalResults).toBeDefined()
      expect(Array.isArray(result.roundResults)).toBe(true)
      expect(Array.isArray(result.participantResults)).toBe(true)
      expect(result.modelInfo).toBeDefined()
    })

    it('应该验证任务ID格式', async () => {
      await expect(federatedTaskService.getTaskResults('invalid-id')).rejects.toThrow('任务ID格式不正确')
    })
  })

  describe('getTaskLogs', () => {
    it('应该成功获取任务日志', async () => {
      const taskId = mockFederatedTasks[0].taskId
      const params: TaskLogsParams = { page: 1, size: 10 }
      const mockResponse = mockFederatedTaskApi.getTaskLogs(taskId, params).data
      
      vi.mocked(federatedTask.getTaskLogs).mockResolvedValue(mockResponse)
      
      const result = await federatedTaskService.getTaskLogs(taskId, params)
      
      expect(federatedTask.getTaskLogs).toHaveBeenCalledWith(taskId, params)
      expect(result.taskId).toBe(taskId)
      expect(result.total).toBeGreaterThanOrEqual(0)
      expect(result.page).toBe(1)
      expect(result.size).toBe(10)
      expect(Array.isArray(result.logs)).toBe(true)
    })

    it('应该支持日志级别过滤', async () => {
      const taskId = mockFederatedTasks[0].taskId
      const params: TaskLogsParams = { level: 'ERROR' }
      const mockResponse = mockFederatedTaskApi.getTaskLogs(taskId, params).data
      
      vi.mocked(federatedTask.getTaskLogs).mockResolvedValue(mockResponse)
      
      const result = await federatedTaskService.getTaskLogs(taskId, params)
      
      expect(federatedTask.getTaskLogs).toHaveBeenCalledWith(taskId, params)
      expect(result.logs.every(log => log.level === 'ERROR' || result.logs.length === 0)).toBe(true)
    })

    it('应该验证日志查询参数', async () => {
      const taskId = mockFederatedTasks[0].taskId
      const invalidParams: TaskLogsParams = { size: 1001 } // 超出限制
      
      await expect(federatedTaskService.getTaskLogs(taskId, invalidParams)).rejects.toThrow('每页大小必须在1-1000范围内')
    })

    it('应该验证时间范围', async () => {
      const taskId = mockFederatedTasks[0].taskId
      const invalidParams: TaskLogsParams = {
        startTime: '2024-01-02T00:00:00Z',
        endTime: '2024-01-01T00:00:00Z' // 结束时间早于开始时间
      }
      
      await expect(federatedTaskService.getTaskLogs(taskId, invalidParams)).rejects.toThrow('开始时间不能晚于结束时间')
    })
  })

  describe('deleteTask', () => {
    it('应该成功删除联邦学习任务', async () => {
      const taskId = mockFederatedTasks[0].taskId
      const deleteOptions: DeleteTaskRequest = {
        deleteData: true,
        deleteModel: false
      }
      
      const mockResponse = mockFederatedTaskApi.deleteTask(taskId, deleteOptions).data
      vi.mocked(federatedTask.deleteTask).mockResolvedValue(mockResponse)
      
      const result = await federatedTaskService.deleteTask(taskId, deleteOptions)
      
      expect(federatedTask.deleteTask).toHaveBeenCalledWith(taskId, deleteOptions)
      expect(result.taskId).toBe(taskId)
      expect(result.deletedAt).toBeDefined()
      expect(result.dataDeleted).toBe(true)
      expect(result.modelPreserved).toBe(true)
    })

    it('应该支持无选项删除', async () => {
      const taskId = mockFederatedTasks[0].taskId
      const mockResponse = mockFederatedTaskApi.deleteTask(taskId).data
      
      vi.mocked(federatedTask.deleteTask).mockResolvedValue(mockResponse)
      
      const result = await federatedTaskService.deleteTask(taskId)
      
      expect(federatedTask.deleteTask).toHaveBeenCalledWith(taskId, undefined)
      expect(result.taskId).toBe(taskId)
    })

    it('应该验证任务ID', async () => {
      await expect(federatedTaskService.deleteTask('')).rejects.toThrow('任务ID不能为空')
    })
  })

  describe('错误处理', () => {
    it('应该正确处理API响应错误', async () => {
      const taskId = 'c3d4e5f6789012345678901234567890'
      const error = {
        response: {
          data: {
            message: 'API错误信息'
          }
        }
      }
      
      vi.mocked(federatedTask.getTaskDetail).mockRejectedValue(error)
      
      await expect(federatedTaskService.getTaskDetail(taskId)).rejects.toThrow('获取联邦学习任务详情失败 (ID: c3d4e5f6789012345678901234567890): API错误信息')
    })

    it('应该正确处理通用错误', async () => {
      const taskId = 'c3d4e5f6789012345678901234567890'
      const error = new Error('网络连接失败')
      
      vi.mocked(federatedTask.getTaskDetail).mockRejectedValue(error)
      
      await expect(federatedTaskService.getTaskDetail(taskId)).rejects.toThrow('获取联邦学习任务详情失败 (ID: c3d4e5f6789012345678901234567890): 网络连接失败')
    })

    it('应该处理未知错误', async () => {
      const taskId = 'c3d4e5f6789012345678901234567890'
      vi.mocked(federatedTask.getTaskDetail).mockRejectedValue('未知错误')
      
      await expect(federatedTaskService.getTaskDetail(taskId)).rejects.toThrow('获取联邦学习任务详情失败')
    })
  })

  describe('数据转换', () => {
    it('应该正确转换联邦学习任务数据', async () => {
      const params: TaskListParams = { page: 1, size: 10 }
      const mockResponse = mockFederatedTaskApi.getTaskList(params).data
      
      vi.mocked(federatedTask.getTaskList).mockResolvedValue(mockResponse)
      
      const result = await federatedTaskService.getTaskList(params)
      
      expect(result.tasks).toEqual(mockResponse.tasks)
      result.tasks.forEach(task => {
        expect(task.taskId).toBeDefined()
        expect(task.taskName).toBeDefined()
        expect(task.taskType).toBeDefined()
        expect(task.status).toBeDefined()
        expect(task.createdAt).toBeDefined()
        expect(task.participantCount).toBeGreaterThanOrEqual(0)
      })
    })

    it('应该正确转换任务详情数据', async () => {
      const taskId = mockFederatedTasks[0].taskId
      const mockDetail = mockFederatedTaskDetails
      
      vi.mocked(federatedTask.getTaskDetail).mockResolvedValue(mockDetail)
      
      const result = await federatedTaskService.getTaskDetail(taskId)
      
      expect(result).toEqual(mockDetail)
      expect(result.taskId).toBe(mockDetail.taskId)
      expect(result.algorithm).toBe(mockDetail.algorithm)
      expect(result.participants).toEqual(mockDetail.participants)
      expect(result.metrics).toEqual(mockDetail.metrics)
    })

    it('应该正确转换任务结果数据', async () => {
      const taskId = mockFederatedTasks[0].taskId
      const mockResults = mockTaskResults
      
      vi.mocked(federatedTask.getTaskResults).mockResolvedValue(mockResults)
      
      const result = await federatedTaskService.getTaskResults(taskId)
      
      expect(result).toEqual(mockResults)
      expect(result.finalResults).toEqual(mockResults.finalResults)
      expect(result.roundResults).toEqual(mockResults.roundResults)
      expect(result.participantResults).toEqual(mockResults.participantResults)
      expect(result.modelInfo).toEqual(mockResults.modelInfo)
    })

    it('应该正确转换任务日志数据', async () => {
      const taskId = mockFederatedTasks[0].taskId
      const params: TaskLogsParams = { page: 1, size: 10 }
      const mockResponse = mockFederatedTaskApi.getTaskLogs(taskId, params).data
      
      vi.mocked(federatedTask.getTaskLogs).mockResolvedValue(mockResponse)
      
      const result = await federatedTaskService.getTaskLogs(taskId, params)
      
      expect(result.logs).toEqual(mockResponse.logs)
      result.logs.forEach(log => {
        expect(log.timestamp).toBeDefined()
        expect(log.level).toBeDefined()
        expect(log.message).toBeDefined()
        expect(log.source).toBeDefined()
      })
    })
  })

  describe('参数验证', () => {
    it('应该验证任务ID格式 - UUID', () => {
      const validIds = ['c3d4e5f6789012345678901234567890', 'a1b2c3d4e5f678901234567890123456']
      const invalidIds = ['invalid-id', '12345', 'c3d4e5f6789012345678901234567890123', '']
      
      validIds.forEach(id => {
        expect(() => federatedTaskService['validateTaskId'](id)).not.toThrow()
      })
      
      invalidIds.forEach(id => {
        expect(() => federatedTaskService['validateTaskId'](id)).toThrow()
      })
    })

    it('应该验证任务名称长度限制', async () => {
      const longName = 'a'.repeat(101) // 超过100字符
      const taskData: CreateTaskRequest = {
        taskName: longName,
        taskType: 'CLASSIFICATION',
        algorithm: 'FEDERATED_AVERAGING',
        participants: [
          { vmId: 'test1', role: 'PARTICIPANT', dataSource: 'data1.csv' },
          { vmId: 'test2', role: 'PARTICIPANT', dataSource: 'data2.csv' }
        ],
        hyperparameters: {
          learningRate: 0.01,
          batchSize: 32,
          epochs: 100,
          rounds: 10,
          minParticipants: 2
        },
        modelConfig: {
          modelType: 'RANDOM_FOREST',
          featureColumns: ['feature_1'],
          targetColumn: 'target',
          testSize: 0.2,
          randomState: 42
        }
      }
      
      await expect(federatedTaskService.createTask(taskData)).rejects.toThrow('任务名称不能超过100个字符')
    })

    it('应该验证模型配置', async () => {
      const taskData: CreateTaskRequest = {
        taskName: '测试任务',
        taskType: 'CLASSIFICATION',
        algorithm: 'FEDERATED_AVERAGING',
        participants: [
          { vmId: 'test1', role: 'PARTICIPANT', dataSource: 'data1.csv' },
          { vmId: 'test2', role: 'PARTICIPANT', dataSource: 'data2.csv' }
        ],
        hyperparameters: {
          learningRate: 0.01,
          batchSize: 32,
          epochs: 100,
          rounds: 10,
          minParticipants: 2
        },
        modelConfig: {
          modelType: 'RANDOM_FOREST',
          featureColumns: [], // 空特征列
          targetColumn: 'target',
          testSize: 0.2,
          randomState: 42
        }
      }
      
      await expect(federatedTaskService.createTask(taskData)).rejects.toThrow('特征列不能为空')
    })

    it('应该验证测试集比例', async () => {
      const taskData: CreateTaskRequest = {
        taskName: '测试任务',
        taskType: 'CLASSIFICATION',
        algorithm: 'FEDERATED_AVERAGING',
        participants: [
          { vmId: 'test1', role: 'PARTICIPANT', dataSource: 'data1.csv' },
          { vmId: 'test2', role: 'PARTICIPANT', dataSource: 'data2.csv' }
        ],
        hyperparameters: {
          learningRate: 0.01,
          batchSize: 32,
          epochs: 100,
          rounds: 10,
          minParticipants: 2
        },
        modelConfig: {
          modelType: 'RANDOM_FOREST',
          featureColumns: ['feature_1'],
          targetColumn: 'target',
          testSize: 1.5, // 无效测试集比例
          randomState: 42
        }
      }
      
      await expect(federatedTaskService.createTask(taskData)).rejects.toThrow('测试集比例必须在0-1之间')
    })
  })
})
