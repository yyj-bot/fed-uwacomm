/**
 * 训练数据 Hook - 封装训练数据状态和操作
 * 为组件层提供简洁的训练数据管理功能访问接口
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { useCallback } from 'react'
import { useDataStore } from './trainingDataStore'
import type { DatasetDetail, DataStatistics, PreprocessTask, ValidationResult, ExportTask } from '@/api/training-data'
import type { 
  UploadTextRequest,
  DataListParams,
  PreprocessRequest,
  ValidationRequest,
  UpdateDataRequest,
  BatchOperationRequest,
  ExportDataRequest,
  DataStatisticsParams
} from '@/services'

// ==================== Hook 实现 ====================

export const useData = () => {
  // 获取状态
  const dataList = useDataStore((state) => state.dataList)
  const dataListTotal = useDataStore((state) => state.dataListTotal)
  const dataListLoading = useDataStore((state) => state.dataListLoading)
  const dataListError = useDataStore((state) => state.dataListError)
  
  const currentDataset = useDataStore((state) => state.currentDataset)
  const currentDatasetLoading = useDataStore((state) => state.currentDatasetLoading)
  const currentDatasetError = useDataStore((state) => state.currentDatasetError)
  
  const uploadLoading = useDataStore((state) => state.uploadLoading)
  const uploadError = useDataStore((state) => state.uploadError)
  const uploadProgress = useDataStore((state) => state.uploadProgress)
  
  const preprocessTasks = useDataStore((state) => state.preprocessTasks)
  const validationResults = useDataStore((state) => state.validationResults)
  const exportTasks = useDataStore((state) => state.exportTasks)
  
  const statistics = useDataStore((state) => state.statistics)
  const statisticsLoading = useDataStore((state) => state.statisticsLoading)
  const statisticsError = useDataStore((state) => state.statisticsError)
  
  const operationLoading = useDataStore((state) => state.operationLoading)
  const operationError = useDataStore((state) => state.operationError)
  
  const pagination = useDataStore((state) => state.pagination)
  const queryParams = useDataStore((state) => state.queryParams)

  // 获取操作方法
  const fetchDataListAction = useDataStore((state) => state.fetchDataList)
  const refreshDataListAction = useDataStore((state) => state.refreshDataList)
  const fetchDataDetailAction = useDataStore((state) => state.fetchDataDetail)
  const setCurrentDatasetAction = useDataStore((state) => state.setCurrentDataset)
  const uploadFileAction = useDataStore((state) => state.uploadFile)
  const uploadTextAction = useDataStore((state) => state.uploadText)
  const preprocessDataAction = useDataStore((state) => state.preprocessData)
  const validateDataAction = useDataStore((state) => state.validateData)
  const updateDataAction = useDataStore((state) => state.updateData)
  const deleteDataAction = useDataStore((state) => state.deleteData)
  const downloadDataAction = useDataStore((state) => state.downloadData)
  const batchOperationAction = useDataStore((state) => state.batchOperation)
  const exportDataAction = useDataStore((state) => state.exportData)
  const fetchStatisticsAction = useDataStore((state) => state.fetchStatistics)
  const setPaginationAction = useDataStore((state) => state.setPagination)
  const setQueryParamsAction = useDataStore((state) => state.setQueryParams)
  const resetQueryParamsAction = useDataStore((state) => state.resetQueryParams)
  const clearErrorAction = useDataStore((state) => state.clearError)
  const clearDataErrorAction = useDataStore((state) => state.clearDataError)
  const resetStateAction = useDataStore((state) => state.resetState)

  // ==================== 封装操作方法 ====================

  /**
   * 获取数据列表
   */
  const fetchDataList = useCallback(async (params?: DataListParams) => {
    try {
      await fetchDataListAction(params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取数据列表失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchDataListAction])

  /**
   * 刷新数据列表
   */
  const refreshDataList = useCallback(async () => {
    try {
      await refreshDataListAction()
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '刷新数据列表失败'
      return { success: false, error: errorMessage }
    }
  }, [refreshDataListAction])

  /**
   * 获取数据详情
   */
  const fetchDataDetail = useCallback(async (datasetId: string) => {
    try {
      await fetchDataDetailAction(datasetId)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取数据详情失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchDataDetailAction])

  /**
   * 设置当前数据集
   */
  const setCurrentDataset = useCallback((dataset: DatasetDetail | null) => {
    setCurrentDatasetAction(dataset)
  }, [setCurrentDatasetAction])

  /**
   * 上传文件
   */
  const uploadFile = useCallback(async (formData: FormData) => {
    try {
      await uploadFileAction(formData)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '文件上传失败'
      return { success: false, error: errorMessage }
    }
  }, [uploadFileAction])

  /**
   * 上传文本数据
   */
  const uploadText = useCallback(async (textData: UploadTextRequest) => {
    try {
      await uploadTextAction(textData)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '文本上传失败'
      return { success: false, error: errorMessage }
    }
  }, [uploadTextAction])

  /**
   * 数据预处理
   */
  const preprocessData = useCallback(async (datasetId: string, request: PreprocessRequest) => {
    try {
      await preprocessDataAction(datasetId, request)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '数据预处理失败'
      return { success: false, error: errorMessage }
    }
  }, [preprocessDataAction])

  /**
   * 数据验证
   */
  const validateData = useCallback(async (datasetId: string, request?: ValidationRequest) => {
    try {
      await validateDataAction(datasetId, request)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '数据验证失败'
      return { success: false, error: errorMessage }
    }
  }, [validateDataAction])

  /**
   * 更新数据信息
   */
  const updateData = useCallback(async (datasetId: string, data: UpdateDataRequest) => {
    try {
      await updateDataAction(datasetId, data)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '更新数据失败'
      return { success: false, error: errorMessage }
    }
  }, [updateDataAction])

  /**
   * 删除数据
   */
  const deleteData = useCallback(async (datasetId: string, force = false) => {
    try {
      await deleteDataAction(datasetId, force)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '删除数据失败'
      return { success: false, error: errorMessage }
    }
  }, [deleteDataAction])

  /**
   * 下载数据
   */
  const downloadData = useCallback(async (datasetId: string) => {
    try {
      await downloadDataAction(datasetId)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '下载数据失败'
      return { success: false, error: errorMessage }
    }
  }, [downloadDataAction])

  /**
   * 批量操作
   */
  const batchOperation = useCallback(async (request: BatchOperationRequest) => {
    try {
      await batchOperationAction(request)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '批量操作失败'
      return { success: false, error: errorMessage }
    }
  }, [batchOperationAction])

  /**
   * 导出数据
   */
  const exportData = useCallback(async (request: ExportDataRequest) => {
    try {
      await exportDataAction(request)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '数据导出失败'
      return { success: false, error: errorMessage }
    }
  }, [exportDataAction])

  /**
   * 获取数据统计
   */
  const fetchStatistics = useCallback(async (params?: DataStatisticsParams) => {
    try {
      await fetchStatisticsAction(params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取数据统计失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchStatisticsAction])

  /**
   * 设置分页参数
   */
  const setPagination = useCallback((page: number, size?: number) => {
    setPaginationAction(page, size)
  }, [setPaginationAction])

  /**
   * 设置查询参数
   */
  const setQueryParams = useCallback((params: DataListParams) => {
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
   * 清除特定数据的错误信息
   */
  const clearDataError = useCallback((datasetId: string) => {
    clearDataErrorAction(datasetId)
  }, [clearDataErrorAction])

  /**
   * 重置状态
   */
  const resetState = useCallback(() => {
    resetStateAction()
  }, [resetStateAction])

  // ==================== 计算属性 ====================

  /**
   * 获取预处理任务
   */
  const getPreprocessTask = useCallback((datasetId: string): PreprocessTask | null => {
    return preprocessTasks[datasetId] || null
  }, [preprocessTasks])

  /**
   * 获取验证结果
   */
  const getValidationResult = useCallback((datasetId: string): ValidationResult | null => {
    return validationResults[datasetId] || null
  }, [validationResults])

  /**
   * 获取导出任务
   */
  const getExportTask = useCallback((taskId: string): ExportTask | null => {
    return exportTasks[taskId] || null
  }, [exportTasks])

  /**
   * 检查数据是否正在执行操作
   */
  const isDataOperating = useCallback((datasetId: string, operation?: string): boolean => {
    if (operation) {
      return !!operationLoading[`${operation}-${datasetId}`]
    }
    
    // 检查是否有任何操作正在进行
    const operations = ['preprocess', 'validate', 'update', 'delete', 'download']
    return operations.some(op => !!operationLoading[`${op}-${datasetId}`])
  }, [operationLoading])

  /**
   * 获取数据操作错误
   */
  const getDataOperationError = useCallback((datasetId: string, operation: string): string | null => {
    return operationError[`${operation}-${datasetId}`] || null
  }, [operationError])

  /**
   * 检查数据是否可以预处理
   */
  const canPreprocessData = useCallback((dataset: any): boolean => {
    if (!dataset) return false
    return dataset.status === 'READY' && !isDataOperating(dataset.datasetId)
  }, [isDataOperating])

  /**
   * 检查数据是否可以验证
   */
  const canValidateData = useCallback((dataset: any): boolean => {
    if (!dataset) return false
    return ['READY', 'PROCESSING'].includes(dataset.status) && !isDataOperating(dataset.datasetId)
  }, [isDataOperating])

  /**
   * 检查数据是否可以删除
   */
  const canDeleteData = useCallback((dataset: any): boolean => {
    if (!dataset) return false
    return !isDataOperating(dataset.datasetId)
  }, [isDataOperating])

  /**
   * 获取不同状态的数据数量
   */
  const getDataCountByStatus = useCallback((status: string): number => {
    return dataList.filter(item => item.status === status).length
  }, [dataList])

  /**
   * 获取不同类型的数据数量
   */
  const getDataCountByType = useCallback((type: string): number => {
    return dataList.filter(item => item.datasetType === type).length
  }, [dataList])

  /**
   * 获取总数据大小
   */
  const getTotalDataSize = useCallback((): number => {
    return statistics?.totalSize || 0
  }, [statistics])

  /**
   * 检查是否有任何上传操作正在进行
   */
  const isUploading = uploadLoading

  /**
   * 检查是否有任何批量操作正在进行
   */
  const isBatchOperating = !!operationLoading['batch-operation']

  /**
   * 检查是否有任何导出操作正在进行
   */
  const isExporting = !!operationLoading['export-data']

  // ==================== 返回接口 ====================

  return {
    // 状态
    dataList,
    dataListTotal,
    dataListLoading,
    dataListError,
    currentDataset,
    currentDatasetLoading,
    currentDatasetError,
    uploadLoading,
    uploadError,
    uploadProgress,
    preprocessTasks,
    validationResults,
    exportTasks,
    statistics,
    statisticsLoading,
    statisticsError,
    operationLoading,
    operationError,
    pagination,
    queryParams,
    
    // 操作方法
    fetchDataList,
    refreshDataList,
    fetchDataDetail,
    setCurrentDataset,
    uploadFile,
    uploadText,
    preprocessData,
    validateData,
    updateData,
    deleteData,
    downloadData,
    batchOperation,
    exportData,
    fetchStatistics,
    setPagination,
    setQueryParams,
    resetQueryParams,
    clearError,
    clearDataError,
    resetState,
    
    // 计算属性和工具方法
    getPreprocessTask,
    getValidationResult,
    getExportTask,
    isDataOperating,
    getDataOperationError,
    canPreprocessData,
    canValidateData,
    canDeleteData,
    getDataCountByStatus,
    getDataCountByType,
    getTotalDataSize,
    isUploading,
    isBatchOperating,
    isExporting
  }
}

// ==================== 导出默认 Hook ====================
export default useData
