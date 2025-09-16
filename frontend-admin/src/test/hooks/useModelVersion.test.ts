/**
 * useModelVersion Hook 测试
 * 测试模型版本hook的错误处理封装和组件接口
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useModel } from '@/store/model-version/useModelVersionStore'
import { useModelStore } from '@/store/model-version/modelVersionStore'
import {
  mockModelVersionList,
  mockModelVersion,
  mockUploadModelRequest,
  mockEvaluationRequest,
  mockDeploymentRequest,
  mockRollbackRequest,
  mockDeleteModelRequest
} from '@/mocks/store/modelVersionStoreMock'

// Mock the store
vi.mock('@/store/model-version/modelVersionStore')

const mockStore = {
  modelList: [],
  modelListTotal: 0,
  modelListLoading: false,
  modelListError: null,
  currentModel: null,
  currentModelLoading: false,
  currentModelError: null,
  uploadProgress: {},
  uploadError: {},
  evaluationResults: {},
  evaluationLoading: {},
  operationLoading: {},
  operationError: {},
  taskModels: {},
  taskModelsLoading: {},
  modelStatistics: null,
  statisticsLoading: false,
  taskStatistics: {},
  pagination: { page: 1, size: 20, total: 0 },
  queryParams: {},
  fetchModelList: vi.fn(),
  refreshModelList: vi.fn(),
  fetchModelDetail: vi.fn(),
  setCurrentModel: vi.fn(),
  uploadModel: vi.fn(),
  evaluateModel: vi.fn(),
  deployModel: vi.fn(),
  rollbackModel: vi.fn(),
  deleteModel: vi.fn(),
  downloadModel: vi.fn(),
  fetchTaskModels: vi.fn(),
  fetchModelStatistics: vi.fn(),
  fetchTaskStatistics: vi.fn(),
  setPagination: vi.fn(),
  setQueryParams: vi.fn(),
  resetQueryParams: vi.fn(),
  clearError: vi.fn(),
  resetState: vi.fn()
}

describe('useModel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Mock the store selector function
    const mockUseModelStore = vi.mocked(useModelStore)
    mockUseModelStore.mockImplementation((selector: any) => {
      if (typeof selector === 'function') {
        return selector(mockStore)
      }
      return mockStore
    })
  })

  describe('状态访问', () => {
    it('应该正确暴露模型版本状态', () => {
      const { result } = renderHook(() => useModel())
      
      expect(result.current.modelList).toEqual([])
      expect(result.current.modelListTotal).toBe(0)
      expect(result.current.modelListLoading).toBe(false)
      expect(result.current.modelListError).toBe(null)
      expect(result.current.currentModel).toBe(null)
      expect(result.current.currentModelLoading).toBe(false)
      expect(result.current.currentModelError).toBe(null)
      expect(result.current.uploadProgress).toEqual({})
      expect(result.current.uploadError).toEqual({})
      expect(result.current.evaluationResults).toEqual({})
      expect(result.current.evaluationLoading).toEqual({})
      // evaluationError 不在hook接口中
      // deployment相关状态不在hook接口中
      expect(result.current.taskModels).toEqual({})
      expect(result.current.taskModelsLoading).toEqual({})
      // taskModelsError 不在hook接口中
      expect(result.current.modelStatistics).toBe(null)
      expect(result.current.statisticsLoading).toBe(false)
      // statisticsError 不在hook接口中
      expect(result.current.taskStatistics).toEqual({})
      // taskStatistics相关状态不在hook接口中
      expect(result.current.pagination).toEqual({ page: 1, size: 20, total: 0 })
      expect(result.current.queryParams).toEqual({})
    })
  })

  describe('模型列表操作', () => {
    it('应该成功获取模型列表', async () => {
      mockStore.fetchModelList.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useModel())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchModelList()
      })
      
      expect(fetchResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchModelList).toHaveBeenCalled()
    })

    it('应该处理获取模型列表失败', async () => {
      const error = new Error('获取模型列表失败')
      mockStore.fetchModelList.mockRejectedValue(error)
      
      const { result } = renderHook(() => useModel())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchModelList()
      })
      
      expect(fetchResult).toEqual({ success: false, error: '获取模型列表失败' })
    })

    it('应该成功刷新模型列表', async () => {
      mockStore.refreshModelList.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useModel())
      
      let refreshResult
      await act(async () => {
        refreshResult = await result.current.refreshModelList()
      })
      
      expect(refreshResult).toEqual({ success: true, error: null })
      expect(mockStore.refreshModelList).toHaveBeenCalled()
    })

    it('应该支持带参数的模型列表获取', async () => {
      mockStore.fetchModelList.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useModel())
      
      const params = {
        taskId: 'task-001',
        status: 'DEPLOYED' as const,
        framework: 'PYTORCH' as const
      }
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchModelList(params)
      })
      
      expect(fetchResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchModelList).toHaveBeenCalledWith(params)
    })
  })

  describe('模型详情操作', () => {
    it('应该成功获取模型详情', async () => {
      mockStore.fetchModelDetail.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useModel())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchModelDetail('model-001')
      })
      
      expect(fetchResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchModelDetail).toHaveBeenCalledWith('model-001')
    })

    it('应该处理获取模型详情失败', async () => {
      const error = new Error('获取模型详情失败')
      mockStore.fetchModelDetail.mockRejectedValue(error)
      
      const { result } = renderHook(() => useModel())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchModelDetail('model-001')
      })
      
      expect(fetchResult).toEqual({ success: false, error: '获取模型详情失败' })
    })

    it('应该设置当前模型', () => {
      const { result } = renderHook(() => useModel())
      
      const testModel = {
        modelId: 'model-001',
        taskId: 'task-001',
        modelName: '测试模型',
        version: '1.0.0',
        framework: 'PYTORCH' as const,
        status: 'TRAINED' as const,
        roundNumber: 10,
        createdAt: '2024-01-15T10:00:00Z',
        createdBy: 'user-001',
        aggregationMethod: 'FedAvg' as const,
        clientCount: 3,
        modelJson: {},
        metrics: { accuracy: 0.95, loss: 0.1 },
        aggregatedAt: '2024-01-15T10:30:00Z'
      }
      
      act(() => {
        result.current.setCurrentModel(testModel)
      })
      
      expect(mockStore.setCurrentModel).toHaveBeenCalledWith(testModel)
    })
  })

  describe('模型管理操作', () => {
    it('应该成功上传模型', async () => {
      const mockResponse = {
        modelId: 'model-new-001',
        taskId: 'task-001',
        roundNumber: 1,
        status: 'UPLOADING' as const,
        description: '新上传的模型',
        parameters: {},
        createdAt: '2024-01-15T10:00:00Z'
      }
      mockStore.uploadModel.mockResolvedValue(mockResponse)
      
      const { result } = renderHook(() => useModel())
      
      let uploadResult
      await act(async () => {
        // 创建 FormData 对象来模拟文件上传
        const formData = new FormData()
        formData.append('modelFile', new File(['test'], 'model.pth'))
        formData.append('taskId', 'task-001')
        formData.append('roundNumber', '1')
        uploadResult = await result.current.uploadModel(formData)
      })
      
      expect(uploadResult).toEqual({ success: true, error: null, data: mockResponse })
      expect(mockStore.uploadModel).toHaveBeenCalled()
    })

    it('应该处理上传模型失败', async () => {
      const error = new Error('上传模型失败')
      mockStore.uploadModel.mockRejectedValue(error)
      
      const { result } = renderHook(() => useModel())
      
      let uploadResult
      await act(async () => {
        // 创建 FormData 对象来模拟文件上传
        const formData = new FormData()
        formData.append('modelFile', new File(['test'], 'model.pth'))
        formData.append('taskId', 'task-001')
        formData.append('roundNumber', '1')
        uploadResult = await result.current.uploadModel(formData)
      })
      
      expect(uploadResult).toEqual({ success: false, error: '上传模型失败', data: null })
    })

    it('应该成功评估模型', async () => {
      mockStore.evaluateModel.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useModel())
      
      let evaluateResult
      await act(async () => {
        evaluateResult = await result.current.evaluateModel('model-001', mockEvaluationRequest)
      })
      
      expect(evaluateResult).toEqual({ success: true, error: null })
      expect(mockStore.evaluateModel).toHaveBeenCalledWith('model-001', mockEvaluationRequest)
    })

    it('应该成功部署模型', async () => {
      const mockResponse = {
        deploymentId: 'deploy-001',
        modelId: 'model-001',
        deploymentName: '生产部署',
        targetVms: ['vm-001', 'vm-002'],
        status: 'DEPLOYING' as const,
        deploymentConfig: {},
        createdAt: '2024-01-15T10:00:00Z'
      }
      mockStore.deployModel.mockResolvedValue(mockResponse)
      
      const { result } = renderHook(() => useModel())
      
      let deployResult
      await act(async () => {
        deployResult = await result.current.deployModel('model-001', mockDeploymentRequest)
      })
      
      expect(deployResult).toEqual({ success: true, error: null, data: mockResponse })
      expect(mockStore.deployModel).toHaveBeenCalledWith('model-001', mockDeploymentRequest)
    })

    it('应该成功回滚模型', async () => {
      mockStore.rollbackModel.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useModel())
      
      let rollbackResult
      await act(async () => {
        rollbackResult = await result.current.rollbackModel('model-001', mockRollbackRequest)
      })
      
      expect(rollbackResult).toEqual({ success: true, error: null })
      expect(mockStore.rollbackModel).toHaveBeenCalledWith('model-001', mockRollbackRequest)
    })

    it('应该成功删除模型', async () => {
      mockStore.deleteModel.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useModel())
      
      let deleteResult
      await act(async () => {
        deleteResult = await result.current.deleteModel('model-001', mockDeleteModelRequest)
      })
      
      expect(deleteResult).toEqual({ success: true, error: null })
      expect(mockStore.deleteModel).toHaveBeenCalledWith('model-001', mockDeleteModelRequest)
    })

    it('应该成功下载模型', async () => {
      mockStore.downloadModel.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useModel())
      
      let downloadResult
      await act(async () => {
        downloadResult = await result.current.downloadModel('model-001')
      })
      
      expect(downloadResult).toEqual({ success: true, error: null })
      expect(mockStore.downloadModel).toHaveBeenCalledWith('model-001', undefined)
    })
  })

  describe('统计信息操作', () => {
    it('应该成功获取任务模型', async () => {
      mockStore.fetchTaskModels.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useModel())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchTaskModels('task-001')
      })
      
      expect(fetchResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchTaskModels).toHaveBeenCalledWith('task-001', undefined)
    })

    it('应该成功获取模型统计', async () => {
      mockStore.fetchModelStatistics.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useModel())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchModelStatistics()
      })
      
      expect(fetchResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchModelStatistics).toHaveBeenCalled()
    })

    it('应该成功获取任务统计', async () => {
      mockStore.fetchTaskStatistics.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useModel())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchTaskStatistics('task-001')
      })
      
      expect(fetchResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchTaskStatistics).toHaveBeenCalledWith('task-001')
    })
  })

  describe('状态管理操作', () => {
    it('应该成功设置分页参数', () => {
      const { result } = renderHook(() => useModel())
      
      act(() => {
        result.current.setPagination(2, 50)
      })
      
      expect(mockStore.setPagination).toHaveBeenCalledWith(2, 50)
    })

    it('应该成功设置查询参数', () => {
      const { result } = renderHook(() => useModel())
      
      const params = {
        taskId: 'task-001',
        status: 'DEPLOYED' as const,
        framework: 'PYTORCH' as const
      }
      
      act(() => {
        result.current.setQueryParams(params)
      })
      
      expect(mockStore.setQueryParams).toHaveBeenCalledWith(params)
    })

    it('应该成功重置查询参数', () => {
      const { result } = renderHook(() => useModel())
      
      act(() => {
        result.current.resetQueryParams()
      })
      
      expect(mockStore.resetQueryParams).toHaveBeenCalled()
    })

    it('应该成功清除错误', () => {
      const { result } = renderHook(() => useModel())
      
      act(() => {
        result.current.clearError()
      })
      
      expect(mockStore.clearError).toHaveBeenCalled()
    })

    it('应该成功重置状态', () => {
      const { result } = renderHook(() => useModel())
      
      act(() => {
        result.current.resetState()
      })
      
      expect(mockStore.resetState).toHaveBeenCalled()
    })
  })

  describe('错误处理', () => {
    it('应该处理非Error类型的异常', async () => {
      mockStore.fetchModelList.mockRejectedValue('string error')
      
      const { result } = renderHook(() => useModel())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchModelList()
      })
      
      expect(fetchResult).toEqual({ success: false, error: '获取模型列表失败' })
    })

    it('应该处理undefined异常', async () => {
      mockStore.uploadModel.mockRejectedValue(undefined)
      
      const { result } = renderHook(() => useModel())
      
      let uploadResult
      await act(async () => {
        // 创建 FormData 对象来模拟文件上传
        const formData = new FormData()
        formData.append('modelFile', new File(['test'], 'model.pth'))
        formData.append('taskId', 'task-001')
        formData.append('roundNumber', '1')
        uploadResult = await result.current.uploadModel(formData)
      })
      
      expect(uploadResult).toEqual({ success: false, error: '模型上传失败', data: null })
    })
  })
})
