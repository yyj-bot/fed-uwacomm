/**
 * Training Data Store 测试
 * 测试训练数据状态管理的各种功能，包括数据上传、管理、预处理、错误处理等
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useDataStore } from '@/store/training-data'
import { trainingDataService } from '@/services'
import {
  mockTrainingDataList,
  mockTrainingDataset,
  mockDataStatistics,
  mockUploadFileRequest,
  mockPreprocessRequest,
  mockTrainingDataErrors
} from '@/mocks/store/trainingDataStoreMock'

// Mock trainingDataService
vi.mock('@/services', () => ({
  trainingDataService: {
    getDataList: vi.fn(),
    getDataDetail: vi.fn(),
    uploadFile: vi.fn(),
    uploadText: vi.fn(),
    preprocessData: vi.fn(),
    validateData: vi.fn(),
    updateData: vi.fn(),
    deleteData: vi.fn(),
    batchOperation: vi.fn(),
    exportData: vi.fn(),
    getDataStatistics: vi.fn()
  }
}))

const mocked = vi.mocked

describe('TrainingDataStore', () => {
  beforeEach(() => {
    // 重置所有mock
    vi.clearAllMocks()
    
    // 重置store状态
    const store = useDataStore.getState()
    store.resetState()
  })

  describe('初始状态', () => {
    it('应该有正确的初始状态', () => {
      const state = useDataStore.getState()
      
      expect(state.dataList).toEqual([])
      expect(state.dataListTotal).toBe(0)
      expect(state.dataListLoading).toBe(false)
      expect(state.dataListError).toBe(null)
      expect(state.currentDataset).toBe(null)
      expect(state.currentDatasetLoading).toBe(false)
      expect(state.currentDatasetError).toBe(null)
      expect(state.uploadLoading).toBe(false)
      expect(state.uploadProgress).toEqual({})
      expect(state.uploadError).toBe(null)
      expect(state.preprocessTasks).toEqual({})
      expect(state.validationResults).toEqual({})
      expect(state.exportTasks).toEqual({})
      expect(state.operationLoading).toEqual({})
      expect(state.operationError).toEqual({})
      expect(state.statistics).toBe(null)
      expect(state.statisticsLoading).toBe(false)
      expect(state.pagination).toEqual({
        page: 1,
        size: 20,
        total: 0
      })
      expect(state.queryParams).toEqual({})
    })
  })

  describe('fetchDataList', () => {
    it('应该成功获取数据列表', async () => {
      const mockResponse = {
        total: mockTrainingDataList.length,
        page: 1,
        size: 20,
        dataList: mockTrainingDataList.map(item => ({
          datasetId: item.datasetId,
          datasetDescription: item.datasetDescription,
          datasetType: item.datasetType,
          vmId: item.vmId,
          status: item.status,
          tags: item.tags
        }))
      }
      
      mocked(trainingDataService.getDataList).mockResolvedValue(mockResponse)
      
      const store = useDataStore.getState()
      await store.fetchDataList()
      
      const state = useDataStore.getState()
      expect(state.dataList).toEqual(mockResponse.dataList)
      expect(state.dataListTotal).toBe(mockTrainingDataList.length)
      expect(state.dataListLoading).toBe(false)
      expect(state.dataListError).toBe(null)
      expect(state.pagination.page).toBe(1)
      expect(state.pagination.total).toBe(mockTrainingDataList.length)
    })

    it('应该处理加载状态', async () => {
      const pendingPromise = new Promise<any>(() => {}) // Never resolves
      mocked(trainingDataService.getDataList).mockReturnValue(pendingPromise)
      
      const store = useDataStore.getState()
      store.fetchDataList()
      
      const state = useDataStore.getState()
      expect(state.dataListLoading).toBe(true)
      expect(state.dataListError).toBe(null)
    })

    it('应该处理错误情况', async () => {
      const error = mockTrainingDataErrors.FETCH_DATA_LIST_ERROR
      mocked(trainingDataService.getDataList).mockRejectedValue(error)
      
      const store = useDataStore.getState()
      
      await expect(store.fetchDataList()).rejects.toThrow(error)
      
      const state = useDataStore.getState()
      expect(state.dataListLoading).toBe(false)
      expect(state.dataListError).toBe(error.message)
    })
  })

  describe('fetchDataDetail', () => {
    it('应该成功获取数据详情', async () => {
      mocked(trainingDataService.getDataDetail).mockResolvedValue(mockTrainingDataset)
      
      const store = useDataStore.getState()
      await store.fetchDataDetail('dataset-001')
      
      const state = useDataStore.getState()
      expect(state.currentDataset).toEqual(mockTrainingDataset)
      expect(state.currentDatasetLoading).toBe(false)
      expect(state.currentDatasetError).toBe(null)
    })

    it('应该处理获取详情失败', async () => {
      const error = mockTrainingDataErrors.FETCH_DATA_DETAIL_ERROR
      mocked(trainingDataService.getDataDetail).mockRejectedValue(error)
      
      const store = useDataStore.getState()
      
      await expect(store.fetchDataDetail('dataset-001')).rejects.toThrow(error)
      
      const state = useDataStore.getState()
      expect(state.currentDatasetLoading).toBe(false)
      expect(state.currentDatasetError).toBe(error.message)
    })
  })

  describe('uploadFile', () => {
    it('应该成功上传文件', async () => {
      const mockResponse = {
        datasetId: 'dataset-new-001',
        datasetDescription: '新上传的训练数据集',
        datasetType: 'ACOUSTIC',
        vmId: 'vm-001',
        status: 'UPLOADING',
        uploadTime: '2024-01-15T10:00:00Z',
        uploadedBy: 'user-001',
        progress: 0
      }
      
      // Mock uploadFile 和 refreshDataList 中调用的 getDataList
      mocked(trainingDataService.uploadFile).mockResolvedValue(mockResponse)
      mocked(trainingDataService.getDataList).mockResolvedValue({
        total: 0,
        page: 1,
        size: 20,
        dataList: []
      })
      
      const store = useDataStore.getState()
      const formData = new FormData()
      formData.append('vmId', 'vm-001')
      formData.append('dataType', 'ACOUSTIC')
      formData.append('file', new File(['test data'], 'test.csv'))
      
      await store.uploadFile(formData)
      
      const state = useDataStore.getState()
      expect(state.uploadLoading).toBe(false)
      expect(trainingDataService.uploadFile).toHaveBeenCalledWith(formData)
    })

    it('应该处理上传失败', async () => {
      const error = mockTrainingDataErrors.UPLOAD_FILE_ERROR
      mocked(trainingDataService.uploadFile).mockRejectedValue(error)
      
      const store = useDataStore.getState()
      const formData = new FormData()
      
      await expect(store.uploadFile(formData)).rejects.toThrow(error)
      
      const state = useDataStore.getState()
      expect(state.uploadError).toBe(error.message)
    })
  })

  describe('uploadText', () => {
    it('应该成功上传文本数据', async () => {
      const mockResponse = {
        datasetId: 'dataset-text-001',
        datasetDescription: '文本数据集',
        datasetType: 'ACOUSTIC',
        vmId: 'vm-001',
        status: 'UPLOADING',
        uploadTime: '2024-01-15T10:00:00Z',
        uploadedBy: 'user-001'
      }
      
      // Mock uploadText 和 refreshDataList 中调用的 getDataList
      mocked(trainingDataService.uploadText).mockResolvedValue(mockResponse)
      mocked(trainingDataService.getDataList).mockResolvedValue({
        total: 0,
        page: 1,
        size: 20,
        dataList: []
      })
      
      const store = useDataStore.getState()
      const textRequest = {
        vmId: 'vm-001',
        dataType: 'ACOUSTIC' as const,
        title: '测试文本数据',
        content: 'sample,text,data\n1,2,3\n4,5,6',
        description: '文本数据集'
      }
      
      await store.uploadText(textRequest)
      
      const state = useDataStore.getState()
      expect(state.uploadLoading).toBe(false)
    })
  })

  describe('preprocessData', () => {
    it('应该成功预处理数据', async () => {
      const mockResponse = {
        datasetId: 'dataset-001',
        taskId: 'process-001',
        status: 'PROCESSING',
        methods: ['NORMALIZATION', 'FEATURE_SELECTION'],
        startedAt: '2024-01-15T10:00:00Z',
        estimatedTime: 600
      }
      mocked(trainingDataService.preprocessData).mockResolvedValue(mockResponse)
      
      const store = useDataStore.getState()
      
      await store.preprocessData('dataset-001', mockPreprocessRequest)
      
      expect(trainingDataService.preprocessData).toHaveBeenCalledWith('dataset-001', mockPreprocessRequest)
      const state = useDataStore.getState()
      expect(state.preprocessTasks['dataset-001']).toEqual(mockResponse)
    })

    it('应该处理预处理失败', async () => {
      const error = mockTrainingDataErrors.PREPROCESS_DATA_ERROR
      mocked(trainingDataService.preprocessData).mockRejectedValue(error)
      
      const store = useDataStore.getState()
      
      await expect(store.preprocessData('dataset-001', mockPreprocessRequest)).rejects.toThrow(error)
    })
  })

  describe('validateData', () => {
    it('应该成功验证数据', async () => {
      const mockResponse = {
        datasetId: 'dataset-001',
        isValid: true,
        validationTime: '2024-01-15T10:00:00Z',
        results: {
          totalRows: 1000,
          validRows: 1000,
          invalidRows: 0,
          missingValues: 0,
          duplicates: 0,
          outliers: 0
        },
        issues: [],
        errors: [],
        warnings: [],
        summary: {
          totalRecords: 1000,
          validRecords: 1000,
          invalidRecords: 0
        }
      }
      mocked(trainingDataService.validateData).mockResolvedValue(mockResponse)
      
      const store = useDataStore.getState()
      const validationRequest = {
        dataType: 'ACOUSTIC' as const,
        requiredColumns: ['feature1', 'feature2'],
        qualityChecks: ['duplicates', 'missing']
      }
      
      await store.validateData('dataset-001', validationRequest)
      
      const state = useDataStore.getState()
      expect(state.validationResults['dataset-001']).toEqual(mockResponse)
    })
  })

  describe('updateData', () => {
    it('应该成功更新数据', async () => {
      const mockResponse = {
        datasetId: 'dataset-001',
        updatedAt: '2024-01-15T10:00:00Z',
        updatedBy: 'user-001'
      }
      mocked(trainingDataService.updateData).mockResolvedValue(mockResponse)
      mocked(trainingDataService.getDataDetail).mockResolvedValue(mockTrainingDataset)
      
      const store = useDataStore.getState()
      const updateRequest = { datasetDescription: '更新后的描述' }
      
      await store.updateData('dataset-001', updateRequest)
      
      expect(trainingDataService.updateData).toHaveBeenCalledWith('dataset-001', updateRequest)
    })
  })

  describe('deleteData', () => {
    it('应该成功删除数据', async () => {
      const mockResponse = {
        datasetId: 'dataset-001',
        deletedAt: '2024-01-15T10:00:00Z',
        deletedBy: 'user-001',
        fileDeleted: true,
        metadataPreserved: false
      }
      mocked(trainingDataService.deleteData).mockResolvedValue(mockResponse)
      
      const store = useDataStore.getState()
      
      // 先添加数据到列表中
      useDataStore.setState({
        dataList: [...mockTrainingDataList.map(item => ({
          datasetId: item.datasetId,
          datasetDescription: item.datasetDescription,
          datasetType: item.datasetType,
          vmId: item.vmId,
          status: item.status,
          tags: item.tags
        }))],
        dataListTotal: mockTrainingDataList.length
      })
      
      await store.deleteData('dataset-001')
      
      expect(trainingDataService.deleteData).toHaveBeenCalledWith('dataset-001', { deleteFile: false })
    })
  })

  describe('batchOperation', () => {
    it('应该成功执行批量操作', async () => {
      const mockResponse = {
        operation: 'DELETE',
        total: 2,
        success: 2,
        failed: 0,
        results: [
          { datasetId: 'dataset-001', status: 'SUCCESS', message: '删除成功' },
          { datasetId: 'dataset-002', status: 'SUCCESS', message: '删除成功' }
        ]
      }
      
      // Mock batchOperation 和 refreshDataList 中调用的 getDataList
      mocked(trainingDataService.batchOperation).mockResolvedValue(mockResponse)
      mocked(trainingDataService.getDataList).mockResolvedValue({
        total: 0,
        page: 1,
        size: 20,
        dataList: []
      })
      
      const store = useDataStore.getState()
      const batchRequest = {
        operation: 'DELETE' as const,
        datasetIds: ['dataset-001', 'dataset-002'],
        parameters: { force: true }
      }
      
      await store.batchOperation(batchRequest)
      
      expect(trainingDataService.batchOperation).toHaveBeenCalledWith(batchRequest)
    })
  })

  describe('exportData', () => {
    it('应该成功导出数据', async () => {
      const mockResponse = {
        taskId: 'export-001',
        status: 'COMPLETED',
        format: 'CSV',
        startedAt: '2024-01-15T10:00:00Z',
        estimatedTime: 300,
        downloadUrl: '/api/download/export-001'
      }
      mocked(trainingDataService.exportData).mockResolvedValue(mockResponse)
      
      const store = useDataStore.getState()
      const exportRequest = {
        exportType: 'CSV' as const,
        filters: {
          dataType: 'ACOUSTIC' as const,
          vmId: 'vm-001'
        },
        fields: ['datasetId', 'datasetDescription'],
        format: 'ZIP' as const
      }
      
      await store.exportData(exportRequest)
      
      const state = useDataStore.getState()
      expect(state.exportTasks['export-001']).toEqual(mockResponse)
    })
  })

  describe('fetchStatistics', () => {
    it('应该成功获取统计信息', async () => {
      // 创建符合API类型的统计数据
      const apiStatistics = {
        totalCount: 150,
        totalSize: 2500000000,
        dataTypeDistribution: {
          'ACOUSTIC': 45,
          'ENVIRONMENT': 35,
          'MODEL': 40,
          'FEATURE': 20,
          'OTHER': 10
        },
        statusDistribution: {
          'READY': 115,
          'PROCESSING': 12,
          'UPLOADING': 5,
          'ERROR': 7,
          'VALIDATING': 8,
          'DELETED': 3
        },
        vmDistribution: {
          'vm-001': { count: 50, size: 800000000 },
          'vm-002': { count: 45, size: 750000000 }
        },
        uploadTrend: {
          last7Days: [5, 8, 12, 7, 15, 10, 9],
          last30Days: Array(30).fill(0).map((_, i) => i + 1)
        },
        topDataTypes: [
          { dataType: 'ACOUSTIC', count: 45, percentage: 30 },
          { dataType: 'ENVIRONMENT', count: 35, percentage: 23.3 }
        ]
      }
      mocked(trainingDataService.getDataStatistics).mockResolvedValue(apiStatistics)
      
      const store = useDataStore.getState()
      await store.fetchStatistics()
      
      const state = useDataStore.getState()
      expect(state.statistics).toEqual(apiStatistics)
      expect(state.statisticsLoading).toBe(false)
    })

    it('应该处理统计获取失败', async () => {
      const error = mockTrainingDataErrors.FETCH_STATISTICS_ERROR
      mocked(trainingDataService.getDataStatistics).mockRejectedValue(error)
      
      const store = useDataStore.getState()
      
      await expect(store.fetchStatistics()).rejects.toThrow(error)
      
      const state = useDataStore.getState()
      expect(state.statisticsLoading).toBe(false)
    })
  })

  describe('分页操作', () => {
    it('应该设置分页参数', () => {
      const store = useDataStore.getState()
      
      store.setPagination(2, 50)
      
      const state = useDataStore.getState()
      expect(state.pagination.page).toBe(2)
      expect(state.pagination.size).toBe(50)
    })
  })

  describe('查询参数操作', () => {
    it('应该设置查询参数', () => {
      const store = useDataStore.getState()
      const params = { 
        dataType: 'ACOUSTIC' as const, 
        status: 'READY' as const,
        keyword: 'test'
      }
      
      store.setQueryParams(params)
      
      const state = useDataStore.getState()
      expect(state.queryParams).toEqual(params)
    })

    it('应该重置查询参数', () => {
      const store = useDataStore.getState()
      
      // 先设置一些参数
      store.setQueryParams({ dataType: 'ACOUSTIC' as const })
      
      store.resetQueryParams()
      
      const state = useDataStore.getState()
      expect(state.queryParams).toEqual({})
    })
  })

  describe('错误处理', () => {
    it('应该清除错误信息', () => {
      const store = useDataStore.getState()
      
      // 设置一些错误状态
      useDataStore.setState({
        dataListError: 'test error',
        currentDatasetError: 'current error',
        uploadError: 'upload error',
        operationError: { 'operation-001': 'operation error' }
      })
      
      store.clearError()
      
      const state = useDataStore.getState()
      expect(state.dataListError).toBe(null)
      expect(state.currentDatasetError).toBe(null)
      expect(state.uploadError).toBe(null)
      expect(state.operationError).toEqual({})
    })
  })

  describe('状态重置', () => {
    it('应该重置所有状态', () => {
      const store = useDataStore.getState()
      
      // 修改一些状态
      useDataStore.setState({
        dataList: mockTrainingDataList.map(item => ({
          datasetId: item.datasetId,
          datasetDescription: item.datasetDescription,
          datasetType: item.datasetType,
          vmId: item.vmId,
          status: item.status,
          tags: item.tags
        })),
        dataListTotal: 10,
        currentDataset: mockTrainingDataset,
        uploadProgress: { 'upload-001': 50 }
      })
      
      store.resetState()
      
      const state = useDataStore.getState()
      expect(state.dataList).toEqual([])
      expect(state.dataListTotal).toBe(0)
      expect(state.currentDataset).toBe(null)
      expect(state.uploadProgress).toEqual({})
    })
  })
})