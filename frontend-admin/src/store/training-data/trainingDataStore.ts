/**
 * 训练数据管理状态 Store
 * 管理训练数据的上传、查询、处理、统计等相关状态和操作
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { create } from 'zustand'
import { trainingDataService } from '@/services'
import type { DatasetDetail, DataStatistics, PreprocessTask, ValidationResult, ExportTask, BatchOperationResult } from '@/api/training-data'
import type { 
  UploadFileRequest,
  UploadTextRequest,
  DataListParams,
  PreprocessRequest,
  ValidationRequest,
  UpdateDataRequest,
  BatchOperationRequest,
  ExportDataRequest,
  DataStatisticsParams
} from '@/services'

// ==================== 状态类型定义 ====================

interface DataState {
  // 数据列表状态
  dataList: any[]
  dataListTotal: number
  dataListLoading: boolean
  dataListError: string | null
  
  // 当前选中的数据集
  currentDataset: DatasetDetail | null
  currentDatasetLoading: boolean
  currentDatasetError: string | null
  
  // 上传状态
  uploadLoading: boolean
  uploadError: string | null
  uploadProgress: Record<string, number>
  
  // 预处理任务
  preprocessTasks: Record<string, PreprocessTask>
  
  // 验证结果
  validationResults: Record<string, ValidationResult>
  
  // 导出任务
  exportTasks: Record<string, ExportTask>
  
  // 数据统计
  statistics: DataStatistics | null
  statisticsLoading: boolean
  statisticsError: string | null
  
  // 操作状态
  operationLoading: Record<string, boolean>
  operationError: Record<string, string | null>
  
  // 分页参数
  pagination: {
    page: number
    size: number
    total: number
  }
  
  // 查询参数
  queryParams: DataListParams
}

interface DataActions {
  // 数据列表操作
  fetchDataList: (params?: DataListParams) => Promise<void>
  refreshDataList: () => Promise<void>
  
  // 数据详情操作
  fetchDataDetail: (datasetId: string) => Promise<void>
  setCurrentDataset: (dataset: DatasetDetail | null) => void
  
  // 数据上传操作
  uploadFile: (formData: FormData) => Promise<void>
  uploadText: (textData: UploadTextRequest) => Promise<void>
  
  // 数据处理操作
  preprocessData: (datasetId: string, request: PreprocessRequest) => Promise<void>
  validateData: (datasetId: string, request?: ValidationRequest) => Promise<void>
  
  // 数据管理操作
  updateData: (datasetId: string, data: UpdateDataRequest) => Promise<void>
  deleteData: (datasetId: string, force?: boolean) => Promise<void>
  downloadData: (datasetId: string) => Promise<void>
  
  // 批量操作
  batchOperation: (request: BatchOperationRequest) => Promise<void>
  
  // 数据导出
  exportData: (request: ExportDataRequest) => Promise<void>
  
  // 数据统计
  fetchStatistics: (params?: DataStatisticsParams) => Promise<void>
  
  // 分页操作
  setPagination: (page: number, size?: number) => void
  
  // 查询参数操作
  setQueryParams: (params: DataListParams) => void
  resetQueryParams: () => void
  
  // 错误处理
  clearError: () => void
  clearDataError: (datasetId: string) => void
  
  // 状态重置
  resetState: () => void
}

type DataStore = DataState & DataActions

// ==================== 初始状态 ====================

const initialState: DataState = {
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
  
  pagination: {
    page: 1,
    size: 20,
    total: 0
  },
  
  queryParams: {}
}

// ==================== Store 实现 ====================

export const useDataStore = create<DataStore>((set, get) => ({
  ...initialState,

  // ==================== 数据列表操作 ====================
  
  /**
   * 获取数据列表
   */
  fetchDataList: async (params?: DataListParams) => {
    const { pagination, queryParams } = get()
    const finalParams = {
      ...queryParams,
      ...params,
      page: params?.page || pagination.page,
      size: params?.size || pagination.size
    }
    
    set({ dataListLoading: true, dataListError: null })
    
    try {
      const response = await trainingDataService.getDataList(finalParams)
      
      set({
        dataList: response.dataList,
        dataListTotal: response.total,
        dataListLoading: false,
        dataListError: null,
        pagination: {
          page: response.page,
          size: response.size,
          total: response.total
        }
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取数据列表失败'
      set({
        dataListLoading: false,
        dataListError: errorMessage
      })
      throw error
    }
  },

  /**
   * 刷新数据列表
   */
  refreshDataList: async () => {
    const { fetchDataList, queryParams, pagination } = get()
    await fetchDataList({
      ...queryParams,
      page: pagination.page,
      size: pagination.size
    })
  },

  // ==================== 数据详情操作 ====================
  
  /**
   * 获取数据详情
   */
  fetchDataDetail: async (datasetId: string) => {
    set({ currentDatasetLoading: true, currentDatasetError: null })
    
    try {
      const dataset = await trainingDataService.getDataDetail(datasetId)
      
      set({
        currentDataset: dataset,
        currentDatasetLoading: false,
        currentDatasetError: null
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取数据详情失败'
      set({
        currentDatasetLoading: false,
        currentDatasetError: errorMessage
      })
      throw error
    }
  },

  /**
   * 设置当前数据集
   */
  setCurrentDataset: (dataset: DatasetDetail | null) => {
    set({ currentDataset: dataset })
  },

  // ==================== 数据上传操作 ====================
  
  /**
   * 上传文件
   */
  uploadFile: async (formData: FormData) => {
    set({ uploadLoading: true, uploadError: null })
    
    try {
      const response = await trainingDataService.uploadFile(formData)
      
      set({
        uploadLoading: false,
        uploadError: null
      })
      
      // 刷新数据列表
      await get().refreshDataList()
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '文件上传失败'
      set({
        uploadLoading: false,
        uploadError: errorMessage
      })
      throw error
    }
  },

  /**
   * 上传文本数据
   */
  uploadText: async (textData: UploadTextRequest) => {
    set({ uploadLoading: true, uploadError: null })
    
    try {
      const response = await trainingDataService.uploadText(textData)
      
      set({
        uploadLoading: false,
        uploadError: null
      })
      
      // 刷新数据列表
      await get().refreshDataList()
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '文本上传失败'
      set({
        uploadLoading: false,
        uploadError: errorMessage
      })
      throw error
    }
  },

  // ==================== 数据处理操作 ====================
  
  /**
   * 数据预处理
   */
  preprocessData: async (datasetId: string, request: PreprocessRequest) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        [`preprocess-${datasetId}`]: true
      },
      operationError: {
        ...state.operationError,
        [`preprocess-${datasetId}`]: null
      }
    }))
    
    try {
      const task = await trainingDataService.preprocessData(datasetId, request)
      
      set((state) => ({
        preprocessTasks: {
          ...state.preprocessTasks,
          [datasetId]: task
        },
        operationLoading: {
          ...state.operationLoading,
          [`preprocess-${datasetId}`]: false
        }
      }))
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '数据预处理失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`preprocess-${datasetId}`]: false
        },
        operationError: {
          ...state.operationError,
          [`preprocess-${datasetId}`]: errorMessage
        }
      }))
      throw error
    }
  },

  /**
   * 数据验证
   */
  validateData: async (datasetId: string, request?: ValidationRequest) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        [`validate-${datasetId}`]: true
      },
      operationError: {
        ...state.operationError,
        [`validate-${datasetId}`]: null
      }
    }))
    
    try {
      const result = await trainingDataService.validateData(datasetId, request)
      
      set((state) => ({
        validationResults: {
          ...state.validationResults,
          [datasetId]: result
        },
        operationLoading: {
          ...state.operationLoading,
          [`validate-${datasetId}`]: false
        }
      }))
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '数据验证失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`validate-${datasetId}`]: false
        },
        operationError: {
          ...state.operationError,
          [`validate-${datasetId}`]: errorMessage
        }
      }))
      throw error
    }
  },

  // ==================== 数据管理操作 ====================
  
  /**
   * 更新数据信息
   */
  updateData: async (datasetId: string, data: UpdateDataRequest) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        [`update-${datasetId}`]: true
      },
      operationError: {
        ...state.operationError,
        [`update-${datasetId}`]: null
      }
    }))
    
    try {
      await trainingDataService.updateData(datasetId, data)
      
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`update-${datasetId}`]: false
        }
      }))
      
      // 重新获取详情
      await get().fetchDataDetail(datasetId)
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '更新数据失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`update-${datasetId}`]: false
        },
        operationError: {
          ...state.operationError,
          [`update-${datasetId}`]: errorMessage
        }
      }))
      throw error
    }
  },

  /**
   * 删除数据
   */
  deleteData: async (datasetId: string, force = false) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        [`delete-${datasetId}`]: true
      },
      operationError: {
        ...state.operationError,
        [`delete-${datasetId}`]: null
      }
    }))
    
    try {
      await trainingDataService.deleteData(datasetId, { deleteFile: force })
      
      // 从列表中移除
      set((state) => ({
        dataList: state.dataList.filter(item => item.datasetId !== datasetId),
        dataListTotal: state.dataListTotal - 1,
        currentDataset: state.currentDataset?.datasetId === datasetId ? null : state.currentDataset,
        operationLoading: {
          ...state.operationLoading,
          [`delete-${datasetId}`]: false
        }
      }))
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '删除数据失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`delete-${datasetId}`]: false
        },
        operationError: {
          ...state.operationError,
          [`delete-${datasetId}`]: errorMessage
        }
      }))
      throw error
    }
  },

  /**
   * 下载数据
   */
  downloadData: async (datasetId: string) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        [`download-${datasetId}`]: true
      },
      operationError: {
        ...state.operationError,
        [`download-${datasetId}`]: null
      }
    }))
    
    try {
      const blob = await trainingDataService.downloadData(datasetId)
      
      // 创建下载链接
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.style.display = 'none'
      a.href = url
      a.download = `dataset-${datasetId}.zip`
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
      
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`download-${datasetId}`]: false
        }
      }))
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '下载数据失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`download-${datasetId}`]: false
        },
        operationError: {
          ...state.operationError,
          [`download-${datasetId}`]: errorMessage
        }
      }))
      throw error
    }
  },

  // ==================== 批量操作 ====================
  
  /**
   * 批量操作
   */
  batchOperation: async (request: BatchOperationRequest) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        'batch-operation': true
      },
      operationError: {
        ...state.operationError,
        'batch-operation': null
      }
    }))
    
    try {
      const result = await trainingDataService.batchOperation(request)
      
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          'batch-operation': false
        }
      }))
      
      // 刷新数据列表
      await get().refreshDataList()
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '批量操作失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          'batch-operation': false
        },
        operationError: {
          ...state.operationError,
          'batch-operation': errorMessage
        }
      }))
      throw error
    }
  },

  // ==================== 数据导出 ====================
  
  /**
   * 导出数据
   */
  exportData: async (request: ExportDataRequest) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        'export-data': true
      },
      operationError: {
        ...state.operationError,
        'export-data': null
      }
    }))
    
    try {
      const task = await trainingDataService.exportData(request)
      
      set((state) => ({
        exportTasks: {
          ...state.exportTasks,
          [task.taskId]: task
        },
        operationLoading: {
          ...state.operationLoading,
          'export-data': false
        }
      }))
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '数据导出失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          'export-data': false
        },
        operationError: {
          ...state.operationError,
          'export-data': errorMessage
        }
      }))
      throw error
    }
  },

  // ==================== 数据统计 ====================
  
  /**
   * 获取数据统计
   */
  fetchStatistics: async (params?: DataStatisticsParams) => {
    set({ statisticsLoading: true, statisticsError: null })
    
    try {
      const statistics = await trainingDataService.getDataStatistics(params)
      
      set({
        statistics,
        statisticsLoading: false,
        statisticsError: null
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取数据统计失败'
      set({
        statisticsLoading: false,
        statisticsError: errorMessage
      })
      throw error
    }
  },

  // ==================== 分页操作 ====================
  
  /**
   * 设置分页参数
   */
  setPagination: (page: number, size?: number) => {
    set((state) => ({
      pagination: {
        ...state.pagination,
        page,
        size: size || state.pagination.size
      }
    }))
  },

  // ==================== 查询参数操作 ====================
  
  /**
   * 设置查询参数
   */
  setQueryParams: (params: DataListParams) => {
    set({ queryParams: params })
  },

  /**
   * 重置查询参数
   */
  resetQueryParams: () => {
    set({ queryParams: {} })
  },

  // ==================== 错误处理 ====================
  
  /**
   * 清除错误信息
   */
  clearError: () => {
    set({
      dataListError: null,
      currentDatasetError: null,
      uploadError: null,
      statisticsError: null,
      operationError: {}
    })
  },

  /**
   * 清除特定数据的错误信息
   */
  clearDataError: (datasetId: string) => {
    set((state) => {
      const newOperationError = { ...state.operationError }
      Object.keys(newOperationError).forEach(key => {
        if (key.includes(datasetId)) {
          delete newOperationError[key]
        }
      })
      return { operationError: newOperationError }
    })
  },

  // ==================== 状态重置 ====================
  
  /**
   * 重置状态
   */
  resetState: () => {
    set(initialState)
  }
}))

// ==================== 导出类型 ====================
export type { DataState, DataActions, DataStore }
