/**
 * useTrainingData Hook 测试
 * 测试训练数据hook的错误处理封装和组件接口
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useData } from '@/store/training-data/useTrainingDataStore'
import { useDataStore } from '@/store/training-data/trainingDataStore'
import {
  mockTrainingDataList,
  mockTrainingDataset,
  mockDataStatistics
} from '@/mocks/store/trainingDataStoreMock'

// Mock the store
vi.mock('@/store/training-data/trainingDataStore')

const mockStore = {
  dataList: [],
  dataListTotal: 0,
  dataListLoading: false,
  dataListError: null,
  currentDataset: null,
  currentDatasetLoading: false,
  currentDatasetError: null,
  uploadLoading: false,
  uploadError: null,
  uploadProgress: {},
  preprocessTasks: {},
  validationResults: {},
  exportTasks: {},
  statistics: null,
  statisticsLoading: false,
  statisticsError: null,
  operationLoading: {},
  operationError: {},
  pagination: { page: 1, size: 20, total: 0 },
  queryParams: {},
  fetchDataList: vi.fn(),
  refreshDataList: vi.fn(),
  fetchDataDetail: vi.fn(),
  uploadFile: vi.fn(),
  uploadText: vi.fn(),
  preprocessData: vi.fn(),
  validateData: vi.fn(),
  updateData: vi.fn(),
  deleteData: vi.fn(),
  batchOperation: vi.fn(),
  exportData: vi.fn(),
  fetchStatistics: vi.fn(),
  setPagination: vi.fn(),
  setQueryParams: vi.fn(),
  resetQueryParams: vi.fn(),
  clearError: vi.fn(),
  resetState: vi.fn()
}

describe('useData', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Mock the store selector function
    const mockUseDataStore = vi.mocked(useDataStore)
    mockUseDataStore.mockImplementation((selector: any) => {
      if (typeof selector === 'function') {
        return selector(mockStore)
      }
      return mockStore
    })
  })

  describe('状态访问', () => {
    it('应该正确暴露训练数据状态', () => {
      const { result } = renderHook(() => useData())
      
      expect(result.current.dataList).toEqual([])
      expect(result.current.dataListTotal).toBe(0)
      expect(result.current.dataListLoading).toBe(false)
      expect(result.current.dataListError).toBe(null)
      expect(result.current.currentDataset).toBe(null)
      expect(result.current.currentDatasetLoading).toBe(false)
      expect(result.current.currentDatasetError).toBe(null)
      expect(result.current.uploadLoading).toBe(false)
      expect(result.current.uploadError).toBe(null)
      expect(result.current.uploadProgress).toEqual({})
      expect(result.current.preprocessTasks).toEqual({})
      expect(result.current.validationResults).toEqual({})
      expect(result.current.exportTasks).toEqual({})
      expect(result.current.statistics).toBe(null)
      expect(result.current.statisticsLoading).toBe(false)
      expect(result.current.statisticsError).toBe(null)
      expect(result.current.operationLoading).toEqual({})
      expect(result.current.operationError).toEqual({})
      expect(result.current.pagination).toEqual({ page: 1, size: 20, total: 0 })
      expect(result.current.queryParams).toEqual({})
    })
  })

  describe('数据列表操作', () => {
    it('应该成功获取数据列表', async () => {
      mockStore.fetchDataList.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useData())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchDataList()
      })
      
      expect(fetchResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchDataList).toHaveBeenCalled()
    })

    it('应该处理获取数据列表失败', async () => {
      const error = new Error('获取数据列表失败')
      mockStore.fetchDataList.mockRejectedValue(error)
      
      const { result } = renderHook(() => useData())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchDataList()
      })
      
      expect(fetchResult).toEqual({ success: false, error: '获取数据列表失败' })
    })

    it('应该成功刷新数据列表', async () => {
      mockStore.refreshDataList.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useData())
      
      let refreshResult
      await act(async () => {
        refreshResult = await result.current.refreshDataList()
      })
      
      expect(refreshResult).toEqual({ success: true, error: null })
      expect(mockStore.refreshDataList).toHaveBeenCalled()
    })

    it('应该支持带参数的数据列表获取', async () => {
      mockStore.fetchDataList.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useData())
      
      const params = {
        dataType: 'ACOUSTIC' as const,
        status: 'READY' as const,
        page: 2,
        size: 10
      }
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchDataList(params)
      })
      
      expect(fetchResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchDataList).toHaveBeenCalledWith(params)
    })
  })

  describe('数据详情操作', () => {
    it('应该成功获取数据详情', async () => {
      mockStore.fetchDataDetail.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useData())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchDataDetail('dataset-001')
      })
      
      expect(fetchResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchDataDetail).toHaveBeenCalledWith('dataset-001')
    })

    it('应该处理获取数据详情失败', async () => {
      const error = new Error('获取数据详情失败')
      mockStore.fetchDataDetail.mockRejectedValue(error)
      
      const { result } = renderHook(() => useData())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchDataDetail('dataset-001')
      })
      
      expect(fetchResult).toEqual({ success: false, error: '获取数据详情失败' })
    })
  })

  describe('数据上传操作', () => {
    it('应该成功上传文件', async () => {
      const mockResponse = {
        datasetId: 'dataset-new-001',
        datasetDescription: '新上传的数据集',
        datasetType: 'ACOUSTIC',
        vmId: 'vm-001',
        status: 'UPLOADING',
        uploadTime: '2024-01-15T10:00:00Z',
        uploadedBy: 'user-001',
        progress: 0
      }
      mockStore.uploadFile.mockResolvedValue(mockResponse)
      
      const { result } = renderHook(() => useData())
      
      const formData = new FormData()
      formData.append('vmId', 'vm-001')
      formData.append('dataType', 'ACOUSTIC')
      formData.append('file', new File(['test'], 'test.csv'))
      
      let uploadResult
      await act(async () => {
        uploadResult = await result.current.uploadFile(formData)
      })
      
      expect(uploadResult).toEqual({ success: true, error: null })
      expect(mockStore.uploadFile).toHaveBeenCalledWith(formData)
    })

    it('应该处理文件上传失败', async () => {
      const error = new Error('文件上传失败')
      mockStore.uploadFile.mockRejectedValue(error)
      
      const { result } = renderHook(() => useData())
      
      const formData = new FormData()
      
      let uploadResult
      await act(async () => {
        uploadResult = await result.current.uploadFile(formData)
      })
      
      expect(uploadResult).toEqual({ success: false, error: '文件上传失败' })
    })

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
      mockStore.uploadText.mockResolvedValue(mockResponse)
      
      const { result } = renderHook(() => useData())
      
      const textRequest = {
        vmId: 'vm-001',
        dataType: 'ACOUSTIC' as const,
        title: '测试文本数据',
        content: 'sample,text,data\n1,2,3',
        description: '测试用文本数据'
      }
      
      let uploadResult
      await act(async () => {
        uploadResult = await result.current.uploadText(textRequest)
      })
      
      expect(uploadResult).toEqual({ success: true, error: null })
      expect(mockStore.uploadText).toHaveBeenCalledWith(textRequest)
    })
  })

  describe('数据预处理操作', () => {
    it('应该成功预处理数据', async () => {
      const mockResponse = {
        datasetId: 'dataset-001',
        taskId: 'process-001',
        status: 'PROCESSING',
        methods: ['NORMALIZATION'],
        startedAt: '2024-01-15T10:00:00Z',
        estimatedTime: 600
      }
      mockStore.preprocessData.mockResolvedValue(mockResponse)
      
      const { result } = renderHook(() => useData())
      
      const preprocessRequest = {
        methods: ['NORMALIZATION' as const],
        parameters: { method: 'min-max', range: [0, 1] }
      }
      
      let preprocessResult
      await act(async () => {
        preprocessResult = await result.current.preprocessData('dataset-001', preprocessRequest)
      })
      
      expect(preprocessResult).toEqual({ success: true, error: null })
      expect(mockStore.preprocessData).toHaveBeenCalledWith('dataset-001', preprocessRequest)
    })

    it('应该处理数据预处理失败', async () => {
      const error = new Error('数据预处理失败')
      mockStore.preprocessData.mockRejectedValue(error)
      
      const { result } = renderHook(() => useData())
      
      const preprocessRequest = {
        methods: ['NORMALIZATION' as const],
        parameters: {}
      }
      
      let preprocessResult
      await act(async () => {
        preprocessResult = await result.current.preprocessData('dataset-001', preprocessRequest)
      })
      
      expect(preprocessResult).toEqual({ success: false, error: '数据预处理失败' })
    })
  })

  describe('数据验证操作', () => {
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
      mockStore.validateData.mockResolvedValue(mockResponse)
      
      const { result } = renderHook(() => useData())
      
      const validationRequest = {
        dataType: 'ACOUSTIC' as const,
        requiredColumns: ['feature1', 'feature2'],
        qualityChecks: ['duplicates', 'missing']
      }
      
      let validateResult
      await act(async () => {
        validateResult = await result.current.validateData('dataset-001', validationRequest)
      })
      
      expect(validateResult).toEqual({ success: true, error: null })
      expect(mockStore.validateData).toHaveBeenCalledWith('dataset-001', validationRequest)
    })
  })

  describe('数据统计操作', () => {
    it('应该成功获取数据统计', async () => {
      mockStore.fetchStatistics.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useData())
      
      let statsResult
      await act(async () => {
        statsResult = await result.current.fetchStatistics()
      })
      
      expect(statsResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchStatistics).toHaveBeenCalled()
    })

    it('应该处理获取统计失败', async () => {
      const error = new Error('获取统计信息失败')
      mockStore.fetchStatistics.mockRejectedValue(error)
      
      const { result } = renderHook(() => useData())
      
      let statsResult
      await act(async () => {
        statsResult = await result.current.fetchStatistics()
      })
      
      expect(statsResult).toEqual({ success: false, error: '获取统计信息失败' })
    })
  })

  describe('数据管理操作', () => {
    it('应该成功更新数据', async () => {
      mockStore.updateData.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useData())
      
      const updateRequest = {
        datasetDescription: '更新后的描述'
      }
      
      let updateResult
      await act(async () => {
        updateResult = await result.current.updateData('dataset-001', updateRequest)
      })
      
      expect(updateResult).toEqual({ success: true, error: null })
      expect(mockStore.updateData).toHaveBeenCalledWith('dataset-001', updateRequest)
    })

    it('应该成功删除数据', async () => {
      mockStore.deleteData.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useData())
      
      let deleteResult
      await act(async () => {
        deleteResult = await result.current.deleteData('dataset-001')
      })
      
      expect(deleteResult).toEqual({ success: true, error: null })
      expect(mockStore.deleteData).toHaveBeenCalledWith('dataset-001', false)
    })

    it('应该成功执行批量操作', async () => {
      const mockResponse = {
        operation: 'DELETE',
        total: 2,
        success: 2,
        failed: 0,
        results: []
      }
      mockStore.batchOperation.mockResolvedValue(mockResponse)
      
      const { result } = renderHook(() => useData())
      
      const batchRequest = {
        operation: 'DELETE' as const,
        datasetIds: ['dataset-001', 'dataset-002'],
        parameters: { force: true }
      }
      
      let batchResult
      await act(async () => {
        batchResult = await result.current.batchOperation(batchRequest)
      })
      
      expect(batchResult).toEqual({ success: true, error: null })
      expect(mockStore.batchOperation).toHaveBeenCalledWith(batchRequest)
    })
  })

  describe('数据导出操作', () => {
    it('应该成功导出数据', async () => {
      const mockResponse = {
        taskId: 'export-001',
        status: 'COMPLETED',
        format: 'CSV',
        startedAt: '2024-01-15T10:00:00Z',
        estimatedTime: 300,
        downloadUrl: '/api/download/export-001'
      }
      mockStore.exportData.mockResolvedValue(mockResponse)
      
      const { result } = renderHook(() => useData())
      
      const exportRequest = {
        exportType: 'CSV' as const,
        filters: { dataType: 'ACOUSTIC' as const },
        fields: ['datasetId', 'datasetDescription'],
        format: 'ZIP' as const
      }
      
      let exportResult
      await act(async () => {
        exportResult = await result.current.exportData(exportRequest)
      })
      
      expect(exportResult).toEqual({ success: true, error: null })
      expect(mockStore.exportData).toHaveBeenCalledWith(exportRequest)
    })
  })

  describe('状态管理操作', () => {
    it('应该成功设置分页参数', () => {
      const { result } = renderHook(() => useData())
      
      act(() => {
        result.current.setPagination(2, 50)
      })
      
      expect(mockStore.setPagination).toHaveBeenCalledWith(2, 50)
    })

    it('应该成功设置查询参数', () => {
      const { result } = renderHook(() => useData())
      
      const params = { dataType: 'ACOUSTIC' as const, status: 'READY' as const }
      
      act(() => {
        result.current.setQueryParams(params)
      })
      
      expect(mockStore.setQueryParams).toHaveBeenCalledWith(params)
    })

    it('应该成功重置查询参数', () => {
      const { result } = renderHook(() => useData())
      
      act(() => {
        result.current.resetQueryParams()
      })
      
      expect(mockStore.resetQueryParams).toHaveBeenCalled()
    })

    it('应该成功清除错误', () => {
      const { result } = renderHook(() => useData())
      
      act(() => {
        result.current.clearError()
      })
      
      expect(mockStore.clearError).toHaveBeenCalled()
    })

    it('应该成功重置状态', () => {
      const { result } = renderHook(() => useData())
      
      act(() => {
        result.current.resetState()
      })
      
      expect(mockStore.resetState).toHaveBeenCalled()
    })
  })

  describe('错误处理', () => {
    it('应该处理非Error类型的异常', async () => {
      mockStore.fetchDataList.mockRejectedValue('string error')
      
      const { result } = renderHook(() => useData())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchDataList()
      })
      
      expect(fetchResult).toEqual({ success: false, error: '获取数据列表失败' })
    })

    it('应该处理undefined异常', async () => {
      mockStore.uploadFile.mockRejectedValue(undefined)
      
      const { result } = renderHook(() => useData())
      
      let uploadResult
      await act(async () => {
        uploadResult = await result.current.uploadFile(new FormData())
      })
      
      expect(uploadResult).toEqual({ success: false, error: '文件上传失败' })
    })
  })
})
