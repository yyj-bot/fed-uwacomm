/**
 * 虚拟机管理 Hook - 封装虚拟机状态和操作
 * 为组件层提供简洁的虚拟机管理功能访问接口
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { useCallback } from 'react'
import { useVMStore } from './vmStore'
import type { VirtualMachine, VMStatus } from '@/api/vm'
import type { 
  VMListParams,
  VMUpdateRequest,
  VMStartRequest,
  VMStopRequest
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

  // ==================== 返回接口 ====================

  return {
    // 状态
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
    
    // 操作方法
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
    
    // 计算属性和工具方法
    getVMStatus,
    isVMOperating,
    getVMOperationError,
    canStartVM,
    canStopVM,
    canRestartVM,
    onlineVMCount,
    offlineVMCount,
    errorVMCount
  }
}

// ==================== 导出默认 Hook ====================
export default useVM
