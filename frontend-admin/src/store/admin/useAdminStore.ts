/**
 * 管理员功能 Hook - 封装管理员状态和操作
 * 为组件层提供简洁的管理员功能访问接口
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { useCallback } from 'react'
import { useAdminStore } from './adminStore'
import type { User, PaginatedResponse } from '@/types'
import type { 
  UserListParams,
  CreateUserRequest,
  UpdateUserRequest,
  LockUserRequest,
  ResetPasswordRequest,
  GrantPermissionRequest,
  Permission
} from '@/services'

// ==================== Hook 实现 ====================

export const useAdmin = () => {
  // 获取状态
  const userList = useAdminStore((state) => state.userList)
  const userListTotal = useAdminStore((state) => state.userListTotal)
  const userListLoading = useAdminStore((state) => state.userListLoading)
  const userListError = useAdminStore((state) => state.userListError)
  
  const currentUser = useAdminStore((state) => state.currentUser)
  const currentUserLoading = useAdminStore((state) => state.currentUserLoading)
  const currentUserError = useAdminStore((state) => state.currentUserError)
  
  
  const operationLoading = useAdminStore((state) => state.operationLoading)
  const operationError = useAdminStore((state) => state.operationError)
  
  const createUserLoading = useAdminStore((state) => state.createUserLoading)
  const createUserError = useAdminStore((state) => state.createUserError)
  
  const pagination = useAdminStore((state) => state.pagination)
  const queryParams = useAdminStore((state) => state.queryParams)
  const userStatistics = useAdminStore((state) => state.userStatistics)

  // 获取操作方法
  const fetchUserListAction = useAdminStore((state) => state.fetchUserList)
  const refreshUserListAction = useAdminStore((state) => state.refreshUserList)
  const fetchUserDetailAction = useAdminStore((state) => state.fetchUserDetail)
  const setCurrentUserAction = useAdminStore((state) => state.setCurrentUser)
  const createUserAction = useAdminStore((state) => state.createUser)
  const updateUserAction = useAdminStore((state) => state.updateUser)
  const deleteUserAction = useAdminStore((state) => state.deleteUser)
  const lockUserAction = useAdminStore((state) => state.lockUser)
  const unlockUserAction = useAdminStore((state) => state.unlockUser)
  const resetUserPasswordAction = useAdminStore((state) => state.resetUserPassword)
  const setPaginationAction = useAdminStore((state) => state.setPagination)
  const setQueryParamsAction = useAdminStore((state) => state.setQueryParams)
  const resetQueryParamsAction = useAdminStore((state) => state.resetQueryParams)
  const fetchUserStatisticsAction = useAdminStore((state) => state.fetchUserStatistics)
  const clearErrorAction = useAdminStore((state) => state.clearError)
  const clearUserErrorAction = useAdminStore((state) => state.clearUserError)
  const resetStateAction = useAdminStore((state) => state.resetState)

  // ==================== 封装操作方法 ====================

  /**
   * 获取用户列表
   */
  const fetchUserList = useCallback(async (params?: UserListParams) => {
    try {
      await fetchUserListAction(params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取用户列表失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchUserListAction])

  /**
   * 刷新用户列表
   */
  const refreshUserList = useCallback(async () => {
    try {
      await refreshUserListAction()
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '刷新用户列表失败'
      return { success: false, error: errorMessage }
    }
  }, [refreshUserListAction])

  /**
   * 获取用户详情
   */
  const fetchUserDetail = useCallback(async (userId: string) => {
    try {
      await fetchUserDetailAction(userId)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取用户详情失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchUserDetailAction])

  /**
   * 设置当前用户
   */
  const setCurrentUser = useCallback((user: User | null) => {
    setCurrentUserAction(user)
  }, [setCurrentUserAction])

  /**
   * 创建用户
   */
  const createUser = useCallback(async (userData: CreateUserRequest) => {
    try {
      const userId = await createUserAction(userData)
      return { success: true, error: null, data: userId }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '创建用户失败'
      return { success: false, error: errorMessage, data: null }
    }
  }, [createUserAction])

  /**
   * 更新用户信息
   */
  const updateUser = useCallback(async (userId: string, userData: UpdateUserRequest) => {
    try {
      await updateUserAction(userId, userData)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '更新用户失败'
      return { success: false, error: errorMessage }
    }
  }, [updateUserAction])

  /**
   * 删除用户
   */
  const deleteUser = useCallback(async (userId: string) => {
    try {
      await deleteUserAction(userId)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '删除用户失败'
      return { success: false, error: errorMessage }
    }
  }, [deleteUserAction])

  /**
   * 锁定用户
   */
  const lockUser = useCallback(async (userId: string, request?: LockUserRequest) => {
    try {
      await lockUserAction(userId, request)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '锁定用户失败'
      return { success: false, error: errorMessage }
    }
  }, [lockUserAction])

  /**
   * 解锁用户
   */
  const unlockUser = useCallback(async (userId: string) => {
    try {
      await unlockUserAction(userId)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '解锁用户失败'
      return { success: false, error: errorMessage }
    }
  }, [unlockUserAction])

  /**
   * 重置用户密码
   */
  const resetUserPassword = useCallback(async (userId: string, request: ResetPasswordRequest) => {
    try {
      await resetUserPasswordAction(userId, request)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '重置密码失败'
      return { success: false, error: errorMessage }
    }
  }, [resetUserPasswordAction])


  /**
   * 设置分页参数
   */
  const setPagination = useCallback((page: number, size?: number) => {
    setPaginationAction(page, size)
  }, [setPaginationAction])

  /**
   * 设置查询参数
   */
  const setQueryParams = useCallback((params: UserListParams) => {
    setQueryParamsAction(params)
  }, [setQueryParamsAction])

  /**
   * 重置查询参数
   */
  const resetQueryParams = useCallback(() => {
    resetQueryParamsAction()
  }, [resetQueryParamsAction])

  /**
   * 获取用户统计
   */
  const fetchUserStatistics = useCallback(async () => {
    try {
      await fetchUserStatisticsAction()
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取用户统计失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchUserStatisticsAction])

  /**
   * 清除错误信息
   */
  const clearError = useCallback(() => {
    clearErrorAction()
  }, [clearErrorAction])

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
   * 检查用户是否正在执行操作
   */
  const isUserOperating = useCallback((userId: string, operation?: string): boolean => {
    if (operation) {
      return !!operationLoading[`${operation}-${userId}`]
    }
    
    // 检查是否有任何操作正在进行
    const operations = ['update', 'delete', 'lock', 'unlock', 'reset-password']
    return operations.some(op => !!operationLoading[`${op}-${userId}`])
  }, [operationLoading])

  /**
   * 获取用户操作错误
   */
  const getUserOperationError = useCallback((userId: string, operation: string): string | null => {
    return operationError[`${operation}-${userId}`] || null
  }, [operationError])

  /**
   * 检查用户是否可以锁定
   */
  const canLockUser = useCallback((user: User): boolean => {
    if (!user) return false
    return user.status === 'ACTIVE' && !isUserOperating(user.userId)
  }, [isUserOperating])

  /**
   * 检查用户是否可以解锁
   */
  const canUnlockUser = useCallback((user: User): boolean => {
    if (!user) return false
    return user.status === 'LOCKED' && !isUserOperating(user.userId)
  }, [isUserOperating])

  /**
   * 检查用户是否可以删除
   */
  const canDeleteUser = useCallback((user: User): boolean => {
    if (!user) return false
    return user.role !== 'ADMIN' && !isUserOperating(user.userId)
  }, [isUserOperating])

  /**
   * 检查用户是否可以编辑
   */
  const canEditUser = useCallback((user: User): boolean => {
    if (!user) return false
    return !isUserOperating(user.userId)
  }, [isUserOperating])

  /**
   * 获取不同角色的用户数量
   */
  const getUserCountByRole = useCallback((role: string): number => {
    return userList.filter(user => user.role === role).length
  }, [userList])

  /**
   * 获取不同状态的用户数量
   */
  const getUserCountByStatus = useCallback((status: string): number => {
    return userList.filter(user => user.status === status).length
  }, [userList])

  /**
   * 检查是否有任何创建操作正在进行
   */
  const isCreatingUser = createUserLoading


  // ==================== 返回接口 ====================

  return {
    // 状态
    userList,
    userListTotal,
    userListLoading,
    userListError,
    currentUser,
    currentUserLoading,
    currentUserError,
    operationLoading,
    operationError,
    createUserLoading,
    createUserError,
    pagination,
    queryParams,
    userStatistics,
    
    // 操作方法
    fetchUserList,
    refreshUserList,
    fetchUserDetail,
    setCurrentUser,
    createUser,
    updateUser,
    deleteUser,
    lockUser,
    unlockUser,
    resetUserPassword,
    setPagination,
    setQueryParams,
    resetQueryParams,
    fetchUserStatistics,
    clearError,
    clearUserError,
    resetState,
    
    // 计算属性和工具方法
    isUserOperating,
    getUserOperationError,
    canLockUser,
    canUnlockUser,
    canDeleteUser,
    canEditUser,
    getUserCountByRole,
    getUserCountByStatus,
    isCreatingUser,
  }
}

// ==================== 导出默认 Hook ====================
export default useAdmin
