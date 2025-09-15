/**
 * Model Version Store 测试
 * 测试模型版本状态管理的各种功能，包括模型上传、版本管理、评估、部署、错误处理等
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useModelStore } from '@/store/model-version'
import { modelVersionService } from '@/services'
import {
  mockModelVersionList,
  mockModelVersion,
  mockTaskModelVersions,
  mockEvaluationResult,
  mockUploadModelRequest,
  mockEvaluationRequest,
  mockDeploymentRequest,
  mockModelVersionErrors
} from '@/mocks/store/modelVersionStoreMock'

// Mock modelVersionService
vi.mock('@/services', () => ({
  modelVersionService: {
    getModelVersions: vi.fn(),
    getModelVersionDetail: vi.fn(),
    uploadModel: vi.fn(),
    evaluateModel: vi.fn(),
    deployModel: vi.fn(),
    rollbackModel: vi.fn(),
    deleteModel: vi.fn(),
    downloadModel: vi.fn(),
    getTaskModelVersions: vi.fn(),
    getModelStatistics: vi.fn(),
    getTaskModelStatistics: vi.fn()
  }
}))

const mocked = vi.mocked

describe('ModelVersionStore', () => {
  beforeEach(() => {
    // 重置所有mock
    vi.clearAllMocks()
    
    // 重置store状态
    const store = useModelStore.getState()
    store.resetState()
  })

  describe('初始状态', () => {
    it('应该有正确的初始状态', () => {
      const state = useModelStore.getState()
      
      expect(state.modelList).toEqual([])
      expect(state.modelListTotal).toBe(0)
      expect(state.modelListLoading).toBe(false)
      expect(state.modelListError).toBe(null)
      expect(state.currentModel).toBe(null)
      expect(state.currentModelLoading).toBe(false)
      expect(state.currentModelError).toBe(null)
      expect(state.taskModels).toEqual({})
      expect(state.taskModelsLoading).toEqual({})
      expect(state.evaluationResults).toEqual({})
      expect(state.evaluationLoading).toEqual({})
      expect(state.uploadLoading).toBe(false)
      expect(state.uploadProgress).toEqual({})
      expect(state.uploadError).toBe(null)
      expect(state.operationLoading).toEqual({})
      expect(state.operationError).toEqual({})
      expect(state.modelStatistics).toBe(null)
      expect(state.statisticsLoading).toBe(false)
      expect(state.taskStatistics).toEqual({})
      expect(state.pagination).toEqual({
        page: 1,
        size: 20,
        total: 0
      })
      expect(state.queryParams).toEqual({})
    })
  })

  describe('fetchModelList', () => {
    it('应该成功获取模型版本列表', async () => {
      const mockResponse = {
        records: mockModelVersionList,
        total: mockModelVersionList.length,
        current: 1,
        size: 20,
        pages: Math.ceil(mockModelVersionList.length / 20)
      }
      
      mocked(modelVersionService.getModelVersions).mockResolvedValue(mockResponse)
      
      const store = useModelStore.getState()
      await store.fetchModelList()
      
      const state = useModelStore.getState()
      expect(state.modelList).toEqual(mockModelVersionList)
      expect(state.modelListTotal).toBe(mockModelVersionList.length)
      expect(state.modelListLoading).toBe(false)
      expect(state.modelListError).toBe(null)
      expect(state.pagination.page).toBe(1)
      expect(state.pagination.total).toBe(mockModelVersionList.length)
    })

    it('应该处理加载状态', async () => {
      const pendingPromise = new Promise<any>(() => {}) // Never resolves
      mocked(modelVersionService.getModelVersions).mockReturnValue(pendingPromise)
      
      const store = useModelStore.getState()
      store.fetchModelList()
      
      const state = useModelStore.getState()
      expect(state.modelListLoading).toBe(true)
      expect(state.modelListError).toBe(null)
    })

    it('应该处理错误情况', async () => {
      const error = mockModelVersionErrors.FETCH_MODEL_LIST_ERROR
      mocked(modelVersionService.getModelVersions).mockRejectedValue(error)
      
      const store = useModelStore.getState()
      
      await expect(store.fetchModelList()).rejects.toThrow(error)
      
      const state = useModelStore.getState()
      expect(state.modelListLoading).toBe(false)
      expect(state.modelListError).toBe(error.message)
    })
  })

  describe('fetchModelDetail', () => {
    it('应该成功获取模型版本详情', async () => {
      const mockDetail = {
        modelId: 'model-001',
        taskId: 'task-001',
        roundNumber: 10,
        aggregationMethod: 'FedAvg',
        clientCount: 5,
        modelJson: { layers: 6 },
        metrics: { accuracy: 92.5, loss: 0.275 },
        createdAt: '2024-01-15T10:00:00Z',
        aggregatedAt: '2024-01-15T10:30:00Z',
        status: 'DEPLOYED'
      }
      mocked(modelVersionService.getModelVersionDetail).mockResolvedValue(mockDetail)
      
      const store = useModelStore.getState()
      await store.fetchModelDetail('model-001')
      
      const state = useModelStore.getState()
      expect(state.currentModel).toEqual(mockDetail)
      expect(state.currentModelLoading).toBe(false)
      expect(state.currentModelError).toBe(null)
    })

    it('应该处理获取详情失败', async () => {
      const error = mockModelVersionErrors.FETCH_MODEL_DETAIL_ERROR
      mocked(modelVersionService.getModelVersionDetail).mockRejectedValue(error)
      
      const store = useModelStore.getState()
      
      await expect(store.fetchModelDetail('model-001')).rejects.toThrow(error)
      
      const state = useModelStore.getState()
      expect(state.currentModelLoading).toBe(false)
      expect(state.currentModelError).toBe(error.message)
    })
  })

  describe('uploadModel', () => {
    it('应该成功上传模型', async () => {
      const mockResponse = {
        modelId: 'model-new-001',
        taskId: 'task-001',
        roundNumber: 11,
        status: 'UPLOADING',
        description: '新上传的模型版本',
        parameters: {
          framework: 'TensorFlow',
          layers: 6,
          totalParams: 431080
        },
        createdAt: '2024-01-15T10:00:00Z'
      }
      
      // Mock uploadModel 和 refreshModelList 中调用的 getModelVersions
      mocked(modelVersionService.uploadModel).mockResolvedValue(mockResponse)
      mocked(modelVersionService.getModelVersions).mockResolvedValue({
        records: [],
        total: 0,
        current: 1,
        size: 20,
        pages: 0
      })
      
      const store = useModelStore.getState()
      const formData = new FormData()
      formData.append('taskId', 'task-001')
      formData.append('roundNumber', '11')
      formData.append('file', new File(['model data'], 'model.h5'))
      
      const result = await store.uploadModel(formData)
      
      expect(result).toEqual(mockResponse.modelId)
      const state = useModelStore.getState()
      expect(state.uploadLoading).toBe(false)
      expect(modelVersionService.uploadModel).toHaveBeenCalledWith(formData)
    })

    it('应该处理上传失败', async () => {
      const error = mockModelVersionErrors.UPLOAD_MODEL_ERROR
      mocked(modelVersionService.uploadModel).mockRejectedValue(error)
      
      const store = useModelStore.getState()
      const formData = new FormData()
      
      await expect(store.uploadModel(formData)).rejects.toThrow(error)
      
      const state = useModelStore.getState()
      expect(state.uploadError).toBe(error.message)
    })
  })

  describe('evaluateModel', () => {
    it('应该成功评估模型', async () => {
      mocked(modelVersionService.evaluateModel).mockResolvedValue(undefined)
      
      const store = useModelStore.getState()
      
      await store.evaluateModel('model-001', mockEvaluationRequest)
      
      expect(modelVersionService.evaluateModel).toHaveBeenCalledWith(mockEvaluationRequest)
    })

    it('应该处理评估失败', async () => {
      const error = mockModelVersionErrors.EVALUATE_MODEL_ERROR
      mocked(modelVersionService.evaluateModel).mockRejectedValue(error)
      
      const store = useModelStore.getState()
      
      await expect(store.evaluateModel('model-001', mockEvaluationRequest)).rejects.toThrow(error)
    })
  })

  describe('deployModel', () => {
    it('应该成功部署模型', async () => {
      const mockResponse = {
        deploymentId: 'deploy-001',
        modelId: 'model-001',
        deploymentName: 'mnist-model-deployment',
        targetVms: ['vm-001', 'vm-002'],
        status: 'DEPLOYING',
        deploymentConfig: {
          replicas: 3,
          resources: {
            cpu: '1000m',
            memory: '2Gi'
          }
        },
        endpoints: [],
        createdAt: '2024-01-15T10:00:00Z'
      }
      mocked(modelVersionService.deployModel).mockResolvedValue(mockResponse)
      
      const store = useModelStore.getState()
      
      const result = await store.deployModel('model-001', mockDeploymentRequest)
      
      expect(result).toEqual(mockResponse.deploymentId)
      expect(modelVersionService.deployModel).toHaveBeenCalledWith(mockDeploymentRequest)
    })

    it('应该处理部署失败', async () => {
      const error = mockModelVersionErrors.DEPLOY_MODEL_ERROR
      mocked(modelVersionService.deployModel).mockRejectedValue(error)
      
      const store = useModelStore.getState()
      
      await expect(store.deployModel('model-001', mockDeploymentRequest)).rejects.toThrow(error)
    })
  })

  describe('rollbackModel', () => {
    it('应该成功回滚模型', async () => {
      mocked(modelVersionService.rollbackModel).mockResolvedValue(undefined)
      
      const store = useModelStore.getState()
      const rollbackRequest = {
        deploymentId: 'deploy-001',
        targetModelId: 'model-002',
        rollbackReason: 'Performance issue'
      }
      
      await store.rollbackModel('deploy-001', rollbackRequest)
      
      expect(modelVersionService.rollbackModel).toHaveBeenCalledWith(rollbackRequest)
    })
  })

  describe('deleteModel', () => {
    it('应该成功删除模型', async () => {
      mocked(modelVersionService.deleteModel).mockResolvedValue(undefined)
      
      const store = useModelStore.getState()
      
      // 先添加模型到列表中
      useModelStore.setState({
        modelList: [...mockModelVersionList],
        modelListTotal: mockModelVersionList.length
      })
      
      const deleteRequest = { force: true, deleteFile: true }
      await store.deleteModel('model-001', deleteRequest)
      
      expect(modelVersionService.deleteModel).toHaveBeenCalledWith('model-001', deleteRequest)
    })
  })

  describe('downloadModel', () => {
    it('应该成功下载模型', async () => {
      // Mock DOM APIs
      const mockCreateObjectURL = vi.fn(() => 'blob:mock-url')
      const mockRevokeObjectURL = vi.fn()
      const mockCreateElement = vi.fn(() => ({
        style: { display: 'none' },
        href: '',
        download: '',
        click: vi.fn()
      }))
      const mockAppendChild = vi.fn()
      const mockRemoveChild = vi.fn()
      
      Object.defineProperty(window, 'URL', {
        value: { 
          createObjectURL: mockCreateObjectURL,
          revokeObjectURL: mockRevokeObjectURL
        },
        writable: true
      })
      Object.defineProperty(document, 'createElement', {
        value: mockCreateElement,
        writable: true
      })
      Object.defineProperty(document.body, 'appendChild', {
        value: mockAppendChild,
        writable: true
      })
      Object.defineProperty(document.body, 'removeChild', {
        value: mockRemoveChild,
        writable: true
      })
      
      // Mock blob response
      const mockBlob = new Blob(['model data'], { type: 'application/octet-stream' })
      mocked(modelVersionService.downloadModel).mockResolvedValue(mockBlob)
      
      const store = useModelStore.getState()
      const downloadRequest = { format: 'original' as const, compressed: true }
      
      await store.downloadModel('model-001', downloadRequest)
      
      expect(modelVersionService.downloadModel).toHaveBeenCalledWith('model-001', downloadRequest)
      expect(mockCreateObjectURL).toHaveBeenCalledWith(mockBlob)
    })
  })

  describe('fetchTaskModels', () => {
    it('应该成功获取任务模型版本', async () => {
      mocked(modelVersionService.getTaskModelVersions).mockResolvedValue(mockTaskModelVersions)
      
      const store = useModelStore.getState()
      await store.fetchTaskModels('task-001')
      
      const state = useModelStore.getState()
      expect(state.taskModels['task-001']).toEqual(mockTaskModelVersions)
      expect(state.taskModelsLoading['task-001']).toBe(false)
    })
  })

  describe('fetchModelStatistics', () => {
    it('应该成功获取模型统计', async () => {
      const mockStatistics = {
        totalModels: 50,
        averageAccuracy: 87.5,
        averageLoss: 0.325,
        uploadTrend: [
          { date: '2024-01-01', count: 5 },
          { date: '2024-01-02', count: 8 }
        ],
        accuracyTrend: [
          { roundNumber: 1, accuracy: 75.2 },
          { roundNumber: 2, accuracy: 78.5 }
        ]
      }
      mocked(modelVersionService.getModelStatistics).mockResolvedValue(mockStatistics)
      
      const store = useModelStore.getState()
      await store.fetchModelStatistics()
      
      const state = useModelStore.getState()
      expect(state.modelStatistics).toEqual(mockStatistics)
      expect(state.statisticsLoading).toBe(false)
    })
  })

  describe('fetchTaskStatistics', () => {
    it('应该成功获取任务统计', async () => {
      const mockStatistics = {
        taskId: 'task-001',
        taskName: 'MNIST分类任务',
        totalRounds: 10,
        completedRounds: 10,
        performanceMetrics: {
          bestAccuracy: 92.5,
          bestRound: 10,
          averageAccuracy: 88.7,
          accuracyImprovement: 15.2
        }
      }
      mocked(modelVersionService.getTaskModelStatistics).mockResolvedValue(mockStatistics)
      
      const store = useModelStore.getState()
      await store.fetchTaskStatistics('task-001')
      
      const state = useModelStore.getState()
      expect(state.taskStatistics['task-001']).toEqual(mockStatistics)
    })
  })

  describe('分页操作', () => {
    it('应该设置分页参数', () => {
      const store = useModelStore.getState()
      
      store.setPagination(2, 50)
      
      const state = useModelStore.getState()
      expect(state.pagination.page).toBe(2)
      expect(state.pagination.size).toBe(50)
    })
  })

  describe('查询参数操作', () => {
    it('应该设置查询参数', () => {
      const store = useModelStore.getState()
      const params = { 
        status: 'DEPLOYED' as const,
        taskId: 'task-001'
      }
      
      store.setQueryParams(params)
      
      const state = useModelStore.getState()
      expect(state.queryParams).toEqual(params)
    })

    it('应该重置查询参数', () => {
      const store = useModelStore.getState()
      
      // 先设置一些参数
      store.setQueryParams({ status: 'DEPLOYED' as const })
      
      store.resetQueryParams()
      
      const state = useModelStore.getState()
      expect(state.queryParams).toEqual({})
    })
  })

  describe('错误处理', () => {
    it('应该清除错误信息', () => {
      const store = useModelStore.getState()
      
      // 设置一些错误状态
      useModelStore.setState({
        modelListError: 'test error',
        currentModelError: 'current error',
        uploadError: 'upload error',
        operationError: { 'deploy-model-001': 'deploy error' }
      })
      
      store.clearError()
      
      const state = useModelStore.getState()
      expect(state.modelListError).toBe(null)
      expect(state.currentModelError).toBe(null)
      expect(state.uploadError).toBe(null)
      expect(state.operationError).toEqual({})
    })
  })

  describe('状态重置', () => {
    it('应该重置所有状态', () => {
      const store = useModelStore.getState()
      
      // 修改一些状态
      useModelStore.setState({
        modelList: mockModelVersionList,
        modelListTotal: 10,
        currentModel: {
          modelId: 'model-001',
          taskId: 'task-001',
          roundNumber: 10,
          aggregationMethod: 'FedAvg',
          clientCount: 5,
          modelJson: {},
          metrics: { accuracy: 92.5, loss: 0.275 },
          createdAt: '2024-01-15T10:00:00Z',
          aggregatedAt: '2024-01-15T10:30:00Z',
          status: 'DEPLOYED'
        },
        taskModels: { 'task-001': mockTaskModelVersions },
        evaluationResults: { 'model-001': [mockEvaluationResult] },
        uploadProgress: { 'upload-001': 50 }
      })
      
      store.resetState()
      
      const state = useModelStore.getState()
      expect(state.modelList).toEqual([])
      expect(state.modelListTotal).toBe(0)
      expect(state.currentModel).toBe(null)
      expect(state.taskModels).toEqual({})
      expect(state.evaluationResults).toEqual({})
      expect(state.uploadProgress).toEqual({})
    })
  })
})