/**
 * 虚拟机管理 Hook - 封装虚拟机状态和操作
 * 为组件层提供简洁的虚拟机管理功能访问接口
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { useCallback } from 'react'
import { useVMStore } from './vmStore'
import type { 
  VirtualMachine, 
  VMStatus,
  VMRoundModel,
  VMModelTrend,
  VMModelBest
} from '@/api/vm'
import type { 
  VMListParams,
  VMUpdateRequest,
  VMStartRequest,
  VMStopRequest,
  VMRestartRequest,
  VMRoundModelListParams,
  VMModelTrendParams,
  VMModelBestParams
} from '@/services'

// ==================== Hook 实现 ====================

export const useVM = () => {
  // 获取状态
  const vmList = useVMStore((state) => state.vmList)
  const vmListTotal = useVMStore((state) => state.vmListTotal)
  const vmListLoading = useVMStore((state) => state.vmListLoading)
  const vmListError = useVMStore((state) => state.vmListError)
  
  const currentVM = useVMStore((state) => state.currentVM)
  const currentVMLoading = useVMStore((state) => state.currentVMLoading)
  const currentVMError = useVMStore((state) => state.currentVMError)
  
  const vmStatusMap = useVMStore((state) => state.vmStatusMap)
  const operationLoading = useVMStore((state) => state.operationLoading)
  const operationError = useVMStore((state) => state.operationError)
  
  const pagination = useVMStore((state) => state.pagination)
  const queryParams = useVMStore((state) => state.queryParams)
  
  // VM本地模型状态
  const vmRoundModels = useVMStore((state) => state.vmRoundModels)
  const vmRoundModelsTotal = useVMStore((state) => state.vmRoundModelsTotal)
  const vmRoundModelsLoading = useVMStore((state) => state.vmRoundModelsLoading)
  const vmRoundModelsError = useVMStore((state) => state.vmRoundModelsError)
  
  const currentVMRoundModel = useVMStore((state) => state.currentVMRoundModel)
  const currentVMRoundModelLoading = useVMStore((state) => state.currentVMRoundModelLoading)
  const currentVMRoundModelError = useVMStore((state) => state.currentVMRoundModelError)
  
  const vmModelTrends = useVMStore((state) => state.vmModelTrends)
  const vmModelTrendsLoading = useVMStore((state) => state.vmModelTrendsLoading)
  const vmModelTrendsError = useVMStore((state) => state.vmModelTrendsError)
  
  const vmModelBests = useVMStore((state) => state.vmModelBests)
  const vmModelBestsLoading = useVMStore((state) => state.vmModelBestsLoading)
  const vmModelBestsError = useVMStore((state) => state.vmModelBestsError)
  
  const vmRoundModelsPagination = useVMStore((state) => state.vmRoundModelsPagination)
  const vmRoundModelsQueryParams = useVMStore((state) => state.vmRoundModelsQueryParams)

  // 获取操作方法
  const fetchVMListAction = useVMStore((state) => state.fetchVMList)
  const refreshVMListAction = useVMStore((state) => state.refreshVMList)
  const fetchVMDetailAction = useVMStore((state) => state.fetchVMDetail)
  const setCurrentVMAction = useVMStore((state) => state.setCurrentVM)
  const fetchVMStatusAction = useVMStore((state) => state.fetchVMStatus)
  const fetchAllVMStatusAction = useVMStore((state) => state.fetchAllVMStatus)
  const updateVMAction = useVMStore((state) => state.updateVM)
  const deleteVMAction = useVMStore((state) => state.deleteVM)
  const startVMAction = useVMStore((state) => state.startVM)
  const stopVMAction = useVMStore((state) => state.stopVM)
  const restartVMAction = useVMStore((state) => state.restartVM)
  const setPaginationAction = useVMStore((state) => state.setPagination)
  const setQueryParamsAction = useVMStore((state) => state.setQueryParams)
  const resetQueryParamsAction = useVMStore((state) => state.resetQueryParams)
  const clearErrorAction = useVMStore((state) => state.clearError)
  const clearVMErrorAction = useVMStore((state) => state.clearVMError)
  const resetStateAction = useVMStore((state) => state.resetState)
  
  // VM本地模型操作方法
  const fetchVMRoundModelsAction = useVMStore((state) => state.fetchVMRoundModels)
  const refreshVMRoundModelsAction = useVMStore((state) => state.refreshVMRoundModels)
  const fetchVMRoundModelDetailAction = useVMStore((state) => state.fetchVMRoundModelDetail)
  const setCurrentVMRoundModelAction = useVMStore((state) => state.setCurrentVMRoundModel)
  const fetchVMModelTrendAction = useVMStore((state) => state.fetchVMModelTrend)
  const fetchVMModelBestAction = useVMStore((state) => state.fetchVMModelBest)
  const setVMRoundModelsPaginationAction = useVMStore((state) => state.setVMRoundModelsPagination)
  const setVMRoundModelsQueryParamsAction = useVMStore((state) => state.setVMRoundModelsQueryParams)
  const resetVMRoundModelsQueryParamsAction = useVMStore((state) => state.resetVMRoundModelsQueryParams)
  const clearVMRoundModelsErrorAction = useVMStore((state) => state.clearVMRoundModelsError)

  // ==================== 封装操作方法 ====================

  /**
   * 获取虚拟机列表
   */
  const fetchVMList = useCallback(async (params?: VMListParams) => {
    try {
      await fetchVMListAction(params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取虚拟机列表失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchVMListAction])

  /**
   * 刷新虚拟机列表
   */
  const refreshVMList = useCallback(async () => {
    try {
      await refreshVMListAction()
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '刷新虚拟机列表失败'
      return { success: false, error: errorMessage }
    }
  }, [refreshVMListAction])

  /**
   * 获取虚拟机详情
   */
  const fetchVMDetail = useCallback(async (vmId: string) => {
    try {
      await fetchVMDetailAction(vmId)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取虚拟机详情失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchVMDetailAction])

  /**
   * 设置当前虚拟机
   */
  const setCurrentVM = useCallback((vm: VirtualMachine | null) => {
    setCurrentVMAction(vm)
  }, [setCurrentVMAction])

  /**
   * 获取虚拟机状态
   */
  const fetchVMStatus = useCallback(async (vmId: string) => {
    try {
      await fetchVMStatusAction(vmId)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取虚拟机状态失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchVMStatusAction])

  /**
   * 获取所有虚拟机状态
   */
  const fetchAllVMStatus = useCallback(async () => {
    try {
      await fetchAllVMStatusAction()
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取虚拟机状态失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchAllVMStatusAction])

  /**
   * 更新虚拟机信息
   */
  const updateVM = useCallback(async (vmId: string, data: VMUpdateRequest) => {
    try {
      await updateVMAction(vmId, data)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '更新虚拟机失败'
      return { success: false, error: errorMessage }
    }
  }, [updateVMAction])

  /**
   * 删除虚拟机
   */
  const deleteVM = useCallback(async (vmId: string, force = false) => {
    try {
      await deleteVMAction(vmId, force)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '删除虚拟机失败'
      return { success: false, error: errorMessage }
    }
  }, [deleteVMAction])

  /**
   * 启动虚拟机
   */
  const startVM = useCallback(async (vmId: string, data?: VMStartRequest) => {
    try {
      await startVMAction(vmId, data)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '启动虚拟机失败'
      return { success: false, error: errorMessage }
    }
  }, [startVMAction])

  /**
   * 停止虚拟机
   */
  const stopVM = useCallback(async (vmId: string, data?: VMStopRequest) => {
    try {
      await stopVMAction(vmId, data)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '停止虚拟机失败'
      return { success: false, error: errorMessage }
    }
  }, [stopVMAction])

  /**
   * 重启虚拟机
   */
  const restartVM = useCallback(async (vmId: string) => {
    try {
      await restartVMAction(vmId)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '重启虚拟机失败'
      return { success: false, error: errorMessage }
    }
  }, [restartVMAction])

  /**
   * 设置分页参数
   */
  const setPagination = useCallback((page: number, size?: number) => {
    setPaginationAction(page, size)
  }, [setPaginationAction])

  /**
   * 设置查询参数
   */
  const setQueryParams = useCallback((params: VMListParams) => {
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
   * 清除特定虚拟机的错误信息
   */
  const clearVMError = useCallback((vmId: string) => {
    clearVMErrorAction(vmId)
  }, [clearVMErrorAction])

  /**
   * 重置状态
   */
  const resetState = useCallback(() => {
    resetStateAction()
  }, [resetStateAction])

  // ==================== 计算属性 ====================

  /**
   * 获取虚拟机状态
   */
  const getVMStatus = useCallback((vmId: string): VMStatus | null => {
    return vmStatusMap[vmId] || null
  }, [vmStatusMap])

  /**
   * 检查虚拟机是否正在执行操作
   */
  const isVMOperating = useCallback((vmId: string, operation?: string): boolean => {
    if (operation) {
      return !!operationLoading[`${operation}-${vmId}`]
    }
    
    // 检查是否有任何操作正在进行
    const operations = ['start', 'stop', 'restart', 'update', 'delete']
    return operations.some(op => !!operationLoading[`${op}-${vmId}`])
  }, [operationLoading])

  /**
   * 获取虚拟机操作错误
   */
  const getVMOperationError = useCallback((vmId: string, operation: string): string | null => {
    return operationError[`${operation}-${vmId}`] || null
  }, [operationError])

  /**
   * 检查虚拟机是否可以启动
   */
  const canStartVM = useCallback((vm: VirtualMachine): boolean => {
    if (!vm) return false
    const status = getVMStatus(vm.vmId)
    return status?.status === 'STOPPED' && !isVMOperating(vm.vmId)
  }, [getVMStatus, isVMOperating])

  /**
   * 检查虚拟机是否可以停止
   */
  const canStopVM = useCallback((vm: VirtualMachine): boolean => {
    if (!vm) return false
    const status = getVMStatus(vm.vmId)
    return status?.status === 'RUNNING' && !isVMOperating(vm.vmId)
  }, [getVMStatus, isVMOperating])

  /**
   * 检查虚拟机是否可以重启
   */
  const canRestartVM = useCallback((vm: VirtualMachine): boolean => {
    if (!vm) return false
    const status = getVMStatus(vm.vmId)
    return status?.status === 'RUNNING' && !isVMOperating(vm.vmId)
  }, [getVMStatus, isVMOperating])

  /**
   * 获取在线虚拟机数量
   */
  const onlineVMCount = useCallback((): number => {
    return vmList.filter(vm => {
      const status = getVMStatus(vm.vmId)
      return status?.status === 'RUNNING'
    }).length
  }, [vmList, getVMStatus])

  /**
   * 获取离线虚拟机数量
   */
  const offlineVMCount = useCallback((): number => {
    return vmList.filter(vm => {
      const status = getVMStatus(vm.vmId)
      return status?.status === 'STOPPED' || status?.status === 'OFFLINE'
    }).length
  }, [vmList, getVMStatus])

  /**
   * 获取异常虚拟机数量
   */
  const errorVMCount = useCallback((): number => {
    return vmList.filter(vm => {
      const status = getVMStatus(vm.vmId)
      return status?.status === 'ERROR'
    }).length
  }, [vmList, getVMStatus])

  // ==================== VM本地模型操作方法 ====================

  /**
   * 获取VM本地模型列表
   */
  const fetchVMRoundModels = useCallback(async (params?: VMRoundModelListParams) => {
    try {
      await fetchVMRoundModelsAction(params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取VM本地模型列表失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchVMRoundModelsAction])

  /**
   * 刷新VM本地模型列表
   */
  const refreshVMRoundModels = useCallback(async () => {
    try {
      await refreshVMRoundModelsAction()
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '刷新VM本地模型列表失败'
      return { success: false, error: errorMessage }
    }
  }, [refreshVMRoundModelsAction])

  /**
   * 获取VM本地模型详情
   */
  const fetchVMRoundModelDetail = useCallback(async (vmRoundModelId: string) => {
    try {
      await fetchVMRoundModelDetailAction(vmRoundModelId)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取VM本地模型详情失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchVMRoundModelDetailAction])

  /**
   * 设置当前VM本地模型
   */
  const setCurrentVMRoundModel = useCallback((model: VMRoundModel | null) => {
    setCurrentVMRoundModelAction(model)
  }, [setCurrentVMRoundModelAction])

  /**
   * 获取VM模型训练指标趋势
   */
  const fetchVMModelTrend = useCallback(async (params: VMModelTrendParams) => {
    try {
      await fetchVMModelTrendAction(params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取VM模型训练指标趋势失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchVMModelTrendAction])

  /**
   * 获取VM模型最佳/离群结果
   */
  const fetchVMModelBest = useCallback(async (params: VMModelBestParams) => {
    try {
      await fetchVMModelBestAction(params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取VM模型最佳/离群结果失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchVMModelBestAction])

  /**
   * 设置VM本地模型分页参数
   */
  const setVMRoundModelsPagination = useCallback((current: number, size?: number) => {
    setVMRoundModelsPaginationAction(current, size)
  }, [setVMRoundModelsPaginationAction])

  /**
   * 设置VM本地模型查询参数
   */
  const setVMRoundModelsQueryParams = useCallback((params: VMRoundModelListParams) => {
    setVMRoundModelsQueryParamsAction(params)
  }, [setVMRoundModelsQueryParamsAction])

  /**
   * 重置VM本地模型查询参数
   */
  const resetVMRoundModelsQueryParams = useCallback(() => {
    resetVMRoundModelsQueryParamsAction()
  }, [resetVMRoundModelsQueryParamsAction])

  /**
   * 清除VM本地模型错误信息
   */
  const clearVMRoundModelsError = useCallback(() => {
    clearVMRoundModelsErrorAction()
  }, [clearVMRoundModelsErrorAction])

  // ==================== VM本地模型计算属性 ====================

  /**
   * 获取VM模型趋势数据
   */
  const getVMModelTrend = useCallback((taskId: string, vmId: string, metric: string): VMModelTrend | null => {
    const trendKey = `${taskId}-${vmId}-${metric}`
    return vmModelTrends[trendKey] || null
  }, [vmModelTrends])

  /**
   * 获取VM模型最佳/离群数据
   */
  const getVMModelBest = useCallback((taskId: string, metric: string, type: 'best' | 'outlier'): VMModelBest | null => {
    const bestKey = `${taskId}-${metric}-${type}`
    return vmModelBests[bestKey] || null
  }, [vmModelBests])

  /**
   * 检查VM模型趋势是否正在加载
   */
  const isVMModelTrendLoading = useCallback((taskId: string, vmId: string, metric: string): boolean => {
    const trendKey = `${taskId}-${vmId}-${metric}`
    return !!vmModelTrendsLoading[trendKey]
  }, [vmModelTrendsLoading])

  /**
   * 检查VM模型最佳/离群是否正在加载
   */
  const isVMModelBestLoading = useCallback((taskId: string, metric: string, type: 'best' | 'outlier'): boolean => {
    const bestKey = `${taskId}-${metric}-${type}`
    return !!vmModelBestsLoading[bestKey]
  }, [vmModelBestsLoading])

  /**
   * 获取VM模型趋势错误
   */
  const getVMModelTrendError = useCallback((taskId: string, vmId: string, metric: string): string | null => {
    const trendKey = `${taskId}-${vmId}-${metric}`
    return vmModelTrendsError[trendKey] || null
  }, [vmModelTrendsError])

  /**
   * 获取VM模型最佳/离群错误
   */
  const getVMModelBestError = useCallback((taskId: string, metric: string, type: 'best' | 'outlier'): string | null => {
    const bestKey = `${taskId}-${metric}-${type}`
    return vmModelBestsError[bestKey] || null
  }, [vmModelBestsError])

  // ==================== 返回接口 ====================

  return {
    // 虚拟机状态
    vmList,
    vmListTotal,
    vmListLoading,
    vmListError,
    currentVM,
    currentVMLoading,
    currentVMError,
    vmStatusMap,
    operationLoading,
    operationError,
    pagination,
    queryParams,
    
    // VM本地模型状态
    vmRoundModels,
    vmRoundModelsTotal,
    vmRoundModelsLoading,
    vmRoundModelsError,
    currentVMRoundModel,
    currentVMRoundModelLoading,
    currentVMRoundModelError,
    vmModelTrends,
    vmModelTrendsLoading,
    vmModelTrendsError,
    vmModelBests,
    vmModelBestsLoading,
    vmModelBestsError,
    vmRoundModelsPagination,
    vmRoundModelsQueryParams,
    
    // 虚拟机操作方法
    fetchVMList,
    refreshVMList,
    fetchVMDetail,
    setCurrentVM,
    fetchVMStatus,
    fetchAllVMStatus,
    updateVM,
    deleteVM,
    startVM,
    stopVM,
    restartVM,
    setPagination,
    setQueryParams,
    resetQueryParams,
    clearError,
    clearVMError,
    resetState,
    
    // VM本地模型操作方法
    fetchVMRoundModels,
    refreshVMRoundModels,
    fetchVMRoundModelDetail,
    setCurrentVMRoundModel,
    fetchVMModelTrend,
    fetchVMModelBest,
    setVMRoundModelsPagination,
    setVMRoundModelsQueryParams,
    resetVMRoundModelsQueryParams,
    clearVMRoundModelsError,
    
    // 虚拟机计算属性和工具方法
    getVMStatus,
    isVMOperating,
    getVMOperationError,
    canStartVM,
    canStopVM,
    canRestartVM,
    onlineVMCount,
    offlineVMCount,
    errorVMCount,
    
    // VM本地模型计算属性和工具方法
    getVMModelTrend,
    getVMModelBest,
    isVMModelTrendLoading,
    isVMModelBestLoading,
    getVMModelTrendError,
    getVMModelBestError
  }
}

// ==================== 导出默认 Hook ====================
export default useVM
