/**
 * Federated Task Store 测试
 * 测试联邦学习任务状态管理的各种功能，包括任务创建、控制、监控、错误处理等
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useTaskStore } from '@/store/federated-task/federatedTaskStore'
import { federatedTaskService } from '@/services'
import {
  mockTaskList,
  mockTask,
  mockTaskDetails,
  mockTaskResults,
  mockTaskLogs,
  mockCreateTaskRequest,
  mockFederatedTaskErrors,
  mockInitialTaskState
} from '@/mocks/store/federatedTaskStoreMock'

// Mock federatedTaskService
vi.mock('@/services', () => ({
  federatedTaskService: {
    getTaskList: vi.fn(),
    getTaskDetail: vi.fn(),
    createTask: vi.fn(),
    configTask: vi.fn(),
    startTask: vi.fn(),
    pauseTask: vi.fn(),
    resumeTask: vi.fn(),
    stopTask: vi.fn(),
    cancelTask: vi.fn(),
    deleteTask: vi.fn(),
    getTaskResults: vi.fn(),
    getTaskLogs: vi.fn()
  }
}))

const mocked = vi.mocked

describe('FederatedTaskStore', () => {
  beforeEach(() => {
    // 重置所有mock
    vi.clearAllMocks()
    
    // 重置store状态
    const store = useTaskStore.getState()
    store.resetState()
  })

  describe('初始状态', () => {
    it('应该有正确的初始状态', () => {
      const state = useTaskStore.getState()
      
      expect(state.taskList).toEqual([])
      expect(state.taskListTotal).toBe(0)
      expect(state.taskListLoading).toBe(false)
      expect(state.taskListError).toBe(null)
      expect(state.currentTask).toBe(null)
      expect(state.currentTaskLoading).toBe(false)
      expect(state.currentTaskError).toBe(null)
      expect(state.taskResults).toEqual({})
      expect(state.taskLogs).toEqual({})
      expect(state.taskLogsLoading).toEqual({})
      expect(state.operationLoading).toEqual({})
      expect(state.operationError).toEqual({})
      expect(state.createTaskLoading).toBe(false)
      expect(state.createTaskError).toBe(null)
      expect(state.pagination).toEqual({
        page: 1,
        size: 20,
        total: 0
      })
      expect(state.queryParams).toEqual({})
      expect(state.realtimeData).toEqual({})
    })
  })

  describe('fetchTaskList', () => {
    it('应该成功获取任务列表', async () => {
      const mockResponse = {
        tasks: mockTaskList,
        total: mockTaskList.length,
        page: 1,
        size: 20
      }
      
      mocked(federatedTaskService.getTaskList).mockResolvedValue(mockResponse)
      
      const store = useTaskStore.getState()
      await store.fetchTaskList()
      
      const state = useTaskStore.getState()
      expect(state.taskList).toEqual(mockTaskList)
      expect(state.taskListTotal).toBe(mockTaskList.length)
      expect(state.taskListLoading).toBe(false)
      expect(state.taskListError).toBe(null)
      expect(state.pagination.page).toBe(1)
      expect(state.pagination.total).toBe(mockTaskList.length)
    })

    it('应该处理加载状态', async () => {
      const pendingPromise = new Promise<any>(() => {}) // Never resolves
      mocked(federatedTaskService.getTaskList).mockReturnValue(pendingPromise)
      
      const store = useTaskStore.getState()
      store.fetchTaskList()
      
      const state = useTaskStore.getState()
      expect(state.taskListLoading).toBe(true)
      expect(state.taskListError).toBe(null)
    })

    it('应该处理错误情况', async () => {
      const error = mockFederatedTaskErrors.TASK_NOT_FOUND
      mocked(federatedTaskService.getTaskList).mockRejectedValue(error)
      
      const store = useTaskStore.getState()
      
      await expect(store.fetchTaskList()).rejects.toThrow(error)
      
      const state = useTaskStore.getState()
      expect(state.taskListLoading).toBe(false)
      expect(state.taskListError).toBe(error.message)
    })
  })

  describe('refreshTaskList', () => {
    it('应该刷新任务列表', async () => {
      const mockResponse = {
        tasks: mockTaskList,
        total: mockTaskList.length,
        page: 1,
        size: 20
      }
      
      mocked(federatedTaskService.getTaskList).mockResolvedValue(mockResponse)
      
      const store = useTaskStore.getState()
      await store.refreshTaskList()
      
      const state = useTaskStore.getState()
      expect(state.taskList).toEqual(mockTaskList)
      expect(federatedTaskService.getTaskList).toHaveBeenCalledWith({
        page: 1,
        size: 20
      })
    })
  })

  describe('fetchTaskDetail', () => {
    it('应该成功获取任务详情', async () => {
      mocked(federatedTaskService.getTaskDetail).mockResolvedValue(mockTaskDetails)
      
      const store = useTaskStore.getState()
      await store.fetchTaskDetail('task-001')
      
      const state = useTaskStore.getState()
      expect(state.currentTask).toEqual(mockTaskDetails)
      expect(state.currentTaskLoading).toBe(false)
      expect(state.currentTaskError).toBe(null)
    })

    it('应该处理加载状态', async () => {
      const pendingPromise = new Promise<any>(() => {})
      mocked(federatedTaskService.getTaskDetail).mockReturnValue(pendingPromise)
      
      const store = useTaskStore.getState()
      store.fetchTaskDetail('task-001')
      
      const state = useTaskStore.getState()
      expect(state.currentTaskLoading).toBe(true)
      expect(state.currentTaskError).toBe(null)
    })

    it('应该处理错误情况', async () => {
      const error = mockFederatedTaskErrors.TASK_NOT_FOUND
      mocked(federatedTaskService.getTaskDetail).mockRejectedValue(error)
      
      const store = useTaskStore.getState()
      
      await expect(store.fetchTaskDetail('task-001')).rejects.toThrow(error)
      
      const state = useTaskStore.getState()
      expect(state.currentTaskLoading).toBe(false)
      expect(state.currentTaskError).toBe(error.message)
    })
  })

  describe('setCurrentTask', () => {
    it('应该设置当前任务', () => {
      const store = useTaskStore.getState()
      
      store.setCurrentTask(mockTaskDetails)
      
      const state = useTaskStore.getState()
      expect(state.currentTask).toEqual(mockTaskDetails)
    })

    it('应该清除当前任务', () => {
      const store = useTaskStore.getState()
      
      // 先设置一个任务
      store.setCurrentTask(mockTaskDetails)
      
      // 然后清除
      store.setCurrentTask(null)
      
      const state = useTaskStore.getState()
      expect(state.currentTask).toBe(null)
    })
  })

  describe('createTask', () => {
    it('应该成功创建任务', async () => {
      const mockResponse = {
        taskId: 'task-new-001',
        taskName: '新创建的任务',
        status: 'CREATED',
        createdAt: '2024-01-15T10:00:00Z',
        createdBy: 'user-001',
        participantCount: 3,
        estimatedDuration: 3600
      }
      mocked(federatedTaskService.createTask).mockResolvedValue(mockResponse)
      mocked(federatedTaskService.getTaskList).mockResolvedValue({
        tasks: [...mockTaskList],
        total: mockTaskList.length,
        page: 1,
        size: 20
      })
      
      const store = useTaskStore.getState()
      const taskId = await store.createTask(mockCreateTaskRequest)
      
      expect(taskId).toBe('task-new-001')
      const state = useTaskStore.getState()
      expect(state.createTaskLoading).toBe(false)
      expect(state.createTaskError).toBe(null)
      expect(federatedTaskService.createTask).toHaveBeenCalledWith(mockCreateTaskRequest)
    })

    it('应该处理创建失败', async () => {
      const error = mockFederatedTaskErrors.TASK_ALREADY_EXISTS
      mocked(federatedTaskService.createTask).mockRejectedValue(error)
      
      const store = useTaskStore.getState()
      
      await expect(store.createTask(mockCreateTaskRequest)).rejects.toThrow(error)
      
      const state = useTaskStore.getState()
      expect(state.createTaskLoading).toBe(false)
      expect(state.createTaskError).toBe(error.message)
    })
  })

  describe('configTask', () => {
    it('应该成功配置任务', async () => {
      mocked(federatedTaskService.configTask).mockResolvedValue(undefined)
      mocked(federatedTaskService.getTaskDetail).mockResolvedValue(mockTaskDetails)
      
      const store = useTaskStore.getState()
      const config = { algorithm: 'FedAvg' }
      
      await store.configTask('task-001', config)
      
      const state = useTaskStore.getState()
      expect(state.operationLoading['config-task-001']).toBe(false)
      expect(federatedTaskService.configTask).toHaveBeenCalledWith('task-001', config)
    })

    it('应该处理配置失败', async () => {
      const error = mockFederatedTaskErrors.TASK_CONFIG_ERROR
      mocked(federatedTaskService.configTask).mockRejectedValue(error)
      
      const store = useTaskStore.getState()
      const config = { algorithm: 'FedAvg' }
      
      await expect(store.configTask('task-001', config)).rejects.toThrow(error)
      
      const state = useTaskStore.getState()
      expect(state.operationLoading['config-task-001']).toBe(false)
      expect(state.operationError['config-task-001']).toBe(error.message)
    })
  })

  describe('startTask', () => {
    it('应该成功启动任务', async () => {
      mocked(federatedTaskService.startTask).mockResolvedValue(undefined)
      mocked(federatedTaskService.getTaskDetail).mockResolvedValue(mockTaskDetails)
      
      const store = useTaskStore.getState()
      
      await store.startTask('task-001')
      
      const state = useTaskStore.getState()
      expect(state.operationLoading['start-task-001']).toBe(false)
      expect(federatedTaskService.startTask).toHaveBeenCalledWith('task-001')
    })

    it('应该处理启动失败', async () => {
      const error = mockFederatedTaskErrors.INSUFFICIENT_CLIENTS
      mocked(federatedTaskService.startTask).mockRejectedValue(error)
      
      const store = useTaskStore.getState()
      
      await expect(store.startTask('task-001')).rejects.toThrow(error)
      
      const state = useTaskStore.getState()
      expect(state.operationLoading['start-task-001']).toBe(false)
      expect(state.operationError['start-task-001']).toBe(error.message)
    })
  })

  describe('pauseTask', () => {
    it('应该成功暂停任务', async () => {
      mocked(federatedTaskService.pauseTask).mockResolvedValue(undefined)
      mocked(federatedTaskService.getTaskDetail).mockResolvedValue(mockTaskDetails)
      
      const store = useTaskStore.getState()
      
      await store.pauseTask('task-001')
      
      const state = useTaskStore.getState()
      expect(state.operationLoading['pause-task-001']).toBe(false)
      expect(federatedTaskService.pauseTask).toHaveBeenCalledWith('task-001')
    })
  })

  describe('resumeTask', () => {
    it('应该成功恢复任务', async () => {
      mocked(federatedTaskService.resumeTask).mockResolvedValue(undefined)
      mocked(federatedTaskService.getTaskDetail).mockResolvedValue(mockTaskDetails)
      
      const store = useTaskStore.getState()
      
      await store.resumeTask('task-001')
      
      const state = useTaskStore.getState()
      expect(state.operationLoading['resume-task-001']).toBe(false)
      expect(federatedTaskService.resumeTask).toHaveBeenCalledWith('task-001')
    })
  })

  describe('stopTask', () => {
    it('应该成功停止任务', async () => {
      mocked(federatedTaskService.stopTask).mockResolvedValue(undefined)
      mocked(federatedTaskService.getTaskDetail).mockResolvedValue(mockTaskDetails)
      
      const store = useTaskStore.getState()
      const stopData = { reason: 'User requested', saveCheckpoint: true }
      
      await store.stopTask('task-001', stopData)
      
      const state = useTaskStore.getState()
      expect(state.operationLoading['stop-task-001']).toBe(false)
      expect(federatedTaskService.stopTask).toHaveBeenCalledWith('task-001', stopData)
    })
  })

  describe('cancelTask', () => {
    it('应该成功取消任务', async () => {
      mocked(federatedTaskService.cancelTask).mockResolvedValue(undefined)
      mocked(federatedTaskService.getTaskDetail).mockResolvedValue(mockTaskDetails)
      
      const store = useTaskStore.getState()
      const cancelData = { reason: 'User cancelled' }
      
      await store.cancelTask('task-001', cancelData)
      
      const state = useTaskStore.getState()
      expect(state.operationLoading['cancel-task-001']).toBe(false)
      expect(federatedTaskService.cancelTask).toHaveBeenCalledWith('task-001', cancelData)
    })
  })

  describe('deleteTask', () => {
    it('应该成功删除任务', async () => {
      mocked(federatedTaskService.deleteTask).mockResolvedValue(undefined)
      
      const store = useTaskStore.getState()
      
      // 先添加一个任务到列表中
      useTaskStore.setState({
        taskList: [...mockTaskList],
        taskListTotal: mockTaskList.length
      })
      
      const deleteOptions = { deleteData: true, deleteModel: false }
      await store.deleteTask('task-001', deleteOptions)
      
      const state = useTaskStore.getState()
      expect(state.operationLoading['delete-task-001']).toBe(false)
      expect(state.taskList.find(t => t.taskId === 'task-001')).toBeUndefined()
      expect(federatedTaskService.deleteTask).toHaveBeenCalledWith('task-001', deleteOptions)
    })
  })

  describe('fetchTaskResults', () => {
    it('应该成功获取任务结果', async () => {
      mocked(federatedTaskService.getTaskResults).mockResolvedValue(mockTaskResults)
      
      const store = useTaskStore.getState()
      await store.fetchTaskResults('task-001')
      
      const state = useTaskStore.getState()
      expect(state.taskResults['task-001']).toEqual(mockTaskResults)
    })
  })

  describe('fetchTaskLogs', () => {
    it('应该成功获取任务日志', async () => {
      const mockLogsResponse = {
        taskId: 'task-001',
        total: mockTaskLogs.length,
        page: 1,
        size: 50,
        logs: mockTaskLogs
      }
      mocked(federatedTaskService.getTaskLogs).mockResolvedValue(mockLogsResponse)
      
      const store = useTaskStore.getState()
      const params = { level: 'INFO', page: 1, size: 50 }
      
      await store.fetchTaskLogs('task-001', params)
      
      const state = useTaskStore.getState()
      expect(state.taskLogs['task-001']).toEqual(mockTaskLogs)
      expect(state.taskLogsLoading['task-001']).toBe(false)
      expect(federatedTaskService.getTaskLogs).toHaveBeenCalledWith('task-001', params)
    })

    it('应该处理日志获取失败', async () => {
      const error = new Error('获取日志失败')
      mocked(federatedTaskService.getTaskLogs).mockRejectedValue(error)
      
      const store = useTaskStore.getState()
      
      await expect(store.fetchTaskLogs('task-001')).rejects.toThrow(error)
      
      const state = useTaskStore.getState()
      expect(state.taskLogsLoading['task-001']).toBe(false)
    })
  })

  describe('分页操作', () => {
    it('应该设置分页参数', () => {
      const store = useTaskStore.getState()
      
      store.setPagination(2, 50)
      
      const state = useTaskStore.getState()
      expect(state.pagination.page).toBe(2)
      expect(state.pagination.size).toBe(50)
    })
  })

  describe('查询参数操作', () => {
    it('应该设置查询参数', () => {
      const store = useTaskStore.getState()
      const params = { status: 'RUNNING' as const, type: 'CLASSIFICATION' as const }
      
      store.setQueryParams(params)
      
      const state = useTaskStore.getState()
      expect(state.queryParams).toEqual(params)
    })

    it('应该重置查询参数', () => {
      const store = useTaskStore.getState()
      
      // 先设置一些参数
      store.setQueryParams({ status: 'RUNNING' as const })
      
      store.resetQueryParams()
      
      const state = useTaskStore.getState()
      expect(state.queryParams).toEqual({})
    })
  })

  describe('实时数据更新', () => {
    it('应该更新实时数据', () => {
      const store = useTaskStore.getState()
      const realtimeData = { currentRound: 5, progress: 50 }
      
      store.updateRealtimeData('task-001', realtimeData)
      
      const state = useTaskStore.getState()
      expect(state.realtimeData['task-001']).toMatchObject(realtimeData)
      expect(state.realtimeData['task-001'].lastUpdated).toBeDefined()
    })
  })

  describe('错误处理', () => {
    it('应该清除错误信息', () => {
      const store = useTaskStore.getState()
      
      // 设置一些错误状态
      useTaskStore.setState({
        taskListError: 'test error',
        currentTaskError: 'current error',
        createTaskError: 'create error',
        operationError: { 'start-task-001': 'start error' }
      })
      
      store.clearError()
      
      const state = useTaskStore.getState()
      expect(state.taskListError).toBe(null)
      expect(state.currentTaskError).toBe(null)
      expect(state.createTaskError).toBe(null)
      expect(state.operationError).toEqual({})
    })

    it('应该清除特定任务的错误信息', () => {
      const store = useTaskStore.getState()
      
      // 设置一些错误状态
      useTaskStore.setState({
        operationError: {
          'start-task-001': 'start error',
          'stop-task-001': 'stop error',
          'start-task-002': 'another error'
        }
      })
      
      store.clearTaskError('task-001')
      
      const state = useTaskStore.getState()
      expect(state.operationError['start-task-001']).toBeUndefined()
      expect(state.operationError['stop-task-001']).toBeUndefined()
      expect(state.operationError['start-task-002']).toBe('another error')
    })
  })

  describe('状态重置', () => {
    it('应该重置所有状态', () => {
      const store = useTaskStore.getState()
      
      // 修改一些状态
      useTaskStore.setState({
        taskList: mockTaskList,
        taskListTotal: 10,
        currentTask: mockTaskDetails,
        taskResults: { 'task-001': mockTaskResults },
        taskLogs: { 'task-001': mockTaskLogs },
        realtimeData: { 'task-001': { progress: 50 } }
      })
      
      store.resetState()
      
      const state = useTaskStore.getState()
      expect(state.taskList).toEqual([])
      expect(state.taskListTotal).toBe(0)
      expect(state.currentTask).toBe(null)
      expect(state.taskResults).toEqual({})
      expect(state.taskLogs).toEqual({})
      expect(state.realtimeData).toEqual({})
    })
  })
})
