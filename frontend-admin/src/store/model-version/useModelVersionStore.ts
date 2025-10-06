/**
 * 模型版本管理 Hook - 封装模型状态和操作
 * 为组件层提供简洁的模型版本管理功能访问接口
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { useCallback } from 'react'
import { useModelStore } from './modelVersionStore'
import useFederatedTaskStore from '../federated-task/useFederatedTaskStore'
import type { 
  ModelVersionDetail,
  TaskModelVersions,
  EvaluationResult,
  RollbackInfo,
  ModelStatistics,
  TaskStatistics,
  ModelVersionListParams,
  EvaluationRequest,
  RollbackRequest,
  DownloadRequest,
  DeleteModelRequest,
  StatisticsParams,
  // 初始模型管理相关类型
  InitialModelInfo,
  InitialModelGenerationRequest,
  InitialModelUploadRequest,
  ModelDistributionRequest,
  DistributionStatusDetail,
  InitialModelDeleteRequest
} from '@/services'

// ==================== Hook 实现 ====================

export const useModel = () => {
  // 获取联邦学习任务相关状态和方法
  const { taskList, fetchTaskDetail, currentTask } = useFederatedTaskStore()
  
  // 获取状态
  const modelList = useModelStore((state) => state.modelList)
  const modelListTotal = useModelStore((state) => state.modelListTotal)
  const modelListLoading = useModelStore((state) => state.modelListLoading)
  const modelListError = useModelStore((state) => state.modelListError)
  
  const currentModel = useModelStore((state) => state.currentModel)
  const currentModelLoading = useModelStore((state) => state.currentModelLoading)
  const currentModelError = useModelStore((state) => state.currentModelError)
  
  const taskModels = useModelStore((state) => state.taskModels)
  const taskModelsLoading = useModelStore((state) => state.taskModelsLoading)
  
  
  const evaluationResults = useModelStore((state) => state.evaluationResults)
  const evaluationLoading = useModelStore((state) => state.evaluationLoading)
  
  const rollbackHistory = useModelStore((state) => state.rollbackHistory)
  const rollbackLoading = useModelStore((state) => state.rollbackLoading)

  const modelStatistics = useModelStore((state) => state.modelStatistics)
  const taskStatistics = useModelStore((state) => state.taskStatistics)
  const statisticsLoading = useModelStore((state) => state.statisticsLoading)
  
  const operationLoading = useModelStore((state) => state.operationLoading)
  const operationError = useModelStore((state) => state.operationError)
  
  const pagination = useModelStore((state) => state.pagination)
  const queryParams = useModelStore((state) => state.queryParams)
  
  // ==================== 初始模型管理状态 ====================
  
  const initialModels = useModelStore((state) => state.initialModels)
  const initialModelLoading = useModelStore((state) => state.initialModelLoading)
  const initialModelError = useModelStore((state) => state.initialModelError)
  
  const generationLoading = useModelStore((state) => state.generationLoading)
  const generationError = useModelStore((state) => state.generationError)
  
  const initialUploadLoading = useModelStore((state) => state.initialUploadLoading)
  const initialUploadError = useModelStore((state) => state.initialUploadError)
  const initialUploadProgress = useModelStore((state) => state.initialUploadProgress)
  
  const distributions = useModelStore((state) => state.distributions)
  const distributionLoading = useModelStore((state) => state.distributionLoading)
  const distributionError = useModelStore((state) => state.distributionError)

  // 获取操作方法
  const fetchModelListAction = useModelStore((state) => state.fetchModelList)
  const refreshModelListAction = useModelStore((state) => state.refreshModelList)
  const fetchModelDetailAction = useModelStore((state) => state.fetchModelDetail)
  const setCurrentModelAction = useModelStore((state) => state.setCurrentModel)
  const fetchTaskModelsAction = useModelStore((state) => state.fetchTaskModels)
  const evaluateModelAction = useModelStore((state) => state.evaluateModel)
  const fetchEvaluationResultsAction = useModelStore((state) => state.fetchEvaluationResults)
  const rollbackModelAction = useModelStore((state) => state.rollbackModel)
  const fetchRollbackHistoryAction = useModelStore((state) => state.fetchRollbackHistory)
  const downloadModelAction = useModelStore((state) => state.downloadModel)
  const downloadModelBatchAction = useModelStore((state) => state.downloadModelBatch)
  const deleteModelAction = useModelStore((state) => state.deleteModel)
  const deleteModelBatchAction = useModelStore((state) => state.deleteModelBatch)
  const fetchModelStatisticsAction = useModelStore((state) => state.fetchModelStatistics)
  const fetchTaskStatisticsAction = useModelStore((state) => state.fetchTaskStatistics)
  const setPaginationAction = useModelStore((state) => state.setPagination)
  const setQueryParamsAction = useModelStore((state) => state.setQueryParams)
  const resetQueryParamsAction = useModelStore((state) => state.resetQueryParams)
  const clearErrorAction = useModelStore((state) => state.clearError)
  const clearModelErrorAction = useModelStore((state) => state.clearModelError)
  const resetStateAction = useModelStore((state) => state.resetState)
  
  // ==================== 初始模型管理操作方法 ====================
  
  const generateInitialModelAction = useModelStore((state) => state.generateInitialModel)
  const uploadCustomInitialModelAction = useModelStore((state) => state.uploadCustomInitialModel)
  const fetchTaskInitialModelAction = useModelStore((state) => state.fetchTaskInitialModel)
  const distributeInitialModelAction = useModelStore((state) => state.distributeInitialModel)
  const fetchDistributionStatusAction = useModelStore((state) => state.fetchDistributionStatus)
  const downloadInitialModelAction = useModelStore((state) => state.downloadInitialModel)
  const deleteInitialModelAction = useModelStore((state) => state.deleteInitialModel)

  // ==================== 封装操作方法 ====================

  /**
   * 获取模型列表
   */
  const fetchModelList = useCallback(async (params?: ModelVersionListParams) => {
    try {
      await fetchModelListAction(params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取模型列表失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchModelListAction])

  /**
   * 刷新模型列表
   */
  const refreshModelList = useCallback(async () => {
    try {
      await refreshModelListAction()
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '刷新模型列表失败'
      return { success: false, error: errorMessage }
    }
  }, [refreshModelListAction])

  /**
   * 获取模型详情
   */
  const fetchModelDetail = useCallback(async (modelId: string) => {
    try {
      await fetchModelDetailAction(modelId)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取模型详情失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchModelDetailAction])

  /**
   * 设置当前模型
   */
  const setCurrentModel = useCallback((model: ModelVersionDetail | null) => {
    setCurrentModelAction(model)
  }, [setCurrentModelAction])


  /**
   * 获取任务模型版本
   */
  const fetchTaskModels = useCallback(async (taskId: string, params?: any) => {
    try {
      await fetchTaskModelsAction(taskId, params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取任务模型失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchTaskModelsAction])

  /**
   * 评估模型
   */
  const evaluateModel = useCallback(async (modelId: string, request: EvaluationRequest) => {
    try {
      await evaluateModelAction(modelId, request)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '模型评估失败'
      return { success: false, error: errorMessage }
    }
  }, [evaluateModelAction])

  /**
   * 获取评估结果
   */
  const fetchEvaluationResults = useCallback(async (modelId?: string) => {
    try {
      await fetchEvaluationResultsAction(modelId)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取评估结果失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchEvaluationResultsAction])

  /**
   * 回滚模型
   */
  const rollbackModel = useCallback(async (rollbackData: RollbackRequest) => {
    try {
      await rollbackModelAction(rollbackData)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '模型回滚失败'
      return { success: false, error: errorMessage }
    }
  }, [rollbackModelAction])

  /**
   * 获取回滚历史
   */
  const fetchRollbackHistory = useCallback(async (params?: { deploymentId?: string; page?: number; size?: number }) => {
    try {
      await fetchRollbackHistoryAction(params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取回滚历史失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchRollbackHistoryAction])

  /**
   * 下载模型
   */
  const downloadModel = useCallback(async (modelId: string, params?: DownloadRequest) => {
    try {
      await downloadModelAction(modelId, params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '下载模型失败'
      return { success: false, error: errorMessage }
    }
  }, [downloadModelAction])

  /**
   * 批量下载模型
   */
  const downloadModelBatch = useCallback(async (modelIds: string[], params?: any) => {
    try {
      await downloadModelBatchAction(modelIds, params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '批量下载失败'
      return { success: false, error: errorMessage }
    }
  }, [downloadModelBatchAction])

  /**
   * 删除模型
   */
  const deleteModel = useCallback(async (modelId: string, params?: DeleteModelRequest) => {
    try {
      await deleteModelAction(modelId, params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '删除模型失败'
      return { success: false, error: errorMessage }
    }
  }, [deleteModelAction])

  /**
   * 批量删除模型
   */
  const deleteModelBatch = useCallback(async (modelIds: string[], params?: any) => {
    try {
      await deleteModelBatchAction(modelIds, params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '批量删除失败'
      return { success: false, error: errorMessage }
    }
  }, [deleteModelBatchAction])

  /**
   * 获取模型统计
   */
  const fetchModelStatistics = useCallback(async (params?: StatisticsParams) => {
    try {
      await fetchModelStatisticsAction(params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取模型统计失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchModelStatisticsAction])

  /**
   * 获取任务统计
   */
  const fetchTaskStatistics = useCallback(async (taskId: string) => {
    try {
      await fetchTaskStatisticsAction(taskId)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取任务统计失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchTaskStatisticsAction])

  /**
   * 设置分页参数
   */
  const setPagination = useCallback((page: number, size?: number) => {
    setPaginationAction(page, size)
  }, [setPaginationAction])

  /**
   * 设置查询参数
   */
  const setQueryParams = useCallback((params: ModelVersionListParams) => {
    setQueryParamsAction(params)
  }, [setQueryParamsAction])

  /**
   * 重置查询参数
   */
  const resetQueryParams = useCallback(() => {
    resetQueryParamsAction()
  }, [resetQueryParamsAction])

  /**
   * 清除错误信息
   */
  const clearError = useCallback(() => {
    clearErrorAction()
  }, [clearErrorAction])

  /**
   * 清除特定模型的错误信息
   */
  const clearModelError = useCallback((modelId: string) => {
    clearModelErrorAction(modelId)
  }, [clearModelErrorAction])

  /**
   * 重置状态
   */
  const resetState = useCallback(() => {
    resetStateAction()
  }, [resetStateAction])

  // ==================== 初始模型管理封装操作方法 ====================

  /**
   * 生成随机初始模型
   */
  const generateInitialModel = useCallback(async (generationData: InitialModelGenerationRequest) => {
    try {
      const modelId = await generateInitialModelAction(generationData)
      return { success: true, error: null, data: modelId }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '初始模型生成失败'
      return { success: false, error: errorMessage, data: null }
    }
  }, [generateInitialModelAction])

  /**
   * 上传自定义初始模型
   */
  const uploadCustomInitialModel = useCallback(async (uploadData: InitialModelUploadRequest) => {
    try {
      const modelId = await uploadCustomInitialModelAction(uploadData)
      return { success: true, error: null, data: modelId }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '自定义初始模型上传失败'
      return { success: false, error: errorMessage, data: null }
    }
  }, [uploadCustomInitialModelAction])

  /**
   * 获取任务初始模型
   */
  const fetchTaskInitialModel = useCallback(async (taskId: string, params?: { includeParameters?: boolean; format?: 'json' | 'binary' }) => {
    try {
      await fetchTaskInitialModelAction(taskId, params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取初始模型失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchTaskInitialModelAction])

  /**
   * 分发初始模型
   */
  const distributeInitialModel = useCallback(async (taskId: string, distributionData: ModelDistributionRequest) => {
    try {
      const distributionId = await distributeInitialModelAction(taskId, distributionData)
      return { success: true, error: null, data: distributionId }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '模型分发失败'
      return { success: false, error: errorMessage, data: null }
    }
  }, [distributeInitialModelAction])

  /**
   * 获取分发状态
   */
  const fetchDistributionStatus = useCallback(async (distributionId: string) => {
    try {
      await fetchDistributionStatusAction(distributionId)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取分发状态失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchDistributionStatusAction])

  /**
   * 下载初始模型
   */
  const downloadInitialModel = useCallback(async (taskId: string, params?: { format?: 'binary' | 'json', modelId?: string }) => {
    try {
      await downloadInitialModelAction(taskId, params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '下载初始模型失败'
      return { success: false, error: errorMessage }
    }
  }, [downloadInitialModelAction])

  /**
   * 删除初始模型
   */
  const deleteInitialModel = useCallback(async (taskId: string, deleteData?: InitialModelDeleteRequest) => {
    try {
      await deleteInitialModelAction(taskId, deleteData)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '删除初始模型失败'
      return { success: false, error: errorMessage }
    }
  }, [deleteInitialModelAction])

  // ==================== 计算属性 ====================

  /**
   * 获取任务模型版本
   */
  const getTaskModels = useCallback((taskId: string): TaskModelVersions | null => {
    return taskModels[taskId] || null
  }, [taskModels])

  /**
   * 检查任务模型是否正在加载
   */
  const isTaskModelsLoading = useCallback((taskId: string): boolean => {
    return !!taskModelsLoading[taskId]
  }, [taskModelsLoading])

  /**
   * 获取模型评估结果
   */
  const getEvaluationResults = useCallback((modelId: string): EvaluationResult[] => {
    return evaluationResults[modelId] || []
  }, [evaluationResults])

  /**
   * 检查模型是否正在评估
   */
  const isModelEvaluating = useCallback((modelId: string): boolean => {
    return !!evaluationLoading[modelId]
  }, [evaluationLoading])


  /**
   * 获取回滚历史
   */
  const getRollbackHistory = useCallback((deploymentId: string): RollbackInfo[] => {
    return rollbackHistory[deploymentId] || []
  }, [rollbackHistory])

  /**
   * 获取任务统计
   */
  const getTaskStatistics = useCallback((taskId: string): TaskStatistics | null => {
    return taskStatistics[taskId] || null
  }, [taskStatistics])

  /**
   * 检查模型是否正在执行操作
   */
  const isModelOperating = useCallback((modelId: string, operation?: string): boolean => {
    if (operation) {
      return !!operationLoading[`${operation}-${modelId}`]
    }
    
    // 检查是否有任何操作正在进行
    const operations = ['evaluate', 'deploy', 'download', 'delete']
    return operations.some(op => !!operationLoading[`${op}-${modelId}`])
  }, [operationLoading])

  /**
   * 获取模型操作错误
   */
  const getModelOperationError = useCallback((modelId: string, operation: string): string | null => {
    return operationError[`${operation}-${modelId}`] || null
  }, [operationError])


  /**
   * 检查模型是否可以评估
   */
  const canEvaluateModel = useCallback((model: any): boolean => {
    if (!model) return false
    return ['UPLOADED', 'VALIDATED'].includes(model.status) && !isModelOperating(model.modelId)
  }, [isModelOperating])

  /**
   * 检查模型是否可以删除
   */
  const canDeleteModel = useCallback((model: any): boolean => {
    if (!model) return false
    return !['DEPLOYING', 'DEPLOYED'].includes(model.status) && !isModelOperating(model.modelId)
  }, [isModelOperating])

  /**
   * 获取不同状态的模型数量
   */
  const getModelCountByStatus = useCallback((status: string): number => {
    return modelList.filter(model => model.status === status).length
  }, [modelList])


  /**
   * 检查是否有任何批量操作正在进行
   */
  const isBatchOperating = !!(operationLoading['batch-download'] || operationLoading['batch-delete'])

  // ==================== 初始模型管理计算属性 ====================

  /**
   * 获取任务初始模型
   */
  const getTaskInitialModel = useCallback((taskId: string): InitialModelInfo | null => {
    return initialModels[taskId] || null
  }, [initialModels])

  /**
   * 检查任务初始模型是否正在加载
   */
  const isTaskInitialModelLoading = useCallback((taskId: string): boolean => {
    return !!initialModelLoading[taskId]
  }, [initialModelLoading])

  /**
   * 获取任务初始模型错误
   */
  const getTaskInitialModelError = useCallback((taskId: string): string | null => {
    return initialModelError[taskId] || null
  }, [initialModelError])

  /**
   * 获取分发状态详情
   */
  const getDistributionStatus = useCallback((distributionId: string): DistributionStatusDetail | null => {
    return distributions[distributionId] || null
  }, [distributions])

  /**
   * 检查分发是否正在进行
   */
  const isDistributing = useCallback((taskId: string): boolean => {
    return !!distributionLoading[taskId]
  }, [distributionLoading])

  /**
   * 获取分发错误
   */
  const getDistributionError = useCallback((taskId: string): string | null => {
    return distributionError[taskId] || null
  }, [distributionError])

  /**
   * 获取任务信息
   */
  const getTaskInfo = useCallback((taskId: string) => {
    // 首先从任务列表中查找
    const taskFromList = taskList.find(task => task.taskId === taskId)
    if (taskFromList) return taskFromList
    
    // 如果当前任务匹配，返回当前任务
    if (currentTask && currentTask.taskId === taskId) return currentTask
    
    return null
  }, [taskList, currentTask])

  /**
   * 检查初始模型是否可以分发
   */
  const canDistributeInitialModel = useCallback((taskId: string): boolean => {
    const initialModel = getTaskInitialModel(taskId)
    if (!initialModel) return false
    
    // 根据接口文档，只检查模型状态和分发状态
    // 接口文档没有明确要求检查任务状态
    return initialModel.status === 'READY' && !isDistributing(taskId)
  }, [getTaskInitialModel, isDistributing])

  /**
   * 检查初始模型是否可以删除
   */
  const canDeleteInitialModel = useCallback((taskId: string): boolean => {
    const initialModel = getTaskInitialModel(taskId)
    if (!initialModel) return false
    // 根据接口文档，所有状态都可以删除，DISTRIBUTING/DISTRIBUTED需要force=true
    // 前端不再限制这些状态的删除，而是在删除时自动添加force参数
    return !isDistributing(taskId) // 只检查是否有分发操作正在进行
  }, [getTaskInitialModel, isDistributing])

  /**
   * 检查是否有初始模型生成操作正在进行
   */
  const isGenerating = generationLoading

  /**
   * 检查是否有初始模型上传操作正在进行
   */
  const isInitialUploading = initialUploadLoading

  /**
   * 检查任务是否有初始模型
   */
  const hasInitialModel = useCallback((taskId: string): boolean => {
    return !!getTaskInitialModel(taskId)
  }, [getTaskInitialModel])

  /**
   * 获取初始模型上传进度
   */
  const getInitialUploadProgress = useCallback((taskId: string): number => {
    return initialUploadProgress[taskId] || 0
  }, [initialUploadProgress])

  // ==================== 返回接口 ====================

  return {
    // 状态
    modelList,
    modelListTotal,
    modelListLoading,
    modelListError,
    currentModel,
    currentModelLoading,
    currentModelError,
    taskModels,
    taskModelsLoading,
    evaluationResults,
    evaluationLoading,
    rollbackHistory,
    rollbackLoading,
    modelStatistics,
    taskStatistics,
    statisticsLoading,
    operationLoading,
    operationError,
    pagination,
    queryParams,
    
    // 初始模型管理状态
    initialModels,
    initialModelLoading,
    initialModelError,
    generationLoading,
    generationError,
    initialUploadLoading,
    initialUploadError,
    initialUploadProgress,
    distributions,
    distributionLoading,
    distributionError,
    
    // 操作方法
    fetchModelList,
    refreshModelList,
    fetchModelDetail,
    setCurrentModel,
    fetchTaskModels,
    evaluateModel,
    fetchEvaluationResults,
    rollbackModel,
    fetchRollbackHistory,
    downloadModel,
    downloadModelBatch,
    deleteModel,
    deleteModelBatch,
    fetchModelStatistics,
    fetchTaskStatistics,
    setPagination,
    setQueryParams,
    resetQueryParams,
    clearError,
    clearModelError,
    resetState,
    
    // 初始模型管理操作方法
    generateInitialModel,
    uploadCustomInitialModel,
    fetchTaskInitialModel,
    distributeInitialModel,
    fetchDistributionStatus,
    downloadInitialModel,
    deleteInitialModel,
    
    // 计算属性和工具方法
    getTaskModels,
    isTaskModelsLoading,
    getEvaluationResults,
    isModelEvaluating,
    getRollbackHistory,
    getTaskStatistics,
    isModelOperating,
    getModelOperationError,
    canEvaluateModel,
    canDeleteModel,
    getModelCountByStatus,
    isBatchOperating,
    
    // 初始模型管理计算属性和工具方法
    getTaskInitialModel,
    isTaskInitialModelLoading,
    getTaskInitialModelError,
    getDistributionStatus,
    isDistributing,
    getDistributionError,
    getTaskInfo,
    canDistributeInitialModel,
    canDeleteInitialModel,
    isGenerating,
    isInitialUploading,
    hasInitialModel,
    getInitialUploadProgress
  }
}

// ==================== 导出默认 Hook ====================
export default useModel
