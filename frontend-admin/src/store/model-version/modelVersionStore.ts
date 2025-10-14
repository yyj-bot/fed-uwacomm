/**
 * 模型版本管理状态 Store
 * 管理模型版本的上传、评估、部署、回滚等相关状态和操作
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { create } from 'zustand'
import { modelVersionService } from '@/services'
import type { 
  ModelVersionDetail,
  TaskModelVersions,
  EvaluationResult,
  RollbackInfo,
  RollbackRequest,
  ModelStatistics,
  TaskStatistics,
  // 初始模型管理相关类型
  InitialModelInfo,
  InitialModelGenerationRequest,
  InitialModelGenerationResponse,
  InitialModelUploadRequest,
  InitialModelUploadResponse,
  ModelDistributionRequest,
  ModelDistributionResponse,
  DistributionStatusDetail,
  InitialModelDeleteRequest,
  InitialModelDeleteResponse
} from '@/services'
import type { 
  ModelVersionListParams,
  EvaluationRequest,
  BatchEvaluationRequest,
  DownloadRequest,
  DeleteModelRequest,
  StatisticsParams
} from '@/services'

// ==================== 状态类型定义 ====================

interface ModelState {
  // 模型版本列表状态
  modelList: any[]
  modelListTotal: number
  modelListLoading: boolean
  modelListError: string | null
  
  // 当前选中的模型
  currentModel: ModelVersionDetail | null
  currentModelLoading: boolean
  currentModelError: string | null
  
  // 任务模型版本
  taskModels: Record<string, TaskModelVersions>
  taskModelsLoading: Record<string, boolean>
  
  
  // ==================== 初始模型管理状态 ====================
  
  // 初始模型信息
  initialModels: Record<string, InitialModelInfo>
  initialModelLoading: Record<string, boolean>
  initialModelError: Record<string, string | null>
  
  // 初始模型生成状态
  generationLoading: boolean
  generationError: string | null
  
  // 初始模型上传状态
  initialUploadLoading: boolean
  initialUploadError: string | null
  initialUploadProgress: Record<string, number>
  
  // 模型分发状态
  distributions: Record<string, DistributionStatusDetail>
  distributionLoading: Record<string, boolean>
  distributionError: Record<string, string | null>
  
  // 评估结果
  evaluationResults: Record<string, EvaluationResult[]>
  evaluationLoading: Record<string, boolean>
  evaluationResultsLoading: boolean
  
  // 回滚历史
  rollbackHistory: Record<string, RollbackInfo[]>
  rollbackLoading: boolean
  
  
  // 统计数据
  modelStatistics: ModelStatistics | null
  taskStatistics: Record<string, TaskStatistics>
  statisticsLoading: boolean
  
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
  queryParams: ModelVersionListParams
}

interface ModelActions {
  // 模型列表操作
  fetchModelList: (params?: ModelVersionListParams) => Promise<void>
  refreshModelList: () => Promise<void>
  
  // 模型详情操作
  fetchModelDetail: (modelId: string) => Promise<void>
  setCurrentModel: (model: ModelVersionDetail | null) => void
  
  
  // 任务模型版本
  fetchTaskModels: (taskId: string, params?: any) => Promise<void>
  
  // ==================== 初始模型管理操作 ====================
  
  // 初始模型生成操作
  generateInitialModel: (generationData: InitialModelGenerationRequest) => Promise<string>
  
  // 初始模型上传操作
  uploadCustomInitialModel: (uploadData: InitialModelUploadRequest) => Promise<string>
  
  // 初始模型查询操作
  fetchTaskInitialModel: (taskId: string, params?: { includeParameters?: boolean; format?: 'json' | 'binary' }) => Promise<void>
  
  // 模型分发操作
  distributeInitialModel: (taskId: string, distributionData: ModelDistributionRequest) => Promise<string>
  fetchDistributionStatus: (distributionId: string) => Promise<void>
  
  // 初始模型下载操作
  downloadInitialModel: (taskId: string, params?: { format?: 'binary' | 'json' }) => Promise<void>
  
  // 初始模型删除操作
  deleteInitialModel: (taskId: string, deleteData?: InitialModelDeleteRequest) => Promise<void>
  
  // 模型评估操作
  evaluateModel: (modelId: string, request: EvaluationRequest) => Promise<void>
  batchEvaluateModels: (request: BatchEvaluationRequest) => Promise<any>
  fetchEvaluationResults: (modelId?: string) => Promise<void>
  
  // 模型回滚操作
  rollbackModel: (rollbackData: RollbackRequest) => Promise<void>
  fetchRollbackHistory: (params?: { deploymentId?: string; page?: number; size?: number }) => Promise<void>
  
  // 模型下载操作
  downloadModel: (modelId: string, params?: DownloadRequest) => Promise<void>
  downloadModelBatch: (modelIds: string[], params?: any) => Promise<void>
  
  // 模型删除操作
  deleteModel: (modelId: string, params?: DeleteModelRequest) => Promise<void>
  deleteModelBatch: (modelIds: string[], params?: any) => Promise<void>
  
  // 统计数据
  fetchModelStatistics: (params?: StatisticsParams) => Promise<void>
  fetchTaskStatistics: (taskId: string) => Promise<void>
  
  // 分页操作
  setPagination: (page: number, size?: number) => void
  
  // 查询参数操作
  setQueryParams: (params: ModelVersionListParams) => void
  resetQueryParams: () => void
  
  // 错误处理
  clearError: () => void
  clearModelError: (modelId: string) => void
  
  // 状态重置
  resetState: () => void
}

type ModelStore = ModelState & ModelActions

// ==================== 初始状态 ====================

const initialState: ModelState = {
  modelList: [],
  modelListTotal: 0,
  modelListLoading: false,
  modelListError: null,
  
  currentModel: null,
  currentModelLoading: false,
  currentModelError: null,
  
  taskModels: {},
  taskModelsLoading: {},
  
  // 初始模型管理状态
  initialModels: {},
  initialModelLoading: {},
  initialModelError: {},
  
  generationLoading: false,
  generationError: null,
  
  initialUploadLoading: false,
  initialUploadError: null,
  initialUploadProgress: {},
  
  distributions: {},
  distributionLoading: {},
  distributionError: {},
  
  evaluationResults: {},
  evaluationLoading: {},
  evaluationResultsLoading: false,
  
  rollbackHistory: {},
  rollbackLoading: false,

  modelStatistics: null,
  taskStatistics: {},
  statisticsLoading: false,
  
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

export const useModelStore = create<ModelStore>((set, get) => ({
  ...initialState,

  // ==================== 模型列表操作 ====================
  
  /**
   * 获取模型版本列表
   */
  fetchModelList: async (params?: ModelVersionListParams) => {
    const { pagination, queryParams } = get()
    const finalParams = {
      ...queryParams,
      ...params,
      page: params?.page || pagination.page,
      size: params?.size || pagination.size
    }
    
    set({ modelListLoading: true, modelListError: null })
    
    try {
      const response = await modelVersionService.getModelVersions(finalParams)
      
      set({
        modelList: response.records,
        modelListTotal: response.total,
        modelListLoading: false,
        modelListError: null,
        pagination: {
          page: response.current,
          size: response.size,
          total: response.total
        }
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取模型列表失败'
      set({
        modelListLoading: false,
        modelListError: errorMessage
      })
      throw error
    }
  },

  /**
   * 刷新模型列表
   */
  refreshModelList: async () => {
    const { fetchModelList, queryParams, pagination } = get()
    await fetchModelList({
      ...queryParams,
      page: pagination.page,
      size: pagination.size
    })
  },

  // ==================== 模型详情操作 ====================
  
  /**
   * 获取模型详情
   */
  fetchModelDetail: async (modelId: string) => {
    set({ currentModelLoading: true, currentModelError: null })
    
    try {
      const model = await modelVersionService.getModelVersionDetail(modelId)
      
      set({
        currentModel: model,
        currentModelLoading: false,
        currentModelError: null
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取模型详情失败'
      set({
        currentModelLoading: false,
        currentModelError: errorMessage
      })
      throw error
    }
  },

  /**
   * 设置当前模型
   */
  setCurrentModel: (model: ModelVersionDetail | null) => {
    set({ currentModel: model })
  },


  // ==================== 任务模型版本 ====================
  
  /**
   * 获取任务模型版本
   */
  fetchTaskModels: async (taskId: string, params?: any) => {
    set((state) => ({
      taskModelsLoading: {
        ...state.taskModelsLoading,
        [taskId]: true
      }
    }))
    
    try {
      const taskModels = await modelVersionService.getTaskModelVersions(taskId, params)
      
      set((state) => ({
        taskModels: {
          ...state.taskModels,
          [taskId]: taskModels
        },
        taskModelsLoading: {
          ...state.taskModelsLoading,
          [taskId]: false
        }
      }))
    } catch (error) {
      set((state) => ({
        taskModelsLoading: {
          ...state.taskModelsLoading,
          [taskId]: false
        }
      }))
      throw error
    }
  },

  // ==================== 初始模型管理操作 ====================
  
  /**
   * 生成随机初始模型
   */
  generateInitialModel: async (generationData: InitialModelGenerationRequest) => {
    set({ generationLoading: true, generationError: null })
    
    try {
      const response = await modelVersionService.generateInitialModel(generationData)
      
      // 更新初始模型状态
      set((state) => ({
        generationLoading: false,
        generationError: null,
        initialModels: {
          ...state.initialModels,
          [generationData.taskId]: {
            modelId: response.modelId,
            taskId: response.taskId,
            modelType: response.modelType,
            modelSize: response.modelSize,
            createdAt: response.generatedAt,
            status: response.status as any,
            architecture: response.architecture,
            distributionStatus: {
              totalVms: 0,
              distributedVms: 0,
              failedVms: 0
            },
            checksum: response.checksum
          }
        }
      }))
      
      return response.modelId
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '初始模型生成失败'
      set({
        generationLoading: false,
        generationError: errorMessage
      })
      throw error
    }
  },

  /**
   * 上传自定义初始模型
   */
  uploadCustomInitialModel: async (uploadData: InitialModelUploadRequest) => {
    set({ initialUploadLoading: true, initialUploadError: null })
    
    try {
      const response = await modelVersionService.uploadCustomInitialModel(uploadData)
      
      // 更新初始模型状态
      set((state) => ({
        initialUploadLoading: false,
        initialUploadError: null,
        initialModels: {
          ...state.initialModels,
          [uploadData.taskId]: {
            modelId: response.modelId,
            taskId: response.taskId,
            modelType: response.modelType,
            modelSize: response.modelSize,
            createdAt: response.uploadedAt,
            status: response.status as any,
            architecture: {
              inputSize: response.metadata?.architecture?.inputSize || 0,
              hiddenLayers: [],
              outputSize: response.metadata?.architecture?.outputSize || 0,
              activationFunction: '',
              optimizer: '',
              learningRate: 0
            },
            distributionStatus: {
              totalVms: 0,
              distributedVms: 0,
              failedVms: 0
            },
            checksum: response.checksum
          }
        }
      }))
      
      return response.modelId
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '自定义初始模型上传失败'
      set({
        initialUploadLoading: false,
        initialUploadError: errorMessage
      })
      throw error
    }
  },

  /**
   * 获取任务初始模型
   */
  fetchTaskInitialModel: async (taskId: string, params?: { includeParameters?: boolean; format?: 'json' | 'binary' }) => {
    set((state) => ({
      initialModelLoading: {
        ...state.initialModelLoading,
        [taskId]: true
      },
      initialModelError: {
        ...state.initialModelError,
        [taskId]: null
      }
    }))
    
    try {
      const initialModel = await modelVersionService.getTaskInitialModel(taskId, params)
      
      set((state) => ({
        initialModelLoading: {
          ...state.initialModelLoading,
          [taskId]: false
        },
        initialModels: {
          ...state.initialModels,
          [taskId]: initialModel
        }
      }))
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取初始模型失败'
      set((state) => ({
        initialModelLoading: {
          ...state.initialModelLoading,
          [taskId]: false
        },
        initialModelError: {
          ...state.initialModelError,
          [taskId]: errorMessage
        }
      }))
      throw error
    }
  },

  /**
   * 分发初始模型
   */
  distributeInitialModel: async (taskId: string, distributionData: ModelDistributionRequest) => {
    set((state) => ({
      distributionLoading: {
        ...state.distributionLoading,
        [taskId]: true
      },
      distributionError: {
        ...state.distributionError,
        [taskId]: null
      }
    }))
    
    try {
      const response = await modelVersionService.distributeInitialModel(taskId, distributionData)
      
      set((state) => ({
        distributionLoading: {
          ...state.distributionLoading,
          [taskId]: false
        },
        distributions: {
          ...state.distributions,
          [response.distributionId]: {
            distributionId: response.distributionId,
            taskId: response.taskId,
            modelId: response.modelId,
            status: response.status as any,
            startedAt: response.startedAt,
            progress: response.progress,
            vmDetails: []
          }
        }
      }))
      
      return response.distributionId
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '模型分发失败'
      set((state) => ({
        distributionLoading: {
          ...state.distributionLoading,
          [taskId]: false
        },
        distributionError: {
          ...state.distributionError,
          [taskId]: errorMessage
        }
      }))
      throw error
    }
  },

  /**
   * 获取分发状态
   */
  fetchDistributionStatus: async (distributionId: string) => {
    try {
      const status = await modelVersionService.getDistributionStatus(distributionId)
      
      set((state) => ({
        distributions: {
          ...state.distributions,
          [distributionId]: status
        }
      }))
    } catch (error) {
      console.error(`获取分发状态失败 (${distributionId}):`, error)
    }
  },

  /**
   * 下载初始模型
   */
  downloadInitialModel: async (taskId: string, params?: { format?: 'binary' | 'json', modelId?: string }) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        [`download-initial-${taskId}`]: true
      }
    }))
    
    try {
      const blob = await modelVersionService.downloadInitialModel(taskId, params)
      
      // 创建下载链接
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.style.display = 'none'
      a.href = url
      a.download = `initial-model-${taskId}.${params?.format === 'json' ? 'json' : 'bin'}`
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
      
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`download-initial-${taskId}`]: false
        }
      }))
    } catch (error) {
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`download-initial-${taskId}`]: false
        }
      }))
      throw error
    }
  },

  /**
   * 删除初始模型
   */
  deleteInitialModel: async (taskId: string, deleteData?: InitialModelDeleteRequest) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        [`delete-initial-${taskId}`]: true
      }
    }))
    
    try {
      await modelVersionService.deleteInitialModel(taskId, deleteData)
      
      // 从状态中移除
      set((state) => {
        const newInitialModels = { ...state.initialModels }
        delete newInitialModels[taskId]
        
        return {
          initialModels: newInitialModels,
          operationLoading: {
            ...state.operationLoading,
            [`delete-initial-${taskId}`]: false
          }
        }
      })
    } catch (error) {
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`delete-initial-${taskId}`]: false
        }
      }))
      throw error
    }
  },

  // ==================== 模型评估操作 ====================
  
  /**
   * 评估模型
   */
  evaluateModel: async (modelId: string, request: EvaluationRequest) => {
    set((state) => ({
      evaluationLoading: {
        ...state.evaluationLoading,
        [modelId]: true
      },
      operationError: {
        ...state.operationError,
        [`evaluate-${modelId}`]: null
      }
    }))
    
    try {
      await modelVersionService.evaluateModel(request)
      
      set((state) => ({
        evaluationLoading: {
          ...state.evaluationLoading,
          [modelId]: false
        }
      }))
      
      // 获取评估结果
      await get().fetchEvaluationResults(modelId)
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '模型评估失败'
      set((state) => ({
        evaluationLoading: {
          ...state.evaluationLoading,
          [modelId]: false
        },
        operationError: {
          ...state.operationError,
          [`evaluate-${modelId}`]: errorMessage
        }
      }))
      throw error
    }
  },

  /**
   * 获取评估结果
   */
  fetchEvaluationResults: async (modelId?: string) => {
    set({ evaluationResultsLoading: true })
    
    try {
      const params = modelId ? { modelId } : { page: 1, size: 50 }
      const response = await modelVersionService.getEvaluationResults(params)
      
      if (modelId) {
        // 为特定模型存储评估结果
        set((state) => ({
          evaluationResults: {
            ...state.evaluationResults,
            [modelId]: response.records
          },
          evaluationResultsLoading: false
        }))
      } else {
        // 存储所有评估结果，按模型ID分组
        const groupedResults: Record<string, EvaluationResult[]> = {}
        response.records.forEach(result => {
          if (!groupedResults[result.modelId]) {
            groupedResults[result.modelId] = []
          }
          groupedResults[result.modelId].push(result)
        })
        
        set((state) => ({
          evaluationResults: {
            ...state.evaluationResults,
            ...groupedResults
          },
          evaluationResultsLoading: false
        }))
      }
    } catch (error) {
      console.error(`获取评估结果失败 (${modelId || 'all'}):`, error)
      set({ evaluationResultsLoading: false })
    }
  },

  /**
   * 批量评估模型
   */
  batchEvaluateModels: async (request: BatchEvaluationRequest) => {
    set((state) => ({
      evaluationLoading: {
        ...state.evaluationLoading,
        [`batch-${request.taskId}`]: true
      },
      operationError: {
        ...state.operationError,
        [`batch-evaluate-${request.taskId}`]: null
      }
    }))
    
    try {
      const response = await modelVersionService.evaluateModelBatch(request)
      
      set((state) => ({
        evaluationLoading: {
          ...state.evaluationLoading,
          [`batch-${request.taskId}`]: false
        }
      }))
      
      // 批量评估完成后，刷新评估结果
      await get().fetchEvaluationResults()
      
      return response
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '批量评估失败'
      set((state) => ({
        evaluationLoading: {
          ...state.evaluationLoading,
          [`batch-${request.taskId}`]: false
        },
        operationError: {
          ...state.operationError,
          [`batch-evaluate-${request.taskId}`]: errorMessage
        }
      }))
      throw error
    }
  },


  // ==================== 模型回滚操作 ====================
  
  /**
   * 回滚模型
   */
  rollbackModel: async (rollbackData: RollbackRequest) => {
    set((state) => ({
      rollbackLoading: true,
      operationError: {
        ...state.operationError,
        [`rollback-${rollbackData.deploymentId}`]: null
      }
    }))
    
    try {
      await modelVersionService.rollbackModel(rollbackData)
      
      set((state) => ({
        rollbackLoading: false
      }))
      
      // 刷新回滚历史
      await get().fetchRollbackHistory({ deploymentId: rollbackData.deploymentId })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '模型回滚失败'
      set((state) => ({
        rollbackLoading: false,
        operationError: {
          ...state.operationError,
          [`rollback-${rollbackData.deploymentId}`]: errorMessage
        }
      }))
      throw error
    }
  },

  /**
   * 获取回滚历史
   */
  fetchRollbackHistory: async (params?: { deploymentId?: string; page?: number; size?: number }) => {
    try {
      const response = await modelVersionService.getRollbackHistory(params)
      
      // 按 deploymentId 存储回滚历史
      const deploymentId = params?.deploymentId || 'default'
      
      set((state) => ({
        rollbackHistory: {
          ...state.rollbackHistory,
          [deploymentId]: response.records
        }
      }))
    } catch (error) {
      console.error('获取回滚历史失败:', error)
    }
  },

  // ==================== 模型下载操作 ====================
  
  /**
   * 下载模型
   */
  downloadModel: async (modelId: string, params?: DownloadRequest) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        [`download-${modelId}`]: true
      }
    }))
    
    try {
      const blob = await modelVersionService.downloadModel(modelId, params)
      
      // 创建下载链接
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.style.display = 'none'
      a.href = url
      a.download = `model-${modelId}.zip`
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
      
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`download-${modelId}`]: false
        }
      }))
    } catch (error) {
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`download-${modelId}`]: false
        }
      }))
      throw error
    }
  },

  /**
   * 批量下载模型
   */
  downloadModelBatch: async (modelIds: string[], params?: any) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        'batch-download': true
      }
    }))
    
    try {
      const blob = await modelVersionService.downloadModelBatch({ modelIds, ...params })
      
      // 创建下载链接
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.style.display = 'none'
      a.href = url
      a.download = `models-batch-${Date.now()}.zip`
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
      
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          'batch-download': false
        }
      }))
    } catch (error) {
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          'batch-download': false
        }
      }))
      throw error
    }
  },

  // ==================== 模型删除操作 ====================
  
  /**
   * 删除模型
   */
  deleteModel: async (modelId: string, params?: DeleteModelRequest) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        [`delete-${modelId}`]: true
      }
    }))
    
    try {
      await modelVersionService.deleteModel(modelId, params)
      
      // 从列表中移除
      set((state) => ({
        modelList: state.modelList.filter(model => model.modelId !== modelId),
        modelListTotal: state.modelListTotal - 1,
        currentModel: state.currentModel?.modelId === modelId ? null : state.currentModel,
        operationLoading: {
          ...state.operationLoading,
          [`delete-${modelId}`]: false
        }
      }))
    } catch (error) {
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`delete-${modelId}`]: false
        }
      }))
      throw error
    }
  },

  /**
   * 批量删除模型
   */
  deleteModelBatch: async (modelIds: string[], params?: any) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        'batch-delete': true
      }
    }))
    
    try {
      await modelVersionService.deleteModelBatch({ modelIds, ...params })
      
      // 从列表中移除
      set((state) => ({
        modelList: state.modelList.filter(model => !modelIds.includes(model.modelId)),
        modelListTotal: state.modelListTotal - modelIds.length,
        operationLoading: {
          ...state.operationLoading,
          'batch-delete': false
        }
      }))
    } catch (error) {
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          'batch-delete': false
        }
      }))
      throw error
    }
  },

  // ==================== 统计数据 ====================
  
  /**
   * 获取模型统计
   */
  fetchModelStatistics: async (params?: StatisticsParams) => {
    set({ statisticsLoading: true })
    
    try {
      const statistics = await modelVersionService.getModelStatistics(params)
      
      set({
        modelStatistics: statistics,
        statisticsLoading: false
      })
    } catch (error) {
      set({ statisticsLoading: false })
      throw error
    }
  },

  /**
   * 获取任务统计
   */
  fetchTaskStatistics: async (taskId: string) => {
    try {
      const statistics = await modelVersionService.getTaskModelStatistics(taskId)
      
      set((state) => ({
        taskStatistics: {
          ...state.taskStatistics,
          [taskId]: statistics
        }
      }))
    } catch (error) {
      console.error(`获取任务统计失败 (${taskId}):`, error)
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
  setQueryParams: (params: ModelVersionListParams) => {
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
      modelListError: null,
      currentModelError: null,
      operationError: {}
    })
  },

  /**
   * 清除特定模型的错误信息
   */
  clearModelError: (modelId: string) => {
    set((state) => {
      const newOperationError = { ...state.operationError }
      Object.keys(newOperationError).forEach(key => {
        if (key.includes(modelId)) {
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
export type { ModelState, ModelActions, ModelStore }
