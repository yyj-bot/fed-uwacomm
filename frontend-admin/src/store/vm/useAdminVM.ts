/**
 * 管理员水下机器人管理 Hook - 封装管理员VM状态和操作
 * 为组件层提供简洁的管理员VM功能访问接口
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { useCallback } from 'react'
import { useAdminVmStore } from './adminVmStore'
import type {
  VirtualMachine,
  UserVm,
  VmAssignmentDetail,
  VmAssignmentOverview,
  VmPermission,
  VmStatus,
  VmControlAction,
  AdminVmListParams,
  UnassignedVmListParams
} from '@/services/admin/type'

// ==================== Hook 实现 ====================

export const useAdminVM = () => {
  // 获取状态
  const adminVmList = useAdminVmStore((state) => state.adminVmList)
  const adminVmListTotal = useAdminVmStore((state) => state.adminVmListTotal)
  const adminVmListLoading = useAdminVmStore((state) => state.adminVmListLoading)
  const adminVmListError = useAdminVmStore((state) => state.adminVmListError)
  
  const vmAssignments = useAdminVmStore((state) => state.vmAssignments)
  const vmAssignmentsLoading = useAdminVmStore((state) => state.vmAssignmentsLoading)
  const vmAssignmentsError = useAdminVmStore((state) => state.vmAssignmentsError)
  
  const userVmMap = useAdminVmStore((state) => state.userVmMap)
  const userVmMapLoading = useAdminVmStore((state) => state.userVmMapLoading)
  const userVmMapError = useAdminVmStore((state) => state.userVmMapError)
  
  const unassignedVms = useAdminVmStore((state) => state.unassignedVms)
  const unassignedVmsTotal = useAdminVmStore((state) => state.unassignedVmsTotal)
  const unassignedVmsLoading = useAdminVmStore((state) => state.unassignedVmsLoading)
  const unassignedVmsError = useAdminVmStore((state) => state.unassignedVmsError)
  
  const assignmentOverview = useAdminVmStore((state) => state.assignmentOverview)
  const assignmentOverviewLoading = useAdminVmStore((state) => state.assignmentOverviewLoading)
  const assignmentOverviewError = useAdminVmStore((state) => state.assignmentOverviewError)
  
  const operationLoading = useAdminVmStore((state) => state.operationLoading)
  const operationError = useAdminVmStore((state) => state.operationError)
  
  const adminVmPagination = useAdminVmStore((state) => state.adminVmPagination)
  const adminVmQueryParams = useAdminVmStore((state) => state.adminVmQueryParams)

  // 获取操作方法
  const fetchAdminVmListAction = useAdminVmStore((state) => state.fetchAdminVmList)
  const refreshAdminVmListAction = useAdminVmStore((state) => state.refreshAdminVmList)
  const assignVmToUserAction = useAdminVmStore((state) => state.assignVmToUser)
  const unassignVmFromUserAction = useAdminVmStore((state) => state.unassignVmFromUser)
  const fetchVmAssignmentsAction = useAdminVmStore((state) => state.fetchVmAssignments)
  const updateUserVmPermissionsAction = useAdminVmStore((state) => state.updateUserVmPermissions)
  const fetchUserVmListAction = useAdminVmStore((state) => state.fetchUserVmList)
  const batchAssignVmsToUserAction = useAdminVmStore((state) => state.batchAssignVmsToUser)
  const batchRemoveUserVmsAction = useAdminVmStore((state) => state.batchRemoveUserVms)
  const fetchUnassignedVmListAction = useAdminVmStore((state) => state.fetchUnassignedVmList)
  const fetchVmAssignmentOverviewAction = useAdminVmStore((state) => state.fetchVmAssignmentOverview)
  const forceControlVmAction = useAdminVmStore((state) => state.forceControlVm)
  const setAdminVmPaginationAction = useAdminVmStore((state) => state.setAdminVmPagination)
  const setAdminVmQueryParamsAction = useAdminVmStore((state) => state.setAdminVmQueryParams)
  const resetAdminVmQueryParamsAction = useAdminVmStore((state) => state.resetAdminVmQueryParams)
  const clearErrorAction = useAdminVmStore((state) => state.clearError)
  const clearVmErrorAction = useAdminVmStore((state) => state.clearVmError)
  const clearUserErrorAction = useAdminVmStore((state) => state.clearUserError)
  const resetStateAction = useAdminVmStore((state) => state.resetState)

  // ==================== 封装操作方法 ====================

  /**
   * 获取管理员VM列表
   */
  const fetchAdminVmList = useCallback(async (params?: AdminVmListParams) => {
    try {
      await fetchAdminVmListAction(params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取管理员水下机器人列表失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchAdminVmListAction])

  /**
   * 刷新管理员VM列表
   */
  const refreshAdminVmList = useCallback(async () => {
    try {
      await refreshAdminVmListAction()
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '刷新水下机器人列表失败'
      return { success: false, error: errorMessage }
    }
  }, [refreshAdminVmListAction])

  /**
   * 分配VM给用户
   */
  const assignVmToUser = useCallback(async (
    vmId: string,
    userId: string,
    params?: { permissions?: VmPermission[]; notes?: string }
  ) => {
    try {
      await assignVmToUserAction(vmId, userId, params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '分配水下机器人失败'
      return { success: false, error: errorMessage }
    }
  }, [assignVmToUserAction])

  /**
   * 取消VM分配
   */
  const unassignVmFromUser = useCallback(async (vmId: string, userId: string) => {
    try {
      await unassignVmFromUserAction(vmId, userId)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '取消分配失败'
      return { success: false, error: errorMessage }
    }
  }, [unassignVmFromUserAction])

  /**
   * 获取VM分配情况
   */
  const fetchVmAssignments = useCallback(async (vmId: string) => {
    try {
      await fetchVmAssignmentsAction(vmId)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取VM分配情况失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchVmAssignmentsAction])

  /**
   * 更新用户VM权限
   */
  const updateUserVmPermissions = useCallback(async (
    vmId: string,
    userId: string,
    permissions: VmPermission[]
  ) => {
    try {
      await updateUserVmPermissionsAction(vmId, userId, permissions)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '更新权限失败'
      return { success: false, error: errorMessage }
    }
  }, [updateUserVmPermissionsAction])

  /**
   * 获取用户VM列表
   */
  const fetchUserVmList = useCallback(async (
    userId: string,
    params?: { status?: VmStatus; page?: number; size?: number }
  ) => {
    try {
      await fetchUserVmListAction(userId, params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取用户VM列表失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchUserVmListAction])

  /**
   * 批量分配VM给用户
   */
  const batchAssignVmsToUser = useCallback(async (
    userId: string,
    vmIds: string[],
    params?: { permissions?: VmPermission[]; notes?: string }
  ) => {
    try {
      await batchAssignVmsToUserAction(userId, vmIds, params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '批量分配失败'
      return { success: false, error: errorMessage }
    }
  }, [batchAssignVmsToUserAction])

  /**
   * 批量移除用户VM
   */
  const batchRemoveUserVms = useCallback(async (userId: string, vmIds: string[]) => {
    try {
      await batchRemoveUserVmsAction(userId, vmIds)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '批量移除失败'
      return { success: false, error: errorMessage }
    }
  }, [batchRemoveUserVmsAction])

  /**
   * 获取未分配VM列表
   */
  const fetchUnassignedVmList = useCallback(async (params?: UnassignedVmListParams) => {
    try {
      await fetchUnassignedVmListAction(params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取未分配VM列表失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchUnassignedVmListAction])

  /**
   * 获取VM分配概况
   */
  const fetchVmAssignmentOverview = useCallback(async () => {
    try {
      await fetchVmAssignmentOverviewAction()
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取VM分配概况失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchVmAssignmentOverviewAction])

  /**
   * 强制控制VM
   */
  const forceControlVm = useCallback(async (
    vmId: string,
    action: VmControlAction,
    reason: string,
    timeout?: number
  ) => {
    try {
      await forceControlVmAction(vmId, action, reason, timeout)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '强制控制VM失败'
      return { success: false, error: errorMessage }
    }
  }, [forceControlVmAction])

  /**
   * 设置分页参数
   */
  const setAdminVmPagination = useCallback((page: number, size?: number) => {
    setAdminVmPaginationAction(page, size)
  }, [setAdminVmPaginationAction])

  /**
   * 设置查询参数
   */
  const setAdminVmQueryParams = useCallback((params: AdminVmListParams) => {
    setAdminVmQueryParamsAction(params)
  }, [setAdminVmQueryParamsAction])

  /**
   * 重置查询参数
   */
  const resetAdminVmQueryParams = useCallback(() => {
    resetAdminVmQueryParamsAction()
  }, [resetAdminVmQueryParamsAction])

  /**
   * 清除错误信息
   */
  const clearError = useCallback(() => {
    clearErrorAction()
  }, [clearErrorAction])

  /**
   * 清除特定VM的错误信息
   */
  const clearVmError = useCallback((vmId: string) => {
    clearVmErrorAction(vmId)
  }, [clearVmErrorAction])

  /**
   * 清除特定用户的错误信息
   */
  const clearUserError = useCallback((userId: string) => {
    clearUserErrorAction(userId)
  }, [clearUserErrorAction])

  /**
   * 重置状态
   */
  const resetState = useCallback(() => {
    resetStateAction()
  }, [resetStateAction])

  // ==================== 计算属性 ====================

  /**
   * 检查VM是否正在执行操作
   */
  const isVmOperating = useCallback((vmId: string, operation?: string): boolean => {
    if (operation) {
      return !!operationLoading[operation]
    }
    
    // 检查是否有任何与该 vmId 相关的操作正在进行
    return Object.keys(operationLoading).some(key => 
      key.includes(vmId) && operationLoading[key]
    )
  }, [operationLoading])

  /**
   * 获取VM操作错误
   */
  const getVmOperationError = useCallback((vmId: string, operation: string): string | null => {
    return operationError[`${operation}-${vmId}`] || null
  }, [operationError])

  /**
   * 获取特定VM的分配信息
   */
  const getVmAssignment = useCallback((vmId: string) => {
    return vmAssignments.get(vmId) || null
  }, [vmAssignments])

  /**
   * 检查VM分配是否正在加载
   */
  const isVmAssignmentLoading = useCallback((vmId: string): boolean => {
    return vmAssignmentsLoading.get(vmId) || false
  }, [vmAssignmentsLoading])

  /**
   * 获取VM分配错误
   */
  const getVmAssignmentError = useCallback((vmId: string): string | null => {
    return vmAssignmentsError.get(vmId) || null
  }, [vmAssignmentsError])

  /**
   * 获取用户的VM列表
   */
  const getUserVms = useCallback((userId: string) => {
    return userVmMap.get(userId) || null
  }, [userVmMap])

  /**
   * 检查用户VM列表是否正在加载
   */
  const isUserVmListLoading = useCallback((userId: string): boolean => {
    return userVmMapLoading.get(userId) || false
  }, [userVmMapLoading])

  /**
   * 获取用户VM列表错误
   */
  const getUserVmListError = useCallback((userId: string): string | null => {
    return userVmMapError.get(userId) || null
  }, [userVmMapError])

  /**
   * 检查VM是否已分配
   */
  const isVmAssigned = useCallback((vmId: string): boolean => {
    const vm = adminVmList.find(v => v.vmId === vmId)
    return vm?.isAssigned || false
  }, [adminVmList])

  /**
   * 获取VM的分配用户数量
   */
  const getVmAssignedUserCount = useCallback((vmId: string): number => {
    const vm = adminVmList.find(v => v.vmId === vmId)
    return vm?.assignedUserCount || 0
  }, [adminVmList])

  /**
   * 按状态筛选VM
   */
  const getVmsByStatus = useCallback((status: VmStatus): VirtualMachine[] => {
    return adminVmList.filter(vm => vm.status === status)
  }, [adminVmList])

  /**
   * 获取已分配的VM数量
   */
  const getAssignedVmCount = useCallback((): number => {
    return adminVmList.filter(vm => vm.isAssigned).length
  }, [adminVmList])

  /**
   * 获取未分配的VM数量
   */
  const getUnassignedVmCount = useCallback((): number => {
    return adminVmList.filter(vm => !vm.isAssigned).length
  }, [adminVmList])

  // ==================== 返回接口 ====================

  return {
    // 状态
    adminVmList,
    adminVmListTotal,
    adminVmListLoading,
    adminVmListError,
    vmAssignments,
    vmAssignmentsLoading,
    vmAssignmentsError,
    userVmMap,
    userVmMapLoading,
    userVmMapError,
    unassignedVms,
    unassignedVmsTotal,
    unassignedVmsLoading,
    unassignedVmsError,
    assignmentOverview,
    assignmentOverviewLoading,
    assignmentOverviewError,
    operationLoading,
    operationError,
    adminVmPagination,
    adminVmQueryParams,
    
    // 操作方法
    fetchAdminVmList,
    refreshAdminVmList,
    assignVmToUser,
    unassignVmFromUser,
    fetchVmAssignments,
    updateUserVmPermissions,
    fetchUserVmList,
    batchAssignVmsToUser,
    batchRemoveUserVms,
    fetchUnassignedVmList,
    fetchVmAssignmentOverview,
    forceControlVm,
    setAdminVmPagination,
    setAdminVmQueryParams,
    resetAdminVmQueryParams,
    clearError,
    clearVmError,
    clearUserError,
    resetState,
    
    // 计算属性和工具方法
    isVmOperating,
    getVmOperationError,
    getVmAssignment,
    isVmAssignmentLoading,
    getVmAssignmentError,
    getUserVms,
    isUserVmListLoading,
    getUserVmListError,
    isVmAssigned,
    getVmAssignedUserCount,
    getVmsByStatus,
    getAssignedVmCount,
    getUnassignedVmCount,
  }
}

// ==================== 导出默认 Hook ====================
export default useAdminVM

