/**
 * useFederatedTask Hook 测试
 * 测试联邦任务hook的错误处理封装和组件接口
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useTask } from '@/store/federated-task/useFederatedTaskStore'
import { useTaskStore } from '@/store/federated-task/federatedTaskStore'
import {
  mockTaskList,
  mockTaskDetails,
  mockCreateTaskRequest,
  mockTaskLogs
} from '@/mocks/store/federatedTaskStoreMock'

// Mock the store
vi.mock('@/store/federated-task/federatedTaskStore')

const mockStore = {
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

  realtimeData: {},
  pagination: { page: 1, size: 20, total: 0 },
  queryParams: {},
  fetchTaskList: vi.fn(),
  refreshTaskList: vi.fn(),
  fetchTaskDetail: vi.fn(),
  setCurrentTask: vi.fn(),
  createTask: vi.fn(),
  configTask: vi.fn(),
  startTask: vi.fn(),
  pauseTask: vi.fn(),
  resumeTask: vi.fn(),
  stopTask: vi.fn(),
  cancelTask: vi.fn(),
  deleteTask: vi.fn(),
  fetchTaskResults: vi.fn(),
  fetchTaskLogs: vi.fn(),
  setPagination: vi.fn(),
  setQueryParams: vi.fn(),
  resetQueryParams: vi.fn(),
  updateRealtimeData: vi.fn(),
  clearError: vi.fn(),
  clearTaskError: vi.fn(),
  resetState: vi.fn()
}

describe('useTask', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Mock the store selector function
    const mockUseTaskStore = vi.mocked(useTaskStore)
    mockUseTaskStore.mockImplementation((selector: any) => {
      if (typeof selector === 'function') {
        return selector(mockStore)
      }
      return mockStore
    })
  })

  describe('状态访问', () => {
    it('应该正确暴露联邦任务状态', () => {
      const { result } = renderHook(() => useTask())
      
      expect(result.current.taskList).toEqual([])
      expect(result.current.taskListTotal).toBe(0)
      expect(result.current.taskListLoading).toBe(false)
      expect(result.current.taskListError).toBe(null)
      expect(result.current.currentTask).toBe(null)
      expect(result.current.currentTaskLoading).toBe(false)
      expect(result.current.currentTaskError).toBe(null)
      expect(result.current.taskResults).toEqual({})
      // taskResultsLoading 和 taskResultsError 不在hook接口中
      expect(result.current.taskLogs).toEqual({})
      expect(result.current.taskLogsLoading).toEqual({})
      // taskLogsError 不在hook接口中
      expect(result.current.realtimeData).toEqual({})
      expect(result.current.pagination).toEqual({ page: 1, size: 20, total: 0 })
      expect(result.current.queryParams).toEqual({})
    })
  })

  describe('任务列表操作', () => {
    it('应该成功获取任务列表', async () => {
      mockStore.fetchTaskList.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useTask())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchTaskList()
      })
      
      expect(fetchResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchTaskList).toHaveBeenCalled()
    })

    it('应该处理获取任务列表失败', async () => {
      const error = new Error('获取任务列表失败')
      mockStore.fetchTaskList.mockRejectedValue(error)
      
      const { result } = renderHook(() => useTask())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchTaskList()
      })
      
      expect(fetchResult).toEqual({ success: false, error: '获取任务列表失败' })
    })

    it('应该成功刷新任务列表', async () => {
      mockStore.refreshTaskList.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useTask())
      
      let refreshResult
      await act(async () => {
        refreshResult = await result.current.refreshTaskList()
      })
      
      expect(refreshResult).toEqual({ success: true, error: null })
      expect(mockStore.refreshTaskList).toHaveBeenCalled()
    })

    it('应该支持带参数的任务列表获取', async () => {
      mockStore.fetchTaskList.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useTask())
      
      const params = {
        status: 'RUNNING' as const,
        algorithm: 'FedAvg' as const,
        createdBy: 'user-001'
      }
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchTaskList(params)
      })
      
      expect(fetchResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchTaskList).toHaveBeenCalledWith(params)
    })
  })

  describe('任务详情操作', () => {
    it('应该成功获取任务详情', async () => {
      mockStore.fetchTaskDetail.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useTask())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchTaskDetail('task-001')
      })
      
      expect(fetchResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchTaskDetail).toHaveBeenCalledWith('task-001')
    })

    it('应该处理获取任务详情失败', async () => {
      const error = new Error('获取任务详情失败')
      mockStore.fetchTaskDetail.mockRejectedValue(error)
      
      const { result } = renderHook(() => useTask())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchTaskDetail('task-001')
      })
      
      expect(fetchResult).toEqual({ success: false, error: '获取任务详情失败' })
    })

    it('应该设置当前任务', () => {
      const { result } = renderHook(() => useTask())
      
      const testTask = {
        taskId: 'task-001',
        taskName: '测试任务',
        algorithm: 'FedAvg' as const,
        status: 'CREATED' as const,
        totalRounds: 10,
        currentRound: 0,
        participantCount: 3,
        createdAt: '2024-01-15T10:00:00Z',
        createdBy: 'user-001',
        estimatedDuration: 3600,
        participants: [
          { vmId: 'vm-001', status: 'IDLE' as const, joinedAt: '2024-01-15T10:00:00Z', role: 'PARTICIPANT' as const, dataSource: 'ACOUSTIC' as const },
          { vmId: 'vm-002', status: 'IDLE' as const, joinedAt: '2024-01-15T10:00:00Z', role: 'PARTICIPANT' as const, dataSource: 'ACOUSTIC' as const },
          { vmId: 'vm-003', status: 'IDLE' as const, joinedAt: '2024-01-15T10:00:00Z', role: 'PARTICIPANT' as const, dataSource: 'ACOUSTIC' as const }
        ],
        taskType: 'CLASSIFICATION' as const
      }
      
      act(() => {
        result.current.setCurrentTask(testTask)
      })
      
      expect(mockStore.setCurrentTask).toHaveBeenCalledWith(testTask)
    })
  })

  describe('任务管理操作', () => {
    it('应该成功创建任务', async () => {
      const mockTaskId = 'task-new-001'
      mockStore.createTask.mockResolvedValue(mockTaskId)
      
      const { result } = renderHook(() => useTask())
      
      let createResult
      await act(async () => {
        createResult = await result.current.createTask(mockCreateTaskRequest)
      })
      
      expect(createResult).toEqual({ success: true, error: null, data: mockTaskId })
      expect(mockStore.createTask).toHaveBeenCalledWith(mockCreateTaskRequest)
    })

    it('应该处理创建任务失败', async () => {
      const error = new Error('创建任务失败')
      mockStore.createTask.mockRejectedValue(error)
      
      const { result } = renderHook(() => useTask())
      
      let createResult
      await act(async () => {
        createResult = await result.current.createTask(mockCreateTaskRequest)
      })
      
      expect(createResult).toEqual({ success: false, error: '创建任务失败', data: null })
    })

    it('应该成功配置任务', async () => {
      mockStore.configTask.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useTask())
      
      const configRequest = {
        algorithm: 'FedAvg' as const,
        rounds: 20,
        participants: ['vm-001', 'vm-002', 'vm-003'],
        hyperparameters: {
          learningRate: 0.01,
          batchSize: 32,
          epochs: 5
        }
      }
      
      let configResult
      await act(async () => {
        configResult = await result.current.configTask('task-001', configRequest)
      })
      
      expect(configResult).toEqual({ success: true, error: null })
      expect(mockStore.configTask).toHaveBeenCalledWith('task-001', configRequest)
    })

    it('应该成功启动任务', async () => {
      mockStore.startTask.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useTask())
      
      let startResult
      await act(async () => {
        startResult = await result.current.startTask('task-001')
      })
      
      expect(startResult).toEqual({ success: true, error: null })
      expect(mockStore.startTask).toHaveBeenCalledWith('task-001')
    })

    it('应该成功暂停任务', async () => {
      mockStore.pauseTask.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useTask())
      
      let pauseResult
      await act(async () => {
        pauseResult = await result.current.pauseTask('task-001')
      })
      
      expect(pauseResult).toEqual({ success: true, error: null })
      expect(mockStore.pauseTask).toHaveBeenCalledWith('task-001')
    })

    it('应该成功恢复任务', async () => {
      mockStore.resumeTask.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useTask())
      
      let resumeResult
      await act(async () => {
        resumeResult = await result.current.resumeTask('task-001')
      })
      
      expect(resumeResult).toEqual({ success: true, error: null })
      expect(mockStore.resumeTask).toHaveBeenCalledWith('task-001')
    })

    it('应该成功停止任务', async () => {
      mockStore.stopTask.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useTask())
      
      let stopResult
      await act(async () => {
        stopResult = await result.current.stopTask('task-001')
      })
      
      expect(stopResult).toEqual({ success: true, error: null })
      expect(mockStore.stopTask).toHaveBeenCalledWith('task-001', undefined)
    })

    it('应该成功取消任务', async () => {
      mockStore.cancelTask.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useTask())
      
      let cancelResult
      await act(async () => {
        cancelResult = await result.current.cancelTask('task-001')
      })
      
      expect(cancelResult).toEqual({ success: true, error: null })
      expect(mockStore.cancelTask).toHaveBeenCalledWith('task-001', undefined)
    })

    it('应该成功删除任务', async () => {
      mockStore.deleteTask.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useTask())
      
      let deleteResult
      await act(async () => {
        deleteResult = await result.current.deleteTask('task-001')
      })
      
      expect(deleteResult).toEqual({ success: true, error: null })
      expect(mockStore.deleteTask).toHaveBeenCalledWith('task-001', undefined)
    })
  })

  describe('任务结果和日志操作', () => {
    it('应该成功获取任务结果', async () => {
      mockStore.fetchTaskResults.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useTask())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchTaskResults('task-001')
      })
      
      expect(fetchResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchTaskResults).toHaveBeenCalledWith('task-001')
    })

    it('应该成功获取任务日志', async () => {
      mockStore.fetchTaskLogs.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useTask())
      
      const params = {
        level: 'INFO' as const,
        startTime: '2024-01-15T10:00:00Z',
        endTime: '2024-01-15T11:00:00Z'
      }
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchTaskLogs('task-001', params)
      })
      
      expect(fetchResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchTaskLogs).toHaveBeenCalledWith('task-001', params)
    })

    it('应该处理获取任务结果失败', async () => {
      const error = new Error('获取任务结果失败')
      mockStore.fetchTaskResults.mockRejectedValue(error)
      
      const { result } = renderHook(() => useTask())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchTaskResults('task-001')
      })
      
      expect(fetchResult).toEqual({ success: false, error: '获取任务结果失败' })
    })
  })

  describe('状态管理操作', () => {
    it('应该成功设置分页参数', () => {
      const { result } = renderHook(() => useTask())
      
      act(() => {
        result.current.setPagination(2, 50)
      })
      
      expect(mockStore.setPagination).toHaveBeenCalledWith(2, 50)
    })

    it('应该成功设置查询参数', () => {
      const { result } = renderHook(() => useTask())
      
      const params = {
        status: 'RUNNING' as const,
        algorithm: 'FedAvg' as const,
        createdBy: 'user-001'
      }
      
      act(() => {
        result.current.setQueryParams(params)
      })
      
      expect(mockStore.setQueryParams).toHaveBeenCalledWith(params)
    })

    it('应该成功重置查询参数', () => {
      const { result } = renderHook(() => useTask())
      
      act(() => {
        result.current.resetQueryParams()
      })
      
      expect(mockStore.resetQueryParams).toHaveBeenCalled()
    })

    it('应该成功更新实时数据', () => {
      const { result } = renderHook(() => useTask())
      
      const realtimeData = {
        taskId: 'task-001',
        currentRound: 5,
        participantStatus: {
          'vm-001': 'TRAINING',
          'vm-002': 'COMPLETED',
          'vm-003': 'TRAINING'
        },
        metrics: {
          accuracy: 0.85,
          loss: 0.23
        }
      }
      
      act(() => {
        result.current.updateRealtimeData('task-001', realtimeData)
      })
      
      expect(mockStore.updateRealtimeData).toHaveBeenCalledWith('task-001', realtimeData)
    })

    it('应该成功清除错误', () => {
      const { result } = renderHook(() => useTask())
      
      act(() => {
        result.current.clearError()
      })
      
      expect(mockStore.clearError).toHaveBeenCalled()
    })

    it('应该成功清除特定任务的错误', () => {
      const { result } = renderHook(() => useTask())
      
      act(() => {
        result.current.clearTaskError('task-001')
      })
      
      expect(mockStore.clearTaskError).toHaveBeenCalledWith('task-001')
    })

    it('应该成功重置状态', () => {
      const { result } = renderHook(() => useTask())
      
      act(() => {
        result.current.resetState()
      })
      
      expect(mockStore.resetState).toHaveBeenCalled()
    })
  })

  describe('错误处理', () => {
    it('应该处理非Error类型的异常', async () => {
      mockStore.fetchTaskList.mockRejectedValue('string error')
      
      const { result } = renderHook(() => useTask())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchTaskList()
      })
      
      expect(fetchResult).toEqual({ success: false, error: '获取任务列表失败' })
    })

    it('应该处理undefined异常', async () => {
      mockStore.createTask.mockRejectedValue(undefined)
      
      const { result } = renderHook(() => useTask())
      
      let createResult
      await act(async () => {
        createResult = await result.current.createTask(mockCreateTaskRequest)
      })
      
      expect(createResult).toEqual({ success: false, error: '创建任务失败', data: null })
    })
  })
})
