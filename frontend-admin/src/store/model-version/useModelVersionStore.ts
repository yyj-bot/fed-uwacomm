/**
 * 模型版本管理 Hook - 封装模型状态和操作
 * 为组件层提供简洁的模型版本管理功能访问接口
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { useCallback } from 'react'
import { useModelStore } from './modelVersionStore'
import type { 
  ModelVersionDetail,
  TaskModelVersions,
  EvaluationResult,
  DeploymentStatus,
  RollbackInfo,
  ModelStatistics,
  TaskStatistics,
  ModelVersionListParams,
  EvaluationRequest,
  DeploymentRequest,
  RollbackRequest,
  DownloadRequest,
  DeleteModelRequest,
  StatisticsParams
} from '@/services'

// ==================== Hook 实现 ====================

export const useModel = () => {
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
  
  const uploadLoading = useModelStore((state) => state.uploadLoading)
  const uploadError = useModelStore((state) => state.uploadError)
  const uploadProgress = useModelStore((state) => state.uploadProgress)
  
  const evaluationResults = useModelStore((state) => state.evaluationResults)
  const evaluationLoading = useModelStore((state) => state.evaluationLoading)
  
  const deployments = useModelStore((state) => state.deployments)
  const deploymentLoading = useModelStore((state) => state.deploymentLoading)
  
  const rollbackHistory = useModelStore((state) => state.rollbackHistory)
  
  const modelStatistics = useModelStore((state) => state.modelStatistics)
  const taskStatistics = useModelStore((state) => state.taskStatistics)
  const statisticsLoading = useModelStore((state) => state.statisticsLoading)
  
  const operationLoading = useModelStore((state) => state.operationLoading)
  const operationError = useModelStore((state) => state.operationError)
  
  const pagination = useModelStore((state) => state.pagination)
  const queryParams = useModelStore((state) => state.queryParams)

  // 获取操作方法
  const fetchModelListAction = useModelStore((state) => state.fetchModelList)
  const refreshModelListAction = useModelStore((state) => state.refreshModelList)
  const fetchModelDetailAction = useModelStore((state) => state.fetchModelDetail)
  const setCurrentModelAction = useModelStore((state) => state.setCurrentModel)
  const uploadModelAction = useModelStore((state) => state.uploadModel)
  const uploadModelBatchAction = useModelStore((state) => state.uploadModelBatch)
  const fetchTaskModelsAction = useModelStore((state) => state.fetchTaskModels)
  const evaluateModelAction = useModelStore((state) => state.evaluateModel)
  const fetchEvaluationResultsAction = useModelStore((state) => state.fetchEvaluationResults)
  const deployModelAction = useModelStore((state) => state.deployModel)
  const fetchDeploymentStatusAction = useModelStore((state) => state.fetchDeploymentStatus)
  const fetchDeploymentListAction = useModelStore((state) => state.fetchDeploymentList)
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
   * 上传模型
   */
  const uploadModel = useCallback(async (formData: FormData) => {
    try {
      const modelId = await uploadModelAction(formData)
      return { success: true, error: null, data: modelId }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '模型上传失败'
      return { success: false, error: errorMessage, data: null }
    }
  }, [uploadModelAction])

  /**
   * 批量上传模型
   */
  const uploadModelBatch = useCallback(async (models: { file: File; roundNumber: number; description?: string }[], taskId: string) => {
    try {
      await uploadModelBatchAction(models, taskId)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '批量上传失败'
      return { success: false, error: errorMessage }
    }
  }, [uploadModelBatchAction])

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
  const fetchEvaluationResults = useCallback(async (modelId: string) => {
    try {
      await fetchEvaluationResultsAction(modelId)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取评估结果失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchEvaluationResultsAction])

  /**
   * 部署模型
   */
  const deployModel = useCallback(async (modelId: string, request: DeploymentRequest) => {
    try {
      const deploymentId = await deployModelAction(modelId, request)
      return { success: true, error: null, data: deploymentId }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '模型部署失败'
      return { success: false, error: errorMessage, data: null }
    }
  }, [deployModelAction])

  /**
   * 获取部署状态
   */
  const fetchDeploymentStatus = useCallback(async (deploymentId: string) => {
    try {
      await fetchDeploymentStatusAction(deploymentId)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取部署状态失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchDeploymentStatusAction])

  /**
   * 获取部署列表
   */
  const fetchDeploymentList = useCallback(async (params?: any) => {
    try {
      await fetchDeploymentListAction(params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取部署列表失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchDeploymentListAction])

  /**
   * 回滚模型
   */
  const rollbackModel = useCallback(async (deploymentId: string, request: RollbackRequest) => {
    try {
      await rollbackModelAction(deploymentId, request)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '模型回滚失败'
      return { success: false, error: errorMessage }
    }
  }, [rollbackModelAction])

  /**
   * 获取回滚历史
   */
  const fetchRollbackHistory = useCallback(async (deploymentId: string) => {
    try {
      await fetchRollbackHistoryAction(deploymentId)
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
   * 获取部署状态
   */
  const getDeploymentStatus = useCallback((deploymentId: string): DeploymentStatus | null => {
    return deployments[deploymentId] || null
  }, [deployments])

  /**
   * 检查是否正在部署
   */
  const isDeploying = useCallback((modelId: string): boolean => {
    return !!deploymentLoading[modelId]
  }, [deploymentLoading])

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
   * 检查模型是否可以部署
   */
  const canDeployModel = useCallback((model: any): boolean => {
    if (!model) return false
    return model.status === 'VALIDATED' && !isModelOperating(model.modelId)
  }, [isModelOperating])

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
   * 检查是否有任何上传操作正在进行
   */
  const isUploading = uploadLoading

  /**
   * 检查是否有任何批量操作正在进行
   */
  const isBatchOperating = !!(operationLoading['batch-download'] || operationLoading['batch-delete'])

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
    uploadLoading,
    uploadError,
    uploadProgress,
    evaluationResults,
    evaluationLoading,
    deployments,
    deploymentLoading,
    rollbackHistory,
    modelStatistics,
    taskStatistics,
    statisticsLoading,
    operationLoading,
    operationError,
    pagination,
    queryParams,
    
    // 操作方法
    fetchModelList,
    refreshModelList,
    fetchModelDetail,
    setCurrentModel,
    uploadModel,
    uploadModelBatch,
    fetchTaskModels,
    evaluateModel,
    fetchEvaluationResults,
    deployModel,
    fetchDeploymentStatus,
    fetchDeploymentList,
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
    
    // 计算属性和工具方法
    getTaskModels,
    isTaskModelsLoading,
    getEvaluationResults,
    isModelEvaluating,
    getDeploymentStatus,
    isDeploying,
    getRollbackHistory,
    getTaskStatistics,
    isModelOperating,
    getModelOperationError,
    canDeployModel,
    canEvaluateModel,
    canDeleteModel,
    getModelCountByStatus,
    isUploading,
    isBatchOperating
  }
}

// ==================== 导出默认 Hook ====================
export default useModel
